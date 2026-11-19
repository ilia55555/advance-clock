package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * Lightweight wheel showing five rows: the selected value at full strength,
 * neighbours at 60% and the outer pair at 30%. Drag or fling to spin (with a
 * constant-volume tick), tap the centre to type with the numeric keyboard.
 */
final class WheelPickerView extends FrameLayout {
    interface Listener { void onValueChanged(WheelPickerView wheel, int value); }

    private static final int VISIBLE = 5;
    private static final float DECAY = 5.6f;      // per second, like 0.91 per frame
    private static final float MIN_FLING = 1.5f;  // items per second

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint divider = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float density;
    private final int touchSlop;
    private final EditText input;

    private String[] labels;
    private int min;
    private boolean wrap = true;
    private boolean typeable;
    private Listener listener;

    private float itemHeight;
    private float pos;          // continuous index (0..count-1)
    private float velocity;     // items per second
    private boolean dragging;
    private boolean animating;
    private float target = Float.NaN;
    private long lastFrame;
    private int lastTick;
    private float downY;
    private float lastY;
    private boolean moved;
    private VelocityTracker tracker;

    WheelPickerView(Context context) {
        super(context);
        setWillNotDraw(false);
        density = context.getResources().getDisplayMetrics().density;
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        itemHeight = 46 * density;
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(AppSettings.textPrimary(context));
        setTextSize(30);
        divider.setColor(AppSettings.primaryColor(context));
        divider.setStrokeWidth(1.5f * density);
        WheelTickSound.prepare(context);

        input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        input.setGravity(Gravity.CENTER);
        input.setTextColor(AppSettings.primaryColor(context));
        input.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        input.setBackgroundResource(R.drawable.bg_field);
        input.setPadding(0, 0, 0, 0);
        input.setSelectAllOnFocus(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setVisibility(GONE);
        input.setOnEditorActionListener((v, action, e) -> {
            if (action == EditorInfo.IME_ACTION_DONE) { commitTyping(); return true; }
            return false;
        });
        input.setOnFocusChangeListener((v, focused) -> { if (!focused) commitTyping(); });
        addView(input, new LayoutParams(-1, Math.round(itemHeight), Gravity.CENTER));
    }

    void setTextSize(float sp) {
        paint.setTextSize(sp * getResources().getDisplayMetrics().scaledDensity);
        if (input != null) input.setTextSize(sp);
    }

    void setRange(int min, int max, boolean twoDigits) {
        this.min = min;
        labels = new String[max - min + 1];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = twoDigits ? String.format(java.util.Locale.US, "%02d", min + i)
                    : String.valueOf(min + i);
        }
        invalidate();
    }

    void setLabels(String[] values) {
        min = 0;
        labels = values;
        invalidate();
    }

    void setWrap(boolean wrap) { this.wrap = wrap; }
    void setTypeable(boolean typeable) { this.typeable = typeable; }
    void setListener(Listener listener) { this.listener = listener; }

    int getValue() {
        return min + index(Math.round(animating && !Float.isNaN(target) ? target : pos));
    }

    void setValue(int value) {
        stop();
        pos = clampIndex(value - min);
        lastTick = Math.round(pos);
        invalidate();
    }

    private int count() { return labels == null ? 1 : labels.length; }

    private int index(int i) {
        int n = count();
        return wrap ? ((i % n) + n) % n : Math.max(0, Math.min(n - 1, i));
    }

