package com.ilia.advanceclock;

import android.Manifest;
import android.app.*;
import android.content.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class DateNotificationDismissalTest {
    @Test public void dismissalRestoresEnabledCardButNeverOverridesDisabledSetting() throws Exception {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AppSettings.setPersistentDateNotificationEnabled(app,true);DateNotificationService.start(app);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        Notification n=Shadows.shadowOf(manager).getNotification(73);assertNotNull(n.deleteIntent);
        Intent dismissed=Shadows.shadowOf(n.deleteIntent).getSavedIntent();
        assertEquals(DateNotificationDismissedReceiver.class.getName(),dismissed.getComponent().getClassName());
        assertEquals(DateNotificationDismissedReceiver.ACTION,dismissed.getAction());
        assertTrue((n.flags & Notification.FLAG_NO_CLEAR) != 0);
        manager.cancel(73);new DateNotificationDismissedReceiver().onReceive(app,dismissed);
        assertNotNull(Shadows.shadowOf(manager).getNotification(73));
        AppSettings.setPersistentDateNotificationEnabled(app,false);manager.cancel(73);
        new DateNotificationDismissedReceiver().onReceive(app,dismissed);
        assertNull(Shadows.shadowOf(manager).getNotification(73));
    }
    @Test public void blockedChannelStaysBlockedAndExistingChannelGetsNewName() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(DateNotificationService.CHANNEL,"Old name",NotificationManager.IMPORTANCE_NONE));
        DateNotificationService.ensureChannel(app);
        assertEquals(app.getString(R.string.persistent_date_channel_name),manager.getNotificationChannel(DateNotificationService.CHANNEL).getName().toString());
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        new DateNotificationDismissedReceiver().onReceive(app,new Intent(DateNotificationDismissedReceiver.ACTION));
        assertNull(Shadows.shadowOf(manager).getNotification(73));
        assertEquals(NotificationManager.IMPORTANCE_NONE,manager.getNotificationChannel(DateNotificationService.CHANNEL).getImportance());
    }
    @Test public void unrelatedBroadcastCannotRestoreCardAndReceiverIsPrivate() throws Exception {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        new DateNotificationDismissedReceiver().onReceive(app,new Intent("unrelated"));
        assertNull(Shadows.shadowOf(app.getSystemService(NotificationManager.class)).getNotification(73));
        assertFalse(app.getPackageManager().getReceiverInfo(new ComponentName(app,DateNotificationDismissedReceiver.class),0).exported);
    }
}
