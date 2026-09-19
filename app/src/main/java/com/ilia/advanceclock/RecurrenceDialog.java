package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public final class RecurrenceDialog {
    public interface Callback {
        void onConfigured(int mode, int intervalDays, String customDatesJson);
    }

    private RecurrenceDialog() {}

    public static void show(Context context, long baseMillis, int mode, int intervalDays,
                            String customDatesJson, Callback callback) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context,18), dp(context,12), dp(context,18), dp(context,8));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Spinner modeSpinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, RecurrenceUtils.labels());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);
        modeSpinner.setSelection(Math.max(0, Math.min(6, mode)));
        root.addView(modeSpinner, new LinearLayout.LayoutParams(-1, dp(context,54)));

        TextView intervalLabel = new TextView(context);
        intervalLabel.setText("فاصله تکرار بر حسب روز");
        intervalLabel.setTextColor(AppSettings.textSecondary(context));
        intervalLabel.setPadding(0, dp(context,8),0,0);
        root.addView(intervalLabel);

        NumberPicker interval = new NumberPicker(context);
        interval.setMinValue(1);
        interval.setMaxValue(365);
        interval.setValue(Math.max(1, intervalDays));
        root.addView(interval, new LinearLayout.LayoutParams(-1, dp(context,110)));

        ArrayList<Long> customDates = new ArrayList<>(RecurrenceUtils.parseDates(customDatesJson));
        TextView customSummary = new TextView(context);
        customSummary.setTextColor(AppSettings.textPrimary(context));
        customSummary.setPadding(0, dp(context,8),0,dp(context,8));
        root.addView(customSummary);

        Button addDate = new Button(context);
        addDate.setText("افزودن تاریخ دلخواه");
        addDate.setAllCaps(false);
        root.addView(addDate, new LinearLayout.LayoutParams(-1, dp(context,50)));

        Button clearDates = new Button(context);
        clearDates.setText("پاک کردن تاریخ‌های دلخواه");
        clearDates.setAllCaps(false);
        root.addView(clearDates, new LinearLayout.LayoutParams(-1, dp(context,48)));

        Runnable refresh = () -> {
            int selectedMode = modeSpinner.getSelectedItemPosition();
            boolean intervalVisible = selectedMode == RecurrenceUtils.INTERVAL_DAYS;
            boolean datesVisible = selectedMode == RecurrenceUtils.CUSTOM_DATES;
            intervalLabel.setVisibility(intervalVisible ? View.VISIBLE : View.GONE);
            interval.setVisibility(intervalVisible ? View.VISIBLE : View.GONE);
            customSummary.setVisibility(datesVisible ? View.VISIBLE : View.GONE);
            addDate.setVisibility(datesVisible ? View.VISIBLE : View.GONE);
            clearDates.setVisibility(datesVisible ? View.VISIBLE : View.GONE);

            StringBuilder sb = new StringBuilder();
            for (Long date : customDates) {
                if (sb.length() > 0) sb.append("\n");
                sb.append("• ").append(CalendarUtils.formatDate(date, AppSettings.defaultCalendar(context)));
            }
            customSummary.setText(sb.length() == 0 ? "هنوز تاریخی انتخاب نشده" : sb.toString());
        };

        modeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refresh.run();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        addDate.setOnClickListener(v -> CalendarPickerDialog.showDate(
                context,
                baseMillis,
                AppSettings.defaultCalendar(context),
                (picked, type) -> {
                    java.util.Calendar base = java.util.Calendar.getInstance();
                    base.setTimeInMillis(baseMillis);
                    java.util.Calendar d = java.util.Calendar.getInstance();
                    d.setTimeInMillis(picked);
                    d.set(java.util.Calendar.HOUR_OF_DAY, base.get(java.util.Calendar.HOUR_OF_DAY));
                    d.set(java.util.Calendar.MINUTE, base.get(java.util.Calendar.MINUTE));
                    d.set(java.util.Calendar.SECOND, 0);
                    d.set(java.util.Calendar.MILLISECOND, 0);
                    customDates.add(d.getTimeInMillis());
                    java.util.Collections.sort(customDates);
                    refresh.run();
                }
        ));

        clearDates.setOnClickListener(v -> {
            customDates.clear();
            refresh.run();
        });

        refresh.run();

        new AlertDialog.Builder(context)
                .setTitle("تنظیم تکرار")
                .setView(root)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("تأیید", (d,w) -> callback.onConfigured(
                        modeSpinner.getSelectedItemPosition(),
                        interval.getValue(),
                        RecurrenceUtils.toJson(customDates)
                ))
                .show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
