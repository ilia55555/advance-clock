package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

public final class PrayerTimesWidgetPrefs {
    public static final int BG_NAVY = 0;
    public static final int BG_NAVY_TRANSPARENT = 1;
    public static final int BG_BLACK = 2;
    public static final int BG_LIGHT = 3;

    private static final String PREFS = "advance_clock_prayer_widget_prefs";

    private PrayerTimesWidgetPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String name, int widgetId) {
        return name + "_" + widgetId;
    }

    public static int background(Context context, int widgetId) {
        return Math.max(0, Math.min(3,
                prefs(context).getInt(key("background", widgetId), BG_NAVY)));
    }

    public static void setBackground(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(
                key("background", widgetId), Math.max(0, Math.min(3, value))).apply();
    }

    public static int mainTextColor(Context context, int widgetId) {
        int fallback = background(context, widgetId) == BG_LIGHT
                ? 0xFF173F5F : 0xFFF2F8FF;
        return prefs(context).getInt(key("main_text", widgetId), fallback);
    }

    public static void setMainTextColor(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(key("main_text", widgetId), value).apply();
    }

    public static int secondaryTextColor(Context context, int widgetId) {
        int fallback = background(context, widgetId) == BG_LIGHT
                ? 0xFF5B7187 : 0xFFA9C9E8;
        return prefs(context).getInt(key("secondary_text", widgetId), fallback);
    }

    public static void setSecondaryTextColor(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(key("secondary_text", widgetId), value).apply();
    }

    public static int accentColor(Context context, int widgetId) {
        return prefs(context).getInt(key("accent", widgetId), 0xFF56D8FF);
    }

    public static void setAccentColor(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(key("accent", widgetId), value).apply();
    }

    public static int activePrayerColor(Context context, int widgetId) {
        return prefs(context).getInt(key("active", widgetId), 0xFFFFC857);
    }

    public static void setActivePrayerColor(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(key("active", widgetId), value).apply();
    }

    public static int fontSize(Context context, int widgetId) {
        return Math.max(0, Math.min(2,
                prefs(context).getInt(key("font_size", widgetId), 1)));
    }

    public static void setFontSize(Context context, int widgetId, int value) {
        prefs(context).edit().putInt(
                key("font_size", widgetId), Math.max(0, Math.min(2, value))).apply();
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

    public static boolean showCountdown(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_countdown", widgetId), true);
    }

    public static void setShowCountdown(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_countdown", widgetId), value).apply();
    }

    public static boolean showIcons(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_icons", widgetId), true);
    }

    public static void setShowIcons(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_icons", widgetId), value).apply();
    }

    public static boolean showCurrentBadge(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_current_badge", widgetId), true);
    }

    public static void setShowCurrentBadge(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_current_badge", widgetId), value).apply();
    }

    public static boolean showManageButton(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_manage", widgetId), true);
    }

    public static void setShowManageButton(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_manage", widgetId), value).apply();
    }

    public static boolean showScrollHint(Context context, int widgetId) {
        return prefs(context).getBoolean(key("show_scroll_hint", widgetId), true);
    }

    public static void setShowScrollHint(Context context, int widgetId, boolean value) {
        prefs(context).edit().putBoolean(key("show_scroll_hint", widgetId), value).apply();
    }

    public static int rootBackgroundResource(Context context, int widgetId) {
        switch (background(context, widgetId)) {
            case BG_NAVY_TRANSPARENT: return R.drawable.widget_prayer_bg_navy_80;
            case BG_BLACK: return R.drawable.widget_prayer_bg_black;
            case BG_LIGHT: return R.drawable.widget_prayer_bg_light;
            case BG_NAVY:
            default: return R.drawable.widget_prayer_bg_navy;
        }
    }

    public static int cardBackgroundResource(Context context, int widgetId, boolean primary) {
        if (background(context, widgetId) == BG_LIGHT) {
            return primary
                    ? R.drawable.widget_prayer_card_primary_light
                    : R.drawable.widget_prayer_card_light;
        }
        return primary
                ? R.drawable.widget_prayer_card_primary
                : R.drawable.widget_prayer_card;
    }

    public static int nextBoxBackgroundResource(Context context, int widgetId) {
        return background(context, widgetId) == BG_LIGHT
                ? R.drawable.widget_prayer_next_box_light
                : R.drawable.widget_prayer_next_box;
    }

    public static void clear(Context context, int widgetId) {
        prefs(context).edit()
                .remove(key("background", widgetId))
                .remove(key("main_text", widgetId))
                .remove(key("secondary_text", widgetId))
                .remove(key("accent", widgetId))
                .remove(key("active", widgetId))
                .remove(key("font_size", widgetId))
                .remove(key("show_header", widgetId))
                .remove(key("show_date", widgetId))
                .remove(key("show_countdown", widgetId))
                .remove(key("show_icons", widgetId))
                .remove(key("show_current_badge", widgetId))
                .remove(key("show_manage", widgetId))
                .remove(key("show_scroll_hint", widgetId))
                .apply();
    }
}
