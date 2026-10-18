package com.ilia.advanceclock;

/** Immutable layout for one launcher-provided size; no shared per-widget geometry. */
final class WorldClockWidgetLayout {
    final float width, height, itemWidth, itemHeight;
    final int columns, rows, capacity, zoneCount;
    final boolean compact;
    private WorldClockWidgetLayout(float width, float height, int columns, int rows, int capacity, int zoneCount) {
        this.width = width; this.height = height; this.columns = columns; this.rows = rows;
        this.capacity = capacity; this.zoneCount = zoneCount;
        itemWidth = Math.max(1, (width - 8 - (columns - 1) * 4) / columns);
        itemHeight = Math.max(1, (height - (rows - 1) * 2) / rows);
        compact = itemHeight < 96;
    }
    static WorldClockWidgetLayout forSize(float width, float height, int zoneCount) {
        width = Float.isFinite(width) && width > 0 ? width : 320;
        height = Float.isFinite(height) && height > 0 ? height : 110;
        int count = Math.max(1, zoneCount);
        int maxColumns = Math.round((width + 30) / 70) <= 3 ? 1 : 2;
        int columns = Math.min(count, maxColumns);
        int availableRows = Math.max(1, Math.round((height + 30) / 70));
        int rows = Math.min(availableRows, (count + columns - 1) / columns);
        return new WorldClockWidgetLayout(width, height, columns, rows, Math.min(count, columns * rows), Math.max(0, zoneCount));
    }
    int pages() { return Math.max(1, (zoneCount + capacity - 1) / capacity); }
    int normalizePage(int page) { return Math.max(0, Math.min(pages() - 1, page)); }
    WorldClockWidgetLayout forPage(int page) {
        int count = Math.max(1, Math.min(capacity, zoneCount - normalizePage(page) * capacity));
        int usedColumns = Math.min(columns, count);
        int usedRows = Math.max(1, (count + usedColumns - 1) / usedColumns);
        return new WorldClockWidgetLayout(width, height, usedColumns, usedRows, capacity, zoneCount);
    }
    String geometryKey() { return width + "x" + height + "_" + columns + "_" + rows + "_" + capacity; }

    static final class TextSizes {
        final float time, name, date, topGap, bottomGap;
        TextSizes(float time, float name, float date, float topGap, float bottomGap) {
            this.time = time; this.name = name; this.date = date; this.topGap = topGap; this.bottomGap = bottomGap;
        }
        float blockHeight() { return name * 1.18f + time * 1.12f + date * 1.18f + topGap + bottomGap; }
    }
    TextSizes textSizes(int timePreset, int namePreset, int datePreset, int topPreset, int bottomPreset) {
        float[] factors = {.7f, .8f, .9f, 1f, 1.1f, 1.2f, 1.3f};
        float time = Math.min(itemWidth / 3.15f, itemHeight * (compact ? .55f : .62f)) * factors[index(timePreset, 6)] / 1.3f;
        float name = (compact ? 12 : 19) * factors[index(namePreset, 6)];
        float date = (compact ? 10 : 15) * factors[index(datePreset, 6)];
        float[] gaps = compact ? new float[]{0, .5f, 1, 2, 3} : new float[]{0, 1, 2, 4, 6};
        float top = gaps[index(topPreset, 4)], bottom = gaps[index(bottomPreset, 4)];
        float block = name * 1.18f + time * 1.12f + date * 1.18f + top + bottom;
        float fit = Math.min(1, Math.max(1, itemHeight - 4) / block);
        return new TextSizes(time * fit, name * fit, date * fit, top * fit, bottom * fit);
    }
    private static int index(int value, int max) { return Math.max(0, Math.min(max, value)); }
}
