package com.ilia.advanceclock;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Locale;

/**
 * Shared time-entry dialog offering three input methods: the round clock,
 * typed digits and a wheel. Typed and wheel modes follow the app's
 * 12/24-hour setting; the last used mode is remembered.
 */
class TimeEntryDialog extends Dialog {
    static final int MODE_CLOCK = 0;
    static final int MODE_TYPED = 1;
    static final int MODE_WHEEL = 2;

    private static final String PREFS = "time_entry_dialog";
    private static final String KEY_MODE = "mode";

    private final TimePickerDialog.OnTimeSetListener listener;
    private final boolean clock24;
    private final boolean app24;
    private final String[] periods;
    private int hour;
    private int minute;
    private int mode;

    private final Button[] tabs = new Button[3];
    private FrameLayout content;
    private android.widget.TimePicker clockView;
    private EditText hourField;
    private EditText minuteField;
    private int typedPeriod;
    private Button amButton;
    private Button pmButton;
    private NumberPicker wheelHour;
    private NumberPicker wheelMinute;
    private NumberPicker wheelPeriod;

    TimeEntryDialog(Context context, TimePickerDialog.OnTimeSetListener listener,
                    int hour, int minute, boolean full) {
        super(context);
        this.listener = listener;
        this.hour = clamp(hour, 0, 23);
        this.minute = clamp(minute, 0, 59);
        this.clock24 = full;
        this.app24 = AppSettings.clock24Hour(context);
        this.periods = periodLabels();
        int saved = prefs().getInt(KEY_MODE, MODE_CLOCK);
        this.mode = saved >= MODE_CLOCK && saved <= MODE_WHEEL ? saved : MODE_CLOCK;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(build());
        setCanceledOnTouchOutside(true);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    private View build() {
        Context c = getContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(c));
        root.setPadding(dp(16), dp(16), dp(16), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppSettings.surface(c));
        bg.setCornerRadius(dp(24));
        root.setBackground(bg);

        LinearLayout tabRow = new LinearLayout(c);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabRow.setBackgroundResource(R.drawable.bg_field);
        int[] labels = {R.string.time_picker_mode_clock, R.string.time_picker_mode_typed,
                R.string.time_picker_mode_wheel};
        for (int i = 0; i < 3; i++) {
            final int target = i;
            Button tab = new Button(c);
            tab.setText(AppString.get(labels[i]));
            tab.setAllCaps(false);
            tab.setTextSize(13);
            tab.setSingleLine(true);
            tab.setPadding(dp(4), 0, dp(4), 0);
            tab.setStateListAnimator(null);
            tab.setOnClickListener(v -> switchMode(target));
            tabs[i] = tab;
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(40), 1f);
            if (i > 0) p.setMarginStart(dp(4));
            tabRow.addView(tab, p);
        }
        root.addView(tabRow, new LinearLayout.LayoutParams(-1, -2));

