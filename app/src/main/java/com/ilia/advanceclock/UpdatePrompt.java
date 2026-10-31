package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Locale;

/** Download and verification stay in the optional bottom sheet; installation remains manual. */
final class UpdatePrompt {
    static final int SAVE_REQUEST=8261;
    private static WeakReference<Session> current=new WeakReference<>(null);
    private static Session session(Activity host) { Session s=current.get();return s!=null && s.host==host ? s:null; }
    static void dismiss(Activity host) { Session s=session(host);if(s!=null)s.close(); }
    static void pause(Activity host) { Session s=session(host);if(s!=null)s.pause(); }
    static boolean onActivityResult(Activity host,int request,int result,Intent data) {
        if(request!=SAVE_REQUEST && request!=UpdateInstaller.PERMISSION_REQUEST && request!=UpdateInstaller.INSTALL_REQUEST)return false;
        Session s=session(host);
        if(s!=null && !s.closed) {
            if(request==SAVE_REQUEST)s.saved(result,data);
            else if(request==UpdateInstaller.PERMISSION_REQUEST)s.installPermissionReturned();
            else s.status.setText(result==Activity.RESULT_OK?R.string.update_install_requested:R.string.update_install_cancelled);
        }
        return true;
    }
    static void show(Activity host,AppUpdates.Release release,boolean manual) {
        if(release==null || host.isFinishing() || host.isDestroyed())return;
        if(!manual && AppUpdates.prefs(host).getLong("shown_code",0)==release.code)return;
        Session previous=current.get();if(previous!=null)previous.close();
        Session s=new Session(host,release);current=new WeakReference<>(s);s.show();
        AppUpdates.prefs(host).edit().putLong("shown_code",release.code).apply();
    }
    interface Downloader {
        File download(android.content.Context context, AppUpdates.Release release, AppUpdates.DownloadObserver observer,
                ForegroundUpdates.Request request) throws Exception;
    }
    static final class Session {
        final Activity host;final AppUpdates.Release release;final Dialog dialog;final Downloader downloader;
        final Handler main=new Handler(Looper.getMainLooper());
        final LinearLayout root;final TextView status,amount;final ProgressBar progress;
        final Button later,download,files;
        ForegroundUpdates.Request request;Uri installUri,publicUri;boolean working,ready,awaitingSave,closed;int generation;
        Session(Activity host,AppUpdates.Release release) { this(host,release,AppUpdates::download); }
        Session(Activity host,AppUpdates.Release release,Downloader downloader) {
            this.downloader=downloader;this.host=host;this.release=release;dialog=new Dialog(host);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            root=new LinearLayout(host);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(AppSettings.layoutDirection(host));
            root.setPadding(dp(host,20),dp(host,12),dp(host,20),dp(host,20));
            GradientDrawable bg=shape(AppSettings.surface(host),dp(host,24));float r=dp(host,24);
            bg.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});bg.setStroke(dp(host,1),AppSettings.primaryColor(host));root.setBackground(bg);
            LinearLayout header=new LinearLayout(host);header.setGravity(Gravity.CENTER_VERTICAL);
            TextView title=text(host,host.getString(R.string.update_available),20);title.setTypeface(null,Typeface.BOLD);title.setTag("update_prompt_title");
            header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            ImageButton close=new ImageButton(host);close.setImageResource(R.drawable.ic_md_close);close.setColorFilter(AppSettings.textPrimary(host));
            close.setBackgroundColor(Color.TRANSPARENT);close.setContentDescription(host.getString(R.string.runtime_text_0002));
            close.setTag("update_close_button");close.setOnClickListener(v->close());
            header.addView(close,new LinearLayout.LayoutParams(dp(host,44),dp(host,44)));root.addView(header);
            String language=Locale.forLanguageTag(AppSettings.language(host)).getLanguage();
            String message=release.text("title",language);if(!message.isEmpty())root.addView(text(host,message,16));
            root.addView(text(host,release.version,14));
            String notes=release.text("releaseNotes",language);
            if(!notes.isEmpty()) {
                root.addView(text(host,host.getString(R.string.update_whats_new),16));
                TextView body=text(host,notes,15);body.setTag("update_release_notes");
                ScrollView scroll=new ScrollView(host){@Override protected void onMeasure(int w,int h){super.onMeasure(w,View.MeasureSpec.makeMeasureSpec(dp(host,190),View.MeasureSpec.AT_MOST));}};
                scroll.addView(body);root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
            }
            status=text(host,"",14);status.setTag("update_download_status");status.setVisibility(View.GONE);root.addView(status);
            progress=new ProgressBar(host,null,android.R.attr.progressBarStyleHorizontal);progress.setTag("update_download_progress");
            progress.setMax(1000);progress.setProgressTintList(ColorStateList.valueOf(AppSettings.primaryColor(host)));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf((AppSettings.primaryColor(host)&0xFFFFFF)|0x22000000));
            LayerDrawable bar=new LayerDrawable(new android.graphics.drawable.Drawable[]{
                    shape((AppSettings.primaryColor(host)&0xFFFFFF)|0x22000000,dp(host,6)),
                    new ClipDrawable(shape(AppSettings.primaryColor(host),dp(host,6)),Gravity.LEFT,ClipDrawable.HORIZONTAL)});
            bar.setId(0,android.R.id.background);bar.setId(1,android.R.id.progress);progress.setProgressDrawable(bar);
            progress.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            progress.setVisibility(View.GONE);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(host,8)));
            amount=text(host,"",13);amount.setTag("update_download_amount");amount.setTextDirection(View.TEXT_DIRECTION_LTR);
            amount.setGravity(Gravity.CENTER);amount.setVisibility(View.GONE);root.addView(amount);
            LinearLayout actions=new LinearLayout(host);actions.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);actions.setPadding(0,dp(host,12),0,0);
            later=new Button(host);later.setText(R.string.update_later);later.setTag("update_later_button");style(later,false);later.setOnClickListener(v->close());
            download=new Button(host);download.setText(R.string.update_download);download.setTag("update_download_button");style(download,true);download.setOnClickListener(v->download());
            actions.addView(later,new LinearLayout.LayoutParams(0,dp(host,48),1));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(host,48),1);p.leftMargin=dp(host,10);actions.addView(download,p);root.addView(actions);
            files=new Button(host);files.setText(R.string.update_open_files);files.setTag("update_open_files_button");style(files,false);
            files.setVisibility(View.GONE);files.setOnClickListener(v->openDownloads());
            LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,dp(host,44));fp.topMargin=dp(host,8);root.addView(files,fp);
        }
        void style(Button button,boolean primary) {
            int accent=AppSettings.primaryColor(host);GradientDrawable bg=shape(primary?accent:AppSettings.surface(host),dp(host,14));bg.setStroke(dp(host,1),accent);
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(primary?0x33FFFFFF:(accent&0xFFFFFF)|0x22000000),bg,shape(Color.WHITE,dp(host,14))));
            button.setBackgroundTintList(null);button.setStateListAnimator(null);button.setElevation(0);button.setAllCaps(false);button.setTextSize(14);
            button.setSingleLine(true);button.setAutoSizeTextTypeUniformWithConfiguration(10,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);
            button.setTextColor(primary?(Color.luminance(accent)>.179?Color.BLACK:Color.WHITE):accent);button.setPadding(dp(host,10),0,dp(host,10),0);
        }
        void show() {
            dialog.setContentView(root);dialog.setCanceledOnTouchOutside(false);dialog.setCancelable(false);dialog.show();
            Window window=dialog.getWindow();if(window!=null){window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));window.setLayout(-1,-2);window.setGravity(Gravity.BOTTOM);}
            root.setTranslationY(dp(host,80));root.setAlpha(0);root.animate().translationY(0).alpha(1).setDuration(220).start();
        }
        void close() {closed=true;generation++;if(request!=null)request.cancel();dialog.dismiss();}
        void pause() {
            if(working && !awaitingSave){generation++;if(request!=null)request.cancel();working=false;download.setEnabled(true);status.setText(R.string.update_download_cancelled);download.setText(R.string.update_download_retry);}
        }
        boolean live(int token){return !closed && token==generation && !host.isDestroyed() && !host.isFinishing();}
        String filename(){return "InfiniTime-"+release.version+".apk";}
        void download() {
            if(ready){install();return;}
            if(working)return;
            working=true;download.setEnabled(false);status.setVisibility(View.VISIBLE);status.setText(R.string.update_downloading);
            progress.setVisibility(View.VISIBLE);progress.setProgress(0);amount.setVisibility(View.VISIBLE);amount.setText(volume(0,release.size));
            int token=++generation;request=new ForegroundUpdates.Request(host);ForegroundUpdates.Request operation=request;
            operation.thread=new Thread(()->{
                File file=null;
                try {
                    final long[] tick={0};
                    file=downloader.download(host.getApplicationContext(),release,(done,total,verifying)->{
                        long now=SystemClock.elapsedRealtime();if(!verifying && done!=0 && done!=total && now-tick[0]<80)return;tick[0]=now;
                        main.post(()->{if(live(token)){progress.setProgress((int)(done*1000/Math.max(1,total)),true);amount.setText(volume(done,total));if(verifying)status.setText(R.string.update_verifying);}});
                    },operation);
                    if(operation.cancelled)throw new java.io.IOException("Cancelled");
                    Uri staged=UpdatePackageProvider.stage(host.getApplicationContext(),file);
                    Uri published=null;
                    if(operation.cancelled)throw new java.io.IOException("Cancelled");
                    if(Build.VERSION.SDK_INT>=29) {
                        try{published=UpdateFiles.publish(host.getApplicationContext(),file,filename(),operation);}
                        catch(Exception exportError){/* Installation can use the private verified copy; manual export stays available. */}
                    }
                    file.delete();file=null;Uri exported=published;
                    main.post(()->{
                        if(!live(token))return;
                        installUri=staged;publicUri=exported;working=false;ready=true;download.setEnabled(true);
                        status.setText(R.string.update_install_ready);download.setText(R.string.update_install_button);files.setVisibility(View.VISIBLE);
                        install();
                    });
                }catch(Exception e){if(file!=null)file.delete();main.post(()->{if(live(token)){working=false;download.setEnabled(true);download.setText(R.string.update_download_retry);status.setText(AppUpdates.errorMessage(e));}});}
            },"verified-update-download");operation.thread.start();
        }
        void chooseSave() {
            if(installUri==null)return;
            try{awaitingSave=true;host.startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("application/vnd.android.package-archive").putExtra(Intent.EXTRA_LOCAL_ONLY,true).putExtra(Intent.EXTRA_TITLE,filename()),SAVE_REQUEST);}
            catch(ActivityNotFoundException e){awaitingSave=false;status.setText(R.string.update_storage_error);}
        }
        void saved(int result,Intent data) {
            awaitingSave=false;
            if(result!=Activity.RESULT_OK || data==null || data.getData()==null || installUri==null){status.setText(R.string.update_choose_location);return;}
            working=true;download.setEnabled(false);int token=++generation;Uri target=data.getData();
            request=new ForegroundUpdates.Request(host);ForegroundUpdates.Request operation=request;
            operation.thread=new Thread(()->{
                try{UpdateFiles.save(host.getApplicationContext(),installUri,target,operation);main.post(()->{if(live(token)){publicUri=target;working=false;ready=true;download.setEnabled(true);download.setText(R.string.update_install_button);status.setText(R.string.update_manual_saved);UpdateInstaller.openFiles(host,publicUri);}});}
                catch(Exception e){main.post(()->{if(live(token)){working=false;download.setEnabled(true);status.setText(AppUpdates.errorMessage(e));}});}
            },"save-verified-update");operation.thread.start();
        }
        void install() {
            if(installUri==null)return;
            if(!UpdateInstaller.allowed(host)) {
                status.setText(R.string.update_install_permission);
                try{host.startActivityForResult(UpdateInstaller.permissionIntent(host),UpdateInstaller.PERMISSION_REQUEST);}
                catch(ActivityNotFoundException|SecurityException error){permissionDenied();}
                return;
            }
            status.setText(R.string.update_install_requested);
            if(!UpdateInstaller.launch(host,installUri))status.setText(R.string.update_installer_unavailable);
        }
        void installPermissionReturned(){if(UpdateInstaller.allowed(host))install();else permissionDenied();}
        void permissionDenied(){status.setText(R.string.update_install_denied);files.setVisibility(View.VISIBLE);download.setEnabled(true);}
        void openDownloads(){
            if(publicUri==null){chooseSave();return;}
            if(!UpdateInstaller.openFiles(host,publicUri))status.setText(R.string.update_file_manager_unavailable);
        }
    }
    static String volume(long done,long total){return String.format(Locale.ROOT,"%.2f / %.2f MB  ·  %d%%",done/1048576d,total/1048576d,(int)(done*100/Math.max(1,total)));}
    private static GradientDrawable shape(int color,float radius){GradientDrawable bg=new GradientDrawable();bg.setColor(color);bg.setCornerRadius(radius);return bg;}
    private static TextView text(Activity host,String value,int size){TextView t=new TextView(host);t.setText(value);t.setTextSize(size);t.setTextColor(AppSettings.textPrimary(host));t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);t.setPadding(0,dp(host,5),0,dp(host,5));return t;}
    private static int dp(Activity host,int n){return Math.round(n*host.getResources().getDisplayMetrics().density);}
}
