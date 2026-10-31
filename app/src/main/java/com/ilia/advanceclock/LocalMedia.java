package com.ilia.advanceclock;

import android.net.Uri;

/** Native media components must never create an unfiltered HTTP/RTSP network connection. */
final class LocalMedia {
    static Uri require(Uri uri){
        if(uri==null)throw new IllegalArgumentException("Missing local media");
        String scheme=uri.getScheme();
        if("content".equals(scheme)||"android.resource".equals(scheme)||"file".equals(scheme))return uri;
        throw new IllegalArgumentException("Only local media is allowed");
    }
}
