package com.ilia.advanceclock;

import java.time.Instant;
import java.time.ZoneId;

/** The next local date boundary, including DST and time-zone date changes. */
final class DateNotificationSchedule {
    private DateNotificationSchedule() {}

    static long nextMidnight(long now, ZoneId zone) {
        return Instant.ofEpochMilli(now).atZone(zone).toLocalDate().plusDays(1)
                .atStartOfDay(zone).toInstant().toEpochMilli();
    }
}
