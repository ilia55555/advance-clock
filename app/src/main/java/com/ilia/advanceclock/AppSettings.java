package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.LocaleList;
import android.view.View;

import java.util.Locale;

public final class AppSettings {
    private static final String PREFS = "advance_clock_settings";

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;

    public static final int PALETTE_TERRACOTTA_NAVY = 0;
    public static final int PALETTE_MAGENTA_SKY = 1;
    public static final int PALETTE_MAGENTA_CHARCOAL = 2;
    public static final int PALETTE_TEAL_RED = 3;
    public static final int PALETTE_PURPLE_GOLD = 4;
    public static final int PALETTE_NEON_MAGENTA_GRAPHITE = 5;
    public static final int PALETTE_BLUE_CYAN = 6;
    public static final int DEFAULT_PALETTE = PALETTE_BLUE_CYAN;

    // Compatibility aliases for older code/preferences.
    public static final int ACCENT_TEAL = 0;
    public static final int ACCENT_SAPPHIRE = 1;
    public static final int ACCENT_VIOLET = 2;
    public static final int ACCENT_EMERALD = 3;
    public static final int ACCENT_CORAL = 4;
    public static final int ACCENT_ROSE = 5;
    public static final int ACCENT_AMBER = 6;
    public static final int ACCENT_INDIGO = 6;

    public static final int CLOCK_LAYOUT_CURRENT = 0;
    public static final int CLOCK_LAYOUT_CALENDAR_FIRST = 1;
    public static final String LANGUAGE_PERSIAN = "fa";
    public static final String LANGUAGE_ENGLISH = "en";
    public static final String LANGUAGE_CHINESE = "zh-CN";
    public static final String LANGUAGE_FRENCH = "fr";
    public static final String LANGUAGE_GERMAN = "de";
    public static final String LANGUAGE_SPANISH = "es";
    public static final String LANGUAGE_RUSSIAN = "ru";
    public static final String LANGUAGE_TURKISH = "tr";
    public static final String LANGUAGE_PORTUGUESE = "pt";
    public static final String LANGUAGE_HINDI = "hi";
    public static final String LANGUAGE_JAPANESE = "ja";
    public static final String LANGUAGE_ARABIC = "ar";

    private static final int[][] PALETTE_COLORS = {
            {0xFFD96B43, 0xFF1E2A38},
            {0xFFB422AF, 0xFF5597E2},
            {0xFFB422A8, 0xFF3B3847},
            {0xFF00A89D, 0xFFB42222},
            {0xFF6500A8, 0xFFECCE36},
            {0xFFD818F2, 0xFF22282A},
            {0xFF005FA8, 0xFF36BFEC}
    };

    private AppSettings() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String[] paletteNames() {
        return new String[]{
                "#D96B43  +  #1E2A38",
                "#B422AF  +  #5597E2",
                "#B422A8  +  #3B3847",
                "#00A89D  +  #B42222",
                "#6500A8  +  #ECCE36",
                "#D818F2  +  #22282A",
                "#005FA8  +  #36BFEC"
        };
    }

    public static int themeMode(Context context) {
        return prefs(context).getInt("theme_mode", THEME_LIGHT);
    }

    public static void setThemeMode(Context context, int value) {
        prefs(context).edit().putInt("theme_mode", value).apply();
    }

    public static int palette(Context context) {
        return clampPalette(prefs(context).getInt("palette", DEFAULT_PALETTE));
    }

    public static void setPalette(Context context, int value) {
        prefs(context).edit().putInt("palette", clampPalette(value)).apply();
    }

    public static int defaultCalendar(Context context) {
        return prefs(context).getInt("default_calendar", CalendarUtils.PERSIAN);
    }

    public static void setDefaultCalendar(Context context, int value) {
        prefs(context).edit().putInt("default_calendar", value).apply();
    }

    public static String language(Context context) {
        String saved = prefs(context).getString("app_language", null);
        return isSupportedLanguage(saved) ? saved : languageForDevice();
    }

    public static void setLanguage(Context context, String value) {
        if (!isSupportedLanguage(value)) value = LANGUAGE_ENGLISH;
        prefs(context).edit().putString("app_language", value).apply();
    }

