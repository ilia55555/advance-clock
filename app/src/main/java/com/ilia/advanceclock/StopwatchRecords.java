package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Personal stopwatch records grouped by activity (e.g. coding, cooking). */
final class StopwatchRecords {
    static final class Record {
        final long millis;
        final long at;
        final String title;
        Record(long millis, long at, String title) {
            this.millis = millis; this.at = at; this.title = title == null ? "" : title;
        }
    }

    static final class Activity {
        long id;
        String name;
        /** true: a longer time is the better record; false: a shorter time wins. */
        boolean longerBetter;
        final ArrayList<Record> records = new ArrayList<>();

        Record best() {
            Record best = null;
            for (Record r : records) {
                if (best == null || (longerBetter ? r.millis > best.millis : r.millis < best.millis)) best = r;
            }
            return best;
        }

        boolean beats(long millis, long other) {
            return longerBetter ? millis > other : millis < other;
        }
    }

    private static final String PREFS = "stopwatch_records";
    private static final String KEY = "activities_v1";
    private final SharedPreferences prefs;

    StopwatchRecords(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized List<Activity> all() {
        ArrayList<Activity> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                Activity a = new Activity();
                a.id = o.getLong("id");
                a.name = o.getString("name");
                a.longerBetter = o.optBoolean("longerBetter", false);
                JSONArray recs = o.optJSONArray("records");
                if (recs != null) for (int j = 0; j < recs.length(); j++) {
                    JSONObject r = recs.getJSONObject(j);
                    a.records.add(new Record(r.getLong("ms"), r.getLong("at"), r.optString("title", "")));
                }
                out.add(a);
            }
        } catch (Exception ignored) {}
        return out;
    }

    synchronized Activity find(String name) {
        if (name == null) return null;
        String key = name.trim();
        for (Activity a : all()) if (a.name.equalsIgnoreCase(key)) return a;
        return null;
    }

    synchronized void save(List<Activity> activities) {
        JSONArray array = new JSONArray();
        try {
            for (Activity a : activities) {
                JSONArray recs = new JSONArray();
                for (Record r : a.records) recs.put(new JSONObject().put("ms", r.millis).put("at", r.at).put("title", r.title));
                array.put(new JSONObject().put("id", a.id).put("name", a.name)
                        .put("longerBetter", a.longerBetter).put("records", recs));
            }
        } catch (Exception ignored) {}
        prefs.edit().putString(KEY, array.toString()).apply();
    }

    /** Adds a record and returns the previous best (null if this is the first). */
    synchronized Record add(String name, boolean longerBetterIfNew, long millis, String title) {
        List<Activity> all = all();
        Activity target = null;
        for (Activity a : all) if (a.name.equalsIgnoreCase(name.trim())) target = a;
        if (target == null) {
            target = new Activity();
            target.id = System.currentTimeMillis();
            target.name = name.trim();
            target.longerBetter = longerBetterIfNew;
            all.add(target);
        }
        Record previous = target.best();
        target.records.add(new Record(millis, System.currentTimeMillis(), title));
        save(all);
        return previous;
    }
}
