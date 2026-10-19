package com.ilia.advanceclock;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ScheduleOrderTest {
    private AlarmItem alarm(long id, long due, boolean enabled) {
        return new AlarmItem(id, "", due, 0, enabled);
    }
    private NoForgetItem note(long id, long due, boolean enabled, long created) {
        return new NoForgetItem(id, "", "", "[]", 2, due > 0, due, enabled, created);
    }
    @Test public void activeAlarmsPrecedeDisabledAndUseTheNearestFutureOccurrence() {
        AlarmItem late = alarm(10, 5000, true), soon = alarm(11, 2000, true);
        AlarmItem disabled = alarm(99, 1500, false), expired = alarm(100, 500, true);
        ArrayList<AlarmItem> items = new ArrayList<>(Arrays.asList(disabled, late, expired, soon));
        ScheduleOrder.alarms(items, 1000);
        assertEquals(Arrays.asList(soon, late, expired, disabled), items);
    }
    @Test public void equalAlarmTimesPreferTheNewestAndInactiveAlarmsAreNewestFirst() {
        AlarmItem older = alarm(1, 2000, true), newer = alarm(2, 2000, true);
        AlarmItem oldOff = alarm(3, 100, false), newOff = alarm(4, 50, false);
        ArrayList<AlarmItem> items = new ArrayList<>(Arrays.asList(oldOff, older, newOff, newer));
        ScheduleOrder.alarms(items, 1000);
        assertEquals(Arrays.asList(newer, older, newOff, oldOff), items);
    }
    @Test public void recurringAlarmsUseTheirNextOccurrenceRatherThanTheOldBaseDate() {
        long now = 1_000_000_000L;
        AlarmItem repeating = alarm(1, now - 2 * 86400_000L + 60000, true);
        repeating.recurrenceMode = RecurrenceUtils.DAILY;
        AlarmItem sooner = alarm(2, now + 30000, true);
        ArrayList<AlarmItem> items = new ArrayList<>(Arrays.asList(repeating, sooner));
        ScheduleOrder.alarms(items, now);
        assertEquals(sooner, items.get(0));
        assertEquals(repeating, items.get(1));
    }
    @Test public void notesPrioritizeActiveDueDatesOverPriorityOrOldOverdueNotes() {
        NoForgetItem soon = note(1, 2000, true, 10), later = note(2, 3000, true, 20);
        NoForgetItem overdue = note(3, 500, false, 30), newest = note(4, 0, false, 40);
        overdue.priority = PriorityUtils.VERY_HIGH;
        ArrayList<NoForgetItem> items = new ArrayList<>(Arrays.asList(overdue, newest, later, soon));
        ScheduleOrder.notes(items, 1000);
        assertEquals(Arrays.asList(soon, later, newest, overdue), items);
    }
    @Test public void equalNoteDatesPreferTheMostRecentlyCreatedNote() {
        NoForgetItem old = note(1, 2000, true, 10), newer = note(2, 2000, true, 20);
        ArrayList<NoForgetItem> items = new ArrayList<>(Arrays.asList(old, newer));
        ScheduleOrder.notes(items, 1000);
        assertEquals(newer, items.get(0));
    }
}
