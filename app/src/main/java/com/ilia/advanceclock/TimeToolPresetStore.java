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

    /** Sort a display copy; storage order stays stable for updates and legacy records. */
    static JSONArray ordered(String encoded, boolean stopwatch, long now, String runningId) {
        JSONArray stored = read(encoded);
        java.util.ArrayList<JSONObject> items = new java.util.ArrayList<>();
        java.util.Map<String, Integer> positions = new java.util.HashMap<>();
        for (int i = 0; i < stored.length(); i++) {
            JSONObject item = stored.optJSONObject(i);
            items.add(item);
            positions.put(item.optString("id"), i);
        }
        items.sort((a, b) -> {
            boolean aRunning = !runningId.isEmpty() && runningId.equals(a.optString("id"));
            boolean bRunning = !runningId.isEmpty() && runningId.equals(b.optString("id"));
            if (aRunning != bRunning) return aRunning ? -1 : 1;
            long first = next(a, stopwatch, now), second = next(b, stopwatch, now);
            boolean aReady = first > 0, bReady = second > 0;
            if (aReady != bReady) return aReady ? -1 : 1;
            if (aReady) {
                int deadline = Long.compare(first, second);
                if (deadline != 0) return deadline;
            }
            int saved = Long.compare(b.optLong("savedAt"), a.optLong("savedAt"));
            return saved != 0 ? saved : Integer.compare(positions.get(b.optString("id")),
                    positions.get(a.optString("id")));
        });
        JSONArray ordered = new JSONArray();
        for (JSONObject item : items) ordered.put(item);
        return ordered;
    }

    private static long next(JSONObject item, boolean stopwatch, long now) {
        boolean date = stopwatch ? item.optInt("mode") == 2 : item.optBoolean("dateMode");
        if (date) {
            long target = item.optLong("target");
            return target > now ? target : -1L;
        }
        if (stopwatch && item.optInt("mode") == 0) return Long.MAX_VALUE;
        long duration = item.optLong("duration");
        return duration > 0L ? now + duration : -1L;
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
