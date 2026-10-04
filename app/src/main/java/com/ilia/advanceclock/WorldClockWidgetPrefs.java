package com.ilia.advanceclock;

import android.content.Context;

public final class WorldClockWidgetPrefs {
    private static final String PREFS = "world_clock_widget_style";
    public static final int DEFAULT_TIME_SIZE = 3;
    private static final int MIN_TIME_SIZE = 0;
    private static final int MAX_TIME_SIZE = 6;
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
        return Math.max(MIN_TIME_SIZE, Math.min(MAX_TIME_SIZE,
                prefs(context).getInt("time_size_" + id, DEFAULT_TIME_SIZE)));
    }

    public static void save(Context context, int id, int background,
                            int textColor, int timeColor, int timeWeight, int timeSize) {
        prefs(context).edit()
                .putInt("background_" + id, background)
                .putInt("text_" + id, textColor)
                .putInt("time_" + id, timeColor)
                .putInt("time_weight_" + id, Math.max(0, Math.min(2, timeWeight)))
                .putInt("time_size_" + id, Math.max(MIN_TIME_SIZE,
                        Math.min(MAX_TIME_SIZE, timeSize)))
                .apply();
    }

    public static void delete(Context context, int id) {
        prefs(context).edit().remove("background_" + id).remove("text_" + id)
                .remove("time_" + id).remove("time_weight_" + id)
                .remove("time_size_" + id)
                .remove("title_" + id).apply();
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

    private static android.content.SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
