package com.ilia.advanceclock;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public final class CalendarEventRepository {
    public static final class Event {
        public final String title;
        public final boolean holiday;

        Event(String title, boolean holiday) {
            this.title = title;
            this.holiday = holiday;
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
        return out;
    }

    public static boolean isOfficialHoliday(Context context, long millis, int calendarType) {
        for (Event event : eventsFor(context, millis, calendarType)) {
            if (event.holiday) return true;
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

        android.icu.util.Calendar h = CalendarUtils.fromMillis(CalendarUtils.HIJRI, millis);
        int hm = h.get(android.icu.util.Calendar.MONTH) + 1;
        int hd = h.get(android.icu.util.Calendar.DAY_OF_MONTH);

        add(out, hm, hd, 1, 9, "تاسوعای حسینی", true);
        add(out, hm, hd, 1, 10, "عاشورای حسینی", true);
        add(out, hm, hd, 2, 20, "اربعین حسینی", true);
        add(out, hm, hd, 2, 28, "رحلت پیامبر اکرم و شهادت امام حسن مجتبی", true);
        add(out, hm, hd, 2, 30, "شهادت امام رضا", true);
        add(out, hm, hd, 3, 8, "شهادت امام حسن عسکری", true);
        add(out, hm, hd, 3, 17, "میلاد پیامبر اکرم و امام جعفر صادق", true);
        add(out, hm, hd, 6, 3, "شهادت حضرت فاطمه زهرا", true);
        add(out, hm, hd, 7, 13, "میلاد امام علی", true);
        add(out, hm, hd, 7, 27, "مبعث پیامبر اکرم", true);
        add(out, hm, hd, 8, 15, "میلاد امام مهدی", true);
        add(out, hm, hd, 9, 21, "شهادت امام علی", true);
        add(out, hm, hd, 10, 1, "عید سعید فطر", true);
        add(out, hm, hd, 10, 2, "تعطیل عید سعید فطر", true);
        add(out, hm, hd, 10, 25, "شهادت امام جعفر صادق", true);
        add(out, hm, hd, 12, 10, "عید قربان", true);
        add(out, hm, hd, 12, 18, "عید غدیر خم", true);
    }

    private static void addArabHijriEvents(List<Event> out, long millis) {
        android.icu.util.Calendar h = CalendarUtils.fromMillis(CalendarUtils.HIJRI, millis);
        int m = h.get(android.icu.util.Calendar.MONTH) + 1;
        int d = h.get(android.icu.util.Calendar.DAY_OF_MONTH);

        add(out, m, d, 1, 1, "رأس السنة الهجرية", true);
        add(out, m, d, 3, 12, "المولد النبوي الشريف", true);
        add(out, m, d, 7, 27, "الإسراء والمعراج", false);
        add(out, m, d, 9, 1, "بداية شهر رمضان", false);
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

        add(out, m, d, 1, 1, "New Year's Day", true);
        add(out, m, d, 3, 8, "International Women's Day", false);
        add(out, m, d, 3, 21, "International Day of Nowruz", false);
        add(out, m, d, 4, 22, "Earth Day", false);
        add(out, m, d, 5, 1, "International Workers' Day", true);
        add(out, m, d, 6, 5, "World Environment Day", false);
        add(out, m, d, 9, 21, "International Day of Peace", false);
        add(out, m, d, 10, 24, "United Nations Day", false);
        add(out, m, d, 12, 25, "Christmas Day", true);
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
