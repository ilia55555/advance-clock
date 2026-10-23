package com.ilia.advanceclock;

import android.app.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlertActionsTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    @Test public void compactActionsAreRightToLeftAndToggleSoundAndVibrationIndependently() {
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();AppSettings.applyTheme(host);
            boolean[] state={false,true};int[] opened={0};
            AlertOutput output=new AlertOutput(){public boolean muted(){return state[0];}public boolean vibrating(){return state[1];}
                public void mute(boolean v){state[0]=v;}public void vibrate(boolean v){state[1]=v;}};
            AlertActions actions=new AlertActions(host,output,"Open timer",()->opened[0]++,15,0xff005599,m->{});
            host.setContentView(actions);
            LinearLayout row=(LinearLayout)actions.getChildAt(0);assertEquals(View.LAYOUT_DIRECTION_RTL,row.getLayoutDirection());
            ImageButton sound=actions.findViewWithTag("alert_sound"),vibration=actions.findViewWithTag("alert_vibration");
            assertSame(row.getChildAt(0),sound.getParent());assertSame(row.getChildAt(1),vibration.getParent());
            assertEquals(sound.getLayoutParams().width,sound.getLayoutParams().height);assertEquals(0f,sound.getElevation(),0f);
            sound.performClick();assertTrue(state[0]);assertTrue(state[1]);
            assertEquals(AppString.get(R.string.alert_unmute),sound.getContentDescription());
            vibration.performClick();assertFalse(state[1]);assertTrue(state[0]);
            assertEquals(AppString.get(R.string.alert_vibration_on),vibration.getContentDescription());
            actions.findViewWithTag("alert_open").performClick();assertEquals(1,opened[0]);
            state[0]=false;actions.refreshState();assertEquals(AppString.get(R.string.alert_mute),sound.getContentDescription());
        }
    }
    @Test public void modalConfirmsAllFiveSelectedTimesTogetherAndCancelChangesNothing() {
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();AppSettings.applyTheme(host);
            int[][] selected={null};AlertActions actions=new AlertActions(host,null,"Open",()->{},15,0xff005599,m->selected[0]=m);
            host.setContentView(actions);actions.findViewWithTag("alert_snooze").performClick();
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            for(int index:new int[]{0,1,4,9})dialog.getListView().performItemClick(null,index,index);
            assertNull(selected[0]);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            assertArrayEquals(new int[]{5,10,15,60,1440},selected[0]);assertFalse(dialog.isShowing());
            selected[0]=null;actions.findViewWithTag("alert_snooze").performClick();
            dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertNull(selected[0]);
        }
    }
    @Test public void emptySelectionKeepsModalOpenUntilAtLeastOneTimeIsSelected() {
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();AppSettings.applyTheme(host);
            int[][] selected={null};AlertActions actions=new AlertActions(host,null,"Open",()->{},15,0xff005599,m->selected[0]=m);
            host.setContentView(actions);actions.findViewWithTag("alert_snooze").performClick();
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            dialog.getListView().performItemClick(null,2,2);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertTrue(dialog.isShowing());assertNull(selected[0]);
            dialog.getListView().performItemClick(null,0,0);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();assertArrayEquals(new int[]{5},selected[0]);
        }
    }
    @Test public void serviceOutputControlsOnlyAffectMatchingOccurrenceAndPreserveSavedSettings() {
        var alarm=Robolectric.buildService(AlarmSoundService.class).create();
        var note=Robolectric.buildService(NoForgetSoundService.class).create();
        var adhan=Robolectric.buildService(AdhanSoundService.class).create();
        try {
            ReflectionHelpers.setField(alarm.get(),"playing",true);ReflectionHelpers.setField(alarm.get(),"currentAlarmId",42L);
            ReflectionHelpers.setField(alarm.get(),"currentVibrate",true);
            NoForgetItem item=new NoForgetItem(7,"Note","","[]",2,true,System.currentTimeMillis()+50000,true,System.currentTimeMillis());
            new NoForgetStore(RuntimeEnvironment.getApplication()).save(item);
            ReflectionHelpers.setField(note.get(),"active",true);ReflectionHelpers.setField(note.get(),"currentItem",item);
            ReflectionHelpers.setField(note.get(),"vibrationEnabled",true);
            ReflectionHelpers.setField(adhan.get(),"playing",true);ReflectionHelpers.setField(adhan.get(),"type",0);
            ReflectionHelpers.setField(adhan.get(),"vibrationEnabled",true);
            for(AlertOutput output:new AlertOutput[]{AlertOutput.alarm(42),AlertOutput.note(7),AlertOutput.adhan(0)}) {
                output.mute(true);assertTrue(output.muted());assertTrue(output.vibrating());
                output.vibrate(false);assertFalse(output.vibrating());assertTrue(output.muted());
                output.vibrate(true);assertTrue(output.vibrating());output.mute(false);assertFalse(output.muted());
            }
            AlertOutput.alarm(43).mute(true);assertFalse(AlertOutput.alarm(42).muted());
            assertTrue(new NoForgetStore(RuntimeEnvironment.getApplication()).find(7).vibrate);
        } finally {alarm.destroy();note.destroy();adhan.destroy();}
    }
}
