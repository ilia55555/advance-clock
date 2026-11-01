package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import java.io.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class UpdateInstallationTest {
    private Context app;
    @Before public void setup(){app=RuntimeEnvironment.getApplication();AppString.init(app);}
    private Uri staged()throws Exception {
        File source=File.createTempFile("verified-test-",".apk",app.getCacheDir());
        try(FileOutputStream out=new FileOutputStream(source)){out.write(new byte[]{1,2,3,4});}
        Uri uri=UpdatePackageProvider.stage(app,source);source.delete();return uri;
    }
    private AppUpdates.Release release()throws Exception {
        return new AppUpdates.Release(new JSONObject().put("versionCode",8).put("versionName","1.0.0.8").put("tag","v1.0.0.8")
                .put("size",4).put("sha256","a".repeat(64)).toString());
    }
    @Test public void stagingProviderIsPrivateReadOnlyAndRejectsTraversal()throws Exception {
        Uri uri=staged();ProviderInfo info=app.getPackageManager().resolveContentProvider(app.getPackageName()+".updates",0);
        assertNotNull(info);assertFalse(info.exported);assertTrue(info.grantUriPermissions);
        try(ParcelFileDescriptor fd=app.getContentResolver().openFileDescriptor(uri,"r")){assertNotNull(fd);assertEquals(4,fd.getStatSize());}
        assertThrows(FileNotFoundException.class,()->app.getContentResolver().openFileDescriptor(uri,"rw"));
        Uri traversal=Uri.parse("content://"+app.getPackageName()+".updates/%2e%2e%2fsecret.apk");
        assertThrows(FileNotFoundException.class,()->app.getContentResolver().openFileDescriptor(traversal,"r"));
        assertThrows(FileNotFoundException.class,()->app.getContentResolver().openFileDescriptor(uri.buildUpon().appendQueryParameter("path","secret").build(),"r"));
    }
    @Test public void installerIntentGrantsOnlyReadAccessToPrivateContentUri()throws Exception {
        Uri uri=staged();Intent intent=UpdateInstaller.installIntent(uri);
        assertEquals(Intent.ACTION_VIEW,intent.getAction());assertEquals(UpdatePackageProvider.MIME,intent.getType());
        assertEquals(uri,intent.getData());assertEquals(uri,intent.getClipData().getItemAt(0).getUri());
        assertTrue((intent.getFlags()&Intent.FLAG_GRANT_READ_URI_PERMISSION)!=0);
        assertEquals(0,intent.getFlags()&Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
    }
    @Test public void missingPermissionOpensSettingsAndDenialShowsManualFallback()throws Exception {
        Shadows.shadowOf(app.getPackageManager()).setCanRequestPackageInstalls(false);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();UpdatePrompt.Session sheet=new UpdatePrompt.Session(host,release());sheet.show();sheet.ready=true;sheet.installUri=staged();
            sheet.download.performClick();
            assertNull(Shadows.shadowOf(host).getNextStartedActivity());
            assertTrue(sheet.optionalPermissionWarning.isShowing());
            sheet.optionalPermissionWarning.getContentView().findViewWithTag("permissions_warning_grant").performClick();
            Intent settings=Shadows.shadowOf(host).getNextStartedActivity();
            assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,settings.getAction());
            assertEquals("package:"+host.getPackageName(),settings.getDataString());
            sheet.installPermissionReturned();assertEquals(host.getString(R.string.update_install_denied),sheet.status.getText().toString());
            assertEquals(android.view.View.VISIBLE,sheet.files.getVisibility());assertTrue(sheet.dialog.isShowing());
            assertNull(Shadows.shadowOf(host).getNextStartedActivity());sheet.close();
        }
    }
    @Test public void permissionApprovalLaunchesSystemInstallerAndArbitraryUrisAreRejected()throws Exception {
        Shadows.shadowOf(app.getPackageManager()).setCanRequestPackageInstalls(true);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();UpdatePrompt.Session sheet=new UpdatePrompt.Session(host,release());sheet.show();sheet.ready=true;sheet.installUri=staged();
            sheet.installPermissionReturned();Intent install=Shadows.shadowOf(host).getNextStartedActivity();
            assertEquals(Intent.ACTION_VIEW,install.getAction());assertEquals(sheet.installUri,install.getData());
            assertTrue(sheet.dialog.isShowing());assertFalse(UpdateInstaller.launch(host,Uri.parse("https://evil.test/app.apk")));sheet.close();
        }
    }
    @Test public void manualSaveUsesActualSelectedFolderInsteadOfAlwaysOpeningDownloads() {
        Uri saved=android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Documents/Updates/InfiniTime.apk");
        Uri folder=UpdateInstaller.folderFor(saved);
        assertEquals("primary:Documents/Updates",android.provider.DocumentsContract.getDocumentId(folder));
    }
    @Test public void fileManagerUsesFolderBrowserInsteadOfDownloadManager() {
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();
            Uri folder=android.provider.DocumentsContract.buildDocumentUri("com.android.externalstorage.documents","primary:Download/InfiniTime");
            Intent browse=new Intent(Intent.ACTION_VIEW).setDataAndType(folder,"vnd.android.document/directory");
            ResolveInfo handler=new ResolveInfo();handler.activityInfo=new ActivityInfo();handler.activityInfo.packageName="files.example";handler.activityInfo.name="Files";
            Shadows.shadowOf(host.getPackageManager()).addResolveInfoForIntent(browse,handler);
            assertTrue(UpdateInstaller.openFiles(host));Intent opened=Shadows.shadowOf(host).getNextStartedActivity();
            assertEquals(folder,opened.getData());assertEquals("vnd.android.document/directory",opened.getType());
        }
    }
}
