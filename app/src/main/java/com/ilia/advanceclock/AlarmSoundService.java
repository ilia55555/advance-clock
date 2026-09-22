package com.ilia.advanceclock;

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
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

public final class AlarmSoundService extends Service {
    public static final String ACTION_START = "com.ilia.advanceclock.START_ALARM";
    public static final String ACTION_STOP = "com.ilia.advanceclock.STOP_ALARM";

    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private long currentAlarmId = -1L;
    private boolean currentVibrate = true;

    public static Intent startIntent(Context context, long id, String label) {
        return startIntent(context, id, label, true);
    }

    public static Intent startIntent(Context context, long id, String label, boolean vibrate) {
        return new Intent(context, AlarmSoundService.class)
                .setAction(ACTION_START)
                .putExtra("alarmId", id)
                .putExtra("label", label == null ? "" : label)
                .putExtra("vibrate", vibrate);
    }

    public static Intent stopIntent(Context context) {
        return new Intent(context, AlarmSoundService.class).setAction(ACTION_STOP);
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationHelper.ensureChannel(this);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_STOP.equals(intent.getAction())) {
            stopAlarm();
            stopSelf();
            return START_NOT_STICKY;
        }

        currentAlarmId = intent.getLongExtra("alarmId", -1L);
        currentVibrate = intent.getBooleanExtra("vibrate", true);
        String label = intent.getStringExtra("label");

        startForeground(
                NotificationHelper.notificationId(currentAlarmId),
                buildNotification(currentAlarmId, label)
        );
        acquireWakeLock();
        startSound();
        if (currentVibrate) {
            startVibration();
        } else {
            stopVibration();
        }
        return START_NOT_STICKY;
    }

    private Notification buildNotification(long id, String label) {
        Intent ring = new Intent(this, AlarmRingActivity.class)
                .putExtra("alarmId", id)
                .putExtra("label", label)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreen = PendingIntent.getActivity(
                this,
                30000 + (int) Math.abs(id % 1_000_000),
                ring,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stop = new Intent(this, StopAlarmReceiver.class).putExtra("alarmId", id);
        PendingIntent stopAction = PendingIntent.getBroadcast(
                this,
                40000 + (int) Math.abs(id % 1_000_000),
                stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String title = label == null || label.trim().isEmpty() ? "آلارم" : label;
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, NotificationHelper.ALARM_CHANNEL)
                : new Notification.Builder(this);

        return builder
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setContentText("زمان آلارم رسیده است")
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(AppSettings.notificationVisibility(this))
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setFullScreenIntent(fullScreen, true)
                .setContentIntent(fullScreen)
                .addAction(new Notification.Action.Builder(null, "قطع", stopAction).build())
                .build();
    }

    private void startSound() {
        stopPlayer();
        try {
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.start();
        } catch (Exception ignored) {
            stopPlayer();
        }
    }

    private void startVibration() {
        stopVibration();
        try {
            if (Build.VERSION.SDK_INT >= 31) {
                VibratorManager manager = getSystemService(VibratorManager.class);
                vibrator = manager == null ? null : manager.getDefaultVibrator();
            } else {
                vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            }

            if (vibrator != null) {
                long[] pattern = {0, 700, 300, 700, 300};
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 1));
            }
        } catch (Exception ignored) {
        }
    }

    private void stopVibration() {
        try {
            if (vibrator != null) vibrator.cancel();
        } catch (Exception ignored) {
        }
        vibrator = null;
    }

    private void acquireWakeLock() {
        try {
            PowerManager pm = getSystemService(PowerManager.class);
            if (pm != null) {
                wakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "AdvanceClock:AlarmWake"
                );
                wakeLock.acquire(10 * 60 * 1000L);
            }
        } catch (Exception ignored) {
        }
    }

    private void stopAlarm() {
        stopPlayer();
        stopVibration();
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ignored) {
        }
        wakeLock = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
    }

    private void stopPlayer() {
        try {
            if (player != null) player.stop();
        } catch (Exception ignored) {
        }
        try {
            if (player != null) player.release();
        } catch (Exception ignored) {
        }
        player = null;
    }

    @Override public void onDestroy() {
        stopAlarm();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
