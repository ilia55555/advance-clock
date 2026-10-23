package com.ilia.advanceclock;

import android.app.Application;
import android.widget.Spinner;
import java.util.*;
import java.time.Instant;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AppReferenceTimeTest {
    private TimeZone original;
    @Before public void init(){original=TimeZone.getDefault();AppTime.configure("");AppString.init(RuntimeEnvironment.getApplication());}
    @After public void reset(){AppTime.configure("");TimeZone.setDefault(original);}
    @Test public void defaultFollowsPhoneChangesAndCustomChoiceNeverChangesPhoneZone(){
        Application app=RuntimeEnvironment.getApplication();assertEquals("",AppSettings.referenceTimeZoneId(app));
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));assertEquals("UTC",AppTime.zone().getID());
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"));assertEquals("Asia/Tehran",AppTime.zone().getID());
        AppSettings.setReferenceTimeZoneId(app,"America/New_York");assertEquals("America/New_York",AppTime.zone().getID());
        assertEquals("Asia/Tehran",TimeZone.getDefault().getID());
        AppSettings.setReferenceTimeZoneId(app,"");assertEquals("Asia/Tehran",AppTime.zone().getID());
    }
    @Test public void clockCalendarAndImplicitAlarmUseReferenceButCityAlarmKeepsItsOwnZone(){
        Application app=RuntimeEnvironment.getApplication();AppSettings.setLanguage(app,"en");AppString.init(app);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));AppSettings.setReferenceTimeZoneId(app,"Pacific/Kiritimati");
        long when=Instant.parse("2026-01-01T12:00:00Z").toEpochMilli();
        assertEquals("02:00",TimeFormat.clock(app,when));
        android.icu.util.Calendar date=CalendarUtils.create(CalendarUtils.GREGORIAN);date.setTimeInMillis(when);
        assertEquals(2,date.get(android.icu.util.Calendar.DAY_OF_MONTH));
        AlarmItem alarm=new AlarmItem(1,"Test",when,0,true);assertEquals("Pacific/Kiritimati",alarm.timeZone().getID());
        alarm.referenceZoneId="America/New_York";assertEquals("America/New_York",alarm.timeZone().getID());
        AppSettings.setReferenceTimeZoneId(app,"");assertEquals("12:00",TimeFormat.clock(app,when));
        assertEquals("America/New_York",alarm.timeZone().getID());
    }
    @Test public void generalSelectorDefaultsToPhoneAndRestoresSelectedSavedCity(){
        Application app=RuntimeEnvironment.getApplication();WorldClockStore.save(app,Arrays.asList("Asia/Tehran","America/New_York"));
        WorldClockStore.saveLabel(app,"Asia/Tehran","اصفهان");
        try(var controller=Robolectric.buildActivity(SettingsActivity.class).setup()){
            SettingsActivity host=controller.get();Spinner reference=host.findViewById(android.R.id.content).findViewWithTag("app_reference_time_zone");
            assertEquals(0,reference.getSelectedItemPosition());assertEquals(3,reference.getCount());
            assertTrue(reference.getItemAtPosition(1).toString().contains("اصفهان"));
            reference.setSelection(1);reference.getOnItemSelectedListener().onItemSelected(reference,null,1,1);
            assertEquals("Asia/Tehran",AppSettings.referenceTimeZoneId(app));assertEquals("Asia/Tehran",AppTime.zone().getID());
            controller.recreate();reference=controller.get().findViewById(android.R.id.content).findViewWithTag("app_reference_time_zone");
            assertEquals(1,reference.getSelectedItemPosition());
            reference.setSelection(0);reference.getOnItemSelectedListener().onItemSelected(reference,null,0,0);
            assertEquals("",AppSettings.referenceTimeZoneId(app));assertEquals(TimeZone.getDefault().getID(),AppTime.zone().getID());
        }
    }
}
