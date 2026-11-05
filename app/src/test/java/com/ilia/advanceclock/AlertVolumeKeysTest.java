package com.ilia.advanceclock;

import android.app.*;
import android.media.AudioManager;
import android.os.*;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=AlertVolumeKeysTest.PerStreamAudio.class)
public class AlertVolumeKeysTest {
 // Robolectric 4.17 adjustStreamVolume changes every simulated stream and cannot reach zero.
 // Model the Android per-stream adjustment while also checking the production call's target.
 @org.robolectric.annotation.Implements(AudioManager.class)
 public static class PerStreamAudio extends org.robolectric.shadows.ShadowAudioManager {
  @org.robolectric.annotation.RealObject AudioManager real;
  @org.robolectric.annotation.Implementation protected void adjustStreamVolume(int stream,int direction,int flags){
   assertEquals(AudioManager.STREAM_ALARM,stream);
   real.setStreamVolume(stream,Math.max(0,Math.min(real.getStreamMaxVolume(stream),real.getStreamVolume(stream)+direction)),flags);
  }
 }
 KeyEvent event(int action,int code,int repeat){return new KeyEvent(0,100,action,code,repeat);}
 @Test public void tapsMuteOrUnmuteOnlyAfterReleaseAndRestoreAudibilityFromZero(){
  try(var host=Robolectric.buildActivity(Activity.class).setup()){
   int[] mute={0},unmute={0};AudioManager audio=host.get().getSystemService(AudioManager.class);audio.setStreamVolume(AudioManager.STREAM_ALARM,3,0);int before=audio.getStreamVolume(AudioManager.STREAM_ALARM);
   AlertVolumeKeys keys=new AlertVolumeKeys(host.get(),()->mute[0]++,()->unmute[0]++);
   assertTrue(keys.down(KeyEvent.KEYCODE_VOLUME_DOWN,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_DOWN,0)));assertEquals(0,mute[0]);
   keys.up(KeyEvent.KEYCODE_VOLUME_DOWN,event(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_VOLUME_DOWN,0));assertEquals(1,mute[0]);assertEquals(before,audio.getStreamVolume(AudioManager.STREAM_ALARM));
   keys.down(KeyEvent.KEYCODE_VOLUME_UP,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP,0));assertEquals(0,unmute[0]);keys.up(KeyEvent.KEYCODE_VOLUME_UP,event(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_VOLUME_UP,0));assertEquals(1,unmute[0]);
   audio.setStreamVolume(AudioManager.STREAM_ALARM,0,0);keys.down(KeyEvent.KEYCODE_VOLUME_UP,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP,0));keys.up(KeyEvent.KEYCODE_VOLUME_UP,event(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_VOLUME_UP,0));assertTrue(audio.getStreamVolume(AudioManager.STREAM_ALARM)>0);
   assertFalse(keys.down(KeyEvent.KEYCODE_ENTER,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_ENTER,0)));keys.cancel();
  }
 }
 @Test public void holdsRepeatAlarmVolumeChangesWithoutTapMuteAndStopAsSoonAsReleased(){
  try(var host=Robolectric.buildActivity(Activity.class).setup()){
   AudioManager audio=host.get().getSystemService(AudioManager.class);audio.setStreamVolume(AudioManager.STREAM_ALARM,3,0);audio.setStreamVolume(AudioManager.STREAM_MUSIC,2,0);int media=audio.getStreamVolume(AudioManager.STREAM_MUSIC);int before=audio.getStreamVolume(AudioManager.STREAM_ALARM);
   int[] mute={0},unmute={0};AlertVolumeKeys keys=new AlertVolumeKeys(host.get(),()->mute[0]++,()->unmute[0]++);
   keys.down(KeyEvent.KEYCODE_VOLUME_DOWN,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_DOWN,0));Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(ViewConfiguration.getLongPressTimeout()+180));
   assertTrue(audio.getStreamVolume(AudioManager.STREAM_ALARM)<before);assertEquals(0,mute[0]);assertEquals(1,unmute[0]);
   keys.up(KeyEvent.KEYCODE_VOLUME_DOWN,event(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_VOLUME_DOWN,0));int paused=audio.getStreamVolume(AudioManager.STREAM_ALARM);Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));assertEquals(paused,audio.getStreamVolume(AudioManager.STREAM_ALARM));assertEquals(0,mute[0]);
   keys.down(KeyEvent.KEYCODE_VOLUME_UP,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_UP,0));Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(ViewConfiguration.getLongPressTimeout()+180));assertTrue(audio.getStreamVolume(AudioManager.STREAM_ALARM)>paused);assertEquals(media,audio.getStreamVolume(AudioManager.STREAM_MUSIC));keys.cancel();
  }
 }
 @Test public void repeatEventsDoNotDoubleAdjustAndCancelledOrInterruptedPressesDoNotBecomeTaps(){
  try(var host=Robolectric.buildActivity(Activity.class).setup()){
   AudioManager audio=host.get().getSystemService(AudioManager.class);audio.setStreamVolume(AudioManager.STREAM_ALARM,3,0);int before=audio.getStreamVolume(AudioManager.STREAM_ALARM);
   int[] taps={0};AlertVolumeKeys keys=new AlertVolumeKeys(host.get(),()->taps[0]++,()->{});
   keys.down(KeyEvent.KEYCODE_VOLUME_DOWN,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_DOWN,0));keys.down(KeyEvent.KEYCODE_VOLUME_DOWN,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_DOWN,1));int once=audio.getStreamVolume(AudioManager.STREAM_ALARM);assertTrue(once<before);
   keys.down(KeyEvent.KEYCODE_VOLUME_DOWN,event(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_VOLUME_DOWN,2));assertEquals(once,audio.getStreamVolume(AudioManager.STREAM_ALARM));keys.cancel();
   keys.up(KeyEvent.KEYCODE_VOLUME_DOWN,event(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_VOLUME_DOWN,0));Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));assertEquals(once,audio.getStreamVolume(AudioManager.STREAM_ALARM));assertEquals(0,taps[0]);
  }
 }
}
