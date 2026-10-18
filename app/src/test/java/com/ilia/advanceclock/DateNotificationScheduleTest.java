package com.ilia.advanceclock;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.Test;
import static org.junit.Assert.*;

public final class DateNotificationScheduleTest {
    @Test public void iranMidnightUsesLocalDateRatherThanUtc() {
        ZoneId zone = ZoneId.of("Asia/Tehran");
        long now = Instant.parse("2026-10-18T20:29:59Z").toEpochMilli();
        assertEquals(Instant.parse("2026-10-18T20:30:00Z").toEpochMilli(),
                DateNotificationSchedule.nextMidnight(now, zone));
    }

    @Test public void exactlyMidnightSchedulesTheNextDay() {
        ZoneId zone = ZoneId.of("Asia/Tehran");
        long midnight = ZonedDateTime.of(2026, 10, 19, 0, 0, 0, 0, zone).toInstant().toEpochMilli();
        assertEquals(midnight + 24 * 3600_000L, DateNotificationSchedule.nextMidnight(midnight, zone));
    }

    @Test public void springAndAutumnClockChangesDoNotAssumeTwentyFourHours() {
        ZoneId zone = ZoneId.of("America/Toronto");
        long spring = ZonedDateTime.of(2026, 3, 8, 0, 0, 0, 0, zone).toInstant().toEpochMilli();
        long autumn = ZonedDateTime.of(2026, 11, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli();
        assertEquals(23 * 3600_000L, DateNotificationSchedule.nextMidnight(spring, zone) - spring);
        assertEquals(25 * 3600_000L, DateNotificationSchedule.nextMidnight(autumn, zone) - autumn);
    }

    @Test public void yearAndLeapDayBoundariesAdvanceCorrectly() {
        ZoneId zone = ZoneId.of("UTC");
        assertEquals(Instant.parse("2027-01-01T00:00:00Z").toEpochMilli(),
                DateNotificationSchedule.nextMidnight(Instant.parse("2026-12-31T23:59:59Z").toEpochMilli(), zone));
        assertEquals(Instant.parse("2028-03-01T00:00:00Z").toEpochMilli(),
                DateNotificationSchedule.nextMidnight(Instant.parse("2028-02-29T12:00:00Z").toEpochMilli(), zone));
    }

    @Test public void timeZoneChangeRecomputesTheNextLocalMidnight() {
        long now = Instant.parse("2026-10-18T21:00:00Z").toEpochMilli();
        assertEquals(Instant.parse("2026-10-19T20:30:00Z").toEpochMilli(),
                DateNotificationSchedule.nextMidnight(now, ZoneId.of("Asia/Tehran")));
        assertEquals(Instant.parse("2026-10-19T04:00:00Z").toEpochMilli(),
                DateNotificationSchedule.nextMidnight(now, ZoneId.of("America/New_York")));
    }
}
