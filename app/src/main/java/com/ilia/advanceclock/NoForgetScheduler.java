package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class NoForgetScheduler {
    private NoForgetScheduler(){}

    public static boolean schedule(Context context,NoForgetItem item){
        cancel(context,item.id);
        if(!item.reminderEnabled||!item.hasDue)return false;
        long now=System.currentTimeMillis();
        long next=RecurrenceUtils.next(item.dueAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now);
        if(next<=now)return false;
        if(next!=item.dueAtMillis){item.dueAtMillis=next;new NoForgetStore(context).save(item);}
        if(!PermissionHelper.exactAlarmsGranted(context))return false;
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        if(manager==null)return false;
        try{
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,item.dueAtMillis,pendingIntent(context,item.id));
            return true;
        }catch(SecurityException e){return false;}
    }

    public static void cancel(Context context,long id){
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        if(manager!=null)manager.cancel(pendingIntent(context,id));
    }

    public static void rescheduleAll(Context context){
        NoForgetStore store=new NoForgetStore(context);
        for(NoForgetItem item:store.all()){
            if(!item.reminderEnabled||!item.hasDue)continue;
            if(!schedule(context,item)){
                long next=RecurrenceUtils.next(item.dueAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,System.currentTimeMillis());
                if(next<=0){item.reminderEnabled=false;store.save(item);}
            }
        }
        NoForgetWidgetProvider.updateAll(context);
    }

    private static PendingIntent pendingIntent(Context context,long id){
        Intent intent=new Intent(context,NoForgetReminderReceiver.class).putExtra("noteId",id);
        return PendingIntent.getBroadcast(context,90000+(int)Math.abs(id%1_000_000),intent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
}
