package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WorldClockStore {
    private static final String PREFS = "world_clocks";
    private static final String KEY_ZONES = "zones";
    private static final String LABEL_PREFIX = "label::";

    private WorldClockStore() {}

    public static List<String> zones(Context context) {
        String saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ZONES, "");
        if (saved == null || saved.isEmpty()) {
            ArrayList<String> defaults = new ArrayList<>();
            for (String zone : Arrays.asList(
                    java.util.TimeZone.getDefault().getID(), "UTC", "Asia/Tehran")) {
                if (!defaults.contains(zone)) defaults.add(zone);
            }
            return defaults;
        }
        return new ArrayList<>(Arrays.asList(saved.split("\\|")));
    }

    public static void save(Context context, List<String> zones) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(KEY_ZONES, android.text.TextUtils.join("|", zones))
                .apply();
        WorldClockWidgetProvider.updateAll(context);
    }

    public static String label(Context context, String zone, String fallback) {
        if (zone == null || zone.isEmpty()) return fallback;
        String saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(LABEL_PREFIX + zone, "");
        return saved == null || saved.trim().isEmpty() ? fallback : saved.trim();
    }

    public static void saveLabel(Context context, String zone, String label) {
        if (zone == null || zone.isEmpty() || label == null || label.trim().isEmpty()) return;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(LABEL_PREFIX + zone, label.trim())
                .apply();
        WorldClockWidgetProvider.updateAll(context);
    }

    public static void removeLabel(Context context, String zone) {
        if (zone == null || zone.isEmpty()) return;
        SharedPreferences preferences =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        preferences.edit().remove(LABEL_PREFIX + zone).apply();
        WorldClockWidgetProvider.updateAll(context);
    }
}
