package com.ilia.advanceclock;

import org.json.JSONException;
import org.json.JSONObject;

public final class AlarmItem {
    public static final int REPEAT_NONE=0, REPEAT_DAILY=1, REPEAT_WEEKLY=2, REPEAT_MONTHLY=3, REPEAT_YEARLY=4;

    public static final int PRIORITY_LOW = PriorityUtils.LOW;
    public static final int PRIORITY_RELATIVELY_LOW = PriorityUtils.RELATIVELY_LOW;
    public static final int PRIORITY_MEDIUM = PriorityUtils.MEDIUM;
    public static final int PRIORITY_NORMAL = PriorityUtils.MEDIUM;
    public static final int PRIORITY_HIGH = PriorityUtils.HIGH;
    public static final int PRIORITY_VERY_HIGH = PriorityUtils.VERY_HIGH;

    public long id;
    public String referenceZoneId="";
    public boolean createdFromWorld;
    public java.util.TimeZone timeZone(){return referenceZoneId.isEmpty()?AppTime.zone():java.util.TimeZone.getTimeZone(referenceZoneId); }
    public boolean applyDate;
    public boolean autoDeleteAfterCompletion;
    public long pendingSnoozeAtMillis;
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

    public int reminderMode;
    public String reminderMinutesJson;
    public long lastFiredAtMillis;
    public String imageUri1;
    public String imageUri2;
    public String imageSource1 = "";
    public String imageSource2 = "";
    public String soundUri;
    public String attachmentsJson = "[]";
    public boolean imagesAsBackground;

    public AlarmItem(long id,String label,long triggerAtMillis,int repeatType,boolean enabled){
        this(id,label,triggerAtMillis,repeatType,enabled,true);
    }

    public AlarmItem(long id,String label,long triggerAtMillis,int repeatType,boolean enabled,boolean vibrate){
        this(id,label,triggerAtMillis,repeatType,enabled,vibrate,
                PRIORITY_MEDIUM,repeatType,1,"[]",15,
                AlarmReminderUtils.MODE_NONE,"[]");
    }

    public AlarmItem(long id,String label,long triggerAtMillis,int repeatType,boolean enabled,boolean vibrate,
                     int priority,int recurrenceMode,int intervalDays,String customDatesJson,int snoozeMinutes){
        this(id,label,triggerAtMillis,repeatType,enabled,vibrate,
                priority,recurrenceMode,intervalDays,customDatesJson,snoozeMinutes,
                AlarmReminderUtils.MODE_NONE,"[]");
    }

    public AlarmItem(long id,String label,long triggerAtMillis,int repeatType,boolean enabled,boolean vibrate,
                     int priority,int recurrenceMode,int intervalDays,String customDatesJson,int snoozeMinutes,
                     int reminderMode,String reminderMinutesJson){
        this.id=id;
        this.label=label==null?"":label;
        this.triggerAtMillis=triggerAtMillis;
        this.repeatType=repeatType;
        this.enabled=enabled;
        this.vibrate=vibrate;
        this.priority=PriorityUtils.clamp(priority);
        this.recurrenceMode=Math.max(0,Math.min(RecurrenceUtils.INTERVAL_MINUTES,recurrenceMode));
        this.intervalDays=Math.max(1,intervalDays);
        this.customDatesJson=customDatesJson==null?"[]":customDatesJson;
        this.snoozeMinutes=Math.max(5,snoozeMinutes);
        this.reminderMode=Math.max(0,Math.min(2,reminderMode));
        this.reminderMinutesJson=reminderMinutesJson==null?"[]":reminderMinutesJson;
        this.lastFiredAtMillis=0L;
        this.imageUri1="";
        this.imageUri2="";
        this.soundUri="";
    }

    public JSONObject toJson() throws JSONException{
        JSONObject o=new JSONObject();
        o.put("createdFromWorld",createdFromWorld);
        o.put("referenceZoneId",referenceZoneId);
        o.put("applyDate", applyDate);
        o.put("autoDeleteAfterCompletion", autoDeleteAfterCompletion);
        o.put("pendingSnoozeAtMillis", pendingSnoozeAtMillis);
        o.put("id",id);o.put("label",label);o.put("triggerAtMillis",triggerAtMillis);
        o.put("repeatType",repeatType);o.put("enabled",enabled);o.put("vibrate",vibrate);
        o.put("priority",priority);o.put("priorityVersion",2);
        o.put("recurrenceMode",recurrenceMode);o.put("intervalDays",intervalDays);
        o.put("customDatesJson",customDatesJson);o.put("snoozeMinutes",snoozeMinutes);
        o.put("reminderMode",reminderMode);o.put("reminderMinutesJson",reminderMinutesJson);
        o.put("lastFiredAtMillis",lastFiredAtMillis);
        o.put("imageUri1",imageUri1);
        o.put("imageUri2",imageUri2);
        o.put("imageSource1",imageSource1);
        o.put("imageSource2",imageSource2);
        o.put("soundUri",soundUri);
        o.put("attachmentsJson",attachmentsJson);
        o.put("imagesAsBackground",imagesAsBackground);
        return o;
    }

    public static AlarmItem fromJson(JSONObject o){
        int oldRepeat=o.optInt("repeatType",REPEAT_NONE);
        int rawPriority=o.optInt("priority",PRIORITY_MEDIUM);
        int priority=o.optInt("priorityVersion",1)>=2
                ? PriorityUtils.clamp(rawPriority)
                : PriorityUtils.migrateLegacy(rawPriority);

        AlarmItem item = new AlarmItem(
                o.optLong("id",0),o.optString("label",""),o.optLong("triggerAtMillis",0),
                oldRepeat,o.optBoolean("enabled",true),o.optBoolean("vibrate",true),
                priority,
                o.has("recurrenceMode")?o.optInt("recurrenceMode",oldRepeat):oldRepeat,
                o.optInt("intervalDays",1),o.optString("customDatesJson","[]"),
                o.optInt("snoozeMinutes",15),
                o.optInt("reminderMode",AlarmReminderUtils.MODE_NONE),
                o.optString("reminderMinutesJson","[]")
        );
        item.referenceZoneId=o.optString("referenceZoneId", "");
        item.createdFromWorld=o.optBoolean("createdFromWorld",!item.referenceZoneId.isEmpty());
        item.autoDeleteAfterCompletion = o.optBoolean("autoDeleteAfterCompletion", false);
        item.pendingSnoozeAtMillis = o.optLong("pendingSnoozeAtMillis", 0L);
        item.applyDate = o.optBoolean("applyDate", true); // Preserve explicit dates in existing records.
        item.lastFiredAtMillis = o.optLong("lastFiredAtMillis", 0L);
        item.imageUri1 = o.optString("imageUri1", "");
        item.imageUri2 = o.optString("imageUri2", "");
        item.imageSource1 = o.optString("imageSource1", item.imageUri1);
        item.imageSource2 = o.optString("imageSource2", item.imageUri2);
        item.soundUri = o.optString("soundUri", "");
        item.attachmentsJson = o.optString("attachmentsJson", "[]");
        item.imagesAsBackground = o.optBoolean("imagesAsBackground", false);
        return item;
    }
}
