package com.ilia.advanceclock;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** Only explicitly granted alarm image files are shared with the camera/viewer. */
public final class AlarmMediaProvider extends ContentProvider {
    static Uri create(android.content.Context context) throws java.io.IOException {
        File dir = new File(context.getFilesDir(), "alarm_media");
        if (!dir.exists() && !dir.mkdirs()) throw new java.io.IOException("Cannot create media directory");
        File file = File.createTempFile("photo_", ".jpg", dir);
        return new Uri.Builder().scheme("content").authority(context.getPackageName() + ".alarmmedia")
                .appendPath(file.getName()).build();
    }
    static void discard(android.content.Context context, String value) {
        try {
            Uri uri = Uri.parse(value);
            if (!(context.getPackageName() + ".alarmmedia").equals(uri.getAuthority())) return;
            File dir = new File(context.getFilesDir(), "alarm_media").getCanonicalFile();
            File file = new File(dir, uri.getLastPathSegment()).getCanonicalFile();
            if (dir.equals(file.getParentFile()) && file.getName().matches("photo_[A-Za-z0-9]+\\.jpg")) file.delete();
        } catch (Exception ignored) {}
    }
    private File file(Uri uri) throws FileNotFoundException {
        if (uri == null || uri.getLastPathSegment() == null || uri.getPathSegments().size() != 1)
            throw new FileNotFoundException();
        try {
            File dir = new File(getContext().getFilesDir(), "alarm_media").getCanonicalFile();
            File file = new File(dir, uri.getLastPathSegment()).getCanonicalFile();
            if (!dir.equals(file.getParentFile()) || !file.getName().matches("photo_[A-Za-z0-9]+\\.jpg"))
                throw new FileNotFoundException();
            return file;
        } catch (java.io.IOException e) { throw new FileNotFoundException(); }
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "image/jpeg"; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.parseMode(mode));
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        try {
            File file = file(uri);
            String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
            MatrixCursor cursor = new MatrixCursor(columns);
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) values[i] = file.getName();
                else if (OpenableColumns.SIZE.equals(columns[i])) values[i] = file.length();
            }
            cursor.addRow(values); return cursor;
        } catch (FileNotFoundException ignored) { return null; }
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String where, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String where, String[] args) { throw new UnsupportedOperationException(); }
}
