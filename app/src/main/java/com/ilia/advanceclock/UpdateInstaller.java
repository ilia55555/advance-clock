package com.ilia.advanceclock;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.provider.*;

/** System-confirmed installation only. Never bypasses the unknown-source permission. */
final class UpdateInstaller {
    static final int PERMISSION_REQUEST=8262,INSTALL_REQUEST=8263;
    static boolean allowed(Activity host){return host.getPackageManager().canRequestPackageInstalls();}
    static Intent permissionIntent(Activity host){return new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+host.getPackageName()));}
    static Intent installIntent(Uri apk){
        Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(apk,UpdatePackageProvider.MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setClipData(ClipData.newRawUri("InfiniTime update",apk));
        return intent;
    }
    static boolean launch(Activity host,Uri apk){
        if(!allowed(host) || apk==null || !"content".equals(apk.getScheme()) || !(host.getPackageName()+".updates").equals(apk.getAuthority()))return false;
        try{host.startActivityForResult(installIntent(apk),INSTALL_REQUEST);return true;}
        catch(ActivityNotFoundException|SecurityException e){return false;}
    }
    static Uri folderFor(Uri saved) {
        if(saved!=null && "com.android.externalstorage.documents".equals(saved.getAuthority())) {
            try {
                String id=DocumentsContract.getDocumentId(saved);int slash=id.lastIndexOf('/');int colon=id.indexOf(':');
                String parent=slash>=0?id.substring(0,slash):colon>=0?id.substring(0,colon+1):null;
                if(parent!=null)return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents",parent);
            }catch(IllegalArgumentException ignored){}
        }
        return DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Download/InfiniTime");
    }
    static boolean openFiles(Activity host){return openFiles(host,null);}
    static boolean openFiles(Activity host,Uri saved){
        Uri folder=folderFor(saved);
        if(tryOpen(host,new Intent(Intent.ACTION_VIEW).setDataAndType(folder,"vnd.android.document/directory")))return true;
        Intent selector=Intent.makeMainSelectorActivity(Intent.ACTION_MAIN,"android.intent.category.APP_FILES");
        if(tryOpen(host,selector))return true;
        for(String pkg:new String[]{"com.sec.android.app.myfiles","com.mi.android.globalFileexplorer","com.android.fileexplorer",
                "com.huawei.hidisk","com.hihonor.filemanager","com.google.android.apps.nbu.files"}){
            Intent intent=host.getPackageManager().getLaunchIntentForPackage(pkg);
            if(intent!=null && tryOpen(host,intent))return true;
        }
        // A system Files browser is preferable to DownloadManager, which does not list MediaStore exports.
        return tryOpen(host,new Intent("android.provider.action.BROWSE").setData(folder));
    }
    private static boolean tryOpen(Activity host,Intent intent){
        if(intent.resolveActivity(host.getPackageManager())==null)return false;
        try{host.startActivity(intent);return true;}catch(ActivityNotFoundException|SecurityException e){return false;}
    }
}
