package com.ilia.advanceclock;

import android.app.*;
import android.view.*;
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
        return new AppUpdates.Release(new JSONObject().put("versionCode",7).put("versionName","1.0.0.7").put("tag","v1.0.0.7")
                .put("size",100).put("sha256","a".repeat(64)).put("releaseNotes",new JSONObject().put("en","New calendar features").put("fa","بهبود تقویم").put("ar","تحسين التقويم")).toString());
    }
    @Test public void sheetOnlyClosesWithExplicitButtonsAndAutomaticOfferIsOncePerVersion() throws Exception {
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();AppUpdates.prefs(host).edit().clear().commit();
            UpdatePrompt.show(host,release(),false);Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            assertTrue(dialog.isShowing());dialog.onBackPressed();assertTrue(dialog.isShowing());
            MotionEvent event=MotionEvent.obtain(0,0,MotionEvent.ACTION_OUTSIDE,-50,-50,0);dialog.onTouchEvent(event);event.recycle();assertTrue(dialog.isShowing());
            assertNull(Shadows.shadowOf(host).getNextStartedActivity());
            dialog.getWindow().getDecorView().findViewWithTag("update_later_button").performClick();assertFalse(dialog.isShowing());
            UpdatePrompt.show(host,release(),false);assertFalse(dialog.isShowing());
            UpdatePrompt.show(host,release(),true);dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();assertTrue(dialog.isShowing());
            dialog.getWindow().getDecorView().findViewWithTag("update_close_button").performClick();assertFalse(dialog.isShowing());
        }
    }
    @Test public void downloadStaysInSheetReportsProgressAndShowsPreciseFailureWithoutLaunchingActivity() throws Exception {
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();
            UpdatePrompt.Session sheet=new UpdatePrompt.Session(host,release(),(c,r,observer,request)->{
                observer.progress(50,100,false);throw new AppUpdates.UpdateFailure(R.string.update_hash_error);
            });
            sheet.show();sheet.download.performClick();sheet.request.thread.join(2000);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue(sheet.dialog.isShowing());assertNull(Shadows.shadowOf(host).getNextStartedActivity());
            assertEquals(500,sheet.progress.getProgress());assertTrue(sheet.amount.getText().toString().contains("50%"));
            assertEquals(host.getString(R.string.update_hash_error),sheet.status.getText().toString());
            assertTrue(sheet.download.isEnabled());sheet.close();
        }
    }
    @Test public void downloadIsPhysicallyRightAndLaterLeftInAllLanguagesAndNotesComeFromJson() throws Exception {
        AppString.init(RuntimeEnvironment.getApplication());
        for(String language:new String[]{"fa","en","ar"}) {
            AppSettings.setLanguage(RuntimeEnvironment.getApplication(),language);
            try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
                Activity host=controller.get();UpdatePrompt.Session sheet=new UpdatePrompt.Session(host,release());sheet.show();
                int width=Math.round(360*host.getResources().getDisplayMetrics().density);
                sheet.root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1800,View.MeasureSpec.AT_MOST));
                sheet.root.layout(0,0,width,sheet.root.getMeasuredHeight());
                assertTrue(sheet.download.getLeft()>sheet.later.getLeft());
                assertNull(sheet.download.getStateListAnimator());assertEquals(0f,sheet.download.getElevation(),0f);
                TextView body=sheet.root.findViewWithTag("update_release_notes");
                assertEquals(release().text("releaseNotes",language),body.getText().toString());sheet.close();
            }
        }
    }
    @Test public void leavingAppCancelsChecksButDoesNotDismissTheUsersSheet() throws Exception {
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();ForegroundUpdates.start(host);
            assertTrue(host.getSystemService(android.app.job.JobScheduler.class).getAllPendingJobs().isEmpty());
            UpdatePrompt.show(host,release(),true);Dialog dialog=org.robolectric.shadows.ShadowDialog.getLatestDialog();
            ForegroundUpdates.stop(host);assertTrue(dialog.isShowing());UpdatePrompt.dismiss(host);assertFalse(dialog.isShowing());
        }
    }
    @Test public void cancellingRequestDisconnectsActiveHttpConnection() throws Exception {
        java.util.concurrent.CountDownLatch disconnected=new java.util.concurrent.CountDownLatch(1);
        ForegroundUpdates.Request request=new ForegroundUpdates.Request(null);
        request.connection=new java.net.HttpURLConnection(new java.net.URL("https://github.com/owner/InfiniTime")) {
            @Override public void disconnect(){disconnected.countDown();}
            @Override public boolean usingProxy(){return false;}
            @Override public void connect(){}
        };
        request.cancel();assertTrue(request.cancelled);assertTrue(disconnected.await(1,java.util.concurrent.TimeUnit.SECONDS));
    }
}
