package com.ilia.advanceclock;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AlarmPhotoLayoutTest {
    @Test public void squareHalfFramesOnTallPhones() {
        assertEquals(2400, AlarmPhotoLayout.height(2400, 1));
        assertEquals(1200, AlarmPhotoLayout.height(2400, 2));
        assertEquals(1.2, AlarmPhotoLayout.fillScale(1000, 1000, 1200, 1200), 0.00001);
    }
    @Test public void tabletFramesUseTabletProportions() {
        assertEquals(1600, AlarmPhotoLayout.height(1600, 1));
        assertEquals(800, AlarmPhotoLayout.height(1600, 2));
        assertEquals(2.56, AlarmPhotoLayout.fillScale(1000, 2000, 2560, 800), 0.00001);
    }
    @Test public void cropCoversEveryPixelWithoutStretchingOrLetterboxing() {
        int[][] cases = {{4000,3000,1080,2400}, {3000,4000,1080,1200},
                {4000,3000,2560,1600}, {3000,4000,2560,800}, {2000,2000,1080,1080}};
        for (int[] c : cases) {
            double scale = AlarmPhotoLayout.fillScale(c[0], c[1], c[2], c[3]);
            assertTrue(c[0] * scale + 0.0001 >= c[2]);
            assertTrue(c[1] * scale + 0.0001 >= c[3]);
            assertEquals((double)c[0]/c[1], (c[0]*scale)/(c[1]*scale), 0.00001);
        }
    }
    @Test public void panningStopsAtTheImageEdges() {
        assertEquals(0, AlarmPhotoLayout.clampOffset(8, 1000, 1000, 1), 0.00001);
        assertEquals(0.5, AlarmPhotoLayout.clampOffset(8, 1000, 1000, 2), 0.00001);
        assertEquals(-0.5, AlarmPhotoLayout.clampOffset(-8, 1000, 1000, 2), 0.00001);
        assertEquals(0.2, AlarmPhotoLayout.clampOffset(0.2, 1000, 1000, 2), 0.00001);
    }
    @Test public void normalizedFramingMatchesPreviewAndFullResolutionExport() {
        double zoom = 2;
        double previewScale = AlarmPhotoLayout.fillScale(4000, 3000, 300, 600) * zoom;
        double exportScale = AlarmPhotoLayout.fillScale(4000, 3000, 1200, 2400) * zoom;
        for (double offset : new double[]{-10, -0.25, 0, 0.25, 10}) {
            assertEquals(AlarmPhotoLayout.clampOffset(offset, 4000, 300, previewScale),
                    AlarmPhotoLayout.clampOffset(offset, 4000, 1200, exportScale), 0.00001);
            assertEquals(AlarmPhotoLayout.clampOffset(offset, 3000, 600, previewScale),
                    AlarmPhotoLayout.clampOffset(offset, 3000, 2400, exportScale), 0.00001);
        }
    }
}
