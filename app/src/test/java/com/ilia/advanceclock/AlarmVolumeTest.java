package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import android.media.AudioManager;
import android.widget.SeekBar;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlarmVolumeTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());Shadows.shadowOf(RuntimeEnvironment.getApplication().getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);}
    @Test public void persistedPercentIsIndependentAndLegacyAlarmsKeepCurrentSystemVolume() throws Exception {
        AlarmItem first=new AlarmItem(1,"One",System.currentTimeMillis()+600000,0,true);first.volumePercent=75;
        AlarmItem second=new AlarmItem(2,"Two",first.triggerAtMillis+300000,0,true);second.volumePercent=40;
        AlarmStore store=new AlarmStore(RuntimeEnvironment.getApplication());store.save(first);store.save(second);
        assertEquals(75,store.find(1).volumePercent);assertEquals(40,store.find(2).volumePercent);
        org.json.JSONObject legacy=first.toJson();legacy.remove("volumePercent");assertEquals(-1,AlarmItem.fromJson(legacy).volumePercent);
        legacy.put("volumePercent",500);assertEquals(100,AlarmItem.fromJson(legacy).volumePercent);
    }
    @Test public void editorStartsAtCurrentVolumeSavesSelectionAndReloadsItWithoutChangingPhoneVolume(){
        Application app=RuntimeEnvironment.getApplication();AudioManager audio=app.getSystemService(AudioManager.class);
        audio.setStreamVolume(AudioManager.STREAM_ALARM,3,0);int before=audio.getStreamVolume(AudioManager.STREAM_ALARM);
        long id;
        try(var activity=Robolectric.buildActivity(AlarmEditorActivity.class).setup()){
            AlarmEditorActivity host=activity.get();SeekBar bar=host.findViewById(R.id.alarm_volume);assertEquals(AlarmVolume.current(app),bar.getProgress());
            assertSame(host.findViewById(R.id.alarm_sound_button).getParent(),host.findViewById(R.id.alarm_priority).getParent());
            bar.setProgress(75);assertEquals(before,audio.getStreamVolume(AudioManager.STREAM_ALARM));host.findViewById(R.id.save_alarm).performClick();
            AlarmItem item=new AlarmStore(app).all().get(0);id=item.id;assertEquals(75,item.volumePercent);
        }
        audio.setStreamVolume(AudioManager.STREAM_ALARM,1,0);
        try(var activity=Robolectric.buildActivity(AlarmEditorActivity.class,new Intent(app,AlarmEditorActivity.class).putExtra("alarmId",id)).setup()){
            assertEquals(75,((SeekBar)activity.get().findViewById(R.id.alarm_volume)).getProgress());
        }
    }
    @Test public void actualRingAppliesEachAlarmsSystemVolumeWithoutChangingMediaOrLegacyVolume(){
        Application app=RuntimeEnvironment.getApplication();AudioManager audio=app.getSystemService(AudioManager.class);audio.setStreamVolume(AudioManager.STREAM_MUSIC,2,0);
        int media=audio.getStreamVolume(AudioManager.STREAM_MUSIC),max=audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        for(int percent:new int[]{75,40,0}){
            AlarmItem item=new AlarmItem(7,"Volume",System.currentTimeMillis()+600000,0,true);item.volumePercent=percent;
            AlarmSoundService.ring(app,item);Intent intent=Shadows.shadowOf(app).getNextStartedService();assertNotNull(intent);assertEquals(percent,intent.getIntExtra("volumePercent",-1));
            var service=Robolectric.buildService(AlarmSoundService.class).create();try{
                service.get().onStartCommand(intent,0,1);assertEquals(Math.round(max*percent/100f),audio.getStreamVolume(AudioManager.STREAM_ALARM));
                assertEquals(media,audio.getStreamVolume(AudioManager.STREAM_MUSIC));
                assertEquals(percent==0,AlertOutput.alarm(item.id).muted());
            }finally{service.destroy();}
        }
        audio.setStreamVolume(AudioManager.STREAM_ALARM,2,0);AlarmVolume.apply(app,-1);assertEquals(2,audio.getStreamVolume(AudioManager.STREAM_ALARM));
    }
}
