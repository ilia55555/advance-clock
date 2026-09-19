package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class AlarmScheduler {
    private AlarmScheduler(){}

    public static boolean schedule(Context context,AlarmItem item){
        if(!item.enabled){cancel(context,item.id);return true;}
        long now=System.currentTimeMillis();
        long normalized=RecurrenceUtils.next(item.triggerAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now);
        if(normalized<=now)return false;
        if(normalized!=item.triggerAtMillis){item.triggerAtMillis=normalized;new AlarmStore(context).save(item);}

        AlarmManager manager=context.getSystemService(AlarmManager.class);
        if(manager==null||!PermissionHelper.exactAlarmsGranted(context))return false;
        PendingIntent operation=alarmPendingIntent(context,item.id);
        PendingIntent show=PendingIntent.getActivity(context,requestCode(item.id)+1,
                new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try{
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(item.triggerAtMillis,show),operation);
            return true;
        }catch(SecurityException e){return false;}
    }

    public static boolean snooze(Context context,long id,long delayMillis){
        AlarmStore store=new AlarmStore(context);
        AlarmItem item=store.find(id);
        if(item==null)return false;
        item.enabled=true;
        item.triggerAtMillis=System.currentTimeMillis()+Math.max(60_000L,delayMillis);
        store.save(item);
        return scheduleDirect(context,item);
    }

    private static boolean scheduleDirect(Context context,AlarmItem item){
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        if(manager==null||!PermissionHelper.exactAlarmsGranted(context))return false;
        PendingIntent show=PendingIntent.getActivity(context,requestCode(item.id)+1,
                new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try{
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(item.triggerAtMillis,show),alarmPendingIntent(context,item.id));
            return true;
        }catch(SecurityException e){return false;}
    }

    public static void cancel(Context context,long id){
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        if(manager!=null)manager.cancel(alarmPendingIntent(context,id));
    }

    public static void rescheduleAll(Context context){
        AlarmStore store=new AlarmStore(context);
        long now=System.currentTimeMillis();
        for(AlarmItem item:store.all()){
            if(!item.enabled)continue;
            long next=RecurrenceUtils.next(item.triggerAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now);
            if(next<=now){item.enabled=false;store.save(item);continue;}
            if(next!=item.triggerAtMillis){item.triggerAtMillis=next;store.save(item);}
            schedule(context,item);
        }
        ClockWidgetProvider.updateAll(context);
    }

    private static PendingIntent alarmPendingIntent(Context context,long id){
        Intent intent=new Intent(context,AlarmReceiver.class).putExtra("alarmId",id);
        return PendingIntent.getBroadcast(context,requestCode(id),intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(long id){return (int)(10000+Math.abs(id%1_000_000));}
}
