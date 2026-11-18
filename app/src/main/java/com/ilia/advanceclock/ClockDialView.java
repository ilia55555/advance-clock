package com.ilia.advanceclock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;

import java.util.Locale;

/**
 * Lightweight custom clock dial. Hours use two rings: 1–12 inside and
 * 13–00 outside; the ring under the selector grows while the other shrinks.
 * Choosing an hour shrinks both rings towards the centre while the minute
 * ring slides in from outside. Everything is drawn with preallocated paints.
 */
final class ClockDialView extends View {
    interface Listener {
        void onTimeChanged(int hour, int minute);
        void onModeChanged(boolean minutes);
    }

    private static final PathInterpolator EASE = new PathInterpolator(0.2f, 0f, 0f, 1f);
    private static final String[] INNER = new String[12];
    private static final String[] OUTER = new String[12];
    private static final String[] MINUTES = new String[12];

    static {
        for (int i = 0; i < 12; i++) {
            INNER[i] = String.valueOf(i == 0 ? 12 : i);
            OUTER[i] = i == 0 ? "00" : String.valueOf(i + 12);
            MINUTES[i] = String.format(Locale.US, "%02d", i * 5);
        }
    }

    private final Paint facePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int textColor;
    private final int primary;
    private final float density;
    private final float scaled;

    private int hour;
    private int minute;
    private boolean minuteMode;
    private Listener listener;

    /** 1 when the outer (13–00) ring is active. */
    private float outerWeight;
    private float weightTarget;
    /** 0 = hour rings shown, 1 = minute ring shown. */
    private float minuteProgress;
    private float hourAngle;
    private float minuteAngle;

    private ValueAnimator weightAnim;
    private ValueAnimator progressAnim;
    private ValueAnimator hourAngleAnim;
    private ValueAnimator minuteAngleAnim;
    private boolean dragging;

