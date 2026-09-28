package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.TimeZone;

/** Repairs locations saved by older builds that accidentally used the phone timezone. */
final class PrayerTimeZoneRepair {
    private static final String SETTINGS_PREFS = "advance_clock_settings";
    private static final String TIME_API = "https://timeapi.io/api/timezone/coordinate";
    private static final String OPEN_METEO = "https://api.open-meteo.com/v1/forecast";
    private static volatile boolean running;

    private PrayerTimeZoneRepair() {}

    static void repairIfNeeded(Context source) {
        Context context = source.getApplicationContext();
        if (!AppSettings.prayerLocationSet(context) || running) return;

        double longitude = AppSettings.prayerLongitude(context);
        String storedId = AppSettings.prayerTimeZoneId(context);
        TimeZone stored = TimeZone.getTimeZone(storedId);
        double zoneHours = stored.getOffset(System.currentTimeMillis()) / 3600000.0;
        double solarHours = longitude / 15.0;

        // Real timezone boundaries can differ from solar longitude, but a gap this large is a
        // strong sign of the historical phone-timezone fallback bug.
        if (circularHourDifference(zoneHours, solarHours) < 3.5) return;

        running = true;
        new Thread(() -> {
            try {
                double latitude = AppSettings.prayerLatitude(context);
                String label = AppSettings.prayerLocationLabel(context);
                String resolved = resolve(label, latitude, longitude);
                if (!isUsable(resolved) || resolved.equals(storedId)) return;

                AppSettings.setPrayerLocation(
                        context, latitude, longitude, label, resolved);
                repairSavedHorizon(context, latitude, longitude, resolved);
            } finally {
                running = false;
            }
        }, "PrayerTimeZoneRepair").start();
    }

    private static double circularHourDifference(double a, double b) {
        double d = Math.abs(a - b) % 24.0;
        return d > 12.0 ? 24.0 - d : d;
    }

    private static String resolve(String label, double latitude, double longitude) {
        // First repair completely offline when the stored label identifies a supported country.
        String zone = OfflineTimeZoneResolver.resolveFromLabel(label, latitude, longitude);
        if (isUsable(zone)) return zone;

        // Network is fallback only for locations that cannot be confidently identified offline.
        zone = fromTimeApi(latitude, longitude);
        if (isUsable(zone)) return zone;
        zone = fromOpenMeteo(latitude, longitude);
        return isUsable(zone) ? zone : "";
    }

    private static String fromTimeApi(double latitude, double longitude) {
        HttpURLConnection connection = null;
        try {
            Uri uri = Uri.parse(TIME_API).buildUpon()
                    .appendQueryParameter("latitude", Double.toString(latitude))
                    .appendQueryParameter("longitude", Double.toString(longitude))
                    .build();
            connection = open(uri.toString());
            if (!ok(connection)) return "";
            return new JSONObject(readFully(connection.getInputStream()))
                    .optString("timeZone", "").trim();
        } catch (Exception ignored) {
            return "";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static String fromOpenMeteo(double latitude, double longitude) {
        HttpURLConnection connection = null;
        try {
            Uri uri = Uri.parse(OPEN_METEO).buildUpon()
                    .appendQueryParameter("latitude", Double.toString(latitude))
                    .appendQueryParameter("longitude", Double.toString(longitude))
                    .appendQueryParameter("current", "temperature_2m")
                    .appendQueryParameter("timezone", "auto")
                    .appendQueryParameter("forecast_days", "1")
                    .build();
            connection = open(uri.toString());
            if (!ok(connection)) return "";
            return new JSONObject(readFully(connection.getInputStream()))
                    .optString("timezone", "").trim();
        } catch (Exception ignored) {
            return "";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static HttpURLConnection open(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(8_000);
        connection.setReadTimeout(8_000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty(
                "User-Agent", "AdvanceClock/1.0 (Android; com.ilia.advanceclock)");
        return connection;
    }

    private static boolean ok(HttpURLConnection connection) throws Exception {
        int code = connection.getResponseCode();
        return code >= 200 && code < 300;
    }

    private static boolean isUsable(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        String normalized = id.trim();
        TimeZone zone = TimeZone.getTimeZone(normalized);
        if (!"GMT".equals(zone.getID())) return true;
        String upper = normalized.toUpperCase(Locale.US);
        return "GMT".equals(upper)
                || upper.startsWith("GMT+")
                || upper.startsWith("GMT-")
                || normalized.startsWith("Etc/GMT");
    }

    private static String readFully(InputStream stream) throws Exception {
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }

    private static void repairSavedHorizon(
            Context context, double latitude, double longitude, String zone) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    SETTINGS_PREFS, Context.MODE_PRIVATE);
            JSONArray source = new JSONArray(prefs.getString("prayer_horizons", "[]"));
            JSONArray updated = new JSONArray();
            boolean changed = false;
            for (int i = 0; i < source.length(); i++) {
                JSONObject item = source.optJSONObject(i);
                if (item == null) continue;
                if (Math.abs(item.optDouble("lat") - latitude) < 0.0001
                        && Math.abs(item.optDouble("lon") - longitude) < 0.0001) {
                    item.put("zone", zone);
                    changed = true;
                }
                updated.put(item);
            }
            if (changed) prefs.edit()
                    .putString("prayer_horizons", updated.toString())
                    .apply();
        } catch (Exception ignored) {}
    }
}
