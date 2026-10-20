package com.ilia.advanceclock;

import android.net.Uri;
import java.util.Locale;

/** Limits external viewers to selected documents and ordinary web links. Not a malware scanner. */
final class AttachmentSafety {
    private AttachmentSafety() {}

    static boolean webLink(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme))
                && uri.getHost() != null && !uri.getHost().isEmpty();
    }

    static boolean document(Uri uri, String mime, String name) {
        if (uri == null || !"content".equalsIgnoreCase(uri.getScheme())) return false;
        String type = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if (type.contains("android.package-archive")) return false;
        return !installerName(name) && !installerName(uri.getLastPathSegment());
    }

    private static boolean installerName(String name) {
        String value = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        return value.endsWith(".apk") || value.endsWith(".apks")
                || value.endsWith(".xapk") || value.endsWith(".aab");
    }
}
