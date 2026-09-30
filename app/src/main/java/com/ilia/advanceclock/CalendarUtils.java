package com.ilia.advanceclock;

import android.icu.util.ULocale;

import java.text.DateFormatSymbols;
import java.util.Locale;

public final class CalendarUtils {
    public static final int PERSIAN = 0;
    public static final int GREGORIAN = 1;
    public static final int HIJRI = 2;

    private static final String[] PERSIAN_MONTHS = {
            AppString.get(R.string.runtime_text_0187), AppString.get(R.string.runtime_text_0188), AppString.get(R.string.runtime_text_0189), AppString.get(R.string.runtime_text_0190), AppString.get(R.string.runtime_text_0191), AppString.get(R.string.runtime_text_0192),
            AppString.get(R.string.runtime_text_0193), AppString.get(R.string.runtime_text_0194), AppString.get(R.string.runtime_text_0195), AppString.get(R.string.runtime_text_0196), AppString.get(R.string.runtime_text_0197), AppString.get(R.string.runtime_text_0198)
    };
    private static final String[] GREGORIAN_MONTHS = {
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    };
    private static final String[] HIJRI_MONTHS = {
            AppString.get(R.string.runtime_text_0620), AppString.get(R.string.runtime_text_0621), AppString.get(R.string.runtime_text_0622), AppString.get(R.string.runtime_text_0623), AppString.get(R.string.runtime_text_0624), AppString.get(R.string.runtime_text_0625),
            AppString.get(R.string.runtime_text_0626), AppString.get(R.string.runtime_text_0627), AppString.get(R.string.runtime_text_0628), AppString.get(R.string.runtime_text_0629), AppString.get(R.string.runtime_text_0630), AppString.get(R.string.runtime_text_0631)
    };

    private CalendarUtils() {}

    public static android.icu.util.Calendar create(int type) {
        ULocale locale;
        switch (type) {
            case GREGORIAN:
                locale = new ULocale("en_US@calendar=gregorian");
                break;
            case HIJRI:
                // Use the published Umm al-Qura calendar instead of ICU's
                // generic tabular Islamic calendar. This is the civil calendar
                // used for the shared Saudi/Arab occasion source.
                locale = new ULocale("ar_SA@calendar=islamic-umalqura");
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
            case GREGORIAN: return AppString.get(R.string.runtime_text_0178);
            case HIJRI: return AppString.get(R.string.runtime_text_0179);
            case PERSIAN:
            default: return AppString.get(R.string.runtime_text_0177);
        }
    }

    public static String monthName(int type, int month) {
        int index = Math.max(0, Math.min(11, month));
        switch (type) {
            case GREGORIAN:
                String localized = DateFormatSymbols.getInstance(Locale.getDefault())
                        .getMonths()[index];
                return localized.isEmpty() ? GREGORIAN_MONTHS[index] : localized;
            case HIJRI: return HIJRI_MONTHS[index];
            case PERSIAN:
            default: return PERSIAN_MONTHS[index];
        }
    }

    public static String formatDate(long millis, int type) {
        android.icu.util.Calendar c = fromMillis(type, millis);
        return formatDate(c, type);
    }

    public static String formatDate(long millis, int type, java.util.TimeZone zone) {
        android.icu.util.Calendar c = create(type);
        c.setTimeZone(android.icu.util.TimeZone.getTimeZone(zone.getID()));
        c.setTimeInMillis(millis);
        return formatDate(c, type);
    }

    private static String formatDate(android.icu.util.Calendar c, int type) {
        String day = fa(c.get(android.icu.util.Calendar.DAY_OF_MONTH));
        String year = fa(c.get(android.icu.util.Calendar.YEAR));
        return day + " " + monthName(type, c.get(android.icu.util.Calendar.MONTH)) + " " + year;
    }

    public static String formatNumeric(long millis, int type) {
        android.icu.util.Calendar c = fromMillis(type, millis);
        return formatNumeric(c);
    }

    public static String formatNumeric(long millis, int type, java.util.TimeZone zone) {
        android.icu.util.Calendar c = create(type);
        c.setTimeZone(android.icu.util.TimeZone.getTimeZone(zone.getID()));
        c.setTimeInMillis(millis);
        return formatNumeric(c);
    }

    private static String formatNumeric(android.icu.util.Calendar c) {
        String value = String.format(Locale.US, "%04d/%02d/%02d",
                c.get(android.icu.util.Calendar.YEAR),
                c.get(android.icu.util.Calendar.MONTH) + 1,
                c.get(android.icu.util.Calendar.DAY_OF_MONTH));
        return fa(value);
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
        String language = Locale.getDefault().getLanguage();
        if (!"fa".equals(language) && !"ar".equals(language)) return value;
        char[] en = {'0','1','2','3','4','5','6','7','8','9'};
        char[] localized = "ar".equals(language)
                ? new char[]{'٠','١','٢','٣','٤','٥','٦','٧','٨','٩'}
                : new char[]{'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};
        String out = value;
        for (int i = 0; i < en.length; i++) out = out.replace(en[i], localized[i]);
        return out;
    }
}
