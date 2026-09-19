package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class SketchView extends View {
    private static final int MAX_STROKES = 250;
    private static final int MAX_POINTS_PER_STROKE = 3500;

    private static final class Stroke {
        final ArrayList<float[]> points = new ArrayList<>();
        int color;
        float widthDp;

        Stroke(int color, float widthDp) {
            this.color = color;
            this.widthDp = widthDp;
        }
    }

    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ArrayList<Stroke> strokes = new ArrayList<>();
    private final ArrayList<Stroke> redo = new ArrayList<>();

    private Stroke current;
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private int penColor = 0xFF087C77;
    private float penWidthDp = 4f;
    private final float density;

    public SketchView(Context context) {
        this(context, null);
    }

    public SketchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setStrokeCap(Paint.Cap.ROUND);
        strokePaint.setStrokeJoin(Paint.Join.ROUND);

        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(Math.max(1f, density * 0.6f));
        gridPaint.setColor(0xFFEAF1F0);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFFFFFFF);
        bg.setCornerRadius(dp(14));
        bg.setStroke(dpInt(1), 0xFFDCE8E6);
        setBackground(bg);
        setClickable(true);
        setFocusable(true);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGrid(canvas);
        for (Stroke stroke : strokes) drawStroke(canvas, stroke);
        if (current != null) drawStroke(canvas, current);
    }

    private void drawGrid(Canvas canvas) {
        float step = dp(24);
        for (float x = step; x < getWidth(); x += step) {
            canvas.drawLine(x, 0, x, getHeight(), gridPaint);
        }
        for (float y = step; y < getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y, gridPaint);
        }
    }

    private void drawStroke(Canvas canvas, Stroke stroke) {
        if (stroke == null || stroke.points.isEmpty()) return;

        strokePaint.setColor(stroke.color);
        strokePaint.setStrokeWidth(Math.max(dp(1f), dp(stroke.widthDp)));

        if (stroke.points.size() == 1) {
            float[] p = stroke.points.get(0);
            canvas.drawCircle(
                    p[0] * getWidth(),
                    p[1] * getHeight(),
                    strokePaint.getStrokeWidth() / 2f,
                    strokePaint
            );
            return;
        }

        Path path = new Path();
        float[] first = stroke.points.get(0);
        float prevX = first[0] * getWidth();
        float prevY = first[1] * getHeight();
        path.moveTo(prevX, prevY);

        for (int i = 1; i < stroke.points.size(); i++) {
            float[] p = stroke.points.get(i);
            float x = p[0] * getWidth();
            float y = p[1] * getHeight();
            float midX = (prevX + x) * 0.5f;
            float midY = (prevY + y) * 0.5f;
            path.quadTo(prevX, prevY, midX, midY);
            prevX = x;
            prevY = y;
        }
        path.lineTo(prevX, prevY);
        canvas.drawPath(path, strokePaint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (getWidth() <= 0 || getHeight() <= 0) return false;

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                activePointerId = event.getPointerId(0);
                requestParentInterception(false);
                beginStroke(event.getX(0), event.getY(0));
                return true;
            }

            case MotionEvent.ACTION_POINTER_DOWN:
                // Ignore secondary fingers/palm. Never switch the drawing pointer mid-stroke.
                requestParentInterception(false);
                return true;

            case MotionEvent.ACTION_MOVE: {
                int index = event.findPointerIndex(activePointerId);
                if (index < 0 || current == null) return true;

                int history = event.getHistorySize();
                for (int h = 0; h < history; h++) {
                    addPoint(event.getHistoricalX(index, h), event.getHistoricalY(index, h));
                }
                addPoint(event.getX(index), event.getY(index));
                invalidate();
                return true;
            }

            case MotionEvent.ACTION_POINTER_UP: {
                int index = event.getActionIndex();
                if (event.getPointerId(index) == activePointerId) {
                    addPoint(event.getX(index), event.getY(index));
                    commitStroke();
                    activePointerId = MotionEvent.INVALID_POINTER_ID;
                    requestParentInterception(true);
                }
                return true;
            }

            case MotionEvent.ACTION_UP: {
                int index = event.findPointerIndex(activePointerId);
                if (index >= 0) addPoint(event.getX(index), event.getY(index));
                commitStroke();
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                requestParentInterception(true);
                performClick();
                return true;
            }

            case MotionEvent.ACTION_CANCEL:
                current = null;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                requestParentInterception(true);
                invalidate();
                return true;

            default:
                return true;
        }
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private void beginStroke(float x, float y) {
        if (strokes.size() >= MAX_STROKES) strokes.remove(0);
        redo.clear();
        current = new Stroke(penColor, penWidthDp);
        addPointRaw(x, y);
        invalidate();
    }

    private void addPoint(float x, float y) {
        if (current == null || current.points.size() >= MAX_POINTS_PER_STROKE) return;
        if (x < 0f || y < 0f || x > getWidth() || y > getHeight()) return;

        if (!current.points.isEmpty()) {
            float[] last = current.points.get(current.points.size() - 1);
            float lastX = last[0] * getWidth();
            float lastY = last[1] * getHeight();
            float dx = x - lastX;
            float dy = y - lastY;
            float distance = (float) Math.hypot(dx, dy);

            if (distance < dp(0.7f)) return;

            float maxJump = Math.max(dp(88f), Math.max(getWidth(), getHeight()) * 0.28f);
            if (distance > maxJump) return;
        }

        addPointRaw(x, y);
    }

    private void addPointRaw(float x, float y) {
        if (current == null || current.points.size() >= MAX_POINTS_PER_STROKE) return;
        current.points.add(new float[]{
                clamp(x / Math.max(1f, getWidth())),
                clamp(y / Math.max(1f, getHeight()))
        });
    }

    private void commitStroke() {
        if (current != null && !current.points.isEmpty()) {
            strokes.add(current);
        }
        current = null;
        invalidate();
    }

    private void requestParentInterception(boolean allowIntercept) {
        ViewParent parent = getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(!allowIntercept);
            parent = parent.getParent();
        }
    }

    public void setPenColor(int color) {
        penColor = color;
    }

    public void setPenWidthDp(float widthDp) {
        penWidthDp = Math.max(1f, Math.min(18f, widthDp));
    }

    public void undo() {
        if (current != null) {
            current = null;
            invalidate();
            return;
        }
        if (strokes.isEmpty()) return;
        redo.add(strokes.remove(strokes.size() - 1));
        invalidate();
    }

    public void redo() {
        if (redo.isEmpty()) return;
        strokes.add(redo.remove(redo.size() - 1));
        invalidate();
    }

    public void clearSketch() {
        strokes.clear();
        redo.clear();
        current = null;
        invalidate();
    }

    public String serialize() {
        JSONArray all = new JSONArray();
        try {
            for (Stroke stroke : strokes) {
                JSONObject out = new JSONObject();
                out.put("c", stroke.color);
                out.put("w", stroke.widthDp);

                JSONArray points = new JSONArray();
                for (float[] p : stroke.points) {
                    JSONArray point = new JSONArray();
                    point.put(p[0]);
                    point.put(p[1]);
                    points.put(point);
                }
                out.put("p", points);
                all.put(out);
            }
        } catch (Exception ignored) {
        }
        return all.toString();
    }

    public void load(String raw) {
        strokes.clear();
        redo.clear();
        current = null;

        try {
            JSONArray all = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < Math.min(all.length(), MAX_STROKES); i++) {
                Object entry = all.opt(i);

                // Backward-compatible loader for the old [[x,y], ...] stroke format.
                if (entry instanceof JSONArray) {
                    Stroke stroke = new Stroke(0xFF087C77, 4f);
                    readPoints((JSONArray) entry, stroke);
                    if (!stroke.points.isEmpty()) strokes.add(stroke);
                    continue;
                }

                if (entry instanceof JSONObject) {
                    JSONObject object = (JSONObject) entry;
                    Stroke stroke = new Stroke(
                            object.optInt("c", 0xFF087C77),
                            (float) object.optDouble("w", 4f)
                    );
                    JSONArray points = object.optJSONArray("p");
                    if (points != null) readPoints(points, stroke);
                    if (!stroke.points.isEmpty()) strokes.add(stroke);
                }
            }
        } catch (Exception ignored) {
        }

        invalidate();
    }

    private void readPoints(JSONArray points, Stroke stroke) {
        for (int j = 0; j < Math.min(points.length(), MAX_POINTS_PER_STROKE); j++) {
            JSONArray point = points.optJSONArray(j);
            if (point != null && point.length() >= 2) {
                stroke.points.add(new float[]{
                        clamp((float) point.optDouble(0, 0)),
                        clamp((float) point.optDouble(1, 0))
                });
            }
        }
    }

    private float dp(float value) {
        return value * density;
    }

    private int dpInt(int value) {
        return Math.round(value * density);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
