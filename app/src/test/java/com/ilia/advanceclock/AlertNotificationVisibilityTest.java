package com.ilia.advanceclock;
import android.app.*;
import android.content.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlertNotificationVisibilityTest {
 @Test public void alarmTimerAndStopwatchNotificationsFollowTheirOwnScreen() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);NotificationHelper.ensureChannels(app);
  for(String kind:new String[]{"",ToolAlarmScheduler.TIMER,ToolAlarmScheduler.STOPWATCH}) {
   long id=kind.isEmpty()?42:ToolAlarmScheduler.alarmId(kind);
   var serviceController=Robolectric.buildService(AlarmSoundService.class).create();
   try {
    AlarmSoundService service=serviceController.get();
    ReflectionHelpers.setField(service,"currentAlarmId",id);ReflectionHelpers.setField(service,"currentLabel","Test");
    ReflectionHelpers.setField(service,"currentToolKind",kind);ReflectionHelpers.setField(service,"playing",true);
    AlarmSoundService.onRingVisibilityChanged();assertTrue(ReflectionHelpers.<Boolean>getField(service,"foreground"));
    NotificationManager manager=app.getSystemService(NotificationManager.class);
    assertNotNull(Shadows.shadowOf(manager).getNotification(NotificationHelper.notificationId(id)));
    Intent intent=new Intent(app,AlarmRingActivity.class).putExtra("alarmId",id).putExtra("toolKind",kind);
    try(var activity=Robolectric.buildActivity(AlarmRingActivity.class,intent).setup().visible()) {
     assertFalse(ReflectionHelpers.<Boolean>getField(service,"foreground"));
     assertNull(Shadows.shadowOf(manager).getNotification(NotificationHelper.notificationId(id)));
     assertTrue(AlarmRingActivity.isVisible(id));assertFalse(AlarmRingActivity.isVisible(id+1));
     // A paused screen may still be visible behind a dialog; do not restore until stopped.
     activity.pause();assertFalse(ReflectionHelpers.<Boolean>getField(service,"foreground"));
     activity.stop();assertTrue(ReflectionHelpers.<Boolean>getField(service,"foreground"));
     Notification restored=Shadows.shadowOf(service).getLastForegroundNotification();
     assertNotNull(restored);assertNull(restored.fullScreenIntent);
     activity.restart().start().resume();assertFalse(ReflectionHelpers.<Boolean>getField(service,"foreground"));
     ReflectionHelpers.callInstanceMethod(service,"stopAlarm");
    }
    assertFalse(ReflectionHelpers.<Boolean>getField(service,"foreground"));
   } finally {serviceController.destroy();}
  }
 }
 @Test public void noteNotificationReturnsOnlyWhenItsScreenIsHidden() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);NotificationHelper.ensureChannels(app);
  NoForgetItem item=new NoForgetItem(51,"Note","Body","[]",2,true,System.currentTimeMillis()+1000,true,System.currentTimeMillis());
  new NoForgetStore(app).save(item);
  var services=Robolectric.buildService(NoForgetSoundService.class).create();
  try {
   NoForgetSoundService service=services.get();ReflectionHelpers.setField(service,"active",true);ReflectionHelpers.setField(service,"currentItem",item);
   NoForgetSoundService.onRingVisibilityChanged();assertTrue(ReflectionHelpers.<Boolean>getField(service,"foreground"));
   NotificationManager manager=app.getSystemService(NotificationManager.class);
   assertNotNull(Shadows.shadowOf(manager).getNotification(NotificationHelper.reminderNotificationId(item.id)));
   try(var activity=Robolectric.buildActivity(NoteReminderActivity.class,new Intent(app,NoteReminderActivity.class).putExtra("noteId",item.id)).setup().visible()) {
    assertFalse(ReflectionHelpers.<Boolean>getField(service,"foreground"));
    assertFalse(NoteReminderActivity.isVisible(item.id+1));
    assertNull(Shadows.shadowOf(manager).getNotification(NotificationHelper.reminderNotificationId(item.id)));
    activity.pause().stop();assertTrue(ReflectionHelpers.<Boolean>getField(service,"foreground"));
    assertNull(Shadows.shadowOf(service).getLastForegroundNotification().fullScreenIntent);
    ReflectionHelpers.callInstanceMethod(service,"stopReminder");
    activity.restart().start().resume();activity.pause().stop();
    assertFalse(ReflectionHelpers.<Boolean>getField(service,"foreground"));
   }
  } finally {services.destroy();}
 }
}
