package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows={MunajatTest.Player.class,SoundPreviewTest.Metadata.class,ReminderScreenFrameworkTest.PlainStore.class})
public class MunajatTest {
    @Implements(MediaPlayer.class) public static class Player extends SoundPreviewTest.Player {
        @Implementation protected void setWakeMode(Context c,int mode){}
        @Implementation protected void stop(){playing=false;}
        @Implementation protected void setLooping(boolean loop){}
    }
    Application app;
    @Before public void init(){app=RuntimeEnvironment.getApplication();AppString.init(app);SoundPreviewTest.Player.instances.clear();SoundPreviewTest.Player.deferred=false;Shadows.shadowOf(app.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);}
    void store(int type,boolean before,MunajatConfig value){android.content.SharedPreferences.Editor editor=MunajatConfig.prefs(app).edit();value.write(editor,type,before);editor.apply();}
    MunajatConfig config(String uri){MunajatConfig v=new MunajatConfig();v.enabled=true;v.uri=uri;v.start=10;v.end=60;v.gradual=true;return v;}
    void drain(){Shadows.shadowOf(Looper.getMainLooper()).idle();}
    Player fake(AdhanSoundService service){MediaPlayer p=ReflectionHelpers.getField(service,"player");assertNotNull(p);return org.robolectric.shadow.api.Shadow.extract(p);}
    @Test public void separateLibraryAndIndependentBeforeAfterSettingsNeverLeakIntoAlarmOrAdhanLists(){
        assertFalse(MunajatConfig.load(app,0,true).enabled);
        SoundLibrary.addMunajat(app,Uri.parse("file:///munajat-before.mp3"));
        assertTrue(SoundLibrary.munajat(app).stream().anyMatch(s->s.uri.endsWith("munajat-before.mp3")));
        assertFalse(SoundLibrary.all(app).stream().anyMatch(s->s.uri.endsWith("munajat-before.mp3")));
        assertFalse(SoundLibrary.all(app,true).stream().anyMatch(s->s.uri.endsWith("munajat-before.mp3")));
        MunajatConfig before=config("file:///before.mp3"),after=config("file:///after.mp3");after.end=40;store(0,true,before);store(0,false,after);
        assertEquals(60,MunajatConfig.load(app,0,true).end);assertEquals(40,MunajatConfig.load(app,0,false).end);assertFalse(MunajatConfig.load(app,1,true).enabled);
        assertEquals(1f/6,before.gain(0),.001f);assertEquals(35f/60,before.gain(.5f),.001f);assertEquals(1,before.gain(1),0);
        before.gradual=false;assertEquals(1,before.gain(0),0);before.end=0;assertEquals(0,before.gain(1),0);
        before.uri="https://example.com/audio.mp3";assertFalse(before.playable());
    }
    @Test public void earlyAlarmHasSeparateIdentityAndLateOrDisabledBroadcastDoesNotPlay(){
        AppSettings.setFajrAdhanEnabled(app,true);AppSettings.setPrayerLocation(app,35.69,51.39,"Tehran","Asia/Tehran");store(0,true,config("file:///before.mp3"));
        AlarmManager manager=app.getSystemService(AlarmManager.class);assertTrue(AdhanScheduler.scheduleNext(app,0));
        boolean found=false;for(org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm alarm:Shadows.shadowOf(manager).getScheduledAlarms()){
            Intent event=Shadows.shadowOf(alarm.operation).getSavedIntent();if(event.getBooleanExtra("munajatBefore",false)){found=true;assertEquals(event.getLongExtra("prayerAt",0)-600000,alarm.triggerAtTime);}
        }assertTrue(found);
        Intent late=new Intent(app,AdhanReceiver.class).putExtra("adhanType",0).putExtra("munajatBefore",true).putExtra("prayerAt",System.currentTimeMillis()-1);
        new AdhanReceiver().onReceive(app,late);assertNull(Shadows.shadowOf(app).getNextStartedService());
        AppSettings.setFajrAdhanEnabled(app,false);AdhanScheduler.rescheduleAll(app);
        assertTrue(Shadows.shadowOf(manager).getScheduledAlarms().stream().noneMatch(a->Shadows.shadowOf(a.operation).getSavedIntent().getBooleanExtra("munajatBefore",false)));
    }
    @Test public void beforeHandsOffToAdhanAndAfterOnlyStartsOnNaturalCompletionWithGradualVolume(){
        store(0,true,config("file:///before.mp3"));store(0,false,config("file:///after.mp3"));AppSettings.setAdhanSound(app,0,true);
        var controller=Robolectric.buildService(AdhanSoundService.class).create();try{
            AdhanSoundService service=controller.get();service.onStartCommand(AdhanSoundService.command(app,AdhanSoundService.ACTION_MUNAJAT_BEFORE).putExtra("adhanType",0).putExtra("prayerAt",System.currentTimeMillis()+600000),0,1);drain();
            Player before=fake(service);assertTrue(before.playing);assertEquals(1f/6,before.volume,.001f);assertEquals(1,ReflectionHelpers.<Integer>getField(service,"phase").intValue());
            before.position=60000;Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));assertEquals(35f/60,before.volume,.001f);
            service.onStartCommand(AdhanSoundService.startIntent(app,0),0,2);drain();assertTrue(before.released);assertEquals(0,ReflectionHelpers.<Integer>getField(service,"phase").intValue());
            Player adhan=fake(service);assertEquals(1,adhan.volume,0);MediaPlayer audio=ReflectionHelpers.getField(service,"player");adhan.playing=false;adhan.complete.onCompletion(audio);drain();
            assertEquals(2,ReflectionHelpers.<Integer>getField(service,"phase").intValue());assertTrue(adhan.released);Player after=fake(service);assertTrue(after.playing);assertEquals(1f/6,after.volume,.001f);
            service.onStartCommand(AdhanSoundService.command(app,AdhanSoundService.ACTION_STOP),0,3);assertTrue(after.released);
        }finally{controller.destroy();}
    }
    @Test public void stoppingAdhanDoesNotStartAfterAndBeforeCannotInterruptAnAdhan(){
        store(0,true,config("file:///before.mp3"));store(0,false,config("file:///after.mp3"));
        var controller=Robolectric.buildService(AdhanSoundService.class).create();try{
            AdhanSoundService service=controller.get();service.onStartCommand(AdhanSoundService.startIntent(app,0),0,1);drain();Player active=fake(service);
            service.onStartCommand(AdhanSoundService.command(app,AdhanSoundService.ACTION_MUNAJAT_BEFORE).putExtra("adhanType",1).putExtra("prayerAt",System.currentTimeMillis()+600000),0,2);
            assertSame(active,fake(service));assertFalse(active.released);assertEquals(0,ReflectionHelpers.<Integer>getField(service,"phase").intValue());
            service.onStartCommand(AdhanSoundService.command(app,AdhanSoundService.ACTION_STOP),0,3);assertTrue(active.released);assertNull(ReflectionHelpers.getField(service,"player"));
        }finally{controller.destroy();}
    }
    @Test public void noteUsesSavedVolumeAndDateSwitchSharesTheDateBoxOppositeTime()throws Exception{
        AppSettings.setAdhanSound(app,0,true);
        NoForgetItem note=new NoForgetItem(17,"Note","","[]",2,true,System.currentTimeMillis()+600000,true,15);note.volumePercent=40;new NoForgetStore(app).save(note);
        assertEquals(40,new NoForgetStore(app).find(17).volumePercent);
        var service=Robolectric.buildService(NoForgetSoundService.class).create();try{service.get().onStartCommand(NoForgetSoundService.startIntent(app,17),0,1);AudioManager audio=app.getSystemService(AudioManager.class);assertEquals(Math.round(audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)*.4f),audio.getStreamVolume(AudioManager.STREAM_ALARM));}finally{service.destroy();}
        try(var host=Robolectric.buildActivity(NoForgetEditorActivity.class,new Intent(app,NoForgetEditorActivity.class).putExtra("noteId",17L)).setup()){
            View date=host.get().findViewById(R.id.note_date),time=host.get().findViewById(R.id.note_time),toggle=host.get().findViewById(R.id.date_option_note_editor);
            assertSame(date.getParent(),toggle.getParent());assertSame(date.getParent().getParent(),time.getParent());assertEquals(40,((SeekBar)host.get().findViewById(R.id.alarm_volume)).getProgress());
            ((Switch)toggle).setChecked(false);assertFalse(date.isEnabled());((Switch)toggle).setChecked(true);assertTrue(date.isEnabled());
        }
    }
    @Test public void settingsSaveTwoChoicesAndLeadTimeForOnlyTheSelectedPrayer(){
        try(var host=Robolectric.buildActivity(MunajatSettingsActivity.class,new Intent(app,MunajatSettingsActivity.class).putExtra("adhanType",0)).setup().visible()){
            MunajatSettingsActivity activity=host.get();View root=activity.findViewById(android.R.id.content);
            ((Switch)root.findViewWithTag("munajat_before_enabled")).setChecked(true);((Switch)root.findViewWithTag("munajat_after_enabled")).setChecked(true);
            ((EditText)root.findViewWithTag("munajat_minutes")).setText("۱۵");
            activity.onActivityResult(920,Activity.RESULT_OK,new Intent().putExtra(SoundPickerActivity.EXTRA_URI,"file:///before.mp3"));
            activity.onActivityResult(921,Activity.RESULT_OK,new Intent().putExtra(SoundPickerActivity.EXTRA_URI,"file:///after.mp3"));
            ((SeekBar)root.findViewWithTag("munajat_end_1")).setProgress(40);root.findViewWithTag("munajat_save").performClick();
            assertTrue(activity.isFinishing());assertEquals(15,MunajatConfig.load(app,0,true).minutes);assertEquals("file:///before.mp3",MunajatConfig.load(app,0,true).uri);assertEquals("file:///after.mp3",MunajatConfig.load(app,0,false).uri);assertEquals(40,MunajatConfig.load(app,0,false).end);assertFalse(MunajatConfig.load(app,1,true).enabled);
        }
        try(var host=Robolectric.buildActivity(SoundPickerActivity.class,new Intent(app,SoundPickerActivity.class).putExtra(SoundPickerActivity.EXTRA_MUNAJAT,true)).setup().visible()){
            assertFalse(host.get().findViewById(android.R.id.content).findViewWithTag("online_sounds_check").isShown());
        }
    }
    @Test public void beforeStopsAtPrayerDeadlineAndFailedPreparationCannotRunForever(){
        store(0,true,config("file:///before.mp3"));
        var controller=Robolectric.buildService(AdhanSoundService.class).create();try{
            AdhanSoundService service=controller.get();service.onStartCommand(AdhanSoundService.command(app,AdhanSoundService.ACTION_MUNAJAT_BEFORE).putExtra("adhanType",0).putExtra("prayerAt",System.currentTimeMillis()+5000),0,1);drain();Player before=fake(service);
            before.position=2500;Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(200));assertEquals(35f/60,before.volume,.01f);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(4900));assertTrue(before.released);assertNull(ReflectionHelpers.getField(service,"player"));
        }finally{controller.destroy();}
        SoundPreviewTest.Player.deferred=true;controller=Robolectric.buildService(AdhanSoundService.class).create();try{
            AdhanSoundService service=controller.get();service.onStartCommand(AdhanSoundService.command(app,AdhanSoundService.ACTION_MUNAJAT_BEFORE).putExtra("adhanType",0).putExtra("prayerAt",System.currentTimeMillis()+600000),0,2);
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(31));assertTrue(Shadows.shadowOf(service).isStoppedBySelf());
        }finally{controller.destroy();SoundPreviewTest.Player.deferred=false;}
    }
    @Test public void explicitlyEnabledMunajatStillWorksWhenTheAdhanAudioIsOff(){
        AppSettings.setAdhanSound(app,0,false);store(0,false,config("file:///after.mp3"));
        var controller=Robolectric.buildService(AdhanSoundService.class).create();try{
            AdhanSoundService service=controller.get();service.onStartCommand(AdhanSoundService.startIntent(app,0),0,1);drain();
            assertEquals(2,ReflectionHelpers.<Integer>getField(service,"phase").intValue());assertTrue(fake(service).playing);
        }finally{controller.destroy();}
    }
}
