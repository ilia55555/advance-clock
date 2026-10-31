package com.ilia.advanceclock;

import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
import java.util.UUID;

/** Private verified APK staging; only the installer receives a temporary read grant. */
public final class UpdatePackageProvider extends ContentProvider {
    static final String MIME="application/vnd.android.package-archive";
    static Uri stage(Context c,File verified) throws IOException {
        File dir=new File(c.getFilesDir(),"verified_updates");
        if(!dir.isDirectory() && !dir.mkdirs())throw new IOException("Unable to stage update");
        File[] old=dir.listFiles();
        if(old!=null)for(File f:old)if(f.getName().matches("update-[a-f0-9]{32}\\.apk") && System.currentTimeMillis()-f.lastModified()>7L*86400000)f.delete();
        File target=new File(dir,"update-"+UUID.randomUUID().toString().replace("-","")+".apk");
        boolean done=false;
        try {
            try(InputStream in=new FileInputStream(verified);FileOutputStream out=new FileOutputStream(target)) {
                byte[] buffer=new byte[32768];int n;
                while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new IOException("Cancelled");out.write(buffer,0,n);}
                out.getFD().sync();
            }
            done=true;
        }finally{if(!done)target.delete();}
        return new Uri.Builder().scheme("content").authority(c.getPackageName()+".updates").appendPath(target.getName()).build();
    }
    private File file(Uri uri)throws FileNotFoundException {
        try {
            if(uri==null || !"content".equals(uri.getScheme()) || !(getContext().getPackageName()+".updates").equals(uri.getAuthority())
                    || uri.getQuery()!=null || uri.getFragment()!=null || uri.getPathSegments().size()!=1
                    || !uri.getLastPathSegment().matches("update-[a-f0-9]{32}\\.apk"))throw new IOException("Invalid update URI");
            File dir=new File(getContext().getFilesDir(),"verified_updates").getCanonicalFile();
            File f=new File(dir,uri.getLastPathSegment()).getCanonicalFile();
            if(!dir.equals(f.getParentFile()) || !f.isFile())throw new IOException("Update unavailable");
            return f;
        }catch(IOException error){throw new FileNotFoundException("Update unavailable");}
    }
    @Override public boolean onCreate(){return true;}
    @Override public String getType(Uri uri){return MIME;}
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException {
        if(!"r".equals(mode))throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String order){
        File f;try{f=file(uri);}catch(FileNotFoundException e){return null;}
        String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;
        MatrixCursor cursor=new MatrixCursor(columns);Object[] values=new Object[columns.length];
        for(int i=0;i<columns.length;i++)values[i]=OpenableColumns.DISPLAY_NAME.equals(columns[i])?"InfiniTime.apk":OpenableColumns.SIZE.equals(columns[i])?f.length():null;
        cursor.addRow(values);return cursor;
    }
    @Override public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException();}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args){throw new UnsupportedOperationException();}
    @Override public int delete(Uri uri,String selection,String[] args){throw new UnsupportedOperationException();}
}
