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
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

public final class AdhanSoundService extends Service {
    public static final String AUDIO_RESOURCE_NAME = "adhan";
    public static final String AUDIO_FILE_NAME = "adhan.mp3";
    public static final String ACTION_START = "com.ilia.advanceclock.ADHAN_START";
    public static final String ACTION_STOP = "com.ilia.advanceclock.ADHAN_STOP";
    public static final String ACTION_MUTE = "com.ilia.advanceclock.ADHAN_MUTE";
    public static final String ACTION_UNMUTE = "com.ilia.advanceclock.ADHAN_UNMUTE";
    public static final String ACTION_LOWER = "com.ilia.advanceclock.ADHAN_LOWER";
    public static final String ACTION_RAISE = "com.ilia.advanceclock.ADHAN_RAISE";

    private MediaPlayer player;
    private Vibrator vibrator;
    private float volume = 1f;
    private boolean muted;
    private int type;

    private static final String ACTION_VOLUME_CHANGED =
            "android.media.VOLUME_CHANGED_ACTION";
    private static final String EXTRA_VOLUME_STREAM_TYPE =
            "android.media.EXTRA_VOLUME_STREAM_TYPE";
    private static final String EXTRA_VOLUME_STREAM_VALUE =
            "android.media.EXTRA_VOLUME_STREAM_VALUE";
    private static final String EXTRA_PREV_VOLUME_STREAM_VALUE =
            "android.media.EXTRA_PREV_VOLUME_STREAM_VALUE";
    private final Handler stopHandler = new Handler(Looper.getMainLooper());
    private final Runnable maximumDuration = this::stopSelf;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) setMuted(true);
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
                setMuted(true);
            } else if (current > previous) {
                setMuted(false);
            }
        }
    };

    public static Intent startIntent(Context context, int type) {
        return command(context, ACTION_START).putExtra("adhanType", type);
    }

    public static Intent command(Context context, String action) {
        return new Intent(context, AdhanSoundService.class).setAction(action);
    }

    @Override public void onCreate() {
        super.onCreate();
        NotificationHelper.ensureChannels(this);
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
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_MUTE.equals(action)) setMuted(true);
        else if (ACTION_UNMUTE.equals(action)) setMuted(false);
        else if (ACTION_LOWER.equals(action)) lowerVolume();
        else if (ACTION_RAISE.equals(action)) raiseVolume();
        else if (ACTION_START.equals(action)) {
            type = intent.getIntExtra("adhanType", AdhanScheduler.FAJR);
            boolean openWhileUnlocked = shouldOpenFullscreenWhileUnlocked();
            startForeground(3_300_000 + type, notification());
            startPlayback();
            if (openWhileUnlocked) openRingActivity();
        }
        return START_NOT_STICKY;
    }

    private Notification notification() {
        Intent ring = new Intent(this, AdhanRingActivity.class)
                .putExtra("adhanType", type)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(
                this, 3_200_000 + type, ring,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(
                this, 3_400_000 + type, command(this, ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(
                this, NotificationHelper.ADHAN_CHANNEL)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(CalendarUtils.fa(AdhanScheduler.title(type)))
                .setContentText(AppSettings.adhanNotification(this, type)
                        ? AppSettings.prayerLocationLabel(this)
                        : AppString.get(R.string.runtime_text_0407))
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(
                        null, AppString.get(R.string.runtime_text_0307), stop).build());
        if (shouldOpenFullscreen()) builder.setFullScreenIntent(open, true);
        return builder.build();
    }

    private boolean shouldOpenFullscreen() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        return locked ? AppSettings.adhanFullscreenLocked(this, type)
                : AppSettings.adhanFullscreenUnlocked(this, type);
    }

    private boolean shouldOpenFullscreenWhileUnlocked() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        return !locked && AppSettings.adhanFullscreenUnlocked(this, type);
    }

    private void openRingActivity() {
        try {
            Intent ring = new Intent(this, AdhanRingActivity.class)
                    .putExtra("adhanType", type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(ring);
        } catch (Exception ignored) {
            // Android may still enforce its background-activity policy.
            // The high-priority full-screen notification remains the fallback.
        }
    }

    private void startPlayback() {
        stopPlayback();
        volume = AppSettings.adhanVolume(this, type) / 100f;
        muted = false;
        stopHandler.removeCallbacks(maximumDuration);
        stopHandler.postDelayed(maximumDuration, 10 * 60_000L);
        if (AppSettings.adhanSound(this, type)) {
            String selected = AppSettings.adhanSoundUri(this, type);
            Uri audioUri = selected == null || selected.isEmpty() ? null : Uri.parse(selected);
            if (audioUri == null) {
                int resource = getResources().getIdentifier(
                        AUDIO_RESOURCE_NAME, "raw", getPackageName());
                if (resource != 0) audioUri = Uri.parse(
                        "android.resource://" + getPackageName() + "/" + resource);
                else audioUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
            if (audioUri != null) {
                try {
                    player = new MediaPlayer();
                    player.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                    player.setDataSource(this, audioUri);
                    player.setLooping(false);
                    player.prepare();
                    applyVolume();
                    player.start();
                    player.setOnCompletionListener(value -> stopSelf());
                } catch (Exception ignored) { stopPlayback(); }
            }
        }
        if (AppSettings.adhanVibrate(this, type)) {
            vibrator = getSystemService(Vibrator.class);
            if (vibrator != null) vibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{0, 700, 350, 700}, -1));
        }
    }

    private void setMuted(boolean value) {
        muted = value;
        applyVolume();
    }

    private void lowerVolume() {
        muted = false;
        volume = Math.max(0.10f, volume - 0.10f);
        applyVolume();
    }

    private void raiseVolume() {
        muted = false;
        volume = Math.min(1f, volume + 0.10f);
        applyVolume();
    }

    private void applyVolume() {
        if (player != null) player.setVolume(muted ? 0f : volume, muted ? 0f : volume);
    }

    private void stopPlayback() {
        try { if (player != null) player.stop(); } catch (Exception ignored) {}
        try { if (player != null) player.release(); } catch (Exception ignored) {}
        player = null;
        if (vibrator != null) vibrator.cancel();
        vibrator = null;
    }

    @Override public void onDestroy() {
        stopHandler.removeCallbacks(maximumDuration);
        stopPlayback();
        try { unregisterReceiver(screenReceiver); } catch (Exception ignored) {}
        try { unregisterReceiver(volumeReceiver); } catch (Exception ignored) {}
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
