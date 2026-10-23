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
    /** Keep the clock and date in separate directional runs inside an LTR summary. */
    static String endTime(Context context, long millis, int calendarType) {
        String time = clock(context, millis);
        String date = CalendarUtils.formatDate(millis, calendarType);
        boolean rtlDate = android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(date, 0, date.length());
        boolean rtlTime = android.text.TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(time, 0, time.length());
        if (!rtlDate && !rtlTime) return time + "   " + date;
        android.text.BidiFormatter bidi = android.text.BidiFormatter.getInstance(false);
        return bidi.unicodeWrap(time, android.text.TextDirectionHeuristics.LTR) + "   "
                + bidi.unicodeWrap(rtlDate ? "\u200F" + date + "\u200F" : date,
                        rtlDate ? android.text.TextDirectionHeuristics.RTL : android.text.TextDirectionHeuristics.LTR);
    }

    static String clock(Context context,long millis) {
        return CalendarUtils.fa(formatter(context,false,TimeZone.getDefault()).format(new java.util.Date(millis)));
    }
}
