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
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public final class SettingsActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

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

        root.addView(label(AppString.get(R.string.runtime_text_0152)));
        Spinner palette = spinner(AppSettings.paletteNames());
        palette.setSelection(AppSettings.palette(this));
        root.addView(palette, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(label(getString(R.string.language_label)));
        Spinner language = languageSpinner(
                getResources().getStringArray(R.array.language_options));
        language.setSelection(AppSettings.languagePosition(this));
        root.addView(language, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(label(getString(R.string.calendar_label)));
        Spinner calendar = spinner(getResources().getStringArray(R.array.calendar_options));
        calendar.setSelection(AppSettings.defaultCalendar(this));
        root.addView(calendar, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(label(AppString.get(R.string.runtime_text_0153)));
        LinearLayout eventsCard = settingsCard();

        TextView eventsTitle = new TextView(this);
        eventsTitle.setText(AppString.get(R.string.runtime_text_0154));
        eventsTitle.setTextColor(AppSettings.textPrimary(this));
        eventsTitle.setTextSize(17);
        eventsTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        eventsCard.addView(eventsTitle);

        Switch showCalendarEvents = settingSwitch(
                AppString.get(R.string.runtime_text_0155),
                AppSettings.showCalendarEvents(this));
        eventsCard.addView(showCalendarEvents);

        int[] initialExtraTypes = extraCalendarTypes(calendar.getSelectedItemPosition());
        Switch extraEventsOne = settingSwitch(
                eventSourceLabel(initialExtraTypes[0]),
                AppSettings.additionalCalendarEventsEnabled(this, initialExtraTypes[0]));
        Switch extraEventsTwo = settingSwitch(
                eventSourceLabel(initialExtraTypes[1]),
                AppSettings.additionalCalendarEventsEnabled(this, initialExtraTypes[1]));
        eventsCard.addView(extraEventsOne);
        eventsCard.addView(extraEventsTwo);

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
        root.addView(eventsCard, settingsCardParams());

        root.addView(label(AppString.get(R.string.runtime_text_0077)));
        LinearLayout prayerCard = settingsCard();
        LinearLayout prayerRow = new LinearLayout(this);
        prayerRow.setOrientation(LinearLayout.HORIZONTAL);
        prayerRow.setGravity(Gravity.CENTER_VERTICAL);
        prayerRow.setLayoutDirection(AppSettings.layoutDirection(this));
        Switch adhanEnabled = settingSwitch(AppString.get(R.string.runtime_text_0361), AppSettings.adhanEnabled(this));
        prayerRow.addView(adhanEnabled, new LinearLayout.LayoutParams(0, dp(50), 1f));

        Button prayerSettings = new Button(this);
        prayerSettings.setText(AppString.get(R.string.runtime_text_0001));
        prayerSettings.setAllCaps(false);
        prayerSettings.setTextColor(AppSettings.primaryColor(this));
        prayerSettings.setBackgroundResource(R.drawable.bg_soft_button);
        prayerSettings.setOnClickListener(v ->
                startActivity(new Intent(this, PrayerSettingsActivity.class)));
        prayerRow.addView(prayerSettings, new LinearLayout.LayoutParams(dp(100), dp(44)));
        prayerCard.addView(prayerRow);
        root.addView(prayerCard, settingsCardParams());

        root.addView(label(AppString.get(R.string.runtime_text_0156)));
        Spinner layout = spinner(new String[]{
                AppString.get(R.string.runtime_text_0157),
                AppString.get(R.string.runtime_text_0158)
        });
        layout.setSelection(AppSettings.clockLayoutMode(this));
        root.addView(layout, new LinearLayout.LayoutParams(-1, dp(54)));

        Switch autoDeleteExpiredAlarms = settingSwitch(
                AppString.get(R.string.runtime_text_0359),
                AppSettings.autoDeleteExpiredAlarms(this));
        root.addView(autoDeleteExpiredAlarms);

        root.addView(label(AppString.get(R.string.runtime_text_0159)));
        Switch tabClock = tabSwitch(getString(R.string.tab_clock), "clock");
        Switch tabNotes = tabSwitch(getString(R.string.tab_notes), "noforget");
        Switch tabStopwatch = tabSwitch(getString(R.string.tab_stopwatch), "stopwatch");
        Switch tabTimer = tabSwitch(getString(R.string.tab_timer), "timer");
        Switch tabWorld = tabSwitch(getString(R.string.tab_world), "world");
        root.addView(tabClock);
        root.addView(tabNotes);
        root.addView(tabStopwatch);
        root.addView(tabTimer);
        root.addView(tabWorld);

        root.addView(label(AppString.get(R.string.runtime_text_0160)));
        Spinner alarmStyle = spinner(new String[]{
                AppString.get(R.string.runtime_text_0161),
                AppString.get(R.string.runtime_text_0162),
                AppString.get(R.string.runtime_text_0163)
        });
        alarmStyle.setSelection(AppSettings.alarmScreenStyle(this));
        root.addView(alarmStyle, new LinearLayout.LayoutParams(-1, dp(54)));

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
                refreshExtraSources.run();
                runtimeChanged();
                AdvanceClockApplication.refreshOpenActivities(SettingsActivity.this, false);
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
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

        adhanEnabled.setOnCheckedChangeListener((button, checked) -> {
            if (AppSettings.adhanEnabled(this) == checked) return;
            AppSettings.setAdhanEnabled(this, checked);
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
        control.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(48)));
        return control;
    }

    private Switch settingSwitch(String text, boolean checked) {
        Switch control = new Switch(this);
        control.setText(text);
        control.setTextColor(AppSettings.textPrimary(this));
        control.setTextSize(14);
        control.setChecked(checked);
        control.setPadding(0, dp(3), 0, dp(3));
        control.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(48)));
        return control;
    }

    private LinearLayout settingsCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(AppSettings.layoutDirection(this));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private LinearLayout.LayoutParams settingsCardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        return params;
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
