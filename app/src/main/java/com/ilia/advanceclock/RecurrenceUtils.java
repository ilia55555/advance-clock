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
    public static final int WEEKDAYS = 7;

    private RecurrenceUtils() {}

    public static String[] labels() {
        return new String[]{
                "بدون تکرار", "هر روز", "هر هفته", "هر ماه", "هر سال",
                "هر چند روز", "تاریخ‌های دلخواه", "روزهای هفته"
        };
    }

    public static long next(long base, int mode, int intervalDays, String customDatesJson, long now) {
        if (mode == NONE) return base > now ? base : -1L;

        if (mode == WEEKDAYS) {
            List<Integer> weekdays = parseWeekdays(customDatesJson);
            if (weekdays.isEmpty()) return -1L;
            Calendar baseCalendar = Calendar.getInstance();
            baseCalendar.setTimeInMillis(base);
            Calendar candidate = (Calendar) baseCalendar.clone();
            int weeksInterval = Math.max(1, intervalDays);
            for (int day = 0; day < 3660; day++) {
                if (candidate.getTimeInMillis() > now
                        && weekdays.contains(candidate.get(Calendar.DAY_OF_WEEK))) {
                    long days = Math.floorDiv(
                            startOfDay(candidate) - startOfDay(baseCalendar), 86_400_000L);
                    long weeks = Math.max(0L, days / 7L);
                    if (weeks % weeksInterval == 0L) return candidate.getTimeInMillis();
                }
                candidate.add(Calendar.DAY_OF_YEAR, 1);
            }
            return -1L;
        }

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

    public static List<Integer> parseWeekdays(String raw) {
        ArrayList<Integer> days = new ArrayList<>();
        try {
            org.json.JSONObject object = new org.json.JSONObject(raw == null ? "{}" : raw);
            JSONArray array = object.optJSONArray("weekdays");
            if (array != null) for (int i = 0; i < array.length(); i++) {
                int day = array.optInt(i, -1);
                if (day >= Calendar.SUNDAY && day <= Calendar.SATURDAY && !days.contains(day))
                    days.add(day);
            }
        } catch (Exception ignored) {}
        return days;
    }

    public static String weekdaysToJson(List<Integer> weekdays) {
        org.json.JSONObject object = new org.json.JSONObject();
        JSONArray array = new JSONArray();
        if (weekdays != null) for (Integer day : weekdays) if (day != null) array.put(day);
        try { object.put("weekdays", array); } catch (Exception ignored) {}
        return object.toString();
    }

    private static long startOfDay(Calendar source) {
        Calendar day = (Calendar) source.clone();
        day.set(Calendar.HOUR_OF_DAY, 0); day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0); day.set(Calendar.MILLISECOND, 0);
        return day.getTimeInMillis();
    }

    public static String summary(int mode, int intervalDays, String customDatesJson) {
        if (mode == INTERVAL_DAYS) return "هر " + Math.max(1, intervalDays) + " روز";
        if (mode == CUSTOM_DATES) return parseDates(customDatesJson).size() + " تاریخ انتخاب شده";
        if (mode == WEEKDAYS) return "روزهای انتخابی هر " + Math.max(1, intervalDays) + " هفته";
        String[] labels = labels();
        return labels[Math.max(0, Math.min(labels.length - 1, mode))];
    }
}
