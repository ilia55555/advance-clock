package com.ilia.advanceclock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Serialization for reusable configurations, independent of the active timer state. */
final class TimeToolPresetStore {
    private TimeToolPresetStore() {}

    static JSONArray read(String encoded) {
        JSONArray valid = new JSONArray();
        try {
            JSONArray items = new JSONArray(encoded);
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null && !item.optString("id").isEmpty()) valid.put(item);
            }
        } catch (JSONException ignored) {}
        return valid;
    }

    static String save(String encoded, JSONObject config) throws JSONException {
        String id = config.getString("id");
        JSONArray items = read(encoded);
        for (int i = 0; i < items.length(); i++) {
            if (id.equals(items.getJSONObject(i).optString("id"))) {
                items.put(i, config);
                return items.toString();
            }
        }
        items.put(config);
        return items.toString();
    }

    static String delete(String encoded, String id) {
        JSONArray items = read(encoded);
        for (int i = items.length() - 1; i >= 0; i--)
            if (id.equals(items.optJSONObject(i).optString("id"))) items.remove(i);
        return items.toString();
    }
}
