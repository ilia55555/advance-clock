package com.ilia.advanceclock;
import android.app.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.time.Duration;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=35)
public class TimerEndTimeTest {
 @Test public void durationCrossesMidnightAndRunningDeadlineDoesNotMove() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);
  java.util.TimeZone previous=java.util.TimeZone.getDefault();
  java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
  AppSettings.setLanguage(app,"en");AppSettings.setDefaultCalendar(app,CalendarUtils.GREGORIAN);
  long now=System.currentTimeMillis();
  long target=java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneOffset.UTC)
          .toLocalDate().plusDays(1).atTime(1,0).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
  long duration=(target-now+999)/1000;
  String expected="01:00   "+CalendarUtils.formatNumeric(target,CalendarUtils.GREGORIAN);
  try(var controller=Robolectric.buildActivity(TimerActivity.class).setup()) {
   TimerActivity host=controller.get();
   ((EditText)host.findViewById(R.id.timer_hours)).setText(Long.toString(duration/3600));
   ((EditText)host.findViewById(R.id.timer_minutes)).setText(Long.toString(duration/60%60));
   ((EditText)host.findViewById(R.id.timer_seconds)).setText(Long.toString(duration%60));
   TextView summary=host.findViewById(R.id.timer_target_summary);
   assertEquals(expected,summary.getText().toString());
   TimerPanelController timer=ReflectionHelpers.getField(host,"controller");
   ReflectionHelpers.setField(timer,"deadline",target);
   ReflectionHelpers.setField(timer,"running",true);
   org.robolectric.shadows.ShadowSystemClock.advanceBy(Duration.ofMinutes(5));
   ReflectionHelpers.callInstanceMethod(timer,"renderEndTime");
   assertEquals(expected,summary.getText().toString());
  } finally {java.util.TimeZone.setDefault(previous);}
 }
}
