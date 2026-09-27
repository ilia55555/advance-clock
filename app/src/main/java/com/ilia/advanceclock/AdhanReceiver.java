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
import android.os.VibrationEffect;
import android.os.Vibrator;

public final class AdhanReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int type = intent == null
                ? AdhanScheduler.FAJR
                : intent.getIntExtra("adhanType", AdhanScheduler.FAJR);
        NotificationHelper.ensureChannels(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        boolean canNotify = Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
        if (manager != null && canNotify) {
            PendingIntent open = PendingIntent.getActivity(
                    context, 3_200_000 + type,
                    new Intent(context, MainActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            Notification notification = new Notification.Builder(
                    context, NotificationHelper.ADHAN_CHANNEL)
                    .setSmallIcon(R.drawable.ic_alarm)
                    .setContentTitle(AdhanScheduler.title(type))
                    .setContentText(AppSettings.prayerLocationLabel(context))
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .setCategory(Notification.CATEGORY_ALARM)
                    .build();
            manager.notify(3_300_000 + type, notification);
        }
        if (AppSettings.adhanVibrate(context)) {
            Vibrator vibrator = context.getSystemService(Vibrator.class);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(VibrationEffect.createWaveform(
                        new long[]{0, 700, 350, 700}, -1));
            }
        }
        AdhanScheduler.scheduleNext(context, type);
    }
}
