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

    private static java.lang.ref.WeakReference<AlarmSoundService> runningService = new java.lang.ref.WeakReference<>(null);
    private boolean playing, foreground;
    private String currentLabel;
    static void onRingVisibilityChanged() {
        AlarmSoundService service=runningService.get();
        if(service!=null) service.updateNotificationVisibility();
    }
    private void updateNotificationVisibility() {
        if(!playing) return;
        if(AlarmRingActivity.isVisible(currentAlarmId)) {
            if(foreground) { stopForeground(STOP_FOREGROUND_REMOVE);foreground=false; }
        } else if(!foreground) {
            startForeground(NotificationHelper.notificationId(currentAlarmId),
                    buildNotification(currentAlarmId,currentLabel,currentToolKind,false));
            foreground=true;
        }
    }
    private final AlarmRingTimeout ringTimeout = new AlarmRingTimeout();
    private String currentToolKind, currentAlarmPayload;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private long currentAlarmId = -1L;
    private static volatile long ringingAlarmId = Long.MIN_VALUE;
    static boolean isRinging(long id) { return ringingAlarmId == id; }
    private boolean currentVibrate = true;
    private String currentSoundUri = "";
    private boolean muted;
    private String currentToolPayload;

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
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) silenceCurrentAlarm();
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
                silenceCurrentAlarm();
            } else if (current > previous) {
                unmuteCurrentAlarm();
            }
        }
    };

    static void ring(Context context, AlarmItem item) {
        Intent service = startIntent(context, item.id, item.label, item.vibrate, item.soundUri);
        try {
            String payload = item.toJson().toString();
            if (payload.length() <= 64_000) service.putExtra("alarmPayload", payload);
        } catch (org.json.JSONException ignored) {}
        long previous = ringingAlarmId;
        ringingAlarmId = item.id;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
            else context.startService(service);
        } catch (RuntimeException error) {
            ringingAlarmId = previous;
            throw error;
        }
    }

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

    private static AlarmSoundService output(long id) {
        AlarmSoundService service=runningService.get();
        return service!=null && service.playing && service.currentAlarmId == id ? service : null;
    }
    static boolean outputMuted(long id) { AlarmSoundService service=output(id);return service!=null && service.muted; }
    static boolean outputVibrating(long id) { AlarmSoundService service=output(id);return service!=null && service.currentVibrate; }
    static void setOutputMuted(long id,boolean value) {
        AlarmSoundService service=output(id);if(service==null)return;
        if(value) service.muteCurrentAlarm(); else service.unmuteCurrentAlarm();
    }
    static void setOutputVibrating(long id,boolean value) {
        AlarmSoundService service=output(id);if(service==null)return;
        service.currentVibrate=value;if(value) service.startVibration(); else service.stopVibration();
        AlertOutputEvents.changed();
    }

    @Override public void onCreate() {
        super.onCreate();
        runningService=new java.lang.ref.WeakReference<>(this);
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
            silenceCurrentAlarm();
            return START_NOT_STICKY;
        }
        if (ACTION_UNMUTE.equals(intent.getAction())) {
            unmuteCurrentAlarm();
            return START_NOT_STICKY;
        }

        currentAlarmId = intent.getLongExtra("alarmId", -1L);
        ringingAlarmId = currentAlarmId;
        currentVibrate = intent.getBooleanExtra("vibrate", true);
        currentSoundUri = intent.getStringExtra("soundUri");
        String label = intent.getStringExtra("label");
        currentLabel=label;
        String toolKind = intent.getStringExtra("toolKind");
        currentToolKind = toolKind;
        currentAlarmPayload = intent.getStringExtra("alarmPayload");
        currentToolPayload = intent.getStringExtra("toolPayload");

        startForeground(
                NotificationHelper.notificationId(currentAlarmId),
                buildNotification(currentAlarmId, label, toolKind, !AlarmRingActivity.isVisible(currentAlarmId))
        );
        playing=true; foreground=true;
        updateNotificationVisibility();
        openScreenIfAllowed(currentAlarmId, label, toolKind);
        ringTimeout.start(this, () -> {
            AlarmRingActivity.finishIfShowing(currentAlarmId);
            stopAlarm();
            stopSelf();
        });
        acquireWakeLock();
        startSound();
        if (currentVibrate) {
            startVibration();
        } else {
            stopVibration();
        }
        AlertOutputEvents.changed();
        return START_NOT_STICKY;
    }

    private Notification buildNotification(long id, String label, String toolKind) {
        return buildNotification(id,label,toolKind,true);
    }
    private Notification buildNotification(long id, String label, String toolKind, boolean allowFullscreen) {
        Intent ring = new Intent(this, AlarmRingActivity.class)
                .putExtra("alarmId", id)
                .putExtra("label", label)
                .putExtra("toolKind", toolKind)
                .putExtra("alarmPayload", currentAlarmPayload)
                .putExtra("toolPayload", currentToolPayload)
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
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentIntent(fullScreen)
                .addAction(new Notification.Action.Builder(
                        null, AppString.get(R.string.runtime_text_0406), stopAction).build());
        if (Build.VERSION.SDK_INT >= 31)
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        if (allowFullscreen && shouldOpenFullscreen()) builder.setFullScreenIntent(fullScreen, true);
        return builder.build();
    }

    private void openScreenIfAllowed(long id, String label, String toolKind) {
        if (AlarmRingActivity.isVisible(id)) return;
        if (!shouldOpenFullscreen()) return;
        try {
            startActivity(new Intent(this, AlarmRingActivity.class)
                    .putExtra("alarmId", id).putExtra("label", label).putExtra("toolKind", toolKind)
                    .putExtra("alarmPayload", currentAlarmPayload)
                .putExtra("toolPayload", currentToolPayload)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        } catch (RuntimeException ignored) {
            // Keep the full-screen notification as the system-controlled fallback.
        }
    }

    private boolean shouldOpenFullscreen() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        return locked ? AppSettings.alarmFullscreenLocked(this)
                : AppSettings.alarmFullscreenUnlocked(this);
    }

    private void startSound() {
        muted = false;
        String kind = ToolAlarmScheduler.TIMER.equals(currentToolKind)
                || ToolAlarmScheduler.STOPWATCH.equals(currentToolKind)
                ? currentToolKind : AlarmPreparationReceiver.CLOCK;
        AlarmAudioEngine.play(this, AlarmPreparationReceiver.key(kind, currentAlarmId), currentSoundUri);
    }

    private void silenceCurrentAlarm() {
        muteCurrentAlarm();
        currentVibrate=false;
        stopVibration();
        AlertOutputEvents.changed();
    }

    private void muteCurrentAlarm() {
        muted = true;
        AlarmAudioEngine.mute(true);
        AlertOutputEvents.changed();
    }

    private void unmuteCurrentAlarm() {
        muted = false;
        AlarmAudioEngine.mute(false);
        AlertOutputEvents.changed();
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
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
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
        playing=false; foreground=false;
        AlertOutputEvents.changed();
        ringTimeout.cancel();
        stopPlayer();
        stopVibration();
        try {
            if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        } catch (Exception ignored) {
        }
        wakeLock = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        if (ringingAlarmId == currentAlarmId) ringingAlarmId = Long.MIN_VALUE;
        if (currentAlarmId >= 0L) {
            currentAlarmId = -1L;
            if (new AlarmStore(this).deleteExpired(System.currentTimeMillis()) > 0)
                ClockWidgetProvider.updateAll(this);
        }
    }

    private void stopPlayer() {
        AlarmAudioEngine.stop();
    }

    @Override public void onDestroy() {
        if(runningService.get()==this) runningService.clear();
        stopAlarm();
        try { unregisterReceiver(screenReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(volumeReceiver); } catch (Exception ignored) {}
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
