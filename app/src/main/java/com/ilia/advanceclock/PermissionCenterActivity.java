package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** One deliberate tap starts a finite sequence; each system prompt remains user-controlled. */
public final class PermissionCenterActivity extends Activity {
    static final int REQUEST=7193;
    private LinearLayout content;
    private boolean batchRunning;
    private int attemptedMask;
    private int pendingKind = -1;
    private static final int SETTINGS_REQUEST = 620;

    static boolean granted(Context c,int kind) {
        switch(kind) {
            case 0:return PermissionHelper.notificationsGranted(c);
            case 1:return PermissionHelper.exactAlarmsGranted(c);
            case 2:return PermissionHelper.fullScreenGranted(c);
            case 3:return QiblaUtils.hasLocationPermission(c);
            default:return c.getPackageManager().canRequestPackageInstalls();
        }
    }
    static boolean incomplete(Context c) {
        for(int i=0;i<5;i++) if(!granted(c,i))return true;
        return false;
    }
    static boolean requiredIncomplete(Context c) {
        for(int i=0;i<3;i++) if(!granted(c,i))return true;
        return false;
    }
    @Override public void onCreate(Bundle state) {
        AppSettings.applyTheme(this);super.onCreate(state);
        if(state!=null) {
            batchRunning=state.getBoolean("batchRunning");
            attemptedMask=state.getInt("attemptedMask");
            pendingKind=state.getInt("pendingKind",-1);
        }
        render();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("batchRunning",batchRunning);
        state.putInt("attemptedMask",attemptedMask);
        state.putInt("pendingKind",pendingKind);
        super.onSaveInstanceState(state);
    }
    @Override protected void onResume() {
        super.onResume();
        if (closeIfComplete()) return;
        if(content!=null)render();
        if(batchRunning && pendingKind<0)postNext();
    }
    private boolean closeIfComplete() {
        if (incomplete(this)) return false;
        batchRunning=false;
        setResult(RESULT_OK);
        finish();
        return true;
    }
    private void startBatch(int first) {
        if(batchRunning)return;
        batchRunning=true;attemptedMask=0;
        launchStep(first);
    }
    private void launchStep(int kind) {
        attemptedMask |= 1 << kind;
        pendingKind=kind;
        request(kind);
    }
    private void postNext() {
        getWindow().getDecorView().post(() -> {
            if(!batchRunning || pendingKind>=0 || isFinishing() || isDestroyed())return;
            for(int i=0;i<3;i++) {
                if((attemptedMask & (1 << i))==0 && !granted(this,i)) {
                    launchStep(i);return;
                }
            }
            batchRunning=false;
            if(!incomplete(this)) { setResult(RESULT_OK);finish(); }
            else { render();leave(); }
        });
    }
    private void stepReturned() {
        pendingKind=-1;
        if (closeIfComplete()) return;
        render();postNext();
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==SETTINGS_REQUEST && batchRunning && pendingKind>=0)stepReturned();
    }
    private void openSettings(Intent intent) {
        startActivityForResult(intent,SETTINGS_REQUEST);
    }
    private void render() {
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(AppSettings.layoutDirection(this));
        content.setPadding(dp(20),dp(20),dp(20),dp(20));
        content.setBackgroundColor(AppSettings.background(this));
        text(getString(R.string.permissions_title),23,AppSettings.textPrimary(this));
        int[] titles={R.string.permissions_notifications,R.string.permissions_alarms,R.string.permissions_fullscreen,R.string.permissions_location,R.string.permissions_install};
        int[] reasons={R.string.permissions_notifications_reason,R.string.permissions_alarms_reason,R.string.permissions_fullscreen_reason,R.string.permissions_location_reason,R.string.permissions_install_reason};
        for(int i=0;i<titles.length;i++) {
            int kind=i;boolean allowed=granted(this,i);
            int color=allowed?0xFF238047:(i>=3 ? (AppSettings.themeMode(this)==AppSettings.THEME_DARK?0xFFFFB74D:0xFFEF6C00) : 0xFFC62828);
            boolean optionalMissing=i>=3 && !allowed;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14),dp(12),dp(14),dp(12));
            GradientDrawable bg=new GradientDrawable();bg.setColor(AppSettings.surface(this));bg.setCornerRadius(dp(16));bg.setStroke(dp(1),color);card.setBackground(bg);
            LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(12);content.addView(card,cp);
            LinearLayout previous=content;content=card;
            text(getString(titles[i]),17,AppSettings.textPrimary(this));
            text(getString(reasons[i]),14,optionalMissing?color:AppSettings.textSecondary(this));
            text(getString(allowed?R.string.permissions_granted:R.string.permissions_missing),14,color);
            Button action=button(getString(allowed?R.string.permissions_manage:R.string.permissions_allow),color);
            action.setTag("permission_"+i);action.setOnClickListener(v->startBatch(kind));
            content=previous;
        }
        button(getString(R.string.permissions_close),AppSettings.primaryColor(this)).setOnClickListener(v->leave());
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.addView(content);
        setContentView(scroll);AlertWindowInsets.apply(getWindow(),scroll);
    }
    /** Leaving with missing permissions first explains what will not work. */
    private void leave() {
        if(!incomplete(this)){finish();return;}
        int[] effects={R.string.permissions_missing_notifications,R.string.permissions_missing_alarms,
                R.string.permissions_missing_fullscreen,R.string.permissions_missing_location,R.string.permissions_missing_install};
        int[] titles={R.string.permissions_notifications,R.string.permissions_alarms,R.string.permissions_fullscreen,R.string.permissions_location,R.string.permissions_install};
        android.app.Dialog dialog=new android.app.Dialog(this);
        AppMotion.window(dialog.getWindow(),R.style.MotionDialog);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(AppSettings.layoutDirection(this));box.setPadding(dp(18),dp(16),dp(18),dp(16));
        GradientDrawable bg=new GradientDrawable();bg.setColor(AppSettings.surface(this));bg.setCornerRadius(dp(24));box.setBackground(bg);
        TextView title=new TextView(this);title.setText(getString(R.string.permissions_missing_title));title.setTextSize(20);
        title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);title.setTextColor(AppSettings.textPrimary(this));
        title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);box.addView(title);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(this);scroll.addView(list);
        LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1f);sp.topMargin=dp(6);box.addView(scroll,sp);
        int first=-1;
        for(int i=0;i<effects.length;i++)if(!granted(this,i)){
            if(first<0)first=i;boolean optional=i>=3;
            int color=optional?(AppSettings.themeMode(this)==AppSettings.THEME_DARK?0xFFFFB74D:0xFFEF6C00):0xFFC62828;
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(10),dp(14),dp(10));
            GradientDrawable cb=new GradientDrawable();cb.setColor(AppSettings.field(this));cb.setCornerRadius(dp(16));cb.setStroke(dp(1),color);card.setBackground(cb);
            TextView name=new TextView(this);name.setText(getString(titles[i]));name.setTextSize(15);name.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);name.setTextColor(color);card.addView(name);
            TextView effect=new TextView(this);effect.setText(getString(effects[i]));effect.setTextSize(13);effect.setTextColor(AppSettings.textSecondary(this));effect.setPadding(0,dp(3),0,0);card.addView(effect);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);list.addView(card,lp);
        }
        int start=first;
        // Red "continue anyway" on the left, "grant" on the right, in every language.
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);actions.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        Button skip=new Button(this);skip.setText(getString(R.string.permissions_continue_anyway));skip.setAllCaps(false);skip.setTextColor(0xFFFFFFFF);
        GradientDrawable red=new GradientDrawable();red.setColor(0xFFC62828);red.setCornerRadius(dp(14));skip.setBackground(red);skip.setStateListAnimator(null);
        skip.setOnClickListener(v->{dialog.dismiss();finish();});
        Button grant=new Button(this);grant.setText(getString(R.string.permissions_allow));grant.setAllCaps(false);grant.setTextColor(0xFFFFFFFF);
        grant.setBackgroundResource(R.drawable.bg_teal_button);grant.setStateListAnimator(null);
        grant.setOnClickListener(v->{dialog.dismiss();startBatch(start);});
        actions.addView(skip,new LinearLayout.LayoutParams(0,dp(52),1f));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(0,dp(52),1f);gp.leftMargin=dp(8);actions.addView(grant,gp);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(14);box.addView(actions,ap);
        dialog.setContentView(box);dialog.setCanceledOnTouchOutside(true);dialog.show();
        android.view.Window window=dialog.getWindow();
        if(window!=null){window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            int width=Math.min(getResources().getDisplayMetrics().widthPixels-dp(32),dp(480));
            window.setLayout(width,android.view.ViewGroup.LayoutParams.WRAP_CONTENT);}
    }
    @Override public void onBackPressed() {
        if(incomplete(this))leave();else super.onBackPressed();
    }
    private void request(int kind) {
        try {
            if(kind==0 && Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
                requestRuntime(new String[]{Manifest.permission.POST_NOTIFICATIONS},kind);return;
            }
            if(kind==3 && !granted(this,3)) {
                requestRuntime(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},kind);return;
            }
            String action=Settings.ACTION_APPLICATION_DETAILS_SETTINGS;
            if(kind==0) {
                openSettings(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,getPackageName()));return;
            }
            if(kind==1 && Build.VERSION.SDK_INT>=31)action=Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM;
            if(kind==2 && Build.VERSION.SDK_INT>=34)action=Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT;
            if(kind==4)action=Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES;
            openSettings(new Intent(action,Uri.parse("package:"+getPackageName())));
        } catch(RuntimeException unavailable) {
            LogoToast.makeText(this,getString(R.string.permissions_unavailable),Toast.LENGTH_LONG).show();
            stepReturned();
        }
    }
    private void requestRuntime(String[] permissions,int kind) {
        android.content.SharedPreferences prefs=getSharedPreferences("permission_center",0);
        if(prefs.getBoolean("asked_"+kind,false) && !shouldShowRequestPermissionRationale(permissions[permissions.length-1])) {
            openSettings(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())));return;
        }
        prefs.edit().putBoolean("asked_"+kind,true).apply();requestPermissions(permissions,kind+100);
    }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(batchRunning && pendingKind>=0 && request==pendingKind+100)stepReturned();
        else render();
    }
    private void text(String value,int size,int color) {
        TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);
        t.setPadding(0,dp(5),0,dp(5));content.addView(t,new LinearLayout.LayoutParams(-1,-2));
    }
    private Button button(String title,int color) {
        Button b=new Button(this);b.setText(title);b.setTextColor(color);b.setAllCaps(false);b.setMinHeight(dp(48));
        GradientDrawable background=new GradientDrawable();
        background.setColor((color & 0x00FFFFFF) | 0x14000000);
        background.setCornerRadius(dp(12));background.setStroke(dp(1),color);
        b.setBackground(background);b.setElevation(0);b.setStateListAnimator(null);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(8);
        content.addView(b,params);return b;
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
