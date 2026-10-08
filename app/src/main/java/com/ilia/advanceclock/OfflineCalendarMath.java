package com.ilia.advanceclock;

import java.time.LocalDate;
import java.util.Arrays;

/** Date-only arithmetic, independent of Android ICU, locale, network and time zone. */
final class OfflineCalendarMath {
    static final int PERSIAN = 0, GREGORIAN = 1, HIJRI = 2;
    static final int IRAN = 0, UMALQURA = 1;
    static final long MIN_UI_DAY = LocalDate.of(1900, 1, 1).toEpochDay();
    static final long MAX_UI_DAY = LocalDate.of(2171, 1, 1).toEpochDay() - 1;
    private static final long PERSIAN_EPOCH_DAY = 1948320L - 2440588L;
    private static final int[] PERSIAN_MONTH_OFFSETS =
            {0, 31, 62, 93, 124, 155, 186, 216, 246, 276, 306, 336};
    // ICU's corrected 33-year Persian algorithm, including the 1502/1503 correction
    // needed in 2123/2124. Unicode License v3; provenance in docs/CALENDAR_ACCURACY.md.
    private static final int[] NON_LEAP_YEARS = {
            1502, 1601, 1634, 1667, 1700, 1733, 1766, 1799, 1832, 1865, 1898,
            1931, 1964, 1997, 2030, 2059, 2063, 2096, 2129, 2158, 2162, 2191,
            2195, 2224, 2228, 2257, 2261, 2290, 2294, 2323, 2327, 2356, 2360,
            2389, 2393, 2422, 2426, 2455, 2459, 2488, 2492, 2521, 2525, 2554,
            2558, 2587, 2591, 2620, 2624, 2653, 2657, 2686, 2690, 2719, 2723,
            2748, 2752, 2756, 2781, 2785, 2789, 2818, 2822, 2847, 2851, 2855,
            2880, 2884, 2888, 2913, 2917, 2921, 2946, 2950, 2954, 2979, 2983, 2987
    };
    private static final int[] IRAN_MONTH_STARTS = iranMonthStarts();

    private OfflineCalendarMath() {}

