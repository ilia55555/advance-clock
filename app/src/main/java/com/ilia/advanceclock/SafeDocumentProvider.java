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

/** Only read grants to immutable, checked copies; original document providers are never delegated. */
public final class SafeDocumentProvider extends ContentProvider {
    static File directory(android.content.Context context) {
        return new File(context.getCacheDir(), "checked_documents");
    }
    private File file(Uri uri) throws FileNotFoundException {
        if (uri == null || uri.getPathSegments().size() != 1
                || !uri.getLastPathSegment().matches("checked_[A-Za-z0-9]+\\.bin"))
            throw new FileNotFoundException();
        try {
            File directory = directory(getContext()).getCanonicalFile();
            File file = new File(directory, uri.getLastPathSegment()).getCanonicalFile();
            if (!directory.equals(file.getParentFile()) || !file.isFile()) throw new FileNotFoundException();
            return file;
        } catch (java.io.IOException error) { throw new FileNotFoundException(); }
    }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) {
        String mime = uri.getQueryParameter("mime");
        return mime == null || mime.isEmpty() ? "application/octet-stream" : mime;
    }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read-only document");
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        try {
            File file = file(uri);
            String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
            MatrixCursor result = new MatrixCursor(columns);
            Object[] values = new Object[columns.length];
            for (int i = 0; i < columns.length; i++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[i])) {
                    String name = uri.getQueryParameter("name");
                    values[i] = name == null ? "document" : name;
                } else if (OpenableColumns.SIZE.equals(columns[i])) values[i] = file.length();
            }
            result.addRow(values); return result;
        } catch (FileNotFoundException error) { return null; }
    }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
