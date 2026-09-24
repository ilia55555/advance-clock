package com.ilia.advanceclock;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

final class StopwatchPanelController {
    private final Activity host;
    private final View root;
    private static final String PREFS = "time_tools";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Long> laps = new ArrayList<>();
    private TextView timeView;
    private Button startButton, lapButton;
    private LinearLayout lapList;
    private View limitInputs, dateTimeInputs, modeButtons;
    private EditText hoursInput, minutesInput, secondsInput, labelInput;
    private Button unlimitedModeButton, durationModeButton, dateTimeModeButton;
    private Button dateButton, timeButton;
    private boolean running;
    private int mode;
    private int calendarType;
    private long accumulatedMillis, startedAtWall, limitMillis, selectedTarget;

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

    StopwatchPanelController(Activity host, View root) {
        this.host = host;
        this.root = root;
        timeView = root.findViewById(R.id.stopwatch_time);
        startButton = root.findViewById(R.id.stopwatch_start);
        lapButton = root.findViewById(R.id.stopwatch_lap);
        lapList = root.findViewById(R.id.lap_list);
        modeButtons = root.findViewById(R.id.stopwatch_mode_buttons);
        unlimitedModeButton = root.findViewById(R.id.stopwatch_unlimited);
        durationModeButton = root.findViewById(R.id.stopwatch_mode_duration);
        dateTimeModeButton = root.findViewById(R.id.stopwatch_mode_datetime);
        limitInputs = root.findViewById(R.id.stopwatch_limit_inputs);
        dateTimeInputs = root.findViewById(R.id.stopwatch_datetime_inputs);
        dateButton = root.findViewById(R.id.stopwatch_date);
        timeButton = root.findViewById(R.id.stopwatch_clock);
        hoursInput = root.findViewById(R.id.stopwatch_limit_hours);
        minutesInput = root.findViewById(R.id.stopwatch_limit_minutes);
        secondsInput = root.findViewById(R.id.stopwatch_limit_seconds);
        labelInput = root.findViewById(R.id.stopwatch_label);
        restore();
        populateLimitInputs();
        View back = root.findViewById(R.id.back_button);
        ((View) back.getParent()).setVisibility(View.GONE);
        startButton.setOnClickListener(v -> toggleRunning());
        root.findViewById(R.id.stopwatch_reset).setOnClickListener(v -> reset());
        lapButton.setOnClickListener(v -> addLap());
        unlimitedModeButton.setOnClickListener(v -> setMode(0));
        durationModeButton.setOnClickListener(v -> setMode(1));
        dateTimeModeButton.setOnClickListener(v -> setMode(2));
        dateButton.setOnClickListener(v -> CalendarPickerDialog.showDate(
                host, selectedTarget, calendarType, (millis, type) -> {
                    Calendar picked = Calendar.getInstance();
                    picked.setTimeInMillis(millis);
                    Calendar old = Calendar.getInstance();
                    old.setTimeInMillis(selectedTarget);
                    picked.set(Calendar.HOUR_OF_DAY, old.get(Calendar.HOUR_OF_DAY));
                    picked.set(Calendar.MINUTE, old.get(Calendar.MINUTE));
                    picked.set(Calendar.SECOND, 0);
                    selectedTarget = picked.getTimeInMillis();
                    calendarType = type;
                    renderTarget();
                    save();
                }));
        timeButton.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedTarget);
            new TimePickerDialog(host, (view, hour, minute) -> {
                c.set(Calendar.HOUR_OF_DAY, hour);
                c.set(Calendar.MINUTE, minute);
                c.set(Calendar.SECOND, 0);
                selectedTarget = c.getTimeInMillis();
                renderTarget();
                save();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });
        if (selectedTarget <= 0L) selectedTarget = System.currentTimeMillis() + 60 * 60_000L;
        renderTarget();
        renderLaps();
        updateControls();
        renderTime(currentElapsed());
    }

    private void toggleRunning() {
        if (running) {
            accumulatedMillis=currentElapsed(); running=false; startedAtWall=0L;
            ToolAlarmScheduler.cancel(host,ToolAlarmScheduler.STOPWATCH); handler.removeCallbacks(ticker);
        } else {
            if (accumulatedMillis == 0L) {
                if (mode == 0) {
                    limitMillis = 0L;
                } else if (mode == 1) {
                    limitMillis = readLimit();
                } else {
                    limitMillis = selectedTarget - System.currentTimeMillis();
                }
                if (mode != 0 && limitMillis <= 0L) {
                    Toast.makeText(host, mode == 2
                            ? "تاریخ و ساعت آینده را انتخاب کنید"
                            : "مدت‌زمان بیشتر از صفر وارد کنید", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if(limitMillis>0L&&accumulatedMillis>=limitMillis){Toast.makeText(host,"ابتدا کرنومتر را صفر کنید",Toast.LENGTH_SHORT).show();return;}
            startedAtWall=System.currentTimeMillis();
            if (mode == 2 && selectedTarget <= startedAtWall) {
                Toast.makeText(host, "تاریخ و ساعت پایان گذشته است", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mode == 2) limitMillis = accumulatedMillis + selectedTarget - startedAtWall;
            running=true;
            if(limitMillis>0L){
                String label=labelInput.getText().toString().trim();
                if(label.isEmpty())label=ToolAlarmScheduler.defaultLabel(ToolAlarmScheduler.STOPWATCH);
                boolean scheduled=ToolAlarmScheduler.schedule(host,ToolAlarmScheduler.STOPWATCH,startedAtWall+(limitMillis-accumulatedMillis),label);
                if(!scheduled)Toast.makeText(host,"برای هشدار دقیق، مجوز آلارم دقیق را فعال کنید",Toast.LENGTH_LONG).show();
            }
            handler.post(ticker);
        }
        save(); updateControls();
    }

    private void setMode(int value) {
        if (running || accumulatedMillis > 0L) return;
        mode = Math.max(0, Math.min(2, value));
        updateControls();
        save();
    }

    private void renderTarget() {
        dateButton.setText(CalendarUtils.formatDate(selectedTarget, calendarType));
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(selectedTarget);
        timeButton.setText(String.format(Locale.US, "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
    }

    private void reset(){running=false;accumulatedMillis=0L;startedAtWall=0L;limitMillis=0L;laps.clear();ToolAlarmScheduler.cancel(host,ToolAlarmScheduler.STOPWATCH);handler.removeCallbacks(ticker);save();renderTime(0);renderLaps();updateControls();}
    private void addLap(){long elapsed=currentElapsed();if(!running||elapsed==0)return;laps.add(0,elapsed);save();renderLaps();}
    private long currentElapsed(){return running?accumulatedMillis+Math.max(0,System.currentTimeMillis()-startedAtWall):accumulatedMillis;}
    private long readLimit(){return(number(hoursInput)*3600L+number(minutesInput)*60L+number(secondsInput))*1000L;}
    private static long number(EditText input){try{String s=input.getText().toString().trim();return s.isEmpty()?0:Long.parseLong(s);}catch(NumberFormatException e){return 0;}}

    private void updateControls() {
        boolean configurable = !running && accumulatedMillis == 0L;
        startButton.setText(running ? "توقف" : accumulatedMillis > 0 ? "ادامه" : "شروع");
        lapButton.setEnabled(running);
        modeButtons.setVisibility(configurable ? View.VISIBLE : View.GONE);
        limitInputs.setVisibility(configurable && mode == 1 ? View.VISIBLE : View.GONE);
        dateTimeInputs.setVisibility(configurable && mode == 2 ? View.VISIBLE : View.GONE);
        labelInput.setVisibility(configurable && mode != 0 ? View.VISIBLE : View.GONE);
        styleMode(unlimitedModeButton, mode == 0);
        styleMode(durationModeButton, mode == 1);
        styleMode(dateTimeModeButton, mode == 2);
    }

    private void styleMode(Button button, boolean active) {
        button.setBackgroundResource(active ? R.drawable.bg_teal_button : R.drawable.bg_field);
        button.setTextColor(active ? 0xFFFFFFFF : AppSettings.primaryColor(host));
    }

    private void populateLimitInputs(){if(limitMillis<=0 || mode != 1)return;long total=limitMillis/1000;hoursInput.setText(String.valueOf(total/3600));minutesInput.setText(String.valueOf((total/60)%60));secondsInput.setText(String.valueOf(total%60));}
    private void renderTime(long millis){timeView.setText(format(millis));}
    private void renderLaps(){lapList.removeAllViews();for(int i=0;i<laps.size();i++){TextView row=new TextView(host);row.setText(String.format(Locale.US,"دور %d     %s",laps.size()-i,format(laps.get(i))));row.setTextColor(AppSettings.textPrimary(host));row.setTextSize(17);row.setGravity(android.view.Gravity.CENTER);row.setPadding(12,18,12,18);lapList.addView(row);}}
    private static String format(long millis){long cs=millis/10;return String.format(Locale.US,"%02d:%02d:%02d.%02d",cs/360000,(cs/6000)%60,(cs/100)%60,cs%100);}
    private void save(){StringBuilder encoded=new StringBuilder();for(long lap:laps){if(!encoded.isEmpty())encoded.append(',');encoded.append(lap);}host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit().putBoolean("stopwatch_running",running).putInt("stopwatch_mode",mode).putInt("stopwatch_calendar",calendarType).putLong("stopwatch_target",selectedTarget).putLong("stopwatch_accumulated",accumulatedMillis).putLong("stopwatch_started",startedAtWall).putLong("stopwatch_limit",limitMillis).putString("stopwatch_laps",encoded.toString()).apply();}
    private void restore(){SharedPreferences p=host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);running=p.getBoolean("stopwatch_running",false);mode=p.getInt("stopwatch_mode",0);calendarType=p.getInt("stopwatch_calendar",AppSettings.defaultCalendar(host));selectedTarget=p.getLong("stopwatch_target",0);accumulatedMillis=p.getLong("stopwatch_accumulated",0);startedAtWall=p.getLong("stopwatch_started",0);limitMillis=p.getLong("stopwatch_limit",0);laps.clear();String encoded=p.getString("stopwatch_laps","");if(!encoded.isEmpty())for(String value:encoded.split(","))try{laps.add(Long.parseLong(value));}catch(NumberFormatException ignored){}if(running&&limitMillis>0&&currentElapsed()>=limitMillis){running=false;accumulatedMillis=limitMillis;startedAtWall=0;save();}}
    void onResume(){restore();renderTarget();renderLaps();updateControls();renderTime(currentElapsed());if(running)handler.post(ticker);}
    void onPause(){handler.removeCallbacks(ticker);save();}
}
