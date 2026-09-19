package com.ilia.advanceclock;

import org.json.JSONException;
import org.json.JSONObject;

public final class AlarmItem {
    public static final int REPEAT_NONE=0, REPEAT_DAILY=1, REPEAT_WEEKLY=2, REPEAT_MONTHLY=3, REPEAT_YEARLY=4;
    public static final int PRIORITY_LOW=0, PRIORITY_NORMAL=1, PRIORITY_HIGH=2;

    public long id;
    public String label;
    public long triggerAtMillis;
    public int repeatType;
    public boolean enabled;
    public boolean vibrate;

    public int priority;
    public int recurrenceMode;
    public int intervalDays;
    public String customDatesJson;
    public int snoozeMinutes;

    public AlarmItem(long id,String label,long triggerAtMillis,int repeatType,boolean enabled){
        this(id,label,triggerAtMillis,repeatType,enabled,true);
    }

    public AlarmItem(long id,String label,long triggerAtMillis,int repeatType,boolean enabled,boolean vibrate){
        this(id,label,triggerAtMillis,repeatType,enabled,vibrate,
                PRIORITY_NORMAL,repeatType,1,"[]",15);
    }

    public AlarmItem(long id,String label,long triggerAtMillis,int repeatType,boolean enabled,boolean vibrate,
                     int priority,int recurrenceMode,int intervalDays,String customDatesJson,int snoozeMinutes){
        this.id=id;
        this.label=label==null?"":label;
        this.triggerAtMillis=triggerAtMillis;
        this.repeatType=repeatType;
        this.enabled=enabled;
        this.vibrate=vibrate;
        this.priority=Math.max(0,Math.min(2,priority));
        this.recurrenceMode=Math.max(0,Math.min(6,recurrenceMode));
        this.intervalDays=Math.max(1,intervalDays);
        this.customDatesJson=customDatesJson==null?"[]":customDatesJson;
        this.snoozeMinutes=Math.max(5,snoozeMinutes);
    }

    public JSONObject toJson() throws JSONException{
        JSONObject o=new JSONObject();
        o.put("id",id);o.put("label",label);o.put("triggerAtMillis",triggerAtMillis);
        o.put("repeatType",repeatType);o.put("enabled",enabled);o.put("vibrate",vibrate);
        o.put("priority",priority);o.put("recurrenceMode",recurrenceMode);o.put("intervalDays",intervalDays);
        o.put("customDatesJson",customDatesJson);o.put("snoozeMinutes",snoozeMinutes);
        return o;
    }

    public static AlarmItem fromJson(JSONObject o){
        int oldRepeat=o.optInt("repeatType",REPEAT_NONE);
        return new AlarmItem(
                o.optLong("id",0),o.optString("label",""),o.optLong("triggerAtMillis",0),
                oldRepeat,o.optBoolean("enabled",true),o.optBoolean("vibrate",true),
                o.optInt("priority",PRIORITY_NORMAL),
                o.has("recurrenceMode")?o.optInt("recurrenceMode",oldRepeat):oldRepeat,
                o.optInt("intervalDays",1),o.optString("customDatesJson","[]"),
                o.optInt("snoozeMinutes",15)
        );
    }
}
