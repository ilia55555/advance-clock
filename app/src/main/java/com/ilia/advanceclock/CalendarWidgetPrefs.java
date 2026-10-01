package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

public final class CalendarWidgetPrefs {
    private static final String PREFS = "calendar_widget_prefs";
    private static final String SHOW_EVENTS = "show_events_";
    private static final String SHOW_PRAYER = "show_prayer_";

    private CalendarWidgetPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean showEvents(Context context, int widgetId) {
        return prefs(context).getBoolean(SHOW_EVENTS + widgetId, true);
    }

    public static void setShowEvents(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(SHOW_EVENTS + widgetId, value).apply();
    }

    public static boolean showPrayerTimes(Context context, int widgetId) {
        return prefs(context).getBoolean(SHOW_PRAYER + widgetId, true);
    }

    public static void setShowPrayerTimes(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(SHOW_PRAYER + widgetId, value).apply();
    }

    public static void migrate(Context context, int oldId, int newId) {
        SharedPreferences values = prefs(context);
        SharedPreferences.Editor editor = values.edit();
        if (values.contains(SHOW_EVENTS + oldId)) {
            editor.putBoolean(
                    SHOW_EVENTS + newId,
                    values.getBoolean(SHOW_EVENTS + oldId, true));
        }
        if (values.contains(SHOW_PRAYER + oldId)) {
            editor.putBoolean(
                    SHOW_PRAYER + newId,
                    values.getBoolean(SHOW_PRAYER + oldId, true));
        }
        editor.apply();
        clear(context, oldId);
    }

    public static void clear(Context context, int widgetId) {
        prefs(context).edit()
                .remove(SHOW_EVENTS + widgetId)
                .remove(SHOW_PRAYER + widgetId)
                .apply();
    }
}
