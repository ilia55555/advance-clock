package com.ilia.advanceclock;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Typeface;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public final class DateNotificationService extends Service {
    public static final String CHANNEL = "advance_clock_date_v1";
    private static final int ID = 73;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean timeReceiverRegistered;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            refreshNow(DateNotificationService.this);
            scheduleNextMinute();
        }
    };

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            // Manual date/time changes and midnight must replace the icon
            // immediately instead of waiting for the minute ticker.
            handler.removeCallbacks(refresh);
            refreshNow(DateNotificationService.this);
            scheduleNextMinute();
        }
    };

    public static void start(Context context) {
        if (!AppSettings.persistentDateNotificationEnabled(context)
                || (Build.VERSION.SDK_INT >= 33
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED)) {
            stop(context);
            return;
        }

        Intent intent = new Intent(context, DateNotificationService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    public static void stop(Context context) {
        try {
            context.stopService(new Intent(context, DateNotificationService.class));
        } catch (Exception ignored) {
        }
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) manager.cancel(ID);
    }

    public static void refreshNow(Context context) {
        if (!AppSettings.persistentDateNotificationEnabled(context)) {
            stop(context);
            return;
        }
        ensureChannel(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;
        try {
            manager.notify(ID, buildNotification(context));
        } catch (SecurityException ignored) {
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        if (!AppSettings.persistentDateNotificationEnabled(this)) {
            stopSelf();
            return;
        }
        ensureChannel(this);
        startForeground(ID, buildNotification(this));
        registerTimeReceiver();

        handler.removeCallbacks(refresh);
        handler.post(refresh);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (!AppSettings.persistentDateNotificationEnabled(this)) {
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        refreshNow(this);
        handler.removeCallbacks(refresh);
        scheduleNextMinute();
        return START_STICKY;
    }

    private void registerTimeReceiver() {
        if (timeReceiverRegistered) return;

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(timeReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(timeReceiver, filter);
        }
        timeReceiverRegistered = true;
    }

    private void scheduleNextMinute() {
        long now = System.currentTimeMillis();
        long delay = 60_000L - Math.floorMod(now, 60_000L) + 60L;
        handler.postDelayed(refresh, delay);
    }

    private static void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < 26) return;

        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel = new NotificationChannel(
                CHANNEL,
                "تاریخ روز",
                NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("نمایش دائمی تاریخ روز در نوار وضعیت");
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

        String title = CalendarUtils.formatDate(now, selectedType);

        StringBuilder twoOtherDates = new StringBuilder();
        if (AppSettings.persistentDateExtraCalendars(context)) {
            for (int type = CalendarUtils.PERSIAN;
                 type <= CalendarUtils.HIJRI;
                 type++) {
                if (type == selectedType) continue;
                if (twoOtherDates.length() > 0) twoOtherDates.append("\n");
                twoOtherDates.append(CalendarUtils.calendarName(type))
                        .append(": ")
                        .append(CalendarUtils.formatDate(now, type));
            }
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL)
                : new Notification.Builder(context);

        builder.setContentTitle(title)
                .setContentText(twoOtherDates.length() == 0
                        ? "تاریخ امروز"
                        : twoOtherDates.toString().replace("\n", "  •  "))
                .setContentIntent(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(AppSettings.notificationVisibility(context))
                .setPriority(Notification.PRIORITY_LOW);

        if (twoOtherDates.length() > 0) {
            builder.setStyle(new Notification.BigTextStyle()
                    .bigText(twoOtherDates.toString()));
        }

        if (Build.VERSION.SDK_INT >= 23) {
            builder.setSmallIcon(Icon.createWithBitmap(dayIcon(day)));
        } else {
            builder.setSmallIcon(R.drawable.ic_alarm);
        }
        return builder.build();
    }

    private static Bitmap dayIcon(int day) {
        Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        circle.setColor(Color.WHITE);
        canvas.drawCircle(48f, 48f, 47f, circle);

        Paint digits = new Paint(Paint.ANTI_ALIAS_FLAG);
        digits.setTextAlign(Paint.Align.CENTER);
        digits.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        digits.setTextSize(day >= 10 ? 78f : 88f);
        digits.setLetterSpacing(-0.06f);
        digits.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        String value = CalendarUtils.fa(day);
        Paint.FontMetrics fm = digits.getFontMetrics();
        float y = 48f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(value, 48f, y, digits);
        digits.setXfermode(null);

        return bitmap;
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (timeReceiverRegistered) {
            try {
                unregisterReceiver(timeReceiver);
            } catch (Exception ignored) {
            }
            timeReceiverRegistered = false;
        }
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
