package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=35)
public class CalendarMarkerSettingsTest {
    @Test public void defaultsAndChangesAreIndependentAndOldAlarmChoiceSurvives() {
        Context c=RuntimeEnvironment.getApplication();
        c.getSharedPreferences("advance_clock_settings",0).edit().clear().commit();
        assertFalse(AppSettings.showCalendarDayMarkers(c,false));
        assertTrue(AppSettings.showCalendarDayMarkers(c,true));
        AppSettings.setShowCalendarDayMarkers(c,true,false);
        assertFalse(AppSettings.showCalendarDayMarkers(c,true));
        AppSettings.setShowCalendarDayMarkers(c,false,true);
        assertTrue(AppSettings.showCalendarDayMarkers(c,false));
        assertFalse(AppSettings.showCalendarDayMarkers(c,true));
        c.getSharedPreferences("advance_clock_settings",0).edit()
                .remove("show_note_calendar_day_markers").commit();
        assertTrue(AppSettings.showCalendarDayMarkers(c,false));
        assertTrue(AppSettings.showCalendarDayMarkers(c,true));
    }
}
