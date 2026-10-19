package com.ilia.advanceclock;

import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AlarmReminderUtilsTest {
    private AlarmItem alarm() {
        AlarmItem item = new AlarmItem(42L, "Meeting", 1_000_000L, AlarmItem.REPEAT_NONE, true);
        item.reminderMode = AlarmReminderUtils.MODE_SMART;
        item.lastFiredAtMillis = 1_000_000L;
        return item;
    }

    @Test public void smartUsesFixedOffsetsFromThePrimaryOccurrence() {
        assertEquals(Arrays.asList(15, 60, 180, 720, 1440),
                AlarmReminderUtils.effective(AlarmReminderUtils.MODE_SMART, "[5]"));
    }

    @Test public void customOffsetsAreSortedUniqueAndRestrictedToAvailableChoices() {
        assertEquals(Arrays.asList(5, 60, 1440), AlarmReminderUtils.effective(
                AlarmReminderUtils.MODE_CUSTOM, "[60,5,60,-1,0,1440,9999]"));
        assertTrue(AlarmReminderUtils.effective(AlarmReminderUtils.MODE_CUSTOM, "invalid").isEmpty());
        assertTrue(AlarmReminderUtils.effective(AlarmReminderUtils.MODE_NONE, "[15]").isEmpty());
    }

    @Test public void completedOneShotStillRingsItsSelectedReminders() {
        AlarmItem item = alarm();
        item.enabled = false; // The primary receiver disables a completed one-shot.
        assertTrue(AlarmReminderUtils.isScheduledReminder(item, item.lastFiredAtMillis, 15));
        assertTrue(AlarmReminderUtils.isScheduledReminder(item, item.lastFiredAtMillis, 1440));
        assertFalse(AlarmReminderUtils.isScheduledReminder(item, item.lastFiredAtMillis, 5));
    }

    @Test public void deletionCancellationAndRemovedSelectionsCannotReplayAnAlarm() {
        AlarmItem item = alarm();
        assertFalse(AlarmReminderUtils.isScheduledReminder(null, 1_000_000L, 15));
        item.lastFiredAtMillis = 0L; // Explicit cancellation clears the occurrence.
        assertFalse(AlarmReminderUtils.isScheduledReminder(item, 1_000_000L, 15));
        item.lastFiredAtMillis = 1_000_000L;
        item.reminderMode = AlarmReminderUtils.MODE_NONE;
        assertFalse(AlarmReminderUtils.isScheduledReminder(item, 1_000_000L, 15));
        item.reminderMode = AlarmReminderUtils.MODE_CUSTOM;
        item.reminderMinutesJson = "[60]";
        assertFalse(AlarmReminderUtils.isScheduledReminder(item, 1_000_000L, 15));
        assertTrue(AlarmReminderUtils.isScheduledReminder(item, 1_000_000L, 60));
    }

    @Test public void recurringAlarmDoesNotInvalidateRemindersOfThePreviousOccurrence() {
        AlarmItem item = alarm();
        item.lastFiredAtMillis = 2_000_000L;
        assertTrue(AlarmReminderUtils.isScheduledReminder(item, 1_000_000L, 1440));
        assertFalse(AlarmReminderUtils.isScheduledReminder(item, 0L, 15));
        assertFalse(AlarmReminderUtils.isScheduledReminder(item, 3_000_000L, 15));
    }
}
