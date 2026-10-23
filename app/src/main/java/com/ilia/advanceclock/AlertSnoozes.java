package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import org.json.JSONObject;
import java.util.*;

/** Independent durable reminders. Never moves an alert's normal recurrence or another snooze. */
final class AlertSnoozes {
    static final String PREFS="alert_snoozes_v2";
    private static final Object LOCK=new Object();
    static long nowMillis(){return System.currentTimeMillis();}
    private static SharedPreferences prefs(Context context){return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);}

    static boolean schedule(Context context,String kind,long id,String label,String payload,int[] minutes) {
        if(minutes==null || minutes.length==0 || !PermissionHelper.exactAlarmsGranted(context))return false;
        Set<Integer> allowed=new HashSet<>();for(int m:AlertActions.MINUTES)allowed.add(m);
        TreeSet<Integer> times=new TreeSet<>();for(int m:minutes){if(!allowed.contains(m))return false;times.add(m);}
        if(!Arrays.asList("alarm","note","timer","stopwatch","adhan").contains(kind))return false;
        try {
            if ("alarm".equals(kind) && AlarmItem.fromJson(new JSONObject(payload)).id != id) return false;
            if ("note".equals(kind) && new NoForgetStore(context).find(id) == null) return false;
            if ("adhan".equals(kind) && (id<AdhanScheduler.FAJR || id>AdhanScheduler.ISHA)) return false;
            if (("timer".equals(kind)||"stopwatch".equals(kind)) && id!=ToolAlarmScheduler.alarmId(kind)) return false;
        } catch (Exception invalid) { return false; }
        AlarmManager manager=context.getSystemService(AlarmManager.class);if(manager==null)return false;
        synchronized(LOCK) {
            long now=nowMillis();
            Map<String,JSONObject> batch=new LinkedHashMap<>();
            SharedPreferences.Editor edit=prefs(context).edit();
            try {
                for(int m:times) {
                    String key=UUID.randomUUID().toString();
                    JSONObject record=new JSONObject().put("kind",kind).put("id",id)
                            .put("label",label==null?"":label).put("payload",payload==null?"":payload)
                            .put("when",now+m*60_000L);
                    batch.put(key,record);edit.putString(key,record.toString());
                }
                if(!edit.commit())throw new IllegalStateException("Unable to persist reminders");
                for(Map.Entry<String,JSONObject> entry:batch.entrySet())arm(context,entry.getKey(),entry.getValue());
                return true;
            } catch(Exception error) {
                SharedPreferences.Editor rollback=prefs(context).edit();
                for(String key:batch.keySet()){manager.cancel(operation(context,key));rollback.remove(key);}
                rollback.commit();return false;
            }
        }
    }
    private static void arm(Context context,String key,JSONObject record) {
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        PendingIntent show=PendingIntent.getActivity(context,0,new Intent(context,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        manager.setAlarmClock(new AlarmManager.AlarmClockInfo(Math.max(nowMillis()+1000,
                record.optLong("when")),show),operation(context,key));
    }
    private static PendingIntent operation(Context context,String key) {
        return PendingIntent.getBroadcast(context,0,new Intent(context,AlertSnoozeReceiver.class)
                .setData(Uri.parse("advanceclock://alert-snooze/"+key)),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    static void restore(Context context) {
        if(!PermissionHelper.exactAlarmsGranted(context))return;
        synchronized(LOCK) {
            // After downtime, replay only the latest overdue reminder per source, keeping all future ones.
            Map<String,String> overdue=new HashMap<>();Map<String,Long> latest=new HashMap<>();
            Map<String,JSONObject> records=new LinkedHashMap<>();long now=nowMillis();
            for(Map.Entry<String,?> entry:prefs(context).getAll().entrySet())try {
                JSONObject record=new JSONObject((String)entry.getValue());records.put(entry.getKey(),record);
                String source=record.optString("kind")+":"+record.optLong("id");long when=record.optLong("when");
                if(when<=now && when>latest.getOrDefault(source,0L)){latest.put(source,when);overdue.put(source,entry.getKey());}
            }catch(Exception ignored){}
            for(Map.Entry<String,JSONObject> entry:records.entrySet()) {
                JSONObject record=entry.getValue();String source=record.optString("kind")+":"+record.optLong("id");
                if(record.optLong("when")<=now && !entry.getKey().equals(overdue.get(source))) {
                    remove(context,entry.getKey());continue;
                }
                try{arm(context,entry.getKey(),record);}catch(RuntimeException ignored){}
            }
        }
    }
    static void cancel(Context context,String kind,long id) {
        synchronized(LOCK) {
            for(Map.Entry<String,?> entry:prefs(context).getAll().entrySet())try {
                JSONObject value=new JSONObject((String)entry.getValue());
                if(kind.equals(value.optString("kind")) && value.optLong("id")==id)remove(context,entry.getKey());
            }catch(Exception ignored){}
        }
    }
    private static void remove(Context context,String key) {
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        if(manager!=null)manager.cancel(operation(context,key));prefs(context).edit().remove(key).commit();
    }
    static void deliver(Context context,Uri uri) {
        if(uri==null || !"advanceclock".equals(uri.getScheme()) || !"alert-snooze".equals(uri.getHost()))return;
        synchronized(LOCK) {
            String key=uri.getLastPathSegment(), encoded=prefs(context).getString(key,null);if(encoded==null)return;
            try {
                JSONObject record=new JSONObject(encoded);
                if(record.optLong("when")>nowMillis()+1000)return;
                String kind=record.getString("kind");long id=record.getLong("id");
                Intent service;
                if("alarm".equals(kind)) {
                    AlarmItem item=AlarmItem.fromJson(new JSONObject(record.getString("payload")));
                    if(item.id!=id)return;
                    AlarmSoundService.ring(context,item);remove(context,key);return;
                } else if("note".equals(kind)) {
                    if(new NoForgetStore(context).find(id)==null){remove(context,key);return;}
                    service=NoForgetSoundService.startIntent(context,id);
                } else if("adhan".equals(kind)) {
                    if(id<AdhanScheduler.FAJR || id>AdhanScheduler.ISHA)return;
                    service=AdhanSoundService.startIntent(context,(int)id);
                } else if(ToolAlarmScheduler.TIMER.equals(kind)||ToolAlarmScheduler.STOPWATCH.equals(kind)) {
                    service=AlarmSoundService.startIntent(context,ToolAlarmScheduler.alarmId(kind),record.optString("label"),true)
                            .putExtra("toolKind",kind).putExtra("toolPayload",record.optString("payload"));
                } else {remove(context,key);return;}
                if(android.os.Build.VERSION.SDK_INT>=26)context.startForegroundService(service);else context.startService(service);
                remove(context,key);
            }catch(Exception ignored){} // Keep a failed delivery available for restoration rather than losing it.
        }
    }
}
