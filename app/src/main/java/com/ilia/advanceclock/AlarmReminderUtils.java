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
                AppString.get(R.string.runtime_text_0035), AppString.get(R.string.runtime_text_0036),
                AppString.get(R.string.runtime_text_0575), AppString.get(R.string.runtime_text_0576), AppString.get(R.string.runtime_text_0577),
                AppString.get(R.string.runtime_text_0578), AppString.get(R.string.runtime_text_0579), AppString.get(R.string.runtime_text_0580), AppString.get(R.string.runtime_text_0581), AppString.get(R.string.runtime_text_0582), AppString.get(R.string.runtime_text_0583)
        };
    }

    public static String labelForMinutes(int minutes) {
        switch (minutes) {
            case 5: return AppString.get(R.string.runtime_text_0575);
            case 15: return AppString.get(R.string.runtime_text_0576);
            case 30: return AppString.get(R.string.runtime_text_0577);
            case 60: return AppString.get(R.string.runtime_text_0578);
            case 120: return AppString.get(R.string.runtime_text_0579);
            case 180: return AppString.get(R.string.runtime_text_0580);
            case 360: return AppString.get(R.string.runtime_text_0581);
            case 720: return AppString.get(R.string.runtime_text_0582);
            case 1440: return AppString.get(R.string.runtime_text_0583);
            default: return minutes + AppString.get(R.string.runtime_text_0647);
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
        if (mode == MODE_NONE) return AppString.get(R.string.runtime_text_0035);
        if (mode == MODE_SMART) return AppString.get(R.string.runtime_text_0036);
        List<Integer> values = effective(mode, customJson);
        if (values.isEmpty()) return AppString.get(R.string.runtime_text_0035);
        if (values.size() == 1) return labelForMinutes(values.get(0));
        return values.size() + AppString.get(R.string.runtime_text_0671);
    }

    private static boolean isAllowed(int value) {
        for (int allowed : VALUES) if (allowed == value) return true;
        return false;
    }
}
