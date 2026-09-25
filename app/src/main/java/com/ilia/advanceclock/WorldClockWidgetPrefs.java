package com.ilia.advanceclock;

import android.content.Context;

public final class WorldClockWidgetPrefs {
    private static final String PREFS = "world_clock_widget_style";
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

    public static void save(Context context, int id, int background,
                            int textColor, int timeColor) {
        prefs(context).edit()
                .putInt("background_" + id, background)
                .putInt("text_" + id, textColor)
                .putInt("time_" + id, timeColor)
                .apply();
    }

    public static void delete(Context context, int id) {
        prefs(context).edit().remove("background_" + id).remove("text_" + id)
                .remove("time_" + id).remove("title_" + id).apply();
    }

    public static int backgroundResource(Context context, int id) {
        switch (background(context, id)) {
            case 1: return R.drawable.widget_world_dark_70;
            case 2: return R.drawable.widget_world_dark;
            case 3: return R.drawable.widget_world_light;
            default: return R.drawable.widget_world_transparent;
        }
    }

    private static android.content.SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
