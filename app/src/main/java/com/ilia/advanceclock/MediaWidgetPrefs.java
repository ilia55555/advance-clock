package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class MediaWidgetPrefs {
    private static final String PREFS = "advance_clock_media_widget_prefs";
    public static final int MAX_ITEMS = 20;

    public static final class Item {
        public final String uri;
        public final String name;
        public final String mime;

        public Item(String uri, String name, String mime) {
            this.uri = uri == null ? "" : uri;
            this.name = name == null || name.trim().isEmpty() ? "فایل" : name;
            this.mime = mime == null || mime.trim().isEmpty()
                    ? "application/octet-stream" : mime;
        }
    }

    private MediaWidgetPrefs() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(int widgetId) {
        return "items_" + widgetId;
    }

    public static List<Item> load(Context context, int widgetId) {
        ArrayList<Item> out = new ArrayList<>();
        String raw = prefs(context).getString(key(widgetId), "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length() && out.size() < MAX_ITEMS; i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String uri = object.optString("uri", "");
                if (uri.isEmpty()) continue;
                out.add(new Item(
                        uri,
                        object.optString("name", "فایل"),
                        object.optString("mime", "application/octet-stream")));
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public static void save(Context context, int widgetId, List<Item> items) {
        JSONArray array = new JSONArray();
        if (items != null) {
            for (int i = 0; i < items.size() && i < MAX_ITEMS; i++) {
                Item item = items.get(i);
                if (item == null || item.uri.isEmpty()) continue;
                try {
                    JSONObject object = new JSONObject();
                    object.put("uri", item.uri);
                    object.put("name", item.name);
                    object.put("mime", item.mime);
                    array.put(object);
                } catch (Exception ignored) {
                }
            }
        }
        prefs(context).edit().putString(key(widgetId), array.toString()).apply();
    }

    public static void clear(Context context, int widgetId) {
        prefs(context).edit().remove(key(widgetId)).apply();
    }
}
