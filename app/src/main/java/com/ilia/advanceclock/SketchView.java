package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public final class SketchView extends View {
    private static final int MAX_STROKES = 200;
    private static final int MAX_POINTS_PER_STROKE = 3000;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<List<float[]>> strokes = new ArrayList<>();
    private List<float[]> current;

    public SketchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(0xFFF8FAFC);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f * getResources().getDisplayMetrics().density);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        setBackgroundColor(0xFF101827);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (List<float[]> stroke : strokes) drawStroke(canvas, stroke);
        if (current != null) drawStroke(canvas, current);
    }

    private void drawStroke(Canvas canvas, List<float[]> stroke) {
        if (stroke == null || stroke.isEmpty()) return;
        Path path = new Path();
        float[] first = stroke.get(0);
        path.moveTo(first[0] * getWidth(), first[1] * getHeight());
        for (int i = 1; i < stroke.size(); i++) {
            float[] p = stroke.get(i);
            path.lineTo(p[0] * getWidth(), p[1] * getHeight());
        }
        canvas.drawPath(path, paint);
    }

    @Override public boolean onTouchEvent(MotionEvent event) {
        if (getWidth() <= 0 || getHeight() <= 0) return false;
        float x = clamp(event.getX() / getWidth());
        float y = clamp(event.getY() / getHeight());

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            if (strokes.size() >= MAX_STROKES) strokes.remove(0);
            current = new ArrayList<>();
            current.add(new float[]{x, y});
            invalidate();
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_MOVE && current != null) {
            if (current.size() < MAX_POINTS_PER_STROKE) current.add(new float[]{x, y});
            invalidate();
            return true;
        }
        if ((event.getActionMasked() == MotionEvent.ACTION_UP
                || event.getActionMasked() == MotionEvent.ACTION_CANCEL) && current != null) {
            if (current.size() < MAX_POINTS_PER_STROKE) current.add(new float[]{x, y});
            strokes.add(current);
            current = null;
            invalidate();
            return true;
        }
        return true;
    }

    public void clearSketch() {
        strokes.clear();
        current = null;
        invalidate();
    }

    public String serialize() {
        JSONArray all = new JSONArray();
        try {
            for (List<float[]> stroke : strokes) {
                JSONArray one = new JSONArray();
                for (float[] p : stroke) {
                    JSONArray point = new JSONArray();
                    point.put(p[0]);
                    point.put(p[1]);
                    one.put(point);
                }
                all.put(one);
            }
        } catch (Exception ignored) {}
        return all.toString();
    }

    public void load(String raw) {
        strokes.clear();
        try {
            JSONArray all = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < Math.min(all.length(), MAX_STROKES); i++) {
                JSONArray one = all.optJSONArray(i);
                if (one == null) continue;
                ArrayList<float[]> stroke = new ArrayList<>();
                for (int j = 0; j < Math.min(one.length(), MAX_POINTS_PER_STROKE); j++) {
                    JSONArray point = one.optJSONArray(j);
                    if (point != null && point.length() >= 2) {
                        stroke.add(new float[]{
                                clamp((float) point.optDouble(0, 0)),
                                clamp((float) point.optDouble(1, 0))
                        });
                    }
                }
                if (!stroke.isEmpty()) strokes.add(stroke);
            }
        } catch (Exception ignored) {}
        invalidate();
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
