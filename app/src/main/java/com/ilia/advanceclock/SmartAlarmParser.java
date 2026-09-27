package com.ilia.advanceclock;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SmartAlarmParser {
    public static final int OFFSET_EXACT = 0;
    public static final int OFFSET_BEFORE = 1;
    public static final int OFFSET_AFTER = 2;

    public static final class Candidate {
        public String label;
        public int calendarType;
        public int year;
        public int month;
        public int day;
        public int hour;
        public int minute;
        public int endHour = -1;
        public int endMinute = -1;
        public int offsetMode = OFFSET_EXACT;
        public int offsetMinutes = 0;
        public String source = "";

        public long baseMillis() {
            return CalendarUtils.toMillis(
                    calendarType, year, month - 1, day, hour, minute);
        }

        public long triggerMillis() {
            long delta = Math.max(0, offsetMinutes) * 60_000L;
            if (offsetMode == OFFSET_BEFORE) return baseMillis() - delta;
            if (offsetMode == OFFSET_AFTER) return baseMillis() + delta;
            return baseMillis();
        }

        public String dateText() {
            return CalendarUtils.formatDate(baseMillis(), calendarType);
        }

        public String baseTimeText() {
            return faTime(hour, minute);
        }

        public String endTimeText() {
            return endHour < 0 ? "" : faTime(endHour, endMinute);
        }

        public String triggerTimeText() {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(triggerMillis());
            return faTime(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
        }

        public String offsetText() {
            if (offsetMode == OFFSET_EXACT || offsetMinutes <= 0) return "سرِ وقت";
            return CalendarUtils.fa(offsetMinutes)
                    + " دقیقه "
                    + (offsetMode == OFFSET_BEFORE ? "قبل" : "بعد");
        }
    }

    public static final class Result {
        public final ArrayList<Candidate> candidates = new ArrayList<>();
        public final ArrayList<String> warnings = new ArrayList<>();
        public boolean jsonDetected;
    }

    private static final Pattern DATE = Pattern.compile(
            "(?<!\\d)(\\d{2,4})\\s*[/\\-.]\\s*(\\d{1,2})\\s*[/\\-.]\\s*(\\d{1,2})(?!\\d)");
    private static final Pattern TIME_COLON = Pattern.compile(
            "(?<!\\d)([01]?\\d|2[0-3])\\s*[:٫.]\\s*([0-5]?\\d)(?!\\d)");
    private static final Pattern TIME_WORD = Pattern.compile(
            "(?:ساعت|زمان|شروع|استارت|پایان|time|start|end)\\s*[:：]?\\s*"
                    + "(\\d{1,2})(?:\\s*[:٫.]\\s*([0-5]?\\d))?\\s*"
                    + "(صبح|بامداد|ظهر|عصر|شب|امشب|am|pm)?",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern TIME_RANGE = Pattern.compile(
            "(?<!\\d)(\\d{1,2})(?:\\s*[:٫.]\\s*([0-5]?\\d))?\\s*"
                    + "(?:تا|الی|لغایت|to|until|[-–—])\\s*"
                    + "(\\d{1,2})(?:\\s*[:٫.]\\s*([0-5]?\\d))?(?!\\d)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern RELATIVE_DATE_ANCHOR = Pattern.compile(
            "پس\\s*فردا|پس‌فردا|فردا|امروز|امشب|"
                    + "یکشنبه|دوشنبه|سه[\\s‌]شنبه|چهارشنبه|پنجشنبه|جمعه|شنبه");

    private static final String[] PERSIAN_MONTHS = {
            "فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور",
            "مهر","آبان","آذر","دی","بهمن","اسفند"
    };
    private static final Pattern PERSIAN_NAMED_DATE = Pattern.compile(
            "(?<!\\d)(\\d{1,2})\\s*(?:" + String.join("|", PERSIAN_MONTHS)
                    + ")(?:\\s+\\d{4})?(?!\\d)");

    private SmartAlarmParser() {}

    public static Result parse(Context context, String raw) {
        Result result = new Result();
        String text = normalize(raw);
        if (text.trim().isEmpty()) {
            result.warnings.add("متنی برای تحلیل وارد نشده است.");
            return result;
        }

        String trimmed = text.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                Object root = new JSONTokener(trimmed).nextValue();
                parseJsonRoot(context, root, result);
                result.jsonDetected = true;
                dedupe(result);
                if (result.candidates.isEmpty()) {
                    result.warnings.add("JSON معتبر بود، اما تاریخ و ساعت قابل استفاده پیدا نشد.");
                }
                return result;
            } catch (Exception ignored) {
                result.warnings.add("ساختار شبیه JSON بود ولی JSON معتبر نبود؛ به‌صورت متن عادی تحلیل شد.");
            }
        }

        parseNatural(context, text, result);
        dedupe(result);
        if (result.candidates.isEmpty()) {
            result.warnings.add(
                    "تاریخ و ساعت قابل تشخیص پیدا نشد. نمونه: ۱۴۰۵/۰۷/۰۴ ساعت ۱۳:۰۰");
        }
        return result;
    }

    private static void parseJsonRoot(Context context, Object root, Result result) {
        parseJsonAny(context, root, result, "");
    }

    private static void parseJsonAny(
            Context context,
            Object value,
            Result result,
            String labelHint) {
        if (value instanceof JSONArray) {
            JSONArray array = (JSONArray) value;
            for (int i = 0; i < array.length(); i++) {
                parseJsonAny(context, array.opt(i), result, labelHint);
            }
            return;
        }

        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            Candidate direct = candidateFromJson(context, object);
            if (direct != null) {
                if ("هشدار هوشمند".equals(direct.label)
                        && labelHint != null
                        && !labelHint.trim().isEmpty()) {
                    direct.label = cleanLabel(labelHint);
                }
                result.candidates.add(direct);
                return;
            }

            Iterator<String> keys = object.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                Object nested = object.opt(key);
                parseJsonAny(context, nested, result, key);
            }
            return;
        }

        if (value instanceof String) {
            String text = normalize((String) value).trim();
            if (text.isEmpty()) return;
            int before = result.candidates.size();
            parseNatural(context, text, result);
            if (result.candidates.size() > before
                    && labelHint != null
                    && !labelHint.trim().isEmpty()) {
                for (int i = before; i < result.candidates.size(); i++) {
                    Candidate candidate = result.candidates.get(i);
                    if ("هشدار هوشمند".equals(candidate.label)
                            || candidate.label.matches("[\\d\\s:./\\-–—]+")) {
                        candidate.label = cleanLabel(labelHint);
                    }
                }
            }
        }
    }

    private static void parseJsonArray(Context context, JSONArray array, Result result) {
        for (int i = 0; i < array.length(); i++) {
            Object value = array.opt(i);
            if (value instanceof JSONObject) {
                Candidate c = candidateFromJson(context, (JSONObject) value);
                if (c != null) result.candidates.add(c);
            } else if (value instanceof String) {
                parseNatural(context, normalize((String) value), result);
            }
        }
    }

    private static Candidate candidateFromJson(Context context, JSONObject o) {
        String date = firstString(o,
                "date", "startDate", "day", "تاریخ", "تاريخ");
        String time = firstString(o,
                "time", "start", "startTime", "from", "ساعت", "ساعت شروع", "شروع");
        String end = firstString(o,
                "end", "endTime", "to", "پایان", "ساعت پایان");

        if ((date == null || date.isEmpty()) && o.has("datetime")) {
            String dt = normalize(o.optString("datetime", ""));
            Matcher dm = DATE.matcher(dt);
            Matcher tm = TIME_COLON.matcher(dt);
            if (dm.find()) date = dm.group();
            if (tm.find()) time = tm.group();
        }

        if (date == null || time == null) return null;

        DateParts d = parseDate(context, date, contextHint(o.toString()));
        TimeParts t = parseTime(time);
        if (d == null || t == null) return null;

        Candidate c = new Candidate();
        c.calendarType = d.type;
        c.year = d.year;
        c.month = d.month;
        c.day = d.day;
        c.hour = t.hour;
        c.minute = t.minute;
        c.label = cleanLabel(firstString(o,
                "title", "label", "name", "reason", "description",
                "عنوان", "برچسب", "علت", "توضیحات"));
        if (c.label.isEmpty()) c.label = "هشدار هوشمند";
        c.source = o.toString();

        TimeParts e = parseTime(end);
        if (e != null) {
            c.endHour = e.hour;
            c.endMinute = e.minute;
        }

        int offset = firstInt(o, 0,
                "offsetMinutes", "offset", "minutes", "دقیقه", "فاصله");
        String mode = firstString(o,
                "offsetMode", "when", "mode", "زمان هشدار", "حالت");
        applyOffset(c, mode, offset);
        return c;
    }

    private static void parseNatural(Context context, String text, Result result) {
        String[] lines = text.split("[\\n\\r]+");
        for (String line : lines) {
            ArrayList<DateMatch> dates = new ArrayList<>();
            Matcher numeric = DATE.matcher(line);
            while (numeric.find()) {
                dates.add(new DateMatch(
                        numeric.start(), numeric.end(), numeric.group(), false));
            }
            Matcher anchors = RELATIVE_DATE_ANCHOR.matcher(line);
            while (anchors.find()) {
                dates.add(new DateMatch(
                        anchors.start(), anchors.end(), anchors.group(), true));
            }
            Matcher named = PERSIAN_NAMED_DATE.matcher(line);
            while (named.find()) {
                dates.add(new DateMatch(
                        named.start(), named.end(), named.group(), true));
            }
            dates.sort((a, b) -> Integer.compare(a.start, b.start));
            if (dates.isEmpty()) {
                DateParts relative = relativeDate(context, line);
                if (relative != null) addCandidatesForSegment(relative, line, result);
                continue;
            }
            int[] segmentStarts = new int[dates.size()];
            int[] segmentEnds = new int[dates.size()];
            segmentStarts[0] = 0;
            for (int i = 1; i < dates.size(); i++) {
                SegmentBoundary boundary = findSegmentBoundary(
                        line, dates.get(i - 1).end, dates.get(i).start);
                segmentEnds[i - 1] = boundary.previousEnd;
                segmentStarts[i] = boundary.nextStart;
            }
            segmentEnds[dates.size() - 1] = line.length();
            for (int i = 0; i < dates.size(); i++) {
                DateMatch match = dates.get(i);
                String segment = line.substring(
                        segmentStarts[i], segmentEnds[i]).trim();
                DateParts date = match.relative
                        ? relativeDate(context, segment)
                        : parseDate(context, match.raw, contextHint(segment));
                if (date != null) addCandidatesForSegment(date, segment, result);
            }
        }
    }

    private static SegmentBoundary findSegmentBoundary(
            String line, int searchStart, int nextDateStart) {
        String between = line.substring(searchStart, nextDateStart);
        Matcher separators = Pattern.compile("[،,؛;|]|\\s+و\\s+").matcher(between);
        int separatorSearchStart = 0;
        ArrayList<TimeHit> previousTimes = findTimes(between);
        if (!previousTimes.isEmpty()) {
            separatorSearchStart = previousTimes.get(previousTimes.size() - 1).end;
        }
        int separatorStart = -1;
        int separatorEnd = -1;
        while (separators.find()) {
            if (separators.start() < separatorSearchStart) continue;
            separatorStart = separators.start();
            separatorEnd = separators.end();
            break;
        }
        if (separatorStart < 0) {
            return new SegmentBoundary(nextDateStart, nextDateStart);
        }
        return new SegmentBoundary(
                searchStart + separatorStart,
                searchStart + separatorEnd);
    }

    private static void addCandidatesForSegment(
            DateParts date, String segment, Result result) {
        String afterDate = segment;
        Matcher dm = DATE.matcher(afterDate);
        if (dm.find()) afterDate = afterDate.substring(dm.end()).trim();

        ArrayList<TimeHit> hits = findTimes(afterDate);
        if (hits.isEmpty()) return;

        String label = deriveLabel(segment);
        if (label.isEmpty()) label = "هشدار هوشمند";

        for (int i = 0; i < hits.size(); i++) {
            TimeHit hit = hits.get(i);

            if (i > 0) {
                TimeHit previous = hits.get(i - 1);
                String between = afterDate.substring(
                        Math.min(previous.end, afterDate.length()),
                        Math.min(hit.start, afterDate.length()));
                if (looksLikeRangeSeparator(between)) continue;
            }

            Candidate c = new Candidate();
            c.calendarType = date.type;
            c.year = date.year;
            c.month = date.month;
            c.day = date.day;
            c.hour = hit.time.hour;
            c.minute = hit.time.minute;
            c.label = label;
            c.source = segment;

            if (i + 1 < hits.size()) {
                TimeHit next = hits.get(i + 1);
                String between = afterDate.substring(
                        Math.min(hit.end, afterDate.length()),
                        Math.min(next.start, afterDate.length()));
                if (looksLikeRangeSeparator(between)) {
                    c.endHour = next.time.hour;
                    c.endMinute = next.time.minute;
                }
            }

            OffsetParts offset = offsetFromText(segment);
            c.offsetMode = offset.mode;
            c.offsetMinutes = offset.minutes;
            result.candidates.add(c);
        }
    }

    private static ArrayList<TimeHit> findTimes(String text) {
        ArrayList<TimeHit> out = new ArrayList<>();

        Matcher range = TIME_RANGE.matcher(text);
        while (range.find()) {
            TimeParts first = new TimeParts(
                    safeInt(range.group(1), -1),
                    safeInt(range.group(2), 0));
            TimeParts second = new TimeParts(
                    safeInt(range.group(3), -1),
                    safeInt(range.group(4), 0));
            if (validTime(first) && validTime(second)) {
                int firstEnd = range.group(2) == null
                        ? range.end(1) : range.end(2);
                int secondEnd = range.group(4) == null
                        ? range.end(3) : range.end(4);
                out.add(new TimeHit(range.start(1), firstEnd, first));
                out.add(new TimeHit(range.start(3), secondEnd, second));
            }
        }

        Matcher colon = TIME_COLON.matcher(text);
        while (colon.find()) {
            if (overlaps(out, colon.start(), colon.end())) continue;
            TimeParts t = new TimeParts(
                    safeInt(colon.group(1), -1),
                    safeInt(colon.group(2), 0));
            if (validTime(t)) out.add(new TimeHit(colon.start(), colon.end(), t));
        }

        Matcher word = TIME_WORD.matcher(text);
        while (word.find()) {
            if (overlaps(out, word.start(), word.end())) continue;
            int hour = safeInt(word.group(1), -1);
            int minute = safeInt(word.group(2), 0);
            hour = applyDayPart(hour, word.group(3));
            TimeParts t = new TimeParts(hour, minute);
            if (validTime(t)) out.add(new TimeHit(word.start(), word.end(), t));
        }

        out.sort((a, b) -> Integer.compare(a.start, b.start));
        return out;
    }

    private static boolean overlaps(
            List<TimeHit> existing,
            int start,
            int end) {
        for (TimeHit hit : existing) {
            if (start < hit.end && end > hit.start) return true;
        }
        return false;
    }

    private static DateParts parseDate(Context context, String raw, int hintedType) {
        if (raw == null) return null;
        String value = normalize(raw);
        Matcher m = DATE.matcher(value);
        if (!m.find()) return null;

        int year = safeInt(m.group(1), -1);
        int month = safeInt(m.group(2), -1);
        int day = safeInt(m.group(3), -1);

        int type = hintedType;
        if (type < 0) {
            if (year >= 1700) {
                type = CalendarUtils.GREGORIAN;
            } else {
                int preferred = AppSettings.defaultCalendar(context);
                type = preferred == CalendarUtils.GREGORIAN
                        ? CalendarUtils.PERSIAN
                        : preferred;
            }
        }

        if (year < 100) {
            android.icu.util.Calendar now =
                    CalendarUtils.fromMillis(type, System.currentTimeMillis());
            int century = (now.get(android.icu.util.Calendar.YEAR) / 100) * 100;
            year = century + year;
        }

        if (month < 1 || month > 12 || day < 1 || day > 31) return null;
        try {
            long millis = CalendarUtils.toMillis(
                    type, year, month - 1, day, 12, 0);
            android.icu.util.Calendar check = CalendarUtils.fromMillis(type, millis);
            if (check.get(android.icu.util.Calendar.YEAR) != year
                    || check.get(android.icu.util.Calendar.MONTH) != month - 1
                    || check.get(android.icu.util.Calendar.DAY_OF_MONTH) != day) {
                return null;
            }
        } catch (Exception ignored) {
            return null;
        }

        return new DateParts(type, year, month, day);
    }

    private static DateParts relativeDate(Context context, String line) {
        String s = normalize(line).toLowerCase(Locale.ROOT);
        int addDays = Integer.MIN_VALUE;
        if (s.contains("پس فردا") || s.contains("پس‌فردا")) addDays = 2;
        else if (s.contains("فردا")) addDays = 1;
        else if (s.contains("امروز") || s.contains("امشب")) addDays = 0;

        Calendar c = Calendar.getInstance();
        if (addDays != Integer.MIN_VALUE) {
            c.add(Calendar.DAY_OF_YEAR, addDays);
            return fromMillis(context, c.getTimeInMillis());
        }

        String[] weekdays = {
                "یکشنبه","دوشنبه","سه شنبه","سه‌شنبه","چهارشنبه",
                "پنجشنبه","جمعه","شنبه"
        };
        int[] javaDays = {
                Calendar.SUNDAY,Calendar.MONDAY,Calendar.TUESDAY,Calendar.TUESDAY,
                Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY,Calendar.SATURDAY
        };
        for (int i = 0; i < weekdays.length; i++) {
            if (!s.contains(weekdays[i])) continue;
            int delta = (javaDays[i] - c.get(Calendar.DAY_OF_WEEK) + 7) % 7;
            if (delta == 0 && s.contains("بعد")) delta = 7;
            c.add(Calendar.DAY_OF_YEAR, delta);
            return fromMillis(context, c.getTimeInMillis());
        }

        for (int month = 0; month < PERSIAN_MONTHS.length; month++) {
            int pos = s.indexOf(PERSIAN_MONTHS[month]);
            if (pos < 0) continue;
            Matcher dayMatcher = Pattern.compile(
                    "(\\d{1,2})\\s*" + PERSIAN_MONTHS[month]
                            + "(?:\\s+(\\d{4}))?")
                    .matcher(s);
            if (!dayMatcher.find()) continue;

            android.icu.util.Calendar now = CalendarUtils.fromMillis(
                    CalendarUtils.PERSIAN, System.currentTimeMillis());
            int explicitYear = safeInt(dayMatcher.group(2), -1);
            int year = explicitYear > 0
                    ? explicitYear
                    : now.get(android.icu.util.Calendar.YEAR);
            int day = safeInt(dayMatcher.group(1), -1);
            long millis = CalendarUtils.toMillis(
                    CalendarUtils.PERSIAN, year, month, day, 12, 0);
            android.icu.util.Calendar check = CalendarUtils.fromMillis(
                    CalendarUtils.PERSIAN, millis);
            if (check.get(android.icu.util.Calendar.YEAR) != year
                    || check.get(android.icu.util.Calendar.MONTH) != month
                    || check.get(android.icu.util.Calendar.DAY_OF_MONTH) != day) {
                return null;
            }
            if (explicitYear <= 0
                    && millis < System.currentTimeMillis() - 86_400_000L) {
                year++;
            }
            return new DateParts(CalendarUtils.PERSIAN, year, month + 1, day);
        }

        return null;
    }

    private static DateParts fromMillis(Context context, long millis) {
        int type = AppSettings.defaultCalendar(context);
        android.icu.util.Calendar c = CalendarUtils.fromMillis(type, millis);
        return new DateParts(
                type,
                c.get(android.icu.util.Calendar.YEAR),
                c.get(android.icu.util.Calendar.MONTH) + 1,
                c.get(android.icu.util.Calendar.DAY_OF_MONTH));
    }

    private static TimeParts parseTime(String raw) {
        if (raw == null) return null;
        String value = normalize(raw);
        Matcher m = TIME_COLON.matcher(value);
        if (m.find()) {
            TimeParts t = new TimeParts(
                    safeInt(m.group(1), -1),
                    safeInt(m.group(2), 0));
            return validTime(t) ? t : null;
        }
        Matcher w = TIME_WORD.matcher(value);
        if (w.find()) {
            int hour = applyDayPart(
                    safeInt(w.group(1), -1), w.group(3));
            TimeParts t = new TimeParts(hour, safeInt(w.group(2), 0));
            return validTime(t) ? t : null;
        }
        Matcher plain = Pattern.compile("^\\s*(\\d{1,2})\\s*$").matcher(value);
        if (plain.find()) {
            TimeParts t = new TimeParts(safeInt(plain.group(1), -1), 0);
            return validTime(t) ? t : null;
        }
        return null;
    }

    private static OffsetParts offsetFromText(String text) {
        String s = normalize(text).toLowerCase(Locale.ROOT);
        Matcher m = Pattern.compile(
                "(\\d{1,3})\\s*دقیقه\\s*(قبل|پیش|بعد)")
                .matcher(s);
        if (m.find()) {
            int minutes = safeInt(m.group(1), 0);
            String direction = m.group(2);
            return new OffsetParts(
                    "بعد".equals(direction) ? OFFSET_AFTER : OFFSET_BEFORE,
                    minutes);
        }
        if (s.contains("سر وقت") || s.contains("سروقت")
                || s.contains("همان ساعت")) {
            return new OffsetParts(OFFSET_EXACT, 0);
        }
        return new OffsetParts(OFFSET_EXACT, 0);
    }

    private static void applyOffset(Candidate c, String modeRaw, int minutes) {
        String mode = normalize(modeRaw == null ? "" : modeRaw).toLowerCase(Locale.ROOT);
        c.offsetMinutes = Math.max(0, minutes);
        if (mode.contains("قبل") || mode.contains("before") || minutes < 0) {
            c.offsetMode = OFFSET_BEFORE;
            c.offsetMinutes = Math.abs(minutes);
        } else if (mode.contains("بعد") || mode.contains("after")) {
            c.offsetMode = OFFSET_AFTER;
        } else {
            c.offsetMode = OFFSET_EXACT;
            if (c.offsetMinutes == 0) return;
        }
    }

    private static int contextHint(String text) {
        String s = normalize(text).toLowerCase(Locale.ROOT);
        if (s.contains("قمری") || s.contains("هجری قمری")
                || s.contains("hijri") || s.contains("islamic")) {
            return CalendarUtils.HIJRI;
        }
        if (s.contains("میلادی") || s.contains("gregorian")
                || s.contains("iso")) {
            return CalendarUtils.GREGORIAN;
        }
        if (s.contains("شمسی") || s.contains("جلالی")
                || s.contains("jalali") || s.contains("persian")) {
            return CalendarUtils.PERSIAN;
        }
        return -1;
    }

    private static boolean looksLikeRangeSeparator(String value) {
        String s = normalize(value).trim().toLowerCase(Locale.ROOT);
        return s.matches(".*(?:تا|الی|لغایت|پایان|end|to|until|[-–—]).*");
    }

    private static int applyDayPart(int hour, String part) {
        if (hour < 0 || part == null) return hour;
        String p = normalize(part).toLowerCase(Locale.ROOT);
        boolean pm = p.equals("pm") || p.contains("عصر")
                || p.contains("شب") || p.contains("ظهر");
        boolean am = p.equals("am") || p.contains("صبح") || p.contains("بامداد");
        if (hour == 12 && p.contains("شب")) return 0;
        if (pm && hour < 12) hour += 12;
        if (am && hour == 12) hour = 0;
        return hour;
    }

    private static boolean validTime(TimeParts t) {
        return t != null && t.hour >= 0 && t.hour <= 23
                && t.minute >= 0 && t.minute <= 59;
    }

    private static String deriveLabel(String segment) {
        String s = normalize(segment);
        s = DATE.matcher(s).replaceAll(" ");
        s = PERSIAN_NAMED_DATE.matcher(s).replaceAll(" ");
        s = RELATIVE_DATE_ANCHOR.matcher(s).replaceAll(" ");
        s = TIME_RANGE.matcher(s).replaceAll(" ");
        s = TIME_COLON.matcher(s).replaceAll(" ");
        s = TIME_WORD.matcher(s).replaceAll(" ");
        s = s.replaceAll(
                "(?i)(ساعت|زمان|شروع|پایان|از|تا|الی|لغایت|تاریخ|date|time|start|end)\\s*[:：]?",
                " ");
        s = s.replaceAll("\\s+", " ").trim();
        s = s.replaceAll("^[\\-–—:،,؛;|]+|[\\-–—:،,؛;|]+$", "").trim();
        if (s.length() > 90) s = s.substring(0, 90).trim() + "…";
        return cleanLabel(s);
    }

    private static String cleanLabel(String value) {
        if (value == null) return "";
        String s = normalize(value).replaceAll("\\s+", " ").trim();
        return s.length() > 100 ? s.substring(0, 100).trim() + "…" : s;
    }

    private static void dedupe(Result result) {
        Set<String> seen = new HashSet<>();
        ArrayList<Candidate> unique = new ArrayList<>();
        for (Candidate c : result.candidates) {
            String key = c.calendarType + ":" + c.year + ":" + c.month + ":" + c.day
                    + ":" + c.hour + ":" + c.minute
                    + ":" + c.endHour + ":" + c.endMinute
                    + ":" + c.offsetMode + ":" + c.offsetMinutes
                    + ":" + c.label;
            if (seen.add(key)) unique.add(c);
        }
        result.candidates.clear();
        result.candidates.addAll(unique);
    }

    private static JSONArray firstArray(JSONObject o, String... keys) {
        for (String key : keys) {
            JSONArray value = o.optJSONArray(key);
            if (value != null) return value;
        }
        return null;
    }

    private static String firstString(JSONObject o, String... keys) {
        for (String key : keys) {
            if (!o.has(key) || o.isNull(key)) continue;
            Object value = o.opt(key);
            if (value == null) continue;
            String text = normalize(String.valueOf(value)).trim();
            if (!text.isEmpty() && !"null".equalsIgnoreCase(text)) return text;
        }
        return null;
    }

    private static int firstInt(JSONObject o, int fallback, String... keys) {
        for (String key : keys) {
            if (!o.has(key)) continue;
            Object value = o.opt(key);
            if (value instanceof Number) return ((Number) value).intValue();
            try {
                return Integer.parseInt(normalize(String.valueOf(value)).trim());
            } catch (Exception ignored) {}
        }
        return fallback;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        String out = input
                .replace('۰','0').replace('۱','1').replace('۲','2')
                .replace('۳','3').replace('۴','4').replace('۵','5')
                .replace('۶','6').replace('۷','7').replace('۸','8')
                .replace('۹','9')
                .replace('٠','0').replace('١','1').replace('٢','2')
                .replace('٣','3').replace('٤','4').replace('٥','5')
                .replace('٦','6').replace('٧','7').replace('٨','8')
                .replace('٩','9')
                .replace('ي','ی').replace('ك','ک')
                .replace("\u200f", "").replace("\u200e", "");
        return out;
    }

    private static int safeInt(String value, int fallback) {
        try {
            return value == null || value.trim().isEmpty()
                    ? fallback : Integer.parseInt(value.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static String faTime(int hour, int minute) {
        return CalendarUtils.fa(String.format(
                Locale.US, "%02d:%02d", hour, minute));
    }

    private static final class DateParts {
        final int type;
        final int year;
        final int month;
        final int day;

        DateParts(int type, int year, int month, int day) {
            this.type = type;
            this.year = year;
            this.month = month;
            this.day = day;
        }
    }

    private static final class TimeParts {
        final int hour;
        final int minute;

        TimeParts(int hour, int minute) {
            this.hour = hour;
            this.minute = minute;
        }
    }

    private static final class TimeHit {
        final int start;
        final int end;
        final TimeParts time;

        TimeHit(int start, int end, TimeParts time) {
            this.start = start;
            this.end = end;
            this.time = time;
        }
    }

    private static final class DateMatch {
        final int start;
        final int end;
        final String raw;
        final boolean relative;

        DateMatch(int start, int end, String raw, boolean relative) {
            this.start = start;
            this.end = end;
            this.raw = raw;
            this.relative = relative;
        }
    }

    private static final class SegmentBoundary {
        final int previousEnd;
        final int nextStart;

        SegmentBoundary(int previousEnd, int nextStart) {
            this.previousEnd = previousEnd;
            this.nextStart = nextStart;
        }
    }

    private static final class OffsetParts {
        final int mode;
        final int minutes;

        OffsetParts(int mode, int minutes) {
            this.mode = mode;
            this.minutes = minutes;
        }
    }
}
