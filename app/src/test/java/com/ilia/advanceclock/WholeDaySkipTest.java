package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class WholeDaySkipTest {
    Context c;long now,end;TimeZone zone;
    @Before public void setup(){
        c=RuntimeEnvironment.getApplication();AppString.init(c);now=System.currentTimeMillis();zone=AppTime.zone();
        end=OccurrenceSkips.dayWindow(now,zone)[1];
        Shadows.shadowOf(c.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);
    }
    @Test public void oneTapSkipsEveryExplicitRepeatTodayButPreservesTomorrowAfterRestore(){
        long first=now+(end-now)/4,second=now+(end-now)/2,tomorrow=end+3600000;
        AlarmItem item=new AlarmItem(801,"Finite",first,RecurrenceUtils.INTERVAL_MINUTES,true);
        item.customDatesJson=RecurrenceUtils.intervalDatesJson(Arrays.asList(second,tomorrow));new AlarmStore(c).save(item);
        assertTrue(OccurrenceSkips.skip(c,"alarm",item.id,now));
        AlarmItem saved=new AlarmStore(c).find(item.id);assertTrue(saved.enabled);assertEquals(tomorrow,saved.triggerAtMillis);
        assertTrue(OccurrenceSkips.contains(c,"alarm",item.id,first));assertTrue(OccurrenceSkips.contains(c,"alarm",item.id,second));
        assertFalse(OccurrenceSkips.contains(c,"alarm",item.id,tomorrow));
        AlarmScheduler.rescheduleAll(c);assertEquals(tomorrow,new AlarmStore(c).find(item.id).triggerAtMillis);
        assertFalse(OccurrenceSkips.skip(c,"alarm",item.id,now));
    }
    @Test public void beforeAndAfterRulesFilterBothEventDayAndActualReminderDay() {
        long today=now+(end-now)/2;
        AlarmItem item=new AlarmItem(802,"Daily",today,RecurrenceUtils.DAILY,true);new AlarmStore(c).save(item);
        assertTrue(OccurrenceSkips.skip(c,"alarm",item.id,now));
        EventReminderRules.Rule before=new EventReminderRules.Rule(true,"day",1,false);
        EventReminderRules.Rule after=new EventReminderRules.Rule(false,"day",1,false);
        long nextBefore=OccurrenceSkips.nextReminder(c,"alarm",item.id,before,today,RecurrenceUtils.DAILY,1,"[]",zone,now);
        assertTrue(before.at(nextBefore,zone)>=end);assertFalse(OccurrenceSkips.contains(c,"alarm",item.id,nextBefore));
        // Even tomorrow's post reminder for today's event is suppressed.
        long nextAfter=OccurrenceSkips.nextReminder(c,"alarm",item.id,after,today,RecurrenceUtils.DAILY,1,"[]",zone,now);
        assertTrue(nextAfter>=end);assertFalse(OccurrenceSkips.reminderBlocked(c,"alarm",item.id,nextAfter,after.at(nextAfter,zone)));
        assertTrue(OccurrenceSkips.reminderBlocked(c,"alarm",item.id,today,after.at(today,zone)));
    }
    @Test public void richReminderRecordsAdvancePastTodayAndStaleDeliveryCannotRing() throws Exception {
        long today=now+(end-now)/2;
        AlarmItem item=new AlarmItem(803,"Rules",today,RecurrenceUtils.DAILY,true);
        item.reminderMode=2;item.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(
                new EventReminderRules.Rule(true,"minute",1,false),new EventReminderRules.Rule(false,"hour",1,false),new EventReminderRules.Rule(true,"day",1,false)));
        new AlarmStore(c).save(item);EventReminderScheduler.sync(c,item,now);
        String key="alarm/803/before_minute_1";JSONObject old=new JSONObject(c.getSharedPreferences("event_reminder_schedule_v1",0).getString(key,""));
        assertTrue(OccurrenceSkips.skip(c,"alarm",item.id,now));
        for(Object raw:c.getSharedPreferences("event_reminder_schedule_v1",0).getAll().values()){
            JSONObject record=new JSONObject(raw.toString());assertTrue(record.getLong("when")>=end);
            assertFalse(OccurrenceSkips.contains(c,"alarm",item.id,record.getLong("occurrence")));
        }
        EventReminderScheduler.dispatch(c,new Intent().putExtra("token",key).putExtra("occurrence",old.getLong("occurrence")).putExtra("when",old.getLong("when")),old.getLong("when"));
        assertNull(Shadows.shadowOf((Application)c).getNextStartedService());
    }
    @Test public void snoozesForOtherDaysAndOtherItemsRemainAndDayStateCanBeCleared() throws Exception {
        AlarmItem item=new AlarmItem(804,"Daily",now+(end-now)/2,RecurrenceUtils.DAILY,true);new AlarmStore(c).save(item);
        android.content.SharedPreferences p=c.getSharedPreferences(AlertSnoozes.PREFS,0);
        p.edit().putString("today",new JSONObject().put("kind","alarm").put("id",804).put("when",now+1).toString())
                .putString("tomorrow",new JSONObject().put("kind","alarm").put("id",804).put("when",end+1).toString())
                .putString("other",new JSONObject().put("kind","alarm").put("id",805).put("when",now+1).toString()).commit();
        assertTrue(OccurrenceSkips.skip(c,"alarm",804,now));assertFalse(p.contains("today"));assertTrue(p.contains("tomorrow"));assertTrue(p.contains("other"));
        OccurrenceSkips.clear(c,"alarm",804);assertFalse(OccurrenceSkips.dayContains(c,"alarm",804,now));
    }
    @Test public void pauseButtonCanEnableTodayAgainAndRestoreRemainingClockAndWorldOccurrences(){
        for(int i=0;i<2;i++){
            TimeZone z=i==0?zone:TimeZone.getTimeZone("America/New_York");long finish=OccurrenceSkips.dayWindow(now,z)[1];
            long first=now+(finish-now)/4,later=now+(finish-now)/2;
            AlarmItem item=new AlarmItem(810+i,"Restore",first,RecurrenceUtils.INTERVAL_MINUTES,true);
            item.customDatesJson=RecurrenceUtils.intervalDatesJson(Collections.singletonList(later));item.referenceZoneId=z.getID();item.createdFromWorld=i==1;new AlarmStore(c).save(item);
            assertTrue(OccurrenceSkips.skip(c,"alarm",item.id,now));assertFalse(new AlarmStore(c).find(item.id).enabled);
            try(var host=Robolectric.buildActivity(Activity.class).setup().visible()){
                android.widget.Button button=OccurrenceSkips.button(host.get(),"alarm",item.id,first,item.recurrenceMode,false,z,()->{});
                assertTrue(button.isEnabled());assertEquals(c.getString(R.string.enable_today_occurrence),button.getText().toString());
            }
            // The first time passed while paused. Only the remaining occurrence returns.
            assertTrue(OccurrenceSkips.enableToday(c,"alarm",item.id,first+1));AlarmItem saved=new AlarmStore(c).find(item.id);
            assertTrue(saved.enabled);assertEquals(later,saved.triggerAtMillis);assertFalse(OccurrenceSkips.dayContains(c,"alarm",item.id,now));
        }
    }
    @Test public void enablingNoteTodayRestoresPrimaryAndRichRemindersWithoutReplayingPastTimes(){
        long first=now+(end-now)/4,later=now+(end-now)/2;
        NoForgetItem item=new NoForgetItem(812,"Restore note","Body","[]",2,true,first,true,now,RecurrenceUtils.INTERVAL_MINUTES,3,
                RecurrenceUtils.intervalDatesJson(Collections.singletonList(later)));
        item.reminderMode=2;item.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(new EventReminderRules.Rule(false,"minute",1,false)));
        new NoForgetStore(c).save(item);assertTrue(OccurrenceSkips.skip(c,"note",item.id,now));assertFalse(new NoForgetStore(c).find(item.id).reminderEnabled);
        assertTrue(OccurrenceSkips.enableToday(c,"note",item.id,first+1));NoForgetItem saved=new NoForgetStore(c).find(item.id);
        assertTrue(saved.reminderEnabled);assertEquals(later,saved.dueAtMillis);
        EventReminderScheduler.sync(c,saved,first+1);assertTrue(EventReminderScheduler.hasPending(c,"note",item.id,first+1));
        assertFalse(OccurrenceSkips.enableToday(c,"note",item.id,first+1));
    }

    @Test public void worldDaysUseLocalMidnightAndRespectDstRatherThanFixed24Hours(){
        TimeZone ny=TimeZone.getTimeZone("America/New_York");Calendar day=Calendar.getInstance(ny);day.clear();day.set(2026,Calendar.MARCH,8,12,0);
        long[] spring=OccurrenceSkips.dayWindow(day.getTimeInMillis(),ny);assertEquals(23*3600000L,spring[1]-spring[0]);
        day.set(2026,Calendar.NOVEMBER,1,12,0);long[] autumn=OccurrenceSkips.dayWindow(day.getTimeInMillis(),ny);assertEquals(25*3600000L,autumn[1]-autumn[0]);
    }
}