    ClockDialView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        scaled = context.getResources().getDisplayMetrics().scaledDensity;
        textColor = AppSettings.textPrimary(context);
        primary = AppSettings.primaryColor(context);
        facePaint.setColor(AppSettings.field(context));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        selectorPaint.setColor(primary);
        linePaint.setColor(primary);
        linePaint.setStrokeWidth(2 * density);
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
    }

    void setListener(Listener listener) { this.listener = listener; }

    void setTime(int hour, int minute) {
        this.hour = hour;
        this.minute = minute;
        cancel(weightAnim);
        cancel(hourAngleAnim);
        cancel(minuteAngleAnim);
        outerWeight = isOuter(hour) ? 1f : 0f;
        weightTarget = outerWeight;
        hourAngle = (hour % 12) * 30f;
        minuteAngle = minute * 6f;
        invalidate();
    }

    int hour() { return hour; }
    int minute() { return minute; }
    boolean isMinuteMode() { return minuteMode; }

    void setMinuteMode(boolean minutes, boolean animate) {
        if (minuteMode == minutes && animate) return;
        minuteMode = minutes;
        cancel(progressAnim);
        float target = minutes ? 1f : 0f;
        if (!animate) {
            minuteProgress = target;
            invalidate();
        } else {
            progressAnim = animate(minuteProgress, target, 260, v -> minuteProgress = v);
        }
        if (listener != null) listener.onModeChanged(minutes);
    }

    /** Changes the hour from outside (e.g. AM/PM), animating ring and selector. */
    void changeHour(int newHour) {
        selectHour(newHour, true);
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        int size = Math.min(w > 0 ? w : Math.round(280 * density), Math.round(280 * density));
        setMeasuredDimension(size, size);
    }

    @Override protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float r = Math.min(cx, cy);
        canvas.drawCircle(cx, cy, r, facePaint);

        float p = minuteProgress;
        float hourScale = 1f - 0.65f * p;
        int hourAlpha = Math.round(255 * (1f - p));
        float outerR = r * lerp(0.70f, 0.82f, outerWeight) * hourScale;
        float innerR = r * lerp(0.54f, 0.47f, outerWeight) * hourScale;
        float outerText = sp(lerp(12f, 17f, outerWeight)) * hourScale;
        float innerText = sp(lerp(17f, 12f, outerWeight)) * hourScale;
        float outerFade = lerp(0.5f, 1f, outerWeight);
        float innerFade = lerp(1f, 0.5f, outerWeight);

        float minuteR = r * lerp(1.25f, 0.80f, p);
        int minuteAlpha = Math.round(255 * p);
        float minuteText = sp(17f);

        // Selector position: blend between hour and minute targets.
        float hourSelR = isOuter(hour) ? outerR : innerR;
        float hx = cx + (float) Math.sin(Math.toRadians(hourAngle)) * hourSelR;
        float hy = cy - (float) Math.cos(Math.toRadians(hourAngle)) * hourSelR;
        float mx = cx + (float) Math.sin(Math.toRadians(minuteAngle)) * minuteR;
        float my = cy - (float) Math.cos(Math.toRadians(minuteAngle)) * minuteR;
        float sx = lerp(hx, mx, p);
        float sy = lerp(hy, my, p);
        float selRadius = 20 * density * lerp(lerp(0.9f, 1f, isOuter(hour) ? outerWeight : 1f - outerWeight), 1f, p);

        if (hourAlpha > 0) {
            drawRing(canvas, cx, cy, OUTER, outerR, outerText, Math.round(hourAlpha * outerFade));
            drawRing(canvas, cx, cy, INNER, innerR, innerText, Math.round(hourAlpha * innerFade));
        }
        if (minuteAlpha > 0) drawRing(canvas, cx, cy, MINUTES, minuteR, minuteText, minuteAlpha);

        canvas.drawLine(cx, cy, sx, sy, linePaint);
        canvas.drawCircle(cx, cy, 4 * density, selectorPaint);
        canvas.drawCircle(sx, sy, selRadius, selectorPaint);

        // Redraw the number under the selector in white, clipped to it.
        canvas.save();
        android.graphics.Path clip = selectorClip;
        clip.rewind();
        clip.addCircle(sx, sy, selRadius, android.graphics.Path.Direction.CW);
        canvas.clipPath(clip);
        if (hourAlpha > 0) {
            drawRingColored(canvas, cx, cy, OUTER, outerR, outerText, hourAlpha, Color.WHITE);
            drawRingColored(canvas, cx, cy, INNER, innerR, innerText, hourAlpha, Color.WHITE);
        }
        if (minuteAlpha > 0) drawRingColored(canvas, cx, cy, MINUTES, minuteR, minuteText, minuteAlpha, Color.WHITE);
        canvas.restore();
        if (p > 0.5f && minute % 5 != 0) {
            textPaint.setColor(Color.WHITE);
            canvas.drawCircle(sx, sy, 3 * density, textPaint);
        }
    }

    private final android.graphics.Path selectorClip = new android.graphics.Path();

    private void drawRing(Canvas canvas, float cx, float cy, String[] labels, float radius, float size, int alpha) {
        drawRingColored(canvas, cx, cy, labels, radius, size, alpha, textColor);
    }

    private void drawRingColored(Canvas canvas, float cx, float cy, String[] labels,
                                 float radius, float size, int alpha, int color) {
        textPaint.setColor(color);
        textPaint.setAlpha(Math.max(0, Math.min(255, alpha)));
        textPaint.setTextSize(Math.max(1f, size));
        float offset = (textPaint.descent() + textPaint.ascent()) / 2f;
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30);
            float x = cx + (float) Math.sin(a) * radius;
            float y = cy - (float) Math.cos(a) * radius - offset;
            canvas.drawText(labels[i], x, y, textPaint);
        }
        textPaint.setAlpha(255);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float dx = event.getX() - cx;
        float dy = event.getY() - cy;
        float angle = (float) Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0) angle += 360f;
        float dist = (float) Math.hypot(dx, dy);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragging = true;
                getParent().requestDisallowInterceptTouchEvent(true);
                handle(angle, dist, false);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) handle(angle, dist, true);
                return true;
            case MotionEvent.ACTION_UP:
                if (dragging) {
                    handle(angle, dist, true);
                    dragging = false;
                    if (!minuteMode) setMinuteMode(true, true);
                    performClick();
                }
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }

    @Override public boolean performClick() {
        return super.performClick();
    }

    private void handle(float angle, float dist, boolean moving) {
        float r = Math.min(getWidth(), getHeight()) / 2f;
        if (minuteMode) {
            int m = Math.round(angle / 6f) % 60;
            if (m != minute) {
                minute = m;
                performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                notifyTime();
            }
            // Follow the finger directly while dragging for zero lag.
            setAngle(false, m * 6f, !moving);
        } else {
            float split = r * (lerp(0.70f, 0.82f, outerWeight) + lerp(0.54f, 0.47f, outerWeight)) / 2f;
            boolean outer = dist > split;
            int index = Math.round(angle / 30f) % 12;
            int h = outer ? (index == 0 ? 0 : index + 12) : (index == 0 ? 12 : index);
            selectHour(h, !moving);
        }
    }

    private void selectHour(int h, boolean animateAngle) {
        boolean changed = h != hour;
        hour = h;
        float targetWeight = isOuter(h) ? 1f : 0f;
        if (targetWeight != weightTarget) {
            weightTarget = targetWeight;
            cancel(weightAnim);
            weightAnim = animate(outerWeight, targetWeight, 200, v -> outerWeight = v);
        }
        setAngle(true, (h % 12) * 30f, animateAngle || changed);
        if (changed) {
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            notifyTime();
        }
    }

    private void setAngle(boolean forHour, float target, boolean animate) {
        float current = forHour ? hourAngle : minuteAngle;
        float diff = ((target - current) % 360f + 540f) % 360f - 180f;
        float end = current + diff;
        if (forHour) cancel(hourAngleAnim); else cancel(minuteAngleAnim);
        if (!animate || Math.abs(diff) < 0.01f) {
            if (forHour) hourAngle = norm(end); else minuteAngle = norm(end);
            invalidate();
            return;
        }
        ValueAnimator anim = animate(current, end, 160, v -> {
            if (forHour) hourAngle = v; else minuteAngle = v;
        });
        if (forHour) hourAngleAnim = anim; else minuteAngleAnim = anim;
    }

    private interface Setter { void set(float value); }

    private ValueAnimator animate(float from, float to, long duration, Setter setter) {
        ValueAnimator anim = ValueAnimator.ofFloat(from, to);
        anim.setDuration(duration);
        anim.setInterpolator(EASE);
        anim.addUpdateListener(a -> {
            setter.set((Float) a.getAnimatedValue());
            invalidate();
        });
        anim.start();
        return anim;
    }

    private void notifyTime() {
        if (listener != null) listener.onTimeChanged(hour, minute);
    }

    @Override protected void onDetachedFromWindow() {
        cancel(weightAnim);
        cancel(progressAnim);
        cancel(hourAngleAnim);
        cancel(minuteAngleAnim);
        super.onDetachedFromWindow();
    }

    private static void cancel(ValueAnimator anim) {
        if (anim != null) anim.cancel();
    }

    private static boolean isOuter(int hour) {
        return hour == 0 || hour > 12;
    }

    private static float norm(float angle) {
        return ((angle % 360f) + 360f) % 360f;
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private float sp(float value) {
        return value * scaled;
    }
}
