package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public final class AppSettings {
    private static final String PREFS = "advance_clock_settings";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static final int ACCENT_TEAL = 0;
    public static final int ACCENT_SAPPHIRE = 1;
    public static final int ACCENT_VIOLET = 2;
    public static final int ACCENT_EMERALD = 3;
    public static final int ACCENT_CORAL = 4;
    public static final int ACCENT_ROSE = 5;
    public static final int ACCENT_AMBER = 6;
    public static final int ACCENT_INDIGO = 7;

    public static final int CLOCK_LAYOUT_CURRENT = 0;
    public static final int CLOCK_LAYOUT_CALENDAR_FIRST = 1;

    private static final int[] LIGHT_COLORS = {
            0xFF087C77, 0xFF2563A6, 0xFF6D4CC4, 0xFF2F7D5B,
            0xFFCB5F47, 0xFFC64F79, 0xFFB7791F, 0xFF4355B9
    };

    private static final int[] DARK_COLORS = {
            0xFF38AAA4, 0xFF68A7DE, 0xFFA58AE8, 0xFF6BC79A,
            0xFFF08B74, 0xFFE888A8, 0xFFE2B05A, 0xFF8E9BEA
    };

    private AppSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String[] accentNames() {
        return new String[]{
                "سبزآبی عمیق", "یاقوت کبود", "بنفش مخملی", "زمردی",
                "مرجانی", "رز", "کهربایی", "نیلی"
        };
    }

    public static int themeMode(Context context) {
        return prefs(context).getInt("theme_mode", THEME_LIGHT);
    }

    public static void setThemeMode(Context context, int value) {
        prefs(context).edit().putInt("theme_mode", value).apply();
    }

    public static int accent(Context context) {
        return clampAccent(prefs(context).getInt("accent", ACCENT_TEAL));
    }

    public static void setAccent(Context context, int value) {
        prefs(context).edit().putInt("accent", clampAccent(value)).apply();
    }

    public static int secondaryAccent(Context context) {
        return clampAccent(prefs(context).getInt("secondary_accent", ACCENT_ROSE));
    }

    public static void setSecondaryAccent(Context context, int value) {
        prefs(context).edit().putInt("secondary_accent", clampAccent(value)).apply();
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
        return colorForAccent(context, accent(context));
    }

    public static int secondaryColor(Context context) {
        return colorForAccent(context, secondaryAccent(context));
    }

    public static int colorForAccent(Context context, int accent) {
        return colorForAccent(accent, themeMode(context) == THEME_DARK);
    }

    public static int colorForAccent(int accent, boolean dark) {
        accent = clampAccent(accent);
        return dark ? DARK_COLORS[accent] : LIGHT_COLORS[accent];
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
            case ACCENT_SAPPHIRE:
                style = dark ? R.style.Theme_AdvanceClock_Sapphire_Dark : R.style.Theme_AdvanceClock_Sapphire_Light;
                break;
            case ACCENT_VIOLET:
                style = dark ? R.style.Theme_AdvanceClock_Violet_Dark : R.style.Theme_AdvanceClock_Violet_Light;
                break;
            case ACCENT_EMERALD:
                style = dark ? R.style.Theme_AdvanceClock_Emerald_Dark : R.style.Theme_AdvanceClock_Emerald_Light;
                break;
            case ACCENT_CORAL:
                style = dark ? R.style.Theme_AdvanceClock_Coral_Dark : R.style.Theme_AdvanceClock_Coral_Light;
                break;
            case ACCENT_ROSE:
                style = dark ? R.style.Theme_AdvanceClock_Rose_Dark : R.style.Theme_AdvanceClock_Rose_Light;
                break;
            case ACCENT_AMBER:
                style = dark ? R.style.Theme_AdvanceClock_Amber_Dark : R.style.Theme_AdvanceClock_Amber_Light;
                break;
            case ACCENT_INDIGO:
                style = dark ? R.style.Theme_AdvanceClock_Indigo_Dark : R.style.Theme_AdvanceClock_Indigo_Light;
                break;
            case ACCENT_TEAL:
            default:
                style = dark ? R.style.Theme_AdvanceClock_Teal_Dark : R.style.Theme_AdvanceClock_Teal_Light;
                break;
        }
        activity.setTheme(style);
        activity.getTheme().applyStyle(secondaryOverlay(secondaryAccent(activity), dark), true);
    }

    public static void applyModalOverlay(Activity activity) {
        activity.getTheme().applyStyle(R.style.OverlayAdvanceClockModal, true);
    }

    private static int secondaryOverlay(int accent, boolean dark) {
        switch (clampAccent(accent)) {
            case ACCENT_SAPPHIRE: return dark ? R.style.OverlaySecondarySapphireDark : R.style.OverlaySecondarySapphireLight;
            case ACCENT_VIOLET: return dark ? R.style.OverlaySecondaryVioletDark : R.style.OverlaySecondaryVioletLight;
            case ACCENT_EMERALD: return dark ? R.style.OverlaySecondaryEmeraldDark : R.style.OverlaySecondaryEmeraldLight;
            case ACCENT_CORAL: return dark ? R.style.OverlaySecondaryCoralDark : R.style.OverlaySecondaryCoralLight;
            case ACCENT_ROSE: return dark ? R.style.OverlaySecondaryRoseDark : R.style.OverlaySecondaryRoseLight;
            case ACCENT_AMBER: return dark ? R.style.OverlaySecondaryAmberDark : R.style.OverlaySecondaryAmberLight;
            case ACCENT_INDIGO: return dark ? R.style.OverlaySecondaryIndigoDark : R.style.OverlaySecondaryIndigoLight;
            case ACCENT_TEAL:
            default: return dark ? R.style.OverlaySecondaryTealDark : R.style.OverlaySecondaryTealLight;
        }
    }

    private static int clampAccent(int value) {
        return Math.max(0, Math.min(7, value));
    }
}
