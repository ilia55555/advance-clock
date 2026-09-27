package com.ilia.advanceclock;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class CalendarEventRepository {
    public static final String DATASET_VERSION = "2026-09-27";
    public static final String IRAN_SOURCE = "مرکز تقویم مؤسسه ژئوفیزیک دانشگاه تهران";
    public static final String INTERNATIONAL_SOURCE = "تقویم مناسبت‌های سازمان ملل متحد";
    public static final String ARAB_SOURCE = "تقویم ام‌القری عربستان برای مناسبت‌های مشترک عربی";
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
                return "میلادی • بین‌المللی";
            case CalendarUtils.HIJRI:
                return "قمری • کشورهای عربی";
            case CalendarUtils.PERSIAN:
            default:
                return "شمسی • ایران";
        }
    }

    public static String holidayLabel(int calendarType) {
        if (calendarType == CalendarUtils.PERSIAN) return "تعطیل رسمی";
        if (calendarType == CalendarUtils.HIJRI) return "تعطیل مشترک/رایج";
        return "تعطیل رایج";
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
        String base = "نسخه داده: " + DATASET_VERSION;
        if (year < REVIEWED_FROM_GREGORIAN_YEAR
                || year > REVIEWED_THROUGH_GREGORIAN_YEAR) {
            return base + " • مناسبت‌های این سال نیازمند بازبینی منبع رسمی‌اند";
        }
        ArrayList<String> sources = new ArrayList<>();
        if (persianEnabled) sources.add(IRAN_SOURCE);
        if (hijriEnabled) sources.add(ARAB_SOURCE);
        if (gregorianEnabled) sources.add(INTERNATIONAL_SOURCE);
        return base + " • منابع فعال: " + android.text.TextUtils.join(" / ", sources);
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

        add(out, pm, pd, 1, 1, "نوروز", true);
        add(out, pm, pd, 1, 2, "تعطیلات نوروز", true);
        add(out, pm, pd, 1, 3, "تعطیلات نوروز", true);
        add(out, pm, pd, 1, 4, "تعطیلات نوروز", true);
        add(out, pm, pd, 1, 12, "روز جمهوری اسلامی ایران", true);
        add(out, pm, pd, 1, 13, "روز طبیعت", true);
        add(out, pm, pd, 3, 14, "رحلت امام خمینی", true);
        add(out, pm, pd, 3, 15, "قیام ۱۵ خرداد", true);
        add(out, pm, pd, 11, 22, "پیروزی انقلاب اسلامی", true);
        add(out, pm, pd, 12, 29, "روز ملی شدن صنعت نفت", true);
        add(out, pm, pd, 9, 30, "شب یلدا", false);
        add(out, pm, pd, 1, 6, "روز امید و شادباش‌نویسی", false);
        add(out, pm, pd, 1, 7, "روز هنرهای نمایشی", false);
        add(out, pm, pd, 1, 20, "روز ملی فناوری هسته‌ای", false);
        add(out, pm, pd, 1, 25, "روز بزرگداشت عطار نیشابوری", false);
        add(out, pm, pd, 2, 1, "روز بزرگداشت سعدی", false);
        add(out, pm, pd, 2, 10, "روز ملی خلیج فارس", false);
        add(out, pm, pd, 2, 25, "روز بزرگداشت فردوسی", false);
        add(out, pm, pd, 2, 28, "روز بزرگداشت خیام", false);
        add(out, pm, pd, 3, 1, "روز بزرگداشت ملاصدرا", false);
        add(out, pm, pd, 3, 3, "فتح خرمشهر", false);
        add(out, pm, pd, 3, 20, "روز جهانی صنایع دستی", false);
        add(out, pm, pd, 4, 1, "روز اصناف", false);
        add(out, pm, pd, 4, 7, "روز قوه قضائیه", false);
        add(out, pm, pd, 4, 10, "روز صنعت و معدن", false);
        add(out, pm, pd, 4, 14, "روز قلم", false);
        add(out, pm, pd, 5, 8, "روز بزرگداشت شیخ شهاب‌الدین سهروردی", false);
        add(out, pm, pd, 5, 17, "روز خبرنگار", false);
        add(out, pm, pd, 5, 28, "سالروز کودتای ۲۸ مرداد", false);
        add(out, pm, pd, 6, 1, "روز بزرگداشت ابوعلی سینا و روز پزشک", false);
        add(out, pm, pd, 6, 4, "روز کارمند", false);
        add(out, pm, pd, 6, 13, "روز بزرگداشت ابوریحان بیرونی", false);
        add(out, pm, pd, 6, 27, "روز شعر و ادب فارسی", false);
        add(out, pm, pd, 7, 7, "روز آتش‌نشانی و ایمنی", false);
        add(out, pm, pd, 7, 8, "روز بزرگداشت مولوی", false);
        add(out, pm, pd, 7, 20, "روز بزرگداشت حافظ", false);
        add(out, pm, pd, 8, 8, "روز نوجوان", false);
        add(out, pm, pd, 8, 13, "روز دانش‌آموز", false);
        add(out, pm, pd, 9, 7, "روز نیروی دریایی", false);
        add(out, pm, pd, 9, 16, "روز دانشجو", false);
        add(out, pm, pd, 9, 25, "روز پژوهش", false);
        add(out, pm, pd, 10, 5, "روز ایمنی در برابر زلزله", false);
        add(out, pm, pd, 10, 13, "روز جهانی مقاومت", false);
        add(out, pm, pd, 10, 20, "سالروز شهادت امیرکبیر", false);
        add(out, pm, pd, 11, 12, "بازگشت امام خمینی به ایران", false);
        add(out, pm, pd, 12, 5, "روز بزرگداشت خواجه نصیرالدین طوسی و روز مهندس", false);
        add(out, pm, pd, 12, 15, "روز درختکاری", false);

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

        add(out, month, day, 1, 3, "میلاد امام علی", true);
        add(out, month, day, 1, 17, "مبعث پیامبر اکرم", true);
        add(out, month, day, 2, 4, "میلاد امام مهدی", true);
        add(out, month, day, 3, 11, "شهادت امام علی", true);
        add(out, month, day, 3, 21, "عید سعید فطر", true);
        add(out, month, day, 3, 22, "تعطیل عید سعید فطر", true);
        add(out, month, day, 4, 15, "شهادت امام جعفر صادق", true);
        add(out, month, day, 5, 27, "عید قربان", true);
        add(out, month, day, 6, 5, "عید غدیر خم", true);
        add(out, month, day, 6, 25, "تاسوعای حسینی", true);
        add(out, month, day, 6, 26, "عاشورای حسینی", true);
        add(out, month, day, 8, 5, "اربعین حسینی", true);
        add(out, month, day, 8, 13,
                "رحلت پیامبر اکرم و شهادت امام حسن مجتبی", true);
        add(out, month, day, 8, 15, "شهادت امام رضا", true);
        add(out, month, day, 8, 22, "شهادت امام حسن عسکری", true);
        add(out, month, day, 8, 31,
                "میلاد پیامبر اکرم و امام جعفر صادق", true);
        add(out, month, day, 11, 14, "شهادت حضرت فاطمه زهرا", true);
        add(out, month, day, 12, 23, "میلاد امام علی", true);
    }

    private static void addArabHijriEvents(List<Event> out, long millis) {
        android.icu.util.Calendar h = CalendarUtils.fromMillis(CalendarUtils.HIJRI, millis);
        int m = h.get(android.icu.util.Calendar.MONTH) + 1;
        int d = h.get(android.icu.util.Calendar.DAY_OF_MONTH);

        add(out, m, d, 1, 1, "رأس السنة الهجرية", true);
        add(out, m, d, 3, 12, "المولد النبوي الشريف", true);
        add(out, m, d, 7, 27, "الإسراء والمعراج", false);
        add(out, m, d, 9, 1, "بداية شهر رمضان", false);
        add(out, m, d, 9, 27, "ليلة القدر", false);
        add(out, m, d, 10, 1, "عيد الفطر", true);
        add(out, m, d, 10, 2, "إجازة عيد الفطر", true);
        add(out, m, d, 10, 3, "إجازة عيد الفطر", true);
        add(out, m, d, 12, 9, "يوم عرفة", true);
        add(out, m, d, 12, 10, "عيد الأضحى", true);
        add(out, m, d, 12, 11, "إجازة عيد الأضحى", true);
        add(out, m, d, 12, 12, "إجازة عيد الأضحى", true);
        add(out, m, d, 12, 13, "إجازة عيد الأضحى", true);
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
