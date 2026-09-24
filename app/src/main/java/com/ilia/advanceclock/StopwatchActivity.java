package com.ilia.advanceclock;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public final class StopwatchActivity extends Activity {
    private static final String PREFS = "time_tools";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Long> laps = new ArrayList<>();
    private TextView timeView;
    private Button startButton, lapButton;
    private LinearLayout lapList;
    private Switch unlimitedSwitch;
    private View limitInputs;
    private EditText hoursInput, minutesInput, secondsInput, labelInput;
    private boolean running;
    private long accumulatedMillis, startedAtWall, limitMillis;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long elapsed = currentElapsed();
            if (limitMillis > 0L && elapsed >= limitMillis) {
                accumulatedMillis = limitMillis; running = false; startedAtWall = 0L;
                save(); updateControls(); renderTime(limitMillis); return;
            }
            renderTime(elapsed);
            if (running) handler.postDelayed(this, 31L);
        }
    };

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        super.onCreate(state);
        setContentView(R.layout.activity_stopwatch);
        timeView=findViewById(R.id.stopwatch_time); startButton=findViewById(R.id.stopwatch_start);
        lapButton=findViewById(R.id.stopwatch_lap); lapList=findViewById(R.id.lap_list);
        unlimitedSwitch=findViewById(R.id.stopwatch_unlimited); limitInputs=findViewById(R.id.stopwatch_limit_inputs);
        hoursInput=findViewById(R.id.stopwatch_limit_hours); minutesInput=findViewById(R.id.stopwatch_limit_minutes);
        secondsInput=findViewById(R.id.stopwatch_limit_seconds); labelInput=findViewById(R.id.stopwatch_label);
        restore(); populateLimitInputs();
        findViewById(R.id.back_button).setOnClickListener(v->finish());
        startButton.setOnClickListener(v->toggleRunning());
        findViewById(R.id.stopwatch_reset).setOnClickListener(v->reset());
        lapButton.setOnClickListener(v->addLap());
        unlimitedSwitch.setOnCheckedChangeListener((button, checked)-> limitInputs.setVisibility(checked?View.GONE:View.VISIBLE));
        renderLaps(); updateControls(); renderTime(currentElapsed());
    }

    private void toggleRunning() {
        if (running) {
            accumulatedMillis=currentElapsed(); running=false; startedAtWall=0L;
            ToolAlarmScheduler.cancel(this,ToolAlarmScheduler.STOPWATCH); handler.removeCallbacks(ticker);
        } else {
            if (accumulatedMillis==0L) {
                limitMillis=unlimitedSwitch.isChecked()?0L:readLimit();
                if(!unlimitedSwitch.isChecked()&&limitMillis<=0L){Toast.makeText(this,"حد نهایی بیشتر از صفر وارد کنید",Toast.LENGTH_SHORT).show();return;}
            }
            if(limitMillis>0L&&accumulatedMillis>=limitMillis){Toast.makeText(this,"ابتدا کرنومتر را صفر کنید",Toast.LENGTH_SHORT).show();return;}
            startedAtWall=System.currentTimeMillis(); running=true;
            if(limitMillis>0L){
                String label=labelInput.getText().toString().trim();
                if(label.isEmpty())label=ToolAlarmScheduler.defaultLabel(ToolAlarmScheduler.STOPWATCH);
                boolean scheduled=ToolAlarmScheduler.schedule(this,ToolAlarmScheduler.STOPWATCH,startedAtWall+(limitMillis-accumulatedMillis),label);
                if(!scheduled)Toast.makeText(this,"برای هشدار دقیق، مجوز آلارم دقیق را فعال کنید",Toast.LENGTH_LONG).show();
            }
            handler.post(ticker);
        }
        save(); updateControls();
    }

    private void reset(){running=false;accumulatedMillis=0L;startedAtWall=0L;limitMillis=0L;laps.clear();ToolAlarmScheduler.cancel(this,ToolAlarmScheduler.STOPWATCH);handler.removeCallbacks(ticker);save();renderTime(0);renderLaps();updateControls();}
    private void addLap(){long elapsed=currentElapsed();if(!running||elapsed==0)return;laps.add(0,elapsed);save();renderLaps();}
    private long currentElapsed(){return running?accumulatedMillis+Math.max(0,System.currentTimeMillis()-startedAtWall):accumulatedMillis;}
    private long readLimit(){return(number(hoursInput)*3600L+number(minutesInput)*60L+number(secondsInput))*1000L;}
    private static long number(EditText input){try{String s=input.getText().toString().trim();return s.isEmpty()?0:Long.parseLong(s);}catch(NumberFormatException e){return 0;}}

    private void updateControls(){boolean configurable=!running&&accumulatedMillis==0L;startButton.setText(running?"توقف":accumulatedMillis>0?"ادامه":"شروع");lapButton.setEnabled(running);unlimitedSwitch.setEnabled(configurable);limitInputs.setVisibility(configurable&&!unlimitedSwitch.isChecked()?View.VISIBLE:View.GONE);labelInput.setVisibility(configurable&&!unlimitedSwitch.isChecked()?View.VISIBLE:View.GONE);}
    private void populateLimitInputs(){if(limitMillis<=0)return;long total=limitMillis/1000;hoursInput.setText(String.valueOf(total/3600));minutesInput.setText(String.valueOf((total/60)%60));secondsInput.setText(String.valueOf(total%60));unlimitedSwitch.setChecked(false);}
    private void renderTime(long millis){timeView.setText(format(millis));}
    private void renderLaps(){lapList.removeAllViews();for(int i=0;i<laps.size();i++){TextView row=new TextView(this);row.setText(String.format(Locale.US,"دور %d     %s",laps.size()-i,format(laps.get(i))));row.setTextColor(AppSettings.textPrimary(this));row.setTextSize(17);row.setGravity(android.view.Gravity.CENTER);row.setPadding(12,18,12,18);lapList.addView(row);}}
    private static String format(long millis){long cs=millis/10;return String.format(Locale.US,"%02d:%02d:%02d.%02d",cs/360000,(cs/6000)%60,(cs/100)%60,cs%100);}
    private void save(){StringBuilder encoded=new StringBuilder();for(long lap:laps){if(encoded.length()>0)encoded.append(',');encoded.append(lap);}getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("stopwatch_running",running).putLong("stopwatch_accumulated",accumulatedMillis).putLong("stopwatch_started",startedAtWall).putLong("stopwatch_limit",limitMillis).putString("stopwatch_laps",encoded.toString()).apply();}
    private void restore(){SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE);running=p.getBoolean("stopwatch_running",false);accumulatedMillis=p.getLong("stopwatch_accumulated",0);startedAtWall=p.getLong("stopwatch_started",0);limitMillis=p.getLong("stopwatch_limit",0);laps.clear();String encoded=p.getString("stopwatch_laps","");if(!encoded.isEmpty())for(String value:encoded.split(","))try{laps.add(Long.parseLong(value));}catch(NumberFormatException ignored){}if(running&&limitMillis>0&&currentElapsed()>=limitMillis){running=false;accumulatedMillis=limitMillis;startedAtWall=0;save();}}
    @Override protected void onResume(){super.onResume();restore();renderLaps();updateControls();renderTime(currentElapsed());if(running)handler.post(ticker);}
    @Override protected void onPause(){handler.removeCallbacks(ticker);save();super.onPause();}
}
