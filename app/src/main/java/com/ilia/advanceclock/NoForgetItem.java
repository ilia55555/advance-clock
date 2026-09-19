package com.ilia.advanceclock;

import org.json.JSONException;
import org.json.JSONObject;

public final class NoForgetItem {
    public static final int PRIORITY_LOW = PriorityUtils.LOW;
    public static final int PRIORITY_RELATIVELY_LOW = PriorityUtils.RELATIVELY_LOW;
    public static final int PRIORITY_MEDIUM = PriorityUtils.MEDIUM;
    public static final int PRIORITY_NORMAL = PriorityUtils.MEDIUM;
    public static final int PRIORITY_HIGH = PriorityUtils.HIGH;
    public static final int PRIORITY_VERY_HIGH = PriorityUtils.VERY_HIGH;

    public long id;
    public String title;
    public String body;
    public String sketchJson;
    public int priority;
    public boolean hasDue;
    public long dueAtMillis;
    public boolean reminderEnabled;
    public long createdAt;

    public int recurrenceMode;
    public int intervalDays;
    public String customDatesJson;

    public NoForgetItem(long id,String title,String body,String sketchJson,int priority,
                        boolean hasDue,long dueAtMillis,boolean reminderEnabled,long createdAt){
        this(id,title,body,sketchJson,priority,hasDue,dueAtMillis,reminderEnabled,createdAt,
                RecurrenceUtils.NONE,1,"[]");
    }

    public NoForgetItem(long id,String title,String body,String sketchJson,int priority,
                        boolean hasDue,long dueAtMillis,boolean reminderEnabled,long createdAt,
                        int recurrenceMode,int intervalDays,String customDatesJson){
        this.id=id;
        this.title=title==null?"":title;
        this.body=body==null?"":body;
        this.sketchJson=sketchJson==null?"[]":sketchJson;
        this.priority=PriorityUtils.clamp(priority);
        this.hasDue=hasDue;
        this.dueAtMillis=dueAtMillis;
        this.reminderEnabled=reminderEnabled;
        this.createdAt=createdAt;
        this.recurrenceMode=Math.max(0,Math.min(6,recurrenceMode));
        this.intervalDays=Math.max(1,intervalDays);
        this.customDatesJson=customDatesJson==null?"[]":customDatesJson;
    }

    public int urgency(long now){
        if(hasDue&&dueAtMillis<=now)return 6;
        if(hasDue&&dueAtMillis-now<=60L*60L*1000L)return 5;
        if(hasDue&&dueAtMillis-now<=24L*60L*60L*1000L)return Math.max(4,priority+1);
        return priority+1;
    }

    public JSONObject toJson() throws JSONException{
        JSONObject o=new JSONObject();
        o.put("id",id);o.put("title",title);o.put("body",body);o.put("sketchJson",sketchJson);
        o.put("priority",priority);o.put("priorityVersion",2);
        o.put("hasDue",hasDue);o.put("dueAtMillis",dueAtMillis);
        o.put("reminderEnabled",reminderEnabled);o.put("createdAt",createdAt);
        o.put("recurrenceMode",recurrenceMode);o.put("intervalDays",intervalDays);o.put("customDatesJson",customDatesJson);
        return o;
    }

    public static NoForgetItem fromJson(JSONObject o){
        int rawPriority=o.optInt("priority",PRIORITY_MEDIUM);
        int priority=o.optInt("priorityVersion",1)>=2
                ? PriorityUtils.clamp(rawPriority)
                : PriorityUtils.migrateLegacy(rawPriority);
        return new NoForgetItem(
                o.optLong("id",0),o.optString("title",""),o.optString("body",""),o.optString("sketchJson","[]"),
                priority,o.optBoolean("hasDue",false),o.optLong("dueAtMillis",0),
                o.optBoolean("reminderEnabled",false),o.optLong("createdAt",System.currentTimeMillis()),
                o.optInt("recurrenceMode",RecurrenceUtils.NONE),o.optInt("intervalDays",1),o.optString("customDatesJson","[]")
        );
    }
}
