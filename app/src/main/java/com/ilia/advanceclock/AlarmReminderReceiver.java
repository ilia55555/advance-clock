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

public final class AlarmReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long alarmId = intent.getLongExtra("alarmId", -1L);
        int minutes = intent.getIntExtra("delayMinutes", 0);
        AlarmItem item = new AlarmStore(context).find(alarmId);
        if (item == null) return;
        if (!AlarmReminderUtils.effective(item.reminderMode, item.reminderMinutesJson)
                .contains(minutes)) return;

        if (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        NotificationHelper.ensureChannels(context);

        Intent edit = new Intent(context, AlarmEditorActivity.class)
                .putExtra("alarmId", alarmId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(
                context,
                2_100_000 + (int) Math.abs((alarmId + minutes) % 700_000),
                edit,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = item.label == null || item.label.trim().isEmpty()
                ? "یادآوری هشدار" : item.label;
        String content = minutes > 0
                ? "یادآوری " + AlarmReminderUtils.labelForMinutes(minutes) + " بعد از هشدار"
                : "یادآوری هشدار";

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, NotificationHelper.REMINDER_CHANNEL)
                : new Notification.Builder(context);

        Notification notification = builder
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setContentText(content)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(open)
                .build();

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(
                    2_000_000 + (int) Math.abs((alarmId + minutes * 31L) % 900_000),
                    notification);
        }
    }
}
