package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

/** Separate, opt-in configuration for each prayer and each side of its adhan. */
final class MunajatConfig {
    boolean enabled, gradual=true;
    String uri="";
    int minutes=0,start=10,end=60;
    /** Audio length in ms, cached when saved; 0 = unknown. */
    long duration;
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("munajat_settings",0);}
    static String prefix(int type,boolean before){return type+"_"+(before?"before_":"after_");}
    static MunajatConfig load(Context c,int type,boolean before){
        SharedPreferences p=prefs(c);String k=prefix(type,before);MunajatConfig v=new MunajatConfig();
        v.enabled=p.getBoolean(k+"enabled",false);v.gradual=p.getBoolean(k+"gradual",true);
        v.uri=p.getString(k+"uri","");
        // Default before-adhan munajat bundled with the app (res/raw/munajat_before.mp3), if present.
        if(v.uri.isEmpty() && before){int id=c.getResources().getIdentifier("munajat_before","raw",c.getPackageName());
            if(id!=0)v.uri="android.resource://"+c.getPackageName()+"/"+id;}v.minutes=Math.max(0,Math.min(1440,p.getInt(k+"minutes",0)));v.duration=Math.max(0,p.getLong(k+"duration",0));
        v.start=Math.max(0,Math.min(100,p.getInt(k+"start",10)));v.end=Math.max(0,Math.min(100,p.getInt(k+"end",60)));return v;
    }
    void write(SharedPreferences.Editor editor,int type,boolean before){
        String k=prefix(type,before);editor.putBoolean(k+"enabled",enabled).putBoolean(k+"gradual",gradual)
                .putString(k+"uri",uri).putInt(k+"minutes",minutes).putLong(k+"duration",duration).putInt(k+"start",start).putInt(k+"end",end);
    }
    /** Length of a local audio file in ms (0 if unknown). Call off the main thread when possible. */
    static long durationOf(Context c,String uri){
        if(uri==null || uri.isEmpty())return 0;android.media.MediaMetadataRetriever r=new android.media.MediaMetadataRetriever();
        try{r.setDataSource(c,LocalMedia.require(android.net.Uri.parse(uri)));String raw=r.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);return raw==null?0:Math.max(0,Long.parseLong(raw));}
        catch(Exception e){return 0;}finally{try{r.release();}catch(Exception ignored){}}
    }
    /** Smallest whole number of minutes that fits the whole audio before the adhan. */
    static int minimumMinutes(long duration){return duration<=0?1:(int)Math.max(1,(duration+59_999)/60_000);}
    /** How long before the adhan to start: 0 = exactly the audio length, so it ends at the adhan. */
    long leadMillis(Context c){
        if(minutes>0)return minutes*60_000L;
        long length=duration>0?duration:durationOf(c,uri);
        return Math.max(1_000L,length);
    }
    boolean playable(){if(!enabled || uri.isEmpty())return false;try{LocalMedia.require(android.net.Uri.parse(uri));return true;}catch(Exception e){return false;}}
    int ceiling(){return gradual?Math.max(start,end):end;}
    float gain(float fraction){
        int max=ceiling();if(max==0)return 0;
        float percent=gradual?start+(end-start)*Math.max(0,Math.min(1,fraction)):end;
        return Math.max(0,Math.min(1,percent/max));
    }
}
