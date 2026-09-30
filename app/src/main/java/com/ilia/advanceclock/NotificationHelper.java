package com.ilia.advanceclock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;

public final class NotificationHelper {
    public static final String ALARM_CHANNEL = "advance_clock_alarms_v1";
    public static final String REMINDER_CHANNEL = "advance_clock_reminders_v1";
    public static final String NOTE_ALARM_CHANNEL = "advance_clock_note_alarms_v1";
    public static final String ADHAN_CHANNEL = "advance_clock_adhan_v1";

    private NotificationHelper() {}

    public static void ensureChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel alarm = new NotificationChannel(
                ALARM_CHANNEL, AppString.get(R.string.runtime_text_0313),
                NotificationManager.IMPORTANCE_HIGH);
        alarm.setDescription(AppString.get(R.string.runtime_text_0412));
        alarm.enableVibration(false);
        alarm.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        alarm.setSound(null, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        manager.createNotificationChannel(alarm);

        NotificationChannel reminders = new NotificationChannel(
                REMINDER_CHANNEL, AppString.get(R.string.runtime_text_0021),
                NotificationManager.IMPORTANCE_HIGH);
        reminders.setDescription(AppString.get(R.string.runtime_text_0413));
        reminders.enableVibration(true);
        reminders.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(reminders);

        NotificationChannel noteAlarms = new NotificationChannel(
                NOTE_ALARM_CHANNEL, AppString.get(R.string.runtime_text_0315),
                NotificationManager.IMPORTANCE_HIGH);
        noteAlarms.setDescription(AppString.get(R.string.runtime_text_0316));
        noteAlarms.enableVibration(false);
        noteAlarms.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        noteAlarms.setSound(null, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).build());
        manager.createNotificationChannel(noteAlarms);

        NotificationChannel adhan = new NotificationChannel(
                ADHAN_CHANNEL, AppString.get(R.string.runtime_text_0077), NotificationManager.IMPORTANCE_HIGH);
        adhan.setDescription(AppString.get(R.string.runtime_text_0314));
        adhan.enableVibration(false);
        adhan.setSound(null, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM).build());
        manager.createNotificationChannel(adhan);
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
