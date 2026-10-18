package com.ilia.advanceclock;

/** Geometry depends on the current display, never on a fixed phone aspect ratio. */
final class AlarmPhotoLayout {
    static int height(int screenHeight, int count) { return Math.max(1, screenHeight / (count > 1 ? 2 : 1)); }
    static double fillScale(int sourceWidth, int sourceHeight, int targetWidth, int targetHeight) {
        return Math.max((double) targetWidth / sourceWidth, (double) targetHeight / sourceHeight);
    }
}