    public static String[] languageCodes() {
        return new String[]{LANGUAGE_ENGLISH, LANGUAGE_PERSIAN, LANGUAGE_CHINESE,
                LANGUAGE_FRENCH, LANGUAGE_GERMAN, LANGUAGE_SPANISH, LANGUAGE_RUSSIAN,
                LANGUAGE_TURKISH, LANGUAGE_PORTUGUESE, LANGUAGE_HINDI,
                LANGUAGE_JAPANESE, LANGUAGE_ARABIC};
    }

    public static int languagePosition(Context context) {
        String selected = language(context);
        String[] codes = languageCodes();
        for (int i = 0; i < codes.length; i++) if (codes[i].equals(selected)) return i;
        return 0;
    }

    public static void applyLanguage(Context context) {
        String selected = language(context);
        Locale locale = Locale.forLanguageTag(selected);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocales(new LocaleList(locale));
        context.getResources().updateConfiguration(
                configuration, context.getResources().getDisplayMetrics());
    }

    public static String languageForDevice() {
        Locale device = android.content.res.Resources.getSystem()
                .getConfiguration().getLocales().get(0);
        String language = device.getLanguage();
        if ("zh".equals(language)) return LANGUAGE_CHINESE;
        for (String code : languageCodes()) {
            if (Locale.forLanguageTag(code).getLanguage().equals(language)) return code;
        }
        return LANGUAGE_ENGLISH;
    }

    private static boolean isSupportedLanguage(String value) {
        if (value == null) return false;
        for (String code : languageCodes()) if (code.equals(value)) return true;
        return false;
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

    public static boolean tabEnabled(Context context, String tab) {
        return prefs(context).getBoolean("tab_enabled_" + tab, true);
    }

    public static void setTabEnabled(Context context, String tab, boolean enabled) {
        prefs(context).edit().putBoolean("tab_enabled_" + tab, enabled).apply();
    }

    public static String[] tabOrder(Context context) {
        String saved = prefs(context).getString(
                "tab_order", "clock,world,noforget,timer,stopwatch");
        java.util.ArrayList<String> result = new java.util.ArrayList<>();
        if (saved != null) {
            for (String tab : saved.split(",")) {
                if (isKnownTab(tab) && !result.contains(tab)) result.add(tab);
            }
        }
        for (String tab : new String[]{"clock", "world", "noforget", "timer", "stopwatch"}) {
            if (!result.contains(tab)) result.add(tab);
        }
        return result.toArray(new String[0]);
    }

    public static void setTabOrder(Context context, java.util.List<String> tabs) {
        prefs(context).edit().putString(
                "tab_order", android.text.TextUtils.join(",", tabs)).apply();
    }

    private static boolean isKnownTab(String tab) {
        return "clock".equals(tab) || "noforget".equals(tab)
                || "stopwatch".equals(tab) || "timer".equals(tab)
                || "world".equals(tab);
    }

    public static boolean persistentDateNotificationEnabled(Context context) {
        return prefs(context).getBoolean("notification_persistent_date", true);
    }

    public static void setPersistentDateNotificationEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("notification_persistent_date", value).apply();
    }

    public static boolean persistentDateExtraCalendars(Context context) {
        return prefs(context).getBoolean("notification_persistent_extra_calendars", true);
    }

    public static void setPersistentDateExtraCalendars(Context context, boolean value) {
        prefs(context).edit().putBoolean("notification_persistent_extra_calendars", value).apply();
    }

    public static boolean alarmReminderNotificationsEnabled(Context context) {
        return prefs(context).getBoolean("notification_alarm_reminders", true);
    }

    public static void setAlarmReminderNotificationsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("notification_alarm_reminders", value).apply();
    }

    public static boolean noteReminderNotificationsEnabled(Context context) {
        return prefs(context).getBoolean("notification_note_reminders", true);
    }

    public static void setNoteReminderNotificationsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("notification_note_reminders", value).apply();
    }

    public static boolean notificationLockscreenDetails(Context context) {
        return prefs(context).getBoolean("notification_lockscreen_details", true);
    }

    public static void setNotificationLockscreenDetails(Context context, boolean value) {
        prefs(context).edit().putBoolean("notification_lockscreen_details", value).apply();
    }

    public static int notificationVisibility(Context context) {
        return notificationLockscreenDetails(context)
                ? android.app.Notification.VISIBILITY_PUBLIC
                : android.app.Notification.VISIBILITY_PRIVATE;
    }

    public static int primaryColor(Context context) {
        return primaryColorForPalette(palette(context));
    }

    public static int secondaryColor(Context context) {
        return secondaryColorForPalette(palette(context));
    }

    public static int primaryColorForPalette(int palette) {
        return PALETTE_COLORS[clampPalette(palette)][0];
    }

    public static int secondaryColorForPalette(int palette) {
        return PALETTE_COLORS[clampPalette(palette)][1];
    }

    // Legacy API kept so older callers and stored widget settings remain safe.
    public static String[] accentNames() {
        return paletteNames();
    }

    public static int accent(Context context) {
        return palette(context);
    }

    public static void setAccent(Context context, int value) {
        setPalette(context, value);
    }

    public static int secondaryAccent(Context context) {
        return palette(context);
    }

    public static void setSecondaryAccent(Context context, int value) {
        setPalette(context, value);
    }

    public static int colorForAccent(Context context, int accent) {
        return primaryColorForPalette(accent);
    }

    public static int colorForAccent(int accent, boolean dark) {
        return primaryColorForPalette(accent);
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
        applyLanguage(activity);
        boolean dark = themeMode(activity) == THEME_DARK;
        int style;

        switch (palette(activity)) {
            case PALETTE_TERRACOTTA_NAVY:
                style = dark ? R.style.Theme_AdvanceClock_Palette0_Dark
                        : R.style.Theme_AdvanceClock_Palette0_Light;
                break;
            case PALETTE_MAGENTA_SKY:
                style = dark ? R.style.Theme_AdvanceClock_Palette1_Dark
                        : R.style.Theme_AdvanceClock_Palette1_Light;
                break;
            case PALETTE_MAGENTA_CHARCOAL:
                style = dark ? R.style.Theme_AdvanceClock_Palette2_Dark
                        : R.style.Theme_AdvanceClock_Palette2_Light;
                break;
            case PALETTE_TEAL_RED:
                style = dark ? R.style.Theme_AdvanceClock_Palette3_Dark
                        : R.style.Theme_AdvanceClock_Palette3_Light;
                break;
            case PALETTE_PURPLE_GOLD:
                style = dark ? R.style.Theme_AdvanceClock_Palette4_Dark
                        : R.style.Theme_AdvanceClock_Palette4_Light;
                break;
            case PALETTE_NEON_MAGENTA_GRAPHITE:
                style = dark ? R.style.Theme_AdvanceClock_Palette5_Dark
                        : R.style.Theme_AdvanceClock_Palette5_Light;
                break;
            case PALETTE_BLUE_CYAN:
            default:
                style = dark ? R.style.Theme_AdvanceClock_Palette6_Dark
                        : R.style.Theme_AdvanceClock_Palette6_Light;
                break;
        }

        activity.setTheme(style);
    }

    public static void applyModalOverlay(Activity activity) {
        activity.getTheme().applyStyle(R.style.OverlayAdvanceClockModal, true);
    }

    public static void applyFullscreenInsets(View root) {
        int start = root.getPaddingStart();
        int top = root.getPaddingTop();
        int end = root.getPaddingEnd();
        int bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPaddingRelative(
                    start,
                    top + insets.getSystemWindowInsetTop(),
                    end,
                    bottom + insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
    }

    public static void playFullscreenEnter(Activity activity) {
        activity.overridePendingTransition(R.anim.editor_enter, R.anim.editor_stay);
    }

    public static void playFullscreenExit(Activity activity) {
        activity.overridePendingTransition(R.anim.editor_stay, R.anim.editor_exit);
    }

    private static int clampPalette(int value) {
        return Math.max(0, Math.min(PALETTE_COLORS.length - 1, value));
    }
}
