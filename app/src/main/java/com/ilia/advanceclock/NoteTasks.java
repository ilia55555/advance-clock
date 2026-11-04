package com.ilia.advanceclock;

import android.content.Context;

/** Completion is local and cancels only the completed occurrence, never the recurrence. */
final class NoteTasks {
    interface Callback {void finished(NoForgetItem item,RuntimeException error);}
    static final java.util.concurrent.ExecutorService WORK=java.util.concurrent.Executors.newSingleThreadExecutor(task->{
        Thread thread=new Thread(task,"task-completion");thread.setDaemon(true);return thread;
    });
    static void setCompletedAsync(Context context,long id,boolean completed,Callback callback){
        Context app=context.getApplicationContext();long requestedAt=System.currentTimeMillis();
        WORK.execute(()->{
            NoForgetItem result=null;RuntimeException failure=null;
            try{result=setCompleted(app,id,completed,requestedAt);}
            catch(RuntimeException error){
                failure=error;android.util.Log.e("NoteTasks","Cannot update task",error);
                try{result=new NoForgetStore(app).find(id);}catch(RuntimeException ignored){}
            }
            NoForgetItem saved=result;RuntimeException error=failure;
            new android.os.Handler(android.os.Looper.getMainLooper()).post(()->callback.finished(saved,error));
        });
    }

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
        item.lastFiredAtMillis=0;store.save(item);NoForgetScheduler.cancel(c,id);
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