    static final class DateParts {
        final int year, month, day; // month is zero-based throughout the app
        DateParts(int year, int month, int day) {
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    private static int[] iranMonthStarts() {
        int[] starts = OfflineCalendarData.UMALQURA_MONTH_STARTS.clone();
        System.arraycopy(OfflineCalendarData.IRAN_REFERENCE_MONTH_STARTS, 0,
                starts, 0, OfflineCalendarData.IRAN_REFERENCE_MONTH_STARTS.length);
        validateMonthStarts(starts);
        validateMonthStarts(OfflineCalendarData.UMALQURA_MONTH_STARTS);
        return starts;
    }

    private static void validateMonthStarts(int[] starts) {
        for (int i = 1; i < starts.length; i++) {
            int length = starts[i] - starts[i - 1];
            if (length != 29 && length != 30) {
                throw new IllegalStateException("Invalid bundled lunar month at " + i);
            }
        }
    }

    private static int[] hijriStarts(int mode) {
        if (mode != IRAN && mode != UMALQURA) throw new IllegalArgumentException("Lunar reference");
        return mode == IRAN ? IRAN_MONTH_STARTS : OfflineCalendarData.UMALQURA_MONTH_STARTS;
    }

    private static int hijriIndex(int year, int month) {
        if (year < OfflineCalendarData.FIRST_HIJRI_YEAR
                || year > OfflineCalendarData.LAST_HIJRI_YEAR || month < 0 || month > 11) {
            throw new IllegalArgumentException("Lunar date outside 1300–1600 AH");
        }
        return (year - OfflineCalendarData.FIRST_HIJRI_YEAR) * 12 + month;
    }

    static long monthStart(int type, int year, int month, int mode) {
        if (month < 0 || month > 11) throw new IllegalArgumentException("Month");
        switch (type) {
            case GREGORIAN: return LocalDate.of(year, month + 1, 1).toEpochDay();
            case HIJRI: return hijriStarts(mode)[hijriIndex(year, month)];
            case PERSIAN: return persianYearStart(year) + PERSIAN_MONTH_OFFSETS[month];
            default: throw new IllegalArgumentException("Calendar type");
        }
    }

    static int monthLength(int type, int year, int month, int mode) {
        if (month < 0 || month > 11) throw new IllegalArgumentException("Month");
        switch (type) {
            case GREGORIAN: return LocalDate.of(year, month + 1, 1).lengthOfMonth();
            case HIJRI:
                int index = hijriIndex(year, month);
                int[] starts = hijriStarts(mode);
                return starts[index + 1] - starts[index];
            case PERSIAN:
                return month < 6 ? 31 : month < 11 ? 30 : persianLeapYear(year) ? 30 : 29;
            default: throw new IllegalArgumentException("Calendar type");
        }
    }

    static long toEpochDay(int type, int year, int month, int day, int mode) {
        int length = monthLength(type, year, month, mode);
        if (day < 1 || day > length) throw new IllegalArgumentException("Invalid day of month");
        return monthStart(type, year, month, mode) + day - 1;
    }

    static DateParts fromEpochDay(int type, long epochDay, int mode) {
        switch (type) {
            case GREGORIAN:
                LocalDate g = LocalDate.ofEpochDay(epochDay);
                return new DateParts(g.getYear(), g.getMonthValue() - 1, g.getDayOfMonth());
            case HIJRI:
                int[] starts = hijriStarts(mode);
                if (epochDay < starts[0] || epochDay >= starts[starts.length - 1]) {
                    throw new IllegalArgumentException("Date outside bundled lunar table");
                }
                int index = Arrays.binarySearch(starts, (int) epochDay);
                if (index < 0) index = -index - 2;
                return new DateParts(OfflineCalendarData.FIRST_HIJRI_YEAR + index / 12,
                        index % 12, (int) (epochDay - starts[index]) + 1);
            case PERSIAN:
                int year = (int) Math.floorDiv(epochDay - PERSIAN_EPOCH_DAY, 366) + 1;
                while (epochDay < persianYearStart(year)) year--;
                while (epochDay >= persianYearStart(year + 1)) year++;
                int doy = (int) (epochDay - persianYearStart(year));
                int month = doy < 186 ? doy / 31 : (doy - 6) / 30;
                return new DateParts(year, month, doy - PERSIAN_MONTH_OFFSETS[month] + 1);
            default: throw new IllegalArgumentException("Calendar type");
        }
    }

    static long persianYearStart(int year) {
        return PERSIAN_EPOCH_DAY + 365L * (year - 1)
                + Math.floorDiv(8L * year + 21, 33)
                - (Arrays.binarySearch(NON_LEAP_YEARS, year - 1) >= 0 ? 1 : 0);
    }

    static boolean persianLeapYear(int year) {
        if (Arrays.binarySearch(NON_LEAP_YEARS, year) >= 0) return false;
        if (Arrays.binarySearch(NON_LEAP_YEARS, year - 1) >= 0) return true;
        return Math.floorMod(25L * year + 11, 33) < 8;
    }

    static int minimumUiYear(int type, int mode) {
        DateParts date = fromEpochDay(type, MIN_UI_DAY, mode);
        return date.month == 0 && date.day == 1 ? date.year : date.year + 1;
    }

    static int maximumUiYear(int type, int mode) {
        DateParts date = fromEpochDay(type, MAX_UI_DAY, mode);
        return date.month == 11 && date.day == monthLength(type, date.year, 11, mode)
                ? date.year : date.year - 1;
    }

    static boolean hasIranReference(long epochDay) {
        return epochDay >= IRAN_MONTH_STARTS[0]
                && epochDay < OfflineCalendarData.IRAN_REFERENCE_END_EXCLUSIVE;
    }
}
