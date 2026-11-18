package com.ilia.advanceclock;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Locale;

/**
 * Shared wheel time picker. Drag a wheel to spin it, tap its value to type
 * with the numeric keyboard. Follows the app's 12/24-hour setting.
 */
class TimeEntryDialog extends Dialog {
    private final TimePickerDialog.OnTimeSetListener listener;
    private final boolean app24;
    private final NumberPicker hourPicker;
    private final NumberPicker minutePicker;
    private final NumberPicker periodPicker;

    TimeEntryDialog(Context context, TimePickerDialog.OnTimeSetListener listener,
                    int hour, int minute, boolean full) {
        super(context);
        this.listener = listener;
        this.app24 = AppSettings.clock24Hour(context);
        hour = Math.max(0, Math.min(23, hour));
        minute = Math.max(0, Math.min(59, minute));
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(context));
        root.setPadding(dp(16), dp(18), dp(16), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppSettings.surface(context));
        bg.setCornerRadius(dp(24));
        root.setBackground(bg);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setBackgroundResource(R.drawable.bg_field);
        row.setPadding(dp(8), dp(4), dp(8), dp(4));

        NumberPicker.Formatter twoDigits = v -> String.format(Locale.US, "%02d", v);
        hourPicker = new NumberPicker(context);
        if (app24) {
            hourPicker.setMinValue(0);
            hourPicker.setMaxValue(23);
            hourPicker.setFormatter(twoDigits);
            hourPicker.setValue(hour);
        } else {
            int h12 = hour % 12;
            hourPicker.setMinValue(1);
            hourPicker.setMaxValue(12);
            hourPicker.setValue(h12 == 0 ? 12 : h12);
        }
        minutePicker = new NumberPicker(context);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setFormatter(twoDigits);
        minutePicker.setValue(minute);
        styleTypeable(hourPicker);
        styleTypeable(minutePicker);

        row.addView(hourPicker, wheelParams());
        TextView colon = new TextView(context);
        colon.setText(":");
        colon.setTextSize(26);
        colon.setTextColor(AppSettings.primaryColor(context));
        colon.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        colon.setGravity(Gravity.CENTER);
        row.addView(colon, new LinearLayout.LayoutParams(dp(18), -2));
        row.addView(minutePicker, wheelParams());

        if (app24) {
            periodPicker = null;
        } else {
            periodPicker = new NumberPicker(context);
            periodPicker.setMinValue(0);
            periodPicker.setMaxValue(1);
            periodPicker.setDisplayedValues(periodLabels());
            periodPicker.setValue(hour >= 12 ? 1 : 0);
            periodPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
            row.addView(periodPicker, wheelParams());
        }
        root.addView(row, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actions = new LinearLayout(context);
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
        actionParams.topMargin = dp(10);
        root.addView(actions, actionParams);

        setContentView(root);
        setCanceledOnTouchOutside(true);
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            int width = Math.min(
                    context.getResources().getDisplayMetrics().widthPixels - dp(32), dp(360));
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    /** Lets a tap on the centre value open the numeric keyboard. */
    private void styleTypeable(NumberPicker picker) {
        picker.setDescendantFocusability(NumberPicker.FOCUS_AFTER_DESCENDANTS);
        EditText input = input(picker);
        if (input != null) {
            input.setInputType(InputType.TYPE_CLASS_NUMBER);
            input.setTextDirection(View.TEXT_DIRECTION_LTR);
        }
    }

    private static EditText input(NumberPicker picker) {
        for (int i = 0; i < picker.getChildCount(); i++) {
            View child = picker.getChildAt(i);
            if (child instanceof EditText) return (EditText) child;
        }
        return null;
    }

    private void confirm() {
        // Commit any value still being typed into a wheel.
        for (NumberPicker picker : new NumberPicker[]{hourPicker, minutePicker}) {
            EditText input = input(picker);
            if (input != null && input.hasFocus()) input.clearFocus();
            picker.clearFocus();
        }
        int hour = hourPicker.getValue();
        if (!app24) {
            int h = hour % 12;
            hour = periodPicker != null && periodPicker.getValue() == 1 ? h + 12 : h;
        }
        if (listener != null) {
            listener.onTimeSet(new android.widget.TimePicker(getContext()), hour,
                    minutePicker.getValue());
        }
        dismiss();
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

    private int dp(int value) {
        return Math.round(value * getContext().getResources().getDisplayMetrics().density);
    }
}
