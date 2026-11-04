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
        show(context,baseMillis,mode,intervalDays,customDatesJson,AppTime.zone(),callback);
    }
    public static void show(Context context,long baseMillis,int mode,int intervalDays,String customDatesJson,
                            java.util.TimeZone zone,Callback callback){
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context,18), dp(context,12), dp(context,18), dp(context,8));
        root.setLayoutDirection(AppSettings.layoutDirection(context));

        Spinner modeSpinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, RecurrenceUtils.labels());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);
        modeSpinner.setSelection(Math.max(0, Math.min(RecurrenceUtils.INTERVAL_MINUTES, mode)));
        root.addView(modeSpinner, new LinearLayout.LayoutParams(-1, dp(context,54)));

        TextView intervalLabel = new TextView(context);
        intervalLabel.setText(AppString.get(R.string.runtime_text_0288));
        intervalLabel.setTextColor(AppSettings.textSecondary(context));
        intervalLabel.setPadding(0, dp(context,8),0,0);
        root.addView(intervalLabel);

        NumberPicker interval = new NumberPicker(context);
        interval.setMinValue(1);
        interval.setMaxValue(mode == RecurrenceUtils.INTERVAL_MINUTES ? 525600 : Math.max(365, intervalDays));
        int initialPeriod=RecurrenceUtils.period(customDatesJson);
        if(mode==RecurrenceUtils.WEEKLY || mode==RecurrenceUtils.MONTHLY || mode==RecurrenceUtils.YEARLY) {
            interval.setMaxValue(Math.max(365,initialPeriod));interval.setValue(initialPeriod);
        } else interval.setValue(Math.max(1, intervalDays));
        root.addView(interval, new LinearLayout.LayoutParams(-1, dp(context,110)));

        Spinner intervalUnit=new Spinner(context);intervalUnit.setTag("recurrence_interval_unit");
        ArrayAdapter<String> units=new ArrayAdapter<>(context,android.R.layout.simple_spinner_item,new String[]{
                context.getString(R.string.event_unit_minutes_name),context.getString(R.string.event_unit_hours_name)});
        units.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);intervalUnit.setAdapter(units);
        boolean initialHours=mode==RecurrenceUtils.INTERVAL_MINUTES && intervalDays%60==0;
        intervalUnit.setSelection(initialHours?1:0);
        if(initialHours){interval.setValue(Math.max(1,intervalDays/60));interval.setMaxValue(8760);}
        interval.setTag("recurrence_interval_value");
        root.addView(intervalUnit,new LinearLayout.LayoutParams(-1,dp(context,48)));

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
            interval.setMaxValue(selectedMode == RecurrenceUtils.INTERVAL_MINUTES
                    ? (intervalUnit.getSelectedItemPosition()==1?8760:525600) : Math.max(Math.max(365,intervalDays),initialPeriod));
            intervalUnit.setVisibility(selectedMode==RecurrenceUtils.INTERVAL_MINUTES?View.VISIBLE:View.GONE);
            boolean intervalVisible = selectedMode == RecurrenceUtils.INTERVAL_DAYS || selectedMode == RecurrenceUtils.INTERVAL_MINUTES
                    || selectedMode==RecurrenceUtils.WEEKLY || selectedMode==RecurrenceUtils.MONTHLY || selectedMode==RecurrenceUtils.YEARLY;
            intervalLabel.setText(selectedMode == RecurrenceUtils.INTERVAL_MINUTES
                    ? R.string.recurrence_interval_minutes : selectedMode==RecurrenceUtils.WEEKLY?R.string.runtime_text_0289:
                    selectedMode==RecurrenceUtils.MONTHLY?R.string.recurrence_interval_months:
                    selectedMode==RecurrenceUtils.YEARLY?R.string.recurrence_interval_years:R.string.runtime_text_0288);
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
                java.text.SimpleDateFormat timeFormat=new java.text.SimpleDateFormat("HH:mm",java.util.Locale.getDefault());
                timeFormat.setTimeZone(zone);String clock=timeFormat.format(new java.util.Date(date));
                sb.append("• ")
                        .append(CalendarUtils.formatDate(date, AppSettings.defaultCalendar(context),zone))
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
        int[] previousUnit={initialHours?1:0};
        intervalUnit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            @Override public void onItemSelected(android.widget.AdapterView<?> parent,View view,int position,long id){
                if(position!=previousUnit[0]) {
                    int minutes=interval.getValue()*(previousUnit[0]==1?60:1);
                    interval.setMaxValue(position==1?8760:525600);
                    interval.setValue(Math.max(1,Math.min(interval.getMaxValue(),position==1?minutes/60:minutes)));
                    previousUnit[0]=position;
                }
                refresh.run();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });
        permanentWeekdays.setOnCheckedChangeListener((button, checked) -> refresh.run());

        addDate.setOnClickListener(v -> CalendarPickerDialog.showDate(
                context,
                WorldAlarmReference.pickerDate(Math.max(baseMillis,System.currentTimeMillis()),zone),
                AppSettings.defaultCalendar(context),
                (picked, type) -> {
                    Calendar base = Calendar.getInstance(zone);
                    base.setTimeInMillis(baseMillis);
                    Calendar chosen = Calendar.getInstance(AppTime.zone());
                    chosen.setTimeInMillis(picked);
                    WorldAlarmReference.changeZone(chosen,zone.getID());

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

        android.widget.ScrollView scroll=ReminderModalStyle.scroll(context,root);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(AppString.get(R.string.runtime_text_0287))
                .setView(scroll)
                .setNegativeButton(AppString.get(R.string.runtime_text_0003), null)
                .setPositiveButton(AppString.get(R.string.runtime_text_0004), (d,w) -> {
                    int selectedMode = modeSpinner.getSelectedItemPosition();
                    ArrayList<Integer> weekdays = new ArrayList<>();
                    for (int index = 0; index < dayChecks.length; index++)
                        if (dayChecks[index].isChecked()) weekdays.add(dayValues[index]);
                    callback.onConfigured(
                            selectedMode,
                            selectedMode == RecurrenceUtils.WEEKDAYS
                                    ? weekInterval.getValue() : interval.getValue()*(selectedMode==RecurrenceUtils.INTERVAL_MINUTES && intervalUnit.getSelectedItemPosition()==1?60:1),
                            selectedMode == RecurrenceUtils.WEEKDAYS
                                    ? RecurrenceUtils.weekdaysToJson(
                                            weekdays, permanentWeekdays.isChecked())
                                    : (selectedMode==RecurrenceUtils.WEEKLY || selectedMode==RecurrenceUtils.MONTHLY || selectedMode==RecurrenceUtils.YEARLY)
                                        ? RecurrenceUtils.periodJson(interval.getValue()):RecurrenceUtils.toJson(customDates));
                })
                .create();
        dialog.show();ReminderModalStyle.apply(dialog,scroll);
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
