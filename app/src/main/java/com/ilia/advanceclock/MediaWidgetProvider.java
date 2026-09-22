package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

public final class MediaWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] ids) {
        for (int id : ids) {
            update(
                    context,
                    manager,
                    id,
                    manager.getAppWidgetOptions(id));
        }
    }

    @Override public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager manager,
            int appWidgetId,
            Bundle newOptions) {
        update(context, manager, appWidgetId, newOptions);
    }

    @Override public void onRestored(
            Context context,
            int[] oldWidgetIds,
            int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);

        int count = Math.min(oldWidgetIds.length, newWidgetIds.length);
        for (int i = 0; i < count; i++) {
            WidgetPrefs.migrate(
                    context,
                    oldWidgetIds[i],
                    newWidgetIds[i]);
            MediaWidgetPrefs.migrate(
                    context,
                    oldWidgetIds[i],
                    newWidgetIds[i]);
            MediaPreviewScheduler.schedule(
                    context,
                    newWidgetIds[i]);
        }
        updateAll(context);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager =
                AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(
                        context,
                        MediaWidgetProvider.class));

        for (int id : ids) {
            update(
                    context,
                    manager,
                    id,
                    manager.getAppWidgetOptions(id));
        }
    }

    public static void update(Context context, int widgetId) {
        AppWidgetManager manager =
                AppWidgetManager.getInstance(context);
        update(
                context,
                manager,
                widgetId,
                manager.getAppWidgetOptions(widgetId));
    }

    private static void update(
            Context context,
            AppWidgetManager manager,
            int widgetId,
            Bundle options) {
        WidgetSizeUtils.updateResponsive(
                context,
                manager,
                widgetId,
                options,
                (widthDp, heightDp) ->
                        createRemoteViews(
                                context,
                                widgetId,
                                widthDp,
                                heightDp));

        manager.notifyAppWidgetViewDataChanged(
                widgetId,
                R.id.media_widget_list);
    }

    private static RemoteViews createRemoteViews(
            Context context,
            int widgetId,
            float widthDp,
            float heightDp) {
        int width = Math.max(1, Math.round(widthDp));
        int height = Math.max(1, Math.round(heightDp));

        boolean compact = width < 180 || height < 150;
        boolean showHeader =
                WidgetPrefs.showHeader(context, widgetId)
                        && !compact;

        RemoteViews root = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_media);

        boolean dark =
                WidgetPrefs.isDark(context, widgetId);
        int text = dark ? 0xFFF2F5F4 : 0xFF173F3B;
        int muted = dark ? 0xFFAFBCB8 : 0xFF758783;
        int secondary =
                WidgetPrefs.secondary(context, widgetId);

        root.setInt(
                R.id.media_widget_root,
                "setBackgroundResource",
                backgroundResource(
                        dark,
                        WidgetPrefs.backgroundOpacityMode(
                                context,
                                widgetId)));

        root.setViewVisibility(
                R.id.media_widget_header,
                showHeader
                        ? View.VISIBLE
                        : View.GONE);

        root.setViewVisibility(
                R.id.media_widget_settings,
                showHeader
                        && WidgetPrefs.showSettingsButton(
                        context,
                        widgetId)
                        ? View.VISIBLE
                        : View.GONE);

        root.setTextViewText(
                R.id.media_widget_title,
                "یادآوری فایل‌ها");
        root.setTextColor(
                R.id.media_widget_title,
                text);
        root.setTextColor(
                R.id.media_widget_count,
                muted);
        root.setInt(
                R.id.media_widget_settings,
                "setColorFilter",
                secondary);

        float[] sizes = fontSizes(
                WidgetPrefs.fontSizeMode(
                        context,
                        widgetId));

        root.setTextViewTextSize(
                R.id.media_widget_title,
                TypedValue.COMPLEX_UNIT_SP,
                sizes[0]);
        root.setTextViewTextSize(
                R.id.media_widget_count,
                TypedValue.COMPLEX_UNIT_SP,
                sizes[1]);

        Intent configIntent = new Intent(
                context,
                MediaWidgetConfigActivity.class)
                .putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        widgetId)
                .putExtra("editExisting", true);

        PendingIntent config =
                PendingIntent.getActivity(
                        context,
                        2_300_000 + widgetId,
                        configIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE);

        root.setOnClickPendingIntent(
                R.id.media_widget_settings,
                config);
        root.setOnClickPendingIntent(
                R.id.media_widget_empty,
                config);

        List<MediaWidgetPrefs.Item> items =
                MediaWidgetPrefs.load(
                        context,
                        widgetId);

        root.setTextViewText(
                R.id.media_widget_count,
                items.size() + " مورد");
        root.setTextColor(
                R.id.media_widget_empty,
                muted);

        if (items.isEmpty()) {
            root.setTextViewText(
                    R.id.media_widget_empty,
                    "فایلی انتخاب نشده است\nبرای افزودن لمس کنید");
            root.setViewVisibility(
                    R.id.media_widget_empty,
                    View.VISIBLE);
            root.setViewVisibility(
                    R.id.media_widget_list,
                    View.GONE);
        } else {
            root.setViewVisibility(
                    R.id.media_widget_empty,
                    View.GONE);
            root.setViewVisibility(
                    R.id.media_widget_list,
                    View.VISIBLE);
        }

        Intent adapterIntent = new Intent(
                context,
                MediaWidgetRemoteViewsService.class)
                .putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        widgetId);
        adapterIntent.setData(Uri.parse(
                "advanceclock://media-widget/"
                        + widgetId
                        + "/"
                        + width
                        + "x"
                        + height));

        root.setRemoteAdapter(
                R.id.media_widget_list,
                adapterIntent);

        Intent actionIntent = new Intent(
                context,
                MediaWidgetActionReceiver.class)
                .putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        widgetId);

        PendingIntent actionTemplate =
                PendingIntent.getBroadcast(
                        context,
                        2_500_000 + widgetId,
                        actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_MUTABLE);

        root.setPendingIntentTemplate(
                R.id.media_widget_list,
                actionTemplate);

        return root;
    }

    @Override public void onDeleted(
            Context context,
            int[] appWidgetIds) {
        ArrayList<MediaWidgetPrefs.Item> removed =
                new ArrayList<>();

        for (int id : appWidgetIds) {
            removed.addAll(
                    MediaWidgetPrefs.load(
                            context,
                            id));
            MediaWidgetPrefs.clear(
                    context,
                    id);
            WidgetPrefs.clear(
                    context,
                    id);
            MediaPreviewScheduler.cancel(
                    context,
                    id);
        }

        MediaUriPermissionUtils.releaseUnused(
                context,
                removed);
        MediaPreviewCache.cleanupUnusedPreviewFiles(
                context);

        super.onDeleted(
                context,
                appWidgetIds);
    }

    private static int backgroundResource(
            boolean dark,
            int mode) {
        if (dark) {
            if (mode == 1) {
                return R.drawable.widget_background_dark_85;
            }
            if (mode == 2) {
                return R.drawable.widget_background_dark_70;
            }
            return R.drawable.widget_background_dark;
        }

        if (mode == 1) {
            return R.drawable.widget_background_light_85;
        }
        if (mode == 2) {
            return R.drawable.widget_background_light_70;
        }
        return R.drawable.widget_background;
    }

    private static float[] fontSizes(int mode) {
        if (mode == 0) {
            return new float[]{
                    13f, 10f
            };
        }
        if (mode == 2) {
            return new float[]{
                    17f, 12f
            };
        }
        return new float[]{
                15f, 11f
        };
    }
}
