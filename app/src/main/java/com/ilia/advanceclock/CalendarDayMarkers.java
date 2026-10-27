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
        int priority=-1;
        for(AlarmItem item:alarms) if(item.enabled) {
            long next=RecurrenceUtils.next(item.triggerAtMillis,item.recurrenceMode,item.intervalDays,
                    item.customDatesJson,start-1,item.timeZone());
            if(next>=start && next<end) priority=Math.max(priority,item.priority);
        }
        for(NoForgetItem item:notes) if(item.hasDue) {
            long next=RecurrenceUtils.next(item.dueAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,start-1);
            if(next>=start && next<end) priority=Math.max(priority,item.priority);
        }
        return priority;
    }
    static int color(int priority) {
        return new int[]{0xFF2E9D50,0xFF75A843,0xFFE5AC28,0xFFED762D,0xFFD32F2F}[PriorityUtils.clamp(priority)];
    }
}
