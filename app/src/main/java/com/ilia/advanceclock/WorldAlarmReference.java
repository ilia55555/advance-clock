package com.ilia.advanceclock;

import android.content.Context;
import java.util.*;

final class WorldAlarmReference {
    static String[] labels(Context context,List<String> zones,long when){
        String[] labels=new String[zones.size()];
        for(int i=0;i<zones.size();i++){
            String id=zones.get(i);String fallback=id.substring(id.lastIndexOf('/')+1).replace('_',' ');
            labels[i]=WorldClockStore.label(context,id,fallback)+"  (\u2066"+offset(TimeZone.getTimeZone(id),when)+"\u2069)";
        }
        return labels;
    }
    static String referenceName(Context context,AlarmItem item){
        String zone=item.timeZone().getID();
        String name=zone.substring(zone.lastIndexOf('/')+1).replace('_',' ');
        String language=AppString.locale().getLanguage();
        if(zone.equals("Asia/Tehran"))name=language.equals("fa")?"تهران":language.equals("ar")?"طهران":"Tehran";
        if(zone.equals("UTC")||zone.equals("GMT")||zone.equals("Etc/UTC")||zone.equals("Etc/GMT"))
            name=language.equals("fa")?"گرینویچ":language.equals("ar")?"غرينتش":"Greenwich";
        return item.referenceZoneId.isEmpty()?name:WorldClockStore.label(context,zone,name);
    }
    static String summary(Context context,AlarmItem item,long when){
        String clock=CalendarUtils.fa(TimeFormat.formatter(context,false,item.timeZone()).format(new Date(when)));
        return CalendarUtils.formatDate(when,AppSettings.defaultCalendar(context),item.timeZone())+"  "
                +android.text.BidiFormatter.getInstance().unicodeWrap(clock,android.text.TextDirectionHeuristics.LTR)
                +"  "+referenceName(context,item);
    }
    static String offset(TimeZone zone,long when){
        int minutes=zone.getOffset(when)/60_000;
        return String.format(Locale.US,"GMT %s%d:%02d",minutes<0?"-":"+",Math.abs(minutes)/60,Math.abs(minutes)%60);
    }
    static long pickerDate(long millis,TimeZone zone){
        Calendar source=Calendar.getInstance(zone);source.setTimeInMillis(millis);
        Calendar target=Calendar.getInstance();target.clear();
        target.set(source.get(Calendar.YEAR),source.get(Calendar.MONTH),source.get(Calendar.DAY_OF_MONTH),12,0);
        return target.getTimeInMillis();
    }
    static void changeZone(Calendar value,String id){
        int year=value.get(Calendar.YEAR),month=value.get(Calendar.MONTH),day=value.get(Calendar.DAY_OF_MONTH);
        int hour=value.get(Calendar.HOUR_OF_DAY),minute=value.get(Calendar.MINUTE);
        value.setTimeZone(TimeZone.getTimeZone(id));value.clear();value.set(year,month,day,hour,minute,0);
    }
}
