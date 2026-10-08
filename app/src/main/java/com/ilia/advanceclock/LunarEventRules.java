package com.ilia.advanceclock;

import java.util.ArrayList;
import java.util.List;

/** Recurring lunar dates shared by all event sources; never Gregorian year snapshots. */
final class LunarEventRules {
    static final int IRAN = 1, ARAB = 2, BOTH = IRAN | ARAB;

    static final class Rule {
        final int titleKey, month, day, sources;
        final boolean iranHoliday, arabHoliday;
        Rule(int key, int month, int day, int sources, boolean iranHoliday, boolean arabHoliday) {
            this.titleKey = key;
            this.month = month;
            this.day = day; // -1 means the last day, including a 29-day month
            this.sources = sources;
            this.iranHoliday = iranHoliday;
            this.arabHoliday = arabHoliday;
        }
        boolean holiday(int source) { return source == IRAN ? iranHoliday : arabHoliday; }
        boolean matches(int month, int day, int length, int source) {
            return (sources & source) != 0 && this.month == month
                    && day == (this.day == -1 ? length : this.day);
        }
    }

    static final Rule[] RULES = {
            new Rule(58, 7, 13, BOTH, true, false), // Imam Ali: birth
            new Rule(59, 7, 27, IRAN, true, false), // Mab'ath (Mi'raj has its own title)
            new Rule(60, 8, 15, BOTH, true, false), // Imam Mahdi: birth
            new Rule(61, 9, 21, BOTH, true, false), // Imam Ali: martyrdom
            new Rule(62, 10, 1, BOTH, true, true), // Eid al-Fitr
            new Rule(63, 10, 2, BOTH, true, true),
            new Rule(64, 10, 25, BOTH, true, false), // Imam Sadiq: martyrdom
            new Rule(65, 12, 10, BOTH, true, true), // Eid al-Adha
            new Rule(66, 12, 18, BOTH, true, false), // Ghadir
            new Rule(67, 1, 9, BOTH, true, false),
            new Rule(68, 1, 10, BOTH, true, false),
            new Rule(69, 2, 20, BOTH, true, false),
            new Rule(70, 2, 28, BOTH, true, false), // Prophet / Imam Hasan: passing
            new Rule(71, 2, -1, BOTH, true, false), // Imam Reza: last day of Safar
            new Rule(72, 3, 8, BOTH, true, false),
            new Rule(73, 3, 17, BOTH, true, false), // Shia Mawlid / Imam Sadiq: birth
            new Rule(74, 6, 3, BOTH, true, false),
            new Rule(75, 1, 1, ARAB, false, true),
            new Rule(76, 3, 12, ARAB, false, true), // Sunni Mawlid remains distinct
            new Rule(77, 7, 27, ARAB, false, false),
            new Rule(78, 9, 1, BOTH, false, false),
            new Rule(79, 9, 27, ARAB, false, false),
            new Rule(81, 10, 3, ARAB, false, true),
            new Rule(82, 12, 9, BOTH, false, true),
            new Rule(84, 12, 11, ARAB, false, true),
            new Rule(84, 12, 12, ARAB, false, true),
            new Rule(84, 12, 13, ARAB, false, true),
            new Rule(101, 9, 15, BOTH, false, false), // Imam Hasan: birth
            new Rule(102, 8, 3, BOTH, false, false), // Imam Hussein: birth
            new Rule(103, 8, 5, BOTH, false, false), // Imam Sajjad: birth
            new Rule(104, 1, 12, BOTH, false, false), // Imam Sajjad: martyrdom (one account)
            new Rule(105, 7, 1, BOTH, false, false), // Imam Baqir: birth
            new Rule(106, 12, 7, BOTH, false, false), // Imam Baqir: martyrdom
            new Rule(107, 12, 20, BOTH, false, false), // Imam Kazim: birth (Iran reference)
            new Rule(108, 7, 25, BOTH, false, false), // Imam Kazim: martyrdom
            new Rule(109, 11, 11, BOTH, false, false), // Imam Reza: birth
            new Rule(110, 7, 10, BOTH, false, false), // Imam Jawad: birth
            new Rule(111, 11, -1, BOTH, false, false), // Imam Jawad: last day of Dhu al-Qadah
            new Rule(112, 12, 15, BOTH, false, false), // Imam Hadi: birth
            new Rule(113, 7, 3, BOTH, false, false), // Imam Hadi: martyrdom
            new Rule(114, 4, 8, BOTH, false, false), // Imam Askari: birth
            new Rule(115, 6, 20, BOTH, false, false), // Fatima: birth
            new Rule(116, 9, 19, BOTH, false, false),
            new Rule(117, 9, 23, BOTH, false, false)
    };

    private LunarEventRules() {}

    static List<Rule> forDate(int month, int day, int monthLength, int source) {
        ArrayList<Rule> result = new ArrayList<>();
        for (Rule rule : RULES) {
            if (rule.matches(month, day, monthLength, source)) result.add(rule);
        }
        return result;
    }
}
