package com.ilia.advanceclock;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Offline prayer-time calculator using the Institute of Geophysics,
 * University of Tehran convention used by Iranian official timetables:
 * Fajr 17.7 degrees, Maghrib 4.5 degrees, Isha 14 degrees,
 * and Jafari midnight (sunset to next true dawn).
 */
public final class PrayerTimeCalculator {
    private static final double FAJR_ANGLE = 17.7;
    private static final double MAGHRIB_ANGLE = 4.5;
    private static final double ISHA_ANGLE = 14.0;
    private static final double SUNRISE_SUNSET_ANGLE = 0.833;

    private static final int FAJR = 0;
    private static final int SUNRISE = 1;
    private static final int DHUHR = 2;
    private static final int ASR = 3;
    private static final int SUNSET = 4;
    private static final int MAGHRIB = 5;
    private static final int ISHA = 6;

    public static final class Times {
        public final int fajrMinutes;
        public final int sunriseMinutes;
        public final int dhuhrMinutes;
        public final int asrMinutes;
        public final int sunsetMinutes;
        public final int maghribMinutes;
        public final int ishaMinutes;
        public final int midnightMinutes;

        Times(
                int fajrMinutes,
                int sunriseMinutes,
                int dhuhrMinutes,
                int asrMinutes,
                int sunsetMinutes,
                int maghribMinutes,
                int ishaMinutes,
                int midnightMinutes) {
            this.fajrMinutes = fajrMinutes;
            this.sunriseMinutes = sunriseMinutes;
            this.dhuhrMinutes = dhuhrMinutes;
            this.asrMinutes = asrMinutes;
            this.sunsetMinutes = sunsetMinutes;
            this.maghribMinutes = maghribMinutes;
            this.ishaMinutes = ishaMinutes;
            this.midnightMinutes = midnightMinutes;
        }

        public String fajr() { return format(fajrMinutes); }
        public String sunrise() { return format(sunriseMinutes); }
        public String dhuhr() { return format(dhuhrMinutes); }
        public String asr() { return format(asrMinutes); }
        public String sunset() { return format(sunsetMinutes); }
        public String maghrib() { return format(maghribMinutes); }
        public String isha() { return format(ishaMinutes); }
        public String midnight() { return format(midnightMinutes); }
    }

    private PrayerTimeCalculator() {}

    public static Times calculate(
            long dateMillis,
            double latitude,
            double longitude,
            TimeZone timeZone) {
        TimeZone zone = timeZone == null ? TimeZone.getDefault() : timeZone;
        Calendar date = Calendar.getInstance(zone);
        date.setTimeInMillis(dateMillis);

        int year = date.get(Calendar.YEAR);
        int month = date.get(Calendar.MONTH) + 1;
        int day = date.get(Calendar.DAY_OF_MONTH);
        double tz = timeZoneHours(zone, year, month, day);

        double[] today = computeDay(year, month, day, latitude, longitude, tz);

        Calendar tomorrow = (Calendar) date.clone();
        tomorrow.add(Calendar.DAY_OF_MONTH, 1);
        int nextYear = tomorrow.get(Calendar.YEAR);
        int nextMonth = tomorrow.get(Calendar.MONTH) + 1;
        int nextDay = tomorrow.get(Calendar.DAY_OF_MONTH);
        double nextTz = timeZoneHours(zone, nextYear, nextMonth, nextDay);
        double[] next = computeDay(
                nextYear, nextMonth, nextDay, latitude, longitude, nextTz);

        double midnight = Double.NaN;
        if (!Double.isNaN(today[SUNSET]) && !Double.isNaN(next[FAJR])) {
            midnight = fixHour(
                    today[SUNSET]
                            + timeDiff(today[SUNSET], next[FAJR]) / 2.0);
        }

        return new Times(
                toMinute(today[FAJR]),
                toMinute(today[SUNRISE]),
                toMinute(today[DHUHR]),
                toMinute(today[ASR]),
                toMinute(today[SUNSET]),
                toMinute(today[MAGHRIB]),
                toMinute(today[ISHA]),
                toMinute(midnight));
    }

    private static double[] computeDay(
            int year,
            int month,
            int day,
            double latitude,
            double longitude,
            double timeZoneHours) {
        double jd = julianDate(year, month, day)
                - longitude / (15.0 * 24.0);

        double[] times = {5, 6, 12, 13, 18, 18, 18};
        double[] t = dayPortion(times);

        times[FAJR] = sunAngleTime(jd, latitude, FAJR_ANGLE, t[FAJR], true);
        times[SUNRISE] = sunAngleTime(
                jd, latitude, SUNRISE_SUNSET_ANGLE, t[SUNRISE], true);
        times[DHUHR] = midDay(jd, t[DHUHR]);
        times[ASR] = asrTime(jd, latitude, 1.0, t[ASR]);
        times[SUNSET] = sunAngleTime(
                jd, latitude, SUNRISE_SUNSET_ANGLE, t[SUNSET], false);
        times[MAGHRIB] = sunAngleTime(
                jd, latitude, MAGHRIB_ANGLE, t[MAGHRIB], false);
        times[ISHA] = sunAngleTime(
                jd, latitude, ISHA_ANGLE, t[ISHA], false);

        double offset = timeZoneHours - longitude / 15.0;
        for (int i = 0; i < times.length; i++) {
            if (!Double.isNaN(times[i])) times[i] += offset;
        }

        adjustHighLatitudes(times);
        for (int i = 0; i < times.length; i++) {
            if (!Double.isNaN(times[i])) times[i] = fixHour(times[i]);
        }
        return times;
    }

