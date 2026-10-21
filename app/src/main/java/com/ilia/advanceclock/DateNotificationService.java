package com.ilia.advanceclock;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.text.BidiFormatter;
import android.view.View;

public final class DateNotificationService extends Service {
    public static final String CHANNEL = "advance_clock_date_v1";
    private static final int ID = 73;

    public static void start(Context context) {
        if (!AppSettings.persistentDateNotificationEnabled(context)
                || (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)) {
            stop(context);
            return;
        }

        refreshNow(context);
        stopLegacyService(context);
    }

    public static void stop(Context context) {
        stopLegacyService(context);
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        PendingIntent refresh = refreshOperation(context, PendingIntent.FLAG_NO_CREATE);
        if (refresh != null) {
            if (alarms != null) alarms.cancel(refresh);
            refresh.cancel();
        }
        NotificationManager manager =
                context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.cancel(ID);
        }
    }

    private static void stopLegacyService(Context context) {
        try {
            context.stopService(
                    new Intent(
                            context,
                            DateNotificationService.class));
        } catch (Exception ignored) {
        }
    }

    public static void refreshNow(Context context) {
        if (!AppSettings.persistentDateNotificationEnabled(context)
                || (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)) {
            stop(context);
            return;
        }

        ensureChannel(context);
        NotificationManager manager =
                context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        if (!manager.areNotificationsEnabled() || isChannelBlocked(context)) {
            stop(context);
            return;
        }

        try {
            manager.notify(
                    ID,
                    buildNotification(context));
            scheduleNextRefresh(context);
        } catch (RuntimeException error) {
            android.util.Log.e("DateNotification", "Unable to post date notification", error);
        }
    }

    static boolean isChannelBlocked(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = manager == null ? null : manager.getNotificationChannel(CHANNEL);
        return channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE;
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {
        refreshNow(this);
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL,
                AppString.get(R.string.runtime_text_0666),
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(AppString.get(R.string.runtime_text_0667));
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        channel.setSound(null, null);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
    }

    private static Notification buildNotification(Context context) {
        int selectedType = AppSettings.defaultCalendar(context);
        long now = System.currentTimeMillis();

        android.icu.util.Calendar selected =
                CalendarUtils.fromMillis(selectedType, now);
        int day = selected.get(android.icu.util.Calendar.DAY_OF_MONTH);

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(
                context,
                73,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);

        int[] weekdays = {
                R.string.runtime_text_0181, R.string.runtime_text_0182,
                R.string.runtime_text_0183, R.string.runtime_text_0184,
                R.string.runtime_text_0185, R.string.runtime_text_0186,
                R.string.runtime_text_0180
        };
        String weekday = AppString.get(weekdays[selected.get(android.icu.util.Calendar.DAY_OF_WEEK) - 1]);
        String title = weekday + " " + CalendarUtils.formatDate(now, selectedType);
        BidiFormatter bidi = BidiFormatter.getInstance(AppSettings.layoutDirection(context) == View.LAYOUT_DIRECTION_RTL);

        StringBuilder twoOtherDates = new StringBuilder();
        if (AppSettings.persistentDateExtraCalendars(context)) {
            for (int type = CalendarUtils.PERSIAN;
                 type <= CalendarUtils.HIJRI;
                 type++) {
                if (type == selectedType) continue;
                if (twoOtherDates.length() > 0) twoOtherDates.append("  •  ");
                twoOtherDates.append(bidi.unicodeWrap(CalendarUtils.formatDate(now, type)));
            }
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL)
                : new Notification.Builder(context);

        builder.setContentTitle(title)
                .setContentText(twoOtherDates.toString())
                .setContentIntent(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(AppSettings.notificationVisibility(context))
                .setPriority(Notification.PRIORITY_LOW)
                .setColor(AppSettings.primaryColor(context))
                .setColorized(false)
                .setStyle(new Notification.BigTextStyle().bigText(twoOtherDates.toString()));

        // Use resource-backed monochrome icons for System UI/OEM compatibility; keep the day number.
        builder.setSmallIcon(DAY_ICONS[Math.max(1, Math.min(31, day)) - 1]);
        return builder.build();
    }

    private static PendingIntent refreshOperation(Context context, int flags) {
        return PendingIntent.getBroadcast(context, 74,
                new Intent(context, DateNotificationRefreshReceiver.class)
                        .setAction("com.ilia.advanceclock.REFRESH_DATE_NOTIFICATION"),
                flags | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void scheduleNextRefresh(Context context) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) return;
        long midnight = DateNotificationSchedule.nextMidnight(System.currentTimeMillis(), java.time.ZoneId.systemDefault());
        PendingIntent operation = refreshOperation(context, PendingIntent.FLAG_UPDATE_CURRENT);
        if (PermissionHelper.exactAlarmsGranted(context)) {
            try {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight, operation);
                return;
            } catch (SecurityException ignored) { /* Permission may change while scheduling. */ }
        }
        // Keep a refresh scheduled even when exact alarms have not been granted.
        try { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, midnight, operation); }
        catch (SecurityException ignored) {}
    }

    private static final int[] DAY_ICONS = {
        R.drawable.ic_status_day_01,
        R.drawable.ic_status_day_02,
        R.drawable.ic_status_day_03,
        R.drawable.ic_status_day_04,
        R.drawable.ic_status_day_05,
        R.drawable.ic_status_day_06,
        R.drawable.ic_status_day_07,
        R.drawable.ic_status_day_08,
        R.drawable.ic_status_day_09,
        R.drawable.ic_status_day_10,
        R.drawable.ic_status_day_11,
        R.drawable.ic_status_day_12,
        R.drawable.ic_status_day_13,
        R.drawable.ic_status_day_14,
        R.drawable.ic_status_day_15,
        R.drawable.ic_status_day_16,
        R.drawable.ic_status_day_17,
        R.drawable.ic_status_day_18,
        R.drawable.ic_status_day_19,
        R.drawable.ic_status_day_20,
        R.drawable.ic_status_day_21,
        R.drawable.ic_status_day_22,
        R.drawable.ic_status_day_23,
        R.drawable.ic_status_day_24,
        R.drawable.ic_status_day_25,
        R.drawable.ic_status_day_26,
        R.drawable.ic_status_day_27,
        R.drawable.ic_status_day_28,
        R.drawable.ic_status_day_29,
        R.drawable.ic_status_day_30,
        R.drawable.ic_status_day_31
    };
}
