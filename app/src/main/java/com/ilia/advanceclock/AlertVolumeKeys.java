package com.ilia.advanceclock;

import android.app.Activity;
import android.media.AudioManager;
import android.os.*;
import android.view.*;

/** Resolve a tap on key-up; a hold adjusts alarm volume and never triggers the tap action. */
final class AlertVolumeKeys {
    private final AudioManager audio;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable mute,unmute;
    private int key=KeyEvent.KEYCODE_UNKNOWN;
    private boolean held;
    private final Runnable repeat=new Runnable(){public void run(){if(key==KeyEvent.KEYCODE_UNKNOWN)return;if(!held){held=true;unmute.run();}adjust();main.postDelayed(this,150);}};
    AlertVolumeKeys(Activity host,Runnable mute,Runnable unmute){audio=host.getSystemService(AudioManager.class);this.mute=mute;this.unmute=unmute;host.setVolumeControlStream(AudioManager.STREAM_ALARM);}
    private boolean volume(int code){return code==KeyEvent.KEYCODE_VOLUME_DOWN || code==KeyEvent.KEYCODE_VOLUME_UP;}
    boolean down(int code,KeyEvent event){
        if(!volume(code))return false;
        if(key!=code){cancel();key=code;main.postDelayed(repeat,ViewConfiguration.getLongPressTimeout());}
        else if(event.getRepeatCount()>0 && !held){main.removeCallbacks(repeat);repeat.run();}
        return true;
    }
    boolean up(int code,KeyEvent event){
        if(!volume(code))return false;if(key!=code)return true;
        boolean longPress=held || event.getEventTime()-event.getDownTime()>=ViewConfiguration.getLongPressTimeout();
        main.removeCallbacks(repeat);
        if(!event.isCanceled()){
            if(longPress){if(!held){unmute.run();adjust();}}
            else if(code==KeyEvent.KEYCODE_VOLUME_DOWN)mute.run();else unmuteTap();
        }
        cancel();return true;
    }
    private void unmuteTap(){
        unmute.run();
        try{if(audio!=null && audio.getStreamVolume(AudioManager.STREAM_ALARM)==0)audio.adjustStreamVolume(AudioManager.STREAM_ALARM,AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);}catch(SecurityException ignored){}
    }
    private void adjust(){try{if(audio!=null)audio.adjustStreamVolume(AudioManager.STREAM_ALARM,key==KeyEvent.KEYCODE_VOLUME_DOWN?AudioManager.ADJUST_LOWER:AudioManager.ADJUST_RAISE,AudioManager.FLAG_SHOW_UI);}catch(SecurityException ignored){}}
    void cancel(){main.removeCallbacks(repeat);key=KeyEvent.KEYCODE_UNKNOWN;held=false;}
}
