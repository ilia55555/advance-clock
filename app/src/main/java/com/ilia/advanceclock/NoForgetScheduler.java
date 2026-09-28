package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public final class NoForgetScheduler {
    private static final int MAIN_REQUEST_BASE = 90_000;
    private static final int PRE_REQUEST_BASE = 1_100_000;

    private NoForgetScheduler(){}

    public static boolean schedule(Context context,NoForgetItem item){
        cancel(context,item.id);
        if(!item.reminderEnabled||!item.hasDue)return false;

        long now=System.currentTimeMillis();
        long next=RecurrenceUtils.next(
                item.dueAtMillis,
                item.recurrenceMode,
                item.intervalDays,
                item.customDatesJson,
                now);
        if(next<=now)return false;
        if(next!=item.dueAtMillis){
            item.dueAtMillis=next;
            new NoForgetStore(context).save(item);
        }
        if(!PermissionHelper.exactAlarmsGranted(context))return false;

        AlarmManager manager=context.getSystemService(AlarmManager.class);
        if(manager==null)return false;
        try{
            manager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    item.dueAtMillis,
                    mainPendingIntent(context,item.id));

            List<Integer> reminders=AlarmReminderUtils.effective(
                    item.reminderMode,
                    item.reminderMinutesJson);
            for(int i=0;i<reminders.size();i++){
                int minutes=reminders.get(i);
                long at=item.dueAtMillis-minutes*60_000L;
                if(at<=now)continue;
                manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        at,
                        preReminderPendingIntent(context,item.id,i,minutes));
            }
            return true;
        }catch(SecurityException e){
            cancel(context,item.id);
            return false;
        }
    }

    public static void cancel(Context context,long id){
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        if(manager==null)return;
        manager.cancel(mainPendingIntent(context,id));
        for(int i=0;i<AlarmReminderUtils.VALUES.length;i++){
            manager.cancel(preReminderPendingIntent(context,id,i,0));
        }
    }

    public static void rescheduleAll(Context context){
        NoForgetStore store=new NoForgetStore(context);
        for(NoForgetItem item:store.all()){
            if(!item.reminderEnabled||!item.hasDue)continue;
            if(!schedule(context,item)){
                long next=RecurrenceUtils.next(
                        item.dueAtMillis,
                        item.recurrenceMode,
                        item.intervalDays,
                        item.customDatesJson,
                        System.currentTimeMillis());
                if(next<=0){
                    item.reminderEnabled=false;
                    store.save(item);
                }
            }
        }
        NoForgetWidgetProvider.updateAll(context);
    }

    private static PendingIntent mainPendingIntent(Context context,long id){
        Intent intent=new Intent(context,NoForgetReminderReceiver.class)
                .putExtra("noteId",id)
                .putExtra("preReminder",false);
        int requestCode=MAIN_REQUEST_BASE+(int)Math.abs(id%1_000_000L);
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent preReminderPendingIntent(
            Context context,long id,int slot,int minutes){
        Intent intent=new Intent(context,NoForgetReminderReceiver.class)
                .putExtra("noteId",id)
                .putExtra("preReminder",true)
                .putExtra("reminderMinutes",minutes)
                .putExtra("reminderSlot",slot);
        int requestCode=PRE_REQUEST_BASE
                +(int)(Math.abs(id%80_000L)*10L)
                +Math.max(0,Math.min(9,slot));
        return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
}
