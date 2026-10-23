package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlertHardwareControlsTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    @Test public void hardwareMuteAndPowerSilenceBothOutputsAndImmediatelyRefreshVisibleIcons() {
        Application app=RuntimeEnvironment.getApplication();
        var alarm=Robolectric.buildService(AlarmSoundService.class).create();
        var note=Robolectric.buildService(NoForgetSoundService.class).create();
        var adhan=Robolectric.buildService(AdhanSoundService.class).create();
        try {
            ReflectionHelpers.setField(alarm.get(),"playing",true);ReflectionHelpers.setField(alarm.get(),"currentAlarmId",42L);
            ReflectionHelpers.setField(note.get(),"active",true);
            ReflectionHelpers.setField(note.get(),"currentItem",new NoForgetItem(7,"Note","","[]",2,true,1000,true,100));
            ReflectionHelpers.setField(adhan.get(),"playing",true);ReflectionHelpers.setField(adhan.get(),"type",0);
            AlertOutput[] outputs={AlertOutput.alarm(42),AlertOutput.note(7),AlertOutput.adhan(0)};
            Runnable[] down={()->alarm.get().onStartCommand(AlarmSoundService.muteIntent(app),0,1),
                ()->note.get().onStartCommand(NoForgetSoundService.volumeIntent(app,true),0,1),
                ()->adhan.get().onStartCommand(AdhanSoundService.command(app,AdhanSoundService.ACTION_MUTE),0,1)};
            for(int i=0;i<outputs.length;i++) {
                AlertOutput output=outputs[i];output.mute(false);output.vibrate(true);
                try(var activity=Robolectric.buildActivity(Activity.class).setup().visible()) {
                    Activity host=activity.get();AppSettings.applyTheme(host);
                    AlertActions actions=new AlertActions(host,output,"Open",()->{},15,0xff123456,m->{});
                    host.setContentView(actions);
                    ImageButton sound=actions.findViewWithTag("alert_sound"),vibration=actions.findViewWithTag("alert_vibration");
                    Drawable soundBefore=sound.getDrawable(),vibrationBefore=vibration.getDrawable();
                    down[i].run();
                    assertSilenced(output,sound,vibration);
                    assertNotSame(soundBefore,sound.getDrawable());assertNotSame(vibrationBefore,vibration.getDrawable());
                    sound.performClick();assertFalse(output.muted());assertFalse(output.vibrating());
                    vibration.performClick();assertTrue(output.vibrating());
                    app.sendBroadcast(new Intent(Intent.ACTION_SCREEN_OFF));
                    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
                    assertSilenced(output,sound,vibration);
                    sound.performClick();vibration.performClick();
                    app.sendBroadcast(new Intent("android.media.VOLUME_CHANGED_ACTION")
                        .putExtra("android.media.EXTRA_VOLUME_STREAM_TYPE",android.media.AudioManager.STREAM_ALARM)
                        .putExtra("android.media.EXTRA_PREV_VOLUME_STREAM_VALUE",5)
                        .putExtra("android.media.EXTRA_VOLUME_STREAM_VALUE",4));
                    Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
                    assertSilenced(output,sound,vibration);
                }
            }
        } finally {alarm.destroy();note.destroy();adhan.destroy();}
    }
    private void assertSilenced(AlertOutput output,ImageButton sound,ImageButton vibration) {
        assertTrue(output.muted());assertFalse(output.vibrating());
        assertEquals(AppString.get(R.string.alert_unmute),sound.getContentDescription());
        assertEquals(AppString.get(R.string.alert_vibration_on),vibration.getContentDescription());
    }

    @Test @GraphicsMode(GraphicsMode.Mode.NATIVE)
    public void compactLabelsStayCompleteOnOneLineAtLargeFontScalesInAllThreeLanguages() {
        Application app=RuntimeEnvironment.getApplication();float previous=RuntimeEnvironment.getFontScale();
        try {
            for(String language:new String[]{"fa","ar","en"}) {
                AppSettings.setLanguage(app,language);
                for(float scale:new float[]{1f,2f,3f}) {
                    RuntimeEnvironment.setFontScale(scale);
                    try(var activity=Robolectric.buildActivity(Activity.class).setup().visible()) {
                        Activity host=activity.get();AppSettings.applyTheme(host);
                        String label=AppString.get(R.string.reminder_open_destination,AppString.get(R.string.tab_stopwatch));
                        AlertActions actions=new AlertActions(host,null,label,()->{},15,0xff123456,m->{});
                        host.setContentView(actions);
                        float density=host.getResources().getDisplayMetrics().density;
                        int width=Math.round(260*density);
                        for(int pass=0;pass<2;pass++) {
                            actions.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
                            actions.layout(0,0,width,actions.getMeasuredHeight());
                        }
                        LinearLayout row=(LinearLayout)actions.getChildAt(0);
                        for(int i=0;i<3;i++) {
                            LinearLayout column=(LinearLayout)row.getChildAt(i);
                            ImageButton button=(ImageButton)column.getChildAt(0);
                            TextView text=(TextView)column.getChildAt(1);
                            assertEquals(Math.round(48*density),button.getWidth());
                            assertEquals(button.getWidth(),button.getHeight());
                            assertEquals(language+" scale="+scale,1,text.getLineCount());
                            assertNull(text.getEllipsize());assertEquals(0,text.getLayout().getEllipsisCount(0));
                            assertTrue(language+" scale="+scale+" text="+text.getText(),
                                text.getLayout().getLineWidth(0)<=text.getWidth()+.5f);
                        }
                        assertEquals(label,((TextView)((LinearLayout)row.getChildAt(2)).getChildAt(1)).getText().toString());
                        View snooze=actions.findViewWithTag("alert_snooze");
                        assertEquals(Math.round(40*density),snooze.getHeight());
                        assertTrue(snooze.getWidth()<actions.getWidth());
                    }
                }
            }
        } finally {RuntimeEnvironment.setFontScale(previous);}
    }
}
