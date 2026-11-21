package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Two independent local audio choices, saved together after validation. */
public final class MunajatSettingsActivity extends Activity {
    private int type;
    private final MunajatConfig[] values=new MunajatConfig[2];
    private final Button[] sounds=new Button[2];
    private final Switch[] enabled=new Switch[2],gradual=new Switch[2];
    private final SeekBar[] start=new SeekBar[2],end=new SeekBar[2];
    private EditText minutes;
    private TextView minutesHint;
    private final long[] durations=new long[2];
    @Override protected void onCreate(Bundle state){
        AppSettings.applyTheme(this);super.onCreate(state);type=getIntent().getIntExtra("adhanType",AdhanScheduler.FAJR);
        if(type!=899 && (type<0 || type>4)){finish();return;}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(AppSettings.layoutDirection(this));root.setPadding(dp(16),dp(16),dp(16),dp(24));root.setBackgroundColor(AppSettings.background(this));scroll.addView(root);
        TextView title=text(getString(R.string.munajat_title),22);title.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);root.addView(title);
        TextView subtitle=text(type==899?getString(R.string.munajat_all):AdhanScheduler.title(type),14);subtitle.setTextColor(AppSettings.primaryColor(this));subtitle.setPadding(0,dp(2),0,0);root.addView(subtitle);
        TextView hint=text(getString(R.string.munajat_description),13);hint.setTextColor(AppSettings.textSecondary(this));hint.setPadding(0,dp(8),0,dp(14));root.addView(hint);
        for(int i=0;i<2;i++){
            final int side=i;MunajatConfig v=MunajatConfig.load(this,type==899?0:type,i==0);values[i]=v;
            if(state!=null){v.uri=state.getString("uri"+i,v.uri);v.enabled=state.getBoolean("enabled"+i,v.enabled);v.gradual=state.getBoolean("gradual"+i,v.gradual);v.start=state.getInt("start"+i,v.start);v.end=state.getInt("end"+i,v.end);v.minutes=state.getInt("minutes",v.minutes);}
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackgroundResource(R.drawable.bg_card);card.setPadding(dp(14),dp(6),dp(14),dp(12));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(12);root.addView(card,cp);
            enabled[i]=toggle(getString(i==0?R.string.munajat_before:R.string.munajat_after),v.enabled);enabled[i].setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);enabled[i].setTextSize(16);enabled[i].setTag(i==0?"munajat_before_enabled":"munajat_after_enabled");card.addView(enabled[i]);
            // Details appear only while this side is switched on.
            LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);card.addView(body,new LinearLayout.LayoutParams(-1,-2));
            if(i==0){
                LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
                TextView label=text(getString(R.string.munajat_minutes),14);row.addView(label,new LinearLayout.LayoutParams(0,-2,1f));
                minutes=new EditText(this);minutes.setId(View.generateViewId());minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);minutes.setSingleLine(true);minutes.setGravity(Gravity.CENTER);minutes.setText(Integer.toString(v.minutes));minutes.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});minutes.setTextColor(AppSettings.textPrimary(this));minutes.setBackgroundResource(R.drawable.bg_field);minutes.setTag("munajat_minutes");
                row.addView(minutes,new LinearLayout.LayoutParams(dp(88),dp(48)));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2);rp.topMargin=dp(6);body.addView(row,rp);
                // Caret goes to the end so a single backspace clears the value.
                View.OnClickListener toEnd=view->minutes.post(()->minutes.setSelection(minutes.getText().length()));
                minutes.setOnClickListener(toEnd);
                minutes.setOnFocusChangeListener((view,focused)->{if(focused)toEnd.onClick(view);else normalizeMinutes();});
            } else {TextView after=text(getString(R.string.munajat_after_hint),13);after.setTextColor(AppSettings.textSecondary(this));body.addView(after);}
            sounds[i]=button("🎵  "+SoundLibrary.munajatName(this,v.uri));sounds[i].setGravity(Gravity.START|Gravity.CENTER_VERTICAL);sounds[i].setPadding(dp(14),0,dp(14),0);sounds[i].setBackgroundResource(R.drawable.bg_field);sounds[i].setTag("munajat_sound_"+i);sounds[i].setOnClickListener(view->startActivityForResult(new Intent(this,SoundPickerActivity.class).putExtra(SoundPickerActivity.EXTRA_MUNAJAT,true).putExtra("previewVolume",gradual[side].isChecked()?start[side].getProgress():end[side].getProgress())
                    .putExtra("rampStart",gradual[side].isChecked()?start[side].getProgress():-1).putExtra("rampEnd",end[side].getProgress()),920+side));
            LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,dp(52));sp.topMargin=dp(10);body.addView(sounds[i],sp);
            gradual[i]=toggle(getString(R.string.munajat_gradual),v.gradual);LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(-1,-2);gp.topMargin=dp(4);body.addView(gradual[i],gp);
            TextView startLabel=text("",13);startLabel.setTextColor(AppSettings.textSecondary(this));body.addView(startLabel);start[i]=slider(v.start);start[i].setTag("munajat_start_"+i);body.addView(start[i],new LinearLayout.LayoutParams(-1,dp(40)));
            TextView endLabel=text("",13);endLabel.setTextColor(AppSettings.textSecondary(this));body.addView(endLabel);end[i]=slider(v.end);end[i].setTag("munajat_end_"+i);body.addView(end[i],new LinearLayout.LayoutParams(-1,dp(40)));
            Runnable update=()->{boolean ramp=gradual[side].isChecked();start[side].setVisibility(ramp?View.VISIBLE:View.GONE);startLabel.setVisibility(ramp?View.VISIBLE:View.GONE);startLabel.setText(getString(R.string.munajat_start_volume)+"  "+CalendarUtils.fa(start[side].getProgress())+"٪");endLabel.setText(getString(ramp?R.string.munajat_end_volume:R.string.alarm_volume)+"  "+CalendarUtils.fa(end[side].getProgress())+"٪");};
            gradual[i].setOnCheckedChangeListener((b,checked)->update.run());SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){update.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}};start[i].setOnSeekBarChangeListener(listener);end[i].setOnSeekBarChangeListener(listener);update.run();
            body.setVisibility(v.enabled?View.VISIBLE:View.GONE);
            enabled[i].setOnCheckedChangeListener((b,checked)->{AppMotion.expand(root);body.setVisibility(checked?View.VISIBLE:View.GONE);});
        }
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button close=button(getString(R.string.runtime_text_0003));close.setOnClickListener(v->finish());actions.addView(close,new LinearLayout.LayoutParams(0,dp(54),1f));
        Button save=button(getString(R.string.munajat_save));save.setBackgroundResource(R.drawable.bg_teal_button);save.setTextColor(-1);save.setTag("munajat_save");save.setOnClickListener(v->save());
        LinearLayout.LayoutParams saveParams=new LinearLayout.LayoutParams(0,dp(54),1f);saveParams.setMarginStart(dp(8));actions.addView(save,saveParams);
        LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,-2);ap.topMargin=dp(4);root.addView(actions,ap);
        for(int i=0;i<2;i++)loadDuration(i);
        setContentView(scroll);AppSettings.applyFullscreenInsets(scroll);
    }
    /** Reads the length of the chosen audio in the background and refreshes the minutes rule. */
    private void loadDuration(int side){
        String uri=values[side].uri;android.content.Context app=getApplicationContext();
        new Thread(()->{long length=MunajatConfig.durationOf(app,uri);runOnUiThread(()->{
            if(isFinishing() || isDestroyed() || !uri.equals(values[side].uri))return;durations[side]=length;
            if(side==0){normalizeMinutes();renderMinutesHint();}
        });},"munajat-duration").start();
    }
    private int readMinutes(){try{return Integer.parseInt(latin(minutes.getText().toString().trim()));}catch(Exception e){return 0;}}
    private static String latin(String v){StringBuilder b=new StringBuilder();for(char ch:v.toCharArray()){if(ch>='\u06F0'&&ch<='\u06F9')ch=(char)('0'+ch-'\u06F0');else if(ch>='\u0660'&&ch<='\u0669')ch=(char)('0'+ch-'\u0660');b.append(ch);}return b.toString();}
    /** Empty or invalid -> 0 (play right before the adhan). Otherwise it must cover the whole audio. */
    private void normalizeMinutes(){
        if(minutes==null)return;int value=readMinutes();int min=MunajatConfig.minimumMinutes(durations[0]);
        if(value<0 || value>1440 || (value>0 && durations[0]>0 && value<min)){
            if(value>0)minutes.setError(getString(R.string.munajat_minutes_too_short,CalendarUtils.fa(min)));
            value=0;
        }
        String text=Integer.toString(value);if(!text.equals(minutes.getText().toString()))minutes.setText(text);
    }
    private void renderMinutesHint(){
        if(minutesHint==null)return;long d=durations[0];
        if(d<=0){minutesHint.setText(getString(R.string.munajat_minutes_zero));return;}
        long sec=(d+999)/1000;String length=CalendarUtils.fa(String.format(java.util.Locale.US,"%d:%02d",sec/60,sec%60));
        minutesHint.setText(getString(R.string.munajat_minutes_rule,length,CalendarUtils.fa(MunajatConfig.minimumMinutes(d))));
    }
    /** Tapping outside a field closes the keyboard. */
    @Override public boolean dispatchTouchEvent(MotionEvent event){
        if(event.getAction()==MotionEvent.ACTION_DOWN){View focus=getCurrentFocus();
            if(focus instanceof EditText){android.graphics.Rect r=new android.graphics.Rect();focus.getGlobalVisibleRect(r);
                if(!r.contains((int)event.getRawX(),(int)event.getRawY())){focus.clearFocus();
                    android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
                    if(imm!=null)imm.hideSoftInputFromWindow(focus.getWindowToken(),0);}}}
        return super.dispatchTouchEvent(event);
    }
    private void collect(){for(int i=0;i<2;i++){values[i].enabled=enabled[i].isChecked();values[i].gradual=gradual[i].isChecked();values[i].start=start[i].getProgress();values[i].end=end[i].getProgress();}}
    private void save(){
        collect();normalizeMinutes();values[0].minutes=readMinutes();
        for(int i=0;i<2;i++)values[i].duration=durations[i];
        for(int i=0;i<2;i++)if(values[i].enabled && !values[i].playable()){sounds[i].setError(getString(R.string.munajat_choose));return;}
        android.content.SharedPreferences.Editor editor=MunajatConfig.prefs(this).edit();
        for(int prayer=0;prayer<=4;prayer++)if(type==899 || type==prayer){values[0].write(editor,prayer,true);values[1].write(editor,prayer,false);}editor.apply();
        AdhanScheduler.rescheduleAll(this);AdhanSoundService.stopMunajatIfDisabled(this);setResult(RESULT_OK);finish();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request<920 || request>921 || result!=RESULT_OK || data==null)return;int side=request-920;String uri=data.getStringExtra(SoundPickerActivity.EXTRA_URI);if(uri==null)return;values[side].uri=uri;sounds[side].setText("🎵  "+SoundLibrary.munajatName(this,uri));durations[side]=0;loadDuration(side);sounds[side].setError(null);}
    @Override protected void onSaveInstanceState(Bundle state){collect();for(int i=0;i<2;i++){state.putString("uri"+i,values[i].uri);state.putBoolean("enabled"+i,values[i].enabled);state.putBoolean("gradual"+i,values[i].gradual);state.putInt("start"+i,values[i].start);state.putInt("end"+i,values[i].end);}state.putInt("minutes",readMinutes());super.onSaveInstanceState(state);}
    private SeekBar slider(int value){SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(value);bar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);return bar;}
    private Switch toggle(String title,boolean value){Switch sw=new Switch(this);sw.setText(title);sw.setTextColor(AppSettings.textPrimary(this));sw.setMinHeight(dp(52));sw.setChecked(value);return sw;}
    private TextView text(String value,int size){TextView tv=new TextView(this);tv.setText(value);tv.setTextSize(size);tv.setTextColor(AppSettings.textPrimary(this));return tv;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(AppSettings.primaryColor(this));b.setBackgroundResource(R.drawable.bg_soft_button);return b;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
