package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=35)
public class SmartScheduleParserTest {
    private SmartAlarmParser.Result parse(String text) {
        Context context=RuntimeEnvironment.getApplication();AppString.init(context);
        return SmartAlarmParser.parse(context,text);
    }
    private SmartAlarmParser.Candidate one(String text) {
        SmartAlarmParser.Result result=parse(text);
        assertEquals(text+" warnings="+result.warnings,1,result.candidates.size());
        return result.candidates.get(0);
    }
    private void dateAfter(SmartAlarmParser.Candidate candidate,int days) {
        Calendar expected=Calendar.getInstance();expected.add(Calendar.DAY_OF_YEAR,days);
        Calendar actual=Calendar.getInstance();actual.setTimeInMillis(candidate.baseMillis());
        assertEquals(expected.get(Calendar.YEAR),actual.get(Calendar.YEAR));
        assertEquals(expected.get(Calendar.DAY_OF_YEAR),actual.get(Calendar.DAY_OF_YEAR));
    }
    @Test public void tomorrowAndDayAfterTomorrow() {
        dateAfter(one("فردا ساعت 08:30 دارو"),1);
        dateAfter(one("پس فردا ساعت 08:30 دارو"),2);
    }
    @Test public void numericAndSpokenRelativeDays() {
        dateAfter(one("۱۰ روز بعد ساعت 08:30 دارو"),10);
        dateAfter(one("ده روز دیگر ساعت هشت و نیم صبح دارو"),10);
        dateAfter(one("بعد از بیست و سه روز ساعت 08:30 دارو"),23);
    }
    @Test public void spokenTimesAndDayParts() {
        SmartAlarmParser.Candidate c=one("فردا ساعت هشت و نیم شب دارو");assertEquals(20,c.hour);assertEquals(30,c.minute);
        c=one("فردا ساعت یک ربع به نه صبح دارو");assertEquals(8,c.hour);assertEquals(45,c.minute);
        c=one("فردا ساعت 12 شب دارو");assertEquals(0,c.hour);
    }
    @Test public void weekdayListRepeatsInsteadOfBeingLost() {
        SmartAlarmParser.Candidate c=one("در روزهای شنبه و یکشنبه ساعت 08:30 میخوام هشدار داشته باشم");
        assertEquals(RecurrenceUtils.WEEKDAYS,c.recurrenceMode);
        assertTrue(RecurrenceUtils.isPermanentWeekdays(c.customDatesJson));
        assertEquals(new HashSet<>(Arrays.asList(Calendar.SATURDAY,Calendar.SUNDAY)),new HashSet<>(RecurrenceUtils.parseWeekdays(c.customDatesJson)));
        assertTrue(c.baseMillis()>System.currentTimeMillis());
    }
    @Test public void weekdayRangesIncludeMiddleDays() {
        SmartAlarmParser.Candidate c=one("هر شنبه تا چهارشنبه ساعت 08:30 کار");
        assertEquals(5,RecurrenceUtils.parseWeekdays(c.customDatesJson).size());
    }
    @Test public void eachDayCanHaveItsOwnTime() {
        SmartAlarmParser.Result result=parse("شنبه ساعت 08:00 و یکشنبه ساعت 09:30 کلاس");
        assertEquals(2,result.candidates.size());assertEquals(8,result.candidates.get(0).hour);assertEquals(9,result.candidates.get(1).hour);
        for(SmartAlarmParser.Candidate c:result.candidates)assertEquals(RecurrenceUtils.WEEKDAYS,c.recurrenceMode);
    }
    @Test public void intervalDaysAndWeeksPersist() {
        SmartAlarmParser.Candidate c=one("هر ده روز یکبار ساعت 08:30 دارو");
        assertEquals(RecurrenceUtils.INTERVAL_DAYS,c.recurrenceMode);assertEquals(10,c.intervalDays);dateAfter(c,10);
        c=one("هر ۲ هفته ساعت 08:30 کار");assertEquals(14,c.intervalDays);
    }
    @Test public void dailyMonthlyYearlyRulesAndEveryTwoMonths() {
        assertEquals(RecurrenceUtils.DAILY,one("هر روز ساعت 08:30 دارو").recurrenceMode);
        assertEquals(RecurrenceUtils.MONTHLY,one("هر ماه ساعت 08:30 قبض").recurrenceMode);
        assertEquals(RecurrenceUtils.YEARLY,one("هر سال ساعت 08:30 تولد").recurrenceMode);
        SmartAlarmParser.Candidate c=one("هر دو ماه ساعت 08:30 قبض");
        Calendar base=Calendar.getInstance();base.setTimeInMillis(c.baseMillis());
        Calendar expected=(Calendar)base.clone();expected.add(Calendar.MONTH,2);
        assertEquals(expected.getTimeInMillis(),RecurrenceUtils.next(c.baseMillis(),c.recurrenceMode,c.intervalDays,c.customDatesJson,c.baseMillis()+1000));
    }
    @Test public void relativeMinutesRequireNoInventedClockTime() {
        SmartAlarmParser.Candidate c=one("بیست دقیقه دیگر چای");
        long delta=c.baseMillis()-System.currentTimeMillis();assertTrue(delta>19*60000&&delta<=20*60000);
    }
    @Test public void multipleDatesMayShareATime() {assertEquals(2,parse("فردا و پس فردا ساعت 08:30 دارو").candidates.size());}
    @Test public void multipleClockTimesRemainSeparate() {assertEquals(2,parse("فردا ساعت 08:30 و 20:30 دارو").candidates.size());}
    @Test public void missingOrInvalidTimesAndIntervalsDoNotGuess() {
        for(String text:new String[]{"فردا دارو","هر 10 روز دارو","فردا ساعت 25:00 دارو","فردا ساعت 08:99 دارو","هر 0 روز ساعت 08:00"}){
            SmartAlarmParser.Result result=parse(text);assertTrue(text,result.candidates.isEmpty());assertFalse(result.warnings.isEmpty());
        }
    }
    @Test public void explicitNumericAndNamedDatesKeepWorking() {
        assertEquals(1,parse("جلسه 2027/01/25 ساعت 08:30").candidates.size());
        assertEquals(1,parse("جلسه ۲۵ مهر ۱۴۰۶ ساعت 08:30").candidates.size());
    }
    @Test public void relativeDelayDoesNotApplyTheSameOffsetTwice() {
        SmartAlarmParser.Candidate c=one("20 دقیقه بعد چای");
        long delta=c.triggerMillis()-System.currentTimeMillis();assertTrue(delta>19*60000&&delta<=20*60000);
    }
    @Test public void pluralWeekdaysAndAlternatingDaysRepeat() {
        assertEquals(RecurrenceUtils.WEEKDAYS,one("شنبه‌ها ساعت 08:30 کار").recurrenceMode);
        SmartAlarmParser.Candidate c=one("یک روز در میان ساعت 08:30 دارو");
        assertEquals(RecurrenceUtils.INTERVAL_DAYS,c.recurrenceMode);assertEquals(2,c.intervalDays);
    }
    @Test public void minuteAndHourRepetitionAreRealRepeatingSchedules() {
        SmartAlarmParser.Candidate c=one("هر 10 دقیقه دارو");
        assertEquals(RecurrenceUtils.INTERVAL_MINUTES,c.recurrenceMode);assertEquals(10,c.intervalDays);
        long delta=c.triggerMillis()-System.currentTimeMillis();assertTrue(delta>9*60000&&delta<=10*60000);
        assertEquals(c.triggerMillis()+10*60000,RecurrenceUtils.next(c.triggerMillis(),c.recurrenceMode,c.intervalDays,c.customDatesJson,c.triggerMillis()+1));
        c=one("هر دو ساعت دارو");assertEquals(120,c.intervalDays);
    }
    @Test public void recurrenceMovesPastExplicitDatesToTheNextOccurrence() {
        SmartAlarmParser.Candidate c=one("هر روز جلسه 2020/01/25 ساعت 08:30");
        assertEquals(RecurrenceUtils.DAILY,c.recurrenceMode);assertTrue(c.triggerMillis()>System.currentTimeMillis());
    }

    @Test public void dottedDateIsNotMistakenForClockTime() {
        assertTrue(parse("جلسه 2027.01.25").candidates.isEmpty());
        assertEquals(1,parse("جلسه 2027.01.25 ساعت 08:30").candidates.size());
    }
    @Test public void numericMinuteShorthandWorks() {
        SmartAlarmParser.Candidate c=one("فردا ساعت 8:5 دارو");assertEquals(8,c.hour);assertEquals(5,c.minute);
    }

}
