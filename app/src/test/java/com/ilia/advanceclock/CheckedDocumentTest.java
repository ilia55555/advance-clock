package com.ilia.advanceclock;

import android.app.Application;
import android.net.Uri;
import java.io.*;
import java.util.zip.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowContentResolver;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class CheckedDocumentTest {
    private File archive(String entry) throws Exception {
        File file = File.createTempFile("renamed", ".dat", RuntimeEnvironment.getApplication().getCacheDir());
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            zip.putNextEntry(new ZipEntry(entry)); zip.write(new byte[]{1,2,3}); zip.closeEntry();
        }
        return file;
    }
    @Test public void renamedApkAabAndSplitPackagesAreRejectedByContents() throws Exception {
        for (String entry : new String[]{"AndroidManifest.xml", "base/manifest/AndroidManifest.xml", "base.apk", "classes.dex", "BundleConfig.pb"}) {
            try { SafeDocumentOpener.checkContents(archive(entry)); fail(entry); }
            catch (IOException expected) {}
        }
        SafeDocumentOpener.checkContents(archive("documents/readme.txt"));
    }
    @Test public void prefixedApkStillCannotPassAsAnOrdinaryFile() throws Exception {
        File archive = archive("AndroidManifest.xml");
        byte[] zip = java.nio.file.Files.readAllBytes(archive.toPath());
        try (FileOutputStream out = new FileOutputStream(archive)) { out.write(new byte[]{1,2,3}); out.write(zip); }
        try { SafeDocumentOpener.checkContents(archive); fail("Prefixed APK must be blocked"); }
        catch (IOException expected) {}
    }
    @Test public void grantedCopyRemainsStableAndProviderRejectsWritesAndTraversal() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Uri original = Uri.parse("content://documents/snapshot");
        byte[] bytes = "%PDF-1.4 original".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ShadowsRegister(original, bytes);
        Uri copy = SafeDocumentOpener.prepare(app, original, "application/pdf", "notes.pdf");
        SafeDocumentProvider provider = Robolectric.buildContentProvider(SafeDocumentProvider.class).create().get();
        ShadowsRegister(original, "changed".getBytes());
        try (InputStream input = new android.os.ParcelFileDescriptor.AutoCloseInputStream(provider.openFile(copy, "r"))) {
            ByteArrayOutputStream read = new ByteArrayOutputStream();
            int value; while ((value = input.read()) != -1) read.write(value);
            assertArrayEquals(bytes, read.toByteArray());
        }
        try { provider.openFile(copy, "rw"); fail("Write grant"); } catch (FileNotFoundException expected) {}
        try { provider.openFile(Uri.parse("content://" + app.getPackageName() + ".checkeddocuments/%2E%2E%2Fsecret"), "r"); fail("Traversal"); }
        catch (FileNotFoundException expected) {}
    }
    @Test public void snapshotPreparationRejectsRenamedPackagesEvenWithGenericMime() throws Exception {
        android.content.Context app = RuntimeEnvironment.getApplication();
        Uri uri = Uri.parse("content://documents/renamed");
        ShadowsRegister(uri, java.nio.file.Files.readAllBytes(archive("AndroidManifest.xml").toPath()));
        try { SafeDocumentOpener.prepare(app, uri, "application/octet-stream", "photo.dat"); fail("Renamed package"); }
        catch (IOException expected) {}
    }
    private void ShadowsRegister(Uri uri, byte[] bytes) {
        org.robolectric.Shadows.shadowOf(RuntimeEnvironment.getApplication().getContentResolver())
                .registerInputStream(uri, new ByteArrayInputStream(bytes));
    }
}
