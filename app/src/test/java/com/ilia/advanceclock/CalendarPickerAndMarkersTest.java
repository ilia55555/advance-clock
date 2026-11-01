package com.ilia.advanceclock;

import android.app.*;
import android.content.Context;
import java.util.Calendar;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class CalendarPickerAndMarkersTest {
    private long date(int day,int hour) {return CalendarUtils.toMillis(CalendarUtils.GREGORIAN,2027,0,day,hour,0);}

    @Test public void tabMarkersAreSeparateAndHighestPriorityWinsIncludingRepeats() {
        Context app=RuntimeEnvironment.getApplication();AppString.init(app);
        AlarmItem low=new AlarmItem(1,"Low",date(1,9),0,true);low.priority=0;
        AlarmItem high=new AlarmItem(2,"High",date(1,10),RecurrenceUtils.INTERVAL_DAYS,true);
        high.intervalDays=2;high.priority=4;
        new AlarmStore(app).save(low);new AlarmStore(app).save(high);
        NoForgetItem note=new NoForgetItem(3,"Note","","[]",2,true,date(2,11),false,System.currentTimeMillis());
        new NoForgetStore(app).save(note);
        CalendarDayMarkers alarms=new CalendarDayMarkers(app,false),notes=new CalendarDayMarkers(app,true);
        assertEquals(4,alarms.priority(date(1,0),date(2,0)));
        assertEquals(-1,alarms.priority(date(2,0),date(3,0)));
        assertEquals(4,alarms.priority(date(3,0),date(4,0)));
        assertEquals(-1,notes.priority(date(1,0),date(2,0)));
        assertEquals(2,notes.priority(date(2,0),date(3,0)));
        new AlarmStore(app).delete(high.id);
        assertEquals(0,new CalendarDayMarkers(app,false).priority(date(1,0),date(2,0)));
        assertNotEquals(CalendarDayMarkers.color(0),CalendarDayMarkers.color(4));
    }

    @Test public void nonRingingNotesHaveMarkersAndEachTabSwitchControlsOnlyItsOwnView() {
        Context app=RuntimeEnvironment.getApplication(); AppString.init(app);
        NoForgetItem note=new NoForgetItem(77,"Silent note","","[]",4,false,date(8,11),false,date(1,10));
        new NoForgetStore(app).save(note);
        TripleCalendarView notes=new TripleCalendarView(app);
        notes.setEventSource(true); notes.setCalendarType(CalendarUtils.GREGORIAN);
        notes.setSelectedMillis(date(8,11));
        AppSettings.setShowCalendarDayMarkers(app,true,true);
        assertEquals(4,dayPriority(notes,8));
        assertEquals(-1,dayPriority(notes,1));
        AppSettings.setShowCalendarDayMarkers(app,false,false);
        assertEquals(4,dayPriority(notes,8));
        AppSettings.setShowCalendarDayMarkers(app,true,false);
        assertEquals(-1,dayPriority(notes,8));
        AppSettings.setShowCalendarDayMarkers(app,true,true);
        assertEquals(4,dayPriority(notes,8));
        assertFalse(new NoForgetStore(app).all().get(0).reminderEnabled);
    }

    @Test public void legacyNotesUseCreationDayButUndatedNotesHaveNoMarker() {
        Context app=RuntimeEnvironment.getApplication(); AppString.init(app);
        NoForgetItem legacy=new NoForgetItem(88,"Old note","","[]",2,false,0,false,date(6,10));
        new NoForgetStore(app).save(legacy);
        assertEquals(2,new CalendarDayMarkers(app,true).priority(date(6,0),date(7,0)));
        legacy.applyDate=false;new NoForgetStore(app).save(legacy);
        assertEquals(-1,new CalendarDayMarkers(app,true).priority(date(6,0),date(7,0)));
    }

    private int dayPriority(TripleCalendarView view,int day) {
        return org.robolectric.util.ReflectionHelpers.callInstanceMethod(view,"priorityForDay",
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(int.class,day));
    }

    @Test public void eachEntryAddsOneDotAndOnlyTheTwoMostImportantRemain() {
        Context app=RuntimeEnvironment.getApplication(); AppString.init(app);
        NoForgetStore store=new NoForgetStore(app);
        CalendarDayMarkers empty=new CalendarDayMarkers(app,true);
        assertArrayEquals(new int[0],empty.priorities(date(12,0),date(13,0)));
        store.save(new NoForgetItem(101,"Medium","","[]",2,false,date(12,9),false,date(1,9)));
        assertArrayEquals(new int[]{2},new CalendarDayMarkers(app,true).priorities(date(12,0),date(13,0)));
        store.save(new NoForgetItem(102,"Very high","","[]",4,false,date(12,10),false,date(1,9)));
        assertArrayEquals(new int[]{4,2},new CalendarDayMarkers(app,true).priorities(date(12,0),date(13,0)));
        store.save(new NoForgetItem(103,"High","","[]",3,false,date(12,11),false,date(1,9)));
        assertArrayEquals(new int[]{4,3},new CalendarDayMarkers(app,true).priorities(date(12,0),date(13,0)));
        store.save(new NoForgetItem(104,"Another very high","","[]",4,false,date(12,12),false,date(1,9)));
        assertArrayEquals(new int[]{4,4},new CalendarDayMarkers(app,true).priorities(date(12,0),date(13,0)));
        assertArrayEquals(new int[0],new CalendarDayMarkers(app,true).priorities(date(13,0),date(14,0)));
        AlarmItem alarm=new AlarmItem(105,"Repeated",date(12,9),RecurrenceUtils.INTERVAL_MINUTES,true);
        alarm.intervalDays=5; alarm.priority=3;
        new AlarmStore(app).save(alarm);
        assertArrayEquals(new int[]{3},new CalendarDayMarkers(app,false).priorities(date(12,0),date(13,0)));
    }

    @Test public void calendarPickerSelectsDayPreservingTimeAndCancelDoesNotSave() {
        Context app=RuntimeEnvironment.getApplication();AppString.init(app);
        try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
            Activity host=controller.get();host.setTheme(R.style.Theme_AdvanceClock);
            long[] result={-1};
            CalendarPickerDialog.showDateAny(host,date(1,17),CalendarUtils.GREGORIAN,(millis,type)->result[0]=millis);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            TripleCalendarView calendar=dialog.findViewById(android.R.id.content).findViewWithTag("date_picker_calendar");
            assertNotNull(calendar);calendar.setSelectedMillis(date(8,0));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            assertEquals(date(8,17),result[0]);
            result[0]=-1;
            CalendarPickerDialog.showDateAny(host,date(1,17),CalendarUtils.GREGORIAN,(millis,type)->result[0]=millis);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(-1,result[0]);
        }
    }
}
