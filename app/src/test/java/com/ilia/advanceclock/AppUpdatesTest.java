package com.ilia.advanceclock;

import android.app.Application;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.net.URL;
import java.security.MessageDigest;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk={27,35})
public class AppUpdatesTest {
    private Context app;
    private final String hash = "a".repeat(64);
    @Before public void init() { app=RuntimeEnvironment.getApplication(); AppUpdates.prefs(app).edit().clear().commit(); }
    private String json() throws Exception {
        return new JSONObject().put("versionCode",5).put("versionName","1.0.0.5").put("tag","v1.0.0.5")
                .put("size",100).put("sha256",hash).toString();
    }
    @Test public void metadataCannotChooseDownloadHostOrFileName() throws Exception {
        AppUpdates.Release r=new AppUpdates.Release(json());
        assertEquals("https://github.com/ilia55555/advance-clock/releases/download/v1.0.0.5/InfiniTime.apk",r.url("https://github.com/ilia55555/advance-clock"));
        for (String[] change:new String[][]{{"tag","../../evil"},{"versionName","1.0.0.5/evil"},{"sha256","missing"},
                {"size","0"},{"size","157286401"},{"versionCode","-1"}}) {
            JSONObject o=new JSONObject(json()).put(change[0],change[1]);
            assertThrows(Exception.class,()->new AppUpdates.Release(o.toString()));
        }
        assertThrows(Exception.class,()->new AppUpdates.Release("{}"));
    }
    @Test public void redirectsRequireExactHttpsHostsWithoutCredentialsOrAlternatePort() throws Exception {
        for(String u:new String[]{"http://github.com/file","https://github.com.evil.test/file","https://evil.test/file",
                "https://user@github.com/file","https://github.com:8443/file","file:///tmp/update.apk"})
            assertFalse(u,AppUpdates.allowed(new URL(u)));
        assertTrue(AppUpdates.allowed(new URL("https://release-assets.githubusercontent.com/file?token=123")));
        assertTrue(AppUpdates.allowed(new URL("https://github.com/ilia55555/advance-clock/releases")));
    }
    private PackageInfo pkg(long code,String signer) {
        PackageInfo p=new PackageInfo(); p.packageName="com.ilia.advanceclock"; p.versionName="1.0.0."+code;
        if(Build.VERSION.SDK_INT>=28) p.setLongVersionCode(code); else p.versionCode=(int)code;
        p.applicationInfo=new ApplicationInfo();
        Signature[] sigs={new Signature(signer.getBytes(java.nio.charset.StandardCharsets.UTF_8))};
        if(Build.VERSION.SDK_INT>=28) { p.signingInfo=new SigningInfo(); Shadows.shadowOf(p.signingInfo).setSignatures(sigs); }
        else p.signatures=sigs;
        return p;
    }
    @Test public void onlyNewerSameSignerProductionApkPasses() throws Exception {
        var r=new AppUpdates.Release(json()); PackageInfo current=pkg(4,"owner");
        AppUpdates.verify(current,pkg(5,"owner"),r);
        assertThrows(Exception.class,()->AppUpdates.verify(current,pkg(5,"attacker"),r));
        assertThrows(Exception.class,()->AppUpdates.verify(current,pkg(4,"owner"),r));
        assertThrows(Exception.class,()->AppUpdates.verify(current,pkg(6,"owner"),r));
        assertThrows(Exception.class,()->AppUpdates.verify(pkg(5,"owner"),pkg(5,"owner"),r));
        assertThrows(Exception.class,()->AppUpdates.verify(current,null,r));
        PackageInfo bad=pkg(5,"owner");bad.packageName+=".debug";
        assertThrows(Exception.class,()->AppUpdates.verify(current,bad,r));
        bad.packageName="com.ilia.advanceclock";bad.versionName="1.0.0.6";
        assertThrows(Exception.class,()->AppUpdates.verify(current,bad,r));
        bad.versionName="1.0.0.5";bad.applicationInfo.flags=ApplicationInfo.FLAG_DEBUGGABLE;
        assertThrows(Exception.class,()->AppUpdates.verify(current,bad,r));
        bad.applicationInfo.flags=ApplicationInfo.FLAG_TEST_ONLY;
        assertThrows(Exception.class,()->AppUpdates.verify(current,bad,r));
        bad.applicationInfo.flags=0;bad.signatures=null;
        if(Build.VERSION.SDK_INT>=28)bad.signingInfo=null;
        assertThrows(Exception.class,()->AppUpdates.verify(current,bad,r));
    }
    @Test public void exactDigestAndSizeAreRequired() throws Exception {
        byte[] bytes="verified APK".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);
        var r=new AppUpdates.Release(new JSONObject(json()).put("sha256",AppUpdates.hex(digest)).put("size",bytes.length).toString());
        AppUpdates.verifyDigest(r,bytes.length,digest);
        assertThrows(Exception.class,()->AppUpdates.verifyDigest(r,bytes.length-1,digest));
        digest[0]^=1;assertThrows(Exception.class,()->AppUpdates.verifyDigest(r,bytes.length,digest));
    }
    @Test public void debugBuildNeverOffersProductionUpdate() throws Exception {
        Assume.assumeTrue(app.getPackageName().endsWith(".debug"));
        AppUpdates.prefs(app).edit().putString("source","https://github.com/owner/InfiniTime").putLong("success",System.currentTimeMillis()).putString("release",json()).commit();
        assertNull(AppUpdates.available(app));
        AppUpdates.cancelLegacyJobs(app);
        assertNull(app.getSystemService(android.app.job.JobScheduler.class).getPendingJob(AppUpdates.JOB_ID));
    }
    @Test public void availabilityExpiresAndInstalledVersionsAreNotOffered() throws Exception {
        Context production=new ContextWrapper(app) { @Override public String getPackageName() { return "com.ilia.advanceclock"; } @Override public Context getApplicationContext() { return this; } };
        PackageInfo installed=pkg(4,"owner"); installed.applicationInfo.packageName=installed.packageName;
        Shadows.shadowOf(app.getPackageManager()).installPackage(installed);
        AppUpdates.prefs(app).edit().putString("source","https://github.com/owner/InfiniTime").putLong("success",System.currentTimeMillis()).putString("release",json()).commit();
        assertNotNull(AppUpdates.available(production,"https://github.com/owner/InfiniTime"));
        assertNull(AppUpdates.available(production,"https://github.com/owner/OtherApp"));
        AppUpdates.prefs(app).edit().putLong("success",System.currentTimeMillis()-8L*86400000).commit();
        assertNull(AppUpdates.available(production,"https://github.com/owner/InfiniTime"));
        installed=pkg(5,"owner");installed.applicationInfo.packageName=installed.packageName;
        Shadows.shadowOf(app.getPackageManager()).installPackage(installed);
        AppUpdates.prefs(app).edit().putLong("success",System.currentTimeMillis()).commit();
        assertNull(AppUpdates.available(production,"https://github.com/owner/InfiniTime"));
    }
    @Test public void legacyBackgroundJobsAreCancelledAndDedicatedRepositoryIsConfigured() {
        var jobs=app.getSystemService(android.app.job.JobScheduler.class);
        android.content.ComponentName component=new ComponentName(app,"com.ilia.advanceclock.UpdateCheckJob");
        jobs.schedule(new android.app.job.JobInfo.Builder(AppUpdates.JOB_ID,component).setPeriodic(86400000).build());
        jobs.schedule(new android.app.job.JobInfo.Builder(AppUpdates.DUE_JOB_ID,component).setMinimumLatency(1000).build());
        assertEquals(2,jobs.getAllPendingJobs().size());
        AppUpdates.cancelLegacyJobs(app);assertEquals(0,jobs.getAllPendingJobs().size());
        assertEquals("https://github.com/infinitimeilia/infinitime",AppUpdates.repository(app)); assertNull(AppUpdates.available(app));
        assertTrue(AppUpdates.validRepository("https://github.com/owner/InfiniTime"));
        for(String bad:new String[]{"", "https://evil.test/a/b","https://github.com.evil.test/a/b","http://github.com/a/b","https://github.com/a/b?next=evil"})
            assertFalse(bad,AppUpdates.validRepository(bad));
    }
    @Test public void rapidActivityReturnsAreThrottledWithoutSchedulingRetries() {
        long now=10L*AppUpdates.ENTRY_THROTTLE;
        assertTrue(AppUpdates.due(now,0));
        assertFalse(AppUpdates.due(now,now-AppUpdates.ENTRY_THROTTLE+1));
        assertTrue(AppUpdates.due(now,now-AppUpdates.ENTRY_THROTTLE));
        assertTrue(AppUpdates.due(now,now+1000));
    }
    @Test public void releaseNotesUseSelectedLanguageAndPlainTextFallback() throws Exception {
        String body=new JSONObject(json()).put("title",new JSONObject().put("fa","نسخه جدید").put("en","New version"))
                .put("releaseNotes",new JSONObject().put("fa","ویژگی اول\nویژگی دوم").put("en","<script>plain text</script>")).toString();
        var r=new AppUpdates.Release(body);
        assertEquals("نسخه جدید",r.text("title","fa"));
        assertEquals("New version",r.text("title","ar"));
        assertEquals("ویژگی اول\nویژگی دوم",r.text("releaseNotes","fa"));
        assertEquals("<script>plain text</script>",r.text("releaseNotes","ar"));
        assertEquals("",r.text("missing","fa"));
    }
    @Test public void updateComponentsPrivateAndNoInstallationPermission() throws Exception {
        var pm=app.getPackageManager();
        assertFalse(pm.getActivityInfo(new ComponentName(app,UpdateDownloadActivity.class),0).exported);
        for (ServiceInfo service : pm.getPackageInfo(app.getPackageName(),PackageManager.GET_SERVICES).services)
            assertNotEquals("com.ilia.advanceclock.UpdateCheckJob",service.name);
        String[] requested=pm.getPackageInfo(app.getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions;
        assertFalse(java.util.Arrays.asList(requested).contains("android.permission.REQUEST_INSTALL_PACKAGES"));
    }
}
