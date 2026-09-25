package com.ilia.advanceclock;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

final class TimerPanelController {
    private final Activity host;
    private final View root;
    private static final String PREFS = "time_tools";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timeView, summary;
    private View durationInputs, dateTimeInputs, modeButtons;
    private EditText hoursInput, minutesInput, secondsInput, labelInput;
    private Button startButton, dateButton, timeButton, durationModeButton, dateTimeModeButton;
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

    TimerPanelController(Activity host, View root) {
        this.host = host;
        this.root = root;
        timeView = root.findViewById(R.id.timer_time);
        summary = root.findViewById(R.id.timer_target_summary);
        durationInputs = root.findViewById(R.id.timer_inputs);
        dateTimeInputs = root.findViewById(R.id.timer_datetime_inputs);
        modeButtons = root.findViewById(R.id.timer_mode_buttons);
        hoursInput = root.findViewById(R.id.timer_hours);
        minutesInput = root.findViewById(R.id.timer_minutes);
        secondsInput = root.findViewById(R.id.timer_seconds);
        labelInput = root.findViewById(R.id.timer_label);
        startButton = root.findViewById(R.id.timer_start);
        dateButton = root.findViewById(R.id.timer_date);
        timeButton = root.findViewById(R.id.timer_clock);
        durationModeButton = root.findViewById(R.id.timer_mode_duration);
        dateTimeModeButton = root.findViewById(R.id.timer_mode_datetime);
        restore();
        if (selectedTarget <= 0L) {
            selectedTarget = System.currentTimeMillis() + 5 * 60_000L;
        }
        View back = root.findViewById(R.id.back_button);
        ((View) back.getParent()).setVisibility(View.GONE);
        durationModeButton.setOnClickListener(v -> setMode(false));
        dateTimeModeButton.setOnClickListener(v -> setMode(true));
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
                    renderSelection();
                }));
        timeButton.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedTarget);
            new TimePickerDialog(host, (view, hour, minute) -> {
                c.set(Calendar.HOUR_OF_DAY, hour);
                c.set(Calendar.MINUTE, minute);
                c.set(Calendar.SECOND, 0);
                selectedTarget = c.getTimeInMillis();
                renderSelection();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        });
        startButton.setOnClickListener(v -> toggleTimer());
        root.findViewById(R.id.timer_reset).setOnClickListener(v -> reset());
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
            ToolAlarmScheduler.cancel(host, ToolAlarmScheduler.TIMER);
            handler.removeCallbacks(ticker); save(); updateControls(); return;
        }
        long value = remainingMillis > 0 ? remainingMillis
                : dateTimeMode ? selectedTarget - System.currentTimeMillis() : readDuration();
        if (value <= 0L) {
            Toast.makeText(host, dateTimeMode ? "تاریخ و ساعت آینده را انتخاب کنید" : "یک زمان بیشتر از صفر وارد کنید", Toast.LENGTH_SHORT).show();
            return;
        }
        deadline = System.currentTimeMillis() + value;
        remainingMillis = value; running = true;
        String label = labelInput.getText().toString().trim();
        if (label.isEmpty()) label = ToolAlarmScheduler.defaultLabel(ToolAlarmScheduler.TIMER);
        boolean scheduled = ToolAlarmScheduler.schedule(host, ToolAlarmScheduler.TIMER, deadline, label);
        if (!scheduled) Toast.makeText(host, "برای هشدار دقیق، مجوز آلارم دقیق را فعال کنید", Toast.LENGTH_LONG).show();
        save(); updateControls(); handler.post(ticker);
    }

    private void reset() {
        running = false; remainingMillis = 0L; deadline = 0L;
        ToolAlarmScheduler.cancel(host, ToolAlarmScheduler.TIMER);
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
        durationModeButton.setBackgroundResource(
                dateTimeMode ? R.drawable.bg_field : R.drawable.bg_teal_button);
        dateTimeModeButton.setBackgroundResource(
                dateTimeMode ? R.drawable.bg_teal_button : R.drawable.bg_field);
        durationModeButton.setTextColor(
                dateTimeMode ? AppSettings.primaryColor(host) : 0xFFFFFFFF);
        dateTimeModeButton.setTextColor(
                dateTimeMode ? 0xFFFFFFFF : AppSettings.primaryColor(host));
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
    private void save() { host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit().putBoolean("timer_running",running).putBoolean("timer_date_mode",dateTimeMode).putLong("timer_remaining",remainingMillis).putLong("timer_end",deadline).putLong("timer_target",selectedTarget).putInt("timer_calendar",calendarType).apply(); }
    private void restore() { SharedPreferences p=host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE); running=p.getBoolean("timer_running",false); dateTimeMode=p.getBoolean("timer_date_mode",false); remainingMillis=p.getLong("timer_remaining",0); deadline=p.getLong("timer_end",0); selectedTarget=p.getLong("timer_target",0); calendarType=p.getInt("timer_calendar",AppSettings.defaultCalendar(host)); if(running&&deadline<=System.currentTimeMillis()){running=false;remainingMillis=0;deadline=0;save();} }
    void onResume(){restore();updateControls();renderSelection();renderTime(running||remainingMillis>0?currentRemaining():dateTimeMode?Math.max(0,selectedTarget-System.currentTimeMillis()):readDuration());if(running)handler.post(ticker);}
    void onPause(){handler.removeCallbacks(ticker);save();}
}
