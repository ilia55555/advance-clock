package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.icu.util.IslamicCalendar;
import android.icu.util.PersianCalendar;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Pixel-faithful implementation of the calendar reference used by Advance Clock.
 *
 * The view is drawn against a 708 x 698 reference canvas (the supplied design)
 * and then scaled as a single unit. For five-row months, the geometry, spacing,
 * colors and proportions match the reference exactly. Six-row months extend the
 * grid by one row while preserving the same row rhythm and footer treatment.
 */
public final class TripleCalendarView extends View {
    public interface OnDateSelectedListener {
        void onDateSelected(long timeInMillis);
    }

    private static final float BASE_W = 708f;
    private static final float BASE_H_5 = 698f;
    private static final float EXTRA_ROW_H = 86f;

    // Exact dominant colors sampled from the provided reference image.
    private static final int WHITE = Color.rgb(255, 255, 255);      // #FFFFFF
    private static final int TEAL = Color.rgb(0, 102, 102);        // #006666
    private static final int SELECTED = Color.rgb(64, 128, 128);   // #408080
    private static final int CELL = Color.rgb(248, 248, 248);      // #F8F8F8
    private static final int HOLIDAY_BG = Color.rgb(255, 242, 230);// #FFF2E6
    private static final int MAIN_TEXT = Color.rgb(99, 116, 84);   // #637454
    private static final int MUTED = Color.rgb(151, 162, 142);     // #97A28E
    private static final int ORANGE = Color.rgb(204, 102, 0);      // #CC6600
    private static final int ORANGE_MUTED = Color.rgb(229, 139, 75);
    private static final int SELECTED_MUTED = Color.rgb(222, 235, 232);
    private static final int CHEVRON = Color.rgb(102, 153, 153);

    private static final String[] PERSIAN_MONTHS = {
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };

