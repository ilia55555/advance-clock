package com.ilia.advanceclock;

import android.Manifest;
import android.app.*;
import android.content.Context;
import android.view.View;
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
        View collapsed=notification.contentView.apply(app,new FrameLayout(app));
        View expanded=notification.bigContentView.apply(app,new FrameLayout(app));
        assertNotNull(collapsed.findViewById(R.id.date_notification_title));
        assertNotNull(expanded.findViewById(R.id.date_notification_title));
    }
}
