package com.ilia.advanceclock;

import android.app.Activity;
import android.media.*;
import android.net.Uri;
import android.os.*;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.*;

/** One audible preview at a time; paused positions survive switching rows. No streaming or looping. */
final class SoundPreviewController implements AutoCloseable {
    private static WeakReference<SoundPreviewController> audible=new WeakReference<>(null);
    private static final class Clip {int position,duration;boolean metadata;}
    private final Activity host;
    private final Map<String,Clip> clips=new HashMap<>();
    private final Set<SoundPreviewRow> rows=new HashSet<>();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService metadata=Executors.newSingleThreadExecutor(),audio=Executors.newSingleThreadExecutor();
    private volatile MediaPlayer player;
    private volatile boolean closed;
    private String active;
    private boolean prepared,wanted;
    private AudioFocusRequest focus;
    private final AudioManager manager;
    SoundPreviewController(Activity host){this.host=host;manager=host.getSystemService(AudioManager.class);}
    private Uri source(String uri){
        Uri result=uri==null || uri.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(uri);
        if(result==null)result=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);return LocalMedia.require(result);
    }
    void register(SoundPreviewRow row){
        if(closed)return;rows.add(row);Clip clip=clips.computeIfAbsent(row.uri,k->new Clip());update(row.uri);
        if(!row.available || clip.metadata)return;clip.metadata=true;
        metadata.execute(()->{
            int duration=0;MediaMetadataRetriever retriever=new MediaMetadataRetriever();
            try{retriever.setDataSource(host.getApplicationContext(),LocalMedia.require(source(row.uri)));String raw=retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);if(raw!=null)duration=Math.max(0,Integer.parseInt(raw));}catch(Exception ignored){}
            finally{try{retriever.release();}catch(Exception ignored){}}
            int result=duration;main.post(()->{if(closed)return;Clip item=clips.get(row.uri);if(item!=null && item.duration==0)item.duration=result;update(row.uri);});
        });
    }
    void unregister(SoundPreviewRow row){rows.remove(row);if(Objects.equals(active,row.uri))pause();}
    void hidden(SoundPreviewRow row){if(Objects.equals(active,row.uri))pause();}
    private void update(String uri){Clip clip=clips.get(uri);if(clip==null)return;for(SoundPreviewRow row:rows)if(row.uri.equals(uri))row.update(clip.position,clip.duration,Objects.equals(active,uri)&&wanted);}
    void toggle(SoundPreviewRow row){
        if(closed || !row.available)return;
        if(Objects.equals(active,row.uri) && player!=null){if(wanted)pause();else{wanted=true;startIfReady();update(active);}return;}
        pause();SoundPreviewController previous=audible.get();if(previous!=null && previous!=this)previous.pause();audible=new WeakReference<>(this);
        active=row.uri;wanted=true;prepared=false;Clip clip=clips.computeIfAbsent(active,k->new Clip());MediaPlayer next=new MediaPlayer();player=next;
        next.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());next.setLooping(false);
        next.setOnPreparedListener(ready->{if(closed || player!=ready)return;prepared=true;try{clip.duration=Math.max(0,ready.getDuration());if(clip.position>0)ready.seekTo(Math.min(clip.position,clip.duration));startIfReady();update(active);}catch(Exception e){failed();}});
        next.setOnCompletionListener(done->{if(player!=done)return;clip.position=0;wanted=false;main.removeCallbacks(tick);abandonFocus();update(active);});
        next.setOnErrorListener((failed,what,extra)->{if(player==failed)failed();return true;});update(active);
        final String uri=row.uri;audio.execute(()->{
            try{if(closed || player!=next)return;next.setDataSource(host.getApplicationContext(),LocalMedia.require(source(uri)));if(!closed && player==next)next.prepareAsync();}
            catch(Exception error){main.post(()->{if(player==next)failed();});}
        });
    }
    private void startIfReady(){
        if(!prepared || !wanted || player==null)return;
        try{
            if(manager!=null){focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setWillPauseWhenDucked(true).setOnAudioFocusChangeListener(change->{if(change<0)pause();},main).build();
                if(manager.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){wanted=false;abandonFocus();update(active);return;}}
            Clip clip=clips.get(active);if(clip!=null)player.seekTo(clip.position);player.start();main.removeCallbacks(tick);main.postDelayed(tick,250);
        }catch(Exception error){failed();}
    }
    void seek(SoundPreviewRow row,int position){Clip clip=clips.get(row.uri);if(clip==null || closed)return;clip.position=Math.max(0,Math.min(position,clip.duration));if(Objects.equals(active,row.uri) && prepared && player!=null)try{player.seekTo(clip.position);}catch(Exception e){failed();}update(row.uri);}
    private final Runnable tick=new Runnable(){public void run(){if(closed || !prepared || !wanted || player==null)return;try{clips.get(active).position=player.getCurrentPosition();update(active);main.postDelayed(this,250);}catch(Exception e){failed();}}};
    void pause(){
        main.removeCallbacks(tick);wanted=false;
        MediaPlayer old=player;
        if(old!=null && prepared)try{old.pause();Clip clip=clips.get(active);if(clip!=null)clip.position=old.getCurrentPosition();}catch(Exception ignored){}
        String previous=active;player=null;prepared=false;abandonFocus();
        if(old!=null){old.setOnPreparedListener(null);old.setOnCompletionListener(null);old.setOnErrorListener(null);audio.execute(()->{try{old.release();}catch(Exception ignored){}});}
        if(previous!=null)update(previous);active=null;
    }
    private void abandonFocus(){if(manager!=null && focus!=null)manager.abandonAudioFocusRequest(focus);focus=null;}
    private void failed(){pause();if(!closed)LogoToast.makeText(host,R.string.sound_preview_error,android.widget.Toast.LENGTH_SHORT).show();}
    @Override public void close(){if(closed)return;pause();closed=true;rows.clear();clips.clear();metadata.shutdownNow();audio.shutdown();main.removeCallbacksAndMessages(null);if(audible.get()==this)audible.clear();}
}
