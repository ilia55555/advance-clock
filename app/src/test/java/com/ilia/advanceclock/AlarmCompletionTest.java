package com.ilia.advanceclock;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AlarmCompletionTest {
    private AlarmItem alarm() {
        AlarmItem item = new AlarmItem(1, "", 1_000_000L, 0, false);
        item.lastFiredAtMillis = 1_000_000L;
        item.autoDeleteAfterCompletion = true;
        return item;
    }
    @Test public void oneShotFinishesAfterItsOccurrence() {
        AlarmItem item = alarm();
        assertFalse(AlarmCompletion.finished(item, 999_999L));
        assertTrue(AlarmCompletion.finished(item, 1_000_001L));
    }
    @Test public void finalSmartReminderMustFinishBeforeDeletion() {
        AlarmItem item = alarm(); item.reminderMode = AlarmReminderUtils.MODE_SMART;
        assertFalse(AlarmCompletion.finished(item, item.lastFiredAtMillis + 59 * 60_000L));
        assertTrue(AlarmCompletion.finished(item, item.lastFiredAtMillis + 60 * 60_000L));
    }
    @Test public void customRemindersUseTheLatestSelectedOffset() {
        AlarmItem item = alarm(); item.reminderMode = AlarmReminderUtils.MODE_CUSTOM;
        item.reminderMinutesJson = "[5,1440]";
        assertFalse(AlarmCompletion.finished(item, item.lastFiredAtMillis + 60 * 60_000L));
        assertTrue(AlarmCompletion.finished(item, item.lastFiredAtMillis + 1440 * 60_000L));
    }
    @Test public void aSnoozedAlarmSurvivesEvenWhenDeliveryIsOverdue() {
        AlarmItem item = alarm(); item.pendingSnoozeAtMillis = 2_000_000L;
        assertFalse(AlarmCompletion.finished(item, 1_500_000L));
        assertFalse(AlarmCompletion.finished(item, 3_000_000L));
        item.pendingSnoozeAtMillis = 0L;
        assertTrue(AlarmCompletion.finished(item, 3_000_000L));
    }
    @Test public void recurringAlarmsAreRetainedWhileTheyHaveAFutureOccurrence() {
        AlarmItem item = alarm(); item.recurrenceMode = RecurrenceUtils.DAILY;
        assertFalse(AlarmCompletion.finished(item, 2_000_000L));
        item.recurrenceMode = RecurrenceUtils.CUSTOM_DATES;
        item.customDatesJson = "[3000000]";
        assertFalse(AlarmCompletion.finished(item, 2_000_000L));
        assertTrue(AlarmCompletion.finished(item, 4_000_000L));
    }
    @Test public void newFieldsSurviveReloadAndDefaultToOffForLegacyAlarms() throws Exception {
        AlarmItem item = alarm(); item.pendingSnoozeAtMillis = 2_000_000L;
        AlarmItem restored = AlarmItem.fromJson(item.toJson());
        assertTrue(restored.autoDeleteAfterCompletion);
        assertEquals(item.pendingSnoozeAtMillis, restored.pendingSnoozeAtMillis);
        org.json.JSONObject legacy = item.toJson();
        legacy.remove("autoDeleteAfterCompletion"); legacy.remove("pendingSnoozeAtMillis");
        restored = AlarmItem.fromJson(legacy);
        assertFalse(restored.autoDeleteAfterCompletion);
        assertEquals(0L, restored.pendingSnoozeAtMillis);
    }
}
