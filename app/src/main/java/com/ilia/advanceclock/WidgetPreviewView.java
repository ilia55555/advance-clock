package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

final class WidgetPreviewView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private String kind = "clock";
    private int backgroundColor = 0x22111418;
    private int textColor = 0xFFFFFFFF;
    private int timeColor = 0xFFFFFFFF;
    private boolean showHeader = true;
    private boolean showDetails = true;
    private int itemCount = 3;
    private float worldTextScale = 1f;
    private float worldNameScale = 1f;
    private float worldDateScale = 1f;
    private float worldTopGapDp = 1f;
    private float worldBottomGapDp = 1f;

    WidgetPreviewView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
    }

    void configure(String kind, int backgroundColor, int textColor, int timeColor,
                   boolean showHeader, boolean showDetails, int itemCount) {
        this.kind = kind;
        this.backgroundColor = backgroundColor;
        this.textColor = textColor;
        this.timeColor = timeColor;
        this.showHeader = showHeader;
        this.showDetails = showDetails;
        this.itemCount = Math.max(1, Math.min(4, itemCount));
        invalidate();
    }

    void setWorldTextSize(int preset) {
        worldTextScale = worldFactor(preset);
        invalidate();
    }

    void setWorldNameTextSize(int preset) {
        worldNameScale = worldFactor(preset);
        invalidate();
    }

    void setWorldDateTextSize(int preset) {
        worldDateScale = worldFactor(preset);
        invalidate();
    }

    void setWorldGaps(int topPreset, int bottomPreset) {
        float[] gaps = {0f, 1f, 2f, 4f, 6f};
        worldTopGapDp = gaps[Math.max(0, Math.min(gaps.length - 1, topPreset))];
        worldBottomGapDp = gaps[
                Math.max(0, Math.min(gaps.length - 1, bottomPreset))];
        invalidate();
    }

    private float worldFactor(int preset) {
        float[] factors = {0.70f, 0.80f, 0.90f, 1.00f, 1.10f, 1.20f, 1.30f};
        return factors[Math.max(0, Math.min(factors.length - 1, preset))];
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        RectF card = new RectF(2 * density, 2 * density,
                getWidth() - 2 * density, getHeight() - 2 * density);
        paint.setColor(backgroundColor);
        paint.setShadowLayer(5 * density, 0, 2 * density, 0x33000000);
        canvas.drawRoundRect(card, 18 * density, 18 * density, paint);
        paint.clearShadowLayer();

        if ("world".equals(kind)) drawWorld(canvas, density);
        else drawListWidget(canvas, density);
    }

    private void drawWorld(Canvas canvas, float d) {
        String[] cities = {
                AppString.get(R.string.preview_city_tehran),
                AppString.get(R.string.preview_city_london),
                AppString.get(R.string.preview_city_tokyo)};
        String[] times = {"14:24", "11:54", "20:54"};
        int columns = Math.min(3, itemCount);
        float columnWidth = (getWidth() - 20 * d) / columns;
        for (int i = 0; i < columns; i++) {
            float center = 10 * d + columnWidth * i + columnWidth / 2f;
            float timeSize = 28 * d * worldTextScale;
            float nameSize = 14 * d * worldNameScale;
            float dateSize = 12 * d * worldDateScale;
            float timeBaseline = 76 * d;
            float nameBaseline = timeBaseline
                    - timeSize * 0.86f
                    - worldTopGapDp * d;
            float dateBaseline = timeBaseline
                    + dateSize * 1.12f
                    + worldBottomGapDp * d;

            text(canvas, cities[i], center, nameBaseline, nameSize,
                    textColor, Paint.Align.CENTER, false);
            text(canvas, times[i], center, timeBaseline, timeSize,
                    timeColor, Paint.Align.CENTER, true);
            text(canvas, AppString.get(R.string.preview_sample_date), center,
                    dateBaseline, dateSize,
                    textColor, Paint.Align.CENTER, false);
        }
    }

    private void drawListWidget(Canvas canvas, float d) {
        float y = 26 * d;
        if (showHeader) {
            text(canvas, "14:24", 16 * d, y, 22 * d, timeColor, Paint.Align.LEFT, true);
            text(canvas, "+   ⚙", getWidth() - 16 * d, y, 18 * d,
                    timeColor, Paint.Align.RIGHT, true);
            y += 25 * d;
        }
        String title = "media".equals(kind) ? AppString.get(R.string.runtime_text_0060)
                : ("note".equals(kind) ? AppString.get(R.string.runtime_text_0016)
                : AppString.get(R.string.runtime_text_0018));
        text(canvas, title, 16 * d, y, 13 * d, textColor, Paint.Align.LEFT, true);
        y += 17 * d;
        for (int i = 0; i < itemCount && y < getHeight() - 8 * d; i++) {
            paint.setColor((textColor & 0x00FFFFFF) | 0x26000000);
            canvas.drawRoundRect(new RectF(12 * d, y, getWidth() - 12 * d,
                    y + 24 * d), 7 * d, 7 * d, paint);
            text(canvas, "media".equals(kind)
                            ? AppString.get(R.string.preview_file_number, i + 1)
                            : ("note".equals(kind)
                            ? AppString.get(R.string.preview_note_number, i + 1)
                            : "07:" + (i + 1) + "0"),
                    20 * d, y + 17 * d, 12 * d, textColor, Paint.Align.LEFT, false);
            if (showDetails) text(canvas, "•••", getWidth() - 20 * d, y + 17 * d,
                    11 * d, textColor, Paint.Align.RIGHT, false);
            y += 29 * d;
        }
    }

    private void text(Canvas canvas, String value, float x, float y, float size,
                      int color, Paint.Align align, boolean bold) {
        paint.setColor(color);
        paint.setTextSize(size);
        paint.setTextAlign(align);
        paint.setTypeface(bold ? android.graphics.Typeface.DEFAULT_BOLD
                : android.graphics.Typeface.DEFAULT);
        canvas.drawText(value, x, y, paint);
    }
}
