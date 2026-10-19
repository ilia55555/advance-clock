package com.ilia.advanceclock;

import java.time.Instant;
import java.util.TimeZone;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AlarmTimePolicyTest {
    private TimeZone original;
    private static long at(String text) { return Instant.parse(text).toEpochMilli(); }
    private long next(String selected, boolean date, String now) {
        return AlarmTimePolicy.next(at(selected), date, RecurrenceUtils.NONE, 1, "[]", at(now));
    }
    @Before public void utc() { original = TimeZone.getDefault(); TimeZone.setDefault(TimeZone.getTimeZone("UTC")); }
    @After public void restoreZone() { TimeZone.setDefault(original); }

    @Test public void timeOnlyIgnoresBothPastAndFutureCalendarDates() {
        long expected = at("2026-10-19T11:30:00Z");
        assertEquals(expected, next("2020-01-01T11:30:00Z", false, "2026-10-19T10:00:00Z"));
        assertEquals(expected, next("2030-01-01T11:30:00Z", false, "2026-10-19T10:00:00Z"));
    }
    @Test public void passedOrEqualTimeRingsTomorrowRatherThanImmediately() {
        assertEquals(at("2026-10-20T09:00:00Z"), next("2020-01-01T09:00:00Z", false, "2026-10-19T10:00:00Z"));
        assertEquals(at("2026-10-20T10:00:00Z"), next("2026-10-19T10:00:00Z", false, "2026-10-19T10:00:00Z"));
    }
    @Test public void explicitPastOneShotCannotBeReenabled() {
        assertEquals(-1L, next("2026-10-18T11:00:00Z", true, "2026-10-19T10:00:00Z"));
        assertEquals(at("2026-10-20T11:00:00Z"), next("2026-10-20T11:00:00Z", true, "2026-10-19T10:00:00Z"));
    }
    @Test public void pastDatesWithFutureRecurrencesRemainEligible() {
        long now = at("2026-10-19T10:00:00Z");
        long old = at("2026-10-18T09:00:00Z");
        assertEquals(at("2026-10-20T09:00:00Z"), AlarmTimePolicy.next(old, true, RecurrenceUtils.DAILY, 1, "[]", now));
        String futureDates = "[" + at("2026-10-25T09:00:00Z") + "]";
        assertEquals(at("2026-10-25T09:00:00Z"), AlarmTimePolicy.next(old, true, RecurrenceUtils.CUSTOM_DATES, 1, futureDates, now));
        assertEquals(-1L, AlarmTimePolicy.next(old, true, RecurrenceUtils.CUSTOM_DATES, 1, "[1]", now));
    }
    @Test public void timeOnlyWeekdaysHonorTheChosenWeekdays() {
        long now = at("2026-10-19T10:00:00Z"); // Monday
        assertEquals(at("2026-10-21T09:00:00Z"), AlarmTimePolicy.next(at("2020-01-01T09:00:00Z"), false,
                RecurrenceUtils.WEEKDAYS, 1, "{\"weekdays\":[4],\"permanent\":true}", now));
    }
    @Test public void localTimeAndDaylightSavingUseCalendarDays() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Toronto"));
        assertEquals(at("2026-03-08T13:00:00Z"), next("2020-01-01T14:00:00Z", false, "2026-03-07T15:00:00Z"));
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"));
        assertEquals(at("2026-10-19T06:30:00Z"), next("2020-01-01T06:30:00Z", false, "2026-10-19T05:00:00Z"));
    }
    @Test public void dateChoicePersistsAndLegacyRecordsKeepTheirExplicitDates() throws Exception {
        AlarmItem alarm = new AlarmItem(1, "", 1234, 0, true);
        assertFalse(alarm.applyDate);
        assertFalse(AlarmItem.fromJson(alarm.toJson()).applyDate);
        JSONObject legacy = alarm.toJson(); legacy.remove("applyDate");
        assertTrue(AlarmItem.fromJson(legacy).applyDate);
        alarm.applyDate = true;
        assertTrue(AlarmItem.fromJson(alarm.toJson()).applyDate);
        NoForgetItem note = new NoForgetItem(1, "", "", "[]", 2, true, 1234, true, 1);
        assertTrue(note.applyDate);
        note.applyDate = false;
        assertFalse(NoForgetItem.fromJson(note.toJson()).applyDate);
        legacy = note.toJson(); legacy.remove("applyDate");
        assertTrue(NoForgetItem.fromJson(legacy).applyDate);
    }
    @Test public void automaticOneShotRecurrenceNeverRearmsOnTheNextDay() {
        assertEquals(-1L, RecurrenceUtils.next(at("2026-10-19T09:00:00Z"), RecurrenceUtils.NONE, 1,
                "[]", at("2026-10-19T10:00:00Z")));
    }
}
