package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.app.TimePickerDialog;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQ_NOTIFICATIONS = 100;
    private static final int REQ_SETTINGS = 200;
    private static final String PREFS = "advance_clock_app";
    private static final String PERMISSION_ONBOARDING = "permission_onboarding_v2";

    private final Calendar quickAlarm = Calendar.getInstance();
    private final Calendar quickNoteDue = Calendar.getInstance();

    private LinearLayout alarmList;
    private LinearLayout noForgetList;
    private View clockPanel;
    private View noForgetPanel;
    private TextView clockTab;
    private TextView noForgetTab;
    private View clockIndicator;
    private View noForgetIndicator;
    private TextView appTitle;

    private Button quickAlarmDate;
    private Button quickAlarmTime;
    private Button quickAlarmRepeat;
    private Spinner quickAlarmPriority;
    private EditText quickAlarmLabel;
    private Switch quickAlarmVibrate;
    private TripleCalendarView clockCalendar;
    private TripleCalendarView noteCalendar;

    private EditText quickNoteTitle;
    private EditText quickNoteBody;
    private Spinner quickNotePriority;
    private Switch quickNoteAlarmSwitch;
    private View quickNoteAlarmControls;
    private Button quickNoteDate;
    private Button quickNoteTime;
    private Button quickNoteRepeat;
    private SketchView quickNoteSketch;

    private int alarmRecurrenceMode = RecurrenceUtils.NONE;
    private int alarmIntervalDays = 1;
    private String alarmCustomDates = "[]";

    private int noteRecurrenceMode = RecurrenceUtils.NONE;
    private int noteIntervalDays = 1;
    private String noteCustomDates = "[]";

    private int permissionStage = -1;
    private boolean waitingForSettings;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationHelper.ensureChannels(this);

        bindViews();
        setupHeader();
        setupAlarmComposer();
        setupNoteComposer();
        setupCalendars();

        clockTab.setOnClickListener(v -> showTab("clock"));
        noForgetTab.setOnClickListener(v -> showTab("noforget"));

        findViewById(R.id.add_clock_widget).setOnClickListener(v -> pinWidget(ClockWidgetProvider.class));
        findViewById(R.id.add_noforget_widget).setOnClickListener(v -> pinWidget(NoForgetWidgetProvider.class));
        findViewById(R.id.open_full_note_editor).setOnClickListener(v ->
                startActivity(new Intent(this, NoForgetEditorActivity.class)));

        String requestedTab = getIntent().getStringExtra("openTab");
        showTab("noforget".equals(requestedTab) ? "noforget" : "clock");

        boolean onboardingDone = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PERMISSION_ONBOARDING, false);
        if (!onboardingDone) getWindow().getDecorView().postDelayed(this::startPermissionFlow, 450);
    }

    private void bindViews() {
        alarmList = findViewById(R.id.alarm_list);
        noForgetList = findViewById(R.id.noforget_list);
        clockPanel = findViewById(R.id.clock_panel);
        noForgetPanel = findViewById(R.id.noforget_panel);
        clockTab = findViewById(R.id.tab_clock);
        noForgetTab = findViewById(R.id.tab_noforget);
        clockIndicator = findViewById(R.id.clock_indicator);
        noForgetIndicator = findViewById(R.id.noforget_indicator);
        appTitle = findViewById(R.id.app_title);

        quickAlarmDate = findViewById(R.id.quick_alarm_date);
        quickAlarmTime = findViewById(R.id.quick_alarm_time);
        quickAlarmRepeat = findViewById(R.id.quick_alarm_repeat);
        quickAlarmPriority = findViewById(R.id.quick_alarm_priority);
        quickAlarmLabel = findViewById(R.id.quick_alarm_label);
        quickAlarmVibrate = findViewById(R.id.quick_alarm_vibrate);
        clockCalendar = findViewById(R.id.clock_calendar);

        noteCalendar = findViewById(R.id.noforget_calendar);
        quickNoteTitle = findViewById(R.id.quick_note_title);
        quickNoteBody = findViewById(R.id.quick_note_body);
        quickNotePriority = findViewById(R.id.quick_note_priority);
        quickNoteAlarmSwitch = findViewById(R.id.quick_note_alarm_switch);
        quickNoteAlarmControls = findViewById(R.id.quick_note_alarm_controls);
        quickNoteDate = findViewById(R.id.quick_note_date);
        quickNoteTime = findViewById(R.id.quick_note_time);
        quickNoteRepeat = findViewById(R.id.quick_note_repeat);
        quickNoteSketch = findViewById(R.id.quick_note_sketch);
    }

    private void setupHeader() {
        findViewById(R.id.theme_toggle).setOnClickListener(v -> {
            int next = AppSettings.themeMode(this) == AppSettings.THEME_DARK
                    ? AppSettings.THEME_LIGHT : AppSettings.THEME_DARK;
            AppSettings.setThemeMode(this, next);
            recreate();
        });

        findViewById(R.id.header_menu).setOnClickListener(anchor -> {
            PopupMenu menu = new PopupMenu(this, anchor);
            menu.getMenu().add(0, 1, 0, "تنظیمات");
            menu.getMenu().add(0, 2, 1, "افزودن ویجت این بخش");
            menu.getMenu().add(0, 3, 2, "مجوزهای آلارم و اعلان");
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    startActivityForResult(new Intent(this, SettingsActivity.class), REQ_SETTINGS);
                    return true;
                }
                if (item.getItemId() == 2) {
                    pinWidget(clockPanel.getVisibility() == View.VISIBLE
                            ? ClockWidgetProvider.class : NoForgetWidgetProvider.class);
                    return true;
                }
                if (item.getItemId() == 3) {
                    startPermissionFlow();
                    return true;
                }
                return false;
            });
            menu.show();
        });
    }

    private void setupCalendars() {
        int type = AppSettings.defaultCalendar(this);
        clockCalendar.setCalendarType(type);
        noteCalendar.setCalendarType(type);

        TripleCalendarView.OnMonthYearClickListener monthClick = (visible, calendarType) ->
                CalendarPickerDialog.showMonthYear(this, visible, calendarType, (picked, pickedType) -> {
                    if (clockPanel.getVisibility() == View.VISIBLE) {
                        clockCalendar.setVisibleMonthMillis(picked, pickedType);
                    } else {
                        noteCalendar.setVisibleMonthMillis(picked, pickedType);
                    }
                });
        clockCalendar.setOnMonthYearClickListener(monthClick);
        noteCalendar.setOnMonthYearClickListener(monthClick);

        clockCalendar.setOnDateSelectedListener(millis -> {
            applyDate(quickAlarm, millis);
            updateQuickAlarmLabels();
        });
        noteCalendar.setOnDateSelectedListener(millis -> {
            applyDate(quickNoteDue, millis);
            updateQuickNoteLabels();
        });
    }

    private void setupAlarmComposer() {
        setPrioritySpinner(quickAlarmPriority, AlarmItem.PRIORITY_NORMAL);

        quickAlarm.add(Calendar.MINUTE, 1);
        quickAlarm.set(Calendar.SECOND, 0);
        quickAlarm.set(Calendar.MILLISECOND, 0);
        updateQuickAlarmLabels();

        quickAlarmDate.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this,
                quickAlarm.getTimeInMillis(),
                AppSettings.defaultCalendar(this),
                (picked, type) -> {
                    applyDate(quickAlarm, picked);
                    clockCalendar.setCalendarType(type);
                    clockCalendar.setSelectedMillis(quickAlarm.getTimeInMillis());
                    updateQuickAlarmLabels();
                }
        ));

        quickAlarmTime.setOnClickListener(v -> new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    quickAlarm.set(Calendar.HOUR_OF_DAY, hour);
                    quickAlarm.set(Calendar.MINUTE, minute);
                    quickAlarm.set(Calendar.SECOND, 0);
                    quickAlarm.set(Calendar.MILLISECOND, 0);
                    updateQuickAlarmLabels();
                },
                quickAlarm.get(Calendar.HOUR_OF_DAY),
                quickAlarm.get(Calendar.MINUTE),
                true
        ).show());

        quickAlarmRepeat.setOnClickListener(v -> RecurrenceDialog.show(
                this,
                quickAlarm.getTimeInMillis(),
                alarmRecurrenceMode,
                alarmIntervalDays,
                alarmCustomDates,
                (mode, interval, dates) -> {
                    alarmRecurrenceMode = mode;
                    alarmIntervalDays = interval;
                    alarmCustomDates = dates;
                    quickAlarmRepeat.setText(RecurrenceUtils.summary(mode, interval, dates));
                }
        ));

        findViewById(R.id.save_quick_alarm).setOnClickListener(v -> saveQuickAlarm());
    }

    private void setupNoteComposer() {
        setPrioritySpinner(quickNotePriority, NoForgetItem.PRIORITY_NORMAL);

        quickNoteDue.add(Calendar.HOUR_OF_DAY, 1);
        quickNoteDue.set(Calendar.SECOND, 0);
        quickNoteDue.set(Calendar.MILLISECOND, 0);
        updateQuickNoteLabels();

        quickNoteAlarmSwitch.setOnCheckedChangeListener((button, checked) ->
                quickNoteAlarmControls.setVisibility(checked ? View.VISIBLE : View.GONE));

        quickNoteDate.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this,
                quickNoteDue.getTimeInMillis(),
                AppSettings.defaultCalendar(this),
                (picked, type) -> {
                    applyDate(quickNoteDue, picked);
                    noteCalendar.setCalendarType(type);
                    noteCalendar.setSelectedMillis(quickNoteDue.getTimeInMillis());
                    updateQuickNoteLabels();
                }
        ));

        quickNoteTime.setOnClickListener(v -> new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    quickNoteDue.set(Calendar.HOUR_OF_DAY, hour);
                    quickNoteDue.set(Calendar.MINUTE, minute);
                    quickNoteDue.set(Calendar.SECOND, 0);
                    quickNoteDue.set(Calendar.MILLISECOND, 0);
                    updateQuickNoteLabels();
                },
                quickNoteDue.get(Calendar.HOUR_OF_DAY),
                quickNoteDue.get(Calendar.MINUTE),
                true
        ).show());

        quickNoteRepeat.setOnClickListener(v -> RecurrenceDialog.show(
                this,
                quickNoteDue.getTimeInMillis(),
                noteRecurrenceMode,
                noteIntervalDays,
                noteCustomDates,
                (mode, interval, dates) -> {
                    noteRecurrenceMode = mode;
                    noteIntervalDays = interval;
                    noteCustomDates = dates;
                    quickNoteRepeat.setText(RecurrenceUtils.summary(mode, interval, dates));
                }
        ));

        findViewById(R.id.save_quick_note).setOnClickListener(v -> saveQuickNote());
        findViewById(R.id.note_undo).setOnClickListener(v -> quickNoteSketch.undo());
        findViewById(R.id.note_redo).setOnClickListener(v -> quickNoteSketch.redo());
        findViewById(R.id.note_clear).setOnClickListener(v -> quickNoteSketch.clearSketch());
        findViewById(R.id.note_palette).setOnClickListener(v ->
                PaletteDialog.show(this, quickNoteSketch.getPenColor(), quickNoteSketch::setPenColor));

        findViewById(R.id.pen_thin).setOnClickListener(v -> quickNoteSketch.setPenWidthDp(2f));
        findViewById(R.id.pen_medium).setOnClickListener(v -> quickNoteSketch.setPenWidthDp(4f));
        findViewById(R.id.pen_thick).setOnClickListener(v -> quickNoteSketch.setPenWidthDp(7f));
    }

    private void setPrioritySpinner(Spinner spinner, int selection) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item,
                new String[]{"اهمیت کم", "اهمیت عادی", "اهمیت زیاد"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(Math.max(0, Math.min(2, selection)));
    }

    private void applyDate(Calendar target, long sourceMillis) {
        Calendar source = Calendar.getInstance();
        source.setTimeInMillis(sourceMillis);
        target.set(Calendar.YEAR, source.get(Calendar.YEAR));
        target.set(Calendar.MONTH, source.get(Calendar.MONTH));
        target.set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH));
    }

    private void updateQuickAlarmLabels() {
        int type = AppSettings.defaultCalendar(this);
        quickAlarmDate.setText("تاریخ\n" + CalendarUtils.formatDate(quickAlarm.getTimeInMillis(), type));
        String time = String.format(Locale.US, "%02d:%02d",
                quickAlarm.get(Calendar.HOUR_OF_DAY), quickAlarm.get(Calendar.MINUTE));
        quickAlarmTime.setText("ساعت\n" + CalendarUtils.fa(time));
    }

    private void updateQuickNoteLabels() {
        int type = AppSettings.defaultCalendar(this);
        quickNoteDate.setText(CalendarUtils.formatDate(quickNoteDue.getTimeInMillis(), type));
        String time = String.format(Locale.US, "%02d:%02d",
                quickNoteDue.get(Calendar.HOUR_OF_DAY), quickNoteDue.get(Calendar.MINUTE));
        quickNoteTime.setText(CalendarUtils.fa(time));
    }

    private void saveQuickAlarm() {
        long trigger = quickAlarm.getTimeInMillis();
        if (trigger <= System.currentTimeMillis() && alarmRecurrenceMode == RecurrenceUtils.NONE) {
            Toast.makeText(this, "تاریخ و ساعت باید در آینده باشد", Toast.LENGTH_LONG).show();
            return;
        }

        long id = System.currentTimeMillis();
        int compatRepeat = alarmRecurrenceMode <= RecurrenceUtils.YEARLY ? alarmRecurrenceMode : AlarmItem.REPEAT_NONE;
        AlarmItem item = new AlarmItem(
                id,
                quickAlarmLabel.getText().toString().trim(),
                trigger,
                compatRepeat,
                true,
                quickAlarmVibrate.isChecked(),
                quickAlarmPriority.getSelectedItemPosition(),
                alarmRecurrenceMode,
                alarmIntervalDays,
                alarmCustomDates,
                15
        );

        new AlarmStore(this).save(item);
        boolean scheduled = AlarmScheduler.schedule(this, item);
        ClockWidgetProvider.updateAll(this);
        renderAlarms();

        quickAlarmLabel.setText("");
        quickAlarmPriority.setSelection(AlarmItem.PRIORITY_NORMAL);
        alarmRecurrenceMode = RecurrenceUtils.NONE;
        alarmIntervalDays = 1;
        alarmCustomDates = "[]";
        quickAlarmRepeat.setText("بدون تکرار");

        quickAlarm.setTimeInMillis(System.currentTimeMillis());
        quickAlarm.add(Calendar.MINUTE, 1);
        quickAlarm.set(Calendar.SECOND, 0);
        quickAlarm.set(Calendar.MILLISECOND, 0);
        clockCalendar.setCalendarType(AppSettings.defaultCalendar(this));
        clockCalendar.setSelectedMillis(quickAlarm.getTimeInMillis());
        updateQuickAlarmLabels();

        if (!scheduled && Build.VERSION.SDK_INT >= 31 && !PermissionHelper.exactAlarmsGranted(this)) {
            Toast.makeText(this, "زنگ ذخیره شد؛ دسترسی آلارم دقیق را فعال کنید.", Toast.LENGTH_LONG).show();
            startPermissionFlow();
        } else {
            Toast.makeText(this, "زنگ ذخیره شد", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQuickNote() {
        String title = quickNoteTitle.getText().toString().trim();
        String body = quickNoteBody.getText().toString().trim();
        String sketch = quickNoteSketch.serialize();

        if (title.isEmpty() && body.isEmpty() && "[]".equals(sketch)) {
            Toast.makeText(this, "یک متن یا نقاشی وارد کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean alarmEnabled = quickNoteAlarmSwitch.isChecked();
        long due = alarmEnabled ? quickNoteDue.getTimeInMillis() : 0L;
        if (alarmEnabled && due <= System.currentTimeMillis() && noteRecurrenceMode == RecurrenceUtils.NONE) {
            Toast.makeText(this, "زمان آلارم یادداشت باید در آینده باشد", Toast.LENGTH_LONG).show();
            return;
        }

        long now = System.currentTimeMillis();
        NoForgetItem item = new NoForgetItem(
                now,
                title,
                body,
                sketch,
                quickNotePriority.getSelectedItemPosition(),
                alarmEnabled,
                due,
                alarmEnabled,
                now,
                noteRecurrenceMode,
                noteIntervalDays,
                noteCustomDates
        );
        new NoForgetStore(this).save(item);
        if (alarmEnabled) NoForgetScheduler.schedule(this, item);
        NoForgetWidgetProvider.updateAll(this);

        quickNoteTitle.setText("");
        quickNoteBody.setText("");
        quickNotePriority.setSelection(NoForgetItem.PRIORITY_NORMAL);
        quickNoteAlarmSwitch.setChecked(false);
        quickNoteSketch.clearSketch();
        noteRecurrenceMode = RecurrenceUtils.NONE;
        noteIntervalDays = 1;
        noteCustomDates = "[]";
        quickNoteRepeat.setText("بدون تکرار");
        renderNoForget();
        Toast.makeText(this, "یادداشت ذخیره شد", Toast.LENGTH_SHORT).show();
    }

    @Override protected void onResume() {
        super.onResume();
        NotificationHelper.ensureChannels(this);
        try { DateNotificationService.start(this); } catch (Exception ignored) {}
        AlarmScheduler.rescheduleAll(this);
        NoForgetScheduler.rescheduleAll(this);
        renderAlarms();
        renderNoForget();

        if (waitingForSettings) {
            waitingForSettings = false;
            getWindow().getDecorView().postDelayed(this::advancePermissionFlow, 300);
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SETTINGS && resultCode == RESULT_OK) recreate();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showTab("noforget".equals(intent.getStringExtra("openTab")) ? "noforget" : "clock");
    }

    private void showTab(String tab) {
        boolean clock = !"noforget".equals(tab);
        clockPanel.setVisibility(clock ? View.VISIBLE : View.GONE);
        noForgetPanel.setVisibility(clock ? View.GONE : View.VISIBLE);
        clockIndicator.setVisibility(clock ? View.VISIBLE : View.INVISIBLE);
        noForgetIndicator.setVisibility(clock ? View.INVISIBLE : View.VISIBLE);
        clockTab.setTextColor(clock ? 0xFFFFFFFF : 0xFFD6EFED);
        noForgetTab.setTextColor(clock ? 0xFFD6EFED : 0xFFFFFFFF);
        appTitle.setText(clock ? "ساعت پیشرفته" : "یادداشت‌ها");
    }

    private void startPermissionFlow() {
        permissionStage = 0;
        waitingForSettings = false;
        advancePermissionFlow();
    }

    private void advancePermissionFlow() {
        if (permissionStage < 0) return;

        if (permissionStage == 0) {
            permissionStage = 1;
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
                return;
            }
        }

        if (permissionStage == 1) {
            permissionStage = 2;
            if (Build.VERSION.SDK_INT >= 31 && !PermissionHelper.exactAlarmsGranted(this)) {
                try {
                    waitingForSettings = true;
                    startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())));
                    return;
                } catch (Exception ignored) {
                    waitingForSettings = false;
                }
            }
        }

        if (permissionStage == 2) {
            permissionStage = 3;
            if (Build.VERSION.SDK_INT >= 34 && !PermissionHelper.fullScreenGranted(this)) {
                try {
                    waitingForSettings = true;
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName())));
                    return;
                } catch (Exception ignored) {
                    waitingForSettings = false;
                }
            }
        }

        permissionStage = -1;
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putBoolean(PERMISSION_ONBOARDING, true).apply();

        if (PermissionHelper.exactAlarmsGranted(this)) {
            AlarmScheduler.rescheduleAll(this);
            NoForgetScheduler.rescheduleAll(this);
        }
        try { DateNotificationService.start(this); } catch (Exception ignored) {}
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_NOTIFICATIONS && permissionStage >= 0) {
            getWindow().getDecorView().post(this::advancePermissionFlow);
        }
    }

    private void pinWidget(Class<?> provider) {
        if (Build.VERSION.SDK_INT < 26) {
            Toast.makeText(this, "ویجت را از فهرست ویجت‌های لانچر اضافه کنید", Toast.LENGTH_LONG).show();
            return;
        }
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(this, "ویجت را از فهرست ویجت‌های لانچر اضافه کنید", Toast.LENGTH_LONG).show();
            return;
        }
        manager.requestPinAppWidget(new ComponentName(this, provider), null, null);
    }

    private void renderAlarms() {
        alarmList.removeAllViews();
        List<AlarmItem> items = new AlarmStore(this).all();
        if (items.isEmpty()) {
            alarmList.addView(emptyText("هنوز زنگی تنظیم نشده است."));
            return;
        }
        for (AlarmItem item : items) alarmList.addView(alarmCard(item));
    }

    private LinearLayout alarmCard(AlarmItem item) {
        int strokeColor = item.priority == AlarmItem.PRIORITY_HIGH ? 0xFFE69A9A
                : item.priority == AlarmItem.PRIORITY_LOW ? 0xFFBFD9C8 : 0xFFD7E4E1;
        LinearLayout card = baseCard(strokeColor);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = cardTitle(item.label.trim().isEmpty() ? "زنگ هشدار" : item.label);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView state = smallText(item.enabled ? "فعال" : "خاموش");
        state.setTextColor(item.enabled ? AppSettings.primaryColor(this) : AppSettings.textSecondary(this));
        state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(state);
        card.addView(top);

        String priority = item.priority == 2 ? "اهمیت زیاد" : item.priority == 0 ? "اهمیت کم" : "اهمیت عادی";
        TextView time = smallText(formatAppDateTime(item.triggerAtMillis)
                + "  •  " + RecurrenceUtils.summary(item.recurrenceMode, item.intervalDays, item.customDatesJson)
                + "  •  " + priority + (item.vibrate ? "  •  لرزش" : ""));
        time.setPadding(0, dp(6), 0, dp(10));
        card.addView(time);

        LinearLayout actions = actionRow();
        Button enabled = actionButton(item.enabled ? "خاموش" : "فعال");
        enabled.setOnClickListener(v -> {
            item.enabled = !item.enabled;
            new AlarmStore(this).save(item);
            if (item.enabled) AlarmScheduler.schedule(this, item);
            else AlarmScheduler.cancel(this, item.id);
            ClockWidgetProvider.updateAll(this);
            renderAlarms();
        });

        Button edit = actionButton("ویرایش");
        edit.setOnClickListener(v -> startActivity(new Intent(this, AlarmEditorActivity.class)
                .putExtra("alarmId", item.id)));

        Button delete = actionButton("حذف");
        delete.setOnClickListener(v -> {
            AlarmScheduler.cancel(this, item.id);
            new AlarmStore(this).delete(item.id);
            ClockWidgetProvider.updateAll(this);
            renderAlarms();
        });

        actions.addView(enabled);
        actions.addView(edit);
        actions.addView(delete);
        card.addView(actions);
        return card;
    }

    private void renderNoForget() {
        noForgetList.removeAllViews();
        List<NoForgetItem> items = new NoForgetStore(this).all();
        if (items.isEmpty()) {
            noForgetList.addView(emptyText("هنوز یادداشتی ندارید."));
            return;
        }
        long now = System.currentTimeMillis();
        for (NoForgetItem item : items) noForgetList.addView(noteCard(item, now));
    }

    private LinearLayout noteCard(NoForgetItem item, long now) {
        int urgency = item.urgency(now);
        int strokeColor = urgency >= 3 ? 0xFFE69A9A : urgency == 2 ? 0xFFE8C58C : 0xFFBFD9C8;
        LinearLayout card = baseCard(strokeColor);

        String titleText = item.title.trim().isEmpty()
                ? (item.body.trim().isEmpty() ? "دست‌نویس" : item.body) : item.title;
        card.addView(cardTitle(titleText));

        if (!item.body.trim().isEmpty() && !item.body.equals(titleText)) {
            TextView body = smallText(item.body);
            body.setMaxLines(2);
            body.setEllipsize(android.text.TextUtils.TruncateAt.END);
            body.setPadding(0, dp(5), 0, dp(4));
            card.addView(body);
        }

        StringBuilder meta = new StringBuilder();
        meta.append(item.priority == 2 ? "اهمیت زیاد" : item.priority == 0 ? "اهمیت کم" : "اهمیت عادی");
        if (item.hasDue) meta.append("  •  ").append(formatAppDateTime(item.dueAtMillis));
        if (item.reminderEnabled) meta.append("  •  آلارم");
        if (item.recurrenceMode != RecurrenceUtils.NONE) {
            meta.append("  •  ").append(RecurrenceUtils.summary(item.recurrenceMode, item.intervalDays, item.customDatesJson));
        }
        if (!"[]".equals(item.sketchJson)) meta.append("  •  نقاشی");
        TextView metaView = smallText(meta.toString());
        metaView.setPadding(0, dp(5), 0, dp(9));
        card.addView(metaView);

        LinearLayout actions = actionRow();
        Button edit = actionButton("ویرایش");
        edit.setOnClickListener(v -> startActivity(new Intent(this, NoForgetEditorActivity.class)
                .putExtra("noteId", item.id)));

        Button delete = actionButton("حذف");
        delete.setOnClickListener(v -> {
            NoForgetScheduler.cancel(this, item.id);
            new NoForgetStore(this).delete(item.id);
            NoForgetWidgetProvider.updateAll(this);
            renderNoForget();
        });

        actions.addView(edit);
        actions.addView(delete);
        card.addView(actions);
        return card;
    }

    private LinearLayout baseCard(int strokeColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppSettings.surface(this));
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), strokeColor);
        card.setBackground(bg);
        return card;
    }

    private TextView cardTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(17);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.START);
        return title;
    }

    private TextView smallText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(AppSettings.textSecondary(this));
        view.setGravity(Gravity.START);
        return view;
    }

    private TextView emptyText(String text) {
        TextView empty = smallText(text);
        empty.setTextSize(15);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(16), dp(28), dp(16), dp(28));
        return empty;
    }

    private LinearLayout actionRow() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        actions.setGravity(Gravity.START);
        return actions;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setPadding(dp(12), 0, dp(12), 0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppSettings.field(this));
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(1), AppSettings.themeMode(this) == AppSettings.THEME_DARK ? 0xFF343D3A : 0xFFD8E8E7);
        button.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        params.setMargins(0, 0, dp(6), 0);
        button.setLayoutParams(params);
        return button;
    }

    private String formatAppDateTime(long millis) {
        String time = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new java.util.Date(millis));
        return CalendarUtils.formatDate(millis, AppSettings.defaultCalendar(this)) + "  " + time;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
