package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.content.*;
import android.net.Uri;

/** Private immutable widget actions; old or duplicate clicks cannot complete a later occurrence. */
public final class NoteWidgetTaskReceiver extends BroadcastReceiver {
    static String action(Context c){return c.getPackageName()+".WIDGET_NOTE_TASK";}
    static Intent intent(Context c,int widgetId,NoForgetItem item){
        boolean done=item.taskDone(System.currentTimeMillis());
        return new Intent(c,NoteWidgetTaskReceiver.class).setAction(action(c))
                .setData(Uri.parse("advanceclock://note-widget-task/"+widgetId+"/"+item.id+"/"+item.dueAtMillis+"/"+item.taskCompletedOccurrence+"/"+done))
                .putExtra("widgetId",widgetId).putExtra("noteId",item.id).putExtra("due",item.dueAtMillis)
                .putExtra("completedOccurrence",item.taskCompletedOccurrence).putExtra("wasDone",done).putExtra("done",!done);
    }
    static PendingIntent pending(Context c,int widgetId,NoForgetItem item){
        return PendingIntent.getBroadcast(c,0,intent(c,widgetId,item),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    @Override public void onReceive(Context context,Intent intent){
        if(intent==null || !action(context).equals(intent.getAction()))return;
        Context app=context.getApplicationContext();PendingResult pending=goAsync();
        try{NoteTasks.WORK.execute(()->{
            try{if(!apply(app,intent))NoForgetWidgetProvider.updateAll(app);}
            catch(RuntimeException error){android.util.Log.e("NoteWidgetTasks","Cannot update task",error);NoForgetWidgetProvider.updateAll(app);}
            finally{if(pending!=null)pending.finish();}
        });}catch(RuntimeException error){if(pending!=null)pending.finish();throw error;}
    }
    static boolean apply(Context c,Intent intent){
        if(intent==null || !action(c).equals(intent.getAction()) || intent.getIntExtra("widgetId",-1)<=0)return false;
        android.appwidget.AppWidgetProviderInfo info=android.appwidget.AppWidgetManager.getInstance(c).getAppWidgetInfo(intent.getIntExtra("widgetId",-1));
        if(info==null || !new ComponentName(c,NoForgetWidgetProvider.class).equals(info.provider))return false;
        synchronized(NoteTasks.class){
            NoForgetItem item=new NoForgetStore(c).find(intent.getLongExtra("noteId",-1));
            if(item==null || !item.taskMode || item.dueAtMillis!=intent.getLongExtra("due",-1)
                    || item.taskCompletedOccurrence!=intent.getLongExtra("completedOccurrence",-1)
                    || item.taskDone(System.currentTimeMillis())!=intent.getBooleanExtra("wasDone",false)
                    || intent.getBooleanExtra("done",false)==intent.getBooleanExtra("wasDone",false))return false;
            return NoteTasks.setCompleted(c,item.id,intent.getBooleanExtra("done",false))!=null;
        }
    }
}
