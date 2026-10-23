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
    static String summary(Context context,AlarmItem item,long when){
        return CalendarUtils.formatDate(when,AppSettings.defaultCalendar(context),item.timeZone())+"  "
                +CalendarUtils.fa(TimeFormat.formatter(context,false,item.timeZone()).format(new Date(when)))
                +"  •  "+labels(context,Collections.singletonList(item.referenceZoneId),when)[0];
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