    private float clampIndex(float i) {
        return wrap ? i : Math.max(0, Math.min(count() - 1, i));
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int h = Math.round(itemHeight * VISIBLE);
        super.onMeasure(widthSpec, MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
    }

    @Override protected void onDraw(Canvas canvas) {
        if (labels == null) return;
        if (animating) step();
        float cy = getHeight() / 2f;
        float cx = getWidth() / 2f;
        float baseSize = paint.getTextSize();
        int first = (int) Math.floor(pos) - 3;
        int last = (int) Math.ceil(pos) + 3;
        for (int i = first; i <= last; i++) {
            if (!wrap && (i < 0 || i >= count())) continue;
            float d = i - pos;
            float ad = Math.abs(d);
            if (ad > 2.6f) continue;
            if (ad < 0.5f && input.getVisibility() == VISIBLE) continue;
            float alpha;
            if (ad <= 1f) alpha = 1f - 0.4f * ad;
            else if (ad <= 2f) alpha = 0.6f - 0.3f * (ad - 1f);
            else alpha = 0.3f * (1f - (ad - 2f) / 0.6f);
            float scale = 1f - 0.12f * Math.min(ad, 2f);
            paint.setTextSize(baseSize * scale);
            paint.setAlpha(Math.round(255 * Math.max(0f, alpha)));
            float y = cy + d * itemHeight - (paint.descent() + paint.ascent()) / 2f;
            canvas.drawText(labels[index(i)], cx, y, paint);
        }
        paint.setTextSize(baseSize);
        paint.setAlpha(255);
        float inset = getWidth() * 0.12f;
        float half = itemHeight / 2f;
        canvas.drawLine(inset, cy - half, getWidth() - inset, cy - half, divider);
        canvas.drawLine(inset, cy + half, getWidth() - inset, cy + half, divider);
        if (animating) postInvalidateOnAnimation();
    }

    private void step() {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrame) / 1e9f);
        lastFrame = now;
        if (Float.isNaN(target) && Math.abs(velocity) > MIN_FLING) {
            pos += velocity * dt;
            velocity *= (float) Math.exp(-DECAY * dt);
            if (!wrap && (pos < 0 || pos > count() - 1)) {
                pos = clampIndex(pos);
                velocity = 0;
            }
            tick();
            return;
        }
        if (Float.isNaN(target)) target = Math.round(clampIndex(pos));
        velocity = 0;
        float k = 1f - (float) Math.pow(0.72f, dt * 60f);
        pos += (target - pos) * k;
        tick();
        if (Math.abs(target - pos) < 0.002f) {
            pos = target;
            normalise();
            animating = false;
            target = Float.NaN;
            notifyValue();
        }
    }

    private void normalise() {
        if (!wrap) return;
        int n = count();
        int whole = (int) Math.floor(pos / n) * n;
        pos -= whole;
        lastTick -= whole;
    }

    private void tick() {
        int now = Math.round(pos);
        if (now != lastTick) {
            lastTick = now;
            WheelTickSound.play();
            performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
            notifyValue();
        }
    }

    private int lastNotified = Integer.MIN_VALUE;

    private void notifyValue() {
        int value = min + index(Math.round(pos));
        if (value != lastNotified) {
            lastNotified = value;
            if (listener != null) listener.onValueChanged(this, value);
        }
    }

    private void startAnimation() {
        if (!animating) {
            animating = true;
            lastFrame = System.nanoTime();
        }
        postInvalidateOnAnimation();
    }

    private void stop() {
        animating = false;
        velocity = 0;
        target = Float.NaN;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (labels == null) return false;
        if (tracker == null) tracker = VelocityTracker.obtain();
        tracker.addMovement(e);
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (input.getVisibility() == VISIBLE) commitTyping();
                getParent().requestDisallowInterceptTouchEvent(true);
                stop();
                dragging = true;
                moved = false;
                downY = lastY = e.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (!moved && Math.abs(e.getY() - downY) > touchSlop) moved = true;
                if (moved) {
                    pos = clampIndex(pos - (e.getY() - lastY) / itemHeight);
                    tick();
                    invalidate();
                }
                lastY = e.getY();
                return true;
            case MotionEvent.ACTION_UP:
                dragging = false;
                if (!moved) {
                    float offset = (e.getY() - getHeight() / 2f) / itemHeight;
                    int rows = Math.round(offset);
                    if (rows == 0) {
                        if (typeable) startTyping();
                    } else {
                        target = Math.round(clampIndex(Math.round(pos) + rows));
                        startAnimation();
                    }
                } else {
                    tracker.computeCurrentVelocity(1000);
                    velocity = -tracker.getYVelocity() / itemHeight;
                    target = Float.NaN;
                    startAnimation();
                }
                recycleTracker();
                performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                recycleTracker();
                target = Float.NaN;
                startAnimation();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    @Override public boolean performClick() { return super.performClick(); }

    private void recycleTracker() {
        if (tracker != null) { tracker.recycle(); tracker = null; }
    }

    private void startTyping() {
        input.setText(labels[index(Math.round(pos))]);
        input.setVisibility(VISIBLE);
        input.requestFocus();
        input.selectAll();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
        invalidate();
    }

    /** Applies a typed value (if valid) and closes the editor. */
    void commitTyping() {
        if (input.getVisibility() != VISIBLE) return;
        String text = input.getText().toString().trim();
        StringBuilder latin = new StringBuilder();
        for (char ch : text.toCharArray()) {
            if (ch >= '۰' && ch <= '۹') ch = (char) ('0' + ch - '۰');
            else if (ch >= '٠' && ch <= '٩') ch = (char) ('0' + ch - '٠');
            latin.append(ch);
        }
        input.setVisibility(GONE);
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
        try {
            int value = Integer.parseInt(latin.toString());
            if (value >= min && value < min + count()) {
                setValue(value);
                notifyValue();
            }
        } catch (NumberFormatException ignored) {
            // Keep the previous value.
        }
        invalidate();
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        recycleTracker();
        super.onDetachedFromWindow();
    }
}
