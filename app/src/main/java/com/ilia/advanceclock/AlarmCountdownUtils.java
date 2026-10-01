package com.ilia.advanceclock;

import android.content.Context;

public final class AlarmCountdownUtils {
    private AlarmCountdownUtils() {}

    public static long nextOccurrence(AlarmItem item, long now) {
        if (item == null) return -1L;

        if (item.triggerAtMillis > now) {
            return item.triggerAtMillis;
        }

        return RecurrenceUtils.next(
                item.triggerAtMillis,
                item.recurrenceMode,
                item.intervalDays,
                item.customDatesJson,
                now);
    }

    public static String remainingText(
            Context context,
            AlarmItem item) {
        long now = System.currentTimeMillis();
        long next = nextOccurrence(item, now);
        if (next <= now) {
            return context.getString(
                    R.string.alarm_remaining_expired);
        }

        long totalMinutes = Math.max(
                1L,
                (next - now + 59_999L) / 60_000L);
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;

        if (hours > 0L && minutes > 0L) {
            return context.getString(
                    R.string.alarm_remaining_hours_minutes,
                    hours,
                    minutes);
        }

        if (hours > 0L) {
            return context.getString(
                    R.string.alarm_remaining_hours,
                    hours);
        }

        return context.getString(
                R.string.alarm_remaining_minutes,
                minutes);
    }
}
