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
    private final AlarmRingTimeout ringTimeout = new AlarmRingTimeout();
    private MediaPlayer player;
    private Vibrator vibrator;
    private static final String ACTION_MUTE = "com.ilia.advanceclock.MUTE_NOTE";
    private static final String ACTION_UNMUTE = "com.ilia.advanceclock.UNMUTE_NOTE";
    private boolean muted, active;
    private final android.content.BroadcastReceiver muteEvents = new android.content.BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) setMuted(true);
            else {
                int current = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_VALUE", -1);
                int previous = intent.getIntExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE", -1);
                int stream = intent.getIntExtra("android.media.EXTRA_VOLUME_STREAM_TYPE", -1);
                if (current >= 0 && previous >= 0 && current != previous
                        && (stream == android.media.AudioManager.STREAM_ALARM || stream == android.media.AudioManager.STREAM_MUSIC || stream == android.media.AudioManager.STREAM_RING)) setMuted(current < previous);
            }
        }
    };
    static Intent volumeIntent(Context context, boolean mute) {
        return new Intent(context, NoForgetSoundService.class).setAction(mute ? ACTION_MUTE : ACTION_UNMUTE);
    }
    private void setMuted(boolean value) {
        muted = value;
        try { if (player != null) player.setVolume(value ? 0 : 1, value ? 0 : 1); } catch (Exception ignored) {}
    }

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
        android.content.IntentFilter filter = new android.content.IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction("android.media.VOLUME_CHANGED_ACTION");
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(muteEvents, filter, RECEIVER_NOT_EXPORTED);
        else registerReceiver(muteEvents, filter);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || ACTION_STOP.equals(intent.getAction())) {
            stopReminder();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_MUTE.equals(intent.getAction()) || ACTION_UNMUTE.equals(intent.getAction())) {
            if (active) setMuted(ACTION_MUTE.equals(intent.getAction()));
            else stopSelf();
            return START_NOT_STICKY;
        }
        long id = intent.getLongExtra("noteId", -1L);
        NoForgetItem item = new NoForgetStore(this).find(id);
        if (item == null) { stopSelf(); return START_NOT_STICKY; }
        stopReminder();
        startForeground(NotificationHelper.reminderNotificationId(id), buildNotification(item));
        active = true;
        ringTimeout.start(this, () -> {
            NoteReminderActivity.finishIfShowing(id);
            stopReminder();
            stopSelf();
        });
        muted = false;
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
        String notificationTitle = item.title.trim().isEmpty()
                ? AppString.get(R.string.runtime_text_0311) : item.title;
        String notificationBody = item.body.trim().isEmpty()
                ? AppString.get(R.string.runtime_text_0312) : item.body;
        builder.setSmallIcon(R.drawable.ic_note)
                .setContentTitle(notificationTitle)
                .setContentText(notificationBody)
                .setStyle(new Notification.BigTextStyle().bigText(notificationBody))
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(AppSettings.notificationVisibility(this)).setPriority(Notification.PRIORITY_MAX)
                .setOngoing(true).setContentIntent(open)
                .addAction(new Notification.Action.Builder(
                        null, AppString.get(R.string.runtime_text_0406), stop).build());
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
            player.setDataSource(this, uri); player.setLooping(true); player.prepare(); player.setVolume(muted ? 0 : 1, muted ? 0 : 1); player.start();
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
        active = false;
        ringTimeout.cancel();
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

    @Override public void onDestroy() {
        try { unregisterReceiver(muteEvents); } catch (Exception ignored) {}
        stopReminder(); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
