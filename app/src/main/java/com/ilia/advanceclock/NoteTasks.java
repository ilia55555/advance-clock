package com.ilia.advanceclock;

import android.content.Context;

/** Completion is local and cancels only the completed occurrence, never the recurrence. */
final class NoteTasks {
    static NoForgetItem setCompleted(Context c,long id,boolean completed){return setCompleted(c,id,completed,System.currentTimeMillis());}
    static synchronized NoForgetItem setCompleted(Context c,long id,boolean completed,long now){
        NoForgetStore store=new NoForgetStore(c);NoForgetItem item=store.find(id);
        if(item==null || !item.taskMode)return item;
        if(item.taskDone(now)==completed)return item;
        if(completed){
            item.taskReminderBeforeCompletion=item.reminderEnabled;
            item.taskPreviousCompletedOccurrence=item.taskCompletedOccurrence;
            item.taskCompletedOccurrence=item.lastFiredAtMillis>item.taskCompletedOccurrence && item.lastFiredAtMillis<=now?item.lastFiredAtMillis:item.dueAtMillis;
            item.taskCompleted=true;
            if(item.recurrenceMode==RecurrenceUtils.NONE)item.reminderEnabled=false;
            else if(item.hasDue && item.dueAtMillis<=item.taskCompletedOccurrence){
                long next=RecurrenceUtils.next(item.dueAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,Math.max(now,item.taskCompletedOccurrence));
                if(next>0)item.dueAtMillis=next;else item.reminderEnabled=false;
            }
        }else{
            if(item.taskCompletedOccurrence>now && item.hasDue)item.dueAtMillis=item.taskCompletedOccurrence;
            item.taskCompletedOccurrence=item.taskPreviousCompletedOccurrence;item.taskPreviousCompletedOccurrence=0;
            item.taskCompleted=false;item.reminderEnabled=item.taskReminderBeforeCompletion;
        }
        store.save(item);NoForgetScheduler.cancel(c,id);
        item.lastFiredAtMillis=0;store.save(item);
        if(item.reminderEnabled && item.hasDue)NoForgetScheduler.schedule(c,item);
        NoForgetSoundService.stopTask(id);NoteReminderActivity.finishIfShowing(id);
        NoForgetWidgetProvider.updateAll(c);return item;
    }
    static synchronized void reopenForOccurrence(Context c,NoForgetItem item,long occurrence){
        item=new NoForgetStore(c).find(item.id);if(item==null)return;
        if(item.taskMode && item.taskCompleted && occurrence!=item.taskCompletedOccurrence && item.recurrenceMode!=RecurrenceUtils.NONE){
            item.taskCompleted=false;new NoForgetStore(c).save(item);
        }
    }
}
