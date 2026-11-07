package com.ilia.advanceclock;

import android.content.Context;
import android.media.AudioManager;

/** Each alarm owns its chosen percentage; only a real ring changes the system alarm stream. */
final class AlarmVolume {
    static int normalize(int percent){return percent<0?-1:Math.min(100,percent);}
    static int current(Context context){
        AudioManager audio=context.getSystemService(AudioManager.class);
        if(audio==null)return 50;
        int max=audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        return max>0?Math.round(audio.getStreamVolume(AudioManager.STREAM_ALARM)*100f/max):0;
    }
    static void apply(Context context,int percent){
        percent=normalize(percent);if(percent<0)return;
        AudioManager audio=context.getSystemService(AudioManager.class);if(audio==null)return;
        int max=audio.getStreamMaxVolume(AudioManager.STREAM_ALARM);
        int level=Math.round(max*percent/100f);
        if(percent>0 && max>0)level=Math.max(1,level);
        try{audio.setStreamVolume(AudioManager.STREAM_ALARM,level,0);}catch(SecurityException ignored){}
    }
}
