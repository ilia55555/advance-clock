package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

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
        modeSpinner.setSelection(Math.max(0, Math.min(7, mode)));
        root.addView(modeSpinner, new LinearLayout.LayoutParams(-1, dp(context,54)));

        TextView intervalLabel = new TextView(context);
        intervalLabel.setText(AppString.get(R.string.runtime_text_0288));
        intervalLabel.setTextColor(AppSettings.textSecondary(context));
        intervalLabel.setPadding(0, dp(context,8),0,0);
        root.addView(intervalLabel);

        NumberPicker interval = new NumberPicker(context);
        interval.setMinValue(1);
        interval.setMaxValue(365);
        interval.setValue(Math.max(1, intervalDays));
        root.addView(interval, new LinearLayout.LayoutParams(-1, dp(context,110)));

        TextView weekIntervalLabel = new TextView(context);
        weekIntervalLabel.setText(AppString.get(R.string.runtime_text_0289));
        weekIntervalLabel.setTextColor(AppSettings.textSecondary(context));
        root.addView(weekIntervalLabel);
        NumberPicker weekInterval = new NumberPicker(context);
        weekInterval.setMinValue(1); weekInterval.setMaxValue(52);
        weekInterval.setValue(Math.max(1, intervalDays));
        root.addView(weekInterval, new LinearLayout.LayoutParams(-1, dp(context, 90)));

        Switch permanentWeekdays = new Switch(context);
        permanentWeekdays.setText(AppString.get(R.string.runtime_text_0678));
        permanentWeekdays.setTextColor(AppSettings.textPrimary(context));
        permanentWeekdays.setChecked(
                mode != RecurrenceUtils.WEEKDAYS
                        || RecurrenceUtils.isPermanentWeekdays(customDatesJson));
        root.addView(permanentWeekdays, new LinearLayout.LayoutParams(-1, dp(context, 48)));

        LinearLayout weekdayGrid = new LinearLayout(context);
        weekdayGrid.setOrientation(LinearLayout.VERTICAL);
        String[] dayNames = {AppString.get(R.string.runtime_text_0180), AppString.get(R.string.runtime_text_0181), AppString.get(R.string.runtime_text_0182), AppString.get(R.string.runtime_text_0183), AppString.get(R.string.runtime_text_0184), AppString.get(R.string.runtime_text_0185), AppString.get(R.string.runtime_text_0186)};
        int[] dayValues = {Calendar.SATURDAY, Calendar.SUNDAY, Calendar.MONDAY,
                Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY};
        java.util.List<Integer> selectedWeekdays = RecurrenceUtils.parseWeekdays(customDatesJson);
        android.widget.CheckBox[] dayChecks = new android.widget.CheckBox[7];
        for (int rowIndex = 0; rowIndex < 2; rowIndex++) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int index = rowIndex * 4; index < Math.min(7, rowIndex * 4 + 4); index++) {
                android.widget.CheckBox check = new android.widget.CheckBox(context);
                check.setText(dayNames[index]); check.setChecked(selectedWeekdays.contains(dayValues[index]));
                dayChecks[index] = check; row.addView(check, new LinearLayout.LayoutParams(0, dp(context,48),1));
            }
            weekdayGrid.addView(row);
        }
        root.addView(weekdayGrid);

        ArrayList<Long> customDates = new ArrayList<>(RecurrenceUtils.parseDates(customDatesJson));
        TextView customSummary = new TextView(context);
        customSummary.setTextColor(AppSettings.textPrimary(context));
        customSummary.setPadding(0, dp(context,8),0,dp(context,8));
        root.addView(customSummary);

        Button addDate = new Button(context);
        addDate.setText(AppString.get(R.string.runtime_text_0290));
        addDate.setAllCaps(false);
        root.addView(addDate, new LinearLayout.LayoutParams(-1, dp(context,50)));

        Button clearDates = new Button(context);
        clearDates.setText(AppString.get(R.string.runtime_text_0291));
        clearDates.setAllCaps(false);
        root.addView(clearDates, new LinearLayout.LayoutParams(-1, dp(context,48)));

        Runnable refresh = () -> {
            int selectedMode = modeSpinner.getSelectedItemPosition();
            boolean intervalVisible = selectedMode == RecurrenceUtils.INTERVAL_DAYS;
            boolean datesVisible = selectedMode == RecurrenceUtils.CUSTOM_DATES;
            boolean weekdaysVisible = selectedMode == RecurrenceUtils.WEEKDAYS;
            intervalLabel.setVisibility(intervalVisible ? View.VISIBLE : View.GONE);
            interval.setVisibility(intervalVisible ? View.VISIBLE : View.GONE);
            customSummary.setVisibility(datesVisible ? View.VISIBLE : View.GONE);
            addDate.setVisibility(datesVisible ? View.VISIBLE : View.GONE);
            clearDates.setVisibility(datesVisible ? View.VISIBLE : View.GONE);
            weekInterval.setVisibility(weekdaysVisible
                    && !permanentWeekdays.isChecked() ? View.VISIBLE : View.GONE);
            weekIntervalLabel.setVisibility(weekdaysVisible
                    && !permanentWeekdays.isChecked() ? View.VISIBLE : View.GONE);
            permanentWeekdays.setVisibility(weekdaysVisible ? View.VISIBLE : View.GONE);
            weekdayGrid.setVisibility(weekdaysVisible ? View.VISIBLE : View.GONE);

            StringBuilder sb = new StringBuilder();
            for (Long date : customDates) {
                if (sb.length() > 0) sb.append("\n");
                String clock = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(new java.util.Date(date));
                sb.append("• ")
                        .append(CalendarUtils.formatDate(date, AppSettings.defaultCalendar(context)))
                        .append("  ")
                        .append(clock);
            }
            customSummary.setText(sb.length() == 0 ? AppString.get(R.string.runtime_text_0292) : sb.toString());
        };

        modeSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                refresh.run();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        permanentWeekdays.setOnCheckedChangeListener((button, checked) -> refresh.run());

        addDate.setOnClickListener(v -> CalendarPickerDialog.showDate(
                context,
                Math.max(baseMillis, System.currentTimeMillis()),
                AppSettings.defaultCalendar(context),
                (picked, type) -> {
                    Calendar base = Calendar.getInstance();
                    base.setTimeInMillis(baseMillis);
                    Calendar chosen = Calendar.getInstance();
                    chosen.setTimeInMillis(picked);

                    int defaultHour = base.get(Calendar.HOUR_OF_DAY);
                    int defaultMinute = base.get(Calendar.MINUTE);

                    new TimePickerDialog(context, (timeView, hour, minute) -> {
                        chosen.set(Calendar.HOUR_OF_DAY, hour);
                        chosen.set(Calendar.MINUTE, minute);
                        chosen.set(Calendar.SECOND, 0);
                        chosen.set(Calendar.MILLISECOND, 0);

                        if (chosen.getTimeInMillis() <= System.currentTimeMillis()) {
                            LogoToast.makeText(context, AppString.get(R.string.runtime_text_0293), Toast.LENGTH_SHORT).show();
                            return;
                        }

                        customDates.add(chosen.getTimeInMillis());
                        java.util.Collections.sort(customDates);
                        refresh.run();
                    }, defaultHour, defaultMinute, true).show();
                }
        ));

        clearDates.setOnClickListener(v -> {
            customDates.clear();
            refresh.run();
        });

        refresh.run();

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(AppString.get(R.string.runtime_text_0287))
                .setView(root)
                .setNegativeButton(AppString.get(R.string.runtime_text_0003), null)
                .setPositiveButton(AppString.get(R.string.runtime_text_0004), (d,w) -> {
                    int selectedMode = modeSpinner.getSelectedItemPosition();
                    ArrayList<Integer> weekdays = new ArrayList<>();
                    for (int index = 0; index < dayChecks.length; index++)
                        if (dayChecks[index].isChecked()) weekdays.add(dayValues[index]);
                    callback.onConfigured(
                            selectedMode,
                            selectedMode == RecurrenceUtils.WEEKDAYS
                                    ? weekInterval.getValue() : interval.getValue(),
                            selectedMode == RecurrenceUtils.WEEKDAYS
                                    ? RecurrenceUtils.weekdaysToJson(
                                            weekdays, permanentWeekdays.isChecked())
                                    : RecurrenceUtils.toJson(customDates));
                })
                .create();
        dialog.show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
