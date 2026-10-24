package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class AlarmDefaultsAndVibrationTest {
    @Test public void glassIsDefaultButExistingStyleIsPreserved() {
        Context app=RuntimeEnvironment.getApplication(); AppString.init(app);
        assertEquals(4,AppSettings.alarmScreenStyle(app));
        AppSettings.setAlarmScreenStyle(app,1);
        assertEquals(1,AppSettings.alarmScreenStyle(app));
    }
    @Test public void adhanAndAlarmLibrariesAndImportedSoundsAreSeparate() {
        Context app=RuntimeEnvironment.getApplication(); AppString.init(app);
        assertTrue(SoundLibrary.all(app).stream().noneMatch(s->s.uri.contains("adhan")));
        assertTrue(SoundLibrary.all(app,true).stream().anyMatch(s->s.uri.contains("rawadhan_1")));
        SoundLibrary.add(app,Uri.parse("content://sounds/alarm"));
        SoundLibrary.add(app,Uri.parse("content://sounds/prayer"),true);
        assertTrue(SoundLibrary.all(app).stream().noneMatch(s->s.uri.endsWith("/prayer")));
        assertTrue(SoundLibrary.all(app,true).stream().noneMatch(s->s.uri.endsWith("/alarm")));
    }
    @Test public void vibrationUsesAlarmUsageAndCanBeCancelled() {
        Context app=RuntimeEnvironment.getApplication();
        Vibrator vibrator=app.getSystemService(Vibrator.class);
        AlarmVibration.start(vibrator,VibrationEffect.createWaveform(new long[]{0,700,300},0));
        var shadow=Shadows.shadowOf(vibrator);
        assertTrue(shadow.isVibrating());
        if(Build.VERSION.SDK_INT>=33) {
            VibrationAttributes attributes=(VibrationAttributes)shadow.getVibrationAttributesFromLastVibration();
            assertNotNull(attributes);assertEquals(VibrationAttributes.USAGE_ALARM,attributes.getUsage());
        } else {
            assertEquals(android.media.AudioAttributes.USAGE_ALARM,
                    shadow.getAudioAttributesFromLastVibration().getUsage());
        }
        vibrator.cancel(); assertFalse(shadow.isVibrating());
    }
}
