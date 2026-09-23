package com.ilia.advanceclock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

public final class MediaWidgetPlaybackService extends Service {
    public static final String ACTION_TOGGLE =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_PLAYBACK_TOGGLE";
    public static final String ACTION_SEEK_RELATIVE =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_PLAYBACK_SEEK_RELATIVE";
    public static final String ACTION_STOP =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_PLAYBACK_STOP";
    public static final String EXTRA_SEEK_MS =
            "media_widget_seek_ms";

    private static final String CHANNEL = "media_widget_playback";
    private static final int NOTIFICATION_ID = 4307;
    private static final String PREFS =
            "advance_clock_media_widget_playback";
    private static final String KEY_URI = "uri";
    private static final String KEY_NAME = "name";
    private static final String KEY_MIME = "mime";
    private static final String KEY_PLAYING = "playing";
    private static final String KEY_POSITION = "position";
    private static final String KEY_DURATION = "duration";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private MediaPlayer player;
    private String currentUri = "";
    private String currentName = "";
    private String currentMime = "";
    private int currentWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean prepared;

    private final Runnable progressTicker = new Runnable() {
        @Override public void run() {
            if (player == null || !prepared) return;
            writeState(safeIsPlaying());
            if (safeIsPlaying()) {
                handler.postDelayed(this, 1000L);
            }
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopPlayback();
            return START_NOT_STICKY;
        }

        String uriValue =
                intent.getStringExtra(MediaWidgetActionReceiver.EXTRA_URI);
        String name =
                intent.getStringExtra(MediaWidgetActionReceiver.EXTRA_NAME);
        String mime =
                intent.getStringExtra(MediaWidgetActionReceiver.EXTRA_MIME);
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if (uriValue == null || uriValue.trim().isEmpty()) {
            return START_NOT_STICKY;
        }

        if (ACTION_TOGGLE.equals(action)) {
            toggle(uriValue, name, mime, widgetId);
            return START_NOT_STICKY;
        }

        if (ACTION_SEEK_RELATIVE.equals(action)) {
            int delta = intent.getIntExtra(EXTRA_SEEK_MS, 0);
            seekRelative(uriValue, name, mime, widgetId, delta);
            return START_NOT_STICKY;
        }

        return START_NOT_STICKY;
    }

    private void toggle(
            String uriValue,
            String name,
            String mime,
            int widgetId) {
        if (uriValue.equals(currentUri)
                && player != null
                && prepared) {
            try {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.start();
                }
                boolean playing = player.isPlaying();
                writeState(playing);
                updateNotification(playing, false);
                scheduleTicker(playing);
            } catch (Exception ignored) {
                stopPlayback();
            }
            return;
        }

