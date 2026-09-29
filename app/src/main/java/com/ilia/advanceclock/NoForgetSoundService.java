package com.ilia.advanceclock;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public final class NoForgetSoundService extends Service {
    private static final String ACTION_START = "com.ilia.advanceclock.START_NOTE_REMINDER";
    static final String ACTION_STOP = "com.ilia.advanceclock.STOP_NOTE_REMINDER";
    private MediaPlayer player;
    private Vibrator vibrator;

    static Intent startIntent(Context context, long noteId) {
        return new Intent(context, NoForgetSoundService.class).setAction(ACTION_START)
                .putExtra("noteId", noteId);
    }

    static Intent stopIntent(Context context) {
        return new Intent(context, NoForgetSoundService.class).setAction(ACTION_STOP);
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationHelper.ensureChannels(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            stopReminder();
            stopSelf();
            return START_NOT_STICKY;
        }
        long id = intent.getLongExtra("noteId", -1L);
        NoForgetItem item = new NoForgetStore(this).find(id);
        if (item == null) { stopSelf(); return START_NOT_STICKY; }
        startForeground(NotificationHelper.reminderNotificationId(id), buildNotification(item));
        play(item.soundUri);
        if (item.vibrate) vibrate();
        return START_NOT_STICKY;
    }

    private Notification buildNotification(NoForgetItem item) {
        Intent show = new Intent(this, NoteReminderActivity.class).putExtra("noteId", item.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent open = PendingIntent.getActivity(this,
                1_310_000 + (int) Math.abs(item.id % 1_000_000), show,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this,
                1_320_000 + (int) Math.abs(item.id % 1_000_000), stopIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, NotificationHelper.NOTE_ALARM_CHANNEL)
                : new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.ic_note)
                .setContentTitle(UiText.trComposite(this, item.title.trim().isEmpty()
                        ? "یادآوری یادداشت" : CalendarUtils.fa(item.title)))
                .setContentText(UiText.trComposite(this, item.body.trim().isEmpty()
                        ? "زمان یادداشت شما رسیده است" : CalendarUtils.fa(item.body)))
                .setStyle(new Notification.BigTextStyle().bigText(
                        UiText.trComposite(this, CalendarUtils.fa(item.body))))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(AppSettings.notificationVisibility(this)).setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true).setContentIntent(open)
                .addAction(new Notification.Action.Builder(
                        null, UiText.tr(this, "قطع"), stop).build());
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        if (locked ? item.fullscreenLocked : item.fullscreenUnlocked)
            builder.setFullScreenIntent(open, true);
        return builder.build();
    }

    private void play(String selected) {
        try {
            String value = selected == null || selected.isEmpty()
                    ? AppSettings.defaultAlarmSoundUri(this) : selected;
            Uri uri = value == null || value.isEmpty() ? null : Uri.parse(value);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setDataSource(this, uri); player.setLooping(true); player.prepare(); player.start();
        } catch (Exception ignored) { releasePlayer(); }
    }

    private void vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager manager = getSystemService(VibratorManager.class);
                vibrator = manager == null ? null : manager.getDefaultVibrator();
            } else vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (vibrator != null) vibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{0, 700, 300, 700, 300}, 1));
        } catch (Exception ignored) {}
    }

    private void stopReminder() {
        releasePlayer();
        try { if (vibrator != null) vibrator.cancel(); } catch (Exception ignored) {}
        vibrator = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private void releasePlayer() {
        try { if (player != null) player.stop(); } catch (Exception ignored) {}
        try { if (player != null) player.release(); } catch (Exception ignored) {}
        player = null;
    }

    @Override public void onDestroy() { stopReminder(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}
