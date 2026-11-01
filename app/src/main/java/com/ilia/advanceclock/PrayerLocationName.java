package com.ilia.advanceclock;

import android.net.Uri;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/** One foreground GPS lookup fallback; never polls or sends notes or other app data. */
final class PrayerLocationName {
    static String lookup(double latitude, double longitude, String language) {
        HttpURLConnection connection = null;
        try {
            String url = Uri.parse("https://nominatim.openstreetmap.org/reverse").buildUpon()
                    .appendQueryParameter("lat", Double.toString(latitude))
                    .appendQueryParameter("lon", Double.toString(longitude))
                    .appendQueryParameter("format", "jsonv2")
                    .appendQueryParameter("addressdetails", "1")
                    .appendQueryParameter("zoom", "10")
                    .appendQueryParameter("accept-language", language).build().toString();
            connection = ReadOnlyNetwork.location(url);
            connection.setConnectTimeout(6000);
            connection.setReadTimeout(6000);
            connection.setRequestProperty("User-Agent", "AdvanceClock/1.0 (Android; com.ilia.advanceclock)");
            if (connection.getResponseCode() != 200) return null;
            try (InputStream input = connection.getInputStream();
                 ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[2048];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    if (bytes.size() + count > 65536) return null;
                    bytes.write(buffer, 0, count);
                }
                return label(new JSONObject(bytes.toString(StandardCharsets.UTF_8.name())));
            }
        } catch (Exception ignored) { return null; }
        finally { if (connection != null) connection.disconnect(); }
    }

    static String label(JSONObject response) {
        JSONObject address = response.optJSONObject("address");
        if (address == null) return null;
        for (String key : new String[]{"city", "town", "village", "municipality", "county", "state", "region", "country"}) {
            String value = address.optString(key, "").trim();
            if (!value.isEmpty() && value.length() <= 200) return value;
        }
        return null;
    }
}
