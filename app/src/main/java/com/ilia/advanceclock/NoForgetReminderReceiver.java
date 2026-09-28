package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class NoForgetReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        long id=intent.getLongExtra("noteId",-1L);
        NoForgetStore store=new NoForgetStore(context);
        NoForgetItem item=store.find(id);
        if(item==null)return;

        Intent service = NoForgetSoundService.startIntent(context, id);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(service);
        else context.startService(service);

        long now=System.currentTimeMillis()+1000L;
        long next=RecurrenceUtils.next(item.dueAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now);
        if(next>now){
            item.dueAtMillis=next;
            item.reminderEnabled=true;
            store.save(item);
            NoForgetScheduler.schedule(context,item);
        }else{
            item.reminderEnabled=false;
            store.save(item);
        }
        NoForgetWidgetProvider.updateAll(context);

    }
}
