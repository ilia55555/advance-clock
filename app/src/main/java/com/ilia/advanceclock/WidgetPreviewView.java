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
    private int worldTimePreset = 3, worldNamePreset = 3, worldDatePreset = 3;
    private int worldTopPreset = 1, worldBottomPreset = 1, worldWeight = 1;

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

    void setWorldTextSize(int preset) { worldTimePreset = preset; invalidate(); }
    void setWorldNameTextSize(int preset) { worldNamePreset = preset; invalidate(); }
    void setWorldDateTextSize(int preset) { worldDatePreset = preset; invalidate(); }
    void setWorldWeight(int weight) { worldWeight = weight; invalidate(); }
    void setWorldGaps(int top, int bottom) { worldTopPreset = top; worldBottomPreset = bottom; invalidate(); }

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
        java.util.List<String> zones = WorldClockStore.zones(getContext());
        WorldClockWidgetLayout base = WorldClockWidgetLayout.forSize(getWidth() / d - 4, getHeight() / d - 4, zones.size());
        WorldClockWidgetLayout layout = base.forPage(0);
        WorldClockWidgetLayout.TextSizes sizes = layout.textSizes(worldTimePreset, worldNamePreset, worldDatePreset, worldTopPreset, worldBottomPreset);
        int count = Math.min(zones.size(), base.capacity);
        java.text.SimpleDateFormat clock = new java.text.SimpleDateFormat("HH:mm", AppString.locale());
        java.text.SimpleDateFormat date = new java.text.SimpleDateFormat("EEE, MMM d", AppString.locale());
        for (int i = 0; i < count; i++) {
            String zone = zones.get(i);
            java.util.TimeZone tz = java.util.TimeZone.getTimeZone(zone); clock.setTimeZone(tz); date.setTimeZone(tz);
            int col = i % layout.columns, row = i / layout.columns;
            float left = (6 + col * (layout.itemWidth + 4)) * d;
            float top = (2 + row * (layout.itemHeight + 2)) * d;
            float center = left + layout.itemWidth * d / 2;
            canvas.save(); canvas.clipRect(left, top, left + layout.itemWidth * d, top + layout.itemHeight * d);
            android.graphics.Typeface regular = android.graphics.Typeface.DEFAULT;
            android.graphics.Typeface timeFace = android.graphics.Typeface.create(worldWeight == 0 ? "sans-serif-light" : "sans-serif",
                    worldWeight == 2 ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            float nameHeight = lineHeight(sizes.name * d, regular);
            float timeHeight = lineHeight(sizes.time * d, timeFace);
            float dateHeight = lineHeight(sizes.date * d, android.graphics.Typeface.DEFAULT_BOLD);
            float y = top + (layout.itemHeight * d - nameHeight - timeHeight - dateHeight - (sizes.topGap + sizes.bottomGap) * d) / 2;
            String city = "Canada/Saskatchewan".equals(zone) ? "Regina" : zone.substring(zone.lastIndexOf('/') + 1).replace('_', ' ');
            y = worldLine(canvas, WorldClockStore.label(getContext(), zone, city), center, y, sizes.name * d, textColor, regular) + sizes.topGap * d;
            y = worldLine(canvas, clock.format(new java.util.Date()), center, y, sizes.time * d, timeColor, timeFace) + sizes.bottomGap * d;
            worldLine(canvas, date.format(new java.util.Date()), center, y, sizes.date * d, textColor, android.graphics.Typeface.DEFAULT_BOLD);
            canvas.restore();
        }
    }
    private float lineHeight(float size, android.graphics.Typeface face) {
        paint.setTextSize(size); paint.setTypeface(face); Paint.FontMetrics metrics = paint.getFontMetrics();
        return metrics.descent - metrics.ascent;
    }
    private float worldLine(Canvas canvas, String value, float x, float top, float size, int color, android.graphics.Typeface face) {
        paint.setTextSize(size); paint.setTypeface(face); paint.setColor(color); paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics metrics = paint.getFontMetrics(); canvas.drawText(value, x, top - metrics.ascent, paint);
        return top + metrics.descent - metrics.ascent;
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
