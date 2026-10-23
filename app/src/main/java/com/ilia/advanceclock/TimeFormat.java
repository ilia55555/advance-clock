package com.ilia.advanceclock;

import android.content.Context;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

final class TimeFormat {
    static SimpleDateFormat formatter(Context context, boolean seconds, TimeZone zone) {
        boolean full=AppSettings.clock24Hour(context);
        Locale locale=AppString.locale();
        SimpleDateFormat format=new SimpleDateFormat(full?(seconds?"HH:mm:ss":"HH:mm"):(seconds?"hh:mm:ss a":"hh:mm a"),locale);
        DateFormatSymbols symbols=format.getDateFormatSymbols();
        if("fa".equals(locale.getLanguage())) symbols.setAmPmStrings(new String[]{"ق.ظ","ب.ظ"});
        else if("ar".equals(locale.getLanguage())) symbols.setAmPmStrings(new String[]{"ص","م"});
        else if("en".equals(locale.getLanguage())) symbols.setAmPmStrings(new String[]{"AM","PM"});
        format.setDateFormatSymbols(symbols);format.setTimeZone(zone);return format;
    }
    static String clock(Context context,long millis) {
        return CalendarUtils.fa(formatter(context,false,TimeZone.getDefault()).format(new java.util.Date(millis)));
    }
}
