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
import android.os.IBinder;

public final class MediaWidgetPlaybackService
        extends Service {
    public static final String ACTION_TOGGLE =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_PLAYBACK_TOGGLE";

    private static final String CHANNEL =
            "media_widget_playback";
    private static final int NOTIFICATION_ID =
            4307;
    private static final String PREFS =
            "advance_clock_media_widget_playback";
    private static final String KEY_URI =
            "uri";
    private static final String KEY_PLAYING =
            "playing";

    private MediaPlayer player;
    private String currentUri = "";
    private String currentName = "";
    private int currentWidgetId =
            AppWidgetManager.INVALID_APPWIDGET_ID;
    private boolean prepared;

    @Override public void onCreate() {
        super.onCreate();
        ensureChannel();
    }

    @Override public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {
        if (intent == null
                || !ACTION_TOGGLE.equals(
                intent.getAction())) {
            return START_NOT_STICKY;
        }

        String uriValue =
                intent.getStringExtra(
                        MediaWidgetActionReceiver.EXTRA_URI);
        String name =
                intent.getStringExtra(
                        MediaWidgetActionReceiver.EXTRA_NAME);
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if (uriValue == null
                || uriValue.trim().isEmpty()) {
            stopPlayback();
            return START_NOT_STICKY;
        }

        if (uriValue.equals(currentUri)
                && player != null
                && prepared) {
            try {
                if (player.isPlaying()) {
                    player.pause();
                    writeState(false);
                    updateNotification(false);
                } else {
                    player.start();
                    writeState(true);
                    updateNotification(true);
                }
            } catch (Exception ignored) {
                stopPlayback();
            }
            return START_NOT_STICKY;
        }

        startNew(
                uriValue,
                name,
                widgetId);
        return START_NOT_STICKY;
    }

    private void startNew(
            String uriValue,
            String name,
            int widgetId) {
        releasePlayer();

        currentUri = uriValue;
        currentName = name == null
                || name.trim().isEmpty()
                ? "رسانه"
                : name;
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
                            .setUsage(
                                    AudioAttributes.USAGE_MEDIA)
                            .setContentType(
                                    AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build());
            player.setDataSource(
                    this,
                    Uri.parse(uriValue));
            player.setOnPreparedListener(mp -> {
                prepared = true;
                try {
                    mp.start();
                    writeState(true);
                    updateNotification(true);
                } catch (Exception ignored) {
                    stopPlayback();
                }
            });
            player.setOnCompletionListener(
                    mp -> stopPlayback());
            player.setOnErrorListener(
                    (mp, what, extra) -> {
                        stopPlayback();
                        return true;
                    });
            player.prepareAsync();
        } catch (Exception ignored) {
            stopPlayback();
        }
    }

    private void updateNotification(
            boolean playing) {
        NotificationManager manager =
                getSystemService(
                        NotificationManager.class);
        if (manager != null) {
            manager.notify(
                    NOTIFICATION_ID,
                    buildNotification(
                            playing,
                            false));
        }
        notifyWidgets();
    }

    private Notification buildNotification(
            boolean playing,
            boolean preparing) {
        Intent openApp = new Intent(
                this,
                MainActivity.class);
        PendingIntent contentIntent =
                PendingIntent.getActivity(
                        this,
                        4_307,
                        openApp,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);

        String state;
        if (preparing) {
            state = "در حال آماده‌سازی";
        } else {
            state = playing
                    ? "در حال پخش"
                    : "مکث";
        }

        return new Notification.Builder(
                this,
                CHANNEL)
                .setSmallIcon(R.drawable.ic_app)
                .setContentTitle("یادآوری فایل‌ها")
                .setContentText(
                        state + ": " + currentName)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .build();
    }

    private void ensureChannel() {
        NotificationManager manager =
                getSystemService(
                        NotificationManager.class);
        if (manager == null) return;

        NotificationChannel channel =
                new NotificationChannel(
                        CHANNEL,
                        "پخش رسانه ویجت فایل‌ها",
                        NotificationManager.IMPORTANCE_LOW);
        channel.setDescription(
                "برای پخش و مکث صوت و ویدیو از ویجت یادآوری فایل‌ها");
        manager.createNotificationChannel(channel);
    }

    private void writeState(boolean playing) {
        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_URI, currentUri)
                .putBoolean(KEY_PLAYING, playing)
                .apply();
        notifyWidgets();
    }

    public static boolean isPlaying(
            Context context,
            String uri) {
        if (uri == null
                || uri.isEmpty()) {
            return false;
        }
        SharedPreferences prefs =
                context.getSharedPreferences(
                        PREFS,
                        Context.MODE_PRIVATE);
        return uri.equals(
                prefs.getString(KEY_URI, ""))
                && prefs.getBoolean(
                KEY_PLAYING,
                false);
    }

    private void clearState() {
        getSharedPreferences(
                PREFS,
                MODE_PRIVATE)
                .edit()
                .remove(KEY_URI)
                .remove(KEY_PLAYING)
                .apply();
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
        if (player == null) return;
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

    private void stopPlayback() {
        releasePlayer();
        currentUri = "";
        currentName = "";
        currentWidgetId =
                AppWidgetManager.INVALID_APPWIDGET_ID;
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

    @Override public IBinder onBind(
            Intent intent) {
        return null;
    }
}
