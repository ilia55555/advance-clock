package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import android.widget.*;
import java.util.*;
import java.time.Instant;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class WorldAlarmReferenceTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    private static long at(String utc){return Instant.parse(utc).toEpochMilli();}
    @Test public void legacyWorldAlarmsAreRecognizedButNewExplicitNonWorldOriginsArePreserved() throws Exception{
        AlarmItem legacy=new AlarmItem(4,"Old",1000,0,true);legacy.referenceZoneId="Asia/Tehran";
        org.json.JSONObject old=legacy.toJson();old.remove("createdFromWorld");
        assertTrue(AlarmItem.fromJson(old).createdFromWorld);
        assertFalse(AlarmItem.fromJson(legacy.toJson()).createdFromWorld);
        legacy.createdFromWorld=true;assertTrue(AlarmItem.fromJson(legacy.toJson()).createdFromWorld);
    }
    @Test public void offsetsIncludeFractionalHoursAndSeasonalDaylightSaving(){
        assertEquals("GMT +3:30",WorldAlarmReference.offset(TimeZone.getTimeZone("Asia/Tehran"),at("2026-06-01T00:00:00Z")));
        assertEquals("GMT +5:45",WorldAlarmReference.offset(TimeZone.getTimeZone("Asia/Kathmandu"),at("2026-06-01T00:00:00Z")));
        assertEquals("GMT -4:00",WorldAlarmReference.offset(TimeZone.getTimeZone("America/New_York"),at("2026-06-01T00:00:00Z")));
        assertEquals("GMT -5:00",WorldAlarmReference.offset(TimeZone.getTimeZone("America/New_York"),at("2026-12-01T00:00:00Z")));
    }
    @Test public void recurringAlarmKeepsCityWallTimeAcrossDstAndPersistsReference() throws Exception{
        AlarmItem alarm=new AlarmItem(7,"City",at("2026-03-07T14:00:00Z"),RecurrenceUtils.DAILY,true);
        alarm.referenceZoneId="America/New_York";
        AlarmItem restored=AlarmItem.fromJson(alarm.toJson());assertEquals(alarm.referenceZoneId,restored.referenceZoneId);
        assertEquals(at("2026-03-08T13:00:00Z"),AlarmCountdownUtils.nextOccurrence(restored,at("2026-03-07T14:01:00Z")));
        assertEquals(at("2026-03-08T13:00:00Z"),AlarmTimePolicy.next(at("2020-01-01T14:00:00Z"),false,
                RecurrenceUtils.NONE,1,"[]",at("2026-03-08T11:00:00Z"),restored.timeZone()));
    }
    @Test public void editorListsExactlySavedCitiesAndSavesCityInsteadOfPriority(){
        Application app=RuntimeEnvironment.getApplication();
        Shadows.shadowOf(app.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);
        WorldClockStore.save(app,Arrays.asList("Asia/Tehran","America/New_York"));
        WorldClockStore.saveLabel(app,"Asia/Tehran","اصفهان");WorldClockStore.saveLabel(app,"America/New_York","New York");
        Intent create=new Intent(app,AlarmEditorActivity.class).putExtra("worldTimeReference",true);
        long id;
        try(var controller=Robolectric.buildActivity(AlarmEditorActivity.class,create).setup().visible()){
            AlarmEditorActivity host=controller.get();Spinner reference=host.findViewById(R.id.alarm_priority);
            assertEquals(2,reference.getCount());assertTrue(reference.getItemAtPosition(0).toString().contains("اصفهان"));
            assertTrue(reference.getItemAtPosition(0).toString().contains("+3:30"));
            reference.setSelection(1);reference.getOnItemSelectedListener().onItemSelected(reference,null,1,1);
            Calendar selected=ReflectionHelpers.getField(host,"selected");
            assertEquals("America/New_York",selected.getTimeZone().getID());
            selected.setTimeInMillis(System.currentTimeMillis()+3*86_400_000L);selected.set(Calendar.SECOND,0);selected.set(Calendar.MILLISECOND,0);
            DateOption date=ReflectionHelpers.getField(host,"dateOption");date.setApplied(true);
            host.findViewById(R.id.save_alarm).performClick();
            AlarmItem saved=new AlarmStore(app).all().get(0);id=saved.id;
            assertTrue(saved.createdFromWorld);
            assertEquals("America/New_York",saved.referenceZoneId);assertEquals(selected.getTimeInMillis(),saved.triggerAtMillis);
            assertEquals(PriorityUtils.MEDIUM,saved.priority);
        }
        try(var controller=Robolectric.buildActivity(AlarmEditorActivity.class,new Intent(app,AlarmEditorActivity.class).putExtra("alarmId",id)).setup()){
            Spinner reference=controller.get().findViewById(R.id.alarm_priority);
            assertEquals(2,reference.getCount());assertEquals(1,reference.getSelectedItemPosition());
        }
    }
    @Test public void noteDeletionRequiresSecondTapAndArmingAnotherNoteCancelsFirst(){
        Application app=RuntimeEnvironment.getApplication();
        NoForgetItem first=new NoForgetItem(1,"One","","[]",2,false,0,false,0);
        NoForgetItem second=new NoForgetItem(2,"Two","","[]",2,false,0,false,0);
        new NoForgetStore(app).save(first);new NoForgetStore(app).save(second);
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()){
            MainActivity host=controller.get();
            LinearLayout a=ReflectionHelpers.callInstanceMethod(host,"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,first));
            LinearLayout b=ReflectionHelpers.callInstanceMethod(host,"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,second));
            ImageButton removeA=(ImageButton)((LinearLayout)a.getChildAt(a.getChildCount()-1)).getChildAt(0);
            ImageButton removeB=(ImageButton)((LinearLayout)b.getChildAt(b.getChildCount()-1)).getChildAt(0);
            removeA.performClick();assertTrue(removeA.isSelected());assertNotNull(new NoForgetStore(app).find(1));
            removeB.performClick();assertFalse(removeA.isSelected());assertTrue(removeB.isSelected());
            removeA.performClick();assertNotNull(new NoForgetStore(app).find(1));removeA.performClick();
            assertNull(new NoForgetStore(app).find(1));assertNotNull(new NoForgetStore(app).find(2));
        }
    }
}
