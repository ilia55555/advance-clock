package com.ilia.advanceclock;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipFile;
import java.util.zip.ZipException;

/** Checks a bounded local copy and shares exactly those bytes, preventing provider substitution. */
final class SafeDocumentOpener {
    static final long MAX_BYTES = 64L * 1024 * 1024;
    private static final ThreadPoolExecutor WORK = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(2), r -> { Thread thread = new Thread(r, "CheckedDocument"); thread.setDaemon(true); return thread; });

    static void open(Context context, Uri uri, String mime, String name, Runnable complete) {
        Handler main = new Handler(Looper.getMainLooper());
        java.util.concurrent.atomic.AtomicBoolean finished = new java.util.concurrent.atomic.AtomicBoolean();
        Runnable timeout = () -> {
            if (finished.compareAndSet(false, true)) {
                try { NoteAttachment.blocked(context); } finally { complete.run(); }
            }
        };
        Runnable done = () -> {
            main.removeCallbacks(timeout);
            if (finished.compareAndSet(false, true)) complete.run();
        };
        main.postDelayed(timeout, 8000);
        try {
            WORK.execute(() -> {
                try {
                    if (finished.get()) return;
                    Uri checked = prepare(context, uri, mime, name);
                    main.post(() -> {
                        if (finished.get()) return;
                        try {
                            context.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(checked, mime)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION));
                        } catch (RuntimeException error) { NoteAttachment.blocked(context); }
                        finally { done.run(); }
                    });
                } catch (Exception error) {
                    main.post(() -> {
                        if (finished.get()) return;
                        try { NoteAttachment.blocked(context); } finally { done.run(); }
                    });
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException busy) {
            NoteAttachment.blocked(context); done.run();
        }
    }

    static Uri prepare(Context context, Uri uri, String mime, String name) throws IOException {
        if (!AttachmentSafety.document(uri, mime, name)
                || !AttachmentSafety.document(uri, context.getContentResolver().getType(uri), name))
            throw new IOException("Blocked document metadata");
        File directory = SafeDocumentProvider.directory(context);
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cannot create cache");
        File[] old = directory.listFiles();
        long total = 0;
        if (old != null) for (File file : old) {
            if (System.currentTimeMillis() - file.lastModified() > 24L * 60 * 60 * 1000) file.delete();
            if (file.isFile()) total += file.length();
        }
        if (total > MAX_BYTES * 4) throw new IOException("Document cache full");
        File copy = File.createTempFile("checked_", ".bin", directory);
        boolean accepted = false;
        try {
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                    FileOutputStream output = new FileOutputStream(copy)) {
                if (input == null) throw new IOException("Unreadable document");
                byte[] buffer = new byte[16384];
                long size = 0;
                int count;
                long deadline = android.os.SystemClock.elapsedRealtime() + 7500;
                while ((count = input.read(buffer)) != -1) {
                    if (android.os.SystemClock.elapsedRealtime() > deadline) throw new IOException("Document read timed out");
                    size += count;
                    if (size > MAX_BYTES) throw new IOException("Document too large");
                    output.write(buffer, 0, count);
                }
            }
            checkContents(copy);
            accepted = true;
            return new Uri.Builder().scheme("content").authority(context.getPackageName() + ".checkeddocuments")
                    .appendPath(copy.getName()).appendQueryParameter("name", name == null ? "document" : name)
                    .appendQueryParameter("mime", mime == null || mime.isEmpty() ? "application/octet-stream" : mime).build();
        } finally { if (!accepted) copy.delete(); }
    }

    static void checkContents(File file) throws IOException {
        // APK/AAB/split archives are ZIP containers, even with a misleading extension or leading bytes.
        try (ZipFile zip = new ZipFile(file)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries = zip.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                if (++count > 10000) throw new IOException("Archive has too many entries");
                String entry = entries.nextElement().getName().replace('\\', '/').toLowerCase(Locale.ROOT);
                if (entry.equals("androidmanifest.xml") || entry.endsWith("/androidmanifest.xml")
                        || entry.endsWith(".apk") || entry.endsWith(".dex") || entry.equals("bundleconfig.pb"))
                    throw new IOException("Android executable package");
            }
        } catch (ZipException notZip) {
            byte[] header = new byte[8];
            int count;
            try (InputStream input = new java.io.FileInputStream(file)) { count = input.read(header); }
            if (count >= 2 && header[0] == 'P' && header[1] == 'K') throw new IOException("Invalid archive", notZip);
            if (count >= 4 && ((header[0] == 'd' && header[1] == 'e' && header[2] == 'x' && header[3] == '\n')
                    || (header[0] == 0x7f && header[1] == 'E' && header[2] == 'L' && header[3] == 'F')
                    || (header[0] == 'M' && header[1] == 'Z')))
                throw new IOException("Executable content");
        }
    }
}
