package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class NoteReminderActivity extends Activity {
    private long noteId=-1L;

    @Override protected void onCreate(Bundle state){
        AppSettings.applyTheme(this);
        super.onCreate(state);
        if(Build.VERSION.SDK_INT>=27){
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }else{
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            |WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        |WindowManager.LayoutParams.FLAG_FULLSCREEN);
        enterImmersiveMode();
        bind(getIntent());
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        bind(intent);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus)enterImmersiveMode();
    }

    private void enterImmersiveMode(){
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        |View.SYSTEM_UI_FLAG_FULLSCREEN
                        |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        |View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void bind(Intent intent){
        noteId=intent==null?-1L:intent.getLongExtra("noteId",-1L);
        NoForgetItem item=new NoForgetStore(this).find(noteId);
        if(item==null){
            finish();
            return;
        }

        LinearLayout content=new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(20),dp(42),dp(20),dp(24));

        TextView heading=text(
                item.title.trim().isEmpty()?AppString.get(R.string.runtime_text_0311):item.title,
                28);
        heading.setGravity(Gravity.CENTER);
        content.addView(heading,matchWrap());

        if(!item.body.trim().isEmpty()){
            TextView body=text(item.body,17);
            body.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams bp=matchWrap();
            bp.topMargin=dp(14);
            content.addView(body,bp);
        }

        addAttachments(content,NoteAttachment.parse(item.attachmentsJson));

        Button openNote=new Button(this);
        openNote.setText(AppString.get(R.string.runtime_text_0308));
        openNote.setAllCaps(false);
        openNote.setOnClickListener(v->{
            startActivity(
                    new Intent(this,NoForgetEditorActivity.class)
                            .putExtra("noteId",noteId));
            stopAndClose();
        });
        LinearLayout.LayoutParams op=matchWrap();
        op.topMargin=dp(18);
        content.addView(openNote,op);

        Button dismiss=new Button(this);
        dismiss.setText(AppString.get(R.string.runtime_text_0309));
        dismiss.setAllCaps(false);
        dismiss.setOnClickListener(v->stopAndClose());
        LinearLayout.LayoutParams dismissParams=matchWrap();
        dismissParams.topMargin=dp(8);
        content.addView(dismiss,dismissParams);

        ScrollView root=new ScrollView(this);
        root.setFillViewport(true);
        root.addView(content);
        setContentView(root);
    }

    private void addAttachments(
            LinearLayout root,
            List<NoteAttachment> values){
        if(values.isEmpty())return;

        TextView label=text(AppString.get(R.string.runtime_text_0310),13);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp=matchWrap();
        lp.topMargin=dp(20);
        root.addView(label,lp);

        HorizontalScrollView scroll=new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        scroll.setFillViewport(false);

        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundColor(Color.TRANSPARENT);

        for(NoteAttachment value:values){
            View card=attachmentView(value);
            card.setOnClickListener(v->value.open(this));
            LinearLayout.LayoutParams cp=
                    new LinearLayout.LayoutParams(dp(230),dp(200));
            cp.setMarginEnd(dp(10));
            row.addView(card,cp);
        }

        scroll.addView(
                row,
                new HorizontalScrollView.LayoutParams(
                        HorizontalScrollView.LayoutParams.WRAP_CONTENT,
                        HorizontalScrollView.LayoutParams.MATCH_PARENT));
        LinearLayout.LayoutParams sp=
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(200));
        sp.topMargin=dp(8);
        root.addView(scroll,sp);
    }

    private View attachmentView(NoteAttachment value){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(6),dp(6),dp(6),dp(6));
        card.setBackgroundColor(Color.TRANSPARENT);
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription(value.name);

        if(value.isImage()){
            ImageView image=new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(Color.TRANSPARENT);
            try{
                image.setImageURI(Uri.parse(value.value));
            }catch(Exception ignored){}
            card.addView(
                    image,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(160)));
        }else if(NoteAttachment.KIND_APP.equals(value.kind)){
            ImageView icon=new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            try{
                Drawable drawable=getPackageManager()
                        .getApplicationIcon(value.value);
                icon.setImageDrawable(drawable);
            }catch(Exception ignored){}
            card.addView(
                    icon,
                    new LinearLayout.LayoutParams(dp(92),dp(128)));
        }else{
            TextView symbol=text(
                    NoteAttachment.KIND_URL.equals(value.kind)?"🌐":"📎",
                    52);
            symbol.setGravity(Gravity.CENTER);
            card.addView(
                    symbol,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(132)));
        }

        TextView name=text(value.name,13);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        LinearLayout.LayoutParams np=matchWrap();
        np.topMargin=dp(4);
        card.addView(name,np);
        return card;
    }

    private TextView text(String value,int sp){
        TextView view=new TextView(this);
        view.setText(CalendarUtils.fa(value));
        view.setTextSize(sp);
        view.setTextColor(AppSettings.textPrimary(this));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap(){
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value){
        return Math.round(
                value*getResources().getDisplayMetrics().density);
    }

    private void stopAndClose(){
        stopService(NoForgetSoundService.stopIntent(this));
        finishAndRemoveTask();
    }

    @Override public void onBackPressed(){
        stopAndClose();
    }
}
