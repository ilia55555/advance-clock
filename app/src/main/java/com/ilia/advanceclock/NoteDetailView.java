package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.view.*;
import android.widget.*;

/** Read-only note contents, separate from editing and ringing. */
final class NoteDetailView extends LinearLayout {
    private final Activity host;
    private final long noteId;
    private final Runnable closeAction;
    private LinearLayout content;
    NoteDetailView(Activity host,long noteId,Runnable closeAction){
        super(host);this.host=host;this.noteId=noteId;this.closeAction=closeAction;setTag("note_detail_view");
        LinearLayout page=this;page.setOrientation(LinearLayout.VERTICAL);page.setBackgroundColor(AppSettings.background(host));
        ScrollView scroll=new ScrollView(host);scroll.setFillViewport(true);
        content=new LinearLayout(host);content.setOrientation(LinearLayout.VERTICAL);content.setLayoutDirection(AppSettings.layoutDirection(host));content.setPadding(dp(16),dp(16),dp(16),dp(16));scroll.addView(content);
        page.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout actions=new LinearLayout(host);actions.setTag("note_detail_actions");actions.setOrientation(LinearLayout.HORIZONTAL);actions.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);actions.setGravity(Gravity.CENTER_VERTICAL);actions.setPadding(dp(16),dp(10),dp(16),dp(12));actions.setBackgroundColor(AppSettings.surface(host));
        Button close=footerButton(R.string.runtime_text_0002,false);close.setTag("note_detail_close");close.setOnClickListener(v->closeAction.run());actions.addView(close,new LinearLayout.LayoutParams(0,-2,1));
        actions.addView(new Space(host),new LinearLayout.LayoutParams(dp(8),1));
        Button edit=footerButton(R.string.runtime_text_0007,true);edit.setTag("note_detail_edit");edit.setOnClickListener(v->AppMotion.launch(host,new Intent(host,NoForgetEditorActivity.class).putExtra("noteId",noteId),content));actions.addView(edit,new LinearLayout.LayoutParams(0,-2,1));
        page.addView(actions,new LinearLayout.LayoutParams(-1,-2));
    }
    boolean render(){
        NoForgetItem item=new NoForgetStore(host).find(noteId);if(item==null)return false;content.removeAllViews();
        if(!item.title.trim().isEmpty())text(item.title,22,true);
        if(!item.body.trim().isEmpty())text(item.body,17,false);
        if(!"[]".equals(item.sketchJson)){
            SketchView sketch=new SketchView(host);sketch.setTag("note_detail_sketch");sketch.setReadOnly(true);sketch.setClickable(false);sketch.setGridVisible(false);sketch.load(item.sketchJson);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(280));lp.topMargin=dp(12);content.addView(sketch,lp);
        }
        for(String uri:new String[]{item.imageUri1,item.imageUri2})if(uri!=null && !uri.isEmpty()){
            AlarmPhotoView photo=new AlarmPhotoView(host,false);photo.setFitWithinBounds(true);photo.load(uri);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);content.addView(photo,lp);
        }
        for(NoteAttachment attachment:NoteAttachment.parse(item.attachmentsJson)){
            Button file=new Button(host);file.setAllCaps(false);file.setText(attachment.name);file.setTextColor(AppSettings.primaryColor(host));file.setMinHeight(dp(52));file.setBackground(ReminderModalStyle.shape(host,AppSettings.field(host),AppSettings.primaryColor(host),12));file.setOnClickListener(v->attachment.open(host));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);content.addView(file,lp);
        }
        return true;
    }
    private Button footerButton(int label,boolean primary){
        Button button=new Button(host);button.setText(label);button.setAllCaps(false);button.setTextSize(14);button.setMinHeight(dp(48));button.setMinimumHeight(dp(48));button.setPadding(dp(12),dp(8),dp(12),dp(8));button.setElevation(0);button.setStateListAnimator(null);button.setBackgroundTintList(null);
        int color=AppSettings.primaryColor(host);button.setTextColor(primary?(android.graphics.Color.luminance(color)>.179f?android.graphics.Color.BLACK:android.graphics.Color.WHITE):color);button.setBackground(ReminderModalStyle.shape(host,primary?color:AppSettings.surface(host),color,12));return button;
    }
    private void text(String value,int size,boolean bold){TextView text=new TextView(host);text.setTag(bold?"note_detail_title":"note_detail_body");text.setText(value);text.setTextSize(size);text.setTextColor(AppSettings.textPrimary(host));text.setTextIsSelectable(true);if(bold)text.setTypeface(null,android.graphics.Typeface.BOLD);text.setPadding(0,dp(12),0,dp(8));content.addView(text,new LinearLayout.LayoutParams(-1,-2));}
    private int dp(int n){return ReminderModalStyle.dp(host,n);}
}
