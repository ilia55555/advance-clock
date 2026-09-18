package com.ilia.advanceclock;

import org.json.JSONException;
import org.json.JSONObject;

public final class NoForgetItem {
    public static final int PRIORITY_LOW = 0;
    public static final int PRIORITY_NORMAL = 1;
    public static final int PRIORITY_HIGH = 2;

    public long id;
    public String title;
    public String body;
    public String sketchJson;
    public int priority;
    public boolean hasDue;
    public long dueAtMillis;
    public boolean reminderEnabled;
    public long createdAt;

    public NoForgetItem(long id, String title, String body, String sketchJson, int priority,
                        boolean hasDue, long dueAtMillis, boolean reminderEnabled, long createdAt) {
        this.id = id;
        this.title = title == null ? "" : title;
        this.body = body == null ? "" : body;
        this.sketchJson = sketchJson == null ? "[]" : sketchJson;
        this.priority = priority;
        this.hasDue = hasDue;
        this.dueAtMillis = dueAtMillis;
        this.reminderEnabled = reminderEnabled;
        this.createdAt = createdAt;
    }

    public int urgency(long now) {
        if (hasDue && dueAtMillis <= now) return 3;
        if (priority == PRIORITY_HIGH) return 3;
        if (hasDue && dueAtMillis - now <= 24L * 60L * 60L * 1000L) return 2;
        if (priority == PRIORITY_NORMAL) return 2;
        return 1;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("title", title);
        o.put("body", body);
        o.put("sketchJson", sketchJson);
        o.put("priority", priority);
        o.put("hasDue", hasDue);
        o.put("dueAtMillis", dueAtMillis);
        o.put("reminderEnabled", reminderEnabled);
        o.put("createdAt", createdAt);
        return o;
    }

    public static NoForgetItem fromJson(JSONObject o) {
        return new NoForgetItem(
                o.optLong("id", 0L),
                o.optString("title", ""),
                o.optString("body", ""),
                o.optString("sketchJson", "[]"),
                o.optInt("priority", PRIORITY_NORMAL),
                o.optBoolean("hasDue", false),
                o.optLong("dueAtMillis", 0L),
                o.optBoolean("reminderEnabled", false),
                o.optLong("createdAt", System.currentTimeMillis())
        );
    }
}
