package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        return new Factory(getApplicationContext(), widgetId);
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
                    MediaWidgetPrefs.load(context, widgetId);
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
            if (position < 0 || position >= items.size()) {
                return null;
            }

            MediaWidgetPrefs.Item item = items.get(position);
            String mime = normalizedMime(item.mime);

            if (mime.startsWith("image/")) {
                return imageRow(item);
            }

            if (mime.startsWith("audio/")
                    || mime.startsWith("video/")) {
                return playableRow(item, mime);
            }

            return fileRow(item);
        }

        private RemoteViews imageRow(MediaWidgetPrefs.Item item) {
            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_media_image_item);

            row.setViewVisibility(
                    R.id.media_item_preview,
                    showPreview ? View.VISIBLE : View.GONE);
            row.setViewVisibility(
                    R.id.media_item_name,
                    showName ? View.VISIBLE : View.INVISIBLE);
            row.setTextViewText(
                    R.id.media_item_name,
                    item.name);
            row.setTextColor(
                    R.id.media_item_name,
                    text);
            row.setTextViewTextSize(
                    R.id.media_item_name,
                    TypedValue.COMPLEX_UNIT_SP,
                    nameSize);
            row.setInt(
                    R.id.media_item_fullscreen,
                    "setColorFilter",
                    secondary);

            if (showPreview) {
                setPreview(
                        row,
                        R.id.media_item_preview,
                        item,
                        true);
            }

            Intent fullscreen =
                    action(
                            MediaWidgetActionReceiver.ACTION_FULLSCREEN,
                            item);
            row.setOnClickFillInIntent(
                    R.id.media_item_fullscreen,
                    fullscreen);
            row.setOnClickFillInIntent(
                    R.id.media_item_root,
                    fullscreen);

            return row;
        }

        private RemoteViews playableRow(
                MediaWidgetPrefs.Item item,
                String mime) {
            boolean audio = mime.startsWith("audio/");
            boolean video = mime.startsWith("video/");

            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_media_playable_item);

            row.setViewVisibility(
                    R.id.media_item_preview,
                    video && showPreview
                            ? View.VISIBLE
                            : View.GONE);

            // Audio names are always visible, regardless of the global
            // "show file name" preference.
            row.setViewVisibility(
                    R.id.media_item_name,
                    audio || showName
                            ? View.VISIBLE
                            : View.GONE);
            row.setTextViewText(
                    R.id.media_item_name,
                    item.name);
            row.setTextColor(
                    R.id.media_item_name,
                    text);
            row.setTextViewTextSize(
                    R.id.media_item_name,
                    TypedValue.COMPLEX_UNIT_SP,
                    nameSize);

            if (video && showPreview) {
                setPreview(
                        row,
                        R.id.media_item_preview,
                        item,
                        true);
            }

            int duration =
                    MediaWidgetPlaybackService.durationMs(
                            context,
                            item.uri);
            int current =
                    MediaWidgetPlaybackService.positionMs(
                            context,
                            item.uri);
            boolean playing =
                    MediaWidgetPlaybackService.isPlaying(
                            context,
                            item.uri);

            int safeDuration = Math.max(1, duration);
            int safeCurrent = Math.max(
                    0,
                    Math.min(safeDuration, current));

            row.setProgressBar(
                    R.id.media_item_progress,
                    safeDuration,
                    safeCurrent,
                    false);
            row.setTextViewText(
                    R.id.media_item_position,
                    formatTime(current));
            row.setTextViewText(
                    R.id.media_item_duration,
                    duration > 0
                            ? formatTime(duration)
                            : "00:00");
            row.setTextColor(
                    R.id.media_item_position,
                    muted);
            row.setTextColor(
                    R.id.media_item_duration,
                    muted);

            row.setImageViewResource(
                    R.id.media_item_playback,
                    playing
                            ? R.drawable.ic_md_pause
                            : R.drawable.ic_md_play);
            row.setInt(
                    R.id.media_item_playback,
                    "setColorFilter",
                    secondary);
            row.setInt(
                    R.id.media_item_fullscreen,
                    "setColorFilter",
                    secondary);
            row.setTextColor(
                    R.id.media_item_back_10,
                    secondary);
            row.setTextColor(
                    R.id.media_item_forward_10,
                    secondary);

            row.setOnClickFillInIntent(
                    R.id.media_item_playback,
                    action(
                            MediaWidgetActionReceiver.ACTION_TOGGLE,
                            item));
            row.setOnClickFillInIntent(
                    R.id.media_item_back_10,
                    action(
                            MediaWidgetActionReceiver.ACTION_SEEK_BACK,
                            item));
            row.setOnClickFillInIntent(
                    R.id.media_item_forward_10,
                    action(
                            MediaWidgetActionReceiver.ACTION_SEEK_FORWARD,
                            item));
            row.setOnClickFillInIntent(
                    R.id.media_item_fullscreen,
                    action(
                            MediaWidgetActionReceiver.ACTION_FULLSCREEN,
                            item));

            return row;
        }

        private RemoteViews fileRow(MediaWidgetPrefs.Item item) {
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
            row.setViewVisibility(
                    R.id.media_item_playback,
                    View.GONE);

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

            if (showPreview) {
                setPreview(
                        row,
                        R.id.media_item_preview,
                        item,
                        false);
            }

            row.setOnClickFillInIntent(
                    R.id.media_item_root,
                    action(
                            MediaWidgetActionReceiver.ACTION_OPEN,
                            item));

            return row;
        }

        private void setPreview(
                RemoteViews row,
                int viewId,
                MediaWidgetPrefs.Item item,
                boolean fullImage) {
            Bitmap preview = loadCachedPreview(item);
            if (preview != null) {
                row.setImageViewBitmap(
                        viewId,
                        preview);
                return;
            }

            row.setImageViewResource(
                    viewId,
                    fallbackIcon(item.mime));
            if (!fullImage) {
                row.setInt(
                        viewId,
                        "setColorFilter",
                        secondary);
            }
        }

        private Intent action(
                String action,
                MediaWidgetPrefs.Item item) {
            return new Intent()
                    .setAction(action)
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
        }

        @Override public RemoteViews getLoadingView() {
            return null;
        }

        @Override public int getViewTypeCount() {
            return 3;
        }

        @Override public long getItemId(int position) {
            if (position < 0 || position >= items.size()) {
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
        String value = normalizedMime(mime);
        if (value.startsWith("image/")) {
            return R.drawable.ic_app;
        }
        return R.drawable.ic_note;
    }

    private static String normalizedMime(String mime) {
        return mime == null
                ? ""
                : mime.toLowerCase(Locale.ROOT);
    }

    private static String typeLabel(String mime) {
        String value = normalizedMime(mime);

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

    private static String formatTime(int millis) {
        int totalSeconds = Math.max(0, millis / 1000);
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        if (minutes >= 60) {
            int hours = minutes / 60;
            minutes %= 60;
            return String.format(
                    Locale.getDefault(),
                    "%d:%02d:%02d",
                    hours,
                    minutes,
                    seconds);
        }
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds);
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
