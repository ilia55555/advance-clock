package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

/** Converts between 12-hour (AM/PM) and 24-hour clock formats. */
public final class TimeFormatToolActivity extends Activity {
    private static final int MODE_12_TO_24 = 0;
    private static final int MODE_24_TO_12 = 1;

    private int mode = MODE_12_TO_24;
    private NumberPicker hourPicker;
    private NumberPicker minutePicker;
    private NumberPicker periodPicker;
    private TextView result;
    private TextView resultHint;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(dp(18), dp(10), dp(18), dp(20));
        root.setBackgroundColor(AppSettings.background(this));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this));
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text(AppString.get(R.string.time_format_tool_title), 23, AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(58), 1f));
        root.addView(header);

        LinearLayout selectorCard = card();
        selectorCard.addView(text(AppString.get(R.string.time_format_tool_direction), 13, AppSettings.textSecondary(this)));
        Spinner selector = new Spinner(this);
        String[] modes = {
                AppString.get(R.string.time_format_tool_12_to_24),
                AppString.get(R.string.time_format_tool_24_to_12)};
        selector.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, modes));
        selector.setBackgroundResource(R.drawable.bg_field);
        selector.setPadding(dp(12), 0, dp(12), 0);
        selectorCard.addView(selector, new LinearLayout.LayoutParams(-1, dp(52)));
        root.addView(selectorCard, margins(14));

        LinearLayout inputCard = card();
        inputCard.addView(text(AppString.get(R.string.time_format_tool_input), 13, AppSettings.textSecondary(this)));
        LinearLayout pickers = new LinearLayout(this);
        pickers.setOrientation(LinearLayout.HORIZONTAL);
        pickers.setGravity(Gravity.CENTER);
        pickers.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        pickers.setBackgroundResource(R.drawable.bg_field);
        pickers.setPadding(dp(8), dp(4), dp(8), dp(4));
        hourPicker = new NumberPicker(this);
        minutePicker = new NumberPicker(this);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(v -> CalendarUtils.fa(String.format(Locale.US, "%02d", v)));
        periodPicker = new NumberPicker(this);
        periodPicker.setMinValue(0);
        periodPicker.setMaxValue(1);
        periodPicker.setDisplayedValues(TimeEntryDialog.periodLabels());
        pickers.addView(hourPicker, pickerParams());
        TextView colon = text(":", 26, AppSettings.primaryColor(this));
        colon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        colon.setGravity(Gravity.CENTER);
        pickers.addView(colon, new LinearLayout.LayoutParams(dp(18), -2));
        pickers.addView(minutePicker, pickerParams());
        pickers.addView(periodPicker, pickerParams());
        LinearLayout.LayoutParams pickersParams = new LinearLayout.LayoutParams(-1, -2);
        pickersParams.topMargin = dp(6);
        inputCard.addView(pickers, pickersParams);
        root.addView(inputCard, margins(14));

        LinearLayout resultCard = card();
        resultCard.addView(text(AppString.get(R.string.time_format_tool_result), 13, AppSettings.textSecondary(this)));
        result = text("", 40, AppSettings.primaryColor(this));
        result.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        result.setGravity(Gravity.CENTER);
        result.setTextDirection(View.TEXT_DIRECTION_LTR);
        resultCard.addView(result, new LinearLayout.LayoutParams(-1, -2));
        resultHint = text("", 12, AppSettings.textSecondary(this));
        resultHint.setGravity(Gravity.CENTER);
        resultCard.addView(resultHint, new LinearLayout.LayoutParams(-1, -2));
        root.addView(resultCard, margins(0));

        NumberPicker.OnValueChangeListener listener = (p, o, n) -> update();
        hourPicker.setOnValueChangedListener(listener);
        minutePicker.setOnValueChangedListener(listener);
        periodPicker.setOnValueChangedListener(listener);
        selector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switchMode(position);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        java.util.Calendar now = java.util.Calendar.getInstance();
        configureHours(now.get(java.util.Calendar.HOUR_OF_DAY));
        minutePicker.setValue(now.get(java.util.Calendar.MINUTE));
        update();

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root);
        setContentView(scroll);
    }

    private void switchMode(int newMode) {
        if (newMode == mode) return;
        int hour24 = currentHour24();
        mode = newMode;
        configureHours(hour24);
        update();
    }

    private int currentHour24() {
        if (mode == MODE_24_TO_12) return hourPicker.getValue();
        int h = hourPicker.getValue() % 12;
        return periodPicker.getValue() == 1 ? h + 12 : h;
    }

    private void configureHours(int hour24) {
        hourPicker.setDisplayedValues(null);
        if (mode == MODE_12_TO_24) {
            hourPicker.setMinValue(1);
            hourPicker.setMaxValue(12);
            hourPicker.setFormatter(v -> CalendarUtils.fa(String.valueOf(v)));
            int h12 = hour24 % 12;
            hourPicker.setValue(h12 == 0 ? 12 : h12);
            periodPicker.setValue(hour24 >= 12 ? 1 : 0);
            periodPicker.setVisibility(View.VISIBLE);
        } else {
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            hourPicker.setFormatter(v -> CalendarUtils.fa(String.format(Locale.US, "%02d", v)));
            hourPicker.setValue(hour24);
            periodPicker.setVisibility(View.GONE);
        }
    }

    private void update() {
        int hour24 = currentHour24();
        int minute = minutePicker.getValue();
        if (mode == MODE_12_TO_24) {
            result.setText(CalendarUtils.fa(String.format(Locale.US, "%02d:%02d", hour24, minute)));
        } else {
            int h12 = hour24 % 12;
            String[] periods = TimeEntryDialog.periodLabels();
            result.setText(CalendarUtils.fa(String.format(Locale.US, "%d:%02d", h12 == 0 ? 12 : h12, minute))
                    + " " + periods[hour24 >= 12 ? 1 : 0]);
        }
        resultHint.setText(AppString.get(hour24 >= 12
                ? R.string.time_format_tool_afternoon
                : R.string.time_format_tool_morning));
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(12), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private LinearLayout.LayoutParams margins(int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(bottom);
        return params;
    }

    private LinearLayout.LayoutParams pickerParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        return params;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
