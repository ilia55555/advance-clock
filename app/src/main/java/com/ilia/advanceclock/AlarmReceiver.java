package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        long id=intent.getLongExtra("alarmId",-1L);
        AlarmStore store=new AlarmStore(context);
        AlarmItem item=store.find(id);
        if(item==null||!item.enabled)return;

        Intent service=AlarmSoundService.startIntent(context,item.id,item.label,item.vibrate);
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)context.startForegroundService(service);
        else context.startService(service);

        long now=System.currentTimeMillis()+1000L;
        long next=RecurrenceUtils.next(item.triggerAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now);
        if(next>now){
            item.triggerAtMillis=next;
            item.enabled=true;
            store.save(item);
            AlarmScheduler.schedule(context,item);
        }else{
            item.enabled=false;
            store.save(item);
        }
        ClockWidgetProvider.updateAll(context);
    }
}
