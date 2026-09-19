package com.ilia.advanceclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    private int lastDay = -1;

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            updateNotification();
            handler.postDelayed(this, 60_000L);
        }
    };

    public static void start(Context context) {
        Intent intent = new Intent(context, DateNotificationService.class);
        if (Build.VERSION.SDK_INT >= 26) context.startForegroundService(intent);
        else context.startService(intent);
    }

    @Override public void onCreate() {
        super.onCreate();
        ensureChannel();
        startForeground(ID, buildNotification());
        handler.post(refresh);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        updateNotification();
        return START_STICKY;
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL, "تاریخ روز", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("نمایش دائمی تاریخ روز در نوار وضعیت");
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        int type = AppSettings.defaultCalendar(this);
        long now = System.currentTimeMillis();
        android.icu.util.Calendar c = CalendarUtils.fromMillis(type, now);
        int day = c.get(android.icu.util.Calendar.DAY_OF_MONTH);
        lastDay = day;

        Intent open = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(
                this, 73, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);

        builder.setContentTitle(CalendarUtils.formatDate(now, type))
                .setContentText(
                        CalendarUtils.formatDate(now, CalendarUtils.PERSIAN)
                                + "  •  " + CalendarUtils.formatDate(now, CalendarUtils.GREGORIAN)
                                + "  •  " + CalendarUtils.formatDate(now, CalendarUtils.HIJRI))
                .setContentIntent(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= 23) builder.setSmallIcon(Icon.createWithBitmap(dayIcon(day)));
        else builder.setSmallIcon(R.drawable.ic_alarm);
        return builder.build();
    }

    private Bitmap dayIcon(int day) {
        Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        p.setTextSize(day >= 10 ? 58f : 68f);
        Paint.FontMetrics fm = p.getFontMetrics();
        float y = 48f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(String.valueOf(day), 48f, y, p);
        return bitmap;
    }

    private void updateNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) manager.notify(ID, buildNotification());
    }

    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
