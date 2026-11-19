package com.ilia.advanceclock;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.DateFormatSymbols;
import java.util.Locale;

/**
 * Shared time picker with two modes: the round clock and a wheel. Drag a
 * wheel to spin it, tap its value to type with the numeric keyboard. The
 * wheel follows the app's 12/24-hour setting; the last mode is remembered.
 */
class TimeEntryDialog extends Dialog {
    private static final int MODE_CLOCK = 0;
    private static final int MODE_WHEEL = 1;
    private static final String PREFS = "time_entry_dialog";
    private static final String KEY_MODE = "mode";

    private final TimePickerDialog.OnTimeSetListener listener;
    private final boolean app24;
    private final View clockView;
    private final ClockDialView dial;
    private final TextView headerHour;
    private final TextView headerMinute;
    private final Button amButton;
    private final Button pmButton;
    private final View wheelView;
    private final Button[] tabs = new Button[2];
    private int mode;
    private final WheelPickerView hourPicker;
    private final WheelPickerView minutePicker;
    private final WheelPickerView periodPicker;

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

        LinearLayout tabRow = new LinearLayout(context);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        tabRow.setPadding(dp(4), dp(4), dp(4), dp(4));
        tabRow.setBackgroundResource(R.drawable.bg_field);
        int[] labels = {R.string.time_picker_mode_clock, R.string.time_picker_mode_wheel};
        for (int i = 0; i < tabs.length; i++) {
            final int target = i;
            Button tab = new Button(context);
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

        LinearLayout clockPanel = new LinearLayout(context);
        clockPanel.setOrientation(LinearLayout.VERTICAL);
        clockPanel.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        headerHour = headerText(context);
        headerMinute = headerText(context);
        TextView headerColon = headerText(context);
        headerColon.setText(":");
        headerColon.setTextColor(AppSettings.textSecondary(context));
        header.addView(headerHour, new LinearLayout.LayoutParams(-2, -2));
        header.addView(headerColon, new LinearLayout.LayoutParams(-2, -2));
        header.addView(headerMinute, new LinearLayout.LayoutParams(-2, -2));
        if (app24) {
            amButton = null;
            pmButton = null;
        } else {
            LinearLayout periods = new LinearLayout(context);
            periods.setOrientation(LinearLayout.VERTICAL);
            String[] periodNames = periodLabels();
            amButton = periodButton(context, periodNames[0]);
            pmButton = periodButton(context, periodNames[1]);
            periods.addView(amButton, new LinearLayout.LayoutParams(dp(56), dp(32)));
            LinearLayout.LayoutParams pmParams = new LinearLayout.LayoutParams(dp(56), dp(32));
            pmParams.topMargin = dp(4);
            periods.addView(pmButton, pmParams);
            LinearLayout.LayoutParams periodsParams = new LinearLayout.LayoutParams(-2, -2);
            periodsParams.leftMargin = dp(12);
            header.addView(periods, periodsParams);
        }
        clockPanel.addView(header, new LinearLayout.LayoutParams(-2, -2));
        dial = new ClockDialView(context);
        dial.setTime(hour, minute);
        LinearLayout.LayoutParams dialParams = new LinearLayout.LayoutParams(-2, -2);
        dialParams.topMargin = dp(12);
        clockPanel.addView(dial, dialParams);
        clockView = clockPanel;
        dial.setListener(new ClockDialView.Listener() {
            @Override public void onTimeChanged(int h, int m) { renderHeader(); }
            @Override public void onModeChanged(boolean minutes) { renderHeader(); }
        });
        headerHour.setOnClickListener(v -> dial.setMinuteMode(false, true));
        headerMinute.setOnClickListener(v -> dial.setMinuteMode(true, true));
        if (amButton != null) {
            amButton.setOnClickListener(v -> {
                if (dial.hour() >= 12) dial.changeHour(dial.hour() - 12);
            });
            pmButton.setOnClickListener(v -> {
                if (dial.hour() < 12) dial.changeHour(dial.hour() + 12);
            });
        }
        renderHeader();

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setPadding(dp(8), dp(4), dp(8), dp(4));

        hourPicker = new WheelPickerView(context);
        if (app24) {
            hourPicker.setRange(0, 23, true);
            hourPicker.setValue(hour);
        } else {
            int h12 = hour % 12;
            hourPicker.setRange(1, 12, false);
            hourPicker.setValue(h12 == 0 ? 12 : h12);
        }
        minutePicker = new WheelPickerView(context);
        minutePicker.setRange(0, 59, true);
        minutePicker.setValue(minute);
        hourPicker.setTypeable(true);
        minutePicker.setTypeable(true);

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
            periodPicker = new WheelPickerView(context);
            periodPicker.setLabels(periodLabels());
            periodPicker.setWrap(false);
            periodPicker.setTextSize(22);
            periodPicker.setValue(hour >= 12 ? 1 : 0);
            row.addView(periodPicker, wheelParams());
        }
        wheelView = row;

