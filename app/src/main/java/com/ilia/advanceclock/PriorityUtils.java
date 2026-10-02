package com.ilia.advanceclock;

public final class PriorityUtils {
    public static final int LOW = 0;
    public static final int RELATIVELY_LOW = 1;
    public static final int MEDIUM = 2;
    public static final int HIGH = 3;
    public static final int VERY_HIGH = 4;

    private static final String[] LABELS = {
            AppString.get(R.string.runtime_text_0024), AppString.get(R.string.runtime_text_0025), AppString.get(R.string.runtime_text_0026), AppString.get(R.string.runtime_text_0027), AppString.get(R.string.runtime_text_0028)
    };

    private PriorityUtils() {}

    public static String[] labels() { return LABELS.clone(); }

    public static String[] displayLabels() {
        String[] out = new String[LABELS.length];
        for (int i = 0; i < LABELS.length; i++) {
            out[i] = displayLabel(i);
        }
        return out;
    }

    public static String label(int value) {
        return LABELS[Math.max(0, Math.min(LABELS.length - 1, value))];
    }

    public static String displayLabel(int value) {
        return AppString.get(R.string.runtime_text_0420).trim()
                + " "
                + label(value).trim();
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
