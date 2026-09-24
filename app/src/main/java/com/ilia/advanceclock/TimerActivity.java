package com.ilia.advanceclock;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public final class TimerActivity extends Activity {
    private static final String PREFS = "time_tools";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timeView, summary;
    private View durationInputs, dateTimeInputs, modeButtons;
    private EditText hoursInput, minutesInput, secondsInput, labelInput;
    private Button startButton, dateButton, timeButton;
    private boolean running, dateTimeMode;
    private long remainingMillis, deadline, selectedTarget;
    private int calendarType;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long remaining = currentRemaining();
            renderTime(remaining);
            if (remaining <= 0L) completeLocally();
            else if (running) handler.postDelayed(this, Math.min(250L, remaining));
        }
    };

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        super.onCreate(state);
        setContentView(R.layout.activity_timer);
        timeView = findViewById(R.id.timer_time);
        summary = findViewById(R.id.timer_target_summary);
        durationInputs = findViewById(R.id.timer_inputs);
        dateTimeInputs = findViewById(R.id.timer_datetime_inputs);
        modeButtons = findViewById(R.id.timer_mode_buttons);
        hoursInput = findViewById(R.id.timer_hours);
        minutesInput = findViewById(R.id.timer_minutes);
        secondsInput = findViewById(R.id.timer_seconds);
        labelInput = findViewById(R.id.timer_label);
        startButton = findViewById(R.id.timer_start);
        dateButton = findViewById(R.id.timer_date);
        timeButton = findViewById(R.id.timer_clock);
        restore();
        if (selectedTarget <= 0L) selectedTarget = System.currentTimeMillis() + 5 * 60_000L;

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        findViewById(R.id.timer_mode_duration).setOnClickListener(v -> setMode(false));
        findViewById(R.id.timer_mode_datetime).setOnClickListener(v -> setMode(true));
        dateButton.setOnClickListener(v -> CalendarPickerDialog.showDate(this, selectedTarget,
                calendarType, (millis, type) -> {
                    Calendar picked = Calendar.getInstance(); picked.setTimeInMillis(millis);
                    Calendar old = Calendar.getInstance(); old.setTimeInMillis(selectedTarget);
                    picked.set(Calendar.HOUR_OF_DAY, old.get(Calendar.HOUR_OF_DAY));
                    picked.set(Calendar.MINUTE, old.get(Calendar.MINUTE)); picked.set(Calendar.SECOND, 0);
                    selectedTarget = picked.getTimeInMillis(); calendarType = type; renderSelection();
                }));
        timeButton.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(selectedTarget);
            new TimePickerDialog(this, (view, hour, minute) -> {
                c.set(Calendar.HOUR_OF_DAY, hour); c.set(Calendar.MINUTE, minute); c.set(Calendar.SECOND, 0);
                selectedTarget = c.getTimeInMillis(); renderSelection();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });
        startButton.setOnClickListener(v -> toggleTimer());
        findViewById(R.id.timer_reset).setOnClickListener(v -> reset());
        updateControls();
        renderTime(running || remainingMillis > 0 ? currentRemaining() : readDuration());
        renderSelection();
    }

    private void setMode(boolean dateMode) {
        if (running || remainingMillis > 0) return;
        dateTimeMode = dateMode;
        updateControls(); renderSelection(); save();
    }

    private void toggleTimer() {
        if (running) {
            remainingMillis = currentRemaining(); running = false;
            ToolAlarmScheduler.cancel(this, ToolAlarmScheduler.TIMER);
            handler.removeCallbacks(ticker); save(); updateControls(); return;
        }
        long value = remainingMillis > 0 ? remainingMillis
                : dateTimeMode ? selectedTarget - System.currentTimeMillis() : readDuration();
        if (value <= 0L) {
            Toast.makeText(this, dateTimeMode ? "تاریخ و ساعت آینده را انتخاب کنید" : "یک زمان بیشتر از صفر وارد کنید", Toast.LENGTH_SHORT).show();
            return;
        }
        deadline = System.currentTimeMillis() + value;
        remainingMillis = value; running = true;
        String label = labelInput.getText().toString().trim();
        if (label.isEmpty()) label = ToolAlarmScheduler.defaultLabel(ToolAlarmScheduler.TIMER);
        boolean scheduled = ToolAlarmScheduler.schedule(this, ToolAlarmScheduler.TIMER, deadline, label);
        if (!scheduled) Toast.makeText(this, "برای هشدار دقیق، مجوز آلارم دقیق را فعال کنید", Toast.LENGTH_LONG).show();
        save(); updateControls(); handler.post(ticker);
    }

    private void reset() {
        running = false; remainingMillis = 0L; deadline = 0L;
        ToolAlarmScheduler.cancel(this, ToolAlarmScheduler.TIMER);
        handler.removeCallbacks(ticker); save(); updateControls();
        renderTime(dateTimeMode ? Math.max(0L, selectedTarget - System.currentTimeMillis()) : readDuration());
    }

    private void completeLocally() {
        running = false; remainingMillis = 0L; deadline = 0L;
        handler.removeCallbacks(ticker); save(); updateControls(); renderTime(0L);
    }

    private long currentRemaining() { return running ? Math.max(0L, deadline - System.currentTimeMillis()) : remainingMillis; }
    private long readDuration() { return (number(hoursInput) * 3600L + number(minutesInput) * 60L + number(secondsInput)) * 1000L; }
    private static long number(EditText input) { try { String s=input.getText().toString().trim(); return s.isEmpty()?0:Long.parseLong(s); } catch (NumberFormatException e) { return 0; } }

    private void updateControls() {
        boolean configured = !running && remainingMillis == 0L;
        modeButtons.setVisibility(configured ? View.VISIBLE : View.GONE);
        durationInputs.setVisibility(configured && !dateTimeMode ? View.VISIBLE : View.GONE);
        dateTimeInputs.setVisibility(configured && dateTimeMode ? View.VISIBLE : View.GONE);
        labelInput.setVisibility(configured ? View.VISIBLE : View.GONE);
        startButton.setText(running ? "توقف" : remainingMillis > 0 ? "ادامه" : "شروع");
    }

    private void renderSelection() {
        dateButton.setText(CalendarUtils.formatDate(selectedTarget, calendarType));
        Calendar c=Calendar.getInstance(); c.setTimeInMillis(selectedTarget);
        timeButton.setText(String.format(Locale.US,"%02d:%02d",c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)));
        summary.setText(running ? "هشدار در " + CalendarUtils.formatDate(deadline, calendarType) + "، " + String.format(Locale.US,"%tR",deadline)
                : dateTimeMode ? "شمارش معکوس تا تاریخ و ساعت انتخاب‌شده" : "شمارش معکوس بر اساس مدت‌زمان");
    }

    private void renderTime(long millis) { long total=(millis+999)/1000; timeView.setText(String.format(Locale.US,"%02d:%02d:%02d",total/3600,(total/60)%60,total%60)); }
    private void save() { getSharedPreferences(PREFS,MODE_PRIVATE).edit().putBoolean("timer_running",running).putBoolean("timer_date_mode",dateTimeMode).putLong("timer_remaining",remainingMillis).putLong("timer_end",deadline).putLong("timer_target",selectedTarget).putInt("timer_calendar",calendarType).apply(); }
    private void restore() { SharedPreferences p=getSharedPreferences(PREFS,MODE_PRIVATE); running=p.getBoolean("timer_running",false); dateTimeMode=p.getBoolean("timer_date_mode",false); remainingMillis=p.getLong("timer_remaining",0); deadline=p.getLong("timer_end",0); selectedTarget=p.getLong("timer_target",0); calendarType=p.getInt("timer_calendar",AppSettings.defaultCalendar(this)); if(running&&deadline<=System.currentTimeMillis()){running=false;remainingMillis=0;deadline=0;save();} }
    @Override protected void onResume(){super.onResume();restore();updateControls();renderSelection();renderTime(running||remainingMillis>0?currentRemaining():dateTimeMode?Math.max(0,selectedTarget-System.currentTimeMillis()):readDuration());if(running)handler.post(ticker);}
    @Override protected void onPause(){handler.removeCallbacks(ticker);save();super.onPause();}
}
