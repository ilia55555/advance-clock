package com.ilia.advanceclock;

import java.util.Calendar;

/** Resolves an explicit user save/start/activation. Automatic recurrence never rearms a one-shot. */
public final class AlarmTimePolicy {
    private AlarmTimePolicy() {}

    public static long next(long selected, boolean applyDate, int recurrence, int interval,
                            String customDates, long now) {
        long base = selected;
        if (!applyDate) {
            Calendar time = Calendar.getInstance();
            time.setTimeInMillis(selected);
            Calendar today = Calendar.getInstance();
            today.setTimeInMillis(now);
            today.set(Calendar.HOUR_OF_DAY, time.get(Calendar.HOUR_OF_DAY));
            today.set(Calendar.MINUTE, time.get(Calendar.MINUTE));
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);
            if (recurrence == RecurrenceUtils.NONE && today.getTimeInMillis() <= now)
                today.add(Calendar.DAY_OF_YEAR, 1);
            base = today.getTimeInMillis();
        }
        return RecurrenceUtils.next(base, recurrence, interval, customDates, now);
    }
}
