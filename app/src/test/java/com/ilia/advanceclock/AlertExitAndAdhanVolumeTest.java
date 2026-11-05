package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.media.MediaPlayer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows={ReminderScreenFrameworkTest.PlainStore.class,SoundPreviewTest.Player.class,SoundPreviewTest.Metadata.class})
public class AlertExitAndAdhanVolumeTest {
 static final class Managed<T extends android.app.Service> implements AutoCloseable {
  final org.robolectric.android.controller.ServiceController<T> controller;
  Managed(Class<T> type){controller=Robolectric.buildService(type).create();}
  T get(){return controller.get();}
  public void close(){controller.destroy();}
 }
 <T extends android.app.Service> Managed<T> service(Class<T> type){return new Managed<>(type);}
 Application c;
 @Before public void init(){c=RuntimeEnvironment.getApplication();AppString.init(c);}
 @Test public void liveAdhanVolumeUpdatesImmediatelyAndZeroIsActuallySilent(){
  try(var service=service(AdhanSoundService.class)){
   AdhanSoundService output=service.get();MediaPlayer player=new MediaPlayer();ReflectionHelpers.setField(output,"player",player);ReflectionHelpers.setField(output,"playing",true);ReflectionHelpers.setField(output,"type",AdhanScheduler.FAJR);
   SoundPreviewTest.Player fake=org.robolectric.shadow.api.Shadow.extract(player);
   AppSettings.setAdhanVolume(c,AdhanScheduler.FAJR,23);assertEquals(.23f,fake.volume,.001f);
   AppSettings.setAdhanVolume(c,AdhanScheduler.DHUHR,99);assertEquals(.23f,fake.volume,.001f);
   AppSettings.setAdhanVolume(c,899,0);assertEquals(0f,fake.volume,0);
   AppSettings.setAdhanVolume(c,899,41);assertEquals(.41f,fake.volume,.001f);
  }
 }
 @Test public void muezzinsExcludeSystemDefaultAndOldEmptyChoiceResolvesToBundledAdhan(){
  assertTrue(SoundLibrary.all(c,true).stream().noneMatch(s->s.uri.isEmpty()));
  assertTrue(SoundLibrary.all(c,false).get(0).uri.isEmpty());
  AppSettings.setAdhanSoundUri(c,AdhanScheduler.FAJR,"");assertTrue(AppSettings.adhanSoundUri(c,AdhanScheduler.FAJR).endsWith("/raw/rawadhan_2"));
 }
 @Test public void homeAndBackStopClockToolAndNoteOutputsImmediately(){
  for(int exit:new int[]{0,1,2}){
   long id=952+exit;
   try(var service=service(AlarmSoundService.class)){
    ReflectionHelpers.setField(service.get(),"playing",true);ReflectionHelpers.setField(service.get(),"currentAlarmId",id);
    try(var host=Robolectric.buildActivity(AlarmRingActivity.class,new Intent(c,AlarmRingActivity.class).putExtra("alarmId",id)).setup().visible()){
     if(exit==1)host.get().onBackPressed();else if(exit==2)host.pause().stop();else ReflectionHelpers.callInstanceMethod(host.get(),"onUserLeaveHint");
     assertTrue(host.get().isFinishing());assertFalse(ReflectionHelpers.<Boolean>getField(service.get(),"playing"));
    }
   }
   NoForgetItem item=new NoForgetItem(id,"Note","Body","[]",2,true,System.currentTimeMillis()+100000,true,1);new NoForgetStore(c).save(item);
   try(var service=service(NoForgetSoundService.class)){
    ReflectionHelpers.setField(service.get(),"active",true);ReflectionHelpers.setField(service.get(),"currentItem",item);
    try(var host=Robolectric.buildActivity(NoteReminderActivity.class,new Intent(c,NoteReminderActivity.class).putExtra("noteId",id)).setup().visible()){
     if(exit==1)host.get().onBackPressed();else if(exit==2)host.pause().stop();else ReflectionHelpers.callInstanceMethod(host.get(),"onUserLeaveHint");
     assertTrue(host.get().isFinishing());assertFalse(ReflectionHelpers.<Boolean>getField(service.get(),"active"));
    }
   }
  }
 }
 @Test public void leavingAdhanStopsSoundWhileOrdinaryPauseDoesNotCancelItsRing(){
  try(var service=service(AdhanSoundService.class)){
   ReflectionHelpers.setField(service.get(),"playing",true);ReflectionHelpers.setField(service.get(),"type",AdhanScheduler.FAJR);
   try(var host=Robolectric.buildActivity(AdhanRingActivity.class,new Intent(c,AdhanRingActivity.class).putExtra("adhanType",AdhanScheduler.FAJR)).setup().visible()){
    assertEquals(android.media.AudioManager.STREAM_ALARM,host.get().getVolumeControlStream());
    Shadows.shadowOf(c.getSystemService(android.os.PowerManager.class)).setIsInteractive(false);host.pause().stop();assertTrue(ReflectionHelpers.<Boolean>getField(service.get(),"playing"));host.restart().start().resume();Shadows.shadowOf(c.getSystemService(android.os.PowerManager.class)).setIsInteractive(true);
    ReflectionHelpers.callInstanceMethod(host.get(),"onUserLeaveHint");assertTrue(host.get().isFinishing());assertFalse(ReflectionHelpers.<Boolean>getField(service.get(),"playing"));
   }
  }
 }
 @Test public void removingTheTaskStopsAnyRemainingActiveRing(){
  try(var alarm=service(AlarmSoundService.class);var note=service(NoForgetSoundService.class);var adhan=service(AdhanSoundService.class)){
   ReflectionHelpers.setField(alarm.get(),"playing",true);ReflectionHelpers.setField(note.get(),"active",true);ReflectionHelpers.setField(adhan.get(),"playing",true);
   alarm.get().onTaskRemoved(new Intent());note.get().onTaskRemoved(new Intent());adhan.get().onTaskRemoved(new Intent());
   assertFalse(ReflectionHelpers.<Boolean>getField(alarm.get(),"playing"));assertFalse(ReflectionHelpers.<Boolean>getField(note.get(),"active"));assertFalse(ReflectionHelpers.<Boolean>getField(adhan.get(),"playing"));
  }
 }
}
