package com.ilia.advanceclock;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NoForgetReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        long id=intent.getLongExtra("noteId",-1L);
        NoForgetStore store=new NoForgetStore(context);
        NoForgetItem item=store.find(id);
        if(item==null)return;

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

        if (!AppSettings.noteReminderNotificationsEnabled(context)) return;
        if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;

        NotificationHelper.ensureChannels(context);
        Intent open=new Intent(context,NoForgetEditorActivity.class).putExtra("noteId",id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content=PendingIntent.getActivity(context,1_300_000+(int)Math.abs(id%1_000_000),open,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder b=Build.VERSION.SDK_INT>=26
                ?new Notification.Builder(context,NotificationHelper.REMINDER_CHANNEL):new Notification.Builder(context);
        Notification n=b.setSmallIcon(R.drawable.ic_note)
                .setContentTitle(item.title.trim().isEmpty()?"یادآوری":item.title)
                .setContentText(item.body.trim().isEmpty()?"زمان یادداشت شما رسیده است":item.body)
                .setStyle(new Notification.BigTextStyle().bigText(item.body))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(AppSettings.notificationVisibility(context))
                .setPriority(Notification.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(content).build();
        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(manager!=null)manager.notify(NotificationHelper.reminderNotificationId(id),n);
    }
}
