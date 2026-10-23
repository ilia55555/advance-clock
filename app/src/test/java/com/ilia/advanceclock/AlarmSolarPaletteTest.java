package com.ilia.advanceclock;

import android.app.Application;
import android.graphics.Color;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlarmSolarPaletteTest {
    private long at(TimeZone zone,int minute){
        Calendar day=Calendar.getInstance(zone);day.clear();day.set(2026,Calendar.OCTOBER,23,0,0);
        day.add(Calendar.MINUTE,minute);return day.getTimeInMillis();
    }
    @Test public void noLocationUsesLocalDayAndNightRegardlessOfAppTheme(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        app.getSharedPreferences("advance_clock_settings",0).edit().putBoolean("prayer_location_set",false).apply();
        TimeZone zone=TimeZone.getDefault();
        assertTrue(AlarmAnalogView.paletteAt(app,at(zone,359)).night);
        assertFalse(AlarmAnalogView.paletteAt(app,at(zone,360)).night);
        assertFalse(AlarmAnalogView.paletteAt(app,at(zone,1079)).night);
        AlarmAnalogView.Palette night=AlarmAnalogView.paletteAt(app,at(zone,1080));
        assertTrue(night.night);assertEquals(Color.BLACK,night.face);assertEquals(Color.WHITE,night.ink);
        AlarmAnalogView.Palette day=AlarmAnalogView.paletteAt(app,at(zone,720));
        assertEquals(Color.WHITE,day.face);assertEquals(Color.BLACK,day.ink);
    }
    @Test public void usesPrimaryPrayerLocationTimezoneAndActualSunriseSunset(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        app.getSharedPreferences("advance_clock_settings",0).edit().putBoolean("prayer_location_set",true)
                .putString("prayer_latitude","35.6892").putString("prayer_longitude","51.3890")
                .putString("prayer_time_zone","Asia/Tehran").apply();
        TimeZone zone=TimeZone.getTimeZone("Asia/Tehran");
        PrayerTimeCalculator.Times times=PrayerTimeCalculator.calculate(at(zone,720),35.6892,51.3890,zone);
        assertTrue(AlarmAnalogView.paletteAt(app,at(zone,times.sunriseMinutes-1)).night);
        AlarmAnalogView.Palette sunrise=AlarmAnalogView.paletteAt(app,at(zone,times.sunriseMinutes));
        assertFalse(sunrise.night);
        assertFalse(AlarmAnalogView.paletteAt(app,at(zone,times.sunsetMinutes-1)).night);
        AlarmAnalogView.Palette sunset=AlarmAnalogView.paletteAt(app,at(zone,times.sunsetMinutes));
        assertTrue(sunset.night);assertEquals(Color.BLACK,sunset.face);assertEquals(Color.WHITE,sunset.ink);
        AlarmAnalogView.Palette noon=AlarmAnalogView.paletteAt(app,at(zone,720));
        assertNotEquals(noon.accent,sunrise.accent);assertNotEquals(noon.face,sunrise.face);
    }
}
