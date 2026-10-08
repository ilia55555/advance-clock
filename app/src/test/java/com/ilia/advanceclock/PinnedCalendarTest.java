package com.ilia.advanceclock;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.TimeZone;
import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.Assert.*;

public class PinnedCalendarTest {
    @Test public void productionAdapterConvertsAndSetsEveryDayAcross100FutureYears() {
        for (int mode = 0; mode <= 1; mode++) for (int type : new int[]{0, 2}) {
            JvmPinnedCalendar calendar = new JvmPinnedCalendar(type, mode);
            calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
            long last = LocalDate.of(2130, 1, 1).toEpochDay();
            for (long epoch = LocalDate.of(2026, 1, 1).toEpochDay(); epoch < last; epoch++) {
                long millis = epoch * 86_400_000L + 43_200_000L;
                calendar.setTimeInMillis(millis);
                OfflineCalendarMath.DateParts date = OfflineCalendarMath.fromEpochDay(type, epoch, mode);
                assertEquals(date.year, calendar.get(Calendar.YEAR));
                assertEquals(date.month, calendar.get(Calendar.MONTH));
                assertEquals(date.day, calendar.get(Calendar.DAY_OF_MONTH));
                assertEquals(OfflineCalendarMath.monthLength(type, date.year, date.month, mode),
                        calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
                calendar.clear();
                calendar.set(date.year, date.month, date.day, 12, 0, 0);
                assertEquals(millis, calendar.getTimeInMillis());
            }
        }
    }

    @Test public void dateFieldsRespectDeviceZonesAndDaylightSavingBoundaries() {
        String[] days = {"2026-03-08", "2026-03-21", "2026-10-08", "2026-11-01", "2124-03-20"};
        for (String zone : new String[]{"UTC", "Asia/Tehran", "Asia/Riyadh", "America/New_York"}) {
            for (int type : new int[]{0, 2}) for (String day : days) {
                JvmPinnedCalendar calendar = new JvmPinnedCalendar(type, 0);
                calendar.setTimeZone(TimeZone.getTimeZone(zone));
                LocalDate local = LocalDate.parse(day);
                long millis = local.atTime(12, 0).atZone(ZoneId.of(zone)).toInstant().toEpochMilli();
                calendar.setTimeInMillis(millis);
                OfflineCalendarMath.DateParts expected = OfflineCalendarMath.fromEpochDay(type, local.toEpochDay(), 0);
                assertEquals(expected.day, calendar.get(Calendar.DAY_OF_MONTH));
                calendar.clear();
                calendar.set(expected.year, expected.month, expected.day, 12, 0, 0);
                assertEquals(millis, calendar.getTimeInMillis());
                calendar.add(Calendar.DATE, 1);
                OfflineCalendarMath.DateParts tomorrow = OfflineCalendarMath.fromEpochDay(type, local.toEpochDay() + 1, 0);
                assertEquals(tomorrow.day, calendar.get(Calendar.DAY_OF_MONTH));
            }
        }
    }

    @Test public void changingMonthClampsDayAndLunarReferencesRemainIsolated() {
        JvmPinnedCalendar calendar = new JvmPinnedCalendar(0, 0);
        calendar.clear();
        calendar.set(1405, 5, 31, 12, 0, 0);
        calendar.add(Calendar.MONTH, 1);
        assertEquals(6, calendar.get(Calendar.MONTH));
        assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH));
        long instant = LocalDate.of(2026, 10, 8).atTime(12, 0).atZone(ZoneId.of("UTC"))
                .toInstant().toEpochMilli();
        JvmPinnedCalendar iran = new JvmPinnedCalendar(2, 0);
        JvmPinnedCalendar saudi = new JvmPinnedCalendar(2, 1);
        iran.setTimeZone(TimeZone.getTimeZone("UTC"));
        saudi.setTimeZone(TimeZone.getTimeZone("UTC"));
        iran.setTimeInMillis(instant);
        saudi.setTimeInMillis(instant);
        assertEquals(26, iran.get(Calendar.DAY_OF_MONTH));
        assertEquals(27, saudi.get(Calendar.DAY_OF_MONTH));
    }
}
