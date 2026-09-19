package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public final class HexPaletteView extends View {
    public interface Listener { void onColorSelected(int color); }

    private static final int[] COLORS = {
            0xFF111827,0xFF4B5563,0xFF9CA3AF,0xFFFFFFFF,0xFFFDE68A,0xFFFFD6A5,
            0xFFB91C1C,0xFFEF4444,0xFFF97316,0xFFF59E0B,0xFFEAB308,0xFF84CC16,
            0xFF166534,0xFF16A34A,0xFF10B981,0xFF0F766E,0xFF06B6D4,0xFF0891B2,
            0xFF1D4ED8,0xFF2563EB,0xFF4F46E5,0xFF7C3AED,0xFF9333EA,0xFFC026D3,
            0xFF9D174D,0xFFDB2777,0xFFF43F5E,0xFF7F1D1D,0xFF78350F,0xFF365314,
            0xFF14532D,0xFF134E4A,0xFF164E63,0xFF1E3A8A,0xFF312E81,0xFF581C87
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path hex = new Path();
    private Listener listener;
    private int selected = COLORS[15];

    public HexPaletteView(Context context) { this(context, null); }
    public HexPaletteView(Context context, AttributeSet attrs) {
        super(context, attrs);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(2));
    }

    public void setListener(Listener listener) { this.listener = listener; }
    public void setSelectedColor(int color) { selected = color; invalidate(); }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        if (w <= 0) w = dp(320);
        int cell = Math.max(dp(28), w / 7);
        int rows = (int) Math.ceil(COLORS.length / 6.0);
        int h = Math.round(rows * cell * 0.82f + cell * 0.35f);
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec));
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cellW = getWidth() / 6.65f;
        float radius = cellW * 0.44f;
        float rowStep = radius * 1.55f;

        for (int i = 0; i < COLORS.length; i++) {
            int row = i / 6;
            int col = i % 6;
            float cx = radius + col * cellW + (row % 2 == 1 ? cellW * 0.5f : 0f);
            float cy = radius + row * rowStep;
            drawHex(canvas, cx, cy, radius, COLORS[i]);

            if (closeColor(COLORS[i], selected)) {
                stroke.setColor(Color.WHITE);
                stroke.setStrokeWidth(dp(2));
                canvas.drawPath(hex, stroke);
                stroke.setColor(0xFF111827);
                stroke.setStrokeWidth(dp(1));
                Path inner = buildHex(cx, cy, radius - dp(3));
                canvas.drawPath(inner, stroke);
            }
        }
    }

    private void drawHex(Canvas canvas, float cx, float cy, float radius, int color) {
        buildHexInto(hex, cx, cy, radius);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        canvas.drawPath(hex, paint);
        stroke.setColor(color == Color.WHITE ? 0xFFB7C2BF : 0x22000000);
        stroke.setStrokeWidth(dp(1));
        canvas.drawPath(hex, stroke);
    }

    private Path buildHex(float cx, float cy, float radius) {
        Path p = new Path();
        buildHexInto(p, cx, cy, radius);
        return p;
    }

    private void buildHexInto(Path p, float cx, float cy, float radius) {
        p.reset();
        for (int i = 0; i < 6; i++) {
            double angle = Math.toRadians(60 * i - 30);
            float x = cx + radius * (float) Math.cos(angle);
            float y = cy + radius * (float) Math.sin(angle);
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getActionMasked() != MotionEvent.ACTION_UP) return true;

        float cellW = getWidth() / 6.65f;
        float radius = cellW * 0.44f;
        float rowStep = radius * 1.55f;

        float best = Float.MAX_VALUE;
        int bestIndex = -1;
        for (int i = 0; i < COLORS.length; i++) {
            int row = i / 6;
            int col = i % 6;
            float cx = radius + col * cellW + (row % 2 == 1 ? cellW * 0.5f : 0f);
            float cy = radius + row * rowStep;
            float d = (float) Math.hypot(e.getX() - cx, e.getY() - cy);
            if (d < radius * 1.05f && d < best) {
                best = d;
                bestIndex = i;
            }
        }

        if (bestIndex >= 0) {
            selected = COLORS[bestIndex];
            invalidate();
            if (listener != null) listener.onColorSelected(selected);
            performClick();
        }
        return true;
    }

    @Override public boolean performClick() {
        super.performClick();
        return true;
    }

    private static boolean closeColor(int a, int b) {
        int dr = Math.abs(Color.red(a) - Color.red(b));
        int dg = Math.abs(Color.green(a) - Color.green(b));
        int db = Math.abs(Color.blue(a) - Color.blue(b));
        return dr + dg + db < 18;
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
