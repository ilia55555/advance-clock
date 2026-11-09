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
    @Override protected void onCreate(Bundle state){
        AppSettings.applyTheme(this);super.onCreate(state);type=getIntent().getIntExtra("adhanType",AdhanScheduler.FAJR);
        if(type!=899 && (type<0 || type>4)){finish();return;}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setLayoutDirection(AppSettings.layoutDirection(this));root.setPadding(dp(16),dp(16),dp(16),dp(24));root.setBackgroundColor(AppSettings.background(this));scroll.addView(root);
        TextView title=text(getString(R.string.munajat_title)+" • "+(type==899?getString(R.string.munajat_all):AdhanScheduler.title(type)),22);root.addView(title);
        TextView hint=text(getString(R.string.munajat_description),13);hint.setPadding(0,dp(8),0,dp(12));root.addView(hint);
        for(int i=0;i<2;i++){
            final int side=i;MunajatConfig v=MunajatConfig.load(this,type==899?0:type,i==0);values[i]=v;
            if(state!=null){v.uri=state.getString("uri"+i,v.uri);v.enabled=state.getBoolean("enabled"+i,v.enabled);v.gradual=state.getBoolean("gradual"+i,v.gradual);v.start=state.getInt("start"+i,v.start);v.end=state.getInt("end"+i,v.end);v.minutes=state.getInt("minutes",v.minutes);}
            LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setBackgroundResource(R.drawable.bg_card);card.setPadding(dp(14),dp(12),dp(14),dp(12));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.bottomMargin=dp(12);root.addView(card,cp);
            enabled[i]=toggle(getString(i==0?R.string.munajat_before:R.string.munajat_after),v.enabled);enabled[i].setTag(i==0?"munajat_before_enabled":"munajat_after_enabled");card.addView(enabled[i]);
            if(i==0){card.addView(text(getString(R.string.munajat_minutes),13));minutes=new EditText(this);minutes.setId(View.generateViewId());minutes.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);minutes.setSingleLine(true);minutes.setText(Integer.toString(v.minutes));minutes.setTextColor(AppSettings.textPrimary(this));minutes.setBackgroundResource(R.drawable.bg_field);minutes.setPadding(dp(12),0,dp(12),0);minutes.setTag("munajat_minutes");card.addView(minutes,new LinearLayout.LayoutParams(-1,dp(52)));}
            else card.addView(text(getString(R.string.munajat_after_hint),13));
            sounds[i]=button(SoundLibrary.munajatName(this,v.uri));sounds[i].setTag("munajat_sound_"+i);sounds[i].setOnClickListener(view->startActivityForResult(new Intent(this,SoundPickerActivity.class).putExtra(SoundPickerActivity.EXTRA_MUNAJAT,true).putExtra("previewVolume",gradual[side].isChecked()?start[side].getProgress():end[side].getProgress()),920+side));card.addView(sounds[i],new LinearLayout.LayoutParams(-1,dp(56)));
            gradual[i]=toggle(getString(R.string.munajat_gradual),v.gradual);card.addView(gradual[i]);
            TextView startLabel=text("",13);card.addView(startLabel);start[i]=slider(v.start);start[i].setTag("munajat_start_"+i);card.addView(start[i],new LinearLayout.LayoutParams(-1,dp(48)));
            TextView endLabel=text("",13);card.addView(endLabel);end[i]=slider(v.end);end[i].setTag("munajat_end_"+i);card.addView(end[i],new LinearLayout.LayoutParams(-1,dp(48)));
            Runnable update=()->{boolean ramp=gradual[side].isChecked();start[side].setVisibility(ramp?View.VISIBLE:View.GONE);startLabel.setVisibility(ramp?View.VISIBLE:View.GONE);startLabel.setText(getString(R.string.munajat_start_volume)+" "+start[side].getProgress()+"٪");endLabel.setText(getString(ramp?R.string.munajat_end_volume:R.string.alarm_volume)+" "+end[side].getProgress()+"٪");};
            gradual[i].setOnCheckedChangeListener((b,checked)->update.run());SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar b,int p,boolean user){update.run();}public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}};start[i].setOnSeekBarChangeListener(listener);end[i].setOnSeekBarChangeListener(listener);update.run();
        }
        Button save=button(getString(R.string.munajat_save));save.setBackgroundResource(R.drawable.bg_teal_button);save.setTextColor(-1);save.setTag("munajat_save");save.setOnClickListener(v->save());root.addView(save,new LinearLayout.LayoutParams(-1,dp(54)));
        Button close=button(getString(R.string.runtime_text_0003));close.setOnClickListener(v->finish());root.addView(close,new LinearLayout.LayoutParams(-1,dp(54)));
        setContentView(scroll);AppSettings.applyFullscreenInsets(scroll);
    }
    private void collect(){for(int i=0;i<2;i++){values[i].enabled=enabled[i].isChecked();values[i].gradual=gradual[i].isChecked();values[i].start=start[i].getProgress();values[i].end=end[i].getProgress();}}
    private void save(){
        collect();int advance;try{advance=Integer.parseInt(minutes.getText().toString().trim());}catch(Exception e){advance=0;}
        if(values[0].enabled && (advance<1 || advance>1440)){minutes.setError(getString(R.string.munajat_invalid_minutes));minutes.requestFocus();return;}
        if(advance>=1 && advance<=1440)values[0].minutes=advance;
        for(int i=0;i<2;i++)if(values[i].enabled && !values[i].playable()){sounds[i].setError(getString(R.string.munajat_choose));return;}
        android.content.SharedPreferences.Editor editor=MunajatConfig.prefs(this).edit();
        for(int prayer=0;prayer<=4;prayer++)if(type==899 || type==prayer){values[0].write(editor,prayer,true);values[1].write(editor,prayer,false);}editor.apply();
        AdhanScheduler.rescheduleAll(this);AdhanSoundService.stopMunajatIfDisabled(this);setResult(RESULT_OK);finish();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request<920 || request>921 || result!=RESULT_OK || data==null)return;int side=request-920;String uri=data.getStringExtra(SoundPickerActivity.EXTRA_URI);if(uri==null)return;values[side].uri=uri;sounds[side].setText(SoundLibrary.munajatName(this,uri));sounds[side].setError(null);}
    @Override protected void onSaveInstanceState(Bundle state){collect();for(int i=0;i<2;i++){state.putString("uri"+i,values[i].uri);state.putBoolean("enabled"+i,values[i].enabled);state.putBoolean("gradual"+i,values[i].gradual);state.putInt("start"+i,values[i].start);state.putInt("end"+i,values[i].end);}try{state.putInt("minutes",Integer.parseInt(minutes.getText().toString()));}catch(Exception ignored){}super.onSaveInstanceState(state);}
    private SeekBar slider(int value){SeekBar bar=new SeekBar(this);bar.setMax(100);bar.setProgress(value);bar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);return bar;}
    private Switch toggle(String title,boolean value){Switch sw=new Switch(this);sw.setText(title);sw.setTextColor(AppSettings.textPrimary(this));sw.setMinHeight(dp(52));sw.setChecked(value);return sw;}
    private TextView text(String value,int size){TextView tv=new TextView(this);tv.setText(value);tv.setTextSize(size);tv.setTextColor(AppSettings.textPrimary(this));return tv;}
    private Button button(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setTextColor(AppSettings.primaryColor(this));b.setBackgroundResource(R.drawable.bg_soft_button);return b;}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