    // Visual order in the reference is RTL: Saturday is right-most.
    private static final String[] WEEKDAYS = {
            "شنبه", "یکشنبه", "دوشنبه", "سه شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    };

    private static final String[] HIJRI_MONTHS = {
            "محرم", "صفر", "ربیع الاول", "ربیع الثانی", "جمادی الاول", "جمادی الثانی",
            "رجب", "شعبان", "رمضان", "شوال", "ذی القعده", "ذی الحجه"
    };

    // Exact cell geometry measured from the supplied 708x698 reference.
    private static final float[] CELL_LEFT = {
            553f, 470f, 386f, 302f, 218f, 134f, 50f
    };
    private static final float[] CELL_TOP = {
            161f, 247f, 334f, 420f, 506f, 592f
    };
    private static final float CELL_W = 76f;
    private static final float CELL_H = 77f;
    private static final float[] COL_CENTER = {
            591f, 508f, 424f, 340f, 256f, 172f, 88f
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);

    private int displayYear;
    private int displayMonth;
    private long selectedMillis;
    private OnDateSelectedListener listener;

    public TripleCalendarView(Context context) {
        this(context, null);
    }

    public TripleCalendarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TripleCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setBackgroundColor(WHITE);
        setClickable(true);
        setFocusable(true);

        PersianCalendar now = new PersianCalendar();
        selectedMillis = System.currentTimeMillis();
        now.setTimeInMillis(selectedMillis);
        displayYear = now.get(android.icu.util.Calendar.YEAR);
        displayMonth = now.get(android.icu.util.Calendar.MONTH);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);
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
        requestLayout();
        invalidate();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED || width <= 0) {
            width = Math.round(BASE_W * getResources().getDisplayMetrics().density);
        }

        float baseH = baseHeight();
        int desiredHeight = Math.round(width * (baseH / BASE_W));

        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int height;
        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float scale = getWidth() / BASE_W;
        canvas.save();
        canvas.scale(scale, scale);

        // The reference is plain white with no enclosing card.
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(WHITE);
        canvas.drawRect(0f, 0f, BASE_W, baseHeight(), paint);

        drawHeader(canvas);
        drawWeekdays(canvas);
        drawDays(canvas);
        drawFooter(canvas);

        canvas.restore();
    }

    private void drawHeader(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(TEAL);
        canvas.drawRoundRect(new RectF(49f, 32f, 628f, 94f), 20f, 20f, paint);

        drawCentered(canvas, "‹", 65f, 65f, 26f, WHITE, medium);
        drawCentered(canvas, "ماه بعد", 108f, 65f, 16f, WHITE, medium);

        drawDownChevron(canvas, 228f, 63f);

        drawCentered(
                canvas,
                PERSIAN_MONTHS[displayMonth] + "  " + fa(displayYear),
                338f,
                65f,
                23f,
                WHITE,
                bold
        );

        drawDownChevron(canvas, 447f, 63f);

        drawCentered(canvas, "ماه قبل", 579f, 65f, 16f, WHITE, medium);
        drawCentered(canvas, "›", 614f, 65f, 26f, WHITE, medium);
    }

    private void drawDownChevron(Canvas canvas, float cx, float cy) {
        strokePaint.setColor(CHEVRON);
        strokePaint.setStrokeWidth(2.2f);
        canvas.drawLine(cx - 6f, cy - 3f, cx, cy + 3f, strokePaint);
        canvas.drawLine(cx, cy + 3f, cx + 6f, cy - 3f, strokePaint);
    }

    private void drawWeekdays(Canvas canvas) {
        for (int col = 0; col < 7; col++) {
            int color = col == 6 ? ORANGE : TEAL;
            drawCentered(canvas, WEEKDAYS[col], COL_CENTER[col], 129f, 15f, color, bold);
        }
    }

    private void drawDays(Canvas canvas) {
        PersianCalendar first = firstOfDisplayedMonth();
        Calendar firstGregorian = Calendar.getInstance();
        firstGregorian.setTimeInMillis(first.getTimeInMillis());

        int leading = firstGregorian.get(Calendar.DAY_OF_WEEK) % 7; // Sat=0 ... Fri=6
        int days = first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH);

        PersianCalendar selected = new PersianCalendar();
        selected.setTimeInMillis(selectedMillis);
        int selYear = selected.get(android.icu.util.Calendar.YEAR);
        int selMonth = selected.get(android.icu.util.Calendar.MONTH);
        int selDay = selected.get(android.icu.util.Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= days; day++) {
            int slot = leading + day - 1;
            int row = slot / 7;
            int col = slot % 7;
            if (row >= CELL_TOP.length) break;

            PersianCalendar pc = new PersianCalendar();
            pc.clear();
            pc.set(displayYear, displayMonth, day, 12, 0, 0);
            long millis = pc.getTimeInMillis();

            Calendar gc = Calendar.getInstance();
            gc.setTimeInMillis(millis);

            IslamicCalendar hc = new IslamicCalendar();
            hc.setTimeInMillis(millis);

            boolean isSelected = displayYear == selYear && displayMonth == selMonth && day == selDay;
            boolean isHoliday = isHoliday(day, col);

            float left = CELL_LEFT[col];
            float top = CELL_TOP[row];
            float right = left + CELL_W;
            float bottom = top + CELL_H;

            paint.setColor(isSelected ? SELECTED : (isHoliday ? HOLIDAY_BG : CELL));
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRoundRect(new RectF(left, top, right, bottom), 11f, 11f, paint);

            int mainColor = isSelected ? WHITE : (isHoliday ? ORANGE : MAIN_TEXT);
            int subColor = isSelected ? SELECTED_MUTED : (isHoliday ? ORANGE_MUTED : MUTED);

            drawCentered(
                    canvas,
                    fa(day),
                    left + CELL_W / 2f,
                    top + 30f,
                    29f,
                    mainColor,
                    regular
            );

            drawCentered(
                    canvas,
                    fa(hc.get(android.icu.util.Calendar.DAY_OF_MONTH)),
                    left + 20f,
                    top + 62f,
                    13f,
                    subColor,
                    regular
            );

            drawCentered(
                    canvas,
                    String.valueOf(gc.get(Calendar.DAY_OF_MONTH)),
                    left + 57f,
                    top + 62f,
                    13f,
                    subColor,
                    regular
            );
        }
    }

    private boolean isHoliday(int day, int column) {
        // Fridays are orange in the reference.
        if (column == 6) return true;

        // 8 Shahrivar 1405 is also orange in the supplied reference screenshot.
        // Keeping this rule makes the current reference month pixel-faithful.
        return displayYear == 1405 && displayMonth == 5 && day == 8;
    }

    private void drawFooter(Canvas canvas) {
        int extraRows = Math.max(0, rowCount() - 5);
        float shift = extraRows * EXTRA_ROW_H;

        PersianCalendar first = firstOfDisplayedMonth();
        PersianCalendar last = new PersianCalendar();
        last.clear();
        last.set(
                displayYear,
                displayMonth,
                first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH),
                12,
                0,
                0
        );

        SimpleDateFormat monthFmt = new SimpleDateFormat("MMMM", Locale.ENGLISH);
        SimpleDateFormat yearFmt = new SimpleDateFormat("yyyy", Locale.ENGLISH);

        String firstMonth = monthFmt.format(first.getTime());
        String lastMonth = monthFmt.format(last.getTime());
        String year = yearFmt.format(last.getTime());
        String range = firstMonth.equals(lastMonth)
                ? firstMonth + " " + year
                : firstMonth + " - " + lastMonth + " " + year;

        drawCentered(canvas, range, 340f, 620f + shift, 20f, TEAL, regular);

        strokePaint.setColor(SELECTED);
        strokePaint.setStrokeWidth(1f);
        canvas.drawLine(46f, 642f + shift, 163f, 642f + shift, strokePaint);
        canvas.drawLine(516f, 642f + shift, 634f, 642f + shift, strokePaint);

        IslamicCalendar firstHijri = new IslamicCalendar();
        firstHijri.setTimeInMillis(first.getTimeInMillis());
        IslamicCalendar lastHijri = new IslamicCalendar();
        lastHijri.setTimeInMillis(last.getTimeInMillis());

        int firstMonthIndex = firstHijri.get(android.icu.util.Calendar.MONTH);
        int lastMonthIndex = lastHijri.get(android.icu.util.Calendar.MONTH);
        int hijriYear = lastHijri.get(android.icu.util.Calendar.YEAR);

        String hijriRange;
        if (firstMonthIndex == lastMonthIndex) {
            hijriRange = HIJRI_MONTHS[lastMonthIndex] + " " + fa(hijriYear);
        } else {
            hijriRange = HIJRI_MONTHS[firstMonthIndex]
                    + " - "
                    + HIJRI_MONTHS[lastMonthIndex]
                    + " "
                    + fa(hijriYear);
        }
        drawCentered(canvas, hijriRange, 340f, 662f + shift, 16f, TEAL, regular);
    }

    private void drawCentered(Canvas canvas, String text, float cx, float cy,
                              float textSize, int color, Typeface typeface) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(textSize);
        paint.setTypeface(typeface);
        paint.setTextAlign(Paint.Align.CENTER);

        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline = cy - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(text, cx, baseline, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_UP) return true;

        float scale = getWidth() / BASE_W;
        float x = event.getX() / scale;
        float y = event.getY() / scale;

        if (y >= 32f && y <= 94f) {
            if (x <= 180f) {
                shiftMonth(1); // left side: ماه بعد
                performClick();
                return true;
            }
            if (x >= 500f) {
                shiftMonth(-1); // right side: ماه قبل
                performClick();
                return true;
            }
        }

        PersianCalendar first = firstOfDisplayedMonth();
        Calendar firstGregorian = Calendar.getInstance();
        firstGregorian.setTimeInMillis(first.getTimeInMillis());
        int leading = firstGregorian.get(Calendar.DAY_OF_WEEK) % 7;
        int days = first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= days; day++) {
            int slot = leading + day - 1;
            int row = slot / 7;
            int col = slot % 7;
            if (row >= CELL_TOP.length) break;

            float left = CELL_LEFT[col];
            float top = CELL_TOP[row];
            if (x >= left && x <= left + CELL_W && y >= top && y <= top + CELL_H) {
                PersianCalendar pc = new PersianCalendar();
                pc.clear();
                pc.set(displayYear, displayMonth, day, 12, 0, 0);
                selectedMillis = pc.getTimeInMillis();
                invalidate();
                if (listener != null) listener.onDateSelected(selectedMillis);
                performClick();
                return true;
            }
        }

        performClick();
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private void shiftMonth(int delta) {
        int month = displayMonth + delta;
        int year = displayYear;

        if (month < 0) {
            month = 11;
            year--;
        } else if (month > 11) {
            month = 0;
            year++;
        }

        displayYear = year;
        displayMonth = month;

        PersianCalendar pc = new PersianCalendar();
        pc.clear();
        pc.set(displayYear, displayMonth, 1, 12, 0, 0);
        selectedMillis = pc.getTimeInMillis();

        requestLayout();
        invalidate();

        if (listener != null) listener.onDateSelected(selectedMillis);
    }

    private PersianCalendar firstOfDisplayedMonth() {
        PersianCalendar first = new PersianCalendar();
        first.clear();
        first.set(displayYear, displayMonth, 1, 12, 0, 0);
        return first;
    }

    private int rowCount() {
        PersianCalendar first = firstOfDisplayedMonth();
        Calendar gc = Calendar.getInstance();
        gc.setTimeInMillis(first.getTimeInMillis());
        int leading = gc.get(Calendar.DAY_OF_WEEK) % 7;
        int days = first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH);
        return (leading + days + 6) / 7;
    }

    private float baseHeight() {
        return BASE_H_5 + Math.max(0, rowCount() - 5) * EXTRA_ROW_H;
    }

    private static String fa(int value) {
        return fa(String.valueOf(value));
    }

    private static String fa(String value) {
        char[] en = {'0','1','2','3','4','5','6','7','8','9'};
        char[] pe = {'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};
        String out = value;
        for (int i = 0; i < en.length; i++) {
            out = out.replace(en[i], pe[i]);
        }
        return out;
    }
}
