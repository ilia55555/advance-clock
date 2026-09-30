package com.ilia.advanceclock;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class CalendarEventRepository {
    public static final String DATASET_VERSION = "2026-09-27";
    public static final String IRAN_SOURCE = AppString.get(R.string.calendar_event_text_001);
    public static final String INTERNATIONAL_SOURCE = AppString.get(R.string.calendar_event_text_002);
    public static final String ARAB_SOURCE = AppString.get(R.string.calendar_event_text_003);
    public static final int REVIEWED_FROM_GREGORIAN_YEAR = 2026;
    public static final int REVIEWED_THROUGH_GREGORIAN_YEAR = 2026;
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
        int year = CalendarUtils.fromMillis(CalendarUtils.GREGORIAN, millis)
                .get(android.icu.util.Calendar.YEAR);
        String base = AppString.get(R.string.calendar_event_text_010) + DATASET_VERSION;
        if (year < REVIEWED_FROM_GREGORIAN_YEAR
                || year > REVIEWED_THROUGH_GREGORIAN_YEAR) {
            return base + AppString.get(R.string.calendar_event_text_011);
        }
        ArrayList<String> sources = new ArrayList<>();
        if (persianEnabled) sources.add(IRAN_SOURCE);
        if (hijriEnabled) sources.add(ARAB_SOURCE);
        if (gregorianEnabled) sources.add(INTERNATIONAL_SOURCE);
        return base + AppString.get(R.string.calendar_event_text_012) + android.text.TextUtils.join(" / ", sources);
    }

    private static List<Event> attachMetadata(List<Event> events, int calendarType) {
        String source = calendarType == CalendarUtils.PERSIAN
                ? IRAN_SOURCE
                : calendarType == CalendarUtils.HIJRI ? ARAB_SOURCE : INTERNATIONAL_SOURCE;
        ArrayList<Event> enriched = new ArrayList<>(events.size());
        for (Event event : events) {
            enriched.add(new Event(
                    event.title,
                    event.holiday,
                    source,
                    REVIEWED_FROM_GREGORIAN_YEAR,
                    REVIEWED_THROUGH_GREGORIAN_YEAR,
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

        addOfficialIranianReligiousEvents(out, millis);
    }

    /**
     * Religious holidays are keyed by the civil date published for the reviewed
     * Iranian calendar dataset. They must not be inferred from Umm al-Qura or a
     * generic ICU Islamic calendar because official Iranian observance can differ.
     */
    private static void addOfficialIranianReligiousEvents(List<Event> out, long millis) {
        android.icu.util.Calendar g = CalendarUtils.fromMillis(
                CalendarUtils.GREGORIAN, millis);
        int year = g.get(android.icu.util.Calendar.YEAR);
        int month = g.get(android.icu.util.Calendar.MONTH) + 1;
        int day = g.get(android.icu.util.Calendar.DAY_OF_MONTH);
        if (year != 2026) return;

        add(out, month, day, 1, 3, AppString.get(R.string.calendar_event_text_058), true);
        add(out, month, day, 1, 17, AppString.get(R.string.calendar_event_text_059), true);
        add(out, month, day, 2, 4, AppString.get(R.string.calendar_event_text_060), true);
        add(out, month, day, 3, 11, AppString.get(R.string.calendar_event_text_061), true);
        add(out, month, day, 3, 21, AppString.get(R.string.calendar_event_text_062), true);
        add(out, month, day, 3, 22, AppString.get(R.string.calendar_event_text_063), true);
        add(out, month, day, 4, 15, AppString.get(R.string.calendar_event_text_064), true);
        add(out, month, day, 5, 27, AppString.get(R.string.calendar_event_text_065), true);
        add(out, month, day, 6, 5, AppString.get(R.string.calendar_event_text_066), true);
        add(out, month, day, 6, 25, AppString.get(R.string.calendar_event_text_067), true);
        add(out, month, day, 6, 26, AppString.get(R.string.calendar_event_text_068), true);
        add(out, month, day, 8, 5, AppString.get(R.string.calendar_event_text_069), true);
        add(out, month, day, 8, 13,
                AppString.get(R.string.calendar_event_text_070), true);
        add(out, month, day, 8, 15, AppString.get(R.string.calendar_event_text_071), true);
        add(out, month, day, 8, 22, AppString.get(R.string.calendar_event_text_072), true);
        add(out, month, day, 8, 31,
                AppString.get(R.string.calendar_event_text_073), true);
        add(out, month, day, 11, 14, AppString.get(R.string.calendar_event_text_074), true);
        add(out, month, day, 12, 23, AppString.get(R.string.calendar_event_text_058), true);
    }

    private static void addArabHijriEvents(List<Event> out, long millis) {
        android.icu.util.Calendar h = CalendarUtils.fromMillis(CalendarUtils.HIJRI, millis);
        int m = h.get(android.icu.util.Calendar.MONTH) + 1;
        int d = h.get(android.icu.util.Calendar.DAY_OF_MONTH);

        add(out, m, d, 1, 1, AppString.get(R.string.calendar_event_text_075), true);
        add(out, m, d, 3, 12, AppString.get(R.string.calendar_event_text_076), true);
        add(out, m, d, 7, 27, AppString.get(R.string.calendar_event_text_077), false);
        add(out, m, d, 9, 1, AppString.get(R.string.calendar_event_text_078), false);
        add(out, m, d, 9, 27, AppString.get(R.string.calendar_event_text_079), false);
        add(out, m, d, 10, 1, AppString.get(R.string.calendar_event_text_080), true);
        add(out, m, d, 10, 2, AppString.get(R.string.calendar_event_text_081), true);
        add(out, m, d, 10, 3, AppString.get(R.string.calendar_event_text_081), true);
        add(out, m, d, 12, 9, AppString.get(R.string.calendar_event_text_082), true);
        add(out, m, d, 12, 10, AppString.get(R.string.calendar_event_text_083), true);
        add(out, m, d, 12, 11, AppString.get(R.string.calendar_event_text_084), true);
        add(out, m, d, 12, 12, AppString.get(R.string.calendar_event_text_084), true);
        add(out, m, d, 12, 13, AppString.get(R.string.calendar_event_text_084), true);
    }

    private static void addInternationalGregorianEvents(List<Event> out, long millis) {
        android.icu.util.Calendar g = CalendarUtils.fromMillis(CalendarUtils.GREGORIAN, millis);
        int m = g.get(android.icu.util.Calendar.MONTH) + 1;
        int d = g.get(android.icu.util.Calendar.DAY_OF_MONTH);

        add(out, m, d, 1, 1, "New Year's Day", false);
        add(out, m, d, 1, 24, "International Day of Education", false);
        add(out, m, d, 2, 21, "International Mother Language Day", false);
        add(out, m, d, 3, 8, "International Women's Day", false);
        add(out, m, d, 3, 21, "International Day of Nowruz", false);
        add(out, m, d, 4, 22, "Earth Day", false);
        add(out, m, d, 5, 1, "International Workers' Day", false);
        add(out, m, d, 6, 5, "World Environment Day", false);
        add(out, m, d, 6, 20, "World Refugee Day", false);
        add(out, m, d, 8, 12, "International Youth Day", false);
        add(out, m, d, 9, 21, "International Day of Peace", false);
        add(out, m, d, 10, 24, "United Nations Day", false);
        add(out, m, d, 11, 20, "World Children's Day", false);
        add(out, m, d, 12, 3, "International Day of Persons with Disabilities", false);
        add(out, m, d, 12, 10, "Human Rights Day", false);
        add(out, m, d, 12, 25, "Christmas Day", false);
        add(out, m, d, 12, 31, "New Year's Eve", false);
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
