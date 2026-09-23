package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import java.util.Locale;

public final class MediaWidgetActionReceiver extends BroadcastReceiver {
    public static final String ACTION_OPEN =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_OPEN";
    public static final String ACTION_FULLSCREEN =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_FULLSCREEN";
    public static final String ACTION_TOGGLE =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_TOGGLE";
    public static final String ACTION_SEEK_BACK =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_SEEK_BACK";
    public static final String ACTION_SEEK_FORWARD =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_SEEK_FORWARD";

    public static final String EXTRA_URI = "media_widget_uri";
    public static final String EXTRA_MIME = "media_widget_mime";
    public static final String EXTRA_NAME = "media_widget_name";

    @Override public void onReceive(
            Context context,
            Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        String uriValue = intent.getStringExtra(EXTRA_URI);
        String mime = intent.getStringExtra(EXTRA_MIME);
        String name = intent.getStringExtra(EXTRA_NAME);
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if (uriValue == null || uriValue.trim().isEmpty()) {
            return;
        }

        if (ACTION_TOGGLE.equals(action)) {
            startPlaybackService(
                    context,
                    MediaWidgetPlaybackService.ACTION_TOGGLE,
                    uriValue,
                    mime,
                    name,
                    widgetId,
                    0);
            return;
        }

        if (ACTION_SEEK_BACK.equals(action)) {
            startPlaybackService(
                    context,
                    MediaWidgetPlaybackService.ACTION_SEEK_RELATIVE,
                    uriValue,
                    mime,
                    name,
                    widgetId,
                    -10_000);
            return;
        }

        if (ACTION_SEEK_FORWARD.equals(action)) {
            startPlaybackService(
                    context,
                    MediaWidgetPlaybackService.ACTION_SEEK_RELATIVE,
                    uriValue,
                    mime,
                    name,
                    widgetId,
                    10_000);
            return;
        }

        if (ACTION_FULLSCREEN.equals(action)) {
            openFullscreen(
                    context,
                    uriValue,
                    mime,
                    name);
            return;
        }

        if (ACTION_OPEN.equals(action)) {
            openNormally(
                    context,
                    uriValue,
                    mime,
                    name);
        }
    }

    private void startPlaybackService(
            Context context,
            String serviceAction,
            String uriValue,
            String mime,
            String name,
            int widgetId,
            int seekMs) {
        Intent playback = new Intent(
                context,
                MediaWidgetPlaybackService.class)
                .setAction(serviceAction)
                .putExtra(EXTRA_URI, uriValue)
                .putExtra(EXTRA_MIME, mime)
                .putExtra(EXTRA_NAME, name)
                .putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        widgetId);

        if (MediaWidgetPlaybackService.ACTION_SEEK_RELATIVE
                .equals(serviceAction)) {
            playback.putExtra(
                    MediaWidgetPlaybackService.EXTRA_SEEK_MS,
                    seekMs);
        }

        try {
            if (Build.VERSION.SDK_INT >= 26) {
                context.startForegroundService(playback);
            } else {
                context.startService(playback);
            }
        } catch (Exception ignored) {
        }
    }

    private void openFullscreen(
            Context context,
            String uriValue,
            String mime,
            String name) {
        String normalizedMime = mime == null
                ? ""
                : mime.toLowerCase(Locale.ROOT);

        if (normalizedMime.startsWith("audio/")
                || normalizedMime.startsWith("video/")) {
            try {
                context.stopService(
                        new Intent(
                                context,
                                MediaWidgetPlaybackService.class));
            } catch (Exception ignored) {
            }
        }

        Intent viewer = new Intent(
                context,
                MediaWidgetViewerActivity.class)
                .putExtra(EXTRA_URI, uriValue)
                .putExtra(EXTRA_MIME, mime)
                .putExtra(EXTRA_NAME, name)
                .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            context.startActivity(viewer);
        } catch (Exception ignored) {
        }
    }

    private void openNormally(
            Context context,
            String uriValue,
            String mime,
            String name) {
        String normalizedMime = mime == null
                ? ""
                : mime.toLowerCase(Locale.ROOT);

        if (normalizedMime.startsWith("image/")) {
            openFullscreen(
                    context,
                    uriValue,
                    mime,
                    name);
            return;
        }

        try {
            Uri uri = Uri.parse(uriValue);
            Intent open = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(
                            uri,
                            mime == null || mime.trim().isEmpty()
                                    ? "*/*"
                                    : mime)
                    .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(open);
        } catch (Exception ignored) {
        }
    }
}
