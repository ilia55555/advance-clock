package com.ilia.advanceclock;

/** Pixel geometry shared by all four reminder screens. */
final class ReminderLayout {
    static final class Placement {
        final int top, height;
        Placement(int top, int height) { this.top = top; this.height = height; }
    }
    static Placement centered(int screenHeight, int headerEnd, int footerStart, int preferredHeight) {
        int topBound = Math.max(0, Math.min(headerEnd, screenHeight));
        int bottomBound = Math.max(topBound, Math.min(footerStart, screenHeight));
        int height = Math.max(0, Math.min(preferredHeight, bottomBound - topBound));
        int centeredTop = (screenHeight - height) / 2;
        return new Placement(Math.max(topBound, Math.min(centeredTop, bottomBound - height)), height);
    }
}
