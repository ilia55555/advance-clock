package com.ilia.advanceclock;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {28, 35})
public final class AdhanPresentationFrameworkTest {
    private Context init() {
        Context c = RuntimeEnvironment.getApplication();
        AppString.init(c);
        AppSettings.setAdhanSound(c, AdhanScheduler.FAJR, false);
        AppSettings.setAdhanVibrate(c, AdhanScheduler.FAJR, false);
        AppSettings.setAdhanFullscreenUnlocked(c, AdhanScheduler.FAJR, false);
        return c;
    }

    @Test public void freshSettingsDefaultToBackgroundAndRespectExplicitChoice() {
        Context c = init();
        c.getSharedPreferences("advance_clock_settings", 0).edit().remove("adhan_qibla_display_mode").commit();
        assertEquals(AppSettings.ADHAN_QIBLA_BACKGROUND, AppSettings.adhanQiblaDisplayMode(c));
        AppSettings.setAdhanQiblaDisplayMode(c, AppSettings.ADHAN_QIBLA_NONE);
        assertEquals(AppSettings.ADHAN_QIBLA_NONE, AppSettings.adhanQiblaDisplayMode(c));
    }

    @Test public void directionLabelsAlignWithTheirOwnRaysExceptUnchangedNorthAndSouth() {
        assertEquals(0f, QiblaAdhanBackgroundView.directionLabelRotation(0), 0f);
        assertEquals(0f, QiblaAdhanBackgroundView.directionLabelRotation(4), 0f);
        for (int index : new int[]{1, 2, 3, 5, 6, 7}) {
            android.graphics.Matrix matrix = new android.graphics.Matrix();
            matrix.setRotate(QiblaAdhanBackgroundView.directionLabelRotation(index));
            float[] textUp = {0f, -1f};
            matrix.mapVectors(textUp);
            double bearing = Math.atan2(textUp[0], -textUp[1]);
            double expected = Math.toRadians(index * 45);
            assertEquals(Math.sin(expected), Math.sin(bearing), .00001);
            assertEquals(Math.cos(expected), Math.cos(bearing), .00001);
        }
    }

    @Test public void controlsSitBelowReservedCompassSpaceInEveryMode() {
        Context c = init();
        int backgroundHeight = 0, shortcutHeight = 0;
        for (int mode = 0; mode <= 2; mode++) {
            AppSettings.setAdhanQiblaDisplayMode(c, mode);
            org.robolectric.android.controller.ActivityController<AdhanRingActivity> controller =
                    Robolectric.buildActivity(AdhanRingActivity.class).setup();
            AdhanRingActivity activity = controller.get();
            View root = activity.findViewById(R.id.adhan_ring_root);
            root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, 1080, 1920);
            View title = activity.findViewById(R.id.adhan_ring_title);
            View time = activity.findViewById(R.id.adhan_ring_time);
            View slot = activity.findViewById(R.id.adhan_qibla_slot);
            View sound = activity.findViewById(R.id.adhan_alert_actions);
            View stop = activity.findViewById(R.id.adhan_stop);
            assertTrue(title.getBottom() <= time.getTop());
            assertTrue(time.getBottom() <= slot.getTop());
            assertTrue(slot.getHeight() > 0);
            View qibla = activity.findViewById(R.id.adhan_qibla_button);
            assertSame(root, qibla.getParent());
            if (mode == AppSettings.ADHAN_QIBLA_BACKGROUND) {
                assertEquals(View.GONE, qibla.getVisibility());
                assertEquals(slot.getBottom(), sound.getTop());
                backgroundHeight = slot.getHeight();
            } else {
                shortcutHeight = slot.getHeight();
                assertEquals(View.VISIBLE, qibla.getVisibility());
            assertEquals(qibla.getWidth(), qibla.getHeight());
            assertEquals(root.getWidth() / 2f, (qibla.getLeft() + qibla.getRight()) / 2f, 1f);
            assertTrue(slot.getBottom() <= qibla.getTop());
            assertTrue(qibla.getBottom() <= sound.getTop());
            assertNotNull(((android.widget.ImageView) activity.findViewById(R.id.adhan_qibla_button_image)).getDrawable());
            qibla.performClick();
            Intent launched = Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(CompassToolActivity.class.getName(), launched.getComponent().getClassName());
            assertEquals(CompassToolActivity.MODE_QIBLA, launched.getIntExtra(CompassToolActivity.EXTRA_START_MODE, -1));
            }
            assertTrue(sound.getBottom() <= stop.getTop());
            assertTrue(stop.getBottom() <= root.getHeight());
            controller.pause().stop().destroy();
        }
        assertTrue(backgroundHeight > shortcutHeight);
    }

    @Test public void notificationDisappearsOnScreenAndReturnsWithoutFullscreenIntent() {
        Context c = init();
        org.robolectric.android.controller.ServiceController<AdhanSoundService> serviceController =
                Robolectric.buildService(AdhanSoundService.class).create();
        AdhanSoundService service = serviceController.get();
        service.onStartCommand(AdhanSoundService.startIntent(c, AdhanScheduler.FAJR), 0, 1);
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        int id = 3_300_000 + AdhanScheduler.FAJR;
        assertNotNull(Shadows.shadowOf(manager).getNotification(id));

        org.robolectric.android.controller.ActivityController<AdhanRingActivity> screen =
                Robolectric.buildActivity(AdhanRingActivity.class,
                        new Intent(c, AdhanRingActivity.class).putExtra("adhanType", AdhanScheduler.FAJR)).setup();
        assertNull(Shadows.shadowOf(manager).getNotification(id));
        screen.pause().stop();
        Notification restored = Shadows.shadowOf(manager).getNotification(id);
        assertNotNull(restored);
        assertNull(restored.fullScreenIntent);
        assertNotNull(restored.contentView);
        assertNotNull(restored.bigContentView);

        View expanded = restored.bigContentView.apply(c, new android.widget.FrameLayout(c));
        View stop = expanded.findViewById(R.id.adhan_notification_stop);
        assertEquals(Math.round(132 * c.getResources().getDisplayMetrics().density), stop.getLayoutParams().width);
        assertTrue(stop.hasOnClickListeners());
        screen.start().resume();
        assertNull(Shadows.shadowOf(manager).getNotification(id));
        screen.pause().stop().destroy();
        serviceController.destroy();
    }

    @Test public void alreadyVisibleScreenSuppressesNewPlaybackNotification() {
        Context c = init();
        org.robolectric.android.controller.ActivityController<AdhanRingActivity> screen =
                Robolectric.buildActivity(AdhanRingActivity.class).setup();
        org.robolectric.android.controller.ServiceController<AdhanSoundService> controller =
                Robolectric.buildService(AdhanSoundService.class).create();
        controller.get().onStartCommand(AdhanSoundService.startIntent(c, AdhanScheduler.FAJR), 0, 1);
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        assertNull(Shadows.shadowOf(manager).getNotification(3_300_000 + AdhanScheduler.FAJR));
        controller.destroy();
        screen.pause().stop().destroy();
    }
}
