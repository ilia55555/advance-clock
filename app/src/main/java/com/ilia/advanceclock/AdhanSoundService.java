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
    private final Handler stopHandler = new Handler(Looper.getMainLooper());
    private final Runnable maximumDuration = this::stopSelf;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) setMuted(true);
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
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
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
            startForeground(3_300_000 + type, notification());
            startPlayback();
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
                .setContentTitle(UiText.tr(this,
                        CalendarUtils.fa(AdhanScheduler.title(type))))
                .setContentText(AppSettings.adhanNotification(this)
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
        return locked ? AppSettings.adhanFullscreenLocked(this)
                : AppSettings.adhanFullscreenUnlocked(this);
    }

    private void startPlayback() {
        stopPlayback();
        volume = AppSettings.adhanVolume(this, type) / 100f;
        muted = false;
        stopHandler.removeCallbacks(maximumDuration);
        stopHandler.postDelayed(maximumDuration, 10 * 60_000L);
        if (AppSettings.adhanSound(this)) {
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
        if (AppSettings.adhanVibrate(this)) {
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
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
