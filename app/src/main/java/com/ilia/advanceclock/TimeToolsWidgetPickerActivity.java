package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Calendar;

public final class TimeToolsWidgetPickerActivity extends Activity {
    private static final String PREFS = "time_tools";

    private int widgetId;
    private boolean stopwatch;
    private boolean dateMode;

    private NumberPicker hours;
    private NumberPicker minutes;
    private NumberPicker seconds;

    private Spinner calendarType;
    private NumberPicker year;
    private NumberPicker month;
    private NumberPicker day;
    private NumberPicker clockHour;
    private NumberPicker clockMinute;

    private boolean refreshingDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        stopwatch = "stopwatch".equals(
                getIntent().getStringExtra("tool"));

        SharedPreferences p = getSharedPreferences(
                PREFS,
                MODE_PRIVATE);

        if (stopwatch) {
            dateMode = p.getInt("stopwatch_mode", 0) == 2;
            if (p.getBoolean("stopwatch_running", false)
                    || elapsed(p) > 0L) {
                finish();
                return;
            }
        } else {
            dateMode = p.getBoolean("timer_date_mode", false);
            if (p.getBoolean("timer_running", false)
                    || remaining(p) > 0L) {
                finish();
                return;
            }
        }

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(AppSettings.background(this));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(dp(18), dp(16), dp(18), dp(18));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = text(
                dateMode
                        ? AppString.get(R.string.runtime_text_0173)
                        : AppString.get(R.string.runtime_text_0172),
                21,
                AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(52)));

        if (dateMode) {
            buildDateTime(root, p);
        } else {
            buildDuration(root, p);
        }

        page.addView(
                scroll,
                new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        actions.setPadding(dp(18), dp(8), dp(18), dp(14));

        Button cancel = new Button(this);
        cancel.setText(AppString.get(R.string.runtime_text_0003));
        cancel.setAllCaps(false);
        cancel.setTextColor(AppSettings.textPrimary(this));
        cancel.setBackgroundResource(R.drawable.bg_soft_button);
        cancel.setOnClickListener(v -> finish());

        Button save = new Button(this);
        save.setText(AppString.get(R.string.runtime_text_0005));
        save.setAllCaps(false);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundResource(R.drawable.bg_teal_button);
        save.setOnClickListener(v -> saveAndFinish());

        actions.addView(
                cancel,
                new LinearLayout.LayoutParams(0, dp(52), 1f));
        TextView gap = new TextView(this);
        actions.addView(gap, new LinearLayout.LayoutParams(dp(10), 1));
        actions.addView(
                save,
                new LinearLayout.LayoutParams(0, dp(52), 1f));

        page.addView(actions);
        setContentView(page);
        AppSettings.applyFullscreenInsets(page);
    }

    private void buildDuration(
            LinearLayout root,
            SharedPreferences p) {
        long value;
        if (stopwatch) {
            value = p.getLong(
                    "stopwatch_limit",
                    5 * 60_000L);
        } else {
            value = TimeToolsWidgetPrefs.timerDefaultMillis(
                    this,
                    widgetId);
        }
        if (value <= 0L) {
            value = 5 * 60_000L;
        }

        long total = value / 1000L;
        hours = picker(
                (int) Math.min(999L, total / 3600L),
                0,
                999);
        minutes = picker(
                (int) ((total / 60L) % 60L),
                0,
                59);
        seconds = picker(
                (int) (total % 60L),
                0,
                59);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setGravity(Gravity.CENTER);
        row.setPadding(dp(6), dp(10), dp(6), dp(10));
        row.setBackgroundResource(R.drawable.bg_card);

        row.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0013),
                        hours),
                new LinearLayout.LayoutParams(0, dp(150), 1f));
        row.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0037),
                        minutes),
                new LinearLayout.LayoutParams(0, dp(150), 1f));
        row.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0038),
                        seconds),
                new LinearLayout.LayoutParams(0, dp(150), 1f));

        root.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void buildDateTime(
            LinearLayout root,
            SharedPreferences p) {
        long target = p.getLong(
                stopwatch
                        ? "stopwatch_target"
                        : "timer_target",
                0L);
        if (target <= System.currentTimeMillis()) {
            target = System.currentTimeMillis()
                    + 5 * 60_000L;
        }

        int initialType = p.getInt(
                stopwatch
                        ? "stopwatch_calendar"
                        : "timer_calendar",
                AppSettings.defaultCalendar(this));

        calendarType = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{
                        AppString.get(R.string.runtime_text_0177),
                        AppString.get(R.string.runtime_text_0178),
                        AppString.get(R.string.runtime_text_0179)
                });
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        calendarType.setAdapter(adapter);
        calendarType.setSelection(
                Math.max(0, Math.min(2, initialType)));
        calendarType.setBackgroundResource(R.drawable.bg_field);
        root.addView(
                calendarType,
                new LinearLayout.LayoutParams(-1, dp(54)));

        year = new NumberPicker(this);
        month = new NumberPicker(this);
        day = new NumberPicker(this);
        clockHour = picker(0, 0, 23);
        clockMinute = picker(0, 0, 59);

        final long initialTarget = target;

        Runnable refresh = () -> {
            refreshingDate = true;
            int type = calendarType.getSelectedItemPosition();
            android.icu.util.Calendar c =
                    CalendarUtils.fromMillis(
                            type,
                            initialTarget);

            year.setMinValue(1);
            year.setMaxValue(CalendarUtils.maximumYear(type));
            year.setMinValue(CalendarUtils.minimumYear(type));

            year.setValue(
                    Math.max(
                            year.getMinValue(),
                            Math.min(
                                    year.getMaxValue(),
                                    c.get(
                                            android.icu.util.Calendar.YEAR))));

            month.setDisplayedValues(null);
            month.setMinValue(0);
            month.setMaxValue(11);
            String[] names = new String[12];
            for (int i = 0; i < 12; i++) {
                names[i] = CalendarUtils.monthName(type, i);
            }
            month.setDisplayedValues(names);
            month.setValue(
                    c.get(android.icu.util.Calendar.MONTH));

            updateDayRange(
                    c.get(
                            android.icu.util.Calendar.DAY_OF_MONTH));

            Calendar normal = Calendar.getInstance();
            normal.setTimeInMillis(initialTarget);
            clockHour.setValue(
                    normal.get(Calendar.HOUR_OF_DAY));
            clockMinute.setValue(
                    normal.get(Calendar.MINUTE));
            refreshingDate = false;
        };

        NumberPicker.OnValueChangeListener dateChanged =
                (picker, oldValue, newValue) -> {
                    if (refreshingDate) return;
                    updateDayRange(day.getValue());
                };
        year.setOnValueChangedListener(dateChanged);
        month.setOnValueChangedListener(dateChanged);

        calendarType.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        if (!refreshingDate) {
                            refresh.run();
                        }
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setLayoutDirection(AppSettings.layoutDirection(this));
        dateRow.setGravity(Gravity.CENTER);
        dateRow.setPadding(dp(4), dp(8), dp(4), dp(8));
        dateRow.setBackgroundResource(R.drawable.bg_card);
        dateRow.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0179),
                        year),
                new LinearLayout.LayoutParams(0, dp(160), 1f));
        dateRow.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0169),
                        month),
                new LinearLayout.LayoutParams(0, dp(160), 1f));
        dateRow.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0039),
                        day),
                new LinearLayout.LayoutParams(0, dp(160), 1f));

        LinearLayout.LayoutParams dateLp =
                new LinearLayout.LayoutParams(-1, -2);
        dateLp.topMargin = dp(10);
        root.addView(dateRow, dateLp);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setOrientation(LinearLayout.HORIZONTAL);
        timeRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        timeRow.setGravity(Gravity.CENTER);
        timeRow.setPadding(dp(6), dp(8), dp(6), dp(8));
        timeRow.setBackgroundResource(R.drawable.bg_card);
        timeRow.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0013),
                        clockHour),
                new LinearLayout.LayoutParams(0, dp(130), 1f));
        timeRow.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0037),
                        clockMinute),
                new LinearLayout.LayoutParams(0, dp(130), 1f));

        LinearLayout.LayoutParams timeLp =
                new LinearLayout.LayoutParams(-1, -2);
        timeLp.topMargin = dp(10);
        root.addView(timeRow, timeLp);

        refresh.run();
    }

    private void updateDayRange(int preferred) {
        int type = calendarType.getSelectedItemPosition();
        int max = CalendarUtils.daysInMonth(type, year.getValue(), month.getValue());
        day.setMinValue(1);
        day.setMaxValue(max);
        day.setValue(
                Math.max(1, Math.min(max, preferred)));
    }

    private void saveAndFinish() {
        SharedPreferences p = getSharedPreferences(
                PREFS,
                MODE_PRIVATE);

        if (dateMode) {
            int type = calendarType.getSelectedItemPosition();
            long value = CalendarUtils.toMillis(
                    type,
                    year.getValue(),
                    month.getValue(),
                    day.getValue(),
                    clockHour.getValue(),
                    clockMinute.getValue());

            if (value <= System.currentTimeMillis()) {
                return;
            }

            if (stopwatch) {
                p.edit()
                        .putInt("stopwatch_mode", 2)
                        .putLong("stopwatch_target", value)
                        .putInt("stopwatch_calendar", type)
                        .apply();
            } else {
                p.edit()
                        .putBoolean("timer_date_mode", true)
                        .putLong("timer_target", value)
                        .putInt("timer_calendar", type)
                        .apply();
            }
        } else {
            long totalSeconds =
                    hours.getValue() * 3600L
                            + minutes.getValue() * 60L
                            + seconds.getValue();
            long value =
                    Math.max(1L, totalSeconds)
                            * 1000L;

            if (stopwatch) {
                p.edit()
                        .putInt("stopwatch_mode", 1)
                        .putLong("stopwatch_limit", value)
                        .apply();
            } else {
                p.edit()
                        .putBoolean("timer_date_mode", false)
                        .apply();
                TimeToolsWidgetPrefs.setTimerDefaultMillis(
                        this,
                        widgetId,
                        value);
            }
        }

        TimeToolsWidgetProvider.updateAll(this);
        finish();
    }

    private NumberPicker picker(
            int value,
            int min,
            int max) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(
                Math.max(min, Math.min(max, value)));
        picker.setWrapSelectorWheel(false);
        return picker;
    }

    private LinearLayout pickerColumn(
            String labelValue,
            NumberPicker picker) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);

        TextView label = text(
                labelValue,
                11,
                AppSettings.textSecondary(this));
        label.setGravity(Gravity.CENTER);
        box.addView(
                label,
                new LinearLayout.LayoutParams(-1, dp(24)));
        box.addView(
                picker,
                new LinearLayout.LayoutParams(-1, 0, 1f));
        return box;
    }

    private TextView text(
            String value,
            int size,
            int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        return view;
    }

    private long elapsed(SharedPreferences p) {
        long accumulated = p.getLong(
                "stopwatch_accumulated",
                0L);
        if (!p.getBoolean(
                "stopwatch_running",
                false)) {
            return Math.max(0L, accumulated);
        }
        return Math.max(
                0L,
                accumulated
                        + System.currentTimeMillis()
                        - p.getLong(
                                "stopwatch_started",
                                0L));
    }

    private long remaining(SharedPreferences p) {
        if (!p.getBoolean(
                "timer_running",
                false)) {
            return Math.max(
                    0L,
                    p.getLong(
                            "timer_remaining",
                            0L));
        }
        return Math.max(
                0L,
                p.getLong(
                        "timer_end",
                        0L)
                        - System.currentTimeMillis());
    }

    private int dp(int value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density);
    }
}
