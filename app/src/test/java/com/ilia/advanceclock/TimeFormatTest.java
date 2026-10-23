package com.ilia.advanceclock;
import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class TimeFormatTest {
 @Test public void defaultsTo24HoursAndLocalizes12HourPeriodMarkers() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);AppSettings.setLanguage(app,"en");
  java.util.TimeZone zone=java.util.TimeZone.getTimeZone("UTC");
  java.util.Date evening=java.util.Date.from(java.time.Instant.parse("2026-10-23T23:04:00Z"));
  assertTrue(AppSettings.clock24Hour(app));
  assertEquals("23:04",TimeFormat.formatter(app,false,zone).format(evening));
  AppSettings.setClock24Hour(app,false);
  assertEquals("11:04 PM",TimeFormat.formatter(app,false,zone).format(evening));
  java.util.Date midnight=java.util.Date.from(java.time.Instant.parse("2026-10-23T00:00:00Z"));
  assertEquals("12:00 AM",TimeFormat.formatter(app,false,zone).format(midnight));
  AppSettings.setLanguage(app,"fa");
  assertTrue(TimeFormat.formatter(app,false,zone).format(evening).contains("ب.ظ"));
  assertTrue(TimeFormat.formatter(app,false,zone).format(midnight).contains("ق.ظ"));
  AppSettings.setLanguage(app,"ar");
  assertTrue(TimeFormat.formatter(app,false,zone).format(evening).endsWith("م"));
  AppSettings.setClock24Hour(app,true);
  assertFalse(TimeFormat.formatter(app,false,zone).format(evening).endsWith("م"));
 }
}
