package com.ilia.advanceclock;

import android.content.Context;
import android.media.*;
import android.net.Uri;
import android.os.*;

/** Real preview outputs, owned exclusively by the preview screen. */
final class PreviewAlertOutput implements AlertOutput {
    private final Context context;
    private MediaPlayer player;
    private Vibrator vibrator;
    private boolean muted, vibrate=true,closed;
    PreviewAlertOutput(Context context){this.context=context;}
    void start(){
        if(closed)return;
        try{
            String selected=AppSettings.defaultAlarmSoundUri(context);
            Uri uri=selected==null||selected.isEmpty()?RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM):Uri.parse(selected);
            if(uri==null)uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            player=new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setLooping(true);
            player.setOnPreparedListener(ready->{if(player==ready&&!closed){ready.setVolume(muted?0:1,muted?0:1);ready.start();}});
            player.setOnErrorListener((failed,what,extra)->{if(player==failed)releasePlayer();return true;});
            player.setDataSource(context,uri);player.prepareAsync();
        }catch(Exception ignored){releasePlayer();}
        if(Build.VERSION.SDK_INT>=31){
            VibratorManager manager=context.getSystemService(VibratorManager.class);
            vibrator=manager==null?null:manager.getDefaultVibrator();
        }else vibrator=context.getSystemService(Vibrator.class);
        vibrate(vibrate);
    }
    public boolean muted(){return muted;}
    public boolean vibrating(){return vibrate&&!closed;}
    public void mute(boolean value){
        muted=value;try{if(player!=null)player.setVolume(value?0:1,value?0:1);}catch(RuntimeException ignored){}
        AlertOutputEvents.changed();
    }
    public void vibrate(boolean enabled){
        vibrate=enabled;
        try{if(vibrator!=null){vibrator.cancel();if(enabled&&!closed)vibrator.vibrate(VibrationEffect.createWaveform(new long[]{0,700,300},0));}}catch(RuntimeException ignored){}
        AlertOutputEvents.changed();
    }
    void silence(){mute(true);vibrate(false);}
    void close(){closed=true;silence();releasePlayer();}
    private void releasePlayer(){
        MediaPlayer old=player;player=null;
        if(old!=null)try{old.setOnPreparedListener(null);old.release();}catch(RuntimeException ignored){}
    }
}
