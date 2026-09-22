package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MediaUriPermissionUtils {
    private MediaUriPermissionUtils() {}

    public static void releaseUnused(
            Context context,
            List<MediaWidgetPrefs.Item> candidates) {
        if (candidates == null || candidates.isEmpty()) return;

        Set<String> referenced = new HashSet<>();
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(
                        context,
                        MediaWidgetProvider.class));

        for (int id : ids) {
            for (MediaWidgetPrefs.Item item :
                    MediaWidgetPrefs.load(context, id)) {
                referenced.add(item.uri);
            }
        }

        for (MediaWidgetPrefs.Item item : candidates) {
            if (item == null
                    || item.uri.isEmpty()
                    || referenced.contains(item.uri)) {
                continue;
            }

            try {
                context.getContentResolver()
                        .releasePersistableUriPermission(
                                Uri.parse(item.uri),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {
            }
        }
    }
}
