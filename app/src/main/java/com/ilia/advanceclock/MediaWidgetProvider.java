package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Locale;

public final class MediaWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager manager,
            int appWidgetId,
            Bundle newOptions) {
        update(context, manager, appWidgetId);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, MediaWidgetProvider.class));
        for (int id : ids) update(context, manager, id);
    }

    public static void update(Context context, int widgetId) {
        update(
                context,
                AppWidgetManager.getInstance(context),
                widgetId);
    }

    private static void update(
            Context context,
            AppWidgetManager manager,
            int widgetId) {
        Bundle options = manager.getAppWidgetOptions(widgetId);

        int height = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                0);
        int width = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                0);

        if (height <= 0) {
            height = Math.max(
                    40,
                    WidgetPrefs.heightCells(context, widgetId) * 70 - 30);
        }
        if (width <= 0) {
            width = Math.max(
                    100,
                    WidgetPrefs.widthCells(context, widgetId) * 70 - 30);
        }

        boolean showHeader = WidgetPrefs.showHeader(context, widgetId);
        int reserved = 20 + (showHeader ? 48 : 0);
        int maxVisible = Math.max(0, (height - reserved) / 70);
        maxVisible = Math.min(
                WidgetPrefs.maxItems(context, widgetId),
                Math.min(10, maxVisible));

        RemoteViews root = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_media);

        boolean dark = WidgetPrefs.isDark(context, widgetId);
        int text = dark ? 0xFFF2F5F4 : 0xFF173F3B;
        int muted = dark ? 0xFFAFBCB8 : 0xFF758783;
        int secondary = WidgetPrefs.secondary(context, widgetId);

        root.setInt(
                R.id.media_widget_root,
                "setBackgroundResource",
                backgroundResource(
                        dark,
                        WidgetPrefs.backgroundOpacityMode(context, widgetId)));

        root.setViewVisibility(
                R.id.media_widget_header,
                showHeader ? View.VISIBLE : View.GONE);
        root.setViewVisibility(
                R.id.media_widget_settings,
                showHeader && WidgetPrefs.showSettingsButton(context, widgetId)
                        ? View.VISIBLE
                        : View.GONE);

        root.setTextColor(R.id.media_widget_title, text);
        root.setTextColor(R.id.media_widget_count, muted);
        root.setInt(
                R.id.media_widget_settings,
                "setColorFilter",
                secondary);

        float[] sizes = fontSizes(
                WidgetPrefs.fontSizeMode(context, widgetId));
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

        PendingIntent config = PendingIntent.getActivity(
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
                MediaWidgetPrefs.load(context, widgetId);

        root.setTextViewText(
                R.id.media_widget_count,
                items.size() + " مورد");
        root.setTextColor(
                R.id.media_widget_empty,
                muted);
        root.setViewVisibility(
                R.id.media_widget_empty,
                items.isEmpty() ? View.VISIBLE : View.GONE);

        root.removeAllViews(R.id.media_widget_list);

        boolean showPreview =
                WidgetPrefs.mediaShowPreview(context, widgetId)
                        && width >= 150;
        boolean showName =
                WidgetPrefs.mediaShowFileName(context, widgetId);
        boolean showMetadata =
                WidgetPrefs.showMetadata(context, widgetId);

        int visible = Math.min(maxVisible, items.size());

        for (int i = 0; i < visible; i++) {
            MediaWidgetPrefs.Item item = items.get(i);

            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_media_item);

            row.setViewVisibility(
                    R.id.media_item_preview,
                    showPreview ? View.VISIBLE : View.GONE);
            row.setViewVisibility(
                    R.id.media_item_name,
                    showName ? View.VISIBLE : View.GONE);
            row.setViewVisibility(
                    R.id.media_item_meta,
                    showMetadata ? View.VISIBLE : View.GONE);

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
            String textPreview = readTextPreview(context, item);
            if (!textPreview.isEmpty()) {
                meta += " • " + textPreview;
            }
            row.setTextViewText(
                    R.id.media_item_meta,
                    meta);

            if (showPreview) {
                Bitmap preview = loadPreview(context, item);
                if (preview != null) {
                    row.setImageViewBitmap(
                            R.id.media_item_preview,
                            preview);
                } else {
                    row.setImageViewResource(
                            R.id.media_item_preview,
                            item.mime.startsWith("image/")
                                    ? R.drawable.ic_app
                                    : R.drawable.ic_note);
                }
            }

            try {
                Uri uri = Uri.parse(item.uri);
                Intent open = new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, item.mime)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                PendingIntent openPi = PendingIntent.getActivity(
                        context,
                        2_400_000 + widgetId * 31 + i,
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

        if (items.size() > visible && visible > 0) {
            RemoteViews more = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_media_item);

            more.setViewVisibility(
                    R.id.media_item_preview,
                    showPreview ? View.VISIBLE : View.GONE);
            more.setViewVisibility(
                    R.id.media_item_name,
                    View.VISIBLE);
            more.setViewVisibility(
                    R.id.media_item_meta,
                    showMetadata ? View.VISIBLE : View.GONE);

            if (showPreview) {
                more.setImageViewResource(
                        R.id.media_item_preview,
                        R.drawable.ic_md_more_vert);
            }

            more.setTextViewText(
                    R.id.media_item_name,
                    "و " + (items.size() - visible) + " مورد دیگر");
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

        manager.updateAppWidget(widgetId, root);
    }

    private static int backgroundResource(boolean dark, int mode) {
        if (dark) {
            if (mode == 1) return R.drawable.widget_background_dark_85;
            if (mode == 2) return R.drawable.widget_background_dark_70;
            return R.drawable.widget_background_dark;
        }
        if (mode == 1) return R.drawable.widget_background_light_85;
        if (mode == 2) return R.drawable.widget_background_light_70;
        return R.drawable.widget_background;
    }

    private static float[] fontSizes(int mode) {
        if (mode == 0) return new float[]{13f, 10f, 12f, 9f};
        if (mode == 2) return new float[]{17f, 12f, 15f, 12f};
        return new float[]{15f, 11f, 13f, 10f};
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

    private static String readTextPreview(
            Context context,
            MediaWidgetPrefs.Item item) {
        if (item.mime == null
                || !(item.mime.startsWith("text/")
                || item.mime.contains("json")
                || item.mime.contains("xml"))) {
            return "";
        }

        try (InputStream in = context
                .getContentResolver()
                .openInputStream(Uri.parse(item.uri))) {
            if (in == null) return "";

            InputStreamReader reader =
                    new InputStreamReader(in);
            char[] buffer = new char[160];
            int count = reader.read(buffer);
            if (count <= 0) return "";

            String text = new String(
                    buffer,
                    0,
                    count)
                    .replaceAll("\\s+", " ")
                    .trim();

            return text.length() > 90
                    ? text.substring(0, 90) + "…"
                    : text;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static Bitmap loadPreview(
            Context context,
            MediaWidgetPrefs.Item item) {
        String mime = item.mime == null
                ? ""
                : item.mime;

        Uri uri;
        try {
            uri = Uri.parse(item.uri);
        } catch (Exception e) {
            return null;
        }

        if (mime.startsWith("image/")) {
            return decodeImage(context, uri);
        }

        if (mime.startsWith("audio/")
                || mime.startsWith("video/")) {
            MediaMetadataRetriever retriever =
                    new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, uri);
                Bitmap bitmap = null;

                if (mime.startsWith("video/")) {
                    bitmap = retriever.getFrameAtTime(
                            -1,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                } else {
                    byte[] art = retriever.getEmbeddedPicture();
                    if (art != null) {
                        bitmap = BitmapFactory.decodeByteArray(
                                art,
                                0,
                                art.length);
                    }
                }

                return scale(bitmap, 128, 96);
            } catch (Exception ignored) {
                return null;
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private static Bitmap decodeImage(
            Context context,
            Uri uri) {
        try {
            BitmapFactory.Options bounds =
                    new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;

            try (InputStream first = context
                    .getContentResolver()
                    .openInputStream(uri)) {
                if (first == null) return null;
                BitmapFactory.decodeStream(
                        first,
                        null,
                        bounds);
            }

            BitmapFactory.Options options =
                    new BitmapFactory.Options();

            int sample = 1;
            while (bounds.outWidth / sample > 256
                    || bounds.outHeight / sample > 192) {
                sample *= 2;
            }
            options.inSampleSize = Math.max(1, sample);

            try (InputStream second = context
                    .getContentResolver()
                    .openInputStream(uri)) {
                if (second == null) return null;
                Bitmap bitmap = BitmapFactory.decodeStream(
                        second,
                        null,
                        options);
                return scale(bitmap, 128, 96);
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Bitmap scale(
            Bitmap source,
            int maxWidth,
            int maxHeight) {
        if (source == null) return null;

        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) return null;

        float ratio = Math.min(
                (float) maxWidth / width,
                (float) maxHeight / height);

        if (ratio >= 1f) return source;

        int targetWidth = Math.max(
                1,
                Math.round(width * ratio));
        int targetHeight = Math.max(
                1,
                Math.round(height * ratio));

        return Bitmap.createScaledBitmap(
                source,
                targetWidth,
                targetHeight,
                true);
    }

    @Override public void onDeleted(
            Context context,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            MediaWidgetPrefs.clear(context, id);
            WidgetPrefs.clear(context, id);
        }
        super.onDeleted(context, appWidgetIds);
    }
}
