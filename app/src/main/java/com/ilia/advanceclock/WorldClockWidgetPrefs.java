package com.ilia.advanceclock;

import android.content.Context;

public final class WorldClockWidgetPrefs {
    private static final String PREFS = "world_clock_widget_style";

    public static final int DEFAULT_TIME_SIZE = 3;
    public static final int DEFAULT_NAME_SIZE = 3;
    public static final int DEFAULT_DATE_SIZE = 3;
    public static final int DEFAULT_TOP_GAP = 1;
    public static final int DEFAULT_BOTTOM_GAP = 1;

    private static final int MIN_SIZE = 0;
    private static final int MAX_SIZE = 6;
    private static final int MIN_GAP = 0;
    private static final int MAX_GAP = 4;

    private WorldClockWidgetPrefs() {}

    public static int background(Context context, int id) {
        return prefs(context).getInt("background_" + id, 0);
    }

    public static int textColor(Context context, int id) {
        return prefs(context).getInt("text_" + id, 0xFFFFFFFF);
    }

    public static int timeColor(Context context, int id) {
        return prefs(context).getInt("time_" + id, 0xFFFFFFFF);
    }

    public static int timeWeight(Context context, int id) {
        return prefs(context).getInt("time_weight_" + id, 1);
    }

    public static int timeSize(Context context, int id) {
        return boundedSize(
                prefs(context).getInt(
                        "time_size_" + id,
                        DEFAULT_TIME_SIZE));
    }

    public static int nameSize(Context context, int id) {
        return boundedSize(
                prefs(context).getInt(
                        "name_size_" + id,
                        DEFAULT_NAME_SIZE));
    }

    public static int dateSize(Context context, int id) {
        return boundedSize(
                prefs(context).getInt(
                        "date_size_" + id,
                        DEFAULT_DATE_SIZE));
    }

    public static int topGap(Context context, int id) {
        return boundedGap(
                prefs(context).getInt(
                        "top_gap_" + id,
                        DEFAULT_TOP_GAP));
    }

    public static int bottomGap(Context context, int id) {
        return boundedGap(
                prefs(context).getInt(
                        "bottom_gap_" + id,
                        DEFAULT_BOTTOM_GAP));
    }

    public static void save(
            Context context,
            int id,
            int background,
            int textColor,
            int timeColor,
            int timeWeight,
            int timeSize,
            int nameSize,
            int dateSize,
            int topGap,
            int bottomGap) {
        prefs(context).edit()
                .putInt("background_" + id, background)
                .putInt("text_" + id, textColor)
                .putInt("time_" + id, timeColor)
                .putInt(
                        "time_weight_" + id,
                        Math.max(0, Math.min(2, timeWeight)))
                .putInt("time_size_" + id, boundedSize(timeSize))
                .putInt("name_size_" + id, boundedSize(nameSize))
                .putInt("date_size_" + id, boundedSize(dateSize))
                .putInt("top_gap_" + id, boundedGap(topGap))
                .putInt("bottom_gap_" + id, boundedGap(bottomGap))
                .apply();
    }

    public static void delete(Context context, int id) {
        prefs(context).edit()
                .remove("background_" + id)
                .remove("text_" + id)
                .remove("time_" + id)
                .remove("time_weight_" + id)
                .remove("time_size_" + id)
                .remove("name_size_" + id)
                .remove("date_size_" + id)
                .remove("top_gap_" + id)
                .remove("bottom_gap_" + id)
                .remove("title_" + id)
                .apply();
    }

    public static int backgroundResource(Context context, int id) {
        switch (background(context, id)) {
            case 1: return R.drawable.widget_world_dark_70;
            case 2: return R.drawable.widget_world_dark;
            case 3: return R.drawable.widget_world_light;
            case 4: return R.drawable.widget_world_blue;
            case 5: return R.drawable.widget_world_teal;
            case 6: return R.drawable.widget_world_purple;
            case 7: return R.drawable.widget_world_burgundy;
            case 8: return R.drawable.widget_world_green;
            default: return R.drawable.widget_world_transparent;
        }
    }

    private static int boundedSize(int value) {
        return Math.max(MIN_SIZE, Math.min(MAX_SIZE, value));
    }

    private static int boundedGap(int value) {
        return Math.max(MIN_GAP, Math.min(MAX_GAP, value));
    }

    private static android.content.SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
