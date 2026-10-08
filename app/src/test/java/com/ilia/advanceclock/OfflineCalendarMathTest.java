package com.ilia.advanceclock;

import com.ibm.icu.util.Calendar;
import com.ibm.icu.util.IslamicCalendar;
import com.ibm.icu.util.PersianCalendar;
import com.ibm.icu.util.TimeZone;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

public class OfflineCalendarMathTest {
    private static final long DAY_MILLIS = 86_400_000L;

    @Test public void everyDayFrom1900Through2170RoundTripsInAllCalendars() {
        for (int mode = 0; mode <= 1; mode++) {
            for (int type = 0; type <= 2; type++) {
                for (long epoch = OfflineCalendarMath.MIN_UI_DAY;
                     epoch <= OfflineCalendarMath.MAX_UI_DAY; epoch++) {
                    OfflineCalendarMath.DateParts date = OfflineCalendarMath.fromEpochDay(type, epoch, mode);
                    assertEquals(epoch, OfflineCalendarMath.toEpochDay(type,
                            date.year, date.month, date.day, mode));
                    assertTrue(date.day >= 1 && date.day <= OfflineCalendarMath.monthLength(
                            type, date.year, date.month, mode));
                }
            }
        }
    }

    @Test public void pinnedSaudiAndPersianDatesMatchIndependentIcu78ForEntireRange() {
        IslamicCalendar saudi = new IslamicCalendar(TimeZone.getTimeZone("UTC"));
        saudi.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        PersianCalendar persian = new PersianCalendar(TimeZone.getTimeZone("UTC"));
        for (long day = OfflineCalendarMath.MIN_UI_DAY; day <= OfflineCalendarMath.MAX_UI_DAY; day++) {
            saudi.setTimeInMillis(day * DAY_MILLIS + DAY_MILLIS / 2);
            persian.setTimeInMillis(day * DAY_MILLIS + DAY_MILLIS / 2);
            assertFields(saudi, OfflineCalendarMath.fromEpochDay(2, day, 1));
            assertFields(persian, OfflineCalendarMath.fromEpochDay(0, day, 0));
        }
    }

    @Test public void officialIran1405DatesAndReligiousHolidaysMatchPublishedCalendar() {
        // Tehran University Calendar Centre, Calendar-1405.pdf (see provenance document).
        String[][] dates = {
                {"2026-03-21", "1447", "10", "1", "62"},
                {"2026-03-22", "1447", "10", "2", "63"},
                {"2026-04-14", "1447", "10", "25", "64"},
                {"2026-05-27", "1447", "12", "10", "65"},
                {"2026-06-04", "1447", "12", "18", "66"},
                {"2026-06-24", "1448", "1", "9", "67"},
                {"2026-06-25", "1448", "1", "10", "68"},
                {"2026-08-04", "1448", "2", "20", "69"},
                {"2026-08-12", "1448", "2", "28", "70"},
                {"2026-08-13", "1448", "2", "29", "71"},
                {"2026-08-21", "1448", "3", "8", "72"},
                {"2026-08-30", "1448", "3", "17", "73"},
                {"2026-10-08", "1448", "4", "26", "0"},
                {"2026-11-13", "1448", "6", "3", "74"},
                {"2026-12-23", "1448", "7", "13", "58"},
                {"2027-01-06", "1448", "7", "27", "59"},
                {"2027-01-24", "1448", "8", "15", "60"},
                {"2027-02-28", "1448", "9", "21", "61"},
                {"2027-03-10", "1448", "10", "1", "62"},
                {"2027-03-20", "1448", "10", "11", "0"}
        };
        for (String[] fixture : dates) {
            long epoch = LocalDate.parse(fixture[0]).toEpochDay();
            OfflineCalendarMath.DateParts date = OfflineCalendarMath.fromEpochDay(2, epoch, 0);
            assertEquals(fixture[0], Integer.parseInt(fixture[1]), date.year);
            assertEquals(fixture[0], Integer.parseInt(fixture[2]), date.month + 1);
            assertEquals(fixture[0], Integer.parseInt(fixture[3]), date.day);
            int key = Integer.parseInt(fixture[4]);
            if (key == 0) continue;
            int length = OfflineCalendarMath.monthLength(2, date.year, date.month, 0);
            for (int source : new int[]{LunarEventRules.IRAN, LunarEventRules.ARAB}) {
                assertTrue(fixture[0], contains(LunarEventRules.forDate(
                        date.month + 1, date.day, length, source),
                        source == LunarEventRules.ARAB && key == 59 ? 77 : key));
            }
        }
        // The reported one-day difference is a genuine difference between references.
        OfflineCalendarMath.DateParts saudi = OfflineCalendarMath.fromEpochDay(2,
                LocalDate.of(2026, 10, 8).toEpochDay(), 1);
        assertEquals(27, saudi.day);
    }

