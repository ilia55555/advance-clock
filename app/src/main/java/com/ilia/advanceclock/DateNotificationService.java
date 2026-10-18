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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.IBinder;
import android.text.BidiFormatter;
import android.view.View;
import android.widget.RemoteViews;

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

        try {
            manager.notify(
                    ID,
                    buildNotification(context));
            scheduleNextRefresh(context);
        } catch (SecurityException ignored) {
        }
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

        // minSdk is 26: the actual status-bar small icon always contains today's day.
        builder.setSmallIcon(Icon.createWithBitmap(dayIcon(day)));
        return builder.build();
    }

    private static RemoteViews dateView(Context context, int layout, String title, String otherDates, int day) {
        RemoteViews views = new RemoteViews(context.getPackageName(), layout);
        int primary = AppSettings.primaryColor(context);
        int text = Color.luminance(primary) > 0.179f ? Color.BLACK : Color.WHITE;
        views.setInt(R.id.date_notification_background, "setColorFilter", primary);
        views.setInt(R.id.date_notification_day_border, "setColorFilter", primary);
        views.setTextColor(R.id.date_notification_day, primary);
        views.setTextColor(R.id.date_notification_title, text);
        views.setTextColor(R.id.date_notification_other_dates, text);
        views.setTextViewText(R.id.date_notification_title, title);
        views.setTextViewText(R.id.date_notification_other_dates, otherDates);
        views.setTextViewText(R.id.date_notification_day, CalendarUtils.fa(day));
        views.setViewVisibility(R.id.date_notification_other_dates, otherDates.isEmpty() ? View.GONE : View.VISIBLE);
        // The badge stays on the left in both layout directions; only the text direction follows the language.
        int direction = AppSettings.layoutDirection(context);
        views.setInt(R.id.date_notification_text, "setLayoutDirection", direction);
        int textDirection = direction == View.LAYOUT_DIRECTION_RTL ? View.TEXT_DIRECTION_RTL : View.TEXT_DIRECTION_LTR;
        views.setInt(R.id.date_notification_title, "setTextDirection", textDirection);
        views.setInt(R.id.date_notification_other_dates, "setTextDirection", textDirection);
        return views;
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

    private static Bitmap dayIcon(int day) {
        Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        circle.setColor(Color.WHITE);
        canvas.drawCircle(48f, 48f, 46f, circle);

        Paint digits = new Paint(Paint.ANTI_ALIAS_FLAG);
        digits.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        digits.setTextSize(day >= 10 ? 74f : 88f);
        String value = CalendarUtils.fa(day);
        Rect bounds = new Rect();
        digits.getTextBounds(value, 0, value.length(), bounds);
        float fit = Math.min(1f, Math.min(68f / Math.max(1, bounds.width()), 66f / Math.max(1, bounds.height())));
        digits.setTextSize(digits.getTextSize() * fit);
        digits.getTextBounds(value, 0, value.length(), bounds);
        // Small icons are alpha masks: transparent digits remain legible after system tinting.
        digits.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        canvas.drawText(value, 48f - bounds.exactCenterX(), 48f - bounds.exactCenterY(), digits);
        digits.setXfermode(null);
        return bitmap;
    }
}
