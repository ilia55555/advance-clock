package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.*;
import android.widget.*;

/** Read-only note contents, separate from editing and ringing. */
public final class NoteDetailActivity extends Activity {
    private LinearLayout content;
    @Override protected void onCreate(Bundle state){
        AppSettings.applyTheme(this);super.onCreate(state);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(AppSettings.background(this));
        content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setLayoutDirection(AppSettings.layoutDirection(this));content.setPadding(dp(16),dp(16),dp(16),dp(24));scroll.addView(content);setContentView(scroll);AppSettings.applyFullscreenInsets(scroll);
    }
    @Override protected void onResume(){super.onResume();render();}
    private void render(){
        NoForgetItem item=new NoForgetStore(this).find(getIntent().getLongExtra("noteId",-1));if(item==null){finish();return;}content.removeAllViews();
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.CENTER_VERTICAL);
        Button close=new Button(this);close.setText(R.string.runtime_text_0002);close.setOnClickListener(v->finish());actions.addView(close,new LinearLayout.LayoutParams(0,dp(48),1));
        Button edit=new Button(this);edit.setText(R.string.runtime_text_0007);edit.setOnClickListener(v->AppMotion.launch(this,new Intent(this,NoForgetEditorActivity.class).putExtra("noteId",item.id),content));actions.addView(edit,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(actions);
        if(!item.title.trim().isEmpty())text(item.title,22,true);
        if(!item.body.trim().isEmpty())text(item.body,17,false);
        if(!"[]".equals(item.sketchJson)){
            SketchView sketch=new SketchView(this);sketch.setTag("note_detail_sketch");sketch.setReadOnly(true);sketch.setClickable(false);sketch.setGridVisible(false);sketch.load(item.sketchJson);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(280));lp.topMargin=dp(12);content.addView(sketch,lp);
        }
        for(String uri:new String[]{item.imageUri1,item.imageUri2})if(uri!=null && !uri.isEmpty()){
            AlarmPhotoView photo=new AlarmPhotoView(this,false);photo.setFitWithinBounds(true);photo.load(uri);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);content.addView(photo,lp);
        }
        for(NoteAttachment attachment:NoteAttachment.parse(item.attachmentsJson)){
            Button file=new Button(this);file.setAllCaps(false);file.setText(attachment.name);file.setTextColor(AppSettings.primaryColor(this));file.setMinHeight(dp(52));file.setBackground(ReminderModalStyle.shape(this,AppSettings.field(this),AppSettings.primaryColor(this),12));file.setOnClickListener(v->attachment.open(this));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);content.addView(file,lp);
        }
    }
    private void text(String value,int size,boolean bold){TextView text=new TextView(this);text.setTag(bold?"note_detail_title":"note_detail_body");text.setText(value);text.setTextSize(size);text.setTextColor(AppSettings.textPrimary(this));text.setTextIsSelectable(true);if(bold)text.setTypeface(null,android.graphics.Typeface.BOLD);text.setPadding(0,dp(12),0,dp(8));content.addView(text,new LinearLayout.LayoutParams(-1,-2));}
    private int dp(int n){return ReminderModalStyle.dp(this,n);}
}
