package com.ilia.advanceclock;

import android.icu.util.ULocale;

import java.util.Locale;

public final class CalendarUtils {
    public static final int PERSIAN = 0;
    public static final int GREGORIAN = 1;
    public static final int HIJRI = 2;

    private static final String[] PERSIAN_MONTHS = {
            "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
            "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
    };
    private static final String[] GREGORIAN_MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };
    private static final String[] HIJRI_MONTHS = {
            "محرم", "صفر", "ربیع الاول", "ربیع الثانی", "جمادی الاول", "جمادی الثانی",
            "رجب", "شعبان", "رمضان", "شوال", "ذی القعده", "ذی الحجه"
    };

    private CalendarUtils() {}

    public static android.icu.util.Calendar create(int type) {
        ULocale locale;
        switch (type) {
            case GREGORIAN:
                locale = new ULocale("en_US@calendar=gregorian");
                break;
            case HIJRI:
                locale = new ULocale("ar_SA@calendar=islamic");
                break;
            case PERSIAN:
            default:
                locale = new ULocale("fa_IR@calendar=persian");
                break;
        }
        return android.icu.util.Calendar.getInstance(locale);
    }

    public static android.icu.util.Calendar fromMillis(int type, long millis) {
        android.icu.util.Calendar c = create(type);
        c.setTimeInMillis(millis);
        return c;
    }

    public static long toMillis(int type, int year, int month, int day, int hour, int minute) {
        android.icu.util.Calendar c = create(type);
        c.clear();
        c.set(year, month, day, hour, minute, 0);
        c.set(android.icu.util.Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static String calendarName(int type) {
        switch (type) {
            case GREGORIAN: return "میلادی";
            case HIJRI: return "قمری";
            case PERSIAN:
            default: return "شمسی";
        }
    }

    public static String monthName(int type, int month) {
        int index = Math.max(0, Math.min(11, month));
        switch (type) {
            case GREGORIAN: return GREGORIAN_MONTHS[index];
            case HIJRI: return HIJRI_MONTHS[index];
            case PERSIAN:
            default: return PERSIAN_MONTHS[index];
        }
    }

    public static String formatDate(long millis, int type) {
        android.icu.util.Calendar c = fromMillis(type, millis);
        String day = type == GREGORIAN
                ? String.valueOf(c.get(android.icu.util.Calendar.DAY_OF_MONTH))
                : fa(c.get(android.icu.util.Calendar.DAY_OF_MONTH));
        String year = type == GREGORIAN
                ? String.valueOf(c.get(android.icu.util.Calendar.YEAR))
                : fa(c.get(android.icu.util.Calendar.YEAR));
        return day + " " + monthName(type, c.get(android.icu.util.Calendar.MONTH)) + " " + year;
    }

    public static String formatNumeric(long millis, int type) {
        android.icu.util.Calendar c = fromMillis(type, millis);
        String value = String.format(Locale.US, "%04d/%02d/%02d",
                c.get(android.icu.util.Calendar.YEAR),
                c.get(android.icu.util.Calendar.MONTH) + 1,
                c.get(android.icu.util.Calendar.DAY_OF_MONTH));
        return type == GREGORIAN ? value : fa(value);
    }

    public static int otherTypeOne(int primary) {
        if (primary == PERSIAN) return GREGORIAN;
        return PERSIAN;
    }

    public static int otherTypeTwo(int primary) {
        if (primary == HIJRI) return GREGORIAN;
        return HIJRI;
    }

    public static String fa(int value) {
        return fa(String.valueOf(value));
    }

    public static String fa(String value) {
        char[] en = {'0','1','2','3','4','5','6','7','8','9'};
        char[] pe = {'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};
        String out = value;
        for (int i = 0; i < en.length; i++) out = out.replace(en[i], pe[i]);
        return out;
    }
}
