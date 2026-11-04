package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.os.Looper;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class EventReminderRulesTest {
    final TimeZone zone=TimeZone.getTimeZone("Asia/Tehran");
    long date(TimeZone z,int year,int month,int day,int hour){Calendar c=Calendar.getInstance(z);c.clear();c.set(year,month-1,day,hour,0);return c.getTimeInMillis();}
    long date(int year,int month,int day,int hour){return date(zone,year,month,day,hour);}
    long calendarDate(int type,int year,int month,int day){
        android.icu.util.Calendar c=CalendarUtils.create(type);c.setTimeZone(android.icu.util.TimeZone.getTimeZone(zone.getID()));c.clear();c.set(year,month-1,day,10,0);return c.getTimeInMillis();
    }
    @Test public void thirtyDaysAreNotAcalendarMonthAndPersianMonthsFollowTheSelectedCalendar(){
        int type=CalendarUtils.PERSIAN;long esfand=calendarDate(type,1405,12,1);
        EventReminderRules.Rule days=new EventReminderRules.Rule(false,"day",30,false);
        assertEquals(30*86400000L,days.at(esfand,zone)-esfand);
        EventReminderRules.Rule month=new EventReminderRules.Rule(false,"month",1,false,type);
        assertEquals(calendarDate(type,1406,1,1),month.at(esfand,zone));assertNotEquals(month.at(esfand,zone),days.at(esfand,zone));
        EventReminderRules.Rule year=new EventReminderRules.Rule(false,"month",12,false,type);
        assertEquals(calendarDate(type,1406,12,1),year.at(esfand,zone));
        EventReminderRules.Rule before=new EventReminderRules.Rule(true,"month",1,false,type);
        assertEquals(esfand,before.at(calendarDate(type,1406,1,1),zone));
        EventReminderRules.Rule restored=EventReminderRules.parse(2,EventReminderRules.encode(java.util.Arrays.asList(year)),false).get(0);
        assertEquals(type,restored.calendarType);assertEquals(year.at(esfand,zone),restored.at(esfand,zone));
    }
    @Test public void hijriMonthsAndTwelveMonthsFollowLunarCalendarRatherThanThirtyDays(){
        int type=CalendarUtils.HIJRI,month=1;
        while(month<12 && CalendarUtils.daysInMonth(type,1448,month-1)!=29)month++;
        assertEquals(29,CalendarUtils.daysInMonth(type,1448,month-1));
        long start=calendarDate(type,1448,month,1),following=calendarDate(type,month==12?1449:1448,month==12?1:month+1,1);
        EventReminderRules.Rule lunarMonth=new EventReminderRules.Rule(false,"month",1,false,type);
        assertEquals(following,lunarMonth.at(start,zone));
        assertEquals(following+86400000L,new EventReminderRules.Rule(false,"day",30,false).at(start,zone));
        assertEquals(calendarDate(type,1449,month,1),new EventReminderRules.Rule(false,"month",12,false,type).at(start,zone));
    }
    @Test public void weeklyFridayRemindersRepeatOnThursdayEveryWeek(){
        EventReminderRules.Rule rule=new EventReminderRules.Rule(true,"day",1,false);
        long anchor=date(2027,1,8,10);
        long first=rule.next(anchor,RecurrenceUtils.WEEKLY,1,"[]",zone,date(2027,1,6,8));
        assertEquals(anchor,first);assertEquals(date(2027,1,7,10),rule.at(first,zone));
        long second=rule.next(anchor,RecurrenceUtils.WEEKLY,1,"[]",zone,rule.at(first,zone));
        assertEquals(date(2027,1,15,10),second);assertEquals(date(2027,1,14,10),rule.at(second,zone));
    }
    @Test public void offsetsLongerThanRepeatPeriodStillTargetTheCorrectOccurrence(){
        long anchor=date(2027,1,8,10),now=date(2027,1,25,10);
        EventReminderRules.Rule after=new EventReminderRules.Rule(false,"week",2,false);
        long previous=after.next(anchor,RecurrenceUtils.WEEKLY,1,"[]",zone,now);
        assertEquals(date(2027,1,15,10),previous);assertEquals(date(2027,1,29,10),after.at(previous,zone));
        EventReminderRules.Rule before=new EventReminderRules.Rule(true,"week",3,false);
        long future=before.next(anchor,RecurrenceUtils.WEEKLY,1,"[]",zone,now);
        assertEquals(date(2027,2,19,10),future);assertEquals(date(2027,1,29,10),before.at(future,zone));
    }
    @Test public void monthOffsetsAreCalendarMonthsAndDaysKeepLocalHourAcrossDst(){
        EventReminderRules.Rule month=new EventReminderRules.Rule(false,"month",1,false);
        assertEquals(date(2027,2,28,10),month.at(date(2027,1,31,10),zone));
        assertEquals(date(2028,2,29,10),month.at(date(2028,1,31,10),zone));
        TimeZone ny=TimeZone.getTimeZone("America/New_York");
        EventReminderRules.Rule day=new EventReminderRules.Rule(true,"day",1,false);
        long occurrence=date(ny,2027,3,14,9),before=day.at(occurrence,ny);
        assertEquals(date(ny,2027,3,13,9),before);assertEquals(23*3600000L,occurrence-before);
    }
    @Test public void irregularMinuteRepeatsFindTheEarliestReminderAcrossADstGap(){
        TimeZone ny=TimeZone.getTimeZone("America/New_York");
        long anchor=date(ny,2027,3,13,0);
        EventReminderRules.Rule rule=new EventReminderRules.Rule(false,"day",1,false);
        long now=date(ny,2027,3,14,3)+15*60000L;
        long expected=-1,earliest=Long.MAX_VALUE;
        for(int i=0;i<100;i++) {
            long occurrence=anchor+i*37*60000L,when=rule.at(occurrence,ny);
            if(when>now && when<earliest){earliest=when;expected=occurrence;}
        }
        assertEquals(expected,rule.next(anchor,RecurrenceUtils.INTERVAL_MINUTES,37,"[]",ny,now));
    }

    @Test public void customRulesAreBoundedDistinctAndRoundTripWithLegacyModes(){
        List<EventReminderRules.Rule> rules=new ArrayList<>();
        for(int i=1;i<=12;i++)rules.add(new EventReminderRules.Rule(i%2==0,"hour",i,true));
        rules.add(new EventReminderRules.Rule(true,"day",1,false));
        List<EventReminderRules.Rule> parsed=EventReminderRules.parse(2,EventReminderRules.encode(rules),false);
        assertEquals(11,parsed.size());
        assertEquals(0,EventReminderRules.parse(0,EventReminderRules.encode(rules),false).size());
        assertTrue(EventReminderRules.parse(2,"[60]",true).get(0).before);
        assertFalse(EventReminderRules.parse(2,"[60]",false).get(0).before);
        assertFalse(EventReminderRules.valid(new EventReminderRules.Rule(true,"month",0,true)));
    }
    @Test public void manyRulesShareOneSystemAlarmAndCoincidentRulesRingOnlyOnce()throws Exception {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        AlarmManager manager=app.getSystemService(AlarmManager.class);Shadows.shadowOf(manager).setCanScheduleExactAlarms(true);
        long now=System.currentTimeMillis();
        AlarmItem item=new AlarmItem(903,"Meeting",now+2*3600000L,0,true);
        item.reminderMode=2;item.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(
                new EventReminderRules.Rule(true,"minute",60,false),new EventReminderRules.Rule(true,"hour",1,false)));
        new AlarmStore(app).save(item);EventReminderScheduler.sync(app,item);
        assertEquals(1,Shadows.shadowOf(manager).getScheduledAlarms().size());
        long when=now+3600000L;
        for(String key:new String[]{"before_minute_60","before_hour_1"})
            EventReminderScheduler.dispatch(app,new Intent().putExtra("token","alarm/903/"+key)
                    .putExtra("occurrence",item.triggerAtMillis).putExtra("when",when),when);
        assertNotNull(Shadows.shadowOf(app).getNextStartedService());assertNull(Shadows.shadowOf(app).getNextStartedService());
        assertEquals(0,Shadows.shadowOf(manager).getScheduledAlarms().size());
    }
    @Test public void biweeklyAndMultimonthRepeatsApplyTheSameBeforeRule(){
        EventReminderRules.Rule before=new EventReminderRules.Rule(true,"day",1,false);
        long anchor=date(2027,1,8,10);
        assertEquals(date(2027,1,22,10),before.next(anchor,RecurrenceUtils.WEEKLY,1,RecurrenceUtils.periodJson(2),zone,date(2027,1,8,11)));
        assertEquals(date(2027,4,8,10),before.next(anchor,RecurrenceUtils.MONTHLY,1,RecurrenceUtils.periodJson(3),zone,date(2027,1,8,11)));
    }

    @Test public void autoDeleteCannotRemoveAnEventBeforeItsDueReminderIsDispatched(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);
        long when=System.currentTimeMillis();
        AlarmItem item=new AlarmItem(904,"Completed event",when-3600000L,0,false);
        item.lastFiredAtMillis=item.triggerAtMillis;item.autoDeleteAfterCompletion=true;
        item.reminderMode=2;item.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(new EventReminderRules.Rule(false,"hour",1,false)));
        AlarmStore store=new AlarmStore(app);store.save(item);EventReminderScheduler.sync(app,item,when-1000);
        assertTrue(AlarmCompletion.finished(item,when+1));
        assertEquals(0,store.deleteExpired(when+1));assertNotNull(store.find(item.id));
    }

    @Test public void completedNoteKeepsAfterReminderAndShowsOriginalEventTime()throws Exception {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);
        long due=System.currentTimeMillis()+86400000L;
        NoForgetItem note=new NoForgetItem(902,"Exam","","[]",4,true,due,true,System.currentTimeMillis());
        note.reminderMode=2;note.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(new EventReminderRules.Rule(false,"hour",8,false)));
        new NoForgetStore(app).save(note);EventReminderScheduler.sync(app,note);
        note.lastFiredAtMillis=due;note.reminderEnabled=false;new NoForgetStore(app).save(note);
        NoForgetItem restored=new NoForgetStore(app).find(note.id);
        assertEquals(due,restored.reminderAnchorMillis);assertEquals(due,restored.lastFiredAtMillis);
        EventReminderScheduler.sync(app,restored,due+1);
        String token="note/902/after_hour_8";
        JSONObject record=new JSONObject(app.getSharedPreferences("event_reminder_schedule_v1",0).getString(token,""));
        long when=record.getLong("when");assertEquals(due+8*3600000L,when);
        EventReminderScheduler.dispatch(app,new Intent().putExtra("token",token).putExtra("occurrence",due).putExtra("when",when),when);
        Intent service=Shadows.shadowOf(app).getNextStartedService();assertNotNull(service);
        assertEquals(due,service.getLongExtra(EventReminderScheduler.OCCURRENCE,0));
        assertEquals(due,new NoForgetStore(app).find(note.id).dueAtMillis);
    }

    @Test public void dispatchAdvancesOnlyItsRuleAndCancellationRejectsStaleDeliveries()throws Exception {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);
        long now=System.currentTimeMillis();
        AlarmItem item=new AlarmItem(901,"Weekly meeting",now+2*86400000L,RecurrenceUtils.WEEKLY,true);
        item.reminderMode=2;item.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(new EventReminderRules.Rule(true,"day",1,false)));
        new AlarmStore(app).save(item);EventReminderScheduler.sync(app,item);
        String token="alarm/901/before_day_1";
        JSONObject record=new JSONObject(app.getSharedPreferences("event_reminder_schedule_v1",0).getString(token,""));
        long firstOccurrence=record.getLong("occurrence"),when=record.getLong("when");
        // Rebuilding the primary schedule at the same instant must not skip the due rule.
        EventReminderScheduler.sync(app,item,when);
        assertEquals(firstOccurrence,new JSONObject(app.getSharedPreferences("event_reminder_schedule_v1",0).getString(token,"")).getLong("occurrence"));
        Intent intent=new Intent().putExtra("token",token).putExtra("occurrence",firstOccurrence).putExtra("when",when);
        EventReminderScheduler.dispatch(app,intent,when);
        Intent service=Shadows.shadowOf(app).getNextStartedService();assertNotNull(service);
        assertEquals(firstOccurrence,service.getLongExtra(EventReminderScheduler.OCCURRENCE,0));
        JSONObject next=new JSONObject(app.getSharedPreferences("event_reminder_schedule_v1",0).getString(token,""));
        assertTrue(next.getLong("occurrence")>firstOccurrence);
        EventReminderScheduler.dispatch(app,intent,when);assertNull(Shadows.shadowOf(app).getNextStartedService());
        EventReminderScheduler.cancel(app,"alarm",item.id);EventReminderScheduler.dispatch(app,intent,when);
        assertNull(Shadows.shadowOf(app).getNextStartedService());
    }
}
