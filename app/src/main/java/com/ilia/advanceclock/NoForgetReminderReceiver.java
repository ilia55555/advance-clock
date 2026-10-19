package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class NoForgetReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        long id=intent==null?-1L:intent.getLongExtra("noteId",-1L);
        NoForgetStore store=new NoForgetStore(context);
        NoForgetItem item=store.find(id);
        if(item==null)return;

        boolean preReminder = intent.getBooleanExtra("preReminder", false);
        if (preReminder) {
            int minutes = intent.getIntExtra("reminderMinutes", 0);
            if (!item.reminderEnabled || !item.hasDue
                    || !AppSettings.noteReminderNotificationsEnabled(context)
                    || !AlarmReminderUtils.effective(item.reminderMode, item.reminderMinutesJson)
                    .contains(minutes)) return;
        }

        Intent service=NoForgetSoundService.startIntent(context,id);
        if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);
        else context.startService(service);

        // An advance reminder rings with the note's settings but leaves the due time intact.
        if (preReminder) return;

        if (intent.getBooleanExtra("snooze", false)) {
            context.getSharedPreferences("note_snoozes", Context.MODE_PRIVATE).edit().remove(Long.toString(id)).apply();
            return; // Snoozing never moves the note's normal recurrence schedule.
        }
        long now=System.currentTimeMillis()+1000L;
        long next=RecurrenceUtils.next(
                item.dueAtMillis,
                item.recurrenceMode,
                item.intervalDays,
                item.customDatesJson,
                now);
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
