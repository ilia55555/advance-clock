package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {
    private static final String PREFS = "advance_clock_settings";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static final int ACCENT_TEAL = 0;
    public static final int ACCENT_BLUE = 1;
    public static final int ACCENT_PURPLE = 2;
    public static final int ACCENT_GREEN = 3;
    public static final int ACCENT_ORANGE = 4;

    public static final int CLOCK_LAYOUT_CURRENT = 0;
    public static final int CLOCK_LAYOUT_CALENDAR_FIRST = 1;

    private AppSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static int themeMode(Context context) {
        return prefs(context).getInt("theme_mode", THEME_LIGHT);
    }

    public static void setThemeMode(Context context, int value) {
        prefs(context).edit().putInt("theme_mode", value).apply();
    }

    public static int accent(Context context) {
        return prefs(context).getInt("accent", ACCENT_TEAL);
    }

    public static void setAccent(Context context, int value) {
        prefs(context).edit().putInt("accent", value).apply();
    }

    public static int defaultCalendar(Context context) {
        return prefs(context).getInt("default_calendar", CalendarUtils.PERSIAN);
    }

    public static void setDefaultCalendar(Context context, int value) {
        prefs(context).edit().putInt("default_calendar", value).apply();
    }

    public static int alarmScreenStyle(Context context) {
        return prefs(context).getInt("alarm_screen_style", 0);
    }

    public static void setAlarmScreenStyle(Context context, int value) {
        prefs(context).edit().putInt("alarm_screen_style", value).apply();
    }

    public static int clockLayoutMode(Context context) {
        return prefs(context).getInt("clock_layout_mode", CLOCK_LAYOUT_CURRENT);
    }

    public static void setClockLayoutMode(Context context, int value) {
        prefs(context).edit().putInt("clock_layout_mode", value).apply();
    }

    public static int primaryColor(Context context) {
        switch (accent(context)) {
            case ACCENT_BLUE: return 0xFF1769AA;
            case ACCENT_PURPLE: return 0xFF6B4CC2;
            case ACCENT_GREEN: return 0xFF2E7D59;
            case ACCENT_ORANGE: return 0xFFB85C00;
            case ACCENT_TEAL:
            default: return 0xFF006666;
        }
    }

    public static int surface(Context context) {
        return themeMode(context) == THEME_DARK ? 0xFF171A1C : 0xFFFFFFFF;
    }

    public static int field(Context context) {
        return themeMode(context) == THEME_DARK ? 0xFF22272A : 0xFFF4F7F7;
    }

    public static int background(Context context) {
        return themeMode(context) == THEME_DARK ? 0xFF0F1214 : 0xFFF4F9F8;
    }

    public static int textPrimary(Context context) {
        return themeMode(context) == THEME_DARK ? 0xFFF2F5F4 : 0xFF173F3B;
    }

    public static int textSecondary(Context context) {
        return themeMode(context) == THEME_DARK ? 0xFFAFBCB8 : 0xFF758783;
    }

    public static void applyTheme(Activity activity) {
        boolean dark = themeMode(activity) == THEME_DARK;
        int style;
        switch (accent(activity)) {
            case ACCENT_BLUE:
                style = dark ? R.style.Theme_AdvanceClock_Blue_Dark : R.style.Theme_AdvanceClock_Blue_Light;
                break;
            case ACCENT_PURPLE:
                style = dark ? R.style.Theme_AdvanceClock_Purple_Dark : R.style.Theme_AdvanceClock_Purple_Light;
                break;
            case ACCENT_GREEN:
                style = dark ? R.style.Theme_AdvanceClock_Green_Dark : R.style.Theme_AdvanceClock_Green_Light;
                break;
            case ACCENT_ORANGE:
                style = dark ? R.style.Theme_AdvanceClock_Orange_Dark : R.style.Theme_AdvanceClock_Orange_Light;
                break;
            case ACCENT_TEAL:
            default:
                style = dark ? R.style.Theme_AdvanceClock_Teal_Dark : R.style.Theme_AdvanceClock_Teal_Light;
                break;
        }
        activity.setTheme(style);
    }
}
