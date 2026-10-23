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
import android.graphics.Color;
import android.widget.RemoteViews;
import android.os.Build;
import android.os.IBinder;
import android.text.BidiFormatter;
import android.view.View;

public final class DateNotificationService extends Service {
    public static final String CHANNEL = "advance_clock_date_v1";
    private static final int ID = 73;

    /** Restore a missing or stale date card on app entry. */
    static void ensureVisible(Context context) {
        if (!AppSettings.persistentDateNotificationEnabled(context)) {
            stop(context);
            return;
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            for (android.service.notification.StatusBarNotification notification : manager.getActiveNotifications()) {
                if (notification.getId() == ID && notification.getTag() == null) {
                    Notification current = notification.getNotification();
                    Notification expected = buildNotification(context);
                    if (android.text.TextUtils.equals(current.extras.getCharSequence(Notification.EXTRA_TITLE),
                            expected.extras.getCharSequence(Notification.EXTRA_TITLE))
                            && android.text.TextUtils.equals(current.extras.getCharSequence(Notification.EXTRA_TEXT),
                            expected.extras.getCharSequence(Notification.EXTRA_TEXT))) return;
                    break;
                }
            }
        }
        start(context);
    }

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
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setCustomContentView(dateView(context, R.layout.notification_date, title, twoOtherDates.toString(), day))
                .setCustomBigContentView(dateView(context, R.layout.notification_date_expanded, title, twoOtherDates.toString(), day));

        // Use resource-backed monochrome icons for System UI/OEM compatibility; keep the day number.
        builder.setSmallIcon(dayIconResource(context, day));
        return builder.build();
    }

    private static RemoteViews dateView(Context context, int layout, String title, String otherDates, int day) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        int primary = AppSettings.primaryColor(context);
        int text = Color.luminance(primary) > 0.179f ? Color.BLACK : Color.WHITE;
        views.setInt(R.id.date_notification_background, "setColorFilter", primary);
        views.setInt(R.id.date_notification_day_border, "setColorFilter", primary);
        views.setInt(R.id.date_notification_day_border, "setImageAlpha", 255);
        views.setTextColor(R.id.date_notification_day, primary);
        views.setTextColor(R.id.date_notification_title, text);
        views.setTextColor(R.id.date_notification_other_dates, text);
        views.setTextViewText(R.id.date_notification_title, title);
        views.setTextViewText(R.id.date_notification_other_dates, otherDates);
        views.setTextViewText(R.id.date_notification_day, dayLabel(context, day));
        if (useEnglishDay(context)) {
            // Latin digits occupy more of the badge than Persian digits. Reserve
            // an inner inset so TextView autosizing leaves the outline clear.
            int inset = Math.round(6 * context.getResources().getDisplayMetrics().density);
            views.setViewPadding(R.id.date_notification_day, inset, inset, inset, inset);
        }
        views.setViewVisibility(R.id.date_notification_other_dates, otherDates.isEmpty() ? View.GONE : View.VISIBLE);
        // The first child is the day badge: left in English, right in Persian/Arabic.
        int direction = AppSettings.layoutDirection(context);
        views.setInt(R.id.date_notification_row, "setLayoutDirection", direction);
        views.setInt(R.id.date_notification_text, "setLayoutDirection", direction);
        if (layout == R.layout.notification_date_expanded) {
            views.setTextViewText(R.id.date_notification_settings,
                    context.getString(R.string.date_notification_settings));
            views.setTextColor(R.id.date_notification_settings, primary);
            Intent settings = new Intent(context, SettingsActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            views.setOnClickPendingIntent(R.id.date_notification_settings,
                    PendingIntent.getActivity(context, 75, settings,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            views.setTextViewText(R.id.date_notification_calendar,
                    context.getString(R.string.date_notification_calendar));
            views.setTextColor(R.id.date_notification_calendar, primary);
            Intent calendar = new Intent(context, MainActivity.class)
                    .putExtra("openTab", "clock")
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            views.setOnClickPendingIntent(R.id.date_notification_calendar,
                    PendingIntent.getActivity(context, 76, calendar,
                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        }
        // Text direction comes from XML: setTextDirection is not permitted by RemoteViews.
        // Calling it through setInt causes System UI to reject the whole notification.
        return views;
    }

    static boolean useEnglishDay(Context context) {
        return AppSettings.defaultCalendar(context) == CalendarUtils.GREGORIAN
                || AppSettings.LANGUAGE_ENGLISH.equals(AppSettings.language(context));
    }

    static String dayLabel(Context context, int day) {
        String value = Integer.toString(day);
        if (useEnglishDay(context)) return value;
        StringBuilder localized = new StringBuilder();
        for (char digit : value.toCharArray()) localized.append((char) ('۰' + digit - '0'));
        return localized.toString();
    }

    static int dayIconResource(Context context, int day) {
        return (useEnglishDay(context) ? DAY_ICONS : PERSIAN_DAY_ICONS)[Math.max(1, Math.min(31, day)) - 1];
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
        long midnight = DateNotificationSchedule.nextMidnight(System.currentTimeMillis(), AppTime.zone().toZoneId());
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
    private static final int[] PERSIAN_DAY_ICONS = {
        R.drawable.ic_status_day_fa_01,
        R.drawable.ic_status_day_fa_02,
        R.drawable.ic_status_day_fa_03,
        R.drawable.ic_status_day_fa_04,
        R.drawable.ic_status_day_fa_05,
        R.drawable.ic_status_day_fa_06,
        R.drawable.ic_status_day_fa_07,
        R.drawable.ic_status_day_fa_08,
        R.drawable.ic_status_day_fa_09,
        R.drawable.ic_status_day_fa_10,
        R.drawable.ic_status_day_fa_11,
        R.drawable.ic_status_day_fa_12,
        R.drawable.ic_status_day_fa_13,
        R.drawable.ic_status_day_fa_14,
        R.drawable.ic_status_day_fa_15,
        R.drawable.ic_status_day_fa_16,
        R.drawable.ic_status_day_fa_17,
        R.drawable.ic_status_day_fa_18,
        R.drawable.ic_status_day_fa_19,
        R.drawable.ic_status_day_fa_20,
        R.drawable.ic_status_day_fa_21,
        R.drawable.ic_status_day_fa_22,
        R.drawable.ic_status_day_fa_23,
        R.drawable.ic_status_day_fa_24,
        R.drawable.ic_status_day_fa_25,
        R.drawable.ic_status_day_fa_26,
        R.drawable.ic_status_day_fa_27,
        R.drawable.ic_status_day_fa_28,
        R.drawable.ic_status_day_fa_29,
        R.drawable.ic_status_day_fa_30,
        R.drawable.ic_status_day_fa_31
    };
}
