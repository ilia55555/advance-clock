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

    public static final int ADHAN_SKIP_NONE = 0;
    public static final int ADHAN_SKIP_WEEKDAYS = 1;
    public static final int ADHAN_SKIP_DATES = 2;
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

    public static boolean showCalendarEvents(Context context) {
        return prefs(context).getBoolean("show_calendar_events", true);
    }

    public static void setShowCalendarEvents(Context context, boolean value) {
        prefs(context).edit().putBoolean("show_calendar_events", value).apply();
    }

    public static boolean persianCalendarEventsEnabled(Context context) {
        return prefs(context).getBoolean("events_source_persian", true);
    }

    public static void setPersianCalendarEventsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("events_source_persian", value).apply();
    }

    public static boolean hijriCalendarEventsEnabled(Context context) {
        return prefs(context).getBoolean("events_source_hijri", true);
    }

    public static void setHijriCalendarEventsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("events_source_hijri", value).apply();
    }

    public static boolean gregorianCalendarEventsEnabled(Context context) {
        return prefs(context).getBoolean("events_source_gregorian", true);
    }

    public static void setGregorianCalendarEventsEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("events_source_gregorian", value).apply();
    }

    public static boolean additionalCalendarEventsEnabled(Context context, int calendarType) {
        return prefs(context).getBoolean("additional_events_" + calendarType, false);
    }

    public static void setAdditionalCalendarEventsEnabled(
            Context context, int calendarType, boolean value) {
        prefs(context).edit().putBoolean("additional_events_" + calendarType, value).apply();
    }

    public static boolean adhanEnabled(Context context) {
        return prefs(context).getBoolean("adhan_enabled", false);
    }

    public static void setAdhanEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("adhan_enabled", value).apply();
        AdhanScheduler.rescheduleAll(context);
    }

    public static boolean prayerLocationSet(Context context) {
        return prefs(context).getBoolean("prayer_location_set", false);
    }

    public static double prayerLatitude(Context context) {
        try {
            return Double.parseDouble(
                    prefs(context).getString("prayer_latitude", "0"));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    public static double prayerLongitude(Context context) {
        try {
            return Double.parseDouble(
                    prefs(context).getString("prayer_longitude", "0"));
        } catch (Exception ignored) {
            return 0.0;
        }
    }

    public static String prayerLocationLabel(Context context) {
        String stored = prefs(context).getString(
                "prayer_location_label", AppString.get(R.string.runtime_text_0371));
        return IranOfflineLocations.localizedLabel(
                prayerLatitude(context), prayerLongitude(context), stored);
    }

    public static String prayerTimeZoneId(Context context) {
        return prefs(context).getString(
                "prayer_time_zone", java.util.TimeZone.getDefault().getID());
    }

    public static java.util.TimeZone prayerTimeZone(Context context) {
        return java.util.TimeZone.getTimeZone(prayerTimeZoneId(context));
    }

    public static void setPrayerLocation(
            Context context, double latitude, double longitude, String label) {
        setPrayerLocation(
                context,
                latitude,
                longitude,
                label,
                java.util.TimeZone.getDefault().getID());
    }

    public static void setPrayerLocation(
            Context context,
            double latitude,
            double longitude,
            String label,
            String timeZoneId) {
        prefs(context).edit()
                .putBoolean("prayer_location_set", true)
                .putString("prayer_latitude", Double.toString(latitude))
                .putString("prayer_longitude", Double.toString(longitude))
                .putString("prayer_location_label",
                        label == null || label.trim().isEmpty()
                                ? AppString.get(R.string.runtime_text_0371) : label.trim())
                .putString("prayer_time_zone",
                        timeZoneId == null || timeZoneId.trim().isEmpty()
                                ? java.util.TimeZone.getDefault().getID()
                                : timeZoneId.trim())
                .apply();
        addPrayerHorizon(context, label, latitude, longitude, timeZoneId);
        AdhanScheduler.rescheduleAll(context);
    }

    public static final class PrayerHorizon {
        public final String label;
        public final double latitude;
        public final double longitude;
        public final String timeZoneId;
        PrayerHorizon(String label, double latitude, double longitude, String timeZoneId) {
            this.label = label; this.latitude = latitude; this.longitude = longitude;
            this.timeZoneId = timeZoneId;
        }
    }

    public static java.util.List<PrayerHorizon> prayerHorizons(Context context) {
        java.util.ArrayList<PrayerHorizon> values = new java.util.ArrayList<>();
        try {
            org.json.JSONArray array = new org.json.JSONArray(
                    prefs(context).getString("prayer_horizons", "[]"));
            for (int i = 0; i < array.length(); i++) {
                org.json.JSONObject item = array.optJSONObject(i);
                if (item != null) {
                    double latitude = item.optDouble("lat");
                    double longitude = item.optDouble("lon");
                    String storedLabel = item.optString(
                            "label", AppString.get(R.string.runtime_text_0418));
                    String localizedLabel = IranOfflineLocations.localizedLabel(
                            latitude, longitude, storedLabel);
                    values.add(new PrayerHorizon(
                            localizedLabel,
                            latitude,
                            longitude,
                            item.optString("zone", "Asia/Tehran")));
                }
            }
        } catch (Exception ignored) {}
        if (values.isEmpty() && prayerLocationSet(context)) {
            values.add(new PrayerHorizon(
                    prayerLocationLabel(context), prayerLatitude(context),
                    prayerLongitude(context), prayerTimeZoneId(context)));
        }
        return values;
    }

    public static boolean isPrimaryPrayerHorizon(
            Context context, double latitude, double longitude) {
        return prayerLocationSet(context)
                && Math.abs(prayerLatitude(context) - latitude) < 0.0001
                && Math.abs(prayerLongitude(context) - longitude) < 0.0001;
    }

    public static void setPrimaryPrayerHorizon(
            Context context,
            double latitude,
            double longitude,
            String label,
            String timeZoneId) {
        setPrayerLocation(context, latitude, longitude, label, timeZoneId);

        java.util.List<PrayerHorizon> current = prayerHorizons(context);
        java.util.ArrayList<PrayerHorizon> ordered = new java.util.ArrayList<>();
        PrayerHorizon selected = null;

        for (PrayerHorizon item : current) {
            if (Math.abs(item.latitude - latitude) < 0.0001
                    && Math.abs(item.longitude - longitude) < 0.0001) {
                selected = item;
                break;
            }
        }
        if (selected == null) {
            selected = new PrayerHorizon(label, latitude, longitude, timeZoneId);
        }
        ordered.add(selected);

        for (PrayerHorizon item : current) {
            if (Math.abs(item.latitude - latitude) >= 0.0001
                    || Math.abs(item.longitude - longitude) >= 0.0001) {
                ordered.add(item);
            }
        }

        org.json.JSONArray array = new org.json.JSONArray();
        for (PrayerHorizon item : ordered) {
            org.json.JSONObject value = new org.json.JSONObject();
            try {
                value.put("label", item.label);
                value.put("lat", item.latitude);
                value.put("lon", item.longitude);
                value.put("zone", item.timeZoneId);
                array.put(value);
            } catch (Exception ignored) {}
        }
        prefs(context).edit().putString("prayer_horizons", array.toString()).apply();
        PrayerTimesWidgetProvider.updateAll(context);
    }

    public static void addPrayerHorizon(
            Context context, String label, double latitude, double longitude, String timeZoneId) {
        java.util.List<PrayerHorizon> current = prayerHorizons(context);
        for (PrayerHorizon item : current)
            if (Math.abs(item.latitude - latitude) < 0.0001
                    && Math.abs(item.longitude - longitude) < 0.0001) {
                PrayerTimesWidgetProvider.updateAll(context);
                return;
            }
        org.json.JSONArray array = new org.json.JSONArray();
        for (PrayerHorizon item : current) {
            org.json.JSONObject value = new org.json.JSONObject();
            try { value.put("label", item.label); value.put("lat", item.latitude);
                value.put("lon", item.longitude); value.put("zone", item.timeZoneId); array.put(value); }
            catch (Exception ignored) {}
        }
        org.json.JSONObject added = new org.json.JSONObject();
        try { added.put("label", label); added.put("lat", latitude); added.put("lon", longitude);
            added.put("zone", timeZoneId); array.put(added); } catch (Exception ignored) {}
        prefs(context).edit().putString("prayer_horizons", array.toString()).apply();
        PrayerTimesWidgetProvider.updateAll(context);
    }

    public static boolean hasPrayerHorizon(Context context, double latitude, double longitude) {
        for (PrayerHorizon item : prayerHorizons(context)) {
            if (Math.abs(item.latitude - latitude) < 0.0001
                    && Math.abs(item.longitude - longitude) < 0.0001) return true;
        }
        return false;
    }

    public static void removePrayerHorizon(Context context, double latitude, double longitude) {
        java.util.ArrayList<PrayerHorizon> kept = new java.util.ArrayList<>();
        for (PrayerHorizon item : prayerHorizons(context)) {
            if (Math.abs(item.latitude - latitude) >= 0.0001
                    || Math.abs(item.longitude - longitude) >= 0.0001) kept.add(item);
        }
        org.json.JSONArray array = new org.json.JSONArray();
        for (PrayerHorizon item : kept) {
            org.json.JSONObject value = new org.json.JSONObject();
            try { value.put("label", item.label); value.put("lat", item.latitude);
                value.put("lon", item.longitude); value.put("zone", item.timeZoneId);
                array.put(value); } catch (Exception ignored) {}
        }
        android.content.SharedPreferences.Editor editor = prefs(context).edit()
                .putString("prayer_horizons", array.toString());
        boolean removedPrimary = Math.abs(prayerLatitude(context) - latitude) < 0.0001
                && Math.abs(prayerLongitude(context) - longitude) < 0.0001;
        if (removedPrimary && kept.isEmpty()) editor.putBoolean("prayer_location_set", false);
        editor.apply();
        if (removedPrimary && !kept.isEmpty()) {
            PrayerHorizon first = kept.get(0);
            setPrayerLocation(context, first.latitude, first.longitude, first.label, first.timeZoneId);
        }
        PrayerTimesWidgetProvider.updateAll(context);
    }

    public static boolean fajrAdhanEnabled(Context context) {
        return prefs(context).getBoolean("adhan_fajr_enabled", true);
    }

    public static void setFajrAdhanEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("adhan_fajr_enabled", value).apply();
        AdhanScheduler.rescheduleAll(context);
    }

    public static boolean dhuhrAdhanEnabled(Context context) {
        return prefs(context).getBoolean("adhan_dhuhr_enabled", true);
    }

    public static void setDhuhrAdhanEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("adhan_dhuhr_enabled", value).apply();
        AdhanScheduler.rescheduleAll(context);
    }

    public static boolean maghribAdhanEnabled(Context context) {
        return prefs(context).getBoolean("adhan_maghrib_enabled", true);
    }

    public static void setMaghribAdhanEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("adhan_maghrib_enabled", value).apply();
        AdhanScheduler.rescheduleAll(context);
    }

    public static boolean asrAdhanEnabled(Context context) {
        return prefs(context).getBoolean("adhan_asr_enabled", false);
    }

    public static void setAsrAdhanEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("adhan_asr_enabled", value).apply();
        AdhanScheduler.rescheduleAll(context);
    }

    public static boolean ishaAdhanEnabled(Context context) {
        return prefs(context).getBoolean("adhan_isha_enabled", false);
    }

    public static void setIshaAdhanEnabled(Context context, boolean value) {
        prefs(context).edit().putBoolean("adhan_isha_enabled", value).apply();
        AdhanScheduler.rescheduleAll(context);
    }

    private static String adhanSkipKey(String base, int type) {
        return base + "_" + Math.max(AdhanScheduler.FAJR, Math.min(AdhanScheduler.ISHA, type));
    }

    private static int normalizedAdhanType(int type) {
        return type == 899
                ? AdhanScheduler.FAJR
                : Math.max(AdhanScheduler.FAJR, Math.min(AdhanScheduler.ISHA, type));
    }

    public static int adhanSkipMode(Context context, int type) {
        if (type == 899) {
            int common = commonAdhanSkipMode(context);
            return common >= 0 ? common : adhanSkipMode(context, AdhanScheduler.FAJR);
        }
        int normalized = normalizedAdhanType(type);
        String key = adhanSkipKey("adhan_skip_mode", normalized);
        if (prefs(context).contains(key)) {
            int value = prefs(context).getInt(key, ADHAN_SKIP_NONE);
            return Math.max(ADHAN_SKIP_NONE, Math.min(ADHAN_SKIP_DATES, value));
        }
        int legacy = prefs(context).getInt("adhan_skip_mode", ADHAN_SKIP_NONE);
        return Math.max(ADHAN_SKIP_NONE, Math.min(ADHAN_SKIP_DATES, legacy));
    }

    public static int commonAdhanSkipMode(Context context) {
        int first = adhanSkipMode(context, AdhanScheduler.FAJR);
        for (int item = AdhanScheduler.DHUHR; item <= AdhanScheduler.ISHA; item++) {
            if (adhanSkipMode(context, item) != first) return -1;
        }
        return first;
    }

    public static boolean allAdhanSkipModesMatch(Context context) {
        return commonAdhanSkipMode(context) >= 0;
    }

    public static void setAdhanSkipMode(Context context, int type, int value) {
        int safe = Math.max(ADHAN_SKIP_NONE, Math.min(ADHAN_SKIP_DATES, value));
        android.content.SharedPreferences.Editor editor = prefs(context).edit();
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                editor.putInt(adhanSkipKey("adhan_skip_mode", item), safe);
            }
        } else {
            editor.putInt(adhanSkipKey("adhan_skip_mode", normalizedAdhanType(type)), safe);
        }
        editor.apply();
    }

    public static int adhanSkipWeekdayMask(Context context, int type) {
        int normalized = normalizedAdhanType(type);
        String key = adhanSkipKey("adhan_skip_weekday_mask", normalized);
        if (prefs(context).contains(key)) {
            return prefs(context).getInt(key, 0);
        }
        return prefs(context).getInt("adhan_skip_weekday_mask", 0);
    }

    public static boolean adhanSkipWeekday(
            Context context, int type, int calendarDayOfWeek) {
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                if (!adhanSkipWeekday(context, item, calendarDayOfWeek)) return false;
            }
            return true;
        }
        int bit = 1 << Math.max(1, Math.min(7, calendarDayOfWeek));
        return (adhanSkipWeekdayMask(context, type) & bit) != 0;
    }

    public static boolean allAdhanSkipWeekdaysMatch(Context context) {
        int first = adhanSkipWeekdayMask(context, AdhanScheduler.FAJR);
        for (int item = AdhanScheduler.DHUHR; item <= AdhanScheduler.ISHA; item++) {
            if (adhanSkipWeekdayMask(context, item) != first) return false;
        }
        return true;
    }

    public static void setAdhanSkipWeekday(
            Context context, int type, int calendarDayOfWeek, boolean skip) {
        android.content.SharedPreferences.Editor editor = prefs(context).edit();
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                int bit = 1 << Math.max(1, Math.min(7, calendarDayOfWeek));
                int mask = adhanSkipWeekdayMask(context, item);
                mask = skip ? (mask | bit) : (mask & ~bit);
                editor.putInt(adhanSkipKey("adhan_skip_weekday_mask", item), mask);
            }
        } else {
            int normalized = normalizedAdhanType(type);
            int bit = 1 << Math.max(1, Math.min(7, calendarDayOfWeek));
            int mask = adhanSkipWeekdayMask(context, normalized);
            mask = skip ? (mask | bit) : (mask & ~bit);
            editor.putInt(adhanSkipKey("adhan_skip_weekday_mask", normalized), mask);
        }
        editor.apply();
    }

    public static java.util.Set<String> adhanSkipDates(Context context, int type) {
        if (type == 899) return commonAdhanSkipDates(context);

        int normalized = normalizedAdhanType(type);
        String key = adhanSkipKey("adhan_skip_dates", normalized);
        if (prefs(context).contains(key)) {
            java.util.Set<String> stored = prefs(context).getStringSet(
                    key, java.util.Collections.emptySet());
            return new java.util.HashSet<>(stored);
        }
        java.util.Set<String> legacy = prefs(context).getStringSet(
                "adhan_skip_dates", java.util.Collections.emptySet());
        return new java.util.HashSet<>(legacy);
    }

    public static java.util.Set<String> commonAdhanSkipDates(Context context) {
        java.util.Set<String> common = new java.util.HashSet<>(
                adhanSkipDates(context, AdhanScheduler.FAJR));
        for (int item = AdhanScheduler.DHUHR; item <= AdhanScheduler.ISHA; item++) {
            common.retainAll(adhanSkipDates(context, item));
        }
        return common;
    }

    public static boolean allAdhanSkipDatesMatch(Context context) {
        java.util.Set<String> first = adhanSkipDates(context, AdhanScheduler.FAJR);
        for (int item = AdhanScheduler.DHUHR; item <= AdhanScheduler.ISHA; item++) {
            if (!first.equals(adhanSkipDates(context, item))) return false;
        }
        return true;
    }

    public static void addAdhanSkipDate(Context context, int type, long millis) {
        String value = adhanSkipDateKey(context, millis);
        android.content.SharedPreferences.Editor editor = prefs(context).edit();
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                java.util.Set<String> values = adhanSkipDates(context, item);
                values.add(value);
                editor.putStringSet(adhanSkipKey("adhan_skip_dates", item), values);
            }
        } else {
            int normalized = normalizedAdhanType(type);
            java.util.Set<String> values = adhanSkipDates(context, normalized);
            values.add(value);
            editor.putStringSet(adhanSkipKey("adhan_skip_dates", normalized), values);
        }
        editor.apply();
    }

    public static void removeAdhanSkipDate(Context context, int type, String keyValue) {
        android.content.SharedPreferences.Editor editor = prefs(context).edit();
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                java.util.Set<String> values = adhanSkipDates(context, item);
                values.remove(keyValue);
                editor.putStringSet(adhanSkipKey("adhan_skip_dates", item), values);
            }
        } else {
            int normalized = normalizedAdhanType(type);
            java.util.Set<String> values = adhanSkipDates(context, normalized);
            values.remove(keyValue);
            editor.putStringSet(adhanSkipKey("adhan_skip_dates", normalized), values);
        }
        editor.apply();
    }

    public static String adhanSkipDateKey(Context context, long millis) {
        java.util.Calendar calendar = java.util.Calendar.getInstance(prayerTimeZone(context));
        calendar.setTimeInMillis(millis);
        return String.format(
                java.util.Locale.US,
                "%04d-%02d-%02d",
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH) + 1,
                calendar.get(java.util.Calendar.DAY_OF_MONTH));
    }

    public static long adhanSkipDateMillis(Context context, String key) {
        try {
            String[] parts = key.split("-");
            if (parts.length != 3) return 0L;
            java.util.Calendar calendar = java.util.Calendar.getInstance(prayerTimeZone(context));
            calendar.clear();
            calendar.set(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]) - 1,
                    Integer.parseInt(parts[2]),
                    12,
                    0,
                    0);
            return calendar.getTimeInMillis();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    public static boolean isAdhanSuppressedNow(Context context, int type) {
        int mode = adhanSkipMode(context, type);
        if (mode == ADHAN_SKIP_NONE) return false;

        java.util.Calendar now = java.util.Calendar.getInstance(prayerTimeZone(context));
        if (mode == ADHAN_SKIP_WEEKDAYS) {
            return adhanSkipWeekday(
                    context, type, now.get(java.util.Calendar.DAY_OF_WEEK));
        }

        return adhanSkipDates(context, type).contains(
                adhanSkipDateKey(context, now.getTimeInMillis()));
    }

    private static boolean adhanBooleanForType(
            Context context, String baseKey, int type, boolean defaultValue) {
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                if (!adhanBooleanForType(context, baseKey, item, defaultValue)) return false;
            }
            return true;
        }
        int normalized = normalizedAdhanType(type);
        String key = baseKey + "_" + normalized;
        android.content.SharedPreferences values = prefs(context);
        if (values.contains(key)) return values.getBoolean(key, defaultValue);
        return values.getBoolean(baseKey, defaultValue);
    }

    private static void setAdhanBooleanForType(
            Context context, String baseKey, int type, boolean value) {
        android.content.SharedPreferences.Editor editor = prefs(context).edit();
        if (type == 899) {
            editor.putBoolean(baseKey, value);
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                editor.putBoolean(baseKey + "_" + item, value);
            }
        } else {
            editor.putBoolean(baseKey + "_" + normalizedAdhanType(type), value);
        }
        editor.apply();
    }

    public static boolean adhanFullscreenUnlocked(Context context) {
        return adhanFullscreenUnlocked(context, 899);
    }
    public static boolean adhanFullscreenUnlocked(Context context, int type) {
        return adhanBooleanForType(
                context, "adhan_fullscreen_unlocked", type, true);
    }
    public static void setAdhanFullscreenUnlocked(Context context, boolean value) {
        setAdhanFullscreenUnlocked(context, 899, value);
    }
    public static void setAdhanFullscreenUnlocked(
            Context context, int type, boolean value) {
        setAdhanBooleanForType(
                context, "adhan_fullscreen_unlocked", type, value);
    }

    public static boolean adhanFullscreenLocked(Context context) {
        return adhanFullscreenLocked(context, 899);
    }
    public static boolean adhanFullscreenLocked(Context context, int type) {
        return adhanBooleanForType(
                context, "adhan_fullscreen_locked", type, true);
    }
    public static void setAdhanFullscreenLocked(Context context, boolean value) {
        setAdhanFullscreenLocked(context, 899, value);
    }
    public static void setAdhanFullscreenLocked(
            Context context, int type, boolean value) {
        setAdhanBooleanForType(
                context, "adhan_fullscreen_locked", type, value);
    }

    public static boolean alarmFullscreenUnlocked(Context context) {
        return prefs(context).getBoolean("alarm_fullscreen_unlocked", true);
    }
    public static void setAlarmFullscreenUnlocked(Context context, boolean value) {
        prefs(context).edit().putBoolean("alarm_fullscreen_unlocked", value).apply();
    }
    public static boolean alarmFullscreenLocked(Context context) {
        return prefs(context).getBoolean("alarm_fullscreen_locked", true);
    }
    public static void setAlarmFullscreenLocked(Context context, boolean value) {
        prefs(context).edit().putBoolean("alarm_fullscreen_locked", value).apply();
    }

    public static boolean adhanFullscreen(Context context) {
        return prefs(context).getBoolean("adhan_fullscreen", true);
    }
    public static void setAdhanFullscreen(Context context, boolean value) {
        prefs(context).edit().putBoolean("adhan_fullscreen", value).apply();
    }

    public static boolean adhanNotification(Context context) {
        return adhanNotification(context, 899);
    }
    public static boolean adhanNotification(Context context, int type) {
        return adhanBooleanForType(
                context, "adhan_notification", type, true);
    }
    public static void setAdhanNotification(Context context, boolean value) {
        setAdhanNotification(context, 899, value);
    }
    public static void setAdhanNotification(
            Context context, int type, boolean value) {
        setAdhanBooleanForType(
                context, "adhan_notification", type, value);
    }

    public static boolean adhanSound(Context context) {
        return adhanSound(context, 899);
    }
    public static boolean adhanSound(Context context, int type) {
        return adhanBooleanForType(
                context, "adhan_sound", type, true);
    }
    public static void setAdhanSound(Context context, boolean value) {
        setAdhanSound(context, 899, value);
    }
    public static void setAdhanSound(
            Context context, int type, boolean value) {
        setAdhanBooleanForType(
                context, "adhan_sound", type, value);
    }

    public static String adhanSoundUri(Context context, int type) {
        String key = "adhan_sound_uri_" + type;
        if (prefs(context).contains(key)) {
            return prefs(context).getString(key, "");
        }
        int resource = context.getResources().getIdentifier(
                "rawadhan_2", "raw", context.getPackageName());
        return resource == 0 ? "" : "android.resource://"
                + context.getPackageName() + "/raw/rawadhan_2";
    }
    public static void setAdhanSoundUri(Context context, int type, String uri) {
        prefs(context).edit().putString(
                "adhan_sound_uri_" + type, uri == null ? "" : uri).apply();
    }

    public static String commonAdhanSoundUri(Context context) {
        String first = adhanSoundUri(context, AdhanScheduler.FAJR);
        for (int item = AdhanScheduler.DHUHR; item <= AdhanScheduler.ISHA; item++) {
            if (!first.equals(adhanSoundUri(context, item))) return null;
        }
        return first;
    }

    public static boolean allAdhanSoundsMatch(Context context) {
        return commonAdhanSoundUri(context) != null;
    }

    public static int adhanVolume(Context context, int type) {
        if (type == 899) {
            int common = commonAdhanVolume(context);
            return common >= 0
                    ? common
                    : adhanVolume(context, AdhanScheduler.FAJR);
        }
        return Math.max(0, Math.min(100,
                prefs(context).getInt("adhan_volume_" + type, 100)));
    }

    public static int commonAdhanVolume(Context context) {
        int first = adhanVolume(context, AdhanScheduler.FAJR);
        for (int item = AdhanScheduler.DHUHR; item <= AdhanScheduler.ISHA; item++) {
            if (adhanVolume(context, item) != first) return -1;
        }
        return first;
    }

    public static boolean allAdhanVolumesMatch(Context context) {
        return commonAdhanVolume(context) >= 0;
    }

    public static void setAdhanVolume(Context context, int type, int value) {
        int safe = Math.max(0, Math.min(100, value));
        android.content.SharedPreferences.Editor editor = prefs(context).edit();
        if (type == 899) {
            for (int item = AdhanScheduler.FAJR; item <= AdhanScheduler.ISHA; item++) {
                editor.putInt("adhan_volume_" + item, safe);
            }
        } else {
            editor.putInt("adhan_volume_" + type, safe);
        }
        editor.apply();
    }

    public static String defaultAlarmSoundUri(Context context) {
        return prefs(context).getString("default_alarm_sound_uri", "");
    }
    public static void setDefaultAlarmSoundUri(Context context, String uri) {
        prefs(context).edit().putString(
                "default_alarm_sound_uri", uri == null ? "" : uri).apply();
    }

    public static boolean adhanVibrate(Context context) {
        return adhanVibrate(context, 899);
    }
    public static boolean adhanVibrate(Context context, int type) {
        return adhanBooleanForType(
                context, "adhan_vibrate", type, true);
    }
    public static void setAdhanVibrate(Context context, boolean value) {
        setAdhanVibrate(context, 899, value);
    }
    public static void setAdhanVibrate(
            Context context, int type, boolean value) {
        setAdhanBooleanForType(
                context, "adhan_vibrate", type, value);
    }

    public static String language(Context context) {
        String saved = prefs(context).getString("app_language", null);
        return isSupportedLanguage(saved) ? saved : languageForDevice();
    }

    public static void setLanguage(Context context, String value) {
        if (!isSupportedLanguage(value)) value = LANGUAGE_ENGLISH;
        // Locale recreation reads this value immediately; persist synchronously so a newly
        // created Activity can never observe the previous language.
        prefs(context).edit().putString("app_language", value).commit();
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

    public static boolean isRtlLanguage(Context context) {
        String code = Locale.forLanguageTag(language(context)).getLanguage();
        return "fa".equals(code) || "ar".equals(code);
    }

    public static int layoutDirection(Context context) {
        return isRtlLanguage(context)
                ? View.LAYOUT_DIRECTION_RTL
                : View.LAYOUT_DIRECTION_LTR;
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
        return prefs(context).getInt("clock_layout_mode", CLOCK_LAYOUT_CALENDAR_FIRST);
    }

    public static void setClockLayoutMode(Context context, int value) {
        prefs(context).edit().putInt("clock_layout_mode", value).apply();
    }

    public static boolean autoDeleteExpiredAlarms(Context context) {
        return prefs(context).getBoolean("auto_delete_expired_alarms", false);
    }

    public static void setAutoDeleteExpiredAlarms(Context context, boolean value) {
        prefs(context).edit().putBoolean("auto_delete_expired_alarms", value).apply();
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
        activity.overridePendingTransition(R.anim.editor_stay, R.anim.settings_exit);
    }

    private static int clampPalette(int value) {
        return Math.max(0, Math.min(PALETTE_COLORS.length - 1, value));
    }
}
