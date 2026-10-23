package com.ilia.advanceclock;

import android.app.Application;
import android.content.Intent;
import android.media.MediaPlayer;
import android.view.KeyEvent;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows={AlarmPreparationFrameworkTest.FakePlayer.class,ReminderScreenFrameworkTest.PlainStore.class})
public class AlarmPreviewOutputTest {
    @Before public void init(){
        AppString.init(RuntimeEnvironment.getApplication());
        AlarmPreparationFrameworkTest.FakePlayer.created=AlarmPreparationFrameworkTest.FakePlayer.started=AlarmPreparationFrameworkTest.FakePlayer.released=0;
        AlarmPreparationFrameworkTest.FakePlayer.deferPreparation=false;
    }
    @Test public void previewPlaysAndHardwareAndButtonsControlRealOutputsUntilClosed(){
        try(var controller=Robolectric.buildActivity(AlarmPreviewActivity.class).setup().visible()){
            AlarmPreviewActivity host=controller.get();
            PreviewAlertOutput output=ReflectionHelpers.getField(host,"output");
            assertEquals(1,AlarmPreparationFrameworkTest.FakePlayer.started);
            assertFalse(output.muted());assertTrue(output.vibrating());
            host.onKeyDown(KeyEvent.KEYCODE_VOLUME_DOWN,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_DOWN));
            assertTrue(output.muted());assertFalse(output.vibrating());
            output.mute(false);output.vibrate(true);assertFalse(output.muted());assertTrue(output.vibrating());
            host.sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue(output.muted());assertFalse(output.vibrating());
            host.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP));
            assertFalse(output.muted());assertFalse(output.vibrating());
            host.finish();assertTrue(output.muted());assertFalse(output.vibrating());
            assertEquals(1,AlarmPreparationFrameworkTest.FakePlayer.released);
            assertNull(Shadows.shadowOf(host).getNextStartedService());
        }
    }
    @Test public void snoozeConfirmationSchedulesRealReminderAndOpenGoesToClock(){
        Application app=RuntimeEnvironment.getApplication();
        Shadows.shadowOf(app.getSystemService(android.app.AlarmManager.class)).setCanScheduleExactAlarms(true);
        try(var controller=Robolectric.buildActivity(AlarmPreviewActivity.class).setup().visible()){
            AlarmPreviewActivity host=controller.get();
            host.findViewById(android.R.id.content).findViewWithTag("alert_snooze").performClick();
            android.app.AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
            assertEquals(1,app.getSharedPreferences(AlertSnoozes.PREFS,0).getAll().size());
            assertTrue(host.isFinishing());
            assertEquals(1,AlarmPreparationFrameworkTest.FakePlayer.released);
        }
        try(var controller=Robolectric.buildActivity(AlarmPreviewActivity.class).setup().visible()){
            AlarmPreviewActivity host=controller.get();
            host.findViewById(android.R.id.content).findViewWithTag("alert_open").performClick();
            Intent target=Shadows.shadowOf(host).getNextStartedActivity();
            assertEquals(MainActivity.class.getName(),target.getComponent().getClassName());
            assertEquals("clock",target.getStringExtra("openTab"));assertTrue(host.isFinishing());
        }
    }
    @Test public void leavingOrClosingBeforePreparationNeverStartsLateAudio(){
        AlarmPreparationFrameworkTest.FakePlayer.deferPreparation=true;
        try(var controller=Robolectric.buildActivity(AlarmPreviewActivity.class).setup().visible()){
            AlarmPreviewActivity host=controller.get();
            MediaPlayer.OnPreparedListener late=AlarmPreparationFrameworkTest.FakePlayer.lastListener;
            MediaPlayer player=AlarmPreparationFrameworkTest.FakePlayer.lastPlayer;
            controller.pause().stop();assertTrue(host.isFinishing());
            late.onPrepared(player);
            assertEquals(0,AlarmPreparationFrameworkTest.FakePlayer.started);
            assertEquals(1,AlarmPreparationFrameworkTest.FakePlayer.released);
        }
    }
}
