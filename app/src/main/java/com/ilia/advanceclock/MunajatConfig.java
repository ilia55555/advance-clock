package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;

/** Separate, opt-in configuration for each prayer and each side of its adhan. */
final class MunajatConfig {
    boolean enabled, gradual=true;
    String uri="";
    int minutes=10,start=10,end=60;
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("munajat_settings",0);}
    static String prefix(int type,boolean before){return type+"_"+(before?"before_":"after_");}
    static MunajatConfig load(Context c,int type,boolean before){
        SharedPreferences p=prefs(c);String k=prefix(type,before);MunajatConfig v=new MunajatConfig();
        v.enabled=p.getBoolean(k+"enabled",false);v.gradual=p.getBoolean(k+"gradual",true);
        v.uri=p.getString(k+"uri","");v.minutes=Math.max(1,Math.min(1440,p.getInt(k+"minutes",10)));
        v.start=Math.max(0,Math.min(100,p.getInt(k+"start",10)));v.end=Math.max(0,Math.min(100,p.getInt(k+"end",60)));return v;
    }
    void write(SharedPreferences.Editor editor,int type,boolean before){
        String k=prefix(type,before);editor.putBoolean(k+"enabled",enabled).putBoolean(k+"gradual",gradual)
                .putString(k+"uri",uri).putInt(k+"minutes",minutes).putInt(k+"start",start).putInt(k+"end",end);
    }
    boolean playable(){if(!enabled || uri.isEmpty())return false;try{LocalMedia.require(android.net.Uri.parse(uri));return true;}catch(Exception e){return false;}}
    int ceiling(){return gradual?Math.max(start,end):end;}
    float gain(float fraction){
        int max=ceiling();if(max==0)return 0;
        float percent=gradual?start+(end-start)*Math.max(0,Math.min(1,fraction)):end;
        return Math.max(0,Math.min(1,percent/max));
    }
}
