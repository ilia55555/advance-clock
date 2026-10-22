package com.ilia.advanceclock;
import android.app.Activity;
import android.media.AudioManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=AdvanceClockApplication.class,sdk=35,shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlarmVolumeRoutingTest {
 @Test public void applicationRoutesHardwareVolumeToAlarmStream() {
  try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
   assertEquals(AudioManager.STREAM_ALARM,controller.get().getVolumeControlStream());
  }
 }
}
