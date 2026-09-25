package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQ_NOTIFICATIONS = 100;
    private static final int REQ_SETTINGS = 200;
    private static final int REQ_NOTIFICATION_SETTINGS = 201;
    private static final int REQ_QUICK_ALARM_IMAGE_1 = 310;
    private static final int REQ_QUICK_ALARM_IMAGE_2 = 311;
    private static final String PREFS = "advance_clock_app";
    private static final String PERMISSION_ONBOARDING = "permission_onboarding_v2";
    private static final String INITIAL_SETUP = "language_calendar_setup_v1";

    private final Calendar quickAlarm = Calendar.getInstance();
    private final Calendar quickNoteDue = Calendar.getInstance();

    private final Handler headerHandler = new Handler(Looper.getMainLooper());
    private final Runnable headerTicker = new Runnable() {
        @Override public void run() {
            updateHeaderClock();
            long now = System.currentTimeMillis();
            long delay = 60_000L - (now % 60_000L) + 60L;
            headerHandler.postDelayed(this, delay);
        }
    };

    private LinearLayout alarmList;
    private LinearLayout noForgetList;
    private LinearLayout clockContent;
    private View alarmComposerCard;
    private View noteComposerCard;
    private View noteSketchCard;
    private View alertsHeader;
    private ScrollView clockPanel;
    private View noForgetPanel;
    private View stopwatchPanel;
    private View timerPanel;
    private View worldPanel;
    private StopwatchPanelController stopwatchController;
    private TimerPanelController timerController;
    private WorldClockPanelController worldController;
    private TextView clockTab;
    private TextView noForgetTab;
    private View clockIndicator;
    private View noForgetIndicator;
    private View stopwatchIndicator;
    private View timerIndicator;
    private View worldIndicator;
    private TextView headerTime;
    private TextView headerDate;
    private ImageButton themeToggle;
    private Button clockFab;
    private Button noteFab;

    private Button quickAlarmDate;
    private Button quickAlarmTime;
    private Button quickAlarmRepeat;
    private Button quickAlarmReminders;
    private Button quickAlarmImage1;
    private Button quickAlarmImage2;
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
    private Spinner penSizeSpinner;
    private ImageButton gridToggle;

    private int quickAlarmCalendarType;
    private int quickNoteCalendarType;

    private int alarmRecurrenceMode = RecurrenceUtils.NONE;
    private int alarmIntervalDays = 1;
    private String alarmCustomDates = "[]";
    private int alarmReminderMode = AlarmReminderUtils.MODE_NONE;
    private String alarmReminderMinutesJson = "[]";
    private String quickAlarmImageUri1 = "";
    private String quickAlarmImageUri2 = "";

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
        stopwatchController = new StopwatchPanelController(this, stopwatchPanel);
        timerController = new TimerPanelController(this, timerPanel);
        worldController = new WorldClockPanelController(this, worldPanel);
        configureHeaderForDisplayCutout();
        quickAlarmCalendarType = AppSettings.defaultCalendar(this);
        quickNoteCalendarType = AppSettings.defaultCalendar(this);
        setupHeader();
        setupAlarmComposer();
        setupNoteComposer();
        setupCalendars();
        applyClockLayoutMode();

        clockTab.setOnClickListener(v -> showTab("clock"));
        noForgetTab.setOnClickListener(v -> showTab("noforget"));
        findViewById(R.id.tab_stopwatch).setOnClickListener(v -> showTab("stopwatch"));
        findViewById(R.id.tab_timer).setOnClickListener(v -> showTab("timer"));
        findViewById(R.id.tab_world).setOnClickListener(v -> showTab("world"));
        applyTabOrder();
        applyTabVisibility();

        findViewById(R.id.add_clock_widget).setOnClickListener(v ->
                pinWidgetAndExit(ClockWidgetProvider.class));
        findViewById(R.id.add_noforget_widget).setOnClickListener(v ->
                pinWidgetAndExit(NoForgetWidgetProvider.class));
        findViewById(R.id.world_add_widget).setOnClickListener(v ->
                pinWidgetAndExit(WorldClockWidgetProvider.class));

        clockFab.setOnClickListener(v ->
                startActivity(new Intent(this, AlarmEditorActivity.class)
                        .putExtra("modalCreate", true)));
        noteFab.setOnClickListener(v ->
                startActivity(new Intent(this, NoForgetEditorActivity.class)
                        .putExtra("modalCreate", true)));

        String requestedTab = getIntent().getStringExtra("openTab");
        showTab("noforget".equals(requestedTab)
                || "stopwatch".equals(requestedTab)
                || "timer".equals(requestedTab)
                || "world".equals(requestedTab)
                ? requestedTab : firstEnabledTab());

        android.content.SharedPreferences appPrefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        boolean setupDone = appPrefs.getBoolean(INITIAL_SETUP, false);
        boolean onboardingDone = appPrefs.getBoolean(PERMISSION_ONBOARDING, false);
        if (!setupDone) {
            getWindow().getDecorView().postDelayed(() ->
                    FirstRunSetupDialog.show(this, languageChanged -> {
                        appPrefs.edit().putBoolean(INITIAL_SETUP, true).apply();
                        if (languageChanged) recreate();
                        else if (!onboardingDone) startPermissionFlow();
                    }), 300);
        } else if (!onboardingDone) {
            getWindow().getDecorView().postDelayed(this::startPermissionFlow, 450);
        }

        headerHandler.post(headerTicker);
    }

    private void bindViews() {
        alarmList = findViewById(R.id.alarm_list);
        noForgetList = findViewById(R.id.noforget_list);
        clockContent = findViewById(R.id.clock_content);
        alarmComposerCard = findViewById(R.id.alarm_composer_card);
        noteComposerCard = findViewById(R.id.note_composer_card);
        noteSketchCard = findViewById(R.id.note_sketch_card);
        alertsHeader = findViewById(R.id.alerts_header);
        clockPanel = findViewById(R.id.clock_panel);
        noForgetPanel = findViewById(R.id.noforget_panel);
        stopwatchPanel = findViewById(R.id.stopwatch_panel);
        timerPanel = findViewById(R.id.timer_panel);
        worldPanel = findViewById(R.id.world_panel);
        clockTab = findViewById(R.id.tab_clock);
        noForgetTab = findViewById(R.id.tab_noforget);
        clockIndicator = findViewById(R.id.clock_indicator);
        noForgetIndicator = findViewById(R.id.noforget_indicator);
        stopwatchIndicator = findViewById(R.id.stopwatch_indicator);
        timerIndicator = findViewById(R.id.timer_indicator);
        worldIndicator = findViewById(R.id.world_indicator);
        headerTime = findViewById(R.id.header_time);
        headerDate = findViewById(R.id.header_date);
        themeToggle = findViewById(R.id.theme_toggle);
        clockFab = findViewById(R.id.clock_fab);
        noteFab = findViewById(R.id.note_fab);

        quickAlarmDate = findViewById(R.id.quick_alarm_date);
        quickAlarmTime = findViewById(R.id.quick_alarm_time);
        quickAlarmRepeat = findViewById(R.id.quick_alarm_repeat);
        quickAlarmReminders = findViewById(R.id.quick_alarm_reminders);
        quickAlarmImage1 = findViewById(R.id.quick_alarm_image1);
        quickAlarmImage2 = findViewById(R.id.quick_alarm_image2);
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
        penSizeSpinner = findViewById(R.id.pen_size_spinner);
        gridToggle = findViewById(R.id.grid_toggle);
    }

    private void configureHeaderForDisplayCutout() {
        View header = findViewById(R.id.header_root);
        int baseHeight = dp(132);
        int basePaddingTop = dp(8);
        int paddingStart = header.getPaddingStart();
        int paddingEnd = header.getPaddingEnd();
        int paddingBottom = header.getPaddingBottom();

        header.setOnApplyWindowInsetsListener((view, insets) -> {
            int cutoutInsetTop = 0;
            if (Build.VERSION.SDK_INT >= 35 && insets.getDisplayCutout() != null) {
                cutoutInsetTop = insets.getDisplayCutout().getSafeInsetTop();
            }

            view.setPaddingRelative(
                    paddingStart,
                    basePaddingTop + cutoutInsetTop,
                    paddingEnd,
                    paddingBottom);
            ViewGroup.LayoutParams params = view.getLayoutParams();
            int requiredHeight = baseHeight + cutoutInsetTop;
            if (params.height != requiredHeight) {
                params.height = requiredHeight;
                view.setLayoutParams(params);
            }
            return insets;
        });
        header.requestApplyInsets();
    }

    private void setupHeader() {
        updateThemeIcon();

        themeToggle.setOnClickListener(v -> {
            int next = AppSettings.themeMode(this) == AppSettings.THEME_DARK
                    ? AppSettings.THEME_LIGHT : AppSettings.THEME_DARK;
            AppSettings.setThemeMode(this, next);
            recreate();
        });

        findViewById(R.id.header_menu).setOnClickListener(anchor -> {
            PopupMenu menu = new PopupMenu(this, anchor);
            menu.getMenu().add(0, 1, 0, "تنظیمات");
            menu.getMenu().add(0, 4, 1, "تنظیمات اعلان");
            menu.getMenu().add(0, 5, 2, "ویجت‌ها و تنظیمات");
            menu.getMenu().add(0, 6, 3, "جابه‌جایی ترتیب تب‌ها");
            menu.getMenu().add(0, 3, 4, "مجوزهای آلارم و اعلان");
            menu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == 1) {
                    startActivityForResult(new Intent(this, SettingsActivity.class), REQ_SETTINGS);
                    return true;
                }
                if (item.getItemId() == 4) {
                    startActivityForResult(
                            new Intent(this, NotificationSettingsActivity.class),
                            REQ_NOTIFICATION_SETTINGS);
                    return true;
                }
                if (item.getItemId() == 5) {
                    startActivity(new Intent(this, WidgetCenterActivity.class));
                    return true;
                }
                if (item.getItemId() == 6) {
                    TabOrderDialog.show(this, () -> {
                        applyTabOrder();
                        applyTabVisibility();
                    });
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

    private void updateThemeIcon() {
        themeToggle.setImageResource(
                AppSettings.themeMode(this) == AppSettings.THEME_DARK
                        ? R.drawable.ic_md_light
                        : R.drawable.ic_md_dark);
    }

    private void updateHeaderClock() {
        long now = System.currentTimeMillis();
        String time = new java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new java.util.Date(now));
        headerTime.setText(CalendarUtils.fa(time));
        headerDate.setText(
                CalendarUtils.formatDate(now, AppSettings.defaultCalendar(this)));
    }

    private void setupCalendars() {
        View root = findViewById(R.id.root_main);
        root.post(() -> {
            int calendarWidth = Math.round(root.getWidth() * 0.98f);
            applyCalendarWidth(clockCalendar, calendarWidth);
            applyCalendarWidth(noteCalendar, calendarWidth);
        });

        int type = AppSettings.defaultCalendar(this);
        clockCalendar.setCalendarType(type);
        noteCalendar.setCalendarType(type);

        TripleCalendarView.OnMonthYearClickListener monthClick =
                (visible, calendarType) -> CalendarPickerDialog.showMonthYear(
                        this,
                        visible,
                        calendarType,
                        (picked, pickedType) -> {
                            if (clockPanel.getVisibility() == View.VISIBLE) {
                                clockCalendar.setVisibleMonthMillis(picked, pickedType);
                            } else {
                                noteCalendar.setVisibleMonthMillis(picked, pickedType);
                            }
                        });

        clockCalendar.setOnMonthYearClickListener(monthClick);
        noteCalendar.setOnMonthYearClickListener(monthClick);

        clockCalendar.setOnDateSelectedListener(millis -> {
            if (millis < startOfToday()) {
                Toast.makeText(this, "تاریخ گذشته قابل انتخاب نیست", Toast.LENGTH_SHORT).show();
                return;
            }
            quickAlarmCalendarType = clockCalendar.getCalendarType();
            applyDate(quickAlarm, millis);
            updateQuickAlarmLabels();
        });

        noteCalendar.setOnDateSelectedListener(millis -> {
            if (millis < startOfToday()) {
                Toast.makeText(this, "تاریخ گذشته قابل انتخاب نیست", Toast.LENGTH_SHORT).show();
                return;
            }
            quickNoteCalendarType = noteCalendar.getCalendarType();
            applyDate(quickNoteDue, millis);
            updateQuickNoteLabels();
        });
    }

    private void applyCalendarWidth(TripleCalendarView calendar, int width) {
        LinearLayout.LayoutParams params =
                (LinearLayout.LayoutParams) calendar.getLayoutParams();
        params.width = width;
        params.gravity = Gravity.CENTER_HORIZONTAL;
        calendar.setLayoutParams(params);
    }

    private void setupAlarmComposer() {
        setPrioritySpinner(quickAlarmPriority, PriorityUtils.MEDIUM);

        quickAlarm.add(Calendar.MINUTE, 1);
        quickAlarm.set(Calendar.SECOND, 0);
        quickAlarm.set(Calendar.MILLISECOND, 0);
        updateQuickAlarmLabels();
        updateAlarmReminderLabel();

        quickAlarmDate.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this,
                quickAlarm.getTimeInMillis(),
                quickAlarmCalendarType,
                (picked, type) -> {
                    quickAlarmCalendarType = type;
                    applyDate(quickAlarm, picked);
                    clockCalendar.setCalendarType(type);
                    clockCalendar.setSelectedMillis(quickAlarm.getTimeInMillis());
                    updateQuickAlarmLabels();
                }));

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
                true).show());

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
                    quickAlarmRepeat.setText(
                            RecurrenceUtils.summary(mode, interval, dates));
                }));

        quickAlarmReminders.setOnClickListener(v -> AlarmReminderDialog.show(
                this,
                alarmReminderMode,
                alarmReminderMinutesJson,
                (mode, json) -> {
                    alarmReminderMode = mode;
                    alarmReminderMinutesJson = json;
                    updateAlarmReminderLabel();
                }));

        quickAlarmImage1.setOnClickListener(v -> pickAlarmImage(REQ_QUICK_ALARM_IMAGE_1));
        quickAlarmImage2.setOnClickListener(v -> pickAlarmImage(REQ_QUICK_ALARM_IMAGE_2));

        findViewById(R.id.save_quick_alarm).setOnClickListener(v -> saveQuickAlarm());
    }

    private void updateAlarmReminderLabel() {
        quickAlarmReminders.setText(
                "یادآوری\n"
                        + AlarmReminderUtils.summary(
                        alarmReminderMode, alarmReminderMinutesJson));
    }

    private void setupNoteComposer() {
        setPrioritySpinner(quickNotePriority, PriorityUtils.MEDIUM);

        quickNoteDue.add(Calendar.HOUR_OF_DAY, 1);
        quickNoteDue.set(Calendar.SECOND, 0);
        quickNoteDue.set(Calendar.MILLISECOND, 0);
        updateQuickNoteLabels();

        quickNoteAlarmSwitch.setOnCheckedChangeListener((button, checked) ->
                quickNoteAlarmControls.setVisibility(
                        checked ? View.VISIBLE : View.GONE));

        quickNoteDate.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this,
                quickNoteDue.getTimeInMillis(),
                quickNoteCalendarType,
                (picked, type) -> {
                    quickNoteCalendarType = type;
                    applyDate(quickNoteDue, picked);
                    noteCalendar.setCalendarType(type);
                    noteCalendar.setSelectedMillis(quickNoteDue.getTimeInMillis());
                    updateQuickNoteLabels();
                }));

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
                true).show());

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
                    quickNoteRepeat.setText(
                            RecurrenceUtils.summary(mode, interval, dates));
                }));

        findViewById(R.id.save_quick_note).setOnClickListener(v -> saveQuickNote());
        findViewById(R.id.note_undo).setOnClickListener(v -> quickNoteSketch.undo());
        findViewById(R.id.note_clear).setOnClickListener(v -> quickNoteSketch.clearSketch());
        findViewById(R.id.note_redo).setOnClickListener(v -> quickNoteSketch.redo());
        findViewById(R.id.note_palette).setOnClickListener(v ->
                PaletteDialog.show(
                        this,
                        quickNoteSketch.getPenColor(),
                        quickNoteSketch::setPenColor));

        penSizeSpinner.setAdapter(new PenSizeAdapter(this));
        penSizeSpinner.setSelection(1);
        penSizeSpinner.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        float[] widths = {2f, 4f, 7f, 10f};
                        quickNoteSketch.setPenWidthDp(
                                widths[Math.max(0, Math.min(widths.length - 1, position))]);
                    }
                    @Override public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        final boolean[] gridVisible = {true};
        quickNoteSketch.setGridVisible(true);
        gridToggle.setImageResource(R.drawable.ic_grid);
        gridToggle.setSelected(false);
        gridToggle.setOnClickListener(v -> {
            gridVisible[0] = !gridVisible[0];
            quickNoteSketch.setGridVisible(gridVisible[0]);
            gridToggle.setSelected(!gridVisible[0]);
            gridToggle.setImageResource(
                    gridVisible[0] ? R.drawable.ic_grid : R.drawable.ic_grid_off);
        });
    }

    private void setPrioritySpinner(Spinner spinner, int selection) {
        String[] source = PriorityUtils.labels();
        String[] values = new String[source.length];
        for (int i = 0; i < source.length; i++) {
            values[i] = "اهمیت " + source[i];
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(PriorityUtils.clamp(selection));
    }

    private void applyClockLayoutMode() {
        clockContent.removeView(alarmComposerCard);
        clockContent.removeView(clockCalendar);
        clockContent.removeView(alertsHeader);
        clockContent.removeView(alarmList);

        boolean compact = AppSettings.clockLayoutMode(this)
                == AppSettings.CLOCK_LAYOUT_CALENDAR_FIRST;

        if (compact) {
            clockContent.addView(clockCalendar);
            clockContent.addView(alertsHeader);
            clockContent.addView(alarmList);
            clockContent.addView(alarmComposerCard);
            alarmComposerCard.setVisibility(View.GONE);
            noteComposerCard.setVisibility(View.GONE);
            noteSketchCard.setVisibility(View.GONE);
        } else {
            clockContent.addView(alarmComposerCard);
            clockContent.addView(clockCalendar);
            clockContent.addView(alertsHeader);
            clockContent.addView(alarmList);
            alarmComposerCard.setVisibility(View.VISIBLE);
            noteComposerCard.setVisibility(View.VISIBLE);
            noteSketchCard.setVisibility(View.VISIBLE);
        }
    }

    private void applyDate(Calendar target, long sourceMillis) {
        Calendar source = Calendar.getInstance();
        source.setTimeInMillis(sourceMillis);
        target.set(Calendar.YEAR, source.get(Calendar.YEAR));
        target.set(Calendar.MONTH, source.get(Calendar.MONTH));
        target.set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH));
    }

    private void updateQuickAlarmLabels() {
        int type = quickAlarmCalendarType;
        quickAlarmDate.setText(
                "تاریخ\n"
                        + CalendarUtils.formatDate(
                        quickAlarm.getTimeInMillis(), type));
        String time = String.format(
                Locale.US,
                "%02d:%02d",
                quickAlarm.get(Calendar.HOUR_OF_DAY),
                quickAlarm.get(Calendar.MINUTE));
        quickAlarmTime.setText("ساعت\n" + CalendarUtils.fa(time));
    }

    private void updateQuickNoteLabels() {
        int type = quickNoteCalendarType;
        quickNoteDate.setText(
                CalendarUtils.formatDate(
                        quickNoteDue.getTimeInMillis(), type));
        String time = String.format(
                Locale.US,
                "%02d:%02d",
                quickNoteDue.get(Calendar.HOUR_OF_DAY),
                quickNoteDue.get(Calendar.MINUTE));
        quickNoteTime.setText(CalendarUtils.fa(time));
    }

    private void saveQuickAlarm() {
        long trigger = quickAlarm.getTimeInMillis();
        if (trigger <= System.currentTimeMillis()) {
            Toast.makeText(
                    this,
                    "هشدار را نمی‌توان برای تاریخ یا ساعت گذشته تنظیم کرد",
                    Toast.LENGTH_LONG).show();
            return;
        }

        long id = System.currentTimeMillis();
        int compatRepeat = alarmRecurrenceMode <= RecurrenceUtils.YEARLY
                ? alarmRecurrenceMode
                : AlarmItem.REPEAT_NONE;

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
                15,
                alarmReminderMode,
                alarmReminderMinutesJson);
        item.imageUri1 = quickAlarmImageUri1;
        item.imageUri2 = quickAlarmImageUri2;

        new AlarmStore(this).save(item);
        boolean scheduled = AlarmScheduler.schedule(this, item);
        ClockWidgetProvider.updateAll(this);
        renderAlarms();

        quickAlarmLabel.setText("");
        quickAlarmPriority.setSelection(PriorityUtils.MEDIUM);
        alarmRecurrenceMode = RecurrenceUtils.NONE;
        alarmIntervalDays = 1;
        alarmCustomDates = "[]";
        quickAlarmRepeat.setText("بدون تکرار");
        alarmReminderMode = AlarmReminderUtils.MODE_NONE;
        alarmReminderMinutesJson = "[]";
        quickAlarmImageUri1 = "";
        quickAlarmImageUri2 = "";
        quickAlarmImage1.setText("افزودن عکس ۱");
        quickAlarmImage2.setText("افزودن عکس ۲");
        updateAlarmReminderLabel();

        quickAlarm.setTimeInMillis(System.currentTimeMillis());
        quickAlarm.add(Calendar.MINUTE, 1);
        quickAlarm.set(Calendar.SECOND, 0);
        quickAlarm.set(Calendar.MILLISECOND, 0);

        quickAlarmCalendarType = AppSettings.defaultCalendar(this);
        clockCalendar.setCalendarType(quickAlarmCalendarType);
        clockCalendar.setSelectedMillis(quickAlarm.getTimeInMillis());
        updateQuickAlarmLabels();


        if (!scheduled
                && Build.VERSION.SDK_INT >= 31
                && !PermissionHelper.exactAlarmsGranted(this)) {
            Toast.makeText(
                    this,
                    "هشدار ذخیره شد؛ دسترسی آلارم دقیق را فعال کنید.",
                    Toast.LENGTH_LONG).show();
            startPermissionFlow();
        } else {
            Toast.makeText(this, "هشدار ذخیره شد", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQuickNote() {
        String title = quickNoteTitle.getText().toString().trim();
        String body = quickNoteBody.getText().toString().trim();
        String sketch = quickNoteSketch.serialize();

        if (title.isEmpty() && body.isEmpty() && "[]".equals(sketch)) {
            Toast.makeText(
                    this,
                    "یک متن یا نقاشی وارد کنید",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean alarmEnabled = quickNoteAlarmSwitch.isChecked();
        long due = alarmEnabled ? quickNoteDue.getTimeInMillis() : 0L;

        if (alarmEnabled && due <= System.currentTimeMillis()) {
            Toast.makeText(
                    this,
                    "آلارم یادداشت را نمی‌توان برای گذشته تنظیم کرد",
                    Toast.LENGTH_LONG).show();
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
                noteCustomDates);

        new NoForgetStore(this).save(item);
        if (alarmEnabled) NoForgetScheduler.schedule(this, item);
        NoForgetWidgetProvider.updateAll(this);

        quickNoteTitle.setText("");
        quickNoteBody.setText("");
        quickNotePriority.setSelection(PriorityUtils.MEDIUM);
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
        stopwatchController.onResume();
        timerController.onResume();
        worldController.onResume();
        applyTabOrder();
        applyTabVisibility();
        updateHeaderClock();
        NotificationHelper.ensureChannels(this);
        try { DateNotificationService.start(this); } catch (Exception ignored) {}
        AlarmScheduler.rescheduleAll(this);
        ToolAlarmScheduler.rescheduleAll(this);
        NoForgetScheduler.rescheduleAll(this);
        renderAlarms();
        renderNoForget();

        if (waitingForSettings) {
            waitingForSettings = false;
            getWindow().getDecorView().postDelayed(
                    this::advancePermissionFlow, 300);
        }
    }

    @Override protected void onPause() {
        stopwatchController.onPause();
        timerController.onPause();
        worldController.onPause();
        super.onPause();
    }

    @Override protected void onDestroy() {
        headerHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == REQ_SETTINGS || requestCode == REQ_NOTIFICATION_SETTINGS)
                && resultCode == RESULT_OK) {
            recreate();
            return;
        }

        if ((requestCode == REQ_QUICK_ALARM_IMAGE_1
                || requestCode == REQ_QUICK_ALARM_IMAGE_2)
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}

            if (requestCode == REQ_QUICK_ALARM_IMAGE_1) {
                quickAlarmImageUri1 = uri.toString();
                quickAlarmImage1.setText("عکس ۱ ✓");
            } else {
                quickAlarmImageUri2 = uri.toString();
                quickAlarmImage2.setText("عکس ۲ ✓");
            }
        }
    }

    private void pickAlarmImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String requestedTab = intent.getStringExtra("openTab");
        showTab("noforget".equals(requestedTab)
                || "stopwatch".equals(requestedTab)
                || "timer".equals(requestedTab)
                || "world".equals(requestedTab)
                ? requestedTab : firstEnabledTab());
    }

    private void showTab(String tab) {
        if (!AppSettings.tabEnabled(this, tab)) tab = firstEnabledTab();
        boolean clock = "clock".equals(tab);
        boolean notes = "noforget".equals(tab);
        boolean stopwatch = "stopwatch".equals(tab);
        boolean timer = "timer".equals(tab);
        boolean world = "world".equals(tab);

        clockPanel.setVisibility(clock ? View.VISIBLE : View.GONE);
        noForgetPanel.setVisibility(notes ? View.VISIBLE : View.GONE);
        stopwatchPanel.setVisibility(stopwatch ? View.VISIBLE : View.GONE);
        timerPanel.setVisibility(timer ? View.VISIBLE : View.GONE);
        worldPanel.setVisibility(world ? View.VISIBLE : View.GONE);
        clockIndicator.setVisibility(clock ? View.VISIBLE : View.INVISIBLE);
        noForgetIndicator.setVisibility(notes ? View.VISIBLE : View.INVISIBLE);
        stopwatchIndicator.setVisibility(stopwatch ? View.VISIBLE : View.INVISIBLE);
        timerIndicator.setVisibility(timer ? View.VISIBLE : View.INVISIBLE);
        worldIndicator.setVisibility(world ? View.VISIBLE : View.INVISIBLE);
        clockTab.setTextColor(clock ? 0xFFFFFFFF : 0xFFD6EFED);
        noForgetTab.setTextColor(notes ? 0xFFFFFFFF : 0xFFD6EFED);
        ((TextView) findViewById(R.id.tab_stopwatch)).setTextColor(
                stopwatch ? 0xFFFFFFFF : 0xFFD6EFED);
        ((TextView) findViewById(R.id.tab_timer)).setTextColor(
                timer ? 0xFFFFFFFF : 0xFFD6EFED);
        ((TextView) findViewById(R.id.tab_world)).setTextColor(
                world ? 0xFFFFFFFF : 0xFFD6EFED);
        boolean compact = AppSettings.clockLayoutMode(this)
                == AppSettings.CLOCK_LAYOUT_CALENDAR_FIRST;
        clockFab.setVisibility(clock && compact ? View.VISIBLE : View.GONE);
        noteFab.setVisibility(notes && compact ? View.VISIBLE : View.GONE);
    }

    private void applyTabOrder() {
        LinearLayout bar = findViewById(R.id.tab_bar);
        java.util.HashMap<String, View> tabs = new java.util.HashMap<>();
        tabs.put("clock", findViewById(R.id.tab_clock_container));
        tabs.put("noforget", findViewById(R.id.tab_noforget_container));
        tabs.put("stopwatch", findViewById(R.id.tab_stopwatch_container));
        tabs.put("timer", findViewById(R.id.tab_timer_container));
        tabs.put("world", findViewById(R.id.tab_world_container));
        bar.removeAllViews();
        for (String tab : AppSettings.tabOrder(this)) {
            View view = tabs.get(tab);
            if (view != null) bar.addView(view);
        }
    }

    private void applyTabVisibility() {
        findViewById(R.id.tab_clock_container).setVisibility(
                AppSettings.tabEnabled(this, "clock") ? View.VISIBLE : View.GONE);
        findViewById(R.id.tab_noforget_container).setVisibility(
                AppSettings.tabEnabled(this, "noforget") ? View.VISIBLE : View.GONE);
        findViewById(R.id.tab_stopwatch_container).setVisibility(
                AppSettings.tabEnabled(this, "stopwatch") ? View.VISIBLE : View.GONE);
        findViewById(R.id.tab_timer_container).setVisibility(
                AppSettings.tabEnabled(this, "timer") ? View.VISIBLE : View.GONE);
        findViewById(R.id.tab_world_container).setVisibility(
                AppSettings.tabEnabled(this, "world") ? View.VISIBLE : View.GONE);
    }

    private String firstEnabledTab() {
        for (String tab : AppSettings.tabOrder(this)) {
            if (AppSettings.tabEnabled(this, tab)) return tab;
        }
        AppSettings.setTabEnabled(this, "clock", true);
        return "clock";
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
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQ_NOTIFICATIONS);
                return;
            }
        }

        if (permissionStage == 1) {
            permissionStage = 2;
            if (Build.VERSION.SDK_INT >= 31
                    && !PermissionHelper.exactAlarmsGranted(this)) {
                try {
                    waitingForSettings = true;
                    startActivity(new Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())));
                    return;
                } catch (Exception ignored) {
                    waitingForSettings = false;
                }
            }
        }

        if (permissionStage == 2) {
            permissionStage = 3;
            if (Build.VERSION.SDK_INT >= 34
                    && !PermissionHelper.fullScreenGranted(this)) {
                try {
                    waitingForSettings = true;
                    startActivity(new Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName())));
                    return;
                } catch (Exception ignored) {
                    waitingForSettings = false;
                }
            }
        }

        permissionStage = -1;
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(PERMISSION_ONBOARDING, true)
                .apply();

        if (PermissionHelper.exactAlarmsGranted(this)) {
            AlarmScheduler.rescheduleAll(this);
            NoForgetScheduler.rescheduleAll(this);
        }
        try { DateNotificationService.start(this); } catch (Exception ignored) {}
    }

    @Override public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_NOTIFICATIONS && permissionStage >= 0) {
            getWindow().getDecorView().post(this::advancePermissionFlow);
        }
    }

    private void pinWidgetAndExit(Class<?> provider) {
        if (Build.VERSION.SDK_INT < 26) {
            Toast.makeText(
                    this,
                    "ویجت را از فهرست ویجت‌های لانچر اضافه کنید",
                    Toast.LENGTH_LONG).show();
            return;
        }

        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(
                    this,
                    "ویجت را از فهرست ویجت‌های لانچر اضافه کنید",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean opened = manager.requestPinAppWidget(
                new ComponentName(this, provider),
                null,
                null);

        Toast.makeText(
                this,
                opened
                        ? "درخواست افزودن ویجت ارسال شد؛ پس از تأیید لانچر، تنظیمات همان ویجت باز می‌شود."
                        : "لانچر درخواست افزودن ویجت را نپذیرفت.",
                Toast.LENGTH_SHORT).show();
    }

    private void renderAlarms() {
        alarmList.removeAllViews();
        List<AlarmItem> items = new AlarmStore(this).all();
        if (items.isEmpty()) {
            alarmList.addView(emptyText("هنوز هشداری تنظیم نشده است."));
            return;
        }

        for (AlarmItem item : items) {
            alarmList.addView(alarmCard(item));
        }
    }

    private LinearLayout alarmCard(AlarmItem item) {
        int strokeColor = priorityStroke(item.priority);
        LinearLayout card = baseCard(strokeColor);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = cardTitle(
                item.label.trim().isEmpty() ? "هشدار" : item.label);
        top.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f));

        TextView state = smallText(item.enabled ? "فعال" : "خاموش");
        state.setTextColor(
                item.enabled
                        ? AppSettings.primaryColor(this)
                        : AppSettings.textSecondary(this));
        state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(state);
        card.addView(top);

        String reminder = AlarmReminderUtils.summary(
                item.reminderMode,
                item.reminderMinutesJson);

        TextView time = smallText(
                formatAppDateTime(item.triggerAtMillis)
                        + "  •  "
                        + RecurrenceUtils.summary(
                        item.recurrenceMode,
                        item.intervalDays,
                        item.customDatesJson)
                        + "  •  اهمیت "
                        + PriorityUtils.label(item.priority)
                        + (item.reminderMode != AlarmReminderUtils.MODE_NONE
                        ? "  •  یادآوری " + reminder
                        : "")
                        + (item.vibrate ? "  •  لرزش" : ""));
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
        edit.setOnClickListener(v ->
                startActivity(new Intent(this, AlarmEditorActivity.class)
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

        for (NoForgetItem item : items) {
            noForgetList.addView(noteCard(item));
        }
    }

    private LinearLayout noteCard(NoForgetItem item) {
        LinearLayout card = baseCard(priorityStroke(item.priority));

        String titleText = item.title.trim().isEmpty()
                ? (item.body.trim().isEmpty() ? "دست‌نویس" : item.body)
                : item.title;
        card.addView(cardTitle(titleText));

        if (!item.body.trim().isEmpty() && !item.body.equals(titleText)) {
            TextView body = smallText(item.body);
            body.setMaxLines(2);
            body.setEllipsize(android.text.TextUtils.TruncateAt.END);
            body.setPadding(0, dp(5), 0, dp(4));
            card.addView(body);
        }

        StringBuilder meta = new StringBuilder();
        meta.append("اهمیت ").append(PriorityUtils.label(item.priority));
        if (item.hasDue) {
            meta.append("  •  ").append(formatAppDateTime(item.dueAtMillis));
        }
        if (item.reminderEnabled) meta.append("  •  آلارم");
        if (item.recurrenceMode != RecurrenceUtils.NONE) {
            meta.append("  •  ")
                    .append(RecurrenceUtils.summary(
                            item.recurrenceMode,
                            item.intervalDays,
                            item.customDatesJson));
        }
        if (!"[]".equals(item.sketchJson)) meta.append("  •  نقاشی");

        TextView metaView = smallText(meta.toString());
        metaView.setPadding(0, dp(5), 0, dp(9));
        card.addView(metaView);

        LinearLayout actions = actionRow();

        Button edit = actionButton("ویرایش");
        edit.setOnClickListener(v ->
                startActivity(new Intent(this, NoForgetEditorActivity.class)
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

    private int priorityStroke(int priority) {
        switch (PriorityUtils.clamp(priority)) {
            case PriorityUtils.VERY_HIGH: return 0xFFE26767;
            case PriorityUtils.HIGH: return 0xFFE9A276;
            case PriorityUtils.MEDIUM: return 0xFFE1C886;
            case PriorityUtils.RELATIVELY_LOW: return 0xFFAFCFBF;
            case PriorityUtils.LOW:
            default: return 0xFFC9D8D4;
        }
    }

    private LinearLayout baseCard(int strokeColor) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
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
        bg.setStroke(
                dp(1),
                AppSettings.themeMode(this) == AppSettings.THEME_DARK
                        ? 0xFF343D3A
                        : 0xFFD8E8E7);
        button.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(40));
        params.setMargins(0, 0, dp(6), 0);
        button.setLayoutParams(params);
        return button;
    }

    private String formatAppDateTime(long millis) {
        String time = new java.text.SimpleDateFormat(
                "HH:mm",
                Locale.getDefault())
                .format(new java.util.Date(millis));
        return CalendarUtils.formatDate(
                millis,
                AppSettings.defaultCalendar(this))
                + "  "
                + time;
    }

    private static long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }
}
