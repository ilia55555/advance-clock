package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

public final class MediaWidgetActionReceiver
        extends BroadcastReceiver {
    public static final String ACTION_OPEN =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_OPEN";
    public static final String ACTION_TOGGLE =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_TOGGLE";

    public static final String EXTRA_URI =
            "media_widget_uri";
    public static final String EXTRA_MIME =
            "media_widget_mime";
    public static final String EXTRA_NAME =
            "media_widget_name";

    @Override public void onReceive(
            Context context,
            Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();
        String uriValue =
                intent.getStringExtra(EXTRA_URI);
        String mime =
                intent.getStringExtra(EXTRA_MIME);
        String name =
                intent.getStringExtra(EXTRA_NAME);
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);

        if (uriValue == null
                || uriValue.trim().isEmpty()) {
            return;
        }

        if (ACTION_TOGGLE.equals(action)) {
            Intent playback = new Intent(
                    context,
                    MediaWidgetPlaybackService.class)
                    .setAction(
                            MediaWidgetPlaybackService.ACTION_TOGGLE)
                    .putExtra(EXTRA_URI, uriValue)
                    .putExtra(EXTRA_MIME, mime)
                    .putExtra(EXTRA_NAME, name)
                    .putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            widgetId);

            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(playback);
                } else {
                    context.startService(playback);
                }
            } catch (Exception ignored) {
            }
            return;
        }

        if (!ACTION_OPEN.equals(action)) {
            return;
        }

        try {
            Uri uri = Uri.parse(uriValue);
            Intent open = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(
                            uri,
                            mime == null
                                    || mime.trim().isEmpty()
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
