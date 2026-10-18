package com.ilia.advanceclock;

import org.junit.Test;
import static org.junit.Assert.*;

public final class WorldClockWidgetLayoutTest {
    @Test public void twoCitiesUseTheWholeHeightAfterVerticalResize() {
        WorldClockWidgetLayout small = WorldClockWidgetLayout.forSize(320, 40, 2);
        WorldClockWidgetLayout large = WorldClockWidgetLayout.forSize(320, 180, 2);
        assertEquals(1, small.rows); assertEquals(1, large.rows);
        assertEquals(180, large.itemHeight, .001);
        assertTrue(large.textSizes(3,3,3,1,1).time > small.textSizes(3,3,3,1,1).time);
    }
    @Test public void widthOnlyResizeChangesGeometryAndClockSize() {
        WorldClockWidgetLayout narrow = WorldClockWidgetLayout.forSize(250, 140, 2);
        WorldClockWidgetLayout wide = WorldClockWidgetLayout.forSize(400, 140, 2);
        assertNotEquals(narrow.geometryKey(), wide.geometryKey());
        assertTrue(wide.itemWidth > narrow.itemWidth);
        assertTrue(wide.textSizes(3,3,3,1,1).time > narrow.textSizes(3,3,3,1,1).time);
    }
    @Test public void twoRowsFitWhenThereAreActuallyFourCities() {
        WorldClockWidgetLayout one = WorldClockWidgetLayout.forSize(320, 40, 4);
        WorldClockWidgetLayout two = WorldClockWidgetLayout.forSize(320, 110, 4);
        assertEquals(2, one.capacity); assertEquals(4, two.capacity);
        assertEquals(2, two.rows);
        assertEquals(110, two.itemHeight * two.rows + (two.rows - 1) * 2, .001);
        assertEquals(320, two.itemWidth * two.columns + (two.columns - 1) * 4 + 8, .001);
    }
    @Test public void lastPageFillsItsAvailableSpaceWithoutChangingPagination() {
        WorldClockWidgetLayout base = WorldClockWidgetLayout.forSize(320, 110, 5);
        WorldClockWidgetLayout last = base.forPage(1);
        assertEquals(4, base.capacity); assertEquals(base.capacity, last.capacity);
        assertEquals(2, base.pages()); assertEquals(1, last.rows); assertEquals(1, last.columns);
        assertEquals(110, last.itemHeight, .001);
    }
    @Test public void nameAndDateControlsHaveNoFlatUpperPresets() {
        WorldClockWidgetLayout layout = WorldClockWidgetLayout.forSize(320, 100, 2);
        for (int preset = 1; preset <= 6; preset++) {
            assertTrue(layout.textSizes(3,preset,3,1,1).name > layout.textSizes(3,preset-1,3,1,1).name);
            assertTrue(layout.textSizes(3,3,preset,1,1).date > layout.textSizes(3,3,preset-1,1,1).date);
        }
    }
    @Test public void everyTextPresetFitsSmallRowsWithoutClipping() {
        for (float height : new float[]{40,55,70,100,180}) {
            WorldClockWidgetLayout layout = WorldClockWidgetLayout.forSize(180, height, 4);
            for (int size = 0; size <= 6; size++) {
                WorldClockWidgetLayout.TextSizes text = layout.textSizes(size,6,6,4,4);
                assertTrue(text.blockHeight() <= Math.max(1, layout.itemHeight - 4) + .001);
                assertTrue(text.time > 0); assertTrue(text.name > 0); assertTrue(text.date > 0);
            }
        }
    }
    @Test public void responsiveAlternativesDoNotOverwriteEachOthersGeometry() {
        WorldClockWidgetLayout portrait = WorldClockWidgetLayout.forSize(180, 200, 4);
        float original = portrait.itemHeight;
        WorldClockWidgetLayout landscape = WorldClockWidgetLayout.forSize(400, 80, 4);
        assertEquals(1, portrait.columns); assertEquals(2, landscape.columns);
        assertEquals(original, portrait.itemHeight, 0);
    }
    @Test public void emptyOrInvalidHostSizesRemainSafe() {
        WorldClockWidgetLayout layout = WorldClockWidgetLayout.forSize(Float.NaN, 0, 0);
        assertEquals(1, layout.capacity); assertEquals(1, layout.pages()); assertEquals(0, layout.normalizePage(99));
        assertTrue(Float.isFinite(layout.itemWidth)); assertTrue(layout.itemHeight > 0);
    }
}