        content = new FrameLayout(c);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, -2);
        contentParams.topMargin = dp(12);
        root.addView(content, contentParams);

        LinearLayout actions = new LinearLayout(c);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        Button cancel = actionButton(AppString.get(R.string.runtime_text_0003));
        cancel.setOnClickListener(v -> cancel());
        Button ok = actionButton(AppString.get(R.string.runtime_text_0004));
        ok.setOnClickListener(v -> confirm());
        actions.addView(cancel, new LinearLayout.LayoutParams(-2, dp(48)));
        LinearLayout.LayoutParams okParams = new LinearLayout.LayoutParams(-2, dp(48));
        okParams.setMarginStart(dp(8));
        actions.addView(ok, okParams);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.topMargin = dp(8);
        root.addView(actions, actionParams);

        showMode();
        return root;
    }

    private void switchMode(int target) {
        if (target == mode) return;
        if (!readCurrent(false)) return;
        mode = target;
        prefs().edit().putInt(KEY_MODE, mode).apply();
        showMode();
    }

    private void showMode() {
        Context c = getContext();
        for (int i = 0; i < tabs.length; i++) {
            boolean active = i == mode;
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(dp(12));
            shape.setColor(active ? AppSettings.primaryColor(c) : Color.TRANSPARENT);
            tabs[i].setBackground(shape);
            tabs[i].setTextColor(active ? Color.WHITE : AppSettings.textPrimary(c));
            tabs[i].setTypeface(Typeface.DEFAULT, active ? Typeface.BOLD : Typeface.NORMAL);
        }
        content.removeAllViews();
        View view = mode == MODE_CLOCK ? clockMode() : mode == MODE_TYPED ? typedMode() : wheelMode();
        content.addView(view, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER_HORIZONTAL));
    }

    private View clockMode() {
        clockView = new android.widget.TimePicker(getContext());
        clockView.setIs24HourView(clock24);
        clockView.setHour(hour);
        clockView.setMinute(minute);
        return clockView;
    }

    private View typedMode() {
        Context c = getContext();
        LinearLayout column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.setPadding(0, dp(12), 0, dp(12));

        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        hourField = numberField(String.format(Locale.US, "%02d", app24 ? hour : hour12(hour)),
                AppString.get(R.string.time_picker_hour));
        minuteField = numberField(String.format(Locale.US, "%02d", minute),
                AppString.get(R.string.time_picker_minute));
        minuteField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        minuteField.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { confirm(); return true; }
            return false;
        });
        row.addView(fieldColumn(hourField, AppString.get(R.string.time_picker_hour)));
        TextView colon = label(":", 32, AppSettings.primaryColor(c));
        colon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        colon.setGravity(Gravity.CENTER);
        row.addView(colon, new LinearLayout.LayoutParams(dp(24), dp(72)));
        row.addView(fieldColumn(minuteField, AppString.get(R.string.time_picker_minute)));
        column.addView(row, new LinearLayout.LayoutParams(-2, -2));

        if (!app24) {
            typedPeriod = hour >= 12 ? 1 : 0;
            LinearLayout periodRow = new LinearLayout(c);
            periodRow.setOrientation(LinearLayout.HORIZONTAL);
            periodRow.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            amButton = actionButton(periods[0]);
            pmButton = actionButton(periods[1]);
            amButton.setOnClickListener(v -> { typedPeriod = 0; paintPeriod(); });
            pmButton.setOnClickListener(v -> { typedPeriod = 1; paintPeriod(); });
            periodRow.addView(amButton, new LinearLayout.LayoutParams(dp(96), dp(44)));
            LinearLayout.LayoutParams pm = new LinearLayout.LayoutParams(dp(96), dp(44));
            pm.leftMargin = dp(8);
            periodRow.addView(pmButton, pm);
            LinearLayout.LayoutParams pr = new LinearLayout.LayoutParams(-2, -2);
            pr.topMargin = dp(12);
            column.addView(periodRow, pr);
            paintPeriod();
        }
        hourField.requestFocus();
        hourField.selectAll();
        Window window = getWindow();
        if (window != null) window.setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return column;
    }

    private View wheelMode() {
        Context c = getContext();
        LinearLayout row = new LinearLayout(c);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setBackgroundResource(R.drawable.bg_field);
        row.setPadding(dp(8), dp(4), dp(8), dp(4));
        NumberPicker.Formatter twoDigits = v -> String.format(Locale.US, "%02d", v);
        wheelHour = new NumberPicker(c);
        if (app24) {
            wheelHour.setMinValue(0);
            wheelHour.setMaxValue(23);
            wheelHour.setFormatter(twoDigits);
            wheelHour.setValue(hour);
        } else {
            wheelHour.setMinValue(1);
            wheelHour.setMaxValue(12);
            wheelHour.setValue(hour12(hour));
        }
        wheelMinute = new NumberPicker(c);
        wheelMinute.setMinValue(0);
        wheelMinute.setMaxValue(59);
        wheelMinute.setFormatter(twoDigits);
        wheelMinute.setValue(minute);
        row.addView(wheelHour, wheelParams());
        TextView colon = label(":", 26, AppSettings.primaryColor(c));
        colon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        colon.setGravity(Gravity.CENTER);
        row.addView(colon, new LinearLayout.LayoutParams(dp(18), -2));
        row.addView(wheelMinute, wheelParams());
        wheelPeriod = null;
        if (!app24) {
            wheelPeriod = new NumberPicker(c);
            wheelPeriod.setMinValue(0);
            wheelPeriod.setMaxValue(1);
            wheelPeriod.setDisplayedValues(periods);
            wheelPeriod.setValue(hour >= 12 ? 1 : 0);
            row.addView(wheelPeriod, wheelParams());
        }
        return row;
    }

    /** Reads the active mode into hour/minute. Returns false if typed input is invalid. */
    private boolean readCurrent(boolean showErrors) {
        if (mode == MODE_CLOCK && clockView != null) {
            hour = clockView.getHour();
            minute = clockView.getMinute();
        } else if (mode == MODE_WHEEL && wheelHour != null) {
            wheelHour.clearFocus();
            wheelMinute.clearFocus();
            int h = wheelHour.getValue();
            if (!app24) h = to24(h, wheelPeriod != null ? wheelPeriod.getValue() : 0);
            hour = h;
            minute = wheelMinute.getValue();
        } else if (mode == MODE_TYPED && hourField != null) {
            int h = parse(hourField);
            int m = parse(minuteField);
            boolean hourOk = app24 ? h >= 0 && h <= 23 : h >= 1 && h <= 12;
            boolean minuteOk = m >= 0 && m <= 59;
            if (!hourOk || !minuteOk) {
                if (showErrors) {
                    String error = AppString.get(R.string.time_picker_invalid);
                    if (!hourOk) { hourField.setError(error); hourField.requestFocus(); }
                    else { minuteField.setError(error); minuteField.requestFocus(); }
                }
                return !showErrors;
            }
            hour = app24 ? h : to24(h, typedPeriod);
            minute = m;
        }
        return true;
    }

    private void confirm() {
        if (!readCurrent(true)) return;
        if (listener != null) {
            android.widget.TimePicker view = clockView != null
                    ? clockView : new android.widget.TimePicker(getContext());
            listener.onTimeSet(view, hour, minute);
        }
        dismiss();
    }

    private void paintPeriod() {
        Context c = getContext();
        paintToggle(amButton, typedPeriod == 0, c);
        paintToggle(pmButton, typedPeriod == 1, c);
    }

    private void paintToggle(Button button, boolean active, Context c) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(12));
        shape.setColor(active ? AppSettings.primaryColor(c) : AppSettings.field(c));
        shape.setStroke(dp(1), AppSettings.primaryColor(c));
        button.setBackground(shape);
        button.setTextColor(active ? Color.WHITE : AppSettings.primaryColor(c));
    }

    private LinearLayout fieldColumn(EditText field, String caption) {
        Context c = getContext();
        LinearLayout column = new LinearLayout(c);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(field, new LinearLayout.LayoutParams(dp(96), dp(72)));
        TextView text = label(caption, 12, AppSettings.textSecondary(c));
        text.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(4);
        column.addView(text, p);
        return column;
    }

    private EditText numberField(String value, String hint) {
        Context c = getContext();
        EditText field = new EditText(c);
        field.setText(value);
        field.setHint(hint);
        field.setInputType(InputType.TYPE_CLASS_NUMBER);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        field.setTextSize(32);
        field.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        field.setTextColor(AppSettings.textPrimary(c));
        field.setHintTextColor(AppSettings.textSecondary(c));
        field.setGravity(Gravity.CENTER);
        field.setTextDirection(View.TEXT_DIRECTION_LTR);
        field.setSelectAllOnFocus(true);
        field.setBackgroundResource(R.drawable.bg_field);
        field.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        return field;
    }

    private Button actionButton(String value) {
        Button button = new Button(getContext());
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(AppSettings.primaryColor(getContext()));
        button.setBackgroundColor(Color.TRANSPARENT);
        button.setPadding(dp(14), 0, dp(14), 0);
        return button;
    }

    private TextView label(String value, int size, int color) {
        TextView view = new TextView(getContext());
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams wheelParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1f);
        params.leftMargin = dp(4);
        params.rightMargin = dp(4);
        return params;
    }

    private static String[] periodLabels() {
        Locale locale = AppString.locale();
        String language = locale.getLanguage();
        if ("fa".equals(language)) return new String[]{"ق.ظ", "ب.ظ"};
        if ("ar".equals(language)) return new String[]{"ص", "م"};
        if ("en".equals(language)) return new String[]{"AM", "PM"};
        String[] symbols = new DateFormatSymbols(locale).getAmPmStrings();
        return symbols != null && symbols.length == 2 ? symbols : new String[]{"AM", "PM"};
    }

    private static int parse(EditText field) {
        String text = latinDigits(field.getText().toString().trim());
        if (text.isEmpty()) return -1;
        try { return Integer.parseInt(text); } catch (NumberFormatException e) { return -1; }
    }

    private static String latinDigits(String value) {
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch >= '\u06F0' && ch <= '\u06F9') ch = (char) ('0' + ch - '\u06F0');
            else if (ch >= '\u0660' && ch <= '\u0669') ch = (char) ('0' + ch - '\u0660');
            out.append(ch);
        }
        return out.toString();
    }

    private static int hour12(int hour24) {
        int h = hour24 % 12;
        return h == 0 ? 12 : h;
    }

    private static int to24(int hour12, int period) {
        int h = hour12 % 12;
        return period == 1 ? h + 12 : h;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
