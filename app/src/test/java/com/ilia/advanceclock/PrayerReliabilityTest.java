package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import java.net.URL;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk={28,35})
public class PrayerReliabilityTest {
    @Test public void prayerUsesWakeupAlarmClockAndCancellationRemovesIt() {
        Context context=RuntimeEnvironment.getApplication();
        AlarmManager manager=context.getSystemService(AlarmManager.class);
        Shadows.shadowOf(manager).setCanScheduleExactAlarms(true);
        AppSettings.setAdhanEnabled(context,true);
        AppSettings.setFajrAdhanEnabled(context,true);
        AppSettings.setPrayerLocation(context,35.69,51.39,"Tehran","Asia/Tehran");
        assertTrue(AdhanScheduler.scheduleNext(context,AdhanScheduler.FAJR));
        AlarmManager.AlarmClockInfo next=manager.getNextAlarmClock();
        assertNotNull(next);
        assertTrue(next.getTriggerTime()>System.currentTimeMillis());
        assertNotNull(next.getShowIntent());
        AppSettings.setAdhanEnabled(context,false);
        AdhanScheduler.rescheduleAll(context);
        assertNull(manager.getNextAlarmClock());
    }
    @Test public void reverseLookupUsesCityThenProvinceWithoutInventingName() throws Exception {
        assertEquals("تهران",PrayerLocationName.label(new JSONObject("{\"address\":{\"city\":\"تهران\",\"state\":\"استان تهران\"}}")));
        assertEquals("استان تهران",PrayerLocationName.label(new JSONObject("{\"address\":{\"state\":\"استان تهران\"}}")));
        assertNull(PrayerLocationName.label(new JSONObject("{}")));
    }
    @Test public void reverseLookupAllowsOnlyCoordinatesAndFixedOptions() throws Exception {
        String url="https://nominatim.openstreetmap.org/reverse?lat=35.69&lon=51.39&format=jsonv2&addressdetails=1&zoom=10&accept-language=fa";
        ReadOnlyNetwork.locationUrl(new URL(url));
        for(String invalid:new String[]{url+"&note=secret",url.replace("lat=35.69","lat=100"),url.replace("openstreetmap.org","evil.example")}) {
            try { ReadOnlyNetwork.locationUrl(new URL(invalid)); fail(invalid); }
            catch(java.io.IOException expected) {}
        }
    }
}
