package com.ilia.advanceclock;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},
        shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class SystemTimeChangedReceiverTest {
    @Test public void clockBroadcastRefreshesNotificationWithoutAnyActivity() {
        Application app=RuntimeEnvironment.getApplication(); AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        DateNotificationService.start(app);
        String expected=Shadows.shadowOf(manager).getNotification(73).extras
                .getCharSequence(Notification.EXTRA_TITLE).toString();
        for(String action:new String[]{Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,Intent.ACTION_DATE_CHANGED}) {
            Notification stale=Shadows.shadowOf(manager).getNotification(73);
            stale.extras.putCharSequence(Notification.EXTRA_TITLE,"Yesterday");
            manager.notify(73,stale);
            Intent event=new Intent(action).setComponent(
                    new android.content.ComponentName(app,SystemTimeChangedReceiver.class));
            assertFalse(app.getPackageManager().queryBroadcastReceivers(event,0).isEmpty());
            new SystemTimeChangedReceiver().onReceive(app,event);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertEquals(expected,Shadows.shadowOf(manager).getNotification(73).extras
                    .getCharSequence(Notification.EXTRA_TITLE).toString());
        }
    }

    @Test public void arbitraryExplicitBroadcastCannotActivateDateNotification() {
        Application app=RuntimeEnvironment.getApplication(); AppString.init(app);
        AppSettings.setPersistentDateNotificationEnabled(app,true);
        NotificationManager manager=app.getSystemService(NotificationManager.class);
        manager.cancel(73);
        new SystemTimeChangedReceiver().onReceive(app,new Intent("untrusted.action"));
        assertNull(Shadows.shadowOf(manager).getNotification(73));
    }
}
