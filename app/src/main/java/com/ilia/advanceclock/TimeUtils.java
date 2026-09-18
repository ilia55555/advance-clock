package com.ilia.advanceclock;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public final class TimeUtils {
    private TimeUtils() {}

    public static long normalizeFuture(long trigger, int repeatType, long now) {
        if (trigger > now || repeatType == AlarmItem.REPEAT_NONE) return trigger;
        long value = trigger;
        int guard = 0;
        while (value <= now && guard++ < 5000) value = nextOccurrence(value, repeatType);
        return value;
    }

    public static long nextOccurrence(long fromMillis, int repeatType) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(fromMillis);
        switch (repeatType) {
            case AlarmItem.REPEAT_DAILY: c.add(Calendar.DAY_OF_YEAR, 1); break;
            case AlarmItem.REPEAT_WEEKLY: c.add(Calendar.WEEK_OF_YEAR, 1); break;
            case AlarmItem.REPEAT_MONTHLY: c.add(Calendar.MONTH, 1); break;
            case AlarmItem.REPEAT_YEARLY: c.add(Calendar.YEAR, 1); break;
            default: return fromMillis;
        }
        return c.getTimeInMillis();
    }

    public static String formatDateTime(long millis) {
        return new SimpleDateFormat("yyyy/MM/dd  HH:mm", new Locale("fa", "IR")).format(new Date(millis));
    }

    public static String repeatLabel(int repeatType) {
        switch (repeatType) {
            case AlarmItem.REPEAT_DAILY: return "هر روز";
            case AlarmItem.REPEAT_WEEKLY: return "هر هفته";
            case AlarmItem.REPEAT_MONTHLY: return "هر ماه";
            case AlarmItem.REPEAT_YEARLY: return "هر سال";
            default: return "بدون تکرار";
        }
    }
}
