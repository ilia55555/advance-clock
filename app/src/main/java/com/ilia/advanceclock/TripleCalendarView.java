package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.icu.util.IslamicCalendar;
import android.icu.util.PersianCalendar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class TripleCalendarView extends LinearLayout {
    public interface OnDateSelectedListener {
        void onDateSelected(long timeInMillis);
    }

    private static final int TEAL = 0xFF006F6B;
    private static final int TEAL_2 = 0xFF0A8882;
    private static final int ORANGE = 0xFFF17600;
    private static final int TEXT = 0xFF314541;
    private static final int MUTED = 0xFF8A9995;
    private static final int CELL = 0xFFF5F7F7;
    private static final int FRIDAY = 0xFFFFF1E5;
    private static final String[] PERSIAN_MONTHS = {
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };
    private static final String[] WEEKDAYS = {
            "شنبه", "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    };
    private static final String[] HIJRI_MONTHS = {
            "محرم", "صفر", "ربیع الاول", "ربیع الثانی", "جمادی الاول", "جمادی الثانی",
            "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    };

    private int displayYear;
    private int displayMonth;
    private long selectedMillis;
    private TextView monthTitle;
    private GridLayout grid;
    private TextView rangeText;
    private TextView persianDate;
    private TextView gregorianDate;
    private TextView hijriDate;
    private OnDateSelectedListener listener;

    public TripleCalendarView(Context context) {
        this(context, null);
    }

    public TripleCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setPadding(dp(14), dp(14), dp(14), dp(14));
        setBackground(roundRect(0xFFFFFFFF, 22, 0xFFE3ECEC, 1));
        setElevation(dp(2));

        PersianCalendar now = new PersianCalendar();
        selectedMillis = System.currentTimeMillis();
        now.setTimeInMillis(selectedMillis);
        displayYear = now.get(android.icu.util.Calendar.YEAR);
        displayMonth = now.get(android.icu.util.Calendar.MONTH);

        build();
        render();
    }

    public void setOnDateSelectedListener(OnDateSelectedListener listener) {
        this.listener = listener;
    }

    public long getSelectedMillis() {
        return selectedMillis;
    }

    public void setSelectedMillis(long millis) {
        selectedMillis = millis;
        PersianCalendar pc = new PersianCalendar();
        pc.setTimeInMillis(millis);
        displayYear = pc.get(android.icu.util.Calendar.YEAR);
        displayMonth = pc.get(android.icu.util.Calendar.MONTH);
        render();
    }

    private void build() {
        LinearLayout header = new LinearLayout(getContext());
        header.setOrientation(HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        header.setPadding(dp(6), 0, dp(6), 0);
        header.setBackground(roundRect(TEAL, 16, TEAL, 0));

        TextView prev = nav("‹  ماه قبل");
        monthTitle = nav("");
        monthTitle.setTextSize(19);
        monthTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView next = nav("ماه بعد  ›");

        header.addView(prev, weighted());
        header.addView(monthTitle, weighted());
        header.addView(next, weighted());
        addView(header, new LayoutParams(LayoutParams.MATCH_PARENT, dp(58)));

        prev.setOnClickListener(v -> shiftMonth(-1));
        next.setOnClickListener(v -> shiftMonth(1));

        LinearLayout week = new LinearLayout(getContext());
        week.setOrientation(HORIZONTAL);
        week.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        week.setGravity(Gravity.CENTER);
        LayoutParams weekLp = new LayoutParams(LayoutParams.MATCH_PARENT, dp(42));
        weekLp.topMargin = dp(6);
        addView(week, weekLp);
        for (int i = 0; i < WEEKDAYS.length; i++) {
            TextView day = new TextView(getContext());
            day.setText(WEEKDAYS[i]);
            day.setGravity(Gravity.CENTER);
            day.setTextSize(12);
            day.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            day.setTextColor(i == 6 ? ORANGE : TEAL);
            week.addView(day, weighted());
        }

        grid = new GridLayout(getContext());
        grid.setColumnCount(7);
        grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);
        grid.setUseDefaultMargins(false);
        grid.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LayoutParams gridLp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        addView(grid, gridLp);

        rangeText = new TextView(getContext());
        rangeText.setGravity(Gravity.CENTER);
        rangeText.setTextColor(TEAL);
        rangeText.setTextSize(16);
        rangeText.setPadding(0, dp(12), 0, dp(9));
        addView(rangeText, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout dates = new LinearLayout(getContext());
        dates.setOrientation(HORIZONTAL);
        dates.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        dates.setGravity(Gravity.CENTER);
        addView(dates, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        persianDate = dateBox();
        gregorianDate = dateBox();
        hijriDate = dateBox();
        dates.addView(persianDate, weighted());
        dates.addView(gregorianDate, weighted());
        dates.addView(hijriDate, weighted());
    }

    private void shiftMonth(int delta) {
        int m = displayMonth + delta;
        int y = displayYear;
        if (m < 0) {
            m = 11;
            y--;
        } else if (m > 11) {
            m = 0;
            y++;
        }
        displayYear = y;
        displayMonth = m;
        PersianCalendar pc = new PersianCalendar();
        pc.clear();
        pc.set(displayYear, displayMonth, 1, 12, 0, 0);
        selectedMillis = pc.getTimeInMillis();
        render();
        if (listener != null) listener.onDateSelected(selectedMillis);
    }

    private void render() {
        monthTitle.setText(PERSIAN_MONTHS[displayMonth] + "  " + fa(displayYear));
        grid.removeAllViews();

        PersianCalendar first = new PersianCalendar();
        first.clear();
        first.set(displayYear, displayMonth, 1, 12, 0, 0);
        Calendar firstG = Calendar.getInstance();
        firstG.setTimeInMillis(first.getTimeInMillis());
        int leading = firstG.get(Calendar.DAY_OF_WEEK) % 7;
        int days = first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH);

        for (int i = 0; i < leading; i++) {
            View blank = new View(getContext());
            grid.addView(blank, cellParams());
        }

        PersianCalendar selectedP = new PersianCalendar();
        selectedP.setTimeInMillis(selectedMillis);
        int selectedYear = selectedP.get(android.icu.util.Calendar.YEAR);
        int selectedMonth = selectedP.get(android.icu.util.Calendar.MONTH);
        int selectedDay = selectedP.get(android.icu.util.Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= days; day++) {
            PersianCalendar pc = new PersianCalendar();
            pc.clear();
            pc.set(displayYear, displayMonth, day, 12, 0, 0);
            long millis = pc.getTimeInMillis();

            Calendar gc = Calendar.getInstance();
            gc.setTimeInMillis(millis);
            IslamicCalendar hc = new IslamicCalendar();
            hc.setTimeInMillis(millis);

            int column = gc.get(Calendar.DAY_OF_WEEK) % 7;
            boolean selected = displayYear == selectedYear && displayMonth == selectedMonth && day == selectedDay;
            boolean friday = column == 6;

            LinearLayout cell = new LinearLayout(getContext());
            cell.setOrientation(VERTICAL);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(2), dp(5), dp(2), dp(4));
            int fill = selected ? TEAL_2 : (friday ? FRIDAY : CELL);
            cell.setBackground(roundRect(fill, 11, selected ? TEAL_2 : 0x00000000, 0));

            TextView main = new TextView(getContext());
            main.setGravity(Gravity.CENTER);
            main.setText(fa(day));
            main.setTextSize(20);
            main.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
            main.setTextColor(selected ? 0xFFFFFFFF : (friday ? ORANGE : TEXT));

            TextView secondary = new TextView(getContext());
            secondary.setGravity(Gravity.CENTER);
            secondary.setText(
                    fa(hc.get(android.icu.util.Calendar.DAY_OF_MONTH))
                            + "   " + gc.get(Calendar.DAY_OF_MONTH)
            );
            secondary.setTextSize(10);
            secondary.setTextColor(selected ? 0xFFD8F3F1 : (friday ? 0xFFF49A51 : MUTED));

            cell.addView(main, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));
            cell.addView(secondary, new LayoutParams(LayoutParams.MATCH_PARENT, dp(18)));
            cell.setOnClickListener(v -> {
                selectedMillis = millis;
                render();
                if (listener != null) listener.onDateSelected(millis);
            });
            grid.addView(cell, cellParams());
        }

        updateFooter();
    }

    private void updateFooter() {
        PersianCalendar first = new PersianCalendar();
        first.clear();
        first.set(displayYear, displayMonth, 1, 12, 0, 0);
        PersianCalendar last = new PersianCalendar();
        last.clear();
        last.set(displayYear, displayMonth,
                first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH), 12, 0, 0);

        SimpleDateFormat month = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        SimpleDateFormat year = new SimpleDateFormat("yyyy", Locale.ENGLISH);
        String firstMonth = month.format(first.getTime());
        String lastMonth = month.format(last.getTime());
        String yr = year.format(last.getTime());
        rangeText.setText(firstMonth.equals(lastMonth)
                ? firstMonth + " " + yr
                : firstMonth + " - " + lastMonth + " " + yr);

        PersianCalendar pc = new PersianCalendar();
        pc.setTimeInMillis(selectedMillis);
        Calendar gc = Calendar.getInstance();
        gc.setTimeInMillis(selectedMillis);
        IslamicCalendar hc = new IslamicCalendar();
        hc.setTimeInMillis(selectedMillis);

        persianDate.setText(
                fa(pc.get(android.icu.util.Calendar.DAY_OF_MONTH)) + " "
                        + PERSIAN_MONTHS[pc.get(android.icu.util.Calendar.MONTH)] + " "
                        + fa(pc.get(android.icu.util.Calendar.YEAR))
                        + "\nهجری شمسی"
        );
        gregorianDate.setText(
                new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(gc.getTime())
                        + "\nمیلادی"
        );
        hijriDate.setText(
                fa(hc.get(android.icu.util.Calendar.DAY_OF_MONTH)) + " "
                        + HIJRI_MONTHS[hc.get(android.icu.util.Calendar.MONTH)] + " "
                        + fa(hc.get(android.icu.util.Calendar.YEAR))
                        + "\nهجری قمری"
        );
    }

    private TextView nav(String text) {
        TextView view = new TextView(getContext());
        view.setText(text);
        view.setTextColor(0xFFFFFFFF);
        view.setTextSize(13);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(4), 0, dp(4), 0);
        return view;
    }

    private TextView dateBox() {
        TextView view = new TextView(getContext());
        view.setGravity(Gravity.CENTER);
        view.setTextColor(TEAL);
        view.setTextSize(11);
        view.setLineSpacing(0, 1.2f);
        view.setPadding(dp(3), dp(4), dp(3), 0);
        return view;
    }

    private GridLayout.LayoutParams cellParams() {
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0;
        lp.height = dp(62);
        lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        return lp;
    }

    private LayoutParams weighted() {
        return new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
    }

    private GradientDrawable roundRect(int color, int radiusDp, int strokeColor, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), strokeColor);
        return d;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static String fa(int value) {
        return fa(String.valueOf(value));
    }

    private static String fa(String value) {
        char[] en = {'0','1','2','3','4','5','6','7','8','9'};
        char[] pe = {'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};
        String out = value;
        for (int i = 0; i < en.length; i++) out = out.replace(en[i], pe[i]);
        return out;
    }
}
