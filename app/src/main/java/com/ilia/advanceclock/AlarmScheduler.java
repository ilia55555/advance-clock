package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

public final class AlarmScheduler {
    private AlarmScheduler(){}

    public static boolean schedule(Context context, AlarmItem item) {
        return schedulePrimary(context, item, true);
    }

    private static boolean schedulePrimary(Context context, AlarmItem item, boolean normalize) {
        if (!item.enabled) {
            cancelPrimary(context, item.id);
            return true;
        }

        long now = System.currentTimeMillis();
        if (normalize) {
            long normalized = RecurrenceUtils.next(
                    item.triggerAtMillis,
                    item.recurrenceMode,
                    item.intervalDays,
                    item.customDatesJson,
                    now);
            if (normalized <= now) {
                cancelPrimary(context, item.id);
                item.enabled = false;
                new AlarmStore(context).save(item);
                return false;
            }
            if (normalized != item.triggerAtMillis) {
                item.triggerAtMillis = normalized;
                new AlarmStore(context).save(item);
            }
        } else if (item.triggerAtMillis <= now) {
            return false;
        }

        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null || !PermissionHelper.exactAlarmsGranted(context)) return false;

        PendingIntent operation = alarmPendingIntent(context, item.id);
        PendingIntent show = PendingIntent.getActivity(
                context,
                requestCode(item.id) + 1,
                new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        try {
            manager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(item.triggerAtMillis, show),
                    operation);
            return true;
        } catch (SecurityException e) {
            return false;
        }
    }

    /**
     * Called when the primary alarm actually fires. Automatic reminders are
     * scheduled relative to that occurrence, so recurring alarms never cancel
     * the reminders that still belong to the occurrence that just rang.
     */
    public static void schedulePostReminders(Context context, AlarmItem item, long firedAtMillis) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return;
        // Editing the selection must also remove old alarms from the system's next-alarm display.
        cancelPostReminders(context, item.id, firedAtMillis);
        if (!PermissionHelper.exactAlarmsGranted(context)) return;

        List<Integer> reminders = AlarmReminderUtils.effective(
                item.reminderMode, item.reminderMinutesJson);

        for (int i = 0; i < reminders.size(); i++) {
            int minutes = reminders.get(i);
            long when = firedAtMillis + minutes * 60_000L;
            if (when <= System.currentTimeMillis()) continue;

            Intent intent = new Intent(context, AlarmReminderReceiver.class)
                    .putExtra("alarmId", item.id)
                    .putExtra("delayMinutes", minutes)
                    .putExtra("firedAtMillis", firedAtMillis);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    reminderRequestCode(item.id, firedAtMillis, minutes),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                PendingIntent show = PendingIntent.getActivity(context, requestCode(item.id) + 1,
                        new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                manager.setAlarmClock(new AlarmManager.AlarmClockInfo(when, show), pi);
            } catch (SecurityException ignored) {
            }
        }
    }

    public static boolean snooze(Context context, long id, long delayMillis) {
        AlarmItem item = new AlarmStore(context).find(id);
        if (item == null) return false;
        return scheduleSnoozeAt(context, item, System.currentTimeMillis() + Math.max(60_000L, delayMillis));
    }

    private static boolean scheduleSnoozeAt(Context context, AlarmItem item, long when) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null || !PermissionHelper.exactAlarmsGranted(context)) return false;
        Intent intent = new Intent(context, AlarmSnoozeReceiver.class).putExtra("alarmId", item.id);
        PendingIntent operation = PendingIntent.getBroadcast(context,
                2_050_000 + (int) Math.abs(item.id % 800_000L), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, when, operation);
            item.pendingSnoozeAtMillis = when;
            new AlarmStore(context).save(item);
            return true;
        } catch (SecurityException error) { return false; }
    }

    public static void cancel(Context context, long id) {
        AlarmStore store = new AlarmStore(context);
        AlarmItem item = store.find(id);
        cancelPrimary(context, id);
        if (item != null) {
            if (item.lastFiredAtMillis > 0L) cancelPostReminders(context, item.id, item.lastFiredAtMillis);
            AlarmManager manager = context.getSystemService(AlarmManager.class);
            if (manager != null) manager.cancel(PendingIntent.getBroadcast(context,
                    2_050_000 + (int) Math.abs(id % 800_000L),
                    new Intent(context, AlarmSnoozeReceiver.class).putExtra("alarmId", id),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            item.lastFiredAtMillis = 0L;
            item.pendingSnoozeAtMillis = 0L;
            store.save(item);
        }
    }

    private static void cancelPostReminders(Context context, long id, long firedAtMillis) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null) return;

        for (int minutes : AlarmReminderUtils.VALUES) {
            Intent intent = new Intent(context, AlarmReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    reminderRequestCode(id, firedAtMillis, minutes),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            manager.cancel(pi);
            pi.cancel();
        }
    }

    private static void cancelPrimary(Context context, long id) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) manager.cancel(alarmPendingIntent(context, id));
    }

    public static void rescheduleAll(Context context) {
        AlarmStore store = new AlarmStore(context);
        long now = System.currentTimeMillis();
        store.deleteExpired(now);

        for (AlarmItem item : store.all()) {
            if (item.pendingSnoozeAtMillis > 0L)
                scheduleSnoozeAt(context, item, Math.max(now + 1000L, item.pendingSnoozeAtMillis));
            if (item.lastFiredAtMillis > 0L) {
                schedulePostReminders(context, item, item.lastFiredAtMillis);
            }
            if (!item.enabled) continue;

            long next = RecurrenceUtils.next(
                    item.triggerAtMillis,
                    item.recurrenceMode,
                    item.intervalDays,
                    item.customDatesJson,
                    now);

            if (next <= now) {
                item.enabled = false;
                store.save(item);
                continue;
            }

            if (next != item.triggerAtMillis) {
                item.triggerAtMillis = next;
                store.save(item);
            }
            schedulePrimary(context, item, false);
        }
        ClockWidgetProvider.updateAll(context);
    }

    private static PendingIntent alarmPendingIntent(Context context, long id) {
        Intent intent = new Intent(context, AlarmReceiver.class).putExtra("alarmId", id);
        return PendingIntent.getBroadcast(
                context,
                requestCode(id),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(long id) {
        return (int) (10000 + Math.abs(id % 1_000_000));
    }

    private static int reminderRequestCode(long id, long firedAt, int minutes) {
        long mixed = id
                ^ (id >>> 32)
                ^ firedAt
                ^ (firedAt >>> 32)
                ^ (minutes * 2654435761L);
        return 1_100_000 + (int) Math.abs(mixed % 900_000L);
    }
}
