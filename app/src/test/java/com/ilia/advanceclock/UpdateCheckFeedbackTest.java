package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.view.View;
import android.widget.TextView;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class UpdateCheckFeedbackTest {
    private Context app,production;
    private final String repo="https://github.com/infinitimeilia/infinitime";
    @Before public void init() {
        app=RuntimeEnvironment.getApplication();AppString.init(app);AppUpdates.prefs(app).edit().clear().commit();
        production=new ContextWrapper(app) { @Override public String getPackageName() { return "com.ilia.advanceclock"; } };
        PackageInfo installed=new PackageInfo();installed.packageName=production.getPackageName();installed.setLongVersionCode(5);
        installed.versionName="1.0.0.5";installed.applicationInfo=new ApplicationInfo();installed.applicationInfo.packageName=installed.packageName;
        Shadows.shadowOf(app.getPackageManager()).installPackage(installed);
    }
    private String json(int version) throws Exception {
        return new JSONObject().put("versionCode",version).put("versionName","1.0.0."+version).put("tag","v1.0.0."+version)
                .put("size",100).put("sha256","a".repeat(64)).toString();
    }
    static class Connection extends HttpURLConnection {
        final int status;final String body;boolean disconnected;
        Connection(int status,String body) throws Exception { super(new URL("https://github.com/example/release"));this.status=status;this.body=body; }
        @Override public int getResponseCode() { return status; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)); }
        @Override public void disconnect() { disconnected=true; }
        @Override public void connect() {}
        @Override public boolean usingProxy() { return false; }
    }
    private AppUpdates.CheckResult check(int status,String body) throws Exception {
        Connection conn=new Connection(status,body);
        AppUpdates.CheckResult result=AppUpdates.check(production,new ForegroundUpdates.Request(null),repo,(url,request)->conn);
        assertTrue(conn.disconnected);return result;
    }
    @Test public void successfulChecksDistinguishNewVersionFromCurrentEvenAfterRecentAttempt() throws Exception {
        AppUpdates.prefs(app).edit().putLong("attempt",System.currentTimeMillis()).commit();
        assertEquals(AppUpdates.CheckResult.AVAILABLE,check(200,json(6)));
        assertNotNull(AppUpdates.available(production));
        assertEquals(AppUpdates.CheckResult.CURRENT,check(200,json(5)));
        assertNull(AppUpdates.available(production));
    }
    @Test public void absentInvalidAndUnavailableReleaseAreNotReportedAsUpToDate() throws Exception {
        assertEquals(AppUpdates.CheckResult.NOT_PUBLISHED,check(404,"Not Found"));
        assertEquals(AppUpdates.CheckResult.SERVER_ERROR,check(503,"Server unavailable"));
        assertEquals(AppUpdates.CheckResult.INVALID_METADATA,check(200,"not JSON"));
        assertEquals(AppUpdates.CheckResult.INVALID_METADATA,check(200,"x".repeat(16385)));
    }
    @Test public void timeoutSecureFailureAndCancellationHaveDistinctResults() {
        ForegroundUpdates.Request request=new ForegroundUpdates.Request(null);
        assertEquals(AppUpdates.CheckResult.NETWORK_ERROR,AppUpdates.check(production,request,repo,(u,r)->{throw new SocketTimeoutException();}));
        assertEquals(AppUpdates.CheckResult.SECURITY_ERROR,AppUpdates.check(production,request,repo,(u,r)->{throw new javax.net.ssl.SSLException("bad certificate");}));
        request.cancel();
        assertEquals(AppUpdates.CheckResult.CANCELLED,AppUpdates.check(production,request,repo,(u,r)->{throw new AssertionError("Cancelled check must not connect");}));
    }
    @Test public void internetCapabilityDoesNotRequireAndroidValidationWhichMayFailWithVpn() {
        ConnectivityManager manager=app.getSystemService(ConnectivityManager.class);
        Network network=manager.getActiveNetwork();assertNotNull(network);
        NetworkCapabilities caps=new NetworkCapabilities();Shadows.shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        Shadows.shadowOf(caps).removeCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        Shadows.shadowOf(manager).setNetworkCapabilities(network,caps);
        assertTrue(ForegroundUpdates.online(app));
        Shadows.shadowOf(caps).removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        Shadows.shadowOf(manager).setNetworkCapabilities(network,caps);assertFalse(ForegroundUpdates.online(app));
    }
    @Test public void aboutMainOffersExplicitCheckAndGitHubButLegalPageDoesNot() {
        try(var controller=Robolectric.buildActivity(AboutActivity.class).setup().visible()) {
            AboutActivity host=controller.get();View root=host.getWindow().getDecorView();
            View button=root.findViewWithTag("about_check_update");assertNotNull(button);
            ConnectivityManager manager=app.getSystemService(ConnectivityManager.class);
            Shadows.shadowOf(manager).setNetworkCapabilities(manager.getActiveNetwork(),new NetworkCapabilities());
            button.performClick();
            TextView status=root.findViewWithTag("about_update_status");
            assertTrue(button.isEnabled());assertEquals(host.getString(AppUpdates.production(host) ? R.string.update_no_internet : R.string.update_release_only),status.getText().toString());
            root.findViewWithTag("about_update_github").performClick();
            assertEquals(repo,Shadows.shadowOf(host).getNextStartedActivity().getDataString());
        }
        try(var controller=Robolectric.buildActivity(AboutActivity.class,new Intent().putExtra("document","privacy")).setup().visible()) {
            assertNull(controller.get().getWindow().getDecorView().findViewWithTag("about_check_update"));
        }
    }
    @Test public void downloadErrorsDistinguishInvalidFilesMissingApkAndConnectivity() throws Exception {
        assertEquals(R.string.update_download_missing,AppUpdates.errorMessage(new AppUpdates.UpdateFailure(R.string.update_download_missing)));
        assertEquals(R.string.update_network_error,AppUpdates.errorMessage(new UnknownHostException()));
        assertEquals(R.string.update_secure_error,AppUpdates.errorMessage(new javax.net.ssl.SSLException("certificate")));
        AppUpdates.Release release=new AppUpdates.Release(json(6));
        try { AppUpdates.verifyDigest(release,100,new byte[32]);fail(); }
        catch(AppUpdates.UpdateFailure error) { assertEquals(R.string.update_hash_error,AppUpdates.errorMessage(error)); }
    }
}
