package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

public final class WidgetPrefs {
    public static final int THEME_FOLLOW_APP = 0;
    public static final int THEME_LIGHT = 1;
    public static final int THEME_DARK = 2;

    private static final String PREFS = "advance_clock_widget_prefs";

    private WidgetPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String prefix, int widgetId) {
        return prefix + "_" + widgetId;
    }

    public static int themeMode(Context context, int widgetId) {
        return prefs(context).getInt(key("theme", widgetId), THEME_FOLLOW_APP);
    }

    public static void setThemeMode(Context context, int widgetId, int mode) {
        prefs(context).edit().putInt(key("theme", widgetId), Math.max(0, Math.min(2, mode))).apply();
    }

    public static int accent(Context context, int widgetId) {
        return prefs(context).getInt(key("accent", widgetId), AppSettings.accent(context));
    }

    public static int widthCells(Context context, int widgetId) {
        return prefs(context).getInt(key("width_cells", widgetId), 3);
    }

    public static int heightCells(Context context, int widgetId) {
        return prefs(context).getInt(key("height_cells", widgetId), 2);
    }

    public static void setSizeCells(Context context, int widgetId, int width, int height) {
        prefs(context).edit()
                .putInt(key("width_cells", widgetId), Math.max(2, Math.min(6, width)))
                .putInt(key("height_cells", widgetId), Math.max(1, Math.min(6, height)))
                .apply();
    }

    public static void setAccent(Context context, int widgetId, int accent) {
        prefs(context).edit().putInt(key("accent", widgetId), Math.max(0, Math.min(7, accent))).apply();
    }

    public static boolean isDark(Context context, int widgetId) {
        int mode = themeMode(context, widgetId);
        if (mode == THEME_LIGHT) return false;
        if (mode == THEME_DARK) return true;
        return AppSettings.themeMode(context) == AppSettings.THEME_DARK;
    }

    public static int primary(Context context, int widgetId) {
        return AppSettings.colorForAccent(accent(context, widgetId), isDark(context, widgetId));
    }

    public static void clear(Context context, int widgetId) {
        prefs(context).edit()
                .remove(key("theme", widgetId))
                .remove(key("accent", widgetId))
                .remove(key("width_cells", widgetId))
                .remove(key("height_cells", widgetId))
                .apply();
    }
}
