package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.Locale;

public final class MediaWidgetActionReceiver
        extends BroadcastReceiver {
    public static final String ACTION_OPEN =
            "com.ilia.advanceclock.action.MEDIA_WIDGET_OPEN";

    public static final String EXTRA_URI =
            "media_widget_uri";
    public static final String EXTRA_MIME =
            "media_widget_mime";
    public static final String EXTRA_NAME =
            "media_widget_name";

    @Override public void onReceive(
            Context context,
            Intent intent) {
        if (intent == null
                || !ACTION_OPEN.equals(intent.getAction())) {
            return;
        }

        String uriValue =
                intent.getStringExtra(EXTRA_URI);
        String mime =
                intent.getStringExtra(EXTRA_MIME);
        String name =
                intent.getStringExtra(EXTRA_NAME);

        if (uriValue == null
                || uriValue.trim().isEmpty()) {
            return;
        }

        String normalizedMime = mime == null
                ? ""
                : mime.toLowerCase(Locale.ROOT);

        if (normalizedMime.startsWith("image/")
                || normalizedMime.startsWith("audio/")
                || normalizedMime.startsWith("video/")) {
            Intent viewer = new Intent(
                    context,
                    MediaWidgetViewerActivity.class)
                    .putExtra(EXTRA_URI, uriValue)
                    .putExtra(EXTRA_MIME, mime)
                    .putExtra(EXTRA_NAME, name)
                    .addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(viewer);
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