    @Test public void iranCoverageIsExplicitAndCenturyYearBoundsStayWithinData() {
        assertTrue(OfflineCalendarMath.hasIranReference(LocalDate.of(2027, 3, 20).toEpochDay()));
        assertFalse(OfflineCalendarMath.hasIranReference(LocalDate.of(2027, 3, 21).toEpochDay()));
        for (int mode = 0; mode <= 1; mode++) for (int type = 0; type <= 2; type++) {
            int first = OfflineCalendarMath.minimumUiYear(type, mode);
            int last = OfflineCalendarMath.maximumUiYear(type, mode);
            assertTrue(last - first >= 100);
            assertTrue(OfflineCalendarMath.toEpochDay(type, first, 0, 1, mode)
                    >= OfflineCalendarMath.MIN_UI_DAY);
            assertTrue(OfflineCalendarMath.toEpochDay(type, last, 11,
                    OfflineCalendarMath.monthLength(type, last, 11, mode), mode)
                    <= OfflineCalendarMath.MAX_UI_DAY);
        }
    }

    @Test public void invalidDatesAndOutOfTableLunarYearsAreRejected() {
        assertInvalid(() -> OfflineCalendarMath.toEpochDay(1, 2026, 5, 31, 0));
        assertInvalid(() -> OfflineCalendarMath.toEpochDay(1, 2100, 1, 29, 0));
        assertInvalid(() -> OfflineCalendarMath.toEpochDay(0, 1502, 11, 30, 0));
        assertInvalid(() -> OfflineCalendarMath.toEpochDay(2, 1299, 0, 1, 0));
        assertInvalid(() -> OfflineCalendarMath.toEpochDay(2, 1601, 0, 1, 1));
        assertEquals(30, OfflineCalendarMath.monthLength(0, 1503, 11, 0));
        assertEquals(LocalDate.of(2124, 3, 20).toEpochDay(),
                OfflineCalendarMath.toEpochDay(0, 1503, 0, 1, 0));
        assertEquals(29, OfflineCalendarMath.monthLength(1, 2000, 1, 0));
        assertEquals(28, OfflineCalendarMath.monthLength(1, 2100, 1, 0));
    }

    @Test public void allReligiousRulesRecurForMoreThanOneHundredLunarYears() {
        for (int mode = 0; mode <= 1; mode++) for (int year = 1440; year <= 1590; year++) {
            for (LunarEventRules.Rule rule : LunarEventRules.RULES) {
                int length = OfflineCalendarMath.monthLength(2, year, rule.month - 1, mode);
                int day = rule.day == -1 ? length : rule.day;
                long epoch = OfflineCalendarMath.toEpochDay(2, year, rule.month - 1, day, mode);
                OfflineCalendarMath.DateParts date = OfflineCalendarMath.fromEpochDay(2, epoch, mode);
                for (int source : new int[]{LunarEventRules.IRAN, LunarEventRules.ARAB}) {
                    if ((rule.sources & source) == 0) continue;
                    assertTrue(contains(LunarEventRules.forDate(date.month + 1, date.day, length, source),
                            rule.titleKey));
                }
            }
        }
        assertTrue(contains(LunarEventRules.forDate(2, 29, 29, 1), 71));
        assertFalse(contains(LunarEventRules.forDate(2, 29, 30, 1), 71));
        assertTrue(contains(LunarEventRules.forDate(2, 30, 30, 2), 71));
        assertTrue(contains(LunarEventRules.forDate(11, 29, 29, 2), 111));
        assertTrue(contains(LunarEventRules.forDate(3, 12, 30, 2), 76));
        assertTrue(contains(LunarEventRules.forDate(3, 17, 30, 1), 73));
    }

    private static void assertFields(Calendar calendar, OfflineCalendarMath.DateParts date) {
        assertEquals(calendar.get(Calendar.YEAR), date.year);
        assertEquals(calendar.get(Calendar.MONTH), date.month);
        assertEquals(calendar.get(Calendar.DAY_OF_MONTH), date.day);
    }

    private static boolean contains(List<LunarEventRules.Rule> events, int key) {
        for (LunarEventRules.Rule rule : events) if (rule.titleKey == key) return true;
        return false;
    }

    private static void assertInvalid(Runnable conversion) {
        try { conversion.run(); fail("Invalid date was accepted"); }
        catch (IllegalArgumentException expected) { /* strict validation */ }
    }
}
