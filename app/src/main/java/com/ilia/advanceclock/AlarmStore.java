package com.ilia.advanceclock;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public final class AlarmStore {
    static final String ACTION_CHANGED = "com.ilia.advanceclock.ALARMS_CHANGED";
    private static final String PREFS = "advance_clock_alarms";
    private static final String KEY = "items_v1";
    private final Context context;

    public AlarmStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized List<AlarmItem> all() {
        ArrayList<AlarmItem> out = new ArrayList<>();
        String raw = SecurePreferences.read(context, PREFS, KEY, "[]");
        try {
            JSONArray array = new JSONArray(raw);
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.optJSONObject(i);
                if (o != null) out.add(AlarmItem.fromJson(o));
            }
        } catch (Exception ignored) {}
        ScheduleOrder.alarms(out, System.currentTimeMillis());
        return out;
    }

    public synchronized AlarmItem find(long id) {
        for (AlarmItem item : all()) if (item.id == id) return item;
        return null;
    }

    public synchronized void save(AlarmItem item) {
        List<AlarmItem> items = all();
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
        List<AlarmItem> items = all();
        items.removeIf(item -> item.id == id);
        write(items);
    }

    public synchronized List<AlarmItem> upcoming(int limit) {
        long now = System.currentTimeMillis();
        ArrayList<AlarmItem> out = new ArrayList<>();
        for (AlarmItem item : all()) {
            if (item.enabled && item.triggerAtMillis > now) out.add(item);
            if (out.size() >= limit) break;
        }
        return out;
    }

    public synchronized int deleteExpired(long now) {
        boolean deleteAllExpired = AppSettings.autoDeleteExpiredAlarms(context);
        List<AlarmItem> items = all();
        int before = items.size();
        items.removeIf(item -> !AlarmSoundService.isRinging(item.id)
                && !AlarmEditorActivity.isEditing(item.id)
                && (deleteAllExpired || (item.autoDeleteAfterCompletion && item.lastFiredAtMillis > 0L))
                && AlarmCompletion.finished(item, now));
        if (items.size() != before) {
            write(items);
            context.sendBroadcast(new android.content.Intent(ACTION_CHANGED).setPackage(context.getPackageName()));
        }
        return before - items.size();
    }

    private void write(List<AlarmItem> items) {
        JSONArray array = new JSONArray();
        for (AlarmItem item : items) {
            try { array.put(item.toJson()); } catch (Exception ignored) {}
        }
        SecurePreferences.write(context, PREFS, KEY, array.toString());
    }
}