        startNew(
                uriValue,
                name,
                mime,
                widgetId,
                0,
                true);
    }

    private void seekRelative(
            String uriValue,
            String name,
            String mime,
            int widgetId,
            int deltaMs) {
        if (uriValue.equals(currentUri)
                && player != null
                && prepared) {
            try {
                int target = clamp(
                        safePosition() + deltaMs,
                        0,
                        Math.max(0, safeDuration()));
                player.seekTo(target);
                writeState(safeIsPlaying());
            } catch (Exception ignored) {
                stopPlayback();
            }
            return;
        }

        startNew(
                uriValue,
                name,
                mime,
                widgetId,
                Math.max(0, deltaMs),
                true);
    }

    private void startNew(
            String uriValue,
            String name,
            String mime,
            int widgetId,
            int initialPosition,
            boolean autoPlay) {
        releasePlayer();

        currentUri = uriValue;
        currentName = name == null || name.trim().isEmpty()
                ? "رسانه"
                : name;
        currentMime = mime == null ? "" : mime;
        currentWidgetId = widgetId;
        prepared = false;

        writeState(false);
        startForeground(
                NOTIFICATION_ID,
                buildNotification(false, true));

        try {
            player = new MediaPlayer();
            player.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            player.setDataSource(this, Uri.parse(uriValue));
            player.setOnPreparedListener(mp -> {
                prepared = true;
                try {
                    int duration = Math.max(0, mp.getDuration());
                    if (initialPosition > 0 && duration > 0) {
                        mp.seekTo(Math.min(initialPosition, duration));
                    }
                    if (autoPlay) {
                        mp.start();
                    }
                    boolean playing = mp.isPlaying();
                    writeState(playing);
                    updateNotification(playing, false);
                    scheduleTicker(playing);
                } catch (Exception ignored) {
                    stopPlayback();
                }
            });
            player.setOnCompletionListener(mp -> {
                writeState(false);
                updateNotification(false, false);
                scheduleTicker(false);
            });
            player.setOnErrorListener((mp, what, extra) -> {
                stopPlayback();
                return true;
            });
            player.prepareAsync();
        } catch (Exception ignored) {
            stopPlayback();
        }
    }

    private void scheduleTicker(boolean playing) {
        handler.removeCallbacks(progressTicker);
        if (playing) {
            handler.postDelayed(progressTicker, 1000L);
        }
    }

    private void writeState(boolean playing) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putString(KEY_URI, currentUri)
                .putString(KEY_NAME, currentName)
                .putString(KEY_MIME, currentMime)
                .putBoolean(KEY_PLAYING, playing)
                .putInt(KEY_POSITION, safePosition())
                .putInt(KEY_DURATION, safeDuration())
                .apply();
        notifyWidgets();
    }

    private void updateNotification(boolean playing, boolean preparing) {
        NotificationManager manager =
                getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify(
                    NOTIFICATION_ID,
                    buildNotification(playing, preparing));
        }
        notifyWidgets();
    }

    private Notification buildNotification(
            boolean playing,
            boolean preparing) {
        Intent viewer = new Intent(
                this,
                MediaWidgetViewerActivity.class)
                .putExtra(
                        MediaWidgetActionReceiver.EXTRA_URI,
                        currentUri)
                .putExtra(
                        MediaWidgetActionReceiver.EXTRA_MIME,
                        currentMime)
                .putExtra(
                        MediaWidgetActionReceiver.EXTRA_NAME,
                        currentName)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        this,
                        4_307,
                        viewer,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);

        String state = preparing
                ? "در حال آماده‌سازی"
                : (playing ? "در حال پخش" : "مکث");

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_app)
                .setContentTitle("یادآوری فایل‌ها")
                .setContentText(state + ": " + currentName)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(playing || preparing)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .build();
    }

    private void ensureChannel() {
        NotificationManager manager =
                getSystemService(NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL,
                        "پخش رسانه ویجت فایل‌ها",
                        NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(
                "کنترل پخش صوت و ویدیو از ویجت یادآوری فایل‌ها");
        manager.createNotificationChannel(channel);
    }

    private int safePosition() {
        if (player == null || !prepared) return 0;
        try {
            return Math.max(0, player.getCurrentPosition());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private int safeDuration() {
        if (player == null || !prepared) return 0;
        try {
            return Math.max(0, player.getDuration());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean safeIsPlaying() {
        if (player == null || !prepared) return false;
        try {
            return player.isPlaying();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean isCurrent(Context context, String uri) {
        if (uri == null || uri.isEmpty()) return false;
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return uri.equals(prefs.getString(KEY_URI, ""));
    }

    public static boolean isPlaying(Context context, String uri) {
        if (!isCurrent(context, uri)) return false;
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_PLAYING, false);
    }

    public static int positionMs(Context context, String uri) {
        if (!isCurrent(context, uri)) return 0;
        return Math.max(
                0,
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getInt(KEY_POSITION, 0));
    }

    public static int durationMs(Context context, String uri) {
        if (!isCurrent(context, uri)) return 0;
        return Math.max(
                0,
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                        .getInt(KEY_DURATION, 0));
    }

    private void notifyWidgets() {
        AppWidgetManager manager =
                AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(
                        this,
                        MediaWidgetProvider.class));
        if (ids.length > 0) {
            manager.notifyAppWidgetViewDataChanged(
                    ids,
                    R.id.media_widget_list);
        }
    }

    private void releasePlayer() {
        handler.removeCallbacks(progressTicker);
        if (player == null) {
            prepared = false;
            return;
        }
        try {
            player.reset();
        } catch (Exception ignored) {
        }
        try {
            player.release();
        } catch (Exception ignored) {
        }
        player = null;
        prepared = false;
    }

    private void clearState() {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    private void stopPlayback() {
        releasePlayer();
        currentUri = "";
        currentName = "";
        currentMime = "";
        currentWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        clearState();
        notifyWidgets();

        try {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } catch (Exception ignored) {
        }
        stopSelf();
    }

    @Override public void onDestroy() {
        releasePlayer();
        clearState();
        notifyWidgets();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) {
        return null;
    }
}
