package com.ilia.advanceclock;

import android.app.Application;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class AttachmentSafetyTest {
    @Test public void onlyWebSchemesAreAccepted() {
        assertTrue(AttachmentSafety.webLink(Uri.parse("https://example.com/path")));
        assertTrue(AttachmentSafety.webLink(Uri.parse("http://example.com")));
        for (String value : new String[]{"intent://install", "file:///private", "content://private", "javascript:alert(1)", "https:"})
            assertFalse(value, AttachmentSafety.webLink(Uri.parse(value)));
    }
    @Test public void installationPackagesAndPrivateFileUrisCannotBeOpened() {
        Uri document = Uri.parse("content://documents/123");
        assertTrue(AttachmentSafety.document(document, "application/pdf", "notes.pdf"));
        assertFalse(AttachmentSafety.document(document, "application/vnd.android.package-archive", "photo.jpg"));
        assertFalse(AttachmentSafety.document(document, "*/*", "app.APK"));
        assertFalse(AttachmentSafety.document(Uri.parse("content://documents/app.apk"), "*/*", "photo.jpg"));
        assertFalse(AttachmentSafety.document(Uri.parse("file:///data/private.xml"), "text/xml", "notes.xml"));
    }
    @Test public void widgetAndBootReceiversAreNotExported() throws Exception {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        android.content.pm.PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(), android.content.pm.PackageManager.GET_RECEIVERS);
        for (android.content.pm.ActivityInfo receiver : info.receivers)
            assertFalse(receiver.name, receiver.exported);
    }
}
