package com.ilia.advanceclock;

/** Completion includes the last automatic reminder and any manually snoozed ring. */
final class AlarmCompletion {
    private AlarmCompletion() {}
    static boolean finished(AlarmItem item, long now) {
        if (item.pendingSnoozeAtMillis > 0L) return false;
        if (RecurrenceUtils.next(item.triggerAtMillis, item.recurrenceMode,
                item.intervalDays, item.customDatesJson, now) > now) return false;
        int lastReminderMinutes = 0;
        for (int minutes : AlarmReminderUtils.effective(item.reminderMode, item.reminderMinutesJson))
            lastReminderMinutes = Math.max(lastReminderMinutes, minutes);
        return item.lastFiredAtMillis <= 0L
                || item.lastFiredAtMillis + lastReminderMinutes * 60_000L <= now;
    }
}
