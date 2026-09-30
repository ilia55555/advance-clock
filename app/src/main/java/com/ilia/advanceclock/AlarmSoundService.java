package com.ilia.advanceclock;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
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
    public static final String ACTION_MUTE = "com.ilia.advanceclock.MUTE_ALARM";
    public static final String ACTION_UNMUTE = "com.ilia.advanceclock.UNMUTE_ALARM";

    private MediaPlayer player;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private long currentAlarmId = -1L;
    private boolean currentVibrate = true;
    private String currentSoundUri = "";
    private boolean muted;

    private static final String ACTION_VOLUME_CHANGED =
            "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_VOLUME_STREAM_TYPE =
            "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String EXTRA_VOLUME_STREAM_VALUE =
            "android.media.EXTRA_VOLUME_STREAM_VALUE";
    private static final String EXTRA_PREV_VOLUME_STREAM_VALUE =
            "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) muteCurrentAlarm();
        }
    };

    private final BroadcastReceiver volumeReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || !ACTION_VOLUME_CHANGED.equals(intent.getAction())) return;
            int stream = intent.getIntExtra(EXTRA_VOLUME_STREAM_TYPE, -1);
            int current = intent.getIntExtra(EXTRA_VOLUME_STREAM_VALUE, -1);
            int previous = intent.getIntExtra(EXTRA_PREV_VOLUME_STREAM_VALUE, -1);
            if (current < 0 || previous < 0
                    || (stream != AudioManager.STREAM_ALARM
                    && stream != AudioManager.STREAM_MUSIC
                    && stream != AudioManager.STREAM_RING)) {
                return;
            }
            if (current < previous) {
                muteCurrentAlarm();
            } else if (current > previous) {
                unmuteCurrentAlarm();
            }
        }
    };

    public static Intent startIntent(Context context, long id, String label) {
        return startIntent(context, id, label, true, "");
    }

    public static Intent startIntent(Context context, long id, String label, boolean vibrate) {
        return startIntent(context, id, label, vibrate, "");
    }

    public static Intent startIntent(
            Context context, long id, String label, boolean vibrate, String soundUri) {
        return new Intent(context, AlarmSoundService.class)
                .setAction(ACTION_START)
                .putExtra("alarmId", id)
                .putExtra("label", label == null ? "" : label)
                .putExtra("vibrate", vibrate)
                .putExtra("soundUri", soundUri == null ? "" : soundUri);
    }

    public static Intent stopIntent(Context context) {
        return new Intent(context, AlarmSoundService.class).setAction(ACTION_STOP);
    }

    public static Intent muteIntent(Context context) {
        return new Intent(context, AlarmSoundService.class).setAction(ACTION_MUTE);
    }

    public static Intent unmuteIntent(Context context) {
        return new Intent(context, AlarmSoundService.class).setAction(ACTION_UNMUTE);
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationHelper.ensureChannel(this);
        IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        IntentFilter volumeFilter = new IntentFilter(ACTION_VOLUME_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, screenFilter, RECEIVER_NOT_EXPORTED);
            registerReceiver(volumeReceiver, volumeFilter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, screenFilter);
            registerReceiver(volumeReceiver, volumeFilter);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        if (ACTION_STOP.equals(intent.getAction())) {
            stopAlarm();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_MUTE.equals(intent.getAction())) {
            muteCurrentAlarm();
            return START_NOT_STICKY;
        }
        if (ACTION_UNMUTE.equals(intent.getAction())) {
            unmuteCurrentAlarm();
            return START_NOT_STICKY;
        }

        currentAlarmId = intent.getLongExtra("alarmId", -1L);
        currentVibrate = intent.getBooleanExtra("vibrate", true);
        currentSoundUri = intent.getStringExtra("soundUri");
        String label = intent.getStringExtra("label");
        String toolKind = intent.getStringExtra("toolKind");

        startForeground(
                NotificationHelper.notificationId(currentAlarmId),
                buildNotification(currentAlarmId, label, toolKind)
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

    private Notification buildNotification(long id, String label, String toolKind) {
        Intent ring = new Intent(this, AlarmRingActivity.class)
                .putExtra("alarmId", id)
                .putExtra("label", label)
                .putExtra("toolKind", toolKind)
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

        String title = label == null || label.trim().isEmpty() ? AppString.get(R.string.runtime_text_0019) : label;
        String message = ToolAlarmScheduler.TIMER.equals(toolKind)
                ? AppString.get(R.string.runtime_text_0573)
                : ToolAlarmScheduler.STOPWATCH.equals(toolKind)
                ? AppString.get(R.string.runtime_text_0302)
                : AppString.get(R.string.runtime_text_0574);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, NotificationHelper.ALARM_CHANNEL)
                : new Notification.Builder(this);

        builder.setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(title)
                .setContentText(message)
                .setCategory(Notification.CATEGORY_ALARM)
                .setVisibility(AppSettings.notificationVisibility(this))
                .setOngoing(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(fullScreen)
                .addAction(new Notification.Action.Builder(
                        null, AppString.get(R.string.runtime_text_0406), stopAction).build());
        if (shouldOpenFullscreen()) builder.setFullScreenIntent(fullScreen, true);
        return builder.build();
    }

    private boolean shouldOpenFullscreen() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        return locked ? AppSettings.alarmFullscreenLocked(this)
                : AppSettings.alarmFullscreenUnlocked(this);
    }

    private void startSound() {
        stopPlayer();
        muted = false;
        try {
            Uri uri = currentSoundUri == null || currentSoundUri.isEmpty()
                    ? Uri.parse(AppSettings.defaultAlarmSoundUri(this))
                    : Uri.parse(currentSoundUri);
            if (uri == null || uri.toString().isEmpty())
                uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            player.setDataSource(this, uri);
            player.setLooping(true);
            player.prepare();
            player.setVolume(muted ? 0f : 1f, muted ? 0f : 1f);
            player.start();
        } catch (Exception ignored) {
            stopPlayer();
        }
    }

    private void muteCurrentAlarm() {
        muted = true;
        try {
            if (player != null) player.setVolume(0f, 0f);
        } catch (Exception ignored) {
        }
    }

    private void unmuteCurrentAlarm() {
        muted = false;
        try {
            if (player != null) player.setVolume(1f, 1f);
        } catch (Exception ignored) {
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
        try { unregisterReceiver(screenReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(volumeReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
