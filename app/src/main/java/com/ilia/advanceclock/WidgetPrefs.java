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

    public static int palette(Context context, int widgetId) {
        int stored = prefs(context).getInt(
                key("palette", widgetId),
                AppSettings.palette(context));
        return Math.max(0, Math.min(6, stored));
    }

    public static void setPalette(Context context, int widgetId, int palette) {
        prefs(context).edit()
                .putInt(key("palette", widgetId), Math.max(0, Math.min(6, palette)))
                .remove(key("accent", widgetId))
                .apply();
    }

    // Compatibility aliases.
    public static int accent(Context context, int widgetId) {
        return palette(context, widgetId);
    }

    public static void setAccent(Context context, int widgetId, int accent) {
        setPalette(context, widgetId, accent);
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

    public static boolean showHeader(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_header", widgetId), true);
    }

    public static void setShowHeader(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_header", widgetId), value).apply();
    }

    public static boolean showDate(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_date", widgetId), true);
    }

    public static void setShowDate(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_date", widgetId), value).apply();
    }

    public static boolean showTime(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_time", widgetId), true);
    }

    public static void setShowTime(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_time", widgetId), value).apply();
    }

    public static int timeFormatMode(Context context, int widgetId) {
        return Math.max(0, Math.min(2,
                prefs(context).getInt(key("time_format", widgetId), 0)));
    }

    public static void setTimeFormatMode(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(
                key("time_format", widgetId),
                Math.max(0, Math.min(2, value))).apply();
    }

    public static boolean showSeconds(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_seconds", widgetId), false);
    }

    public static void setShowSeconds(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_seconds", widgetId), value).apply();
    }

    public static boolean showAddButton(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_add", widgetId), true);
    }

    public static void setShowAddButton(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_add", widgetId), value).apply();
    }

    public static boolean showSettingsButton(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_settings", widgetId), true);
    }

    public static void setShowSettingsButton(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_settings", widgetId), value).apply();
    }

    public static boolean showSectionLabel(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_section_label", widgetId), true);
    }

    public static void setShowSectionLabel(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_section_label", widgetId), value).apply();
    }

    public static boolean showPriority(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_priority", widgetId), true);
    }

    public static void setShowPriority(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_priority", widgetId), value).apply();
    }

    public static boolean showMetadata(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_metadata", widgetId), true);
    }

    public static void setShowMetadata(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_metadata", widgetId), value).apply();
    }

    public static int maxItems(Context context, int widgetId) {
        return Math.max(1, Math.min(10,
                prefs(context).getInt(key("max_items", widgetId), 10)));
    }

    public static void setMaxItems(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(
                key("max_items", widgetId),
                Math.max(1, Math.min(10, value))).apply();
    }

    public static int fontSizeMode(Context context, int widgetId) {
        return Math.max(0, Math.min(2,
                prefs(context).getInt(key("font_size", widgetId), 1)));
    }

    public static void setFontSizeMode(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(
                key("font_size", widgetId),
                Math.max(0, Math.min(2, value))).apply();
    }

    public static int backgroundOpacityMode(Context context, int widgetId) {
        return Math.max(0, Math.min(2,
                prefs(context).getInt(key("background_opacity", widgetId), 0)));
    }

    public static void setBackgroundOpacityMode(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(
                key("background_opacity", widgetId),
                Math.max(0, Math.min(2, value))).apply();
    }

    public static int sortMode(Context context, int widgetId) {
        return Math.max(0, Math.min(2,
                prefs(context).getInt(key("sort_mode", widgetId), 0)));
    }

    public static void setSortMode(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(
                key("sort_mode", widgetId),
                Math.max(0, Math.min(2, value))).apply();
    }

    public static boolean mediaShowPreview(Context context, int widgetId) {
        return prefs(context).getBoolean(key("media_show_preview", widgetId), true);
    }

    public static void setMediaShowPreview(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("media_show_preview", widgetId), value).apply();
    }

    public static boolean mediaShowFileName(Context context, int widgetId) {
        return prefs(context).getBoolean(key("media_show_name", widgetId), true);
    }

    public static void setMediaShowFileName(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("media_show_name", widgetId), value).apply();
    }

    public static boolean isDark(Context context, int widgetId) {
        int mode = themeMode(context, widgetId);
        if (mode == THEME_LIGHT) return false;
        if (mode == THEME_DARK) return true;
        return AppSettings.themeMode(context) == AppSettings.THEME_DARK;
    }

    public static int primary(Context context, int widgetId) {
        return AppSettings.primaryColorForPalette(palette(context, widgetId));
    }

    public static int secondary(Context context, int widgetId) {
        return AppSettings.secondaryColorForPalette(palette(context, widgetId));
    }

    public static void clear(Context context, int widgetId) {
        prefs(context).edit()
                .remove(key("theme", widgetId))
                .remove(key("palette", widgetId))
                .remove(key("accent", widgetId))
                .remove(key("width_cells", widgetId))
                .remove(key("height_cells", widgetId))
                .remove(key("show_header", widgetId))
                .remove(key("show_date", widgetId))
                .remove(key("show_time", widgetId))
                .remove(key("time_format", widgetId))
                .remove(key("show_seconds", widgetId))
                .remove(key("show_add", widgetId))
                .remove(key("show_settings", widgetId))
                .remove(key("show_section_label", widgetId))
                .remove(key("show_priority", widgetId))
                .remove(key("show_metadata", widgetId))
                .remove(key("max_items", widgetId))
                .remove(key("font_size", widgetId))
                .remove(key("background_opacity", widgetId))
                .remove(key("sort_mode", widgetId))
                .remove(key("media_show_preview", widgetId))
                .remove(key("media_show_name", widgetId))
                .apply();
    }
}
