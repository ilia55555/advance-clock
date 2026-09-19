package com.ilia.advanceclock;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

public final class RecurrenceUtils {
    public static final int NONE = 0;
    public static final int DAILY = 1;
    public static final int WEEKLY = 2;
    public static final int MONTHLY = 3;
    public static final int YEARLY = 4;
    public static final int INTERVAL_DAYS = 5;
    public static final int CUSTOM_DATES = 6;

    private RecurrenceUtils() {}

    public static String[] labels() {
        return new String[]{
                "بدون تکرار", "هر روز", "هر هفته", "هر ماه", "هر سال",
                "هر چند روز", "تاریخ‌های دلخواه"
        };
    }

    public static long next(long base, int mode, int intervalDays, String customDatesJson, long now) {
        if (mode == NONE) return base > now ? base : -1L;

        if (mode == CUSTOM_DATES) {
            long best = base > now ? base : Long.MAX_VALUE;
            for (long candidate : parseDates(customDatesJson)) {
                if (candidate > now && candidate < best) best = candidate;
            }
            return best == Long.MAX_VALUE ? -1L : best;
        }

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(base);
        int safeInterval = Math.max(1, intervalDays);
        while (c.getTimeInMillis() <= now) {
            switch (mode) {
                case DAILY: c.add(Calendar.DAY_OF_YEAR, 1); break;
                case WEEKLY: c.add(Calendar.WEEK_OF_YEAR, 1); break;
                case MONTHLY: c.add(Calendar.MONTH, 1); break;
                case YEARLY: c.add(Calendar.YEAR, 1); break;
                case INTERVAL_DAYS: c.add(Calendar.DAY_OF_YEAR, safeInterval); break;
                default: return -1L;
            }
        }
        return c.getTimeInMillis();
    }

    public static List<Long> parseDates(String raw) {
        ArrayList<Long> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(raw == null ? "[]" : raw);
            for (int i = 0; i < a.length(); i++) {
                long value = a.optLong(i, -1L);
                if (value > 0) out.add(value);
            }
        } catch (Exception ignored) {}
        Collections.sort(out);
        return out;
    }

    public static String toJson(List<Long> dates) {
        JSONArray a = new JSONArray();
        if (dates != null) for (Long date : dates) if (date != null && date > 0) a.put(date);
        return a.toString();
    }

    public static String summary(int mode, int intervalDays, String customDatesJson) {
        if (mode == INTERVAL_DAYS) return "هر " + Math.max(1, intervalDays) + " روز";
        if (mode == CUSTOM_DATES) return parseDates(customDatesJson).size() + " تاریخ انتخاب شده";
        String[] labels = labels();
        return labels[Math.max(0, Math.min(labels.length - 1, mode))];
    }
}
