package com.ilia.advanceclock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Shared list order for the app and widgets: active, nearest occurrence, then newest. */
final class ScheduleOrder {
    private ScheduleOrder() {}

    static void alarms(List<AlarmItem> items, long now) {
        Map<Long, Long> next = new HashMap<>();
        for (AlarmItem item : items) next.put(item.id, item.enabled ? RecurrenceUtils.next(
                item.triggerAtMillis, item.recurrenceMode, item.intervalDays, item.customDatesJson, now) : -1L);
        items.sort((a, b) -> compare(next.get(a.id), a.id, next.get(b.id), b.id));
    }

    static void notes(List<NoForgetItem> items, long now) {
        Map<Long, Long> next = new HashMap<>();
        for (NoForgetItem item : items) next.put(item.id, item.reminderEnabled && item.hasDue
                ? RecurrenceUtils.next(item.dueAtMillis, item.recurrenceMode,
                item.intervalDays, item.customDatesJson, now) : -1L);
        items.sort((a, b) -> {
            int order = compare(next.get(a.id), a.createdAt, next.get(b.id), b.createdAt);
            return order != 0 ? order : Long.compare(b.id, a.id);
        });
    }

    private static int compare(long firstNext, long firstCreated, long secondNext, long secondCreated) {
        boolean firstActive = firstNext > 0L, secondActive = secondNext > 0L;
        if (firstActive != secondActive) return firstActive ? -1 : 1;
        if (firstActive) {
            int time = Long.compare(firstNext, secondNext);
            if (time != 0) return time;
        }
        return Long.compare(secondCreated, firstCreated);
    }
}
