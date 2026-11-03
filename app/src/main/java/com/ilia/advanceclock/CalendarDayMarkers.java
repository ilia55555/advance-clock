package com.ilia.advanceclock;

import android.content.Context;
import java.util.List;

/** Per-view snapshot; never put user events into the shared calendar-render cache. */
final class CalendarDayMarkers {
    private final List<AlarmItem> alarms;
    private final List<NoForgetItem> notes;
    CalendarDayMarkers(Context context, boolean noteMode) {
        alarms=noteMode ? java.util.Collections.emptyList() : new AlarmStore(context).all();
        notes=noteMode ? new NoForgetStore(context).all() : java.util.Collections.emptyList();
    }
    int priority(long start,long end) {
        int[] values = priorities(start,end);
        return values.length == 0 ? -1 : values[0];
    }
    int[] priorities(long start,long end) {
        java.util.ArrayList<Integer> priorities = new java.util.ArrayList<>();
        for(AlarmItem item:alarms) if(item.enabled) {
            long next=RecurrenceUtils.next(item.triggerAtMillis,item.recurrenceMode,item.intervalDays,
                    item.customDatesJson,start-1,item.timeZone());
            if(next>=start && next<end) priorities.add(item.priority<0?-1:PriorityUtils.clamp(item.priority));
        }
        for(NoForgetItem item:notes) {
            if (!item.applyDate && !item.hasDue) continue;
            // A calendar date is independent of whether the note rings.
            // Older non-ringing notes did not retain the chosen date: use their creation day.
            long date = item.dueAtMillis > 0 ? item.dueAtMillis : item.createdAt;
            if (date <= 0) continue;
            long next = item.hasDue
                    ? RecurrenceUtils.next(date,item.recurrenceMode,item.intervalDays,item.customDatesJson,start-1)
                    : date;
            if(next>=start && next<end) priorities.add(item.priority<0?-1:PriorityUtils.clamp(item.priority));
        }
        priorities.sort(java.util.Collections.reverseOrder());
        int[] result = new int[Math.min(2,priorities.size())];
        for(int i=0;i<result.length;i++) result[i]=priorities.get(i);
        return result;
    }
    static int color(int priority) {
        if(priority<0)return 0xFF78909C;
        return new int[]{0xFF2E9D50,0xFF75A843,0xFFE5AC28,0xFFED762D,0xFFD32F2F}[PriorityUtils.clamp(priority)];
    }
}
