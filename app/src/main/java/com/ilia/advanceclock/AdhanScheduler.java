package com.ilia.advanceclock;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.Calendar;
import java.util.TimeZone;

public final class AdhanScheduler {
    public static final int FAJR = 0;
    public static final int DHUHR = 1;
    public static final int ASR = 2;
    public static final int MAGHRIB = 3;
    public static final int ISHA = 4;
    private static final String STATE_PREFS = "advance_clock_adhan_schedule_state";

    public enum Status {
        DISABLED,
        LOCATION_MISSING,
        EXACT_PERMISSION_MISSING,
        NOTIFICATION_PERMISSION_MISSING,
        SCHEDULE_FAILED,
        SCHEDULED
    }

    private AdhanScheduler() {}

    public static void rescheduleAll(Context context) {
        if (!AppSettings.adhanEnabled(context)) {
            for (int type = FAJR; type <= ISHA; type++) cancel(context, type);
            return;
        }
        for (int type = FAJR; type <= ISHA; type++) {
            cancel(context, type);
            if (isEnabled(context, type)) scheduleNext(context, type);
        }
    }

    public static boolean scheduleNext(Context context, int type) {
        if (!AppSettings.adhanEnabled(context)
                || !AppSettings.prayerLocationSet(context) || !isEnabled(context, type)) {
            setScheduled(context, type, false);
            return false;
        }
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null || !PermissionHelper.exactAlarmsGranted(context)) {
            setScheduled(context, type, false);
            return false;
        }

        long now = System.currentTimeMillis();
        Calendar day = Calendar.getInstance(AppSettings.prayerTimeZone(context));
        for (int offset = 0; offset <= 2; offset++) {
            if (offset > 0) day.add(Calendar.DAY_OF_YEAR, 1);
            long when = prayerMillis(context, day, type);
            if (when <= now) continue;
            try {
                manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, when, operation(context, type));
            } catch (SecurityException ignored) {
                setScheduled(context, type, false);
                return false;
            }
            setScheduled(context, type, true);
            return true;
        }
        setScheduled(context, type, false);
        return false;
    }

    public static Status status(Context context) {
        if (!AppSettings.adhanEnabled(context) || !anyEnabled(context)) return Status.DISABLED;
        if (!AppSettings.prayerLocationSet(context)) return Status.LOCATION_MISSING;
        if (!PermissionHelper.exactAlarmsGranted(context)) {
            return Status.EXACT_PERMISSION_MISSING;
        }
        if (AppSettings.adhanNotification(context)
                && Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return Status.NOTIFICATION_PERMISSION_MISSING;
        }
        for (int type = FAJR; type <= ISHA; type++) {
            if (isEnabled(context, type) && !isScheduled(context, type)) {
                return Status.SCHEDULE_FAILED;
            }
        }
        return Status.SCHEDULED;
    }

    public static String title(int type) {
        if (type == FAJR) return "اذان صبح";
        if (type == DHUHR) return "اذان ظهر";
        if (type == ASR) return "عصر";
        if (type == MAGHRIB) return "اذان مغرب";
        return "عشاء";
    }

    private static long prayerMillis(Context context, Calendar day, int type) {
        TimeZone zone = AppSettings.prayerTimeZone(context);
        PrayerTimeCalculator.Times times = PrayerTimeCalculator.calculate(
                day.getTimeInMillis(),
                AppSettings.prayerLatitude(context),
                AppSettings.prayerLongitude(context),
                zone);
        int minutes = type == FAJR ? times.fajrMinutes
                : type == DHUHR ? times.dhuhrMinutes
                : type == ASR ? times.asrMinutes
                : type == MAGHRIB ? times.maghribMinutes : times.ishaMinutes;
        if (minutes < 0) return -1L;
        Calendar result = Calendar.getInstance(zone);
        result.clear();
        result.set(
                day.get(Calendar.YEAR), day.get(Calendar.MONTH),
                day.get(Calendar.DAY_OF_MONTH), minutes / 60, minutes % 60, 0);
        return result.getTimeInMillis();
    }

    private static boolean isEnabled(Context context, int type) {
        if (type == FAJR) return AppSettings.fajrAdhanEnabled(context);
        if (type == DHUHR) return AppSettings.dhuhrAdhanEnabled(context);
        if (type == ASR) return AppSettings.asrAdhanEnabled(context);
        if (type == MAGHRIB) return AppSettings.maghribAdhanEnabled(context);
        return AppSettings.ishaAdhanEnabled(context);
    }

    private static boolean anyEnabled(Context context) {
        return isEnabled(context, FAJR)
                || isEnabled(context, DHUHR)
                || isEnabled(context, ASR)
                || isEnabled(context, MAGHRIB)
                || isEnabled(context, ISHA);
    }

    private static boolean isScheduled(Context context, int type) {
        return context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
                .getBoolean("scheduled_" + type, false);
    }

    private static void setScheduled(Context context, int type, boolean value) {
        context.getSharedPreferences(STATE_PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean("scheduled_" + type, value).apply();
    }

    private static PendingIntent operation(Context context, int type) {
        Intent intent = new Intent(context, AdhanReceiver.class).putExtra("adhanType", type);
        return PendingIntent.getBroadcast(
                context, 3_100_000 + type, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void cancel(Context context, int type) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) manager.cancel(operation(context, type));
        setScheduled(context, type, false);
    }
}
