package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class ColorMixerView extends View {
    public interface Listener { void onColorChanged(int color); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint marker = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float[] hsv = {180f, 0.8f, 0.8f};
    private Listener listener;

    private RectF field = new RectF();
    private RectF hueBar = new RectF();
    private boolean draggingHue;

    public ColorMixerView(Context context) { this(context, null); }
    public ColorMixerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        marker.setStyle(Paint.Style.STROKE);
        marker.setStrokeWidth(dp(2));
    }

    public void setListener(Listener listener) { this.listener = listener; }

    public void setColor(int color) {
        Color.colorToHSV(color, hsv);
        invalidate();
    }

    public int getColor() {
        return Color.HSVToColor(hsv);
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (w <= 0) w = dp(320);
        int desired = dp(238);
        setMeasuredDimension(w, resolveSize(desired, heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float pad = dp(2);
        float hueH = dp(28);
        float gap = dp(14);
        field.set(pad, pad, getWidth() - pad, getHeight() - hueH - gap - pad);
        hueBar.set(pad, field.bottom + gap, getWidth() - pad, getHeight() - pad);

        int hueColor = Color.HSVToColor(new float[]{hsv[0], 1f, 1f});
        paint.setShader(new LinearGradient(field.left, 0, field.right, 0,
                Color.WHITE, hueColor, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(field, dp(8), dp(8), paint);

        paint.setShader(new LinearGradient(0, field.top, 0, field.bottom,
                0x00FFFFFF, 0xFF000000, Shader.TileMode.CLAMP));
        canvas.drawRoundRect(field, dp(8), dp(8), paint);
        paint.setShader(null);

        int[] hueColors = {
                0xFFFF0000,0xFFFFFF00,0xFF00FF00,0xFF00FFFF,
                0xFF0000FF,0xFFFF00FF,0xFFFF0000
        };
        float[] positions = {0f,1f/6f,2f/6f,3f/6f,4f/6f,5f/6f,1f};
        paint.setShader(new LinearGradient(
                hueBar.left,0,hueBar.right,0,hueColors,positions,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(hueBar, dp(10), dp(10), paint);
        paint.setShader(null);

        float x = field.left + hsv[1] * field.width();
        float y = field.top + (1f - hsv[2]) * field.height();
        marker.setColor(Color.WHITE);
        marker.setStrokeWidth(dp(3));
        canvas.drawCircle(x, y, dp(8), marker);
        marker.setColor(0xAA000000);
        marker.setStrokeWidth(dp(1));
        canvas.drawCircle(x, y, dp(10), marker);

        float hueX = hueBar.left + (hsv[0] / 360f) * hueBar.width();
        marker.setColor(Color.WHITE);
        marker.setStrokeWidth(dp(3));
        canvas.drawCircle(hueX, hueBar.centerY(), dp(9), marker);
        marker.setColor(0xAA000000);
        marker.setStrokeWidth(dp(1));
        canvas.drawCircle(hueX, hueBar.centerY(), dp(11), marker);
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                draggingHue = hueBar.contains(e.getX(), e.getY());
                getParent().requestDisallowInterceptTouchEvent(true);
                updateFromTouch(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                updateFromTouch(e.getX(), e.getY());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                updateFromTouch(e.getX(), e.getY());
                getParent().requestDisallowInterceptTouchEvent(false);
                performClick();
                return true;
            default:
                return true;
        }
    }

    private void updateFromTouch(float x, float y) {
        if (draggingHue) {
            float clamped = Math.max(hueBar.left, Math.min(hueBar.right, x));
            hsv[0] = 360f * (clamped - hueBar.left) / Math.max(1f, hueBar.width());
        } else {
            float cx = Math.max(field.left, Math.min(field.right, x));
            float cy = Math.max(field.top, Math.min(field.bottom, y));
            hsv[1] = (cx - field.left) / Math.max(1f, field.width());
            hsv[2] = 1f - (cy - field.top) / Math.max(1f, field.height());
        }
        invalidate();
        if (listener != null) listener.onColorChanged(getColor());
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
