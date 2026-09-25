package com.ilia.advanceclock;

import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class WorldClockStore {
    private static final String PREFS = "world_clocks";
    private static final String KEY_ZONES = "zones";

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
}
