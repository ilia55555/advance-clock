package com.ilia.advanceclock;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class CalendarEventRepository {
    public static final String DATASET_VERSION = "2026-10-08-lunar-rules";
    public static final int SUPPORTED_FROM_GREGORIAN_YEAR = 1900;
    public static final int SUPPORTED_THROUGH_GREGORIAN_YEAR = 2170;
    public static final class Event {
        public final String title;
        public final boolean holiday;
        public final String sourceId;
        public final int validFromYear;
        public final int validThroughYear;
        public final String verifiedOn;

        Event(String title, boolean holiday) {
            this(title, holiday, "", 0, 0, "");
        }

        Event(
                String title,
                boolean holiday,
                String sourceId,
                int validFromYear,
                int validThroughYear,
                String verifiedOn) {
            this.title = title;
            this.holiday = holiday;
            this.sourceId = sourceId;
            this.validFromYear = validFromYear;
            this.validThroughYear = validThroughYear;
            this.verifiedOn = verifiedOn;
        }
    }

    private CalendarEventRepository() {}

    public static boolean sourceEnabled(
            Context context, int primaryCalendarType, int sourceCalendarType) {
        return sourceCalendarType == primaryCalendarType
                || AppSettings.additionalCalendarEventsEnabled(
                context, sourceCalendarType);
    }

    public static String sourceTitle(int calendarType) {
        switch (calendarType) {
            case CalendarUtils.GREGORIAN:
                return AppString.get(R.string.calendar_event_text_004);
            case CalendarUtils.HIJRI:
                return AppString.get(R.string.calendar_event_text_005);
            case CalendarUtils.PERSIAN:
            default:
                return AppString.get(R.string.calendar_event_text_006);
        }
    }

    public static String holidayLabel(int calendarType) {
        if (calendarType == CalendarUtils.PERSIAN) return AppString.get(R.string.calendar_event_text_007);
        if (calendarType == CalendarUtils.HIJRI) return AppString.get(R.string.calendar_event_text_008);
        return AppString.get(R.string.calendar_event_text_009);
    }

    public static List<Event> eventsFor(Context context, long millis, int calendarType) {
        ArrayList<Event> out = new ArrayList<>();
        if (calendarType == CalendarUtils.PERSIAN) {
            addIranianEvents(out, millis);
        } else if (calendarType == CalendarUtils.HIJRI) {
            addArabHijriEvents(out, millis);
        } else {
            addInternationalGregorianEvents(out, millis);
        }
        return attachMetadata(out, calendarType);
    }

    public static boolean isOfficialHoliday(Context context, long millis, int calendarType) {
        for (Event event : eventsFor(context, millis, calendarType)) {
            if (event.holiday) return true;
        }
        return false;
    }

    public static boolean isOfficialHolidayInEnabledSources(
            Context context, long millis, int primaryCalendarType) {
        for (int source = CalendarUtils.PERSIAN;
             source <= CalendarUtils.HIJRI;
             source++) {
            if (sourceEnabled(context, primaryCalendarType, source)
                    && isOfficialHoliday(context, millis, source)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isWeekend(long millis, int calendarType) {
        android.icu.util.Calendar gregorian =
                CalendarUtils.fromMillis(CalendarUtils.GREGORIAN, millis);
        int dayOfWeek = gregorian.get(android.icu.util.Calendar.DAY_OF_WEEK);

        if (calendarType == CalendarUtils.GREGORIAN) {
            return dayOfWeek == android.icu.util.Calendar.SUNDAY;
        }
        return dayOfWeek == android.icu.util.Calendar.FRIDAY;
    }

    public static String datasetNotice(
            long millis,
            boolean persianEnabled,
            boolean hijriEnabled,
            boolean gregorianEnabled) {
        if (CalendarUtils.hijriReference() == CalendarUtils.HIJRI_UMALQURA) {
            return AppString.get(R.string.hijri_reference_umalqura);
        }
        return AppString.get(CalendarUtils.hasIranReference(millis)
                ? R.string.hijri_date_reference : R.string.hijri_date_calculated);
    }

    private static List<Event> attachMetadata(List<Event> events, int calendarType) {
        String source = AppString.get(calendarType == CalendarUtils.PERSIAN
                ? R.string.calendar_event_text_001
                : calendarType == CalendarUtils.HIJRI
                ? R.string.calendar_event_text_003 : R.string.calendar_event_text_002);
        ArrayList<Event> enriched = new ArrayList<>(events.size());
        for (Event event : events) {
            enriched.add(new Event(
                    event.title,
                    event.holiday,
                    source,
                    SUPPORTED_FROM_GREGORIAN_YEAR,
                    SUPPORTED_THROUGH_GREGORIAN_YEAR,
                    DATASET_VERSION));
        }
        return enriched;
    }

    private static void addIranianEvents(List<Event> out, long millis) {
        android.icu.util.Calendar p = CalendarUtils.fromMillis(CalendarUtils.PERSIAN, millis);
        int pm = p.get(android.icu.util.Calendar.MONTH) + 1;
        int pd = p.get(android.icu.util.Calendar.DAY_OF_MONTH);

        add(out, pm, pd, 1, 1, AppString.get(R.string.calendar_event_text_013), true);
        add(out, pm, pd, 1, 2, AppString.get(R.string.calendar_event_text_014), true);
        add(out, pm, pd, 1, 3, AppString.get(R.string.calendar_event_text_014), true);
        add(out, pm, pd, 1, 4, AppString.get(R.string.calendar_event_text_014), true);
        add(out, pm, pd, 1, 12, AppString.get(R.string.calendar_event_text_015), true);
        add(out, pm, pd, 1, 13, AppString.get(R.string.calendar_event_text_016), true);
        add(out, pm, pd, 3, 14, AppString.get(R.string.calendar_event_text_017), true);
        add(out, pm, pd, 3, 15, AppString.get(R.string.calendar_event_text_018), true);
        add(out, pm, pd, 11, 22, AppString.get(R.string.calendar_event_text_019), true);
        add(out, pm, pd, 12, 29, AppString.get(R.string.calendar_event_text_020), true);
        add(out, pm, pd, 9, 30, AppString.get(R.string.calendar_event_text_021), false);
        add(out, pm, pd, 1, 6, AppString.get(R.string.calendar_event_text_022), false);
        add(out, pm, pd, 1, 7, AppString.get(R.string.calendar_event_text_023), false);
        add(out, pm, pd, 1, 20, AppString.get(R.string.calendar_event_text_024), false);
        add(out, pm, pd, 1, 25, AppString.get(R.string.calendar_event_text_025), false);
        add(out, pm, pd, 2, 1, AppString.get(R.string.calendar_event_text_026), false);
        add(out, pm, pd, 2, 10, AppString.get(R.string.calendar_event_text_027), false);
        add(out, pm, pd, 2, 25, AppString.get(R.string.calendar_event_text_028), false);
        add(out, pm, pd, 2, 28, AppString.get(R.string.calendar_event_text_029), false);
        add(out, pm, pd, 3, 1, AppString.get(R.string.calendar_event_text_030), false);
        add(out, pm, pd, 3, 3, AppString.get(R.string.calendar_event_text_031), false);
        add(out, pm, pd, 3, 20, AppString.get(R.string.calendar_event_text_032), false);
        add(out, pm, pd, 4, 1, AppString.get(R.string.calendar_event_text_033), false);
        add(out, pm, pd, 4, 7, AppString.get(R.string.calendar_event_text_034), false);
        add(out, pm, pd, 4, 10, AppString.get(R.string.calendar_event_text_035), false);
        add(out, pm, pd, 4, 14, AppString.get(R.string.calendar_event_text_036), false);
        add(out, pm, pd, 5, 8, AppString.get(R.string.calendar_event_text_037), false);
        add(out, pm, pd, 5, 17, AppString.get(R.string.calendar_event_text_038), false);
        add(out, pm, pd, 5, 28, AppString.get(R.string.calendar_event_text_039), false);
        add(out, pm, pd, 6, 1, AppString.get(R.string.calendar_event_text_040), false);
        add(out, pm, pd, 6, 4, AppString.get(R.string.calendar_event_text_041), false);
        add(out, pm, pd, 6, 13, AppString.get(R.string.calendar_event_text_042), false);
        add(out, pm, pd, 6, 27, AppString.get(R.string.calendar_event_text_043), false);
        add(out, pm, pd, 7, 7, AppString.get(R.string.calendar_event_text_044), false);
        add(out, pm, pd, 7, 8, AppString.get(R.string.calendar_event_text_045), false);
        add(out, pm, pd, 7, 20, AppString.get(R.string.calendar_event_text_046), false);
        add(out, pm, pd, 8, 8, AppString.get(R.string.calendar_event_text_047), false);
        add(out, pm, pd, 8, 13, AppString.get(R.string.calendar_event_text_048), false);
        add(out, pm, pd, 9, 7, AppString.get(R.string.calendar_event_text_049), false);
        add(out, pm, pd, 9, 16, AppString.get(R.string.calendar_event_text_050), false);
        add(out, pm, pd, 9, 25, AppString.get(R.string.calendar_event_text_051), false);
        add(out, pm, pd, 10, 5, AppString.get(R.string.calendar_event_text_052), false);
        add(out, pm, pd, 10, 13, AppString.get(R.string.calendar_event_text_053), false);
        add(out, pm, pd, 10, 20, AppString.get(R.string.calendar_event_text_054), false);
        add(out, pm, pd, 11, 12, AppString.get(R.string.calendar_event_text_055), false);
        add(out, pm, pd, 12, 5, AppString.get(R.string.calendar_event_text_056), false);
        add(out, pm, pd, 12, 15, AppString.get(R.string.calendar_event_text_057), false);

        addLunarEvents(out, millis, LunarEventRules.IRAN);
    }

    private static void addArabHijriEvents(List<Event> out, long millis) {
        addLunarEvents(out, millis, LunarEventRules.ARAB);
    }

    private static void addLunarEvents(List<Event> out, long millis, int source) {
        android.icu.util.Calendar lunar = CalendarUtils.fromMillis(CalendarUtils.HIJRI, millis);
        int year = lunar.get(android.icu.util.Calendar.YEAR);
        int month = lunar.get(android.icu.util.Calendar.MONTH);
        int day = lunar.get(android.icu.util.Calendar.DAY_OF_MONTH);
        int length = CalendarUtils.daysInMonth(CalendarUtils.HIJRI, year, month);
        for (LunarEventRules.Rule rule : LunarEventRules.forDate(month + 1, day, length, source)) {
            out.add(new Event(AppString.get(lunarTitleId(rule.titleKey)), rule.holiday(source)));
        }
    }

    private static int lunarTitleId(int key) {
        switch (key) {
            case 58: return R.string.calendar_event_text_058;
            case 59: return R.string.calendar_event_text_059;
            case 60: return R.string.calendar_event_text_060;
            case 61: return R.string.calendar_event_text_061;
            case 62: return R.string.calendar_event_text_062;
            case 63: return R.string.calendar_event_text_063;
            case 64: return R.string.calendar_event_text_064;
            case 65: return R.string.calendar_event_text_065;
            case 66: return R.string.calendar_event_text_066;
            case 67: return R.string.calendar_event_text_067;
            case 68: return R.string.calendar_event_text_068;
            case 69: return R.string.calendar_event_text_069;
            case 70: return R.string.calendar_event_text_070;
            case 71: return R.string.calendar_event_text_071;
            case 72: return R.string.calendar_event_text_072;
            case 73: return R.string.calendar_event_text_073;
            case 74: return R.string.calendar_event_text_074;
            case 75: return R.string.calendar_event_text_075;
            case 76: return R.string.calendar_event_text_076;
            case 77: return R.string.calendar_event_text_077;
            case 78: return R.string.calendar_event_text_078;
            case 79: return R.string.calendar_event_text_079;
            case 81: return R.string.calendar_event_text_081;
            case 82: return R.string.calendar_event_text_082;
            case 84: return R.string.calendar_event_text_084;
            case 101: return R.string.lunar_event_101;
            case 102: return R.string.lunar_event_102;
            case 103: return R.string.lunar_event_103;
            case 104: return R.string.lunar_event_104;
            case 105: return R.string.lunar_event_105;
            case 106: return R.string.lunar_event_106;
            case 107: return R.string.lunar_event_107;
            case 108: return R.string.lunar_event_108;
            case 109: return R.string.lunar_event_109;
            case 110: return R.string.lunar_event_110;
            case 111: return R.string.lunar_event_111;
            case 112: return R.string.lunar_event_112;
            case 113: return R.string.lunar_event_113;
            case 114: return R.string.lunar_event_114;
            case 115: return R.string.lunar_event_115;
            case 116: return R.string.lunar_event_116;
            case 117: return R.string.lunar_event_117;
            default: throw new IllegalArgumentException("Unknown lunar event: " + key);
        }
    }

    private static void addInternationalGregorianEvents(List<Event> out, long millis) {
        android.icu.util.Calendar g = CalendarUtils.fromMillis(CalendarUtils.GREGORIAN, millis);
        int m = g.get(android.icu.util.Calendar.MONTH) + 1;
        int d = g.get(android.icu.util.Calendar.DAY_OF_MONTH);

        add(out, m, d, 1, 1, AppString.get(R.string.calendar_event_new_years_day), false);
        add(out, m, d, 1, 24, AppString.get(R.string.calendar_event_international_education_day), false);
        add(out, m, d, 2, 21, AppString.get(R.string.calendar_event_international_mother_language_day), false);
        add(out, m, d, 3, 8, AppString.get(R.string.calendar_event_international_womens_day), false);
        add(out, m, d, 3, 21, AppString.get(R.string.calendar_event_international_nowruz_day), false);
        add(out, m, d, 4, 22, AppString.get(R.string.calendar_event_earth_day), false);
        add(out, m, d, 5, 1, AppString.get(R.string.calendar_event_international_workers_day), false);
        add(out, m, d, 6, 5, AppString.get(R.string.calendar_event_world_environment_day), false);
        add(out, m, d, 6, 20, AppString.get(R.string.calendar_event_world_refugee_day), false);
        add(out, m, d, 8, 12, AppString.get(R.string.calendar_event_international_youth_day), false);
        add(out, m, d, 9, 21, AppString.get(R.string.calendar_event_international_peace_day), false);
        add(out, m, d, 10, 24, AppString.get(R.string.calendar_event_united_nations_day), false);
        add(out, m, d, 11, 20, AppString.get(R.string.calendar_event_world_childrens_day), false);
        add(out, m, d, 12, 3, AppString.get(R.string.calendar_event_international_disabilities_day), false);
        add(out, m, d, 12, 10, AppString.get(R.string.calendar_event_human_rights_day), false);
        add(out, m, d, 12, 25, AppString.get(R.string.calendar_event_christmas_day), false);
        add(out, m, d, 12, 31, AppString.get(R.string.calendar_event_new_years_eve), false);
    }

    private static void add(
            List<Event> out,
            int currentMonth,
            int currentDay,
            int month,
            int day,
            String title,
            boolean holiday) {
        if (currentMonth == month && currentDay == day) {
            out.add(new Event(title, holiday));
        }
    }
}
