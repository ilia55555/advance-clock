package com.ilia.advanceclock;

import android.icu.util.Calendar;
import android.icu.util.TimeZone;
import android.icu.util.ULocale;

/** ICU supplies time-zone/week arithmetic; date conversion uses bundled app data. */
final class PinnedCalendar extends Calendar {
    private static final long serialVersionUID = 1L;
    private final int calendarType;
    private final int lunarReference;

    PinnedCalendar(int type, int reference) {
        super(TimeZone.getTimeZone(java.util.TimeZone.getDefault().getID()),
                new ULocale(type == OfflineCalendarMath.PERSIAN ? "fa_IR" : "ar_SA"));
        calendarType = type;
        lunarReference = reference;
        setLenient(false);
        setTimeInMillis(System.currentTimeMillis());
    }

    @Override public String getType() {
        return calendarType == OfflineCalendarMath.PERSIAN ? "persian" : "islamic-umalqura";
    }

    @Override protected int handleGetExtendedYear() {
        return newerField(EXTENDED_YEAR, YEAR) == EXTENDED_YEAR
                ? internalGet(EXTENDED_YEAR, 1400) : internalGet(YEAR, 1400);
    }

    @Override protected int handleGetLimit(int field, int limitType) {
        boolean lunar = calendarType == OfflineCalendarMath.HIJRI;
        switch (field) {
            case ERA: return 0;
            case YEAR: case EXTENDED_YEAR: case YEAR_WOY:
                return limitType < 2 ? (lunar ? 1300 : 1) : (lunar ? 1600 : 3000);
            case MONTH: return limitType < 2 ? 0 : 11;
            case WEEK_OF_YEAR: return limitType < 2 ? 1 : lunar ? 52 : 53;
            case DAY_OF_MONTH: return limitType < 2 ? 1 : limitType == 2 ? 29 : lunar ? 30 : 31;
            case DAY_OF_YEAR: return limitType < 2 ? 1 : lunar ? 355 : limitType == 2 ? 365 : 366;
            case DAY_OF_WEEK_IN_MONTH: return limitType < 2 ? -1 : 5;
            default: throw new IllegalArgumentException("Unsupported calendar limit: " + field);
        }
    }

    @Override protected int handleComputeMonthStart(int year, int month, boolean useMonth) {
        year += Math.floorDiv(month, 12);
        month = Math.floorMod(month, 12);
        return (int) (OfflineCalendarMath.monthStart(calendarType, year, month, lunarReference)
                + 2440588L - 1);
    }

    @Override protected int handleGetMonthLength(int year, int month) {
        year += Math.floorDiv(month, 12);
        month = Math.floorMod(month, 12);
        return OfflineCalendarMath.monthLength(calendarType, year, month, lunarReference);
    }

    @Override protected int handleGetYearLength(int year) {
        int length = 0;
        for (int month = 0; month < 12; month++) length += handleGetMonthLength(year, month);
        return length;
    }

    @Override protected void handleComputeFields(int julianDay) {
        long epochDay = julianDay - 2440588L;
        OfflineCalendarMath.DateParts date =
                OfflineCalendarMath.fromEpochDay(calendarType, epochDay, lunarReference);
        internalSet(ERA, 0);
        internalSet(YEAR, date.year);
        internalSet(EXTENDED_YEAR, date.year);
        internalSet(MONTH, date.month);
        internalSet(DAY_OF_MONTH, date.day);
        internalSet(DAY_OF_YEAR, (int) (epochDay
                - OfflineCalendarMath.monthStart(calendarType, date.year, 0, lunarReference)) + 1);
    }
}
