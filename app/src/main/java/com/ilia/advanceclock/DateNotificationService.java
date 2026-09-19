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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
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

    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            updateNotification();
            long now = System.currentTimeMillis();
            long nextMinute = 60_000L - (now % 60_000L) + 80L;
            handler.postDelayed(this, nextMinute);
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
        handler.removeCallbacksAndMessages(null);
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
        channel.setSound(null, null);
        channel.enableVibration(false);
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        int selectedType = AppSettings.defaultCalendar(this);
        long now = System.currentTimeMillis();

        android.icu.util.Calendar selected = CalendarUtils.fromMillis(selectedType, now);
        int day = selected.get(android.icu.util.Calendar.DAY_OF_MONTH);

        Intent open = new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent content = PendingIntent.getActivity(
                this, 73, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = CalendarUtils.formatDate(now, selectedType);

        StringBuilder twoOtherDates = new StringBuilder();
        for (int type = CalendarUtils.PERSIAN; type <= CalendarUtils.HIJRI; type++) {
            if (type == selectedType) continue;
            if (twoOtherDates.length() > 0) twoOtherDates.append("\n");
            twoOtherDates.append(CalendarUtils.calendarName(type))
                    .append(": ")
                    .append(CalendarUtils.formatDate(now, type));
        }

        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL)
                : new Notification.Builder(this);

        builder.setContentTitle(title)
                .setContentText(twoOtherDates.toString().replace("\n", "  •  "))
                .setStyle(new Notification.BigTextStyle().bigText(twoOtherDates.toString()))
                .setContentIntent(content)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_STATUS)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setPriority(Notification.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= 23) {
            builder.setSmallIcon(Icon.createWithBitmap(dayIcon(day)));
        } else {
            builder.setSmallIcon(R.drawable.ic_alarm);
        }
        return builder.build();
    }

    /**
     * Android status-bar icons are rendered as a monochrome alpha mask.
     * We therefore draw an opaque rounded square and cut the Persian day
     * digits out of it. On a dark status bar this appears exactly as a
     * white tile with dark/transparent Persian digits.
     */
    private Bitmap dayIcon(int day) {
        Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint circle = new Paint(Paint.ANTI_ALIAS_FLAG);
        circle.setColor(Color.WHITE);
        canvas.drawCircle(48f, 48f, 47f, circle);

        Paint digits = new Paint(Paint.ANTI_ALIAS_FLAG);
        digits.setTextAlign(Paint.Align.CENTER);
        digits.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        digits.setTextSize(day >= 10 ? 67f : 76f);
        digits.setLetterSpacing(-0.06f);
        digits.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        String value = CalendarUtils.fa(day);
        Paint.FontMetrics fm = digits.getFontMetrics();
        float y = 48f - (fm.ascent + fm.descent) / 2f;
        canvas.drawText(value, 48f, y, digits);
        digits.setXfermode(null);

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

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
