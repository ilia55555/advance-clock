package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import org.json.*;
import java.util.*;

/** One shared next exact alarm per event; rules advance independently without polling. */
final class EventReminderScheduler {
    private static final String PREFS="event_reminder_schedule_v1";
    static final String OCCURRENCE="reminderOccurrence";
    private static String prefix(String kind,long id){return kind+"/"+id+"/";}
    private static PendingIntent operation(Context c,String token,long occurrence,long when){
        Intent intent=new Intent(c,EventReminderReceiver.class)
                .setData(Uri.parse("advanceclock://event-reminder/"+token.substring(0,token.lastIndexOf('/'))))
                .putExtra("token",token).putExtra("occurrence",occurrence).putExtra("when",when);
        return PendingIntent.getBroadcast(c,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static boolean hasPending(Context c,String kind,long id,long now){
        for(Map.Entry<String,?> entry:c.getSharedPreferences(PREFS,0).getAll().entrySet())
            if(entry.getKey().startsWith(prefix(kind,id)))try{
                if(new JSONObject(String.valueOf(entry.getValue())).getLong("when")>=now-10*60000L)return true;
            }catch(JSONException ignored){}
        return false;
    }
    static synchronized void cancel(Context c,String kind,long id){
        AlarmManager manager=c.getSystemService(AlarmManager.class);
        android.content.SharedPreferences prefs=c.getSharedPreferences(PREFS,0);
        android.content.SharedPreferences.Editor edit=prefs.edit();
        PendingIntent pending=operation(c,prefix(kind,id)+"clear",0,0);
        if(manager!=null)manager.cancel(pending);pending.cancel();
        for(String token:prefs.getAll().keySet())if(token.startsWith(prefix(kind,id)))edit.remove(token);
        edit.apply();
        c.getSharedPreferences("event_reminder_delivered",0).edit().remove(kind+"/"+id).apply();
    }
    static synchronized void sync(Context c,AlarmItem item){sync(c,item,System.currentTimeMillis());}
    static synchronized void sync(Context c,AlarmItem item,long now){
        sync(c,"alarm",item.id,item.reminderAnchorMillis>0?item.reminderAnchorMillis:item.triggerAtMillis,
                item.recurrenceMode,item.intervalDays,item.customDatesJson,item.timeZone(),item.reminderMode,item.reminderMinutesJson,
                item.enabled || item.lastFiredAtMillis>0,now);
    }
    static synchronized void sync(Context c,NoForgetItem item){sync(c,item,System.currentTimeMillis());}
    static synchronized void sync(Context c,NoForgetItem item,long now){
        sync(c,"note",item.id,item.reminderAnchorMillis>0?item.reminderAnchorMillis:item.dueAtMillis,
                item.recurrenceMode,item.intervalDays,item.customDatesJson,AppTime.zone(),item.reminderMode,item.reminderMinutesJson,
                item.hasDue && (item.reminderEnabled || item.lastFiredAtMillis>0),now);
    }
    private static void sync(Context c,String kind,long id,long anchor,int recurrence,int interval,String dates,
                             TimeZone zone,int mode,String json,boolean active,long now){
        if(!active || !EventReminderRules.rich(json) || !PermissionHelper.exactAlarmsGranted(c)){cancel(c,kind,id);return;}
        String signature=signature(anchor,recurrence,interval,dates,zone,json);
        android.content.SharedPreferences prefs=c.getSharedPreferences(PREFS,0);
        List<EventReminderRules.Rule> rules=EventReminderRules.parse(mode,json,false);
        Set<String> wanted=new HashSet<>();for(EventReminderRules.Rule rule:rules)wanted.add(prefix(kind,id)+rule.key());
        for(String token:prefs.getAll().keySet())if(token.startsWith(prefix(kind,id)) && !wanted.contains(token)){
            prefs.edit().remove(token).apply();
        }
        for(EventReminderRules.Rule rule:rules){
            String token=prefix(kind,id)+rule.key();boolean restored=false;
            try {
                JSONObject pending=new JSONObject(prefs.getString(token,""));
                if(signature.equals(pending.getString("signature")) && pending.getLong("when")>=now-10*60000L){
                    schedule(c,kind,id,rule,pending.getLong("occurrence"),pending.getLong("when"),signature);restored=true;
                }
            }catch(JSONException ignored){}
            if(!restored){
                long occurrence=rule.next(anchor,recurrence,interval,dates,zone,now);
                if(occurrence>0)schedule(c,kind,id,rule,occurrence,rule.at(occurrence,zone),signature);
                else {
                    prefs.edit().remove(token).apply();
                }
            }
        }
        armNext(c,kind,id);
    }

    private static String signature(long anchor,int recurrence,int interval,String dates,TimeZone zone,String json){
        String value=anchor+":"+recurrence+":"+interval+":"+dates+":"+zone.getID()+":"+json;
        try {
            byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder digest=new StringBuilder();for(byte part:bytes)digest.append(String.format(java.util.Locale.ROOT,"%02x",part&255));
            return digest.toString();
        }catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
    private static void schedule(Context c,String kind,long id,EventReminderRules.Rule rule,long occurrence,long when,String signature){
        String token=prefix(kind,id)+rule.key();
        try{
            JSONObject record=new JSONObject().put("occurrence",occurrence).put("when",when).put("signature",signature);
            c.getSharedPreferences(PREFS,0).edit().putString(token,record.toString()).commit();
        }catch(JSONException impossible){throw new IllegalStateException(impossible);}
    }
    private static void armNext(Context c,String kind,long id){
        AlarmManager manager=c.getSystemService(AlarmManager.class);if(manager==null)return;
        String token=null;long next=Long.MAX_VALUE,occurrence=0;
        for(Map.Entry<String,?> entry:c.getSharedPreferences(PREFS,0).getAll().entrySet())if(entry.getKey().startsWith(prefix(kind,id)))try{
            JSONObject record=new JSONObject(String.valueOf(entry.getValue()));
            long when=record.getLong("when");
            if(when<next){next=when;occurrence=record.getLong("occurrence");token=entry.getKey();}
        }catch(JSONException ignored){}
        if(token==null){PendingIntent pending=operation(c,prefix(kind,id)+"clear",0,0);manager.cancel(pending);pending.cancel();return;}
        try{
            PendingIntent show=PendingIntent.getActivity(c,kind.equals("note")?801:802,new Intent(c,MainActivity.class)
                    .putExtra("openTab",kind.equals("note")?"noforget":"clock"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(next,show),operation(c,token,occurrence,next));
        }catch(RuntimeException error){android.util.Log.e("EventReminders","Cannot schedule next event reminder",error);}
    }
    static void dispatch(Context c,Intent intent){dispatch(c,intent,System.currentTimeMillis());}
    static void dispatch(Context c,Intent intent,long now){
        String token=intent==null?null:intent.getStringExtra("token");if(token==null)return;
        String[] parts=token.split("/");if(parts.length!=3 || (!parts[0].equals("alarm") && !parts[0].equals("note")))return;
        String kind=parts[0];long id;try{id=Long.parseLong(parts[1]);}catch(Exception e){return;}
        try{
            String raw=c.getSharedPreferences(PREFS,0).getString(token,"");if(raw.isEmpty())return;
            JSONObject record=new JSONObject(raw);long occurrence=record.getLong("occurrence"),when=record.getLong("when");
            if(occurrence!=intent.getLongExtra("occurrence",0) || when!=intent.getLongExtra("when",0))return;
            AlarmItem alarm=kind.equals("alarm")?new AlarmStore(c).find(id):null;
            NoForgetItem note=kind.equals("note")?new NoForgetStore(c).find(id):null;
            if(alarm==null && note==null){cancel(c,kind,id);return;}
            long anchor=alarm!=null?alarm.reminderAnchorMillis:note.reminderAnchorMillis;
            int recurrence=alarm!=null?alarm.recurrenceMode:note.recurrenceMode,interval=alarm!=null?alarm.intervalDays:note.intervalDays;
            String dates=alarm!=null?alarm.customDatesJson:note.customDatesJson;
            TimeZone zone=alarm!=null?alarm.timeZone():AppTime.zone();
            int mode=alarm!=null?alarm.reminderMode:note.reminderMode;
            String json=alarm!=null?alarm.reminderMinutesJson:note.reminderMinutesJson;
            boolean active=alarm!=null?(alarm.enabled || alarm.lastFiredAtMillis>0):(note.hasDue && (note.reminderEnabled || note.lastFiredAtMillis>0));
            if(!active || !record.getString("signature").equals(signature(anchor,recurrence,interval,dates,zone,json))){cancel(c,kind,id);return;}
            EventReminderRules.Rule selected=null;
            for(EventReminderRules.Rule rule:EventReminderRules.parse(mode,json,false))if(rule.key().equals(parts[2]))selected=rule;
            if(selected==null)return;
            if(when>now+1000)return;
            boolean notifications=alarm!=null?AppSettings.alarmReminderNotificationsEnabled(c):AppSettings.noteReminderNotificationsEnabled(c);
            boolean shouldRing=notifications && now-when<=10*60000L;
            synchronized(EventReminderScheduler.class) {
                if(!raw.equals(c.getSharedPreferences(PREFS,0).getString(token,"")))return;
                c.getSharedPreferences(PREFS,0).edit().remove(token).commit();
                // Advance this rule independently: offsets longer than the repeat period still recur correctly.
                long next=selected.next(anchor,recurrence,interval,dates,zone,Math.max(now,when));
                if(next>0)schedule(c,kind,id,selected,next,selected.at(next,zone),record.getString("signature"));
                armNext(c,kind,id);
                if(shouldRing){
                    android.content.SharedPreferences delivered=c.getSharedPreferences("event_reminder_delivered",0);
                    String delivery=occurrence+":"+when,key=kind+"/"+id;
                    if(delivery.equals(delivered.getString(key,"")))shouldRing=false;
                    else delivered.edit().putString(key,delivery).commit();
                }
            }
            if(!shouldRing)return;
            if(alarm!=null)AlarmSoundService.ring(c,alarm,occurrence);
            else {
                Intent service=NoForgetSoundService.startIntent(c,id).putExtra(OCCURRENCE,occurrence);
                if(Build.VERSION.SDK_INT>=26)c.startForegroundService(service);else c.startService(service);
            }
        }catch(JSONException ignored){}
    }
}
