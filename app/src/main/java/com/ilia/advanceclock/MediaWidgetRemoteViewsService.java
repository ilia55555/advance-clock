package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MediaWidgetRemoteViewsService
        extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(
            Intent intent) {
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        return new Factory(
                getApplicationContext(),
                widgetId);
    }

    private static final class Factory
            implements RemoteViewsService.RemoteViewsFactory {
        private final Context context;
        private final int widgetId;
        private final ArrayList<MediaWidgetPrefs.Item> items =
                new ArrayList<>();

        private boolean showName;
        private boolean showPreview;
        private boolean showMetadata;
        private int text;
        private int muted;
        private int secondary;
        private float nameSize;
        private float metaSize;

        Factory(Context context, int widgetId) {
            this.context = context;
            this.widgetId = widgetId;
        }

        @Override public void onCreate() {
            reload();
        }

        @Override public void onDataSetChanged() {
            reload();
        }

        private void reload() {
            items.clear();
            List<MediaWidgetPrefs.Item> loaded =
                    MediaWidgetPrefs.load(
                            context,
                            widgetId);
            items.addAll(loaded);

            showName =
                    WidgetPrefs.mediaShowFileName(
                            context,
                            widgetId);
            showPreview =
                    WidgetPrefs.mediaShowPreview(
                            context,
                            widgetId)
                            || !showName;
            showMetadata =
                    showName
                            && WidgetPrefs.showMetadata(
                            context,
                            widgetId);

            boolean dark =
                    WidgetPrefs.isDark(
                            context,
                            widgetId);
            text = dark
                    ? 0xFFF2F5F4
                    : 0xFF173F3B;
            muted = dark
                    ? 0xFFAFBCB8
                    : 0xFF758783;
            secondary =
                    WidgetPrefs.secondary(
                            context,
                            widgetId);

            float[] sizes = itemFontSizes(
                    WidgetPrefs.fontSizeMode(
                            context,
                            widgetId));
            nameSize = sizes[0];
            metaSize = sizes[1];
        }

        @Override public void onDestroy() {
            items.clear();
        }

        @Override public int getCount() {
            return items.size();
        }

        @Override public RemoteViews getViewAt(int position) {
            if (position < 0
                    || position >= items.size()) {
                return null;
            }

            MediaWidgetPrefs.Item item =
                    items.get(position);
            boolean previewOnly = !showName;

            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    previewOnly
                            ? R.layout.widget_media_preview_item
                            : R.layout.widget_media_item);

            if (!previewOnly) {
                row.setViewVisibility(
                        R.id.media_item_preview,
                        showPreview
                                ? View.VISIBLE
                                : View.GONE);
                row.setViewVisibility(
                        R.id.media_item_name,
                        View.VISIBLE);
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
                        nameSize);
                row.setTextViewTextSize(
                        R.id.media_item_meta,
                        TypedValue.COMPLEX_UNIT_SP,
                        metaSize);

                String meta = typeLabel(item.mime);
                if (!item.textPreview.isEmpty()) {
                    meta += " • " + item.textPreview;
                }
                row.setTextViewText(
                        R.id.media_item_meta,
                        meta);
            }

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

            boolean playable =
                    isPlayable(item.mime);
            row.setViewVisibility(
                    R.id.media_item_playback,
                    playable
                            ? View.VISIBLE
                            : View.GONE);

            if (playable) {
                boolean playing =
                        MediaWidgetPlaybackService.isPlaying(
                                context,
                                item.uri);

                row.setImageViewResource(
                        R.id.media_item_playback,
                        playing
                                ? R.drawable.ic_md_pause
                                : R.drawable.ic_md_play);
                row.setInt(
                        R.id.media_item_playback,
                        "setColorFilter",
                        secondary);

                Intent toggle = new Intent()
                        .setAction(
                                MediaWidgetActionReceiver.ACTION_TOGGLE)
                        .putExtra(
                                MediaWidgetActionReceiver.EXTRA_URI,
                                item.uri)
                        .putExtra(
                                MediaWidgetActionReceiver.EXTRA_MIME,
                                item.mime)
                        .putExtra(
                                MediaWidgetActionReceiver.EXTRA_NAME,
                                item.name)
                        .putExtra(
                                AppWidgetManager.EXTRA_APPWIDGET_ID,
                                widgetId);

                row.setOnClickFillInIntent(
                        R.id.media_item_playback,
                        toggle);
            }

            Intent open = new Intent()
                    .setAction(
                            MediaWidgetActionReceiver.ACTION_OPEN)
                    .putExtra(
                            MediaWidgetActionReceiver.EXTRA_URI,
                            item.uri)
                    .putExtra(
                            MediaWidgetActionReceiver.EXTRA_MIME,
                            item.mime)
                    .putExtra(
                            MediaWidgetActionReceiver.EXTRA_NAME,
                            item.name)
                    .putExtra(
                            AppWidgetManager.EXTRA_APPWIDGET_ID,
                            widgetId);

            row.setOnClickFillInIntent(
                    R.id.media_item_root,
                    open);

            return row;
        }

        @Override public RemoteViews getLoadingView() {
            return null;
        }

        @Override public int getViewTypeCount() {
            return 2;
        }

        @Override public long getItemId(int position) {
            if (position < 0
                    || position >= items.size()) {
                return position;
            }
            return items.get(position).uri.hashCode();
        }

        @Override public boolean hasStableIds() {
            return true;
        }
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

    private static boolean isPlayable(String mime) {
        String value = mime == null
                ? ""
                : mime.toLowerCase(Locale.ROOT);
        return value.startsWith("audio/")
                || value.startsWith("video/");
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

    private static float[] itemFontSizes(int mode) {
        if (mode == 0) {
            return new float[]{12f, 9f};
        }
        if (mode == 2) {
            return new float[]{15f, 12f};
        }
        return new float[]{13f, 10f};
    }
}
