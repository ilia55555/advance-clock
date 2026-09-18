package com.ilia.advanceclock;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;

public final class NotificationHelper {
    public static final String ALARM_CHANNEL = "advance_clock_alarms_v1";
    private NotificationHelper() {}

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                ALARM_CHANNEL,
                "آلارم‌ها",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("آلارم‌های Advance Clock");
        channel.enableVibration(false);
        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        channel.setSound(null, new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build());
        manager.createNotificationChannel(channel);
    }

    public static int notificationId(long alarmId) {
        return (int) (20000 + Math.abs(alarmId % 1_000_000));
    }
}
