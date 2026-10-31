package com.ilia.advanceclock;

import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.*;
import java.io.*;

/** Exports only an APK that already passed size, hash, package and signer validation. */
final class UpdateFiles {
    static void copy(InputStream in, OutputStream out, ForegroundUpdates.Request request) throws IOException {
        if (in == null || out == null) throw new IOException("No output");
        byte[] buffer=new byte[32768];int n;
        while ((n=in.read(buffer))!=-1) {
            if (request.cancelled || Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
            out.write(buffer,0,n);
        }
    }
    @android.annotation.TargetApi(29)
    static Uri publish(Context c, File verified, String name, ForegroundUpdates.Request request) throws Exception {
        ContentValues values=new ContentValues();values.put(MediaStore.Downloads.DISPLAY_NAME,name);
        values.put(MediaStore.Downloads.MIME_TYPE,"application/vnd.android.package-archive");
        values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/InfiniTime");
        values.put(MediaStore.Downloads.IS_PENDING,1);
        Uri uri=null;
        try {
            uri=c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
            if (uri==null) throw new IOException("No destination");
            try(InputStream in=new FileInputStream(verified);OutputStream out=c.getContentResolver().openOutputStream(uri)) { copy(in,out,request); }
            if(request.cancelled)throw new IOException("Cancelled");
            values.clear();values.put(MediaStore.Downloads.IS_PENDING,0);
            if(c.getContentResolver().update(uri,values,null,null)==0)throw new IOException("Unable to publish");
            return uri;
        } catch(Exception e) {
            if(uri!=null)try{c.getContentResolver().delete(uri,null,null);}catch(Exception ignored){}
            throw new AppUpdates.UpdateFailure(R.string.update_storage_error);
        } finally { verified.delete(); }
    }
    static void save(Context c,Uri verified,Uri uri,ForegroundUpdates.Request request) throws Exception {
        try(InputStream in=c.getContentResolver().openInputStream(verified);OutputStream out=c.getContentResolver().openOutputStream(uri)) { copy(in,out,request); }
        catch(Exception error) {
            try{DocumentsContract.deleteDocument(c.getContentResolver(),uri);}catch(Exception ignored){}
            throw new AppUpdates.UpdateFailure(R.string.update_storage_error);
        }
    }
    static void save(Context c,File verified,Uri uri,ForegroundUpdates.Request request) throws Exception {
        try(InputStream in=new FileInputStream(verified);OutputStream out=c.getContentResolver().openOutputStream(uri)) { copy(in,out,request); }
        catch(Exception error) {
            try{DocumentsContract.deleteDocument(c.getContentResolver(),uri);}catch(Exception ignored){}
            throw new AppUpdates.UpdateFailure(R.string.update_storage_error);
        }
    }
}
