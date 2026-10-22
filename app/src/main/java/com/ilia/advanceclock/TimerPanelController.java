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
    private TimeToolPresets presets;
    private DurationInputs durationFields;
    private static final String PREFS = "time_tools";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView timeView, summary;
    private View durationInputs, dateTimeInputs, modeButtons;
    private EditText hoursInput, minutesInput, secondsInput, labelInput;
    private Button startButton, dateButton, timeButton, durationModeButton, dateTimeModeButton;
    private boolean running, dateTimeMode;
    private boolean resumed;
    private boolean active;
    private long remainingMillis, deadline, selectedTarget, initialDurationMillis;
    private int calendarType;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            long remaining = currentRemaining();
            renderTime(remaining);
            if (remaining <= 0L) {
                completeLocally();
            } else if (running && resumed && active) {
                long visibleSeconds = (remaining + 999L) / 1000L;
                long delay = remaining - (visibleSeconds - 1L) * 1000L;
                handler.postDelayed(
                        this,
                        Math.max(
                                50L,
                                Math.min(delay, remaining)));
            }
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
        durationFields = new DurationInputs(hoursInput, minutesInput, secondsInput);
        labelInput = root.findViewById(R.id.timer_label);
        startButton = root.findViewById(R.id.timer_start);
        dateButton = root.findViewById(R.id.timer_date);
        timeButton = root.findViewById(R.id.timer_clock);
        durationModeButton = root.findViewById(R.id.timer_mode_duration);
        dateTimeModeButton = root.findViewById(R.id.timer_mode_datetime);
        restoreDraft();
        presets = new TimeToolPresets(host, labelInput, ToolAlarmScheduler.TIMER,
                new TimeToolPresets.Editor() {
                    public org.json.JSONObject capture() throws org.json.JSONException { return capturePreset(); }
                    public void load(org.json.JSONObject config, boolean run) throws org.json.JSONException { loadPreset(config, run); }
                    public boolean busy() { return running || remainingMillis > 0; }
                });
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
        android.text.TextWatcher durationWatcher = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!running && remainingMillis == 0 && !dateTimeMode) renderTime(readDuration());
            }
            public void afterTextChanged(android.text.Editable s) {}
        };
        for (EditText field : new EditText[]{hoursInput, minutesInput, secondsInput})
            field.addTextChangedListener(durationWatcher);
        updateControls();
        renderTime(running || remainingMillis > 0 ? currentRemaining() : readDuration());
        renderSelection();
    }

    private void setMode(boolean dateMode) {
        if (running || remainingMillis > 0) return;
        dateTimeMode = dateMode;
        updateControls(); renderSelection(); save();
        renderTime(dateTimeMode ? Math.max(0L, selectedTarget - System.currentTimeMillis()) : readDuration());
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
            LogoToast.makeText(host, dateTimeMode ? AppString.get(R.string.runtime_text_0165) : AppString.get(R.string.runtime_text_0166), Toast.LENGTH_SHORT).show();
            return;
        }
        if (remainingMillis == 0L) host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit()
                .remove("timer_active_preset_id").apply();
        if (remainingMillis == 0L) initialDurationMillis = value;
        deadline = System.currentTimeMillis() + value;
        remainingMillis = value; running = true;
        String label = labelInput.getText().toString().trim();
        if (label.isEmpty()) label = ToolAlarmScheduler.defaultLabel(ToolAlarmScheduler.TIMER);
        boolean scheduled = ToolAlarmScheduler.schedule(host, ToolAlarmScheduler.TIMER, deadline, label);
        if (!scheduled) LogoToast.makeText(host, AppString.get(R.string.runtime_text_0149), Toast.LENGTH_LONG).show();
        save(); updateControls(); handler.post(ticker);
    }

    private void stopTimer() {
        running = false; remainingMillis = 0L; deadline = 0L;
        ToolAlarmScheduler.cancel(host, ToolAlarmScheduler.TIMER);
        handler.removeCallbacks(ticker);
        host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit()
                .remove("timer_active_preset_id").apply();
    }

    private void reset() {
        boolean started = running || remainingMillis > 0;
        stopTimer();
        if (!started) {
            dateTimeMode = false;
            initialDurationMillis = 0L;
            hoursInput.setText("0"); minutesInput.setText("0"); secondsInput.setText("0");
        } else if (dateTimeMode && initialDurationMillis > 0) {
            selectedTarget = System.currentTimeMillis() + initialDurationMillis;
        }
        saveDraft(); save(); updateControls(); renderSelection();
        renderTime(dateTimeMode ? Math.max(0L, selectedTarget - System.currentTimeMillis()) : readDuration());
    }

    private void completeLocally() {
        running = false; remainingMillis = 0L; deadline = 0L;
        handler.removeCallbacks(ticker); save(); updateControls(); renderTime(0L);
    }

    private long currentRemaining() { return running ? Math.max(0L, deadline - System.currentTimeMillis()) : remainingMillis; }
    private long readDuration() { return durationFields.readMillis(); }

    private void updateControls() {
        boolean configured = !running && remainingMillis == 0L;
        if (presets != null) presets.refresh();
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
        ((Button) root.findViewById(R.id.timer_reset)).setText(
                configured ? R.string.timer_clear_fields : R.string.tool_reset_label);
        renderEndTime();
        startButton.setText(running ? AppString.get(R.string.runtime_text_0011) : remainingMillis > 0 ? AppString.get(R.string.runtime_text_0012) : AppString.get(R.string.runtime_text_0010));
    }

    private void renderSelection() {
        dateButton.setText(CalendarUtils.formatDate(selectedTarget, calendarType));
        Calendar c=Calendar.getInstance(); c.setTimeInMillis(selectedTarget);
        timeButton.setText(String.format(Locale.US,"%02d:%02d",c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE)));
        renderEndTime();
    }

    private void renderEndTime() {
        long remaining = running ? currentRemaining() : remainingMillis > 0 ? remainingMillis : readDuration();
        long target = running ? deadline : remainingMillis > 0 ? System.currentTimeMillis() + remaining
                : dateTimeMode ? selectedTarget : remaining > 0 ? System.currentTimeMillis() + remaining : 0;
        summary.setText(target <= 0 ? "" : CalendarUtils.fa(String.format(Locale.US, "%tR", target))
                + "   " + CalendarUtils.formatNumeric(target, calendarType));
    }

    private void renderTime(long millis) {
        long total=(millis+999)/1000;
        timeView.setText(String.format(Locale.US,"%02d:%02d:%02d",total/3600,(total/60)%60,total%60));
        renderEndTime();
    }

    private void save() { host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit().putBoolean("timer_running",running).putBoolean("timer_date_mode",dateTimeMode).putLong("timer_remaining",remainingMillis).putLong("timer_initial_duration",initialDurationMillis).putLong("timer_end",deadline).putLong("timer_target",selectedTarget).putInt("timer_calendar",calendarType).apply();     TimeToolsWidgetProvider.updateAll(host);
    }
    private void restore() { SharedPreferences p=host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE); running=p.getBoolean("timer_running",false); dateTimeMode=p.getBoolean("timer_date_mode",false); remainingMillis=p.getLong("timer_remaining",0); initialDurationMillis=p.getLong("timer_initial_duration",remainingMillis); deadline=p.getLong("timer_end",0); selectedTarget=p.getLong("timer_target",0); calendarType=p.getInt("timer_calendar",AppSettings.defaultCalendar(host)); if(running&&deadline<=System.currentTimeMillis()){running=false;remainingMillis=0;deadline=0;save();} }
    void setActive(boolean value) {
        if (active && !value) KeyboardDismiss.hide(host);
        active = value;
        handler.removeCallbacks(ticker);
        if (active && resumed) {
            renderTime(running || remainingMillis > 0
                    ? currentRemaining()
                    : dateTimeMode
                    ? Math.max(0, selectedTarget - System.currentTimeMillis())
                    : readDuration());
            if (running) handler.post(ticker);
        }
    }

    void onResume(){
        resumed = true;
        restoreDraft();
        restore();
        updateControls();
        renderSelection();
        renderTime(running||remainingMillis>0?currentRemaining():dateTimeMode?Math.max(0,selectedTarget-System.currentTimeMillis()):readDuration());
        handler.removeCallbacks(ticker);
        if(active && running) handler.post(ticker);
    }

    void onPause(){
        resumed = false;
        handler.removeCallbacks(ticker);
        saveDraft();
        save();
    }

    private void saveDraft() {
        host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit()
                .putString("timer_draft_label", labelInput.getText().toString())
                .putString("timer_draft_hours", hoursInput.getText().toString())
                .putString("timer_draft_minutes", minutesInput.getText().toString())
                .putString("timer_draft_seconds", secondsInput.getText().toString()).apply();
    }

    private void restoreDraft() {
        SharedPreferences p = host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        labelInput.setText(p.getString("timer_draft_label", ""));
        hoursInput.setText(p.getString("timer_draft_hours", ""));
        minutesInput.setText(p.getString("timer_draft_minutes", "5"));
        secondsInput.setText(p.getString("timer_draft_seconds", ""));
    }

    private org.json.JSONObject capturePreset() throws org.json.JSONException {
        saveDraft();
        long duration = readDuration();
        if ((!dateTimeMode && duration <= 0) || (dateTimeMode && selectedTarget <= System.currentTimeMillis())) {
            LogoToast.makeText(host, AppString.get(R.string.tool_invalid), Toast.LENGTH_SHORT).show();
            return null;
        }
        String label = labelInput.getText().toString().trim();
        String defaultTitle = dateTimeMode
                ? CalendarUtils.formatDate(selectedTarget, calendarType) + " " + String.format(Locale.US, "%tR", selectedTarget)
                : String.format(Locale.US, "%02d:%02d:%02d", duration / 3600000, duration / 60000 % 60, duration / 1000 % 60);
        return new org.json.JSONObject().put("label", label.isEmpty() ? defaultTitle : label)
                .put("inputHours", hoursInput.getText().toString())
                .put("inputMinutes", minutesInput.getText().toString())
                .put("inputSeconds", secondsInput.getText().toString())
                .put("dateMode", dateTimeMode).put("duration", duration)
                .put("target", selectedTarget).put("calendar", calendarType);
    }

    private void loadPreset(org.json.JSONObject config, boolean run) throws org.json.JSONException {
        boolean dateMode = config.getBoolean("dateMode");
        long duration = config.getLong("duration");
        long target = config.getLong("target");
        if ((run && dateMode && target <= System.currentTimeMillis()) || (!dateMode && duration <= 0))
            throw new org.json.JSONException("Expired or invalid timer");
        int savedCalendar = config.getInt("calendar");
        String savedLabel = config.getString("label");
        stopTimer();
        dateTimeMode = dateMode;
        selectedTarget = target;
        calendarType = savedCalendar;
        labelInput.setText(savedLabel);
        long seconds = duration / 1000;
        hoursInput.setText(config.optString("inputHours", String.valueOf(seconds / 3600)));
        minutesInput.setText(config.optString("inputMinutes", String.valueOf(seconds / 60 % 60)));
        secondsInput.setText(config.optString("inputSeconds", String.valueOf(seconds % 60)));
        TimeToolPresets.restoreImages(host, ToolAlarmScheduler.TIMER, config);
        saveDraft(); save(); updateControls(); renderSelection();
        renderTime(dateTimeMode ? Math.max(0L, target - System.currentTimeMillis()) : duration);
        if (run) toggleTimer();
    }
}
