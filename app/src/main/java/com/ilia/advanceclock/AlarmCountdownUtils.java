package com.ilia.advanceclock;

import android.content.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class AlarmCountdownUtils {
    private AlarmCountdownUtils() {}

    public static long nextOccurrence(AlarmItem item, long now) {
        if (item == null) return -1L;

        if (item.triggerAtMillis > now) {
            return item.triggerAtMillis;
        }

        return RecurrenceUtils.next(
                item.triggerAtMillis,
                item.recurrenceMode,
                item.intervalDays,
                item.customDatesJson,
                now);
    }

    public static String remainingText(
            Context context,
            AlarmItem item) {
        long nowMillis = System.currentTimeMillis();
        long next = nextOccurrence(item, nowMillis);
        if (next <= nowMillis) {
            return context.getString(
                    R.string.alarm_remaining_expired);
        }

        ZoneId zone = ZoneId.systemDefault();
        ZonedDateTime cursor = Instant.ofEpochMilli(nowMillis)
                .atZone(zone);
        ZonedDateTime target = Instant.ofEpochMilli(next)
                .atZone(zone);

        if (!target.isAfter(cursor)) {
            return context.getString(
                    R.string.alarm_remaining_less_than_minute);
        }

        long years = ChronoUnit.YEARS.between(
                cursor.toLocalDate(),
                target.toLocalDate());
        if (years > 0L) {
            ZonedDateTime candidate = cursor.plusYears(years);
            while (candidate.isAfter(target) && years > 0L) {
                years--;
                candidate = cursor.plusYears(years);
            }
            cursor = candidate;
        }

        long months = ChronoUnit.MONTHS.between(
                cursor.toLocalDate(),
                target.toLocalDate());
        if (months > 0L) {
            ZonedDateTime candidate = cursor.plusMonths(months);
            while (candidate.isAfter(target) && months > 0L) {
                months--;
                candidate = cursor.plusMonths(months);
            }
            cursor = candidate;
        }

        long days = ChronoUnit.DAYS.between(
                cursor.toLocalDate(),
                target.toLocalDate());
        if (days > 0L) {
            ZonedDateTime candidate = cursor.plusDays(days);
            while (candidate.isAfter(target) && days > 0L) {
                days--;
                candidate = cursor.plusDays(days);
            }
            cursor = candidate;
        }

        long hours = ChronoUnit.HOURS.between(cursor, target);
        if (hours > 0L) {
            cursor = cursor.plusHours(hours);
        }

        long minutes = Math.max(
                0L,
                ChronoUnit.MINUTES.between(cursor, target));

        List<String> parts = new ArrayList<>();
        addPart(
                context,
                parts,
                years,
                R.plurals.alarm_remaining_years);
        addPart(
                context,
                parts,
                months,
                R.plurals.alarm_remaining_months);
        addPart(
                context,
                parts,
                days,
                R.plurals.alarm_remaining_days);
        addPart(
                context,
                parts,
                hours,
                R.plurals.alarm_remaining_hours_unit);
        addPart(
                context,
                parts,
                minutes,
                R.plurals.alarm_remaining_minutes_unit);

        if (parts.isEmpty()) {
            return context.getString(
                    R.string.alarm_remaining_less_than_minute);
        }

        return context.getString(
                R.string.alarm_remaining_full_format,
                joinParts(
                        parts,
                        context.getString(
                                R.string.alarm_remaining_separator)));
    }

    private static void addPart(
            Context context,
            List<String> parts,
            long value,
            int pluralsId) {
        if (value <= 0L) return;
        int safe = value > Integer.MAX_VALUE
                ? Integer.MAX_VALUE
                : (int) value;
        parts.add(
                context.getResources().getQuantityString(
                        pluralsId,
                        safe,
                        value));
    }

    private static String joinParts(
            List<String> parts,
            String separator) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) out.append(separator);
            out.append(parts.get(i));
        }
        return out.toString();
    }
}
