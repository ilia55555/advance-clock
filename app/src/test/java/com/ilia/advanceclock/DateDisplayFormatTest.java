package com.ilia.advanceclock;
import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class DateDisplayFormatTest {
 @Test public void formatChoicePersistsAcrossInitializationAndPreservesCalendarAndTimeZone() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);
  AppSettings.setLanguage(app,"fa");
  CalendarUtils.setHijriReference(CalendarUtils.HIJRI_UMALQURA);
  long time=java.time.Instant.parse("2026-09-23T00:30:00Z").toEpochMilli();
  assertEquals(0,AppSettings.dateDisplayFormat(app));
  String written=CalendarUtils.formatDate(time,CalendarUtils.PERSIAN);
  assertFalse(written.contains("/"));
  AppSettings.setDateDisplayFormat(app,1);AppString.init(app);
  assertEquals(1,AppSettings.dateDisplayFormat(app));
  for(int calendar:new int[]{CalendarUtils.PERSIAN,CalendarUtils.GREGORIAN,CalendarUtils.HIJRI}) {
   for(String zone:new String[]{"UTC","America/Los_Angeles"}) {
    java.util.TimeZone tz=java.util.TimeZone.getTimeZone(zone);
    assertEquals(CalendarUtils.formatNumeric(time,calendar,tz),CalendarUtils.formatDate(time,calendar,tz));
   }
  }
  AppSettings.setDateDisplayFormat(app,0);
  assertEquals(written,CalendarUtils.formatDate(time,CalendarUtils.PERSIAN));
 }
}
