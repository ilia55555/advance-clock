package com.ilia.advanceclock;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = AdvanceClockApplication.class, sdk = {28, 35},
        shadows = {ReminderScreenFrameworkTest.PlainStore.class, AlarmPreparationFrameworkTest.FakePlayer.class})
public final class AlarmDeliveryFrameworkTest {
    @Test public void activeAppOpensAlertAndPostsNotificationWithoutWaitingForAudio() {
        Context c = RuntimeEnvironment.getApplication();
        org.robolectric.android.controller.ActivityController<Activity> foreground =
                Robolectric.buildActivity(Activity.class).setup();
        assertTrue(AdvanceClockApplication.hasResumedActivity());
        AppSettings.setAlarmFullscreenUnlocked(c, true);
        org.robolectric.android.controller.ServiceController<AlarmSoundService> controller =
                Robolectric.buildService(AlarmSoundService.class).create();
        Intent start = AlarmSoundService.startIntent(c, ToolAlarmScheduler.alarmId(ToolAlarmScheduler.TIMER), "Timer")
                .putExtra("toolKind", ToolAlarmScheduler.TIMER);
        controller.get().onStartCommand(start, 0, 1);
        assertNotNull(Shadows.shadowOf(c.getSystemService(NotificationManager.class)).getNotification(
                NotificationHelper.notificationId(ToolAlarmScheduler.alarmId(ToolAlarmScheduler.TIMER))));
        Intent screen = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertNotNull(screen);
        assertEquals(AlarmRingActivity.class.getName(), screen.getComponent().getClassName());
        assertEquals(ToolAlarmScheduler.TIMER, screen.getStringExtra("toolKind"));
        controller.destroy();
        foreground.pause().stop().destroy();
        assertFalse(AdvanceClockApplication.hasResumedActivity());
    }

    @Test public void backgroundAlertAttemptsOpeningImmediatelyAndRespectsFullscreenSetting(){
        Context c=RuntimeEnvironment.getApplication();assertFalse(AdvanceClockApplication.hasResumedActivity());
        var controller=Robolectric.buildService(AlarmSoundService.class).create();
        try{
            AppSettings.setAlarmFullscreenUnlocked(c,true);
            org.robolectric.util.ReflectionHelpers.callInstanceMethod(controller.get(),"openScreenIfAllowed",
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(long.class,42L),
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(String.class,"Wake"),
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(String.class,""));
            Intent screen=Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
            assertNotNull(screen);assertEquals(AlarmRingActivity.class.getName(),screen.getComponent().getClassName());
            AppSettings.setAlarmFullscreenUnlocked(c,false);
            org.robolectric.util.ReflectionHelpers.callInstanceMethod(controller.get(),"openScreenIfAllowed",
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(long.class,42L),
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(String.class,"Wake"),
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(String.class,""));
            assertNull(Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity());
        }finally{controller.destroy();}
    }

    @Test public void receiverPassesAlarmSnapshotToAvoidRepeatedEncryptedRead() throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        AlarmItem item = new AlarmItem(31L, "Wake", System.currentTimeMillis() + 60000, 0, true);
        item.imageUri1 = "content://photos/first";
        AlarmSoundService.ring(c, item);
        Intent service = Shadows.shadowOf(RuntimeEnvironment.getApplication()).getNextStartedService();
        assertNotNull(service);
        AlarmItem snapshot = AlarmItem.fromJson(new org.json.JSONObject(service.getStringExtra("alarmPayload")));
        assertEquals(item.id, snapshot.id);
        assertEquals(item.imageUri1, snapshot.imageUri1);
        assertEquals(item.label, snapshot.label);
    }
}
