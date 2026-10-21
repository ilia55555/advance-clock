package com.ilia.advanceclock;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.view.View;
import android.graphics.drawable.Icon;
import android.widget.FrameLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class DateNotificationDisplayTest {
    @Test public void enabledNotificationCanActuallyInflateInSystemUi() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        DateNotificationService.start(app);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        Notification notification=Shadows.shadowOf(manager).getNotification(73);
        assertNotNull(notification);
        assertNotNull(notification.getSmallIcon());
        assertEquals(Icon.TYPE_RESOURCE, notification.getSmallIcon().getType());
        assertNotNull(app.getDrawable(notification.getSmallIcon().getResId()));
        assertTrue((notification.flags & Notification.FLAG_ONGOING_EVENT) != 0);
        assertFalse(notification.extras.getCharSequence(Notification.EXTRA_TITLE).toString().isEmpty());
        Notification.Builder recovered=Notification.Builder.recoverBuilder(app,notification);
        View collapsed=recovered.createContentView().apply(app,new FrameLayout(app));
        View expanded=recovered.createBigContentView().apply(app,new FrameLayout(app));
        assertNotNull(collapsed);
        assertNotNull(expanded);
        AppSettings.setPersistentDateNotificationEnabled(app,false);
        DateNotificationService.start(app);
        assertNull(Shadows.shadowOf(manager).getNotification(73));
    }
    @Test public void blockedChannelButtonOpensTheDateChannelSettings() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        app.getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(DateNotificationService.CHANNEL,"Date",
                        NotificationManager.IMPORTANCE_NONE));
        try (org.robolectric.android.controller.ActivityController<NotificationSettingsActivity> controller =
                     Robolectric.buildActivity(NotificationSettingsActivity.class).setup()) {
            NotificationSettingsActivity activity=controller.get();
            android.widget.Button button=org.robolectric.util.ReflectionHelpers.getField(activity,"dateChannelButton");
            assertEquals(View.VISIBLE,button.getVisibility());
            button.performClick();
            android.content.Intent intent=Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS,intent.getAction());
            assertEquals(DateNotificationService.CHANNEL,
                    intent.getStringExtra(android.provider.Settings.EXTRA_CHANNEL_ID));
        }
    }

    @Test public void blockedDateChannelIsDetectedAndRespected() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(DateNotificationService.CHANNEL,
                "Date",NotificationManager.IMPORTANCE_NONE));
        DateNotificationService.start(app);
        assertTrue(DateNotificationService.isChannelBlocked(app));
        assertNull(Shadows.shadowOf(manager).getNotification(73));
        assertEquals(NotificationManager.IMPORTANCE_NONE,
                manager.getNotificationChannel(DateNotificationService.CHANNEL).getImportance());
    }
}
