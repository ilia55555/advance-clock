package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

public final class TimeToolsWidgetPrefs {
    public static final int TAB_STOPWATCH = 0;
    public static final int TAB_TIMER = 1;

    private static final String PREFS = "time_tools_widget_prefs";
    private static final String ACTIVE_TAB = "active_tab_";
    private static final String SHOW_DETAILS = "show_details_";
    private static final String SHOW_RESET = "show_reset_";
    private static final String SHOW_LAP = "show_lap_";
    private static final String SHOW_OPEN = "show_open_";
    private static final String TIMER_DEFAULT = "timer_default_";

    private TimeToolsWidgetPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int activeTab(Context context, int widgetId) {
        int value = prefs(context).getInt(ACTIVE_TAB + widgetId, TAB_STOPWATCH);
        return value == TAB_TIMER ? TAB_TIMER : TAB_STOPWATCH;
    }

    public static void setActiveTab(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(
                ACTIVE_TAB + widgetId,
                value == TAB_TIMER ? TAB_TIMER : TAB_STOPWATCH).apply();
    }

    public static boolean showDetails(Context context, int widgetId) {
        return prefs(context).getBoolean(SHOW_DETAILS + widgetId, true);
    }

    public static void setShowDetails(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(SHOW_DETAILS + widgetId, value).apply();
    }

    public static boolean showReset(Context context, int widgetId) {
        return prefs(context).getBoolean(SHOW_RESET + widgetId, true);
    }

    public static void setShowReset(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(SHOW_RESET + widgetId, value).apply();
    }

    public static boolean showLap(Context context, int widgetId) {
        return prefs(context).getBoolean(SHOW_LAP + widgetId, true);
    }

    public static void setShowLap(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(SHOW_LAP + widgetId, value).apply();
    }

    public static boolean showOpen(Context context, int widgetId) {
        return prefs(context).getBoolean(SHOW_OPEN + widgetId, true);
    }

    public static void setShowOpen(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(SHOW_OPEN + widgetId, value).apply();
    }

    public static long timerDefaultMillis(Context context, int widgetId) {
        return Math.max(1_000L, prefs(context).getLong(
                TIMER_DEFAULT + widgetId, 5 * 60_000L));
    }

    public static void setTimerDefaultMillis(Context context, int widgetId, long value) {
        prefs(context).edit().putLong(
                TIMER_DEFAULT + widgetId,
                Math.max(1_000L, value)).apply();
    }

    public static void migrate(Context context, int oldId, int newId) {
        SharedPreferences p = prefs(context);
        SharedPreferences.Editor e = p.edit();
        e.putInt(ACTIVE_TAB + newId, activeTab(context, oldId));
        e.putBoolean(SHOW_DETAILS + newId, showDetails(context, oldId));
        e.putBoolean(SHOW_RESET + newId, showReset(context, oldId));
        e.putBoolean(SHOW_LAP + newId, showLap(context, oldId));
        e.putBoolean(SHOW_OPEN + newId, showOpen(context, oldId));
        e.putLong(TIMER_DEFAULT + newId, timerDefaultMillis(context, oldId));
        e.apply();
        clear(context, oldId);
    }

    public static void clear(Context context, int widgetId) {
        prefs(context).edit()
                .remove(ACTIVE_TAB + widgetId)
                .remove(SHOW_DETAILS + widgetId)
                .remove(SHOW_RESET + widgetId)
                .remove(SHOW_LAP + widgetId)
                .remove(SHOW_OPEN + widgetId)
                .remove(TIMER_DEFAULT + widgetId)
                .apply();
    }
}
