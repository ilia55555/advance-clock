package com.ilia.advanceclock;

import android.Manifest;
import android.app.Application;
import android.app.NotificationManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk={28,35},
        shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class DateNotificationStartupTest {
    @Test public void existingCardWithPreviousDateRefreshesWithoutTogglingSetting() {
        Application app=RuntimeEnvironment.getApplication(); AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        DateNotificationService.start(app);
        android.app.Notification stale=Shadows.shadowOf(manager).getNotification(73);
        String expectedTitle=stale.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString();
        stale.extras.putCharSequence(android.app.Notification.EXTRA_TITLE,"Yesterday");
        manager.notify(73,stale);
        DateNotificationService.ensureVisible(app);
        String newTitle=Shadows.shadowOf(manager).getNotification(73).extras
                .getCharSequence(android.app.Notification.EXTRA_TITLE).toString();
        assertEquals(expectedTitle,newTitle);
        assertTrue(AppSettings.persistentDateNotificationEnabled(app));
    }

    @Test public void returningUserGetsEnabledDateWithoutOpeningSettings() {
        Application app=RuntimeEnvironment.getApplication(); AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        app.getSharedPreferences("advance_clock_app",0).edit()
                .putBoolean("language_calendar_setup_v1",true)
                .putBoolean("permission_onboarding_v2",true).commit();
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        manager.cancel(73);
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            assertNotNull(Shadows.shadowOf(manager).getNotification(73));
            controller.pause();
            manager.cancel(73);
            controller.resume();
            assertNotNull(Shadows.shadowOf(manager).getNotification(73));
            controller.pause();
            AppSettings.setPersistentDateNotificationEnabled(app,false);
            controller.resume();
            assertNull(Shadows.shadowOf(manager).getNotification(73));
            assertFalse(AppSettings.persistentDateNotificationEnabled(app));
        }
    }
}
