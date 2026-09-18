package com.ilia.advanceclock;

import org.json.JSONException;
import org.json.JSONObject;

public final class AlarmItem {
    public static final int REPEAT_NONE = 0;
    public static final int REPEAT_DAILY = 1;
    public static final int REPEAT_WEEKLY = 2;
    public static final int REPEAT_MONTHLY = 3;
    public static final int REPEAT_YEARLY = 4;

    public long id;
    public String label;
    public long triggerAtMillis;
    public int repeatType;
    public boolean enabled;

    public AlarmItem(long id, String label, long triggerAtMillis, int repeatType, boolean enabled) {
        this.id = id;
        this.label = label == null ? "" : label;
        this.triggerAtMillis = triggerAtMillis;
        this.repeatType = repeatType;
        this.enabled = enabled;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject o = new JSONObject();
        o.put("id", id);
        o.put("label", label);
        o.put("triggerAtMillis", triggerAtMillis);
        o.put("repeatType", repeatType);
        o.put("enabled", enabled);
        return o;
    }

    public static AlarmItem fromJson(JSONObject o) {
        return new AlarmItem(
                o.optLong("id", 0L),
                o.optString("label", ""),
                o.optLong("triggerAtMillis", 0L),
                o.optInt("repeatType", REPEAT_NONE),
                o.optBoolean("enabled", true)
        );
    }
}
