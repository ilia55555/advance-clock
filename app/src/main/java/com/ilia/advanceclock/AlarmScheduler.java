package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class AlarmScheduler {
    private AlarmScheduler() {}

    public static boolean schedule(Context context, AlarmItem item) {
        if (!item.enabled) {
            cancel(context, item.id);
            return true;
        }
        long now = System.currentTimeMillis();
        long normalized = TimeUtils.normalizeFuture(item.triggerAtMillis, item.repeatType, now);
        if (normalized <= now) return false;
        if (normalized != item.triggerAtMillis) {
            item.triggerAtMillis = normalized;
            new AlarmStore(context).save(item);
        }

        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null || !PermissionHelper.exactAlarmsGranted(context)) return false;

        PendingIntent operation = alarmPendingIntent(context, item.id);
        PendingIntent showIntent = PendingIntent.getActivity(
                context,
                requestCode(item.id) + 1,
                new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        try {
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(item.triggerAtMillis, showIntent), operation);
            return true;
        } catch (SecurityException error) {
            return false;
        }
    }

    public static void cancel(Context context, long id) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) manager.cancel(alarmPendingIntent(context, id));
    }

    public static void rescheduleAll(Context context) {
        AlarmStore store = new AlarmStore(context);
        long now = System.currentTimeMillis();
        for (AlarmItem item : store.all()) {
            if (!item.enabled) continue;
            long normalized = TimeUtils.normalizeFuture(item.triggerAtMillis, item.repeatType, now);
            if (item.repeatType == AlarmItem.REPEAT_NONE && normalized <= now) {
                item.enabled = false;
                store.save(item);
                continue;
            }
            if (normalized != item.triggerAtMillis) {
                item.triggerAtMillis = normalized;
                store.save(item);
            }
            schedule(context, item);
        }
        ClockWidgetProvider.updateAll(context);
    }

    private static PendingIntent alarmPendingIntent(Context context, long id) {
        Intent intent = new Intent(context, AlarmReceiver.class).putExtra("alarmId", id);
        return PendingIntent.getBroadcast(
                context,
                requestCode(id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    private static int requestCode(long id) {
        return (int) (10000 + Math.abs(id % 1_000_000));
    }
}