    private static void adjustHighLatitudes(double[] times) {
        if (Double.isNaN(times[SUNSET]) || Double.isNaN(times[SUNRISE])) return;

        double night = timeDiff(times[SUNSET], times[SUNRISE]);
        times[FAJR] = adjustHighLatitudeTime(
                times[FAJR], times[SUNRISE], FAJR_ANGLE, night, true);
        times[MAGHRIB] = adjustHighLatitudeTime(
                times[MAGHRIB], times[SUNSET], MAGHRIB_ANGLE, night, false);
        times[ISHA] = adjustHighLatitudeTime(
                times[ISHA], times[SUNSET], ISHA_ANGLE, night, false);
    }

    private static double adjustHighLatitudeTime(
            double time,
            double base,
            double angle,
            double night,
            boolean beforeBase) {
        double portion = (angle / 60.0) * night;
        double distance = beforeBase
                ? timeDiff(time, base)
                : timeDiff(base, time);
        if (Double.isNaN(time) || distance > portion) {
            return base + (beforeBase ? -portion : portion);
        }
        return time;
    }

    private static double[] dayPortion(double[] times) {
        double[] out = new double[times.length];
        for (int i = 0; i < times.length; i++) out[i] = times[i] / 24.0;
        return out;
    }

    private static double midDay(double jd, double time) {
        double equation = sunPosition(jd + time).equation;
        return fixHour(12.0 - equation);
    }

    private static double sunAngleTime(
            double jd,
            double latitude,
            double angle,
            double time,
            boolean beforeNoon) {
        SunPosition sun = sunPosition(jd + time);
        double noon = midDay(jd, time);

        double numerator = -sin(angle)
                - sin(sun.declination) * sin(latitude);
        double denominator = cos(sun.declination) * cos(latitude);
        if (Math.abs(denominator) < 1e-12) return Double.NaN;

        double value = numerator / denominator;
        if (value < -1.0 || value > 1.0) return Double.NaN;

        double delta = acos(value) / 15.0;
        return noon + (beforeNoon ? -delta : delta);
    }

    private static double asrTime(
            double jd,
            double latitude,
            double shadowFactor,
            double time) {
        SunPosition sun = sunPosition(jd + time);
        double angle = -acot(
                shadowFactor + tan(Math.abs(latitude - sun.declination)));
        return sunAngleTime(jd, latitude, angle, time, false);
    }

    private static SunPosition sunPosition(double jd) {
        double d = jd - 2451545.0;
        double g = fixAngle(357.529 + 0.98560028 * d);
        double q = fixAngle(280.459 + 0.98564736 * d);
        double l = fixAngle(
                q + 1.915 * sin(g) + 0.020 * sin(2.0 * g));
        double e = 23.439 - 0.00000036 * d;

        double ra = atan2(cos(e) * sin(l), cos(l)) / 15.0;
        ra = fixHour(ra);
        double equation = q / 15.0 - ra;
        double declination = asin(sin(e) * sin(l));
        return new SunPosition(declination, equation);
    }

    private static double julianDate(int year, int month, int day) {
        int y = year;
        int m = month;
        if (m <= 2) {
            y--;
            m += 12;
        }
        int a = (int) Math.floor(y / 100.0);
        int b = 2 - a + (int) Math.floor(a / 4.0);
        return Math.floor(365.25 * (y + 4716))
                + Math.floor(30.6001 * (m + 1))
                + day + b - 1524.5;
    }

    private static double timeZoneHours(
            TimeZone zone, int year, int month, int day) {
        Calendar noon = Calendar.getInstance(zone);
        noon.clear();
        noon.set(year, month - 1, day, 12, 0, 0);
        return zone.getOffset(noon.getTimeInMillis()) / 3600000.0;
    }

    private static int toMinute(double hours) {
        if (Double.isNaN(hours) || Double.isInfinite(hours)) return -1;
        double fixed = fixHour(hours);
        int minute = (int) Math.floor(fixed * 60.0 + 0.5);
        return ((minute % 1440) + 1440) % 1440;
    }

    public static String format(int minutes) {
        if (minutes < 0) return "—:—";
        int normalized = ((minutes % 1440) + 1440) % 1440;
        String value = String.format(
                java.util.Locale.US,
                "%02d:%02d",
                normalized / 60,
                normalized % 60);
        return CalendarUtils.fa(value);
    }

    private static double timeDiff(double a, double b) {
        return fixHour(b - a);
    }

    private static double fixAngle(double angle) {
        return fix(angle, 360.0);
    }

    private static double fixHour(double hour) {
        return fix(hour, 24.0);
    }

    private static double fix(double value, double cycle) {
        double out = value - cycle * Math.floor(value / cycle);
        return out < 0 ? out + cycle : out;
    }

    private static double sin(double degrees) {
        return Math.sin(Math.toRadians(degrees));
    }

    private static double cos(double degrees) {
        return Math.cos(Math.toRadians(degrees));
    }

    private static double tan(double degrees) {
        return Math.tan(Math.toRadians(degrees));
    }

    private static double asin(double value) {
        return Math.toDegrees(Math.asin(value));
    }

    private static double acos(double value) {
        return Math.toDegrees(Math.acos(value));
    }

    private static double atan2(double y, double x) {
        return Math.toDegrees(Math.atan2(y, x));
    }

    private static double acot(double value) {
        return Math.toDegrees(Math.atan2(1.0, value));
    }

    private static final class SunPosition {
        final double declination;
        final double equation;

        SunPosition(double declination, double equation) {
            this.declination = declination;
            this.equation = equation;
        }
    }
}
