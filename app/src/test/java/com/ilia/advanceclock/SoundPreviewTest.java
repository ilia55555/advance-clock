package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.media.*;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows={SoundPreviewTest.Player.class,SoundPreviewTest.Metadata.class,ReminderScreenFrameworkTest.PlainStore.class})
public class SoundPreviewTest {
 @Implements(MediaPlayer.class) public static class Player {
  @RealObject MediaPlayer real;static final List<MediaPlayer> instances=new ArrayList<>();static boolean deferred;
  MediaPlayer.OnPreparedListener prepared;MediaPlayer.OnCompletionListener complete;int position;boolean playing,released;float volume=1f;
  @Implementation protected void __constructor__(){instances.add(real);}
  @Implementation protected void setAudioAttributes(AudioAttributes a){assertEquals(AudioAttributes.USAGE_ALARM,a.getUsage());}
  @Implementation protected void setVolume(float left,float right){assertEquals(left,right,0);volume=left;}
  @Implementation protected void setLooping(boolean b){assertFalse(b);}
  @Implementation protected void setDataSource(Context c,Uri uri){LocalMedia.require(uri);}
  @Implementation protected void setOnPreparedListener(MediaPlayer.OnPreparedListener p){prepared=p;}
  @Implementation protected void setOnCompletionListener(MediaPlayer.OnCompletionListener p){complete=p;}
  @Implementation protected void setOnErrorListener(MediaPlayer.OnErrorListener p){}
  @Implementation protected void prepareAsync(){if(!deferred)new Handler(Looper.getMainLooper()).post(()->{if(prepared!=null)prepared.onPrepared(real);});}
  @Implementation protected int getDuration(){return 120000;}
  @Implementation protected int getCurrentPosition(){return position;}
  @Implementation protected void seekTo(int value){position=value;}
  @Implementation protected void start(){assertFalse(released);playing=true;assertEquals(1,instances.stream().filter(p->org.robolectric.shadow.api.Shadow.<Player>extract(p).playing).count());}
  @Implementation protected void pause(){playing=false;}
  @Implementation protected void release(){playing=false;released=true;}
 }
 @Implements(MediaMetadataRetriever.class) public static class Metadata {
  @Implementation protected void __constructor__(){}
  @Implementation protected void setDataSource(Context c,Uri uri){LocalMedia.require(uri);}
  @Implementation protected String extractMetadata(int key){return "120000";}
  @Implementation protected void release(){}
 }
 final List<SoundPreviewController> controllers=new ArrayList<>();
 @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());Player.instances.clear();Player.deferred=false;}
 @After public void close()throws Exception{for(SoundPreviewController c:controllers){c.close();ExecutorService worker=ReflectionHelpers.getField(c,"audio");worker.awaitTermination(2,TimeUnit.SECONDS);}Shadows.shadowOf(Looper.getMainLooper()).idle();}
 SoundPreviewController controller(Activity host){SoundPreviewController c=new SoundPreviewController(host);controllers.add(c);return c;}
 void drain(SoundPreviewController c)throws Exception{for(String key:new String[]{"audio","metadata"}){ExecutorService worker=ReflectionHelpers.getField(c,key);worker.submit(()->{}).get(3,TimeUnit.SECONDS);}Shadows.shadowOf(Looper.getMainLooper()).idle();}
 SoundPreviewRow row(Activity host,SoundPreviewController c,String uri){return new SoundPreviewRow(host,c,uri,uri,true,new Button(host),null);}
 void play(SoundPreviewRow row){row.findViewWithTag("sound_preview_play").performClick();}
 Player player(SoundPreviewController c){return org.robolectric.shadow.api.Shadow.<Player>extract(ReflectionHelpers.<MediaPlayer>getField(c,"player"));}
 @Test public void switchingPausesPreviousAndResumesItsSavedPositionWithOnlyOneAudiblePlayer()throws Exception{
  try(var host=Robolectric.buildActivity(Activity.class).setup().visible()){
   SoundPreviewController c=controller(host.get());SoundPreviewRow first=row(host.get(),c,"file:///first.mp3"),second=row(host.get(),c,"file:///second.mp3");
   play(first);drain(c);Player old=player(c);assertTrue(old.playing);assertEquals(AudioManager.STREAM_ALARM,host.get().getVolumeControlStream());old.position=17000;
   play(second);assertFalse(old.playing);drain(c);assertTrue(player(c).playing);
   play(first);drain(c);assertTrue(player(c).playing);assertEquals(17000,player(c).position);
   play(first);drain(c);assertNull(ReflectionHelpers.getField(c,"player"));assertEquals(AudioManager.USE_DEFAULT_STREAM_TYPE,host.get().getVolumeControlStream());
  }
 }
 @Test public void timelinesShowDurationAllowPausedAndPlayingSeekingAndResetAtCompletion()throws Exception{
  try(var host=Robolectric.buildActivity(Activity.class).setup().visible()){
   SoundPreviewController c=controller(host.get());SoundPreviewRow row=row(host.get(),c,"file:///seek.mp3");drain(c);
   assertEquals("2:00",((TextView)row.findViewWithTag("sound_preview_duration")).getText().toString());assertTrue(row.findViewWithTag("sound_preview_seek").isEnabled());
   c.seek(row,45000);assertEquals("0:45",((TextView)row.findViewWithTag("sound_preview_elapsed")).getText().toString());play(row);drain(c);assertEquals(45000,player(c).position);
   c.seek(row,70000);assertEquals(70000,player(c).position);
   MediaPlayer current=ReflectionHelpers.getField(c,"player");Player p=player(c);p.playing=false;p.complete.onCompletion(current);
   assertEquals("0:00",((TextView)row.findViewWithTag("sound_preview_elapsed")).getText().toString());play(row);assertTrue(p.playing);assertEquals(0,p.position);
  }
 }
 @Test public void latePreparationCannotRestartPausedOrClosedPreviewAndDifferentControllersAreExclusive()throws Exception{
  try(var host=Robolectric.buildActivity(Activity.class).setup().visible()){
   SoundPreviewController first=controller(host.get()),second=controller(host.get());SoundPreviewRow a=row(host.get(),first,"file:///a.mp3"),b=row(host.get(),second,"file:///b.mp3");
   Player.deferred=true;play(a);drain(first);MediaPlayer pending=ReflectionHelpers.getField(first,"player");MediaPlayer.OnPreparedListener late=player(first).prepared;
   first.pause();late.onPrepared(pending);assertTrue(Player.instances.stream().noneMatch(p->org.robolectric.shadow.api.Shadow.<Player>extract(p).playing));
   Player.deferred=false;play(a);drain(first);Player old=player(first);play(b);drain(second);assertFalse(old.playing);assertTrue(player(second).playing);
   second.close();assertFalse(Player.instances.stream().anyMatch(p->org.robolectric.shadow.api.Shadow.<Player>extract(p).playing));
  }
 }
 @Test public void pickerExposesPreviewWithoutSelectingAndStopsWhenLeaving()throws Exception{
  try(var host=Robolectric.buildActivity(SoundPickerActivity.class).setup().visible()){
   SoundPreviewController c=ReflectionHelpers.getField(host.get(),"previews");controllers.add(c);
   View root=host.get().findViewById(android.R.id.content);root.findViewWithTag("sound_preview_play").performClick();drain(c);
   assertTrue(player(c).playing);assertFalse(host.get().isFinishing());assertNull(Shadows.shadowOf(host.get()).getNextStartedActivity());
   host.pause().stop();drain(c);assertTrue(Player.instances.stream().noneMatch(p->org.robolectric.shadow.api.Shadow.<Player>extract(p).playing));
  }
 }
 @Test public void adhanListUsesSameControlsAndSelectingStillChangesOnlyTheSelectedAdhan()throws Exception{
  try(var host=Robolectric.buildActivity(SettingsActivity.class,new Intent(RuntimeEnvironment.getApplication(),SettingsActivity.class).putExtra(SettingsActivity.EXTRA_SECTION,"prayer")).setup().visible()){
   PrayerSettingsPanel panel=ReflectionHelpers.getField(host.get(),"prayerPanel");SoundPreviewController c=ReflectionHelpers.getField(panel,"soundPreviews");controllers.add(c);
   LinearLayout root=new LinearLayout(host.get());root.setOrientation(LinearLayout.VERTICAL);host.get().setContentView(root);
   ReflectionHelpers.callInstanceMethod(panel,"showAdhanSettings",ReflectionHelpers.ClassParameter.from(String.class,"Adhan"),ReflectionHelpers.ClassParameter.from(int.class,AdhanScheduler.FAJR),ReflectionHelpers.ClassParameter.from(LinearLayout.class,root));
   LinearLayout list=ReflectionHelpers.getField(panel,"activeMuezzinList");assertTrue(list.getChildCount()>=2);SoundPreviewRow row=(SoundPreviewRow)list.getChildAt(1);
   String before=AppSettings.adhanSoundUri(host.get(),AdhanScheduler.FAJR);play(row);drain(c);assertTrue(player(c).playing);assertEquals(before,AppSettings.adhanSoundUri(host.get(),AdhanScheduler.FAJR));
   SeekBar volume=root.findViewWithTag("adhan_volume_seek");assertNotNull(volume);
   SeekBar.OnSeekBarChangeListener listener=ReflectionHelpers.getField(volume,"mOnSeekBarChangeListener");
   listener.onProgressChanged(volume,19,true);assertEquals(.19f,player(c).volume,.001f);assertEquals(19,AppSettings.adhanVolume(host.get(),AdhanScheduler.FAJR));
   listener.onProgressChanged(volume,0,true);assertEquals(0f,player(c).volume,0f);
   assertNotNull(row.findViewWithTag("sound_preview_seek"));host.pause().stop();drain(c);assertNull(ReflectionHelpers.getField(c,"player"));
  }
 }
}
