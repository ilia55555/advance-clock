package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends Activity {
    public static final String EXTRA_SECTION = "settings_section";
    private final java.util.List<SettingsSection> sections = new java.util.ArrayList<>();
    private String expandedSection = "";
    private NotificationSettingsPanel notificationPanel;
    private PrayerSettingsPanel prayerPanel;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        expandedSection = savedInstanceState != null
                ? savedInstanceState.getString(EXTRA_SECTION, "")
                : getIntent().getStringExtra(EXTRA_SECTION);
        if (expandedSection == null) expandedSection = "";
        int pad = dp(18);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(pad, dp(12), pad, pad);
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText(AppString.get(R.string.runtime_text_0001));
        title.setTextSize(25);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        root.addView(top);

        LinearLayout generalSettings = section(root, "general", R.string.settings_section_general, R.drawable.ic_md_settings);
        LinearLayout tabSettings = section(root, "tabs", R.string.settings_section_tabs, R.drawable.ic_settings_tabs);
        LinearLayout notificationSettings = section(root, "notification", R.string.settings_section_notifications, R.drawable.ic_settings_notification);
        LinearLayout calendarSettings = section(root, "calendar", R.string.settings_section_calendar, R.drawable.ic_settings_calendar);
        LinearLayout alarmSettings = section(root, "alarm", R.string.settings_section_alarms, R.drawable.ic_settings_alarm);
        LinearLayout prayerSettingsCard = section(root, "prayer", R.string.runtime_text_0077, R.drawable.ic_settings_adhan);
        notificationPanel = new NotificationSettingsPanel(this, notificationSettings, alarmSettings);

        generalSettings.addView(label(AppString.get(R.string.runtime_text_0043)));
        Spinner theme = spinner(new String[]{AppString.get(R.string.runtime_text_0044), AppString.get(R.string.runtime_text_0045)});
        theme.setSelection(AppSettings.themeMode(this) == AppSettings.THEME_DARK ? 1 : 0);
        generalSettings.addView(theme, new LinearLayout.LayoutParams(-1, dp(54)));
        watch(theme, position -> {
            int mode = position == 1 ? AppSettings.THEME_DARK : AppSettings.THEME_LIGHT;
            if (AppSettings.themeMode(this) == mode) return;
            AppSettings.setThemeMode(this, mode);
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, true);
        });

        generalSettings.addView(label(getString(R.string.language_label)));
        Spinner language = languageSpinner(
                getResources().getStringArray(R.array.language_options));
        language.setSelection(AppSettings.languagePosition(this));
        generalSettings.addView(language, new LinearLayout.LayoutParams(-1, dp(54)));

        generalSettings.addView(label(AppString.get(R.string.runtime_text_0152)));
        Spinner palette = spinner(AppSettings.paletteNames());
        palette.setSelection(AppSettings.palette(this));
        generalSettings.addView(palette, new LinearLayout.LayoutParams(-1, dp(54)));

        generalSettings.addView(label(AppString.get(R.string.clock_format_label)));
        Spinner clockFormat=spinner(new String[]{AppString.get(R.string.clock_format_24),AppString.get(R.string.clock_format_12)});
        clockFormat.setSelection(AppSettings.clock24Hour(this)?0:1);
        generalSettings.addView(clockFormat,new LinearLayout.LayoutParams(-1,dp(54)));

        calendarSettings.addView(label(getString(R.string.calendar_label)));
        Spinner calendar = spinner(getResources().getStringArray(R.array.calendar_options));
        calendar.setSelection(AppSettings.defaultCalendar(this));
        calendarSettings.addView(calendar, new LinearLayout.LayoutParams(-1, dp(54)));

        calendarSettings.addView(label(AppString.get(R.string.date_display_format_label)));
        java.util.ArrayList<String> dateExamples = new java.util.ArrayList<>(java.util.Arrays.asList(dateFormatOptions()));
        ArrayAdapter<String> dateAdapter = new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,dateExamples);
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        Spinner dateFormat = new Spinner(this);
        dateFormat.setLayoutDirection(AppSettings.layoutDirection(this));
        dateFormat.setAdapter(dateAdapter);
        dateFormat.setSelection(AppSettings.dateDisplayFormat(this));
        calendarSettings.addView(dateFormat, new LinearLayout.LayoutParams(-1, dp(54)));

        Switch showCalendarEvents = settingSwitch(
                AppString.get(R.string.runtime_text_0155),
                AppSettings.showCalendarEvents(this));
        calendarSettings.addView(showCalendarEvents);

        int[] initialExtraTypes = extraCalendarTypes(calendar.getSelectedItemPosition());
        Switch extraEventsOne = settingSwitch(
                eventSourceLabel(initialExtraTypes[0]),
                AppSettings.additionalCalendarEventsEnabled(this, initialExtraTypes[0]));
        Switch extraEventsTwo = settingSwitch(
                eventSourceLabel(initialExtraTypes[1]),
                AppSettings.additionalCalendarEventsEnabled(this, initialExtraTypes[1]));
        calendarSettings.addView(extraEventsOne);
        calendarSettings.addView(extraEventsTwo);

        boolean[] bindingExtraSources = {false};
        Runnable refreshExtraSources = () -> {
            bindingExtraSources[0] = true;
            int[] types = extraCalendarTypes(calendar.getSelectedItemPosition());
            extraEventsOne.setText(eventSourceLabel(types[0]));
            extraEventsTwo.setText(eventSourceLabel(types[1]));
            extraEventsOne.setChecked(
                    AppSettings.additionalCalendarEventsEnabled(this, types[0]));
            extraEventsTwo.setChecked(
                    AppSettings.additionalCalendarEventsEnabled(this, types[1]));
            boolean enabled = showCalendarEvents.isChecked();
            extraEventsOne.setEnabled(enabled);
            extraEventsTwo.setEnabled(enabled);
            bindingExtraSources[0] = false;
        };
        refreshExtraSources.run();

        prayerPanel = new PrayerSettingsPanel(this, prayerSettingsCard);

        generalSettings.addView(label(AppString.get(R.string.runtime_text_0156)));
        Spinner layout = spinner(new String[]{
                AppString.get(R.string.runtime_text_0157),
                AppString.get(R.string.runtime_text_0158)
        });
        layout.setSelection(AppSettings.clockLayoutMode(this));
        generalSettings.addView(layout, new LinearLayout.LayoutParams(-1, dp(54)));

        Switch autoDeleteExpiredAlarms = settingSwitch(
                AppString.get(R.string.runtime_text_0359),
                AppSettings.autoDeleteExpiredAlarms(this));

        Switch tabClock = tabSwitch(getString(R.string.tab_clock), "clock");
        Switch tabNotes = tabSwitch(getString(R.string.tab_notes), "noforget");
        Switch tabStopwatch = tabSwitch(getString(R.string.tab_stopwatch), "stopwatch");
        Switch tabTimer = tabSwitch(getString(R.string.tab_timer), "timer");
        Switch tabWorld = tabSwitch(getString(R.string.tab_world), "world");
        tabSettings.addView(label(AppString.get(R.string.runtime_text_0679)));
        java.util.Map<String, Switch> tabControls = new java.util.LinkedHashMap<>();
        tabControls.put("clock", tabClock);
        tabControls.put("noforget", tabNotes);
        tabControls.put("stopwatch", tabStopwatch);
        tabControls.put("timer", tabTimer);
        tabControls.put("world", tabWorld);
        tabSettings.addView(new TabOrderControls(this, tabControls, () -> {
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, false);
        }), new LinearLayout.LayoutParams(-1, -2));

        alarmSettings.addView(label(AppString.get(R.string.runtime_text_0160)));
        Spinner alarmStyle = spinner(new String[]{
                AppString.get(R.string.runtime_text_0161),
                AppString.get(R.string.runtime_text_0162),
                AppString.get(R.string.runtime_text_0163),
                AppString.get(R.string.alarm_style_analog),
                AppString.get(R.string.alarm_style_glass)
        });
        alarmStyle.setSelection(AppSettings.alarmScreenStyle(this));
        alarmSettings.addView(alarmStyle, new LinearLayout.LayoutParams(-1, dp(54)));

        alarmSettings.addView(label(getString(R.string.alarm_auto_stop_title)));
        String[] ringOptions = new String[AppSettings.ALARM_RING_MINUTES.length];
        int ringSelection = 2;
        for (int i = 0; i < ringOptions.length; i++) {
            int minutes = AppSettings.ALARM_RING_MINUTES[i];
            ringOptions[i] = minutes == 0 ? getString(R.string.alarm_auto_stop_never)
                    : getString(R.string.alarm_auto_stop_minutes, minutes);
            if (minutes == AppSettings.alarmRingMinutes(this)) ringSelection = i;
        }
        Spinner ringDuration = spinner(ringOptions);
        ringDuration.setSelection(ringSelection);
        alarmSettings.addView(ringDuration, new LinearLayout.LayoutParams(-1, dp(54)));
        alarmSettings.addView(autoDeleteExpiredAlarms);
        watch(ringDuration, position -> AppSettings.setAlarmRingMinutes(
                this, AppSettings.ALARM_RING_MINUTES[position]));

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);

        // Every control below writes its setting immediately. There is no deferred Save step.
        watch(palette, position -> {
            if (AppSettings.palette(this) == position) return;
            AppSettings.setPalette(this, position);
            runtimeChanged();
            // Palette affects the current settings screen too, so recreate it as well.
            AdvanceClockApplication.refreshOpenActivities(this, true);
        });

        watch(language, position -> {
            if (!AppSettings.isSelectableLanguagePosition(position)) {
                int current = AppSettings.languagePosition(this);
                if (language.getSelectedItemPosition() != current) {
                    language.setSelection(current);
                }
                return;
            }
            String selected = AppSettings.languageCodes()[position];
            if (AppSettings.language(this).equals(selected)) return;
            AppSettings.setLanguage(this, selected);
            runtimeChanged();
            AdvanceClockApplication.restartForLanguage(this);
        });

        calendar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                if (AppSettings.defaultCalendar(SettingsActivity.this) == position) {
                    refreshExtraSources.run();
                    return;
                }
                AppSettings.setDefaultCalendar(SettingsActivity.this, position);
                String[] examples = dateFormatOptions();
                dateExamples.set(0,examples[0]);
                dateExamples.set(1,examples[1]);
                dateAdapter.notifyDataSetChanged();
                refreshExtraSources.run();
                runtimeChanged();
                AdvanceClockApplication.refreshOpenActivities(SettingsActivity.this, false);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        watch(clockFormat,position -> {
            if(AppSettings.clock24Hour(this)==(position==0)) return;
            AppSettings.setClock24Hour(this,position==0);
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this,false);
        });

        watch(dateFormat, position -> {
            if (AppSettings.dateDisplayFormat(this) == position) return;
            AppSettings.setDateDisplayFormat(this, position);
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, false);
        });

        showCalendarEvents.setOnCheckedChangeListener((button, checked) -> {
            if (AppSettings.showCalendarEvents(this) == checked) return;
            AppSettings.setShowCalendarEvents(this, checked);
            extraEventsOne.setEnabled(checked);
            extraEventsTwo.setEnabled(checked);
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, false);
        });

        extraEventsOne.setOnCheckedChangeListener((button, checked) -> {
            if (bindingExtraSources[0]) return;
            int[] types = extraCalendarTypes(calendar.getSelectedItemPosition());
            if (AppSettings.additionalCalendarEventsEnabled(this, types[0]) == checked) return;
            AppSettings.setAdditionalCalendarEventsEnabled(this, types[0], checked);
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, false);
        });

        extraEventsTwo.setOnCheckedChangeListener((button, checked) -> {
            if (bindingExtraSources[0]) return;
            int[] types = extraCalendarTypes(calendar.getSelectedItemPosition());
            if (AppSettings.additionalCalendarEventsEnabled(this, types[1]) == checked) return;
            AppSettings.setAdditionalCalendarEventsEnabled(this, types[1], checked);
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, false);
        });

        watch(layout, position -> {
            if (AppSettings.clockLayoutMode(this) == position) return;
            AppSettings.setClockLayoutMode(this, position);
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, false);
        });

        autoDeleteExpiredAlarms.setOnCheckedChangeListener((button, checked) -> {
            if (AppSettings.autoDeleteExpiredAlarms(this) == checked) return;
            AppSettings.setAutoDeleteExpiredAlarms(this, checked);
            AlarmScheduler.rescheduleAll(this);
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, false);
        });

        watch(alarmStyle, position -> {
            if (AppSettings.alarmScreenStyle(this) == position) return;
            AppSettings.setAlarmScreenStyle(this, position);
            runtimeChanged();
        });

        CompoundButton.OnCheckedChangeListener saveTabs = (button, checked) -> {
            if (!checked && !tabClock.isChecked() && !tabNotes.isChecked()
                    && !tabStopwatch.isChecked() && !tabTimer.isChecked()
                    && !tabWorld.isChecked()) {
                button.setChecked(true);
                LogoToast.makeText(this, AppString.get(R.string.runtime_text_0164), Toast.LENGTH_SHORT).show();
                return;
            }
            AppSettings.setTabEnabled(this, "clock", tabClock.isChecked());
            AppSettings.setTabEnabled(this, "noforget", tabNotes.isChecked());
            AppSettings.setTabEnabled(this, "stopwatch", tabStopwatch.isChecked());
            AppSettings.setTabEnabled(this, "timer", tabTimer.isChecked());
            AppSettings.setTabEnabled(this, "world", tabWorld.isChecked());
            runtimeChanged();
            AdvanceClockApplication.refreshOpenActivities(this, false);
        };
        tabClock.setOnCheckedChangeListener(saveTabs);
        tabNotes.setOnCheckedChangeListener(saveTabs);
        tabStopwatch.setOnCheckedChangeListener(saveTabs);
        tabTimer.setOnCheckedChangeListener(saveTabs);
        tabWorld.setOnCheckedChangeListener(saveTabs);
    }

    private void runtimeChanged() {
        setResult(RESULT_OK);
        try { DateNotificationService.start(this); } catch (Exception ignored) {}
        ClockWidgetProvider.updateAll(this);
        CalendarWidgetProvider.updateAll(this);
        TimeToolsWidgetProvider.updateAll(this);
        NoForgetWidgetProvider.updateAll(this);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(AppSettings.textSecondary(this));
        v.setTextSize(13);
        v.setPadding(0, dp(12), 0, dp(4));
        return v;
    }

    private Switch tabSwitch(String label, String key) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setTextColor(AppSettings.textPrimary(this));
        control.setChecked(AppSettings.tabEnabled(this, key));
        control.setPadding(0, dp(4), 0, dp(4));
        control.setMinHeight(dp(48));
        control.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return control;
    }

    private Switch settingSwitch(String text, boolean checked) {
        Switch control = new Switch(this);
        control.setText(text);
        control.setTextColor(AppSettings.textPrimary(this));
        control.setTextSize(14);
        control.setChecked(checked);
        control.setPadding(0, dp(3), 0, dp(3));
        control.setMinHeight(dp(48));
        control.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return control;
    }

    private LinearLayout section(LinearLayout root, String key, int title, int icon) {
        SettingsSection section = new SettingsSection(this, key, title, icon, () -> {
            SettingsSection clicked = null;
            for (SettingsSection item : sections) if (item.key.equals(key)) clicked = item;
            boolean open = clicked != null && !clicked.isExpanded();
            android.transition.TransitionManager.beginDelayedTransition(root,
                    new android.transition.AutoTransition().setDuration(180));
            expandedSection = open ? key : "";
            for (SettingsSection item : sections) item.setExpanded(open && item.key.equals(key));
        });
        sections.add(section);
        section.setExpanded(key.equals(expandedSection));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(14);
        root.addView(section, params);
        return section.content;
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString(EXTRA_SECTION, expandedSection);
    }

    @Override protected void onResume() {
        super.onResume();
        if (notificationPanel != null) notificationPanel.onResume();
        if (prayerPanel != null) prayerPanel.onResume();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (notificationPanel != null) notificationPanel.onActivityResult(request, result, data);
        if (prayerPanel != null) prayerPanel.onActivityResult(request, result, data);
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (notificationPanel != null) notificationPanel.onRequestPermissionsResult(request, permissions, results);
        if (prayerPanel != null) prayerPanel.onRequestPermissionsResult(request, permissions, results);
    }

    @Override protected void onDestroy() {
        if (prayerPanel != null) prayerPanel.onDestroy();
        super.onDestroy();
    }

    private int[] extraCalendarTypes(int primary) {
        if (primary == CalendarUtils.PERSIAN) {
            return new int[]{CalendarUtils.HIJRI, CalendarUtils.GREGORIAN};
        }
        if (primary == CalendarUtils.GREGORIAN) {
            return new int[]{CalendarUtils.PERSIAN, CalendarUtils.HIJRI};
        }
        return new int[]{CalendarUtils.PERSIAN, CalendarUtils.GREGORIAN};
    }

    private String eventSourceLabel(int type) {
        if (type == CalendarUtils.PERSIAN) {
            return AppString.get(R.string.runtime_text_0472);
        }
        if (type == CalendarUtils.HIJRI) {
            return AppString.get(R.string.runtime_text_0473);
        }
        return AppString.get(R.string.runtime_text_0474);
    }

    private String[] dateFormatOptions() {
        int type = AppSettings.defaultCalendar(this);
        int year = CalendarUtils.fromMillis(type,System.currentTimeMillis()).get(android.icu.util.Calendar.YEAR);
        String written = CalendarUtils.fa(1) + " " + CalendarUtils.monthName(type,0) + " " + CalendarUtils.fa(year);
        String numeric = CalendarUtils.fa(String.format(java.util.Locale.US,"%04d/01/01",year));
        return new String[]{AppString.get(R.string.date_display_format_written,written),
                AppString.get(R.string.date_display_format_numeric,numeric)};
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setLayoutDirection(AppSettings.layoutDirection(this));
        return spinner;
    }

    private Spinner languageSpinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                values) {
            @Override public boolean areAllItemsEnabled() {
                return false;
            }

            @Override public boolean isEnabled(int position) {
                return AppSettings.isSelectableLanguagePosition(position);
            }

            @Override public View getDropDownView(
                    int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                boolean enabled = isEnabled(position);
                view.setEnabled(enabled);
                view.setAlpha(enabled ? 1f : 0.38f);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(enabled
                            ? AppSettings.textPrimary(SettingsActivity.this)
                            : AppSettings.textSecondary(SettingsActivity.this));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setLayoutDirection(AppSettings.layoutDirection(this));
        return spinner;
    }

    private interface SpinnerChanged { void onChanged(int position); }

    private void watch(Spinner spinner, SpinnerChanged changed) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                changed.onChanged(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
