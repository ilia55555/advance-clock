package com.ilia.advanceclock;

public final class PriorityUtils {
    public static final int LOW = 0;
    public static final int RELATIVELY_LOW = 1;
    public static final int MEDIUM = 2;
    public static final int HIGH = 3;
    public static final int VERY_HIGH = 4;

    private static final String[] LABELS = {
            "کم", "نسبتاً کم", "متوسط", "زیاد", "خیلی زیاد"
    };

    private PriorityUtils() {}

    public static String[] labels() { return LABELS.clone(); }

    public static String label(int value) {
        return LABELS[Math.max(0, Math.min(LABELS.length - 1, value))];
    }

    public static int clamp(int value) {
        return Math.max(LOW, Math.min(VERY_HIGH, value));
    }

    public static int migrateLegacy(int oldValue) {
        if (oldValue <= 0) return LOW;
        if (oldValue == 1) return MEDIUM;
        return VERY_HIGH;
    }
}
