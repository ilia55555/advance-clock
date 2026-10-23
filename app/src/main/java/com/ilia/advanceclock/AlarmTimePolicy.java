package com.ilia.advanceclock;

import java.util.Calendar;

/** Resolves an explicit user save/start/activation. Automatic recurrence never rearms a one-shot. */
public final class AlarmTimePolicy {
    private AlarmTimePolicy() {}

    public static long next(long selected, boolean applyDate, int recurrence, int interval,
                            String customDates, long now) {
        return next(selected,applyDate,recurrence,interval,customDates,now,AppTime.zone());
    }
    public static long next(long selected,boolean applyDate,int recurrence,int interval,String customDates,long now,java.util.TimeZone zone){
        long base = selected;
        if (!applyDate) {
            Calendar time = Calendar.getInstance(zone);
            time.setTimeInMillis(selected);
            Calendar today = Calendar.getInstance(zone);
            today.setTimeInMillis(now);
            today.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
            today.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            if (recurrence == RecurrenceUtils.NONE && today.getTimeInMillis() <= now)
                today.add(Calendar.DAY_OF_YEAR, 1);
            base = today.getTimeInMillis();
        }
        return RecurrenceUtils.next(base, recurrence, interval, customDates, now,zone);
    }
}
