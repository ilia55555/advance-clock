package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MediaPreviewCache {
    private static final int PREVIEW_WIDTH = 480;
    private static final int PREVIEW_HEIGHT = 320;

    private MediaPreviewCache() {}

    public static void prepareWidget(Context context, int widgetId) {
        List<MediaWidgetPrefs.Item> source =
                MediaWidgetPrefs.load(context, widgetId);
        ArrayList<MediaWidgetPrefs.Item> prepared = new ArrayList<>();

        for (MediaWidgetPrefs.Item item : source) {
            prepared.add(prepareItem(context, item));
        }

        MediaWidgetPrefs.save(context, widgetId, prepared);
        MediaWidgetProvider.update(context, widgetId);
        cleanupUnusedPreviewFiles(context);
    }

    private static MediaWidgetPrefs.Item prepareItem(
            Context context,
            MediaWidgetPrefs.Item item) {
        String path = item.previewPath;
        String textPreview = item.textPreview;

        File cached = path.isEmpty() ? null : new File(path);
        if (cached == null || !cached.isFile() || previewTooSmall(cached)) {
            Bitmap preview = createPreview(context, item);
            path = savePreview(context, item.uri, preview);
        }

        if (textPreview.isEmpty()) {
            textPreview = readTextPreview(context, item);
        }

        return item.withPreview(path, textPreview);
    }

    private static boolean previewTooSmall(File file) {
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            return options.outWidth < PREVIEW_WIDTH / 2
                    || options.outHeight < PREVIEW_HEIGHT / 2;
        } catch (Exception ignored) {
            return true;
        }
    }

    private static Bitmap createPreview(
            Context context,
            MediaWidgetPrefs.Item item) {
        String mime = item.mime == null
                ? ""
                : item.mime.toLowerCase(Locale.ROOT);
        Uri uri;

        try {
            uri = Uri.parse(item.uri);
        } catch (Exception e) {
            return null;
        }

        if (mime.startsWith("image/")) {
            return decodeImage(context, uri);
        }

        if (mime.startsWith("video/")) {
            MediaMetadataRetriever retriever =
                    new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, uri);
                Bitmap frame;
                if (Build.VERSION.SDK_INT >= 27) {
                    frame = retriever.getScaledFrameAtTime(
                            -1,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                            PREVIEW_WIDTH,
                            PREVIEW_HEIGHT);
                } else {
                    frame = retriever.getFrameAtTime(
                            -1,
                            MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
                    frame = scale(frame);
                }
                return frame;
            } catch (Exception ignored) {
                return null;
            } finally {
                try {
                    retriever.release();
                } catch (Exception ignored) {
                }
            }
        }

        if (mime.startsWith("audio/")) {
            MediaMetadataRetriever retriever =
                    new MediaMetadataRetriever();
            try {
                retriever.setDataSource(context, uri);
                byte[] art = retriever.getEmbeddedPicture();
                if (art == null || art.length == 0) return null;
                return decodeByteArraySampled(art);
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

    private static Bitmap decodeImage(Context context, Uri uri) {
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;

            try (InputStream first = context
                    .getContentResolver()
                    .openInputStream(uri)) {
                if (first == null) return null;
                BitmapFactory.decodeStream(first, null, bounds);
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sampleSize(
                    bounds.outWidth,
                    bounds.outHeight);

            try (InputStream second = context
                    .getContentResolver()
                    .openInputStream(uri)) {
                if (second == null) return null;
                return scale(BitmapFactory.decodeStream(
                        second,
                        null,
                        options));
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Bitmap decodeByteArraySampled(byte[] data) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize(
                bounds.outWidth,
                bounds.outHeight);

        return scale(BitmapFactory.decodeByteArray(
                data,
                0,
                data.length,
                options));
    }

    private static int sampleSize(int width, int height) {
        int sample = 1;
        while (width > 0
                && height > 0
                && (width / sample > PREVIEW_WIDTH * 2
                || height / sample > PREVIEW_HEIGHT * 2)) {
            sample *= 2;
        }
        return Math.max(1, sample);
    }

    private static Bitmap scale(Bitmap source) {
        if (source == null) return null;

        int width = source.getWidth();
        int height = source.getHeight();
        if (width <= 0 || height <= 0) return null;

        float ratio = Math.min(
                (float) PREVIEW_WIDTH / width,
                (float) PREVIEW_HEIGHT / height);

        if (ratio >= 1f) return source;

        return Bitmap.createScaledBitmap(
                source,
                Math.max(1, Math.round(width * ratio)),
                Math.max(1, Math.round(height * ratio)),
                true);
    }

    private static String readTextPreview(
            Context context,
            MediaWidgetPrefs.Item item) {
        String mime = item.mime == null
                ? ""
                : item.mime.toLowerCase(Locale.ROOT);

        if (!(mime.startsWith("text/")
                || mime.contains("json")
                || mime.contains("xml"))) {
            return "";
        }

        try (InputStream in = context
                .getContentResolver()
                .openInputStream(Uri.parse(item.uri))) {
            if (in == null) return "";

            InputStreamReader reader = new InputStreamReader(in);
            char[] buffer = new char[200];
            int count = reader.read(buffer);
            if (count <= 0) return "";

            String value = new String(buffer, 0, count)
                    .replaceAll("\\s+", " ")
                    .trim();

            return value.length() > 90
                    ? value.substring(0, 90) + "…"
                    : value;
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String savePreview(
            Context context,
            String uri,
            Bitmap bitmap) {
        if (bitmap == null) return "";

        File directory = new File(
                context.getFilesDir(),
                "media_widget_previews");
        if (!directory.exists() && !directory.mkdirs()) {
            return "";
        }

        File file = new File(
                directory,
                sha256(uri) + ".jpg");

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
            out.flush();
            return file.getAbsolutePath();
        } catch (Exception ignored) {
            return "";
        }
    }

    public static void cleanupUnusedPreviewFiles(Context context) {
        File directory = new File(
                context.getFilesDir(),
                "media_widget_previews");
        File[] files = directory.listFiles();
        if (files == null || files.length == 0) return;

        java.util.HashSet<String> used = new java.util.HashSet<>();
        android.appwidget.AppWidgetManager manager =
                android.appwidget.AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new android.content.ComponentName(
                        context,
                        MediaWidgetProvider.class));

        for (int id : ids) {
            for (MediaWidgetPrefs.Item item :
                    MediaWidgetPrefs.load(context, id)) {
                if (!item.previewPath.isEmpty()) {
                    used.add(item.previewPath);
                }
            }
        }

        for (File file : files) {
            if (file.isFile() && !used.contains(file.getAbsolutePath())) {
                try {
                    file.delete();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(
                    value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format(Locale.ROOT, "%02x", b));
            }
            return builder.toString();
        } catch (Exception ignored) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
