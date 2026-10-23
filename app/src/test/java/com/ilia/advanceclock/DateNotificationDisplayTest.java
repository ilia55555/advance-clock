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
    @Test public void openingSettingsDoesNotRepostDateButSwitchStillControlsIt() {
        Application app=RuntimeEnvironment.getApplication(); AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        DateNotificationService.start(app);
        manager.cancel(73); // User dismissed it; reading settings must not restore it.
        try (var controller=Robolectric.buildActivity(SettingsActivity.class).setup()) {
            SettingsActivity activity=controller.get();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertNull(Shadows.shadowOf(manager).getNotification(73));
            assertTrue(AppSettings.persistentDateNotificationEnabled(app));
            controller.pause().resume();
            assertNull(Shadows.shadowOf(manager).getNotification(73));
            Object panel=org.robolectric.util.ReflectionHelpers.getField(activity,"notificationPanel");
            android.widget.Switch toggle=org.robolectric.util.ReflectionHelpers.getField(panel,"persistentDate");
            toggle.setChecked(false);
            assertFalse(AppSettings.persistentDateNotificationEnabled(app));
            toggle.setChecked(true);
            assertNotNull(Shadows.shadowOf(manager).getNotification(73));
            toggle.setChecked(false);
            assertNull(Shadows.shadowOf(manager).getNotification(73));
            controller.recreate();
            assertFalse(AppSettings.persistentDateNotificationEnabled(app));
            assertNull(Shadows.shadowOf(manager).getNotification(73));
        }
    }

    @Test public void returningFromNotificationPermissionChangeReconcilesDate() {
        Application app=RuntimeEnvironment.getApplication(); AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        try (var controller=Robolectric.buildActivity(SettingsActivity.class).setup()) {
            controller.pause();
            Shadows.shadowOf(manager).setNotificationsEnabled(false);
            controller.resume();
            assertNull(Shadows.shadowOf(manager).getNotification(73));
            controller.pause();
            Shadows.shadowOf(manager).setNotificationsEnabled(true);
            controller.resume();
            assertNotNull(Shadows.shadowOf(manager).getNotification(73));
        }
    }

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
        assertNotNull(collapsed.findViewById(R.id.date_notification_title));
        assertNotNull(expanded.findViewById(R.id.date_notification_day));
        assertNull(collapsed.findViewById(R.id.date_notification_settings));
        assertNotNull(expanded.findViewById(R.id.date_notification_settings));
        assertNull(collapsed.findViewById(R.id.date_notification_calendar));
        assertNotNull(expanded.findViewById(R.id.date_notification_calendar));
        assertEquals(AppSettings.primaryColor(app),
                ((android.widget.TextView) collapsed.findViewById(R.id.date_notification_day)).getCurrentTextColor());
        AppSettings.setPersistentDateNotificationEnabled(app,false);
        DateNotificationService.start(app);
        assertNull(Shadows.shadowOf(manager).getNotification(73));
    }
    @Test public void dayDigitsFollowOnlyLanguageAndAllIconsLoad() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        for (String language : new String[]{"fa", "ar", "en"}) {
            AppSettings.setLanguage(app, language);
            for (int calendar : new int[]{CalendarUtils.PERSIAN, CalendarUtils.GREGORIAN, CalendarUtils.HIJRI}) {
                AppSettings.setDefaultCalendar(app,calendar);
                boolean english=language.equals("en");
                assertEquals(english ? "21" : "۲۱",
                        DateNotificationService.dayLabel(app,21));
                assertEquals(english ? "2026/10/24" : "۲۰۲۶/۱۰/۲۴",
                        DateNotificationService.notificationDigits(app,"۲۰۲۶/10/٢٤"));
                for (int day=1;day<=31;day++) {
                    assertNotNull(app.getDrawable(DateNotificationService.dayIconResource(app,day)));
                }
            }
        }
    }

    @Test public void blockedChannelButtonOpensTheDateChannelSettings() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        app.getSystemService(NotificationManager.class).createNotificationChannel(
                new NotificationChannel(DateNotificationService.CHANNEL,"Date",
                        NotificationManager.IMPORTANCE_NONE));
        try (org.robolectric.android.controller.ActivityController<SettingsActivity> controller =
                     Robolectric.buildActivity(SettingsActivity.class).setup()) {
            SettingsActivity activity=controller.get();
            Object panel=org.robolectric.util.ReflectionHelpers.getField(activity,"notificationPanel");
            android.widget.Button button=org.robolectric.util.ReflectionHelpers.getField(panel,"dateChannelButton");
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
