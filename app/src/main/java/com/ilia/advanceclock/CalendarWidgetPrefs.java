package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

public final class CalendarWidgetPrefs {
    private static final String PREFS = "calendar_widget_prefs";
    private static final String SHOW_EVENTS = "show_events_";
    private static final String SHOW_PRAYER = "show_prayer_";
    private static final String SELECTED_MILLIS = "selected_millis_";
    private static final String VISIBLE_MONTH_MILLIS = "visible_month_millis_";

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

    public static long selectedMillis(Context context, int widgetId) {
        return prefs(context).getLong(
                SELECTED_MILLIS + widgetId,
                System.currentTimeMillis());
    }

    public static void setSelectedMillis(Context context, int widgetId, long value) {
        prefs(context).edit().putLong(
                SELECTED_MILLIS + widgetId,
                value).apply();
    }

    public static long visibleMonthMillis(Context context, int widgetId) {
        return prefs(context).getLong(
                VISIBLE_MONTH_MILLIS + widgetId,
                selectedMillis(context, widgetId));
    }

    public static void setVisibleMonthMillis(Context context, int widgetId, long value) {
        prefs(context).edit().putLong(
                VISIBLE_MONTH_MILLIS + widgetId,
                value).apply();
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
        if (values.contains(SELECTED_MILLIS + oldId)) {
            editor.putLong(
                    SELECTED_MILLIS + newId,
                    values.getLong(SELECTED_MILLIS + oldId, System.currentTimeMillis()));
        }
        if (values.contains(VISIBLE_MONTH_MILLIS + oldId)) {
            editor.putLong(
                    VISIBLE_MONTH_MILLIS + newId,
                    values.getLong(VISIBLE_MONTH_MILLIS + oldId, System.currentTimeMillis()));
        }
        editor.apply();
        clear(context, oldId);
    }

    public static void clear(Context context, int widgetId) {
        prefs(context).edit()
                .remove(SHOW_EVENTS + widgetId)
                .remove(SHOW_PRAYER + widgetId)
                .remove(SELECTED_MILLIS + widgetId)
                .remove(VISIBLE_MONTH_MILLIS + widgetId)
                .apply();
    }
}
