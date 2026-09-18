package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NoForgetStore {
    private static final String PREFS = "noforget_notes";
    private static final String KEY = "items_v1";
    private final SharedPreferences prefs;

    public NoForgetStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public synchronized List<NoForgetItem> all() {
        ArrayList<NoForgetItem> out = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs.getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o != null) out.add(NoForgetItem.fromJson(o));
            }
        } catch (Exception ignored) {}

        long now = System.currentTimeMillis();
        out.sort((a, b) -> {
            int urgency = Integer.compare(b.urgency(now), a.urgency(now));
            if (urgency != 0) return urgency;
            if (a.hasDue && b.hasDue) return Long.compare(a.dueAtMillis, b.dueAtMillis);
            if (a.hasDue != b.hasDue) return a.hasDue ? -1 : 1;
            return Long.compare(b.createdAt, a.createdAt);
        });
        return out;
    }

    public synchronized NoForgetItem find(long id) {
        for (NoForgetItem item : all()) if (item.id == id) return item;
        return null;
    }

    public synchronized void save(NoForgetItem item) {
        List<NoForgetItem> items = all();
        boolean replaced = false;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).id == item.id) {
                items.set(i, item);
                replaced = true;
                break;
            }
        }
        if (!replaced) items.add(item);
        write(items);
    }

    public synchronized void delete(long id) {
        List<NoForgetItem> items = all();
        items.removeIf(item -> item.id == id);
        write(items);
    }

    public synchronized List<NoForgetItem> top(int limit) {
        List<NoForgetItem> all = all();
        return new ArrayList<>(all.subList(0, Math.min(limit, all.size())));
    }

    private void write(List<NoForgetItem> items) {
        JSONArray array = new JSONArray();
        for (NoForgetItem item : items) {
            try { array.put(item.toJson()); } catch (Exception ignored) {}
        }
        prefs.edit().putString(KEY, array.toString()).apply();
    }
}
