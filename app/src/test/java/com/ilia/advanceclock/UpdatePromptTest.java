package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.widget.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class UpdatePromptTest {
    private AppUpdates.Release release() throws Exception {
        return new AppUpdates.Release(new JSONObject().put("versionCode",5).put("versionName","1.0.0.5").put("tag","v1.0.0.5")
                .put("size",100).put("sha256","a".repeat(64)).put("releaseNotes","New calendar features").toString());
    }
    @Test public void optionalSheetAppearsOnceAndNeverStartsDownloadUntilExplicitClick() throws Exception {
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get(); AppUpdates.prefs(host).edit().clear().commit();
            UpdatePrompt.show(host,release(),false);
            Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertTrue(dialog.isShowing());
            assertEquals(android.view.Gravity.BOTTOM,dialog.getWindow().getAttributes().gravity);
            TextView notes=dialog.getWindow().getDecorView().findViewWithTag("update_release_notes");
            assertEquals("New calendar features",notes.getText().toString());
            assertNull(Shadows.shadowOf(host).getNextStartedActivity());
            dialog.dismiss();UpdatePrompt.show(host,release(),false);assertFalse(dialog.isShowing());
            UpdatePrompt.show(host,release(),true);
            dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertTrue(dialog.isShowing());
            Button download=dialog.getWindow().getDecorView().findViewWithTag("update_download_button");download.performClick();
            Intent intent=Shadows.shadowOf(host).getNextStartedActivity();
            assertEquals(UpdateDownloadActivity.class.getName(),intent.getComponent().getClassName());
            assertFalse(dialog.isShowing());
        }
    }
    @Test public void cancellingRequestDisconnectsActiveHttpConnection() throws Exception {
        java.util.concurrent.CountDownLatch disconnected=new java.util.concurrent.CountDownLatch(1);
        ForegroundUpdates.Request request=new ForegroundUpdates.Request(null);
        request.connection=new java.net.HttpURLConnection(new java.net.URL("https://github.com/owner/InfiniTime")) {
            @Override public void disconnect() { disconnected.countDown(); }
            @Override public boolean usingProxy() { return false; }
            @Override public void connect() {}
        };
        request.cancel();assertTrue(request.cancelled);
        assertTrue(disconnected.await(1,java.util.concurrent.TimeUnit.SECONDS));
    }
    @Test public void leavingAppCancelsChecksAndClosesPromptWithoutSchedulingJobs() throws Exception {
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();
            ForegroundUpdates.start(host); // Repository deliberately unset, no network or scheduled task.
            assertTrue(host.getSystemService(android.app.job.JobScheduler.class).getAllPendingJobs().isEmpty());
            UpdatePrompt.show(host,release(),true);Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            ForegroundUpdates.stop(host);assertFalse(dialog.isShowing());
            ForegroundUpdates.Request request=new ForegroundUpdates.Request(host);request.cancel();assertTrue(request.cancelled);
        }
    }
}
