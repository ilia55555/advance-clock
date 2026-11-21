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
    private TimeToolPresets presets;
    private DurationInputs durationFields;
    private static final String PREFS = "time_tools";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<Long> laps = new ArrayList<>();
    private TextView timeView, summary;
    private Button startButton, lapButton;
    private LinearLayout lapList;
    private Button saveRecordButton;
    private TextView recordHint;
    private View limitInputs, dateTimeInputs, modeButtons;
    private EditText hoursInput, minutesInput, secondsInput, labelInput;
    private Button unlimitedModeButton, durationModeButton, dateTimeModeButton;
    private Button dateButton, timeButton;
    private boolean running;
    private boolean resumed;
    private boolean active;
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
            if (running && resumed && active) {
                handler.postDelayed(this, 50L);
            }
        }
    };

    StopwatchPanelController(Activity host, View root) {
        this.host = host;
        this.root = root;
        timeView = root.findViewById(R.id.stopwatch_time);
        summary = root.findViewById(R.id.stopwatch_target_summary);
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
        durationFields = new DurationInputs(hoursInput, minutesInput, secondsInput);
        labelInput = root.findViewById(R.id.stopwatch_label);
        restoreDraft();
        presets = new TimeToolPresets(host, labelInput, ToolAlarmScheduler.STOPWATCH,
                new TimeToolPresets.Editor() {
                    public org.json.JSONObject capture() throws org.json.JSONException { return capturePreset(); }
                    public void load(org.json.JSONObject config, boolean run) throws org.json.JSONException { loadPreset(config, run); }
                    public boolean busy() { return running || accumulatedMillis > 0; }
                });
        restore();
        if (!host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).contains("stopwatch_draft_minutes")) populateLimitInputs();
        View back = root.findViewById(R.id.back_button);
        ((View) back.getParent()).setVisibility(View.GONE);
        startButton.setOnClickListener(v -> toggleRunning());
        root.findViewById(R.id.stopwatch_reset).setOnClickListener(v -> reset());
        lapButton.setOnClickListener(v -> addLap());
        buildRecordControls();
        unlimitedModeButton.setOnClickListener(v -> setMode(0));
        durationModeButton.setOnClickListener(v -> setMode(1));
        dateTimeModeButton.setOnClickListener(v -> setMode(2));
        dateButton.setOnClickListener(v -> CalendarPickerDialog.showDate(
                host, selectedTarget, calendarType, (millis, type) -> {
                    Calendar picked = Calendar.getInstance(AppTime.zone());
                    picked.setTimeInMillis(millis);
                    Calendar old = Calendar.getInstance(AppTime.zone());
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
            Calendar c = Calendar.getInstance(AppTime.zone());
            c.setTimeInMillis(selectedTarget);
            new MotionDialogs.TimePicker(host, (view, hour, minute) -> {
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
        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int start,int count,int after) {}
            public void onTextChanged(CharSequence s,int start,int before,int count) { renderEndTime(); }
            public void afterTextChanged(android.text.Editable s) {}
        };
        for (EditText field : new EditText[]{hoursInput,minutesInput,secondsInput}) field.addTextChangedListener(watcher);
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
                    LogoToast.makeText(host, mode == 2
                            ? AppString.get(R.string.runtime_text_0165)
                            : AppString.get(R.string.runtime_text_0298), Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            if(limitMillis>0L&&accumulatedMillis>=limitMillis){LogoToast.makeText(host,AppString.get(R.string.runtime_text_0299),Toast.LENGTH_SHORT).show();return;}
            if (accumulatedMillis == 0L) host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit()
                    .remove("stopwatch_active_preset_id").apply();
            startedAtWall=System.currentTimeMillis();
            if (mode == 2 && selectedTarget <= startedAtWall) {
                LogoToast.makeText(host, AppString.get(R.string.runtime_text_0300), Toast.LENGTH_SHORT).show();
                return;
            }
            if (mode == 2) limitMillis = accumulatedMillis + selectedTarget - startedAtWall;
            running=true;
            if(limitMillis>0L){
                String label=labelInput.getText().toString().trim();
                boolean scheduled=ToolAlarmScheduler.schedule(host,ToolAlarmScheduler.STOPWATCH,startedAtWall+(limitMillis-accumulatedMillis),label);
                if(!scheduled)LogoToast.makeText(host,AppString.get(R.string.runtime_text_0149),Toast.LENGTH_LONG).show();
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
        Calendar c = Calendar.getInstance(AppTime.zone());
        c.setTimeInMillis(selectedTarget);
        timeButton.setText(String.format(Locale.US, "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
        renderEndTime();
    }

    private void renderEndTime() {
        summary.setVisibility(mode == 0 ? View.GONE : View.VISIBLE);
        if (mode == 0) { summary.setText(""); return; }
        long remaining = Math.max(0, limitMillis - accumulatedMillis);
        long target = mode == 2 ? selectedTarget : running ? startedAtWall + remaining
                : accumulatedMillis > 0 ? System.currentTimeMillis() + remaining
                : System.currentTimeMillis() + readLimit();
        summary.setText(TimeFormat.endTime(host, target, calendarType));
    }

    private void reset(){running=false;accumulatedMillis=0L;startedAtWall=0L;limitMillis=0L;laps.clear();savedLaps.clear();host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit().remove("stopwatch_record_category").apply();ToolAlarmScheduler.cancel(host,ToolAlarmScheduler.STOPWATCH);handler.removeCallbacks(ticker);save();renderTime(0);renderLaps();updateControls();}
    private void addLap(){long elapsed=currentElapsed();if(elapsed<=0||(!laps.isEmpty()&&laps.get(0)==elapsed))return;laps.add(0,elapsed);save();renderLaps(true);}
    /** Adds "save record" / "my records" buttons and a best-record hint under the clock. */
    private void buildRecordControls() {
        LinearLayout parent = (LinearLayout) lapButton.getParent();
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(AppSettings.layoutDirection(host));
        saveRecordButton = recordButton(AppString.get(R.string.stopwatch_record_save));
        Button records = recordButton(AppString.get(R.string.stopwatch_records));
        row.addView(saveRecordButton, new LinearLayout.LayoutParams(0, -1, 1f));
        View gap = new View(host);
        row.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        row.addView(records, new LinearLayout.LayoutParams(0, -1, 1f));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(52));
        rowParams.topMargin = dp(8);
        // Records replace the old save row, right under the photo/file controls.
        View controls = presets.controlsView();
        parent.addView(row, controls.getParent() == parent
                ? parent.indexOfChild(controls) + 1 : parent.indexOfChild(lapButton) + 1, rowParams);
        saveRecordButton.setOnClickListener(v -> {
            // Capture the time into the list below; each entry has its own save button.
            addLap();
        });
        records.setOnClickListener(v -> StopwatchRecordsUi.showList(host, this::renderRecordHint));

        recordHint = new TextView(host);
        recordHint.setGravity(android.view.Gravity.CENTER);
        recordHint.setTextColor(AppSettings.textSecondary(host));
        recordHint.setTextSize(14);
        recordHint.setVisibility(View.GONE);
        LinearLayout timeParent = (LinearLayout) timeView.getParent();
        timeParent.addView(recordHint, timeParent.indexOfChild(timeView) + 1, new LinearLayout.LayoutParams(-1, -2));
        labelInput.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s,int a,int b,int c) {}
            public void onTextChanged(CharSequence s,int a,int b,int c) {}
            public void afterTextChanged(android.text.Editable e) { renderRecordHint(); }
        });
        renderRecordHint();
    }

    private Button recordButton(String text) {
        Button button = new Button(host);
        button.setText(text);
        button.setAllCaps(false);
        button.setBackgroundResource(R.drawable.bg_field);
        button.setTextColor(AppSettings.primaryColor(host));
        return button;
    }

    private void renderRecordHint() {
        if (recordHint == null) return;
        StopwatchRecords.Activity activity = new StopwatchRecords(host).find(labelInput.getText().toString());
        StopwatchRecords.Record best = activity == null ? null : activity.best();
        if (best == null) { recordHint.setVisibility(View.GONE); return; }
        recordHint.setText("🏆 " + activity.name + "  " + StopwatchRecordsUi.format(best.millis));
        recordHint.setVisibility(View.VISIBLE);
    }

    private long currentElapsed(){return running?accumulatedMillis+Math.max(0,System.currentTimeMillis()-startedAtWall):accumulatedMillis;}
    private long readLimit() { return durationFields.readMillis(); }

    private void updateControls() {
        boolean configurable = !running && accumulatedMillis == 0L;
        if(modeButtons.getVisibility()!=(configurable?View.VISIBLE:View.GONE)
                || limitInputs.getVisibility()!=(configurable && mode==1?View.VISIBLE:View.GONE)
                || dateTimeInputs.getVisibility()!=(configurable && mode==2?View.VISIBLE:View.GONE))
            AppMotion.expand((android.view.ViewGroup)modeButtons.getParent());
        if (presets != null) presets.refresh();
        startButton.setText(running ? AppString.get(R.string.runtime_text_0011) : accumulatedMillis > 0 ? AppString.get(R.string.runtime_text_0012) : AppString.get(R.string.runtime_text_0010));
        lapButton.setEnabled(running);
        boolean canSave = running || accumulatedMillis > 0L;
        if (saveRecordButton != null) saveRecordButton.setEnabled(canSave);
        if (saveRecordButton != null) saveRecordButton.setAlpha(canSave ? 1f : 0.45f);
        // The separate lap button is replaced by "save record".
        lapButton.setVisibility(View.GONE);
        renderEndTime();
        modeButtons.setVisibility(configurable ? View.VISIBLE : View.GONE);
        limitInputs.setVisibility(configurable && mode == 1 ? View.VISIBLE : View.GONE);
        dateTimeInputs.setVisibility(configurable && mode == 2 ? View.VISIBLE : View.GONE);
        labelInput.setVisibility(configurable ? View.VISIBLE : View.GONE);
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
    private void renderLaps(){renderLaps(false);}
    private final java.util.HashSet<Long> savedLaps = new java.util.HashSet<>();

    /** Category chosen for the current stopwatch run; reused for every later entry. */
    private String sessionCategory() {
        return host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).getString("stopwatch_record_category", null);
    }

    private void setSessionCategory(String category) {
        host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit()
                .putString("stopwatch_record_category", category).apply();
        renderCategory();
    }

    private void saveEntry(long value) {
        String category = sessionCategory();
        if (category == null) { askCategory(value); return; }
        StopwatchRecordsUi.saveDirect(host, value, category, false);
        markSaved(value);
    }

    private void askCategory(long value) {
        String current = sessionCategory();
        StopwatchRecordsUi.showSave(host, value,
                current != null ? current : labelInput.getText().toString(), category -> {
                    setSessionCategory(category);
                    markSaved(value);
                });
    }

    private void markSaved(long value) {
        savedLaps.add(value);
        renderLaps();
        renderRecordHint();
    }

    private void renderCategory() {
        if (categoryView == null) return;
        String category = sessionCategory();
        if (category == null || laps.isEmpty()) { categoryView.setVisibility(View.GONE); return; }
        categoryView.setText(AppString.get(R.string.stopwatch_record_category) + "  «" + category + "»   "
                + AppString.get(R.string.stopwatch_record_category_change));
        categoryView.setVisibility(View.VISIBLE);
    }

    private TextView categoryView;

    private void renderLaps(boolean animateNew) {
        if (categoryView == null) {
            categoryView = new TextView(host);
            categoryView.setTextColor(AppSettings.primaryColor(host));
            categoryView.setTextSize(13);
            categoryView.setGravity(android.view.Gravity.CENTER);
            categoryView.setPadding(dp(8), dp(8), dp(8), dp(4));
            categoryView.setOnClickListener(v -> {
                if (laps.isEmpty()) return;
                askCategory(laps.get(0));
            });
            LinearLayout listParent = (LinearLayout) lapList.getParent();
            listParent.addView(categoryView, listParent.indexOfChild(lapList), new LinearLayout.LayoutParams(-1, -2));
        }
        renderCategory();
        lapList.removeAllViews();
        for (int i = 0; i < laps.size(); i++) {
            long value = laps.get(i);
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(AppSettings.layoutDirection(host));
            row.setPadding(dp(12), dp(6), dp(12), dp(6));
            TextView label = new TextView(host);
            label.setText(CalendarUtils.fa(String.format(Locale.US,
                    AppString.get(R.string.stopwatch_record_entry), laps.size() - i, format(value))));
            label.setTextColor(AppSettings.textPrimary(host));
            label.setTextSize(17);
            label.setGravity(android.view.Gravity.CENTER);
            row.addView(label, new LinearLayout.LayoutParams(0, -2, 1f));
            Button save = new Button(host);
            boolean saved = savedLaps.contains(value);
            save.setText(saved ? "✓" : AppString.get(R.string.runtime_text_0005));
            save.setAllCaps(false);
            save.setTextSize(13);
            save.setEnabled(!saved);
            save.setBackgroundResource(R.drawable.bg_field);
            save.setTextColor(AppSettings.primaryColor(host));
            save.setOnClickListener(v -> saveEntry(value));
            save.setOnLongClickListener(v -> { askCategory(value); return true; });
            save.setTextSize(12);
            save.setPadding(0, 0, 0, 0);
            save.setMinWidth(0);
            save.setMinimumWidth(0);
            save.setMinHeight(0);
            save.setMinimumHeight(0);
            row.addView(save, new LinearLayout.LayoutParams(dp(60), dp(34)));
            lapList.addView(row, new LinearLayout.LayoutParams(-1, -2));
            if(animateNew && i==0)AppMotion.appear(row);
            View divider = new View(host);
            divider.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            divider.setBackgroundColor(AppSettings.field(host));
            lapList.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
        }
    }

    private int dp(int value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }

    private static String format(long millis){long cs=millis/10;return String.format(Locale.US,"%02d:%02d:%02d.%02d",cs/360000,(cs/6000)%60,(cs/100)%60,cs%100);}
    private void save(){StringBuilder encoded=new StringBuilder();for(long lap:laps){if(encoded.length() > 0)encoded.append(',');encoded.append(lap);}host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).edit().putBoolean("stopwatch_running",running).putInt("stopwatch_mode",mode).putInt("stopwatch_calendar",calendarType).putLong("stopwatch_target",selectedTarget).putLong("stopwatch_accumulated",accumulatedMillis).putLong("stopwatch_started",startedAtWall).putLong("stopwatch_limit",limitMillis).putString("stopwatch_laps",encoded.toString()).apply();TimeToolsWidgetProvider.updateAll(host);}
    private void restore(){SharedPreferences p=host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);running=p.getBoolean("stopwatch_running",false);mode=p.getInt("stopwatch_mode",0);calendarType=p.getInt("stopwatch_calendar",AppSettings.defaultCalendar(host));selectedTarget=p.getLong("stopwatch_target",0);accumulatedMillis=p.getLong("stopwatch_accumulated",0);startedAtWall=p.getLong("stopwatch_started",0);limitMillis=p.getLong("stopwatch_limit",0);laps.clear();String encoded=p.getString("stopwatch_laps","");if(encoded.length() > 0)for(String value:encoded.split(","))try{laps.add(Long.parseLong(value));}catch(NumberFormatException ignored){}if(running&&limitMillis>0&&currentElapsed()>=limitMillis){running=false;accumulatedMillis=limitMillis;startedAtWall=0;save();}}
    void setActive(boolean value) {
        if (active && !value) KeyboardDismiss.hide(host);
        active = value;
        handler.removeCallbacks(ticker);
        if (active && resumed) {
            renderTime(currentElapsed());
            if (running) handler.post(ticker);
        }
    }

    void onResume(){
        resumed = true;
        restoreDraft();
        restore();
        if (!host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE).contains("stopwatch_draft_minutes")) populateLimitInputs();
        renderTarget();
        renderLaps();
        updateControls();
        renderTime(currentElapsed());
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
                .putString("stopwatch_draft_label", labelInput.getText().toString())
                .putString("stopwatch_draft_hours", hoursInput.getText().toString())
                .putString("stopwatch_draft_minutes", minutesInput.getText().toString())
                .putString("stopwatch_draft_seconds", secondsInput.getText().toString()).apply();
    }

    private void restoreDraft() {
        SharedPreferences p = host.getSharedPreferences(PREFS, Activity.MODE_PRIVATE);
        labelInput.setText(p.getString("stopwatch_draft_label", ""));
        hoursInput.setText(p.getString("stopwatch_draft_hours", ""));
        minutesInput.setText(p.getString("stopwatch_draft_minutes", ""));
        secondsInput.setText(p.getString("stopwatch_draft_seconds", ""));
    }

    private org.json.JSONObject capturePreset() throws org.json.JSONException {
        saveDraft();
        long duration = readLimit();
        if ((mode == 1 && duration <= 0) || (mode == 2 && selectedTarget <= System.currentTimeMillis())) {
            LogoToast.makeText(host, AppString.get(R.string.tool_invalid), Toast.LENGTH_SHORT).show(); return null;
        }
        String label = labelInput.getText().toString().trim();
        return new org.json.JSONObject().put("label", label.isEmpty() ? ToolAlarmScheduler.defaultLabel(ToolAlarmScheduler.STOPWATCH) : label)
                .put("inputHours", hoursInput.getText().toString())
                .put("inputMinutes", minutesInput.getText().toString())
                .put("inputSeconds", secondsInput.getText().toString())
                .put("mode", mode).put("duration", duration).put("target", selectedTarget).put("calendar", calendarType);
    }

    private void loadPreset(org.json.JSONObject config, boolean run) throws org.json.JSONException {
        int savedMode = config.getInt("mode");
        long duration = config.getLong("duration");
        long target = config.getLong("target");
        if (savedMode < 0 || savedMode > 2 || (savedMode == 1 && duration <= 0)
                || (run && savedMode == 2 && target <= System.currentTimeMillis()))
            throw new org.json.JSONException("Expired or invalid stopwatch");
        int savedCalendar = config.getInt("calendar");
        String savedLabel = config.getString("label");
        reset();
        mode = savedMode; selectedTarget = target; calendarType = savedCalendar;
        labelInput.setText(savedLabel);
        long seconds = duration / 1000;
        hoursInput.setText(config.optString("inputHours", String.valueOf(seconds / 3600)));
        minutesInput.setText(config.optString("inputMinutes", String.valueOf(seconds / 60 % 60)));
        secondsInput.setText(config.optString("inputSeconds", String.valueOf(seconds % 60)));
        TimeToolPresets.restoreImages(host, ToolAlarmScheduler.STOPWATCH, config);
        saveDraft(); save(); updateControls(); renderTarget(); renderTime(0);
        if (run) toggleRunning();
    }
}
