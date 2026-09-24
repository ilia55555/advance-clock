package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class ToolAlarmScheduler {
    public static final String TIMER = "timer";
    public static final String STOPWATCH = "stopwatch";
    private static final String PREFS = "time_tools";

    private ToolAlarmScheduler() {}

    public static boolean schedule(Context context, String kind, long deadline, String label) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(kind + "_deadline", deadline)
                .putString(kind + "_label", label)
                .apply();
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null || !PermissionHelper.exactAlarmsGranted(context)) return false;
        PendingIntent operation = pendingIntent(context, kind);
        PendingIntent show = PendingIntent.getActivity(context, requestCode(kind) + 1,
                new Intent(context, MainActivity.class)
                        .putExtra("openTab", kind)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            manager.setAlarmClock(new AlarmManager.AlarmClockInfo(deadline, show), operation);
            return true;
        } catch (SecurityException ignored) {
            return false;
        }
    }

    public static void cancel(Context context, String kind) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) manager.cancel(pendingIntent(context, kind));
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(kind + "_deadline").remove(kind + "_label").apply();
    }

    public static void rescheduleAll(Context context) {
        long now = System.currentTimeMillis();
        for (String kind : new String[]{TIMER, STOPWATCH}) {
            long deadline = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getLong(kind + "_deadline", 0L);
            if (deadline > now) {
                String label = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getString(kind + "_label", defaultLabel(kind));
                schedule(context, kind, deadline, label);
            } else if (deadline > 0L) {
                schedule(context, kind, now + 1_000L,
                        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                .getString(kind + "_label", defaultLabel(kind)));
            }
        }
    }

    static String savedLabel(Context context, String kind) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(kind + "_label", defaultLabel(kind));
    }

    static void markFired(Context context, String kind) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .remove(kind + "_deadline").remove(kind + "_label").apply();
    }

    private static PendingIntent pendingIntent(Context context, String kind) {
        return PendingIntent.getBroadcast(context, requestCode(kind),
                new Intent(context, ToolAlarmReceiver.class).putExtra("toolKind", kind),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(String kind) {
        return TIMER.equals(kind) ? 2_310_001 : 2_310_002;
    }

    static long alarmId(String kind) {
        return TIMER.equals(kind) ? -2_310_001L : -2_310_002L;
    }

    static String defaultLabel(String kind) {
        return TIMER.equals(kind) ? "تایمر تمام شد" : "کرنومتر به حد نهایی رسید";
    }
}