        // Both modes share one frame so the dialog keeps the same size.
        FrameLayout content = new FrameLayout(context);
        content.addView(clockView, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        content.addView(row, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(-1, -2);
        contentParams.topMargin = dp(12);
        root.addView(content, contentParams);

        int saved = prefs().getInt(KEY_MODE, MODE_CLOCK);
        mode = saved == MODE_WHEEL ? MODE_WHEEL : MODE_CLOCK;
        showMode();

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

    private void switchMode(int target) {
        if (target == mode) return;
        commitWheel();
        int hour = mode == MODE_CLOCK ? dial.hour() : wheelHour();
        int minute = mode == MODE_CLOCK ? dial.minute() : minutePicker.getValue();
        if (target == MODE_CLOCK) {
            dial.setTime(hour, minute);
            dial.setMinuteMode(false, false);
            renderHeader();
        } else {
            setWheel(hour, minute);
        }
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
        clockView.setVisibility(mode == MODE_CLOCK ? View.VISIBLE : View.INVISIBLE);
        wheelView.setVisibility(mode == MODE_WHEEL ? View.VISIBLE : View.INVISIBLE);
    }

    private void renderHeader() {
        Context c = getContext();
        int h = dial.hour();
        int shown = app24 ? h : (h % 12 == 0 ? 12 : h % 12);
        headerHour.setText(String.format(Locale.US, "%02d", shown));
        headerMinute.setText(String.format(Locale.US, "%02d", dial.minute()));
        boolean minutes = dial.isMinuteMode();
        headerHour.setTextColor(minutes ? AppSettings.textSecondary(c) : AppSettings.primaryColor(c));
        headerMinute.setTextColor(minutes ? AppSettings.primaryColor(c) : AppSettings.textSecondary(c));
        if (amButton != null) {
            paintPeriod(amButton, h < 12);
            paintPeriod(pmButton, h >= 12);
        }
    }

    private TextView headerText(Context c) {
        TextView view = new TextView(c);
        view.setTextSize(44);
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setPadding(dp(4), 0, dp(4), 0);
        return view;
    }

    private Button periodButton(Context c, String label) {
        Button button = new Button(c);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setPadding(0, 0, 0, 0);
        button.setStateListAnimator(null);
        return button;
    }

    private void paintPeriod(Button button, boolean active) {
        Context c = getContext();
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(10));
        shape.setColor(active ? AppSettings.primaryColor(c) : Color.TRANSPARENT);
        shape.setStroke(dp(1), AppSettings.primaryColor(c));
        button.setBackground(shape);
        button.setTextColor(active ? Color.WHITE : AppSettings.primaryColor(c));
    }

    private int wheelHour() {
        int hour = hourPicker.getValue();
        if (app24) return hour;
        int h = hour % 12;
        return periodPicker != null && periodPicker.getValue() == 1 ? h + 12 : h;
    }

    private void setWheel(int hour, int minute) {
        if (app24) {
            hourPicker.setValue(hour);
        } else {
            int h12 = hour % 12;
            hourPicker.setValue(h12 == 0 ? 12 : h12);
            if (periodPicker != null) periodPicker.setValue(hour >= 12 ? 1 : 0);
        }
        minutePicker.setValue(minute);
    }

    private void commitWheel() {
        hourPicker.commitTyping();
        minutePicker.commitTyping();
    }

    private SharedPreferences prefs() {
        return getContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private void confirm() {
        commitWheel();
        int hour = mode == MODE_CLOCK ? dial.hour() : wheelHour();
        int minute = mode == MODE_CLOCK ? dial.minute() : minutePicker.getValue();
        if (listener != null) listener.onTimeSet(null, hour, minute);
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
