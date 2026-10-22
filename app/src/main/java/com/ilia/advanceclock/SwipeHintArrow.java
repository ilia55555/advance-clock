package com.ilia.advanceclock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Decorative and touch-transparent; the actual swipe is handled by SwipeAlarmSurface. */
final class SwipeHintArrow extends View {
    private final View hint;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path chevron = new Path();
    private final int[] hintPosition = new int[2], ownPosition = new int[2];
    private ValueAnimator animator;
    private float progress;

    SwipeHintArrow(Context context, View hint, int color) {
        super(context);
        this.hint = hint;
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(3));
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setClickable(false);
        setFocusable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (hint.getWidth() == 0 || getHeight() == 0) return;
        hint.getLocationInWindow(hintPosition);
        getLocationInWindow(ownPosition);
        float x = hintPosition[0] - ownPosition[0] + hint.getWidth() / 2f;
        float y = hintPosition[1] - ownPosition[1] - dp(14) - getHeight() * .225f * progress;
        float opacity = Math.min(1f, progress / .12f) * Math.min(1f, (1f - progress) / .35f);
        paint.setAlpha(Math.round(255 * Math.max(0, opacity)));
        chevron.reset();
        chevron.moveTo(x - dp(9), y + dp(6));
        chevron.lineTo(x, y - dp(3));
        chevron.lineTo(x + dp(9), y + dp(6));
        canvas.drawPath(chevron, paint);
    }
    private void updateAnimation() {
        boolean visible = isAttachedToWindow() && getWindowVisibility() == VISIBLE && isShown();
        if (!visible) { stopAnimation(); return; }
        if (animator != null) return;
        if (!ValueAnimator.areAnimatorsEnabled()) { progress = .12f; invalidate(); return; }
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(1000);
        animator.setInterpolator(new LinearInterpolator());
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(value -> { progress = (float) value.getAnimatedValue(); invalidate(); });
        animator.start();
    }
    private void stopAnimation() {
        if (animator != null) { animator.cancel(); animator = null; }
        progress = 0;
        invalidate();
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); updateAnimation(); }
    @Override protected void onWindowVisibilityChanged(int visibility) { super.onWindowVisibilityChanged(visibility); updateAnimation(); }
    @Override public void onVisibilityAggregated(boolean visible) { super.onVisibilityAggregated(visible); updateAnimation(); }
    @Override protected void onDetachedFromWindow() { stopAnimation(); super.onDetachedFromWindow(); }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
}
