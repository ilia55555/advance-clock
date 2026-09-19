package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import java.util.Calendar;

public final class TripleCalendarView extends View {
    public interface OnDateSelectedListener { void onDateSelected(long timeInMillis); }
    public interface OnMonthYearClickListener { void onMonthYearClick(long visibleMonthMillis, int calendarType); }

    private static final float BASE_W = 708f;
    private static final float BASE_H_5 = 698f;
    private static final float EXTRA_ROW_H = 86f;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int CELL_LIGHT = 0xFFF8F8F8;
    private static final int CELL_DARK = 0xFF22272A;
    private static final int MAIN_TEXT_LIGHT = 0xFF637454;
    private static final int MAIN_TEXT_DARK = 0xFFDCE5E1;
    private static final int MUTED_LIGHT = 0xFF97A28E;
    private static final int MUTED_DARK = 0xFF98A7A2;

    private static final String[] WEEKDAYS = {
            "شنبه", "یکشنبه", "دوشنبه", "سه شنبه", "چهارشنبه", "پنجشنبه", "جمعه"
    };
    private static final float[] CELL_LEFT = {553f,470f,386f,302f,218f,134f,50f};
    private static final float[] CELL_TOP = {161f,247f,334f,420f,506f,592f};
    private static final float CELL_W = 76f;
    private static final float CELL_H = 77f;
    private static final float[] COL_CENTER = {591f,508f,424f,340f,256f,172f,88f};

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface regular = Typeface.create("sans-serif", Typeface.NORMAL);
    private final Typeface medium = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface bold = Typeface.create("sans-serif", Typeface.BOLD);

    private int calendarType;
    private int displayYear;
    private int displayMonth;
    private long selectedMillis;
    private float downX;
    private float downY;
    private boolean horizontalGesture;
    private OnDateSelectedListener dateListener;
    private OnMonthYearClickListener monthYearListener;

    public TripleCalendarView(Context context) { this(context, null); }
    public TripleCalendarView(Context context, AttributeSet attrs) { this(context, attrs, 0); }

    public TripleCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setClickable(true);
        setFocusable(true);
        calendarType = AppSettings.defaultCalendar(context);
        selectedMillis = System.currentTimeMillis();
        syncVisibleFrom(selectedMillis);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setOnDateSelectedListener(OnDateSelectedListener l) { dateListener = l; }
    public void setOnMonthYearClickListener(OnMonthYearClickListener l) { monthYearListener = l; }
    public int getCalendarType() { return calendarType; }

    public void setCalendarType(int type) {
        calendarType = Math.max(0, Math.min(2, type));
        syncVisibleFrom(selectedMillis);
        requestLayout();
        invalidate();
    }

    public long getSelectedMillis() { return selectedMillis; }

    public void setSelectedMillis(long millis) {
        selectedMillis = millis;
        syncVisibleFrom(millis);
        requestLayout();
        invalidate();
    }

    public void setVisibleMonthMillis(long millis, int type) {
        calendarType = Math.max(0, Math.min(2, type));
        android.icu.util.Calendar visible = CalendarUtils.fromMillis(calendarType, millis);
        displayYear = visible.get(android.icu.util.Calendar.YEAR);
        displayMonth = visible.get(android.icu.util.Calendar.MONTH);
        requestLayout();
        invalidate();
    }

