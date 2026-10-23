package com.ilia.advanceclock;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.location.Location;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {28, 35},
        shadows = ReminderScreenFrameworkTest.PlainStore.class)
public final class RequestedBehaviorFrameworkTest {
    @Test public void firstHorizonWinsOverOldDeviceCacheAndSelectedPrimary() {
        Context c = RuntimeEnvironment.getApplication();
        AppString.init(c);
        QiblaUtils.remember(c, 48.8566, 2.3522);
        c.getSharedPreferences("advance_clock_settings", 0).edit()
                .putString("prayer_horizons", "[{\"label\":\"First City\",\"lat\":34.1,\"lon\":50.1},{\"label\":\"Second City\",\"lat\":36.2,\"lon\":52.2}]")
                .putBoolean("prayer_location_set", true)
                .putString("prayer_latitude", "36.2").putString("prayer_longitude", "52.2").commit();
        Location location = QiblaUtils.bestKnownLocation(c);
        assertNotNull(location);
        assertEquals(34.1, location.getLatitude(), .00001);
        assertEquals(50.1, location.getLongitude(), .00001);
        assertEquals("First City", location.getExtras().getString("qibla_city"));
        assertTrue(QiblaUtils.sourceLabel(location).contains("First City"));
    }

    @Test public void staleDeviceFixIsNotCurrent() {
        Location fix = new Location("gps");
        fix.setElapsedRealtimeNanos(1);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMinutes(3));
        assertFalse(QiblaUtils.isRecent(fix));
        long now = android.os.SystemClock.elapsedRealtimeNanos();
        fix.setElapsedRealtimeNanos(now);
        assertTrue(QiblaUtils.isRecent(fix));
    }

    @Test public void blockedNotificationsDisableSettingsAndExposePermissionButton() {
        Context c = RuntimeEnvironment.getApplication();
        AppString.init(c);
        Shadows.shadowOf((Application) c).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS);
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        Shadows.shadowOf(manager).setNotificationsEnabled(false);
        org.robolectric.android.controller.ActivityController<SettingsActivity> controller =
                Robolectric.buildActivity(SettingsActivity.class).setup();
        ViewGroup root = controller.get().findViewById(android.R.id.content).findViewWithTag("settings_content_notification");
        assertDisabledSwitches(root);
        assertTrue(hasVisiblePermission(root));
        Shadows.shadowOf(manager).setNotificationsEnabled(true);
        controller.pause().resume();
        assertFalse(hasVisiblePermission(root));
        controller.pause().stop().destroy();
    }

    private void assertDisabledSwitches(View v) {
        if (v instanceof Switch) assertFalse(v.isEnabled());
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) assertDisabledSwitches(g.getChildAt(i));
        }
    }

    private boolean hasVisiblePermission(View v) {
        if (v instanceof android.widget.Button && v.getVisibility() == View.VISIBLE
                && ((android.widget.Button) v).getText().toString().equals(AppString.get(R.string.grant_notifications))) return true;
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) v;
            for (int i = 0; i < g.getChildCount(); i++) if (hasVisiblePermission(g.getChildAt(i))) return true;
        }
        return false;
    }

    @Test public void alarmActionsKeepStateOnRightAndRequireTwoDeletionTaps() throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        AppString.init(c);
        c.getSharedPreferences("advance_clock_app", 0).edit()
                .putBoolean("language_calendar_setup_v1", true).commit();
        org.robolectric.android.controller.ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        Method card = MainActivity.class.getDeclaredMethod("alarmCard", AlarmItem.class);
        card.setAccessible(true);
        AlarmItem item = new AlarmItem(99, "Test", System.currentTimeMillis() + 60000, 0, true);
        new AlarmStore(c).save(item);
        LinearLayout content = (LinearLayout) card.invoke(controller.get(), item);
        LinearLayout actions = (LinearLayout) content.getChildAt(content.getChildCount() - 1);
        assertEquals(View.LAYOUT_DIRECTION_LTR, actions.getLayoutDirection());
        assertEquals(4, actions.getChildCount());
        assertEquals(1f, ((LinearLayout.LayoutParams) actions.getChildAt(2).getLayoutParams()).weight, 0f);
        assertTrue(actions.getChildAt(0) instanceof ImageButton);
        ImageButton delete = (ImageButton) actions.getChildAt(0);
        assertNotNull(delete.getDrawable());
        assertEquals(AppString.get(R.string.runtime_text_0006), delete.getContentDescription());
        content.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.AT_MOST));
        content.layout(0, 0, 1080, content.getMeasuredHeight());
        assertTrue(delete.getRight() <= actions.getChildAt(1).getLeft());
        assertTrue(actions.getChildAt(1).getRight() <= actions.getChildAt(3).getLeft());

        assertEquals(actions.getChildAt(3).getLayoutParams().height, delete.getLayoutParams().height);
        assertEquals(delete.getLayoutParams().height, delete.getLayoutParams().width);
        assertFalse(delete.isSelected());
        assertTrue(delete.getBackground() instanceof android.graphics.drawable.GradientDrawable);
        assertNotNull(delete.getColorFilter());
        delete.performClick();
        assertTrue(delete.isSelected());
        assertNotNull(new AlarmStore(c).find(item.id));
        delete.performClick();
        assertNull(new AlarmStore(c).find(item.id));
        controller.pause().stop().destroy();
    }
}
