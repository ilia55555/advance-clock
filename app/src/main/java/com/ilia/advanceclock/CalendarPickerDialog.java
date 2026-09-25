package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public final class CalendarPickerDialog {
    public interface Callback {
        void onPicked(long millis, int calendarType);
    }

    private CalendarPickerDialog() {}

    public static void showDate(Context context, long initialMillis, int initialType, Callback callback) {
        show(context, initialMillis, initialType, false, true, callback);
    }

    public static void showDateAny(Context context, long initialMillis, int initialType,
                                   Callback callback) {
        show(context, initialMillis, initialType, false, false, callback);
    }

    public static void showMonthYear(Context context, long initialMillis, int initialType, Callback callback) {
        show(context, initialMillis, initialType, true, false, callback);
    }

    private static void show(Context context, long initialMillis, int initialType,
                             boolean monthYearOnly, boolean rejectPast, Callback callback) {
        int pad = dp(context, 18);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, dp(context, 8));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(context);
        title.setText(monthYearOnly ? "انتخاب ماه و سال" : "انتخاب تاریخ");
        title.setTextSize(20);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppSettings.textPrimary(context));
        title.setGravity(Gravity.START);
        root.addView(title);

        Spinner type = new Spinner(context);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item,
                new String[]{"شمسی", "میلادی", "قمری"});
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        type.setAdapter(typeAdapter);
        type.setSelection(Math.max(0, Math.min(2, initialType)));
        LinearLayout.LayoutParams typeLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 54));
        typeLp.topMargin = dp(context, 12);
        root.addView(type, typeLp);

        LinearLayout pickers = new LinearLayout(context);
        pickers.setOrientation(LinearLayout.HORIZONTAL);
        pickers.setGravity(Gravity.CENTER);
        pickers.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout.LayoutParams pickersLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 180));
        pickersLp.topMargin = dp(context, 8);
        root.addView(pickers, pickersLp);

        NumberPicker day = new NumberPicker(context);
        NumberPicker month = new NumberPicker(context);
        NumberPicker year = new NumberPicker(context);

        pickers.addView(year, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        pickers.addView(month, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        if (!monthYearOnly) {
            pickers.addView(day, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
        }

        final long[] currentMillis = {
                rejectPast ? Math.max(initialMillis, System.currentTimeMillis()) : initialMillis
        };
        final boolean[] updating = {false};

        Runnable refresh = () -> {
            updating[0] = true;
            int calType = type.getSelectedItemPosition();
            android.icu.util.Calendar c = CalendarUtils.fromMillis(calType, currentMillis[0]);

            int currentYear = c.get(android.icu.util.Calendar.YEAR);
            if (calType == CalendarUtils.PERSIAN) {
                year.setMinValue(1200);
                year.setMaxValue(1600);
            } else if (calType == CalendarUtils.HIJRI) {
                year.setMinValue(1200);
                year.setMaxValue(1700);
            } else {
                year.setMinValue(1900);
                year.setMaxValue(2200);
            }
            year.setValue(Math.max(year.getMinValue(), Math.min(year.getMaxValue(), currentYear)));

            month.setMinValue(0);
            month.setMaxValue(11);
            String[] names = new String[12];
            for (int i = 0; i < 12; i++) names[i] = CalendarUtils.monthName(calType, i);
            month.setDisplayedValues(null);
            month.setDisplayedValues(names);
            month.setValue(c.get(android.icu.util.Calendar.MONTH));

            if (!monthYearOnly) {
                android.icu.util.Calendar probe = CalendarUtils.create(calType);
                probe.clear();
                probe.set(year.getValue(), month.getValue(), 1);
                int maxDay = probe.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH);
                day.setMinValue(1);
                day.setMaxValue(maxDay);
                day.setValue(Math.max(1, Math.min(maxDay,
                        c.get(android.icu.util.Calendar.DAY_OF_MONTH))));
            }
            updating[0] = false;
        };

        type.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!updating[0]) refresh.run();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        NumberPicker.OnValueChangeListener changed = (picker, oldVal, newVal) -> {
            if (updating[0]) return;
            int calType = type.getSelectedItemPosition();
            int d = monthYearOnly ? 1 : day.getValue();
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis(initialMillis);
            currentMillis[0] = CalendarUtils.toMillis(
                    calType, year.getValue(), month.getValue(), d,
                    time.get(Calendar.HOUR_OF_DAY), time.get(Calendar.MINUTE));
            refresh.run();
        };
        year.setOnValueChangedListener(changed);
        month.setOnValueChangedListener(changed);
        if (!monthYearOnly) day.setOnValueChangedListener(changed);

        refresh.run();

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(root)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("تأیید", null);
        if (monthYearOnly) {
            builder.setNeutralButton("بازگشت به امروز", null);
        }
        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int calType = type.getSelectedItemPosition();
                int selectedDay = monthYearOnly ? 1 : day.getValue();
                Calendar time = Calendar.getInstance();
                time.setTimeInMillis(initialMillis);
                long millis = CalendarUtils.toMillis(
                        calType,
                        year.getValue(),
                        month.getValue(),
                        selectedDay,
                        time.get(Calendar.HOUR_OF_DAY),
                        time.get(Calendar.MINUTE)
                );

                if (rejectPast && millis < startOfToday()) {
                    Toast.makeText(
                            context,
                            "تاریخ گذشته قابل انتخاب نیست",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                callback.onPicked(millis, calType);
                dialog.dismiss();
            });
            if (monthYearOnly) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    callback.onPicked(
                            System.currentTimeMillis(),
                            type.getSelectedItemPosition());
                    dialog.dismiss();
                });
            }
        });
        dialog.show();
    }

    private static long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
