package com.ilia.advanceclock;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AlarmReminderUtils {
    public static final int MODE_NONE = 0;
    public static final int MODE_SMART = 1;
    public static final int MODE_CUSTOM = 2;

    public static final int[] VALUES = {5, 15, 30, 60, 120, 180, 360, 720, 1440};
    public static final int[] SMART_VALUES = {15, 60, 180, 720, 1440};

    private AlarmReminderUtils() {}

    public static String[] optionLabels() {
        return new String[]{
                "بدون یادآوری", "خودکار",
                "۵ دقیقه", "۱۵ دقیقه", "۳۰ دقیقه",
                "۱ ساعت", "۲ ساعت", "۳ ساعت", "۶ ساعت", "۱۲ ساعت", "۲۴ ساعت"
        };
    }

    public static String labelForMinutes(int minutes) {
        switch (minutes) {
            case 5: return "۵ دقیقه";
            case 15: return "۱۵ دقیقه";
            case 30: return "۳۰ دقیقه";
            case 60: return "۱ ساعت";
            case 120: return "۲ ساعت";
            case 180: return "۳ ساعت";
            case 360: return "۶ ساعت";
            case 720: return "۱۲ ساعت";
            case 1440: return "۲۴ ساعت";
            default: return minutes + " دقیقه";
        }
    }

    public static List<Integer> effective(int mode, String customJson) {
        ArrayList<Integer> out = new ArrayList<>();
        if (mode == MODE_SMART) {
            for (int value : SMART_VALUES) out.add(value);
            return out;
        }
        if (mode != MODE_CUSTOM) return out;
        try {
            JSONArray a = new JSONArray(customJson == null ? "[]" : customJson);
            for (int i = 0; i < a.length(); i++) {
                int v = a.optInt(i, -1);
                if (isAllowed(v) && !out.contains(v)) out.add(v);
            }
        } catch (Exception ignored) {}
        Collections.sort(out);
        return out;
    }

    public static String toJson(List<Integer> values) {
        JSONArray a = new JSONArray();
        if (values != null) {
            ArrayList<Integer> copy = new ArrayList<>();
            for (Integer value : values) {
                if (value != null && isAllowed(value) && !copy.contains(value)) copy.add(value);
            }
            Collections.sort(copy);
            for (Integer value : copy) a.put(value);
        }
        return a.toString();
    }

    public static String summary(int mode, String customJson) {
        if (mode == MODE_NONE) return "بدون یادآوری";
        if (mode == MODE_SMART) return "خودکار";
        List<Integer> values = effective(mode, customJson);
        if (values.isEmpty()) return "بدون یادآوری";
        if (values.size() == 1) return labelForMinutes(values.get(0));
        return values.size() + " یادآوری";
    }

    private static boolean isAllowed(int value) {
        for (int allowed : VALUES) if (allowed == value) return true;
        return false;
    }
}
