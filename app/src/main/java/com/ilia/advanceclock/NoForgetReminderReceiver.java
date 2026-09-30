package com.ilia.advanceclock;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
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

        if(intent.getBooleanExtra("preReminder",false)){
            showPreReminder(
                    context,
                    item,
                    Math.max(1,intent.getIntExtra("reminderMinutes",15)));
            return;
        }

        Intent service=NoForgetSoundService.startIntent(context,id);
        if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);
        else context.startService(service);

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

    private static void showPreReminder(
            Context context,
            NoForgetItem item,
            int minutes){
        NotificationHelper.ensureChannels(context);
        Intent openIntent=new Intent(context,NoForgetEditorActivity.class)
                .putExtra("noteId",item.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open=PendingIntent.getActivity(
                context,
                1_500_000+(int)Math.abs(item.id%1_000_000L),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder=Build.VERSION.SDK_INT>=26
                ?new Notification.Builder(context,NotificationHelper.REMINDER_CHANNEL)
                :new Notification.Builder(context);
        String title=item.title.trim().isEmpty()
                ?"یادآوری یادداشت"
                :CalendarUtils.fa(item.title);
        builder.setSmallIcon(R.drawable.ic_note)
                .setContentTitle(UiText.trComposite(context, title))
                .setContentText(
                        UiText.trComposite(context, "تا زمان یادداشت "
                                +AlarmReminderUtils.labelForMinutes(minutes)
                                +" مانده است"))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(AppSettings.notificationVisibility(context))
                .setAutoCancel(true)
                .setContentIntent(open);

        NotificationManager manager=context.getSystemService(NotificationManager.class);
        if(manager!=null){
            int notificationId=1_600_000
                    +(int)Math.abs((item.id+minutes*31L)%1_000_000L);
            manager.notify(notificationId,builder.build());
        }
    }
}