    private void syncVisibleFrom(long millis) {
        android.icu.util.Calendar c = CalendarUtils.fromMillis(calendarType, millis);
        displayYear = c.get(android.icu.util.Calendar.YEAR);
        displayMonth = c.get(android.icu.util.Calendar.MONTH);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED || width <= 0) {
            width = Math.round(BASE_W * getResources().getDisplayMetrics().density);
        }
        int desired = Math.round(width * (baseHeight() / BASE_W));
        int mode = MeasureSpec.getMode(heightMeasureSpec);
        int size = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(width,
                mode == MeasureSpec.EXACTLY ? size
                        : (mode == MeasureSpec.AT_MOST ? Math.min(desired, size) : desired));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float s = getWidth() / BASE_W;
        canvas.save();
        canvas.scale(s, s);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(AppSettings.surface(getContext()));
        canvas.drawRect(0, 0, BASE_W, baseHeight(), paint);
        drawHeader(canvas);
        drawWeekdays(canvas);
        drawDays(canvas);
        drawFooter(canvas);
        canvas.restore();
    }

    private int primary() { return AppSettings.primaryColor(getContext()); }
    private int secondary() { return AppSettings.secondaryColor(getContext()); }
    private boolean dark() { return AppSettings.themeMode(getContext()) == AppSettings.THEME_DARK; }

    private int blendOnSurface(int color, float amount) {
        int base = dark() ? AppSettings.surface(getContext()) : 0xFFFFFFFF;
        int r = Math.round(android.graphics.Color.red(base) * (1f - amount)
                + android.graphics.Color.red(color) * amount);
        int g = Math.round(android.graphics.Color.green(base) * (1f - amount)
                + android.graphics.Color.green(color) * amount);
        int b = Math.round(android.graphics.Color.blue(base) * (1f - amount)
                + android.graphics.Color.blue(color) * amount);
        return android.graphics.Color.rgb(r, g, b);
    }

    private void drawHeader(Canvas c) {
        paint.setColor(primary());
        paint.setStyle(Paint.Style.FILL);
        c.drawRoundRect(new RectF(49, 32, 628, 94), 20, 20, paint);

        // Swapped to match the direction of finger swipes requested by the user.
        centered(c, "‹", 65, 65, 26, WHITE, medium);
        centered(c, "ماه قبل", 108, 65, 16, WHITE, medium);
        chevron(c, 228, 63);

        centered(c,
                CalendarUtils.monthName(calendarType, displayMonth) + "  "
                        + CalendarUtils.fa(displayYear),
                338, 65, 23, WHITE, bold);

        chevron(c, 447, 63);
        centered(c, "ماه بعد", 579, 65, 16, WHITE, medium);
        centered(c, "›", 614, 65, 26, WHITE, medium);
    }

    private void chevron(Canvas c, float cx, float cy) {
        stroke.setColor(0xFF9AD0CC);
        stroke.setStrokeWidth(2.2f);
        c.drawLine(cx - 6, cy - 3, cx, cy + 3, stroke);
        c.drawLine(cx, cy + 3, cx + 6, cy - 3, stroke);
    }

    private void drawWeekdays(Canvas c) {
        for (int col = 0; col < 7; col++) {
            centered(c, WEEKDAYS[col], COL_CENTER[col], 129, 15,
                    col == 6 ? secondary() : primary(), bold);
        }
    }

    private android.icu.util.Calendar first() {
        android.icu.util.Calendar c = CalendarUtils.create(calendarType);
        c.clear();
        c.set(displayYear, displayMonth, 1, 12, 0, 0);
        return c;
    }

    private void drawDays(Canvas c) {
        android.icu.util.Calendar first = first();
        int leading = first.get(android.icu.util.Calendar.DAY_OF_WEEK) % 7;
        int days = first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH);

        android.icu.util.Calendar sel = CalendarUtils.fromMillis(calendarType, selectedMillis);
        android.icu.util.Calendar today = CalendarUtils.fromMillis(
                calendarType, System.currentTimeMillis());

        int other1 = CalendarUtils.otherTypeOne(calendarType);
        int other2 = CalendarUtils.otherTypeTwo(calendarType);

        for (int day = 1; day <= days; day++) {
            int slot = leading + day - 1;
            int row = slot / 7;
            int col = slot % 7;
            if (row >= CELL_TOP.length) break;

            long millis = CalendarUtils.toMillis(
                    calendarType, displayYear, displayMonth, day, 12, 0);
            android.icu.util.Calendar a = CalendarUtils.fromMillis(other1, millis);
            android.icu.util.Calendar b = CalendarUtils.fromMillis(other2, millis);

            boolean selected =
                    sel.get(android.icu.util.Calendar.YEAR) == displayYear
                    && sel.get(android.icu.util.Calendar.MONTH) == displayMonth
                    && sel.get(android.icu.util.Calendar.DAY_OF_MONTH) == day;
            boolean isToday =
                    today.get(android.icu.util.Calendar.YEAR) == displayYear
                    && today.get(android.icu.util.Calendar.MONTH) == displayMonth
                    && today.get(android.icu.util.Calendar.DAY_OF_MONTH) == day;
            boolean holiday = col == 6;

            float left = CELL_LEFT[col], top = CELL_TOP[row];
            int fill = selected ? primary()
                    : (holiday
                    ? blendOnSurface(secondary(), dark() ? 0.20f : 0.10f)
                    : (dark() ? CELL_DARK : CELL_LIGHT));

            paint.setColor(fill);
            c.drawRoundRect(new RectF(left, top, left + CELL_W, top + CELL_H), 11, 11, paint);

            if (isToday) {
                stroke.setStyle(Paint.Style.STROKE);
                stroke.setStrokeWidth(2.8f);
                stroke.setColor(primary());
                c.drawRoundRect(
                        new RectF(left + 1.5f, top + 1.5f,
                                left + CELL_W - 1.5f, top + CELL_H - 1.5f),
                        10, 10, stroke);
                if (selected) {
                    stroke.setStrokeWidth(1.6f);
                    stroke.setColor(WHITE);
                    c.drawRoundRect(
                            new RectF(left + 5f, top + 5f,
                                    left + CELL_W - 5f, top + CELL_H - 5f),
                            8, 8, stroke);
                }
            }

            int main = selected ? WHITE
                    : (holiday ? secondary() : (dark() ? MAIN_TEXT_DARK : MAIN_TEXT_LIGHT));
            int muted = selected ? 0xFFDDECEA
                    : (holiday ? secondary() : (dark() ? MUTED_DARK : MUTED_LIGHT));

            centered(c, CalendarUtils.fa(day), left + CELL_W / 2f, top + 30,
                    29, main, regular);

            String smallLeft = other1 == CalendarUtils.GREGORIAN
                    ? String.valueOf(a.get(android.icu.util.Calendar.DAY_OF_MONTH))
                    : CalendarUtils.fa(a.get(android.icu.util.Calendar.DAY_OF_MONTH));
            String smallRight = other2 == CalendarUtils.GREGORIAN
                    ? String.valueOf(b.get(android.icu.util.Calendar.DAY_OF_MONTH))
                    : CalendarUtils.fa(b.get(android.icu.util.Calendar.DAY_OF_MONTH));

            centered(c, smallLeft, left + 20, top + 62, 16, muted, medium);
            centered(c, smallRight, left + 57, top + 62, 16, muted, medium);
        }
    }

    private void drawFooter(Canvas c) {
        float shift = Math.max(0, rowCount() - 5) * EXTRA_ROW_H;
        android.icu.util.Calendar first = first();
        android.icu.util.Calendar last = CalendarUtils.create(calendarType);
        last.clear();
        last.set(displayYear, displayMonth,
                first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH),
                12, 0, 0);

        int one = CalendarUtils.otherTypeOne(calendarType);
        int two = CalendarUtils.otherTypeTwo(calendarType);

        centered(c, rangeFor(one, first.getTimeInMillis(), last.getTimeInMillis()),
                340, 620 + shift, 20, primary(), regular);
        stroke.setColor(primary());
        stroke.setStrokeWidth(1);
        c.drawLine(46, 642 + shift, 163, 642 + shift, stroke);
        c.drawLine(516, 642 + shift, 634, 642 + shift, stroke);
        centered(c, rangeFor(two, first.getTimeInMillis(), last.getTimeInMillis()),
                340, 662 + shift, 16, primary(), regular);
    }

    private String rangeFor(int type, long start, long end) {
        android.icu.util.Calendar a = CalendarUtils.fromMillis(type, start);
        android.icu.util.Calendar b = CalendarUtils.fromMillis(type, end);
        String am = CalendarUtils.monthName(type, a.get(android.icu.util.Calendar.MONTH));
        String bm = CalendarUtils.monthName(type, b.get(android.icu.util.Calendar.MONTH));
        String y = type == CalendarUtils.GREGORIAN
                ? String.valueOf(b.get(android.icu.util.Calendar.YEAR))
                : CalendarUtils.fa(b.get(android.icu.util.Calendar.YEAR));
        return am.equals(bm) ? am + " " + y : am + " - " + bm + " " + y;
    }

    private void centered(Canvas c, String text, float cx, float cy,
                          float size, int color, Typeface tf) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTypeface(tf);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fm = paint.getFontMetrics();
        c.drawText(text, cx, cy - (fm.ascent + fm.descent) / 2f, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float s = getWidth() / BASE_W;
        float x = e.getX() / s;
        float y = e.getY() / s;

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = x;
                downY = y;
                horizontalGesture = false;
                requestParent(false);
                return true;

            case MotionEvent.ACTION_MOVE: {
                float dx = x - downX;
                float dy = y - downY;
                float absX = Math.abs(dx);
                float absY = Math.abs(dy);

                if (absX > 14f && absX > absY * 1.15f) {
                    horizontalGesture = true;
                    requestParent(false);
                } else if (!horizontalGesture && absY > 18f && absY > absX) {
                    requestParent(true);
                }
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                requestParent(true);
                horizontalGesture = false;
                return true;

            case MotionEvent.ACTION_UP:
                requestParent(true);
                break;

            default:
                return true;
        }

        float dx = x - downX;
        float dy = y - downY;

        if (Math.abs(dx) > 58f && Math.abs(dx) > Math.abs(dy) * 1.15f) {
            // Right-to-left = next month; left-to-right = previous month.
            // Header button actions stay unchanged.
            shiftMonth(dx > 0 ? -1 : 1);
            performClick();
            return true;
        }

        if (y >= 32 && y <= 94) {
            if (x <= 180) {
                shiftMonth(-1);
                performClick();
                return true;
            }
            if (x >= 500) {
                shiftMonth(1);
                performClick();
                return true;
            }
            if (x >= 190 && x <= 490) {
                if (monthYearListener != null) {
                    monthYearListener.onMonthYearClick(first().getTimeInMillis(), calendarType);
                }
                performClick();
                return true;
            }
        }

        android.icu.util.Calendar first = first();
        int leading = first.get(android.icu.util.Calendar.DAY_OF_WEEK) % 7;
        int days = first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH);

        for (int day = 1; day <= days; day++) {
            int slot = leading + day - 1;
            int row = slot / 7;
            int col = slot % 7;
            if (row >= CELL_TOP.length) break;

            float l = CELL_LEFT[col], t = CELL_TOP[row];
            if (x >= l && x <= l + CELL_W && y >= t && y <= t + CELL_H) {
                long candidate = CalendarUtils.toMillis(
                        calendarType, displayYear, displayMonth, day, 12, 0);

                if (candidate < startOfToday()) {
                    performClick();
                    return true;
                }

                selectedMillis = candidate;
                invalidate();
                if (dateListener != null) dateListener.onDateSelected(selectedMillis);
                performClick();
                return true;
            }
        }

        performClick();
        return true;
    }

    private void requestParent(boolean allowIntercept) {
        ViewParent parent = getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(!allowIntercept);
            parent = parent.getParent();
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
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
        displayMonth = m;
        displayYear = y;
        requestLayout();
        invalidate();
    }

    private int rowCount() {
        android.icu.util.Calendar first = first();
        int leading = first.get(android.icu.util.Calendar.DAY_OF_WEEK) % 7;
        return (leading
                + first.getActualMaximum(android.icu.util.Calendar.DAY_OF_MONTH) + 6) / 7;
    }

    private float baseHeight() {
        return BASE_H_5 + Math.max(0, rowCount() - 5) * EXTRA_ROW_H;
    }

    private static long startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
