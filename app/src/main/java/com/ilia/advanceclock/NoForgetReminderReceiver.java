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
        if(item==null || item.completedOccurrence(item.dueAtMillis) || OccurrenceSkips.contains(context,"note",id,item.dueAtMillis))return;
        NoteTasks.reopenForOccurrence(context,item,item.dueAtMillis);

        boolean preReminder = intent.getBooleanExtra("preReminder", false);
        if(!preReminder && !intent.getBooleanExtra("snooze",false) && item.dueAtMillis>System.currentTimeMillis()+1000)return;
        if (preReminder) {
            int minutes = intent.getIntExtra("reminderMinutes", 0);
            if(item.dueAtMillis-minutes*60000L>System.currentTimeMillis()+1000)return;
            if (!item.reminderEnabled || !item.hasDue
                    || !AppSettings.noteReminderNotificationsEnabled(context)
                    || !AlarmReminderUtils.effective(item.reminderMode, item.reminderMinutesJson)
                    .contains(minutes)) return;
        }

        Intent service=NoForgetSoundService.startIntent(context,id).putExtra("taskOccurrence",item.dueAtMillis);
        if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);
        else context.startService(service);

        // An advance reminder rings with the note's settings but leaves the due time intact.
        if (preReminder) return;

        Context application=context.getApplicationContext();
        AlarmReceiverWork.execute(goAsync(),()->afterRing(application,intent,item));
    }

    private void afterRing(Context context,Intent intent,NoForgetItem item){
        synchronized(NoteTasks.class){
        long id=item.id;
        NoForgetStore store=new NoForgetStore(context);
        NoForgetItem latest=store.find(id);
        if(latest==null || latest.completedOccurrence(item.dueAtMillis))return;
        if (intent.getBooleanExtra("snooze", false)) {
            context.getSharedPreferences("note_snoozes", Context.MODE_PRIVATE).edit().remove(Long.toString(id)).apply();
            return; // Snoozing never moves the note's normal recurrence schedule.
        }
        item.lastFiredAtMillis=item.dueAtMillis;
        EventReminderScheduler.sync(context,item);
        long now=System.currentTimeMillis()+1000L;
        long next=OccurrenceSkips.next(context,"note",id,
                item.dueAtMillis,
                item.recurrenceMode,
                item.intervalDays,
                item.customDatesJson,
                now,AppTime.zone());
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

}
