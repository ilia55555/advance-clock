package com.ilia.advanceclock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;

public final class NotificationHelper {
    public static final String ALARM_CHANNEL = "advance_clock_alarms_v1";
    public static final String REMINDER_CHANNEL = "advance_clock_reminders_v1";

    private NotificationHelper() {}

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel alarm = new NotificationChannel(
                ALARM_CHANNEL, "آلارم‌ها", NotificationManager.IMPORTANCE_HIGH);
        alarm.setDescription("آلارم‌های Advance Clock");
        alarm.enableVibration(false);
        alarm.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        alarm.setSound(null, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        manager.createNotificationChannel(alarm);

        NotificationChannel reminders = new NotificationChannel(
                REMINDER_CHANNEL, "یادآوری‌ها", NotificationManager.IMPORTANCE_HIGH);
        reminders.setDescription("یادآوری هشدارها و یادداشت‌ها");
        reminders.enableVibration(true);
        reminders.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(reminders);
    }

    public static void ensureChannel(Context context) {
        ensureChannels(context);
    }

    public static int notificationId(long alarmId) {
        return (int) (20000 + Math.abs(alarmId % 1_000_000));
    }

    public static int reminderNotificationId(long noteId) {
        return (int) (1_200_000 + Math.abs(noteId % 1_000_000));
    }
}
