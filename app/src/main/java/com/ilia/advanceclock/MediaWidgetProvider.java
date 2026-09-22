package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
                new android.content.ComponentName(
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
    }

    private static RemoteViews createRemoteViews(
            Context context,
            int widgetId,
            float widthDp,
            float heightDp) {
        int width = Math.max(1, Math.round(widthDp));
        int height = Math.max(1, Math.round(heightDp));

        boolean showHeader =
                WidgetPrefs.showHeader(context, widgetId);
        int reserved = 20 + (showHeader ? 48 : 0);

        int maxVisible = Math.max(
                0,
                (height - reserved) / 70);
        maxVisible = Math.min(
                WidgetPrefs.maxItems(context, widgetId),
                Math.min(10, maxVisible));

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
        } else if (maxVisible <= 0) {
            root.setTextViewText(
                    R.id.media_widget_empty,
                    "برای نمایش فایل‌ها، ارتفاع ویجت را بیشتر کنید");
            root.setViewVisibility(
                    R.id.media_widget_empty,
                    View.VISIBLE);
        } else {
            root.setViewVisibility(
                    R.id.media_widget_empty,
                    View.GONE);
        }

        root.removeAllViews(
                R.id.media_widget_list);

        boolean showPreview =
                WidgetPrefs.mediaShowPreview(
                        context,
                        widgetId)
                        && width >= 150;
        boolean showName =
                WidgetPrefs.mediaShowFileName(
                        context,
                        widgetId);
        boolean showMetadata =
                WidgetPrefs.showMetadata(
                        context,
                        widgetId);

        boolean hasMore =
                items.size() > maxVisible;
        int visible;
        if (hasMore) {
            visible = Math.max(
                    0,
                    maxVisible - 1);
        } else {
            visible = Math.min(
                    maxVisible,
                    items.size());
        }

        for (int i = 0; i < visible; i++) {
            MediaWidgetPrefs.Item item =
                    items.get(i);

            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_media_item);

            row.setViewVisibility(
                    R.id.media_item_preview,
                    showPreview
                            ? View.VISIBLE
                            : View.GONE);
            row.setViewVisibility(
                    R.id.media_item_name,
                    showName
                            ? View.VISIBLE
                            : View.GONE);
            row.setViewVisibility(
                    R.id.media_item_meta,
                    showMetadata
                            ? View.VISIBLE
                            : View.GONE);

            row.setTextViewText(
                    R.id.media_item_name,
                    item.name);
            row.setTextColor(
                    R.id.media_item_name,
                    text);
            row.setTextColor(
                    R.id.media_item_meta,
                    muted);

            row.setTextViewTextSize(
                    R.id.media_item_name,
                    TypedValue.COMPLEX_UNIT_SP,
                    sizes[2]);
            row.setTextViewTextSize(
                    R.id.media_item_meta,
                    TypedValue.COMPLEX_UNIT_SP,
                    sizes[3]);

            String meta = typeLabel(item.mime);
            if (!item.textPreview.isEmpty()) {
                meta += " • " + item.textPreview;
            }
            row.setTextViewText(
                    R.id.media_item_meta,
                    meta);

            if (showPreview) {
                Bitmap preview =
                        loadCachedPreview(item);
                if (preview != null) {
                    row.setImageViewBitmap(
                            R.id.media_item_preview,
                            preview);
                } else {
                    row.setImageViewResource(
                            R.id.media_item_preview,
                            fallbackIcon(item.mime));
                }
            }

            try {
                Uri uri = Uri.parse(item.uri);
                Intent open = new Intent(
                        Intent.ACTION_VIEW)
                        .setDataAndType(
                                uri,
                                item.mime)
                        .addFlags(
                                Intent.FLAG_GRANT_READ_URI_PERMISSION);

                PendingIntent openPi =
                        PendingIntent.getActivity(
                                context,
                                2_400_000
                                        + widgetId * 31
                                        + i,
                                open,
                                PendingIntent.FLAG_UPDATE_CURRENT
                                        | PendingIntent.FLAG_IMMUTABLE);

                row.setOnClickPendingIntent(
                        R.id.media_item_root,
                        openPi);
            } catch (Exception ignored) {
            }

            root.addView(
                    R.id.media_widget_list,
                    row);
        }

        if (hasMore && maxVisible > 0) {
            RemoteViews more = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_media_item);

            more.setViewVisibility(
                    R.id.media_item_preview,
                    showPreview
                            ? View.VISIBLE
                            : View.GONE);
            more.setViewVisibility(
                    R.id.media_item_name,
                    View.VISIBLE);
            more.setViewVisibility(
                    R.id.media_item_meta,
                    showMetadata
                            ? View.VISIBLE
                            : View.GONE);

            if (showPreview) {
                more.setImageViewResource(
                        R.id.media_item_preview,
                        R.drawable.ic_md_more_vert);
            }

            more.setTextViewText(
                    R.id.media_item_name,
                    "و "
                            + (items.size() - visible)
                            + " مورد دیگر");
            more.setTextViewText(
                    R.id.media_item_meta,
                    "برای مشاهده و ویرایش لمس کنید");
            more.setTextColor(
                    R.id.media_item_name,
                    text);
            more.setTextColor(
                    R.id.media_item_meta,
                    muted);

            more.setTextViewTextSize(
                    R.id.media_item_name,
                    TypedValue.COMPLEX_UNIT_SP,
                    sizes[2]);
            more.setTextViewTextSize(
                    R.id.media_item_meta,
                    TypedValue.COMPLEX_UNIT_SP,
                    sizes[3]);

            more.setOnClickPendingIntent(
                    R.id.media_item_root,
                    config);

            root.addView(
                    R.id.media_widget_list,
                    more);
        }

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

        super.onDeleted(
                context,
                appWidgetIds);
    }

    private static Bitmap loadCachedPreview(
            MediaWidgetPrefs.Item item) {
        if (item.previewPath == null
                || item.previewPath.isEmpty()) {
            return null;
        }

        try {
            File file = new File(item.previewPath);
            if (!file.isFile()) return null;
            return BitmapFactory.decodeFile(
                    file.getAbsolutePath());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int fallbackIcon(String mime) {
        String value = mime == null
                ? ""
                : mime.toLowerCase(Locale.ROOT);

        if (value.startsWith("image/")) {
            return R.drawable.ic_app;
        }
        return R.drawable.ic_note;
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
                    13f, 10f, 12f, 9f
            };
        }
        if (mode == 2) {
            return new float[]{
                    17f, 12f, 15f, 12f
            };
        }
        return new float[]{
                15f, 11f, 13f, 10f
        };
    }

    private static String typeLabel(String mime) {
        String value = mime == null
                ? ""
                : mime.toLowerCase(Locale.ROOT);

        if (value.startsWith("image/")) return "تصویر";
        if (value.startsWith("audio/")) return "صوت";
        if (value.startsWith("video/")) return "ویدیو";
        if (value.startsWith("text/")) return "متن";
        if (value.contains("pdf")) return "PDF";
        if (value.contains("zip")
                || value.contains("rar")
                || value.contains("7z")) {
            return "فایل فشرده";
        }
        if (value.contains("word")
                || value.contains("document")) {
            return "سند";
        }
        if (value.contains("sheet")
                || value.contains("excel")) {
            return "صفحه گسترده";
        }
        return "فایل";
    }
}
