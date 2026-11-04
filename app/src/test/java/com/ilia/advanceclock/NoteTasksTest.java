package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.graphics.Paint;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class NoteTasksTest {
    Context c;long now;
    @Before public void init(){c=RuntimeEnvironment.getApplication();AppString.init(c);now=System.currentTimeMillis();Shadows.shadowOf(c.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true); }
    NoForgetItem task(long id,int repeat){
        NoForgetItem item=new NoForgetItem(id,"Task","Details","[]",2,true,now+2*86400000L,true,now,repeat,1,"[]");
        item.taskMode=true;item.reminderMode=2;
        item.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(new EventReminderRules.Rule(true,"day",1,false),new EventReminderRules.Rule(false,"hour",8,false)));
        new NoForgetStore(c).save(item);NoForgetScheduler.schedule(c,item);return item;
    }
    @Test public void oneShotCompletionCancelsAllRemindersAndUndoRestoresFutureAlarm(){
        NoForgetItem item=task(101,RecurrenceUtils.NONE);
        NoForgetItem done=NoteTasks.setCompleted(c,item.id,true,now);
        assertTrue(done.taskDone(now));assertFalse(done.reminderEnabled);
        assertFalse(EventReminderScheduler.hasPending(c,"note",item.id,now));
        NoForgetItem persisted=new NoForgetStore(c).find(item.id);assertTrue(persisted.taskCompleted);assertEquals("Details",persisted.body);
        NoForgetScheduler.rescheduleAll(c);assertFalse(EventReminderScheduler.hasPending(c,"note",item.id,now));
        NoForgetItem undone=NoteTasks.setCompleted(c,item.id,false,now);assertFalse(undone.taskDone(now));assertTrue(undone.reminderEnabled);
        assertEquals(item.dueAtMillis,undone.dueAtMillis);assertTrue(EventReminderScheduler.hasPending(c,"note",item.id,now));
    }
    @Test public void weeklyCompletionSkipsOnlyOneOccurrenceAndKeepsNextWeeksReminders(){
        NoForgetItem item=task(102,RecurrenceUtils.WEEKLY);long original=item.dueAtMillis;
        NoForgetItem done=NoteTasks.setCompleted(c,item.id,true,now);
        assertTrue(done.taskDone(now));assertEquals(original,done.taskCompletedOccurrence);assertTrue(done.reminderEnabled);
        assertEquals(original+7*86400000L,done.dueAtMillis);assertTrue(done.completedOccurrence(original));assertFalse(done.completedOccurrence(done.dueAtMillis));
        assertTrue(EventReminderScheduler.hasPending(c,"note",item.id,now));
        NoteTasks.reopenForOccurrence(c,done,done.dueAtMillis-86400000L);assertFalse(new NoForgetStore(c).find(item.id).taskCompleted);
        assertTrue(new NoForgetStore(c).find(item.id).completedOccurrence(original));
        for(Object value:c.getSharedPreferences("event_reminder_schedule_v1",0).getAll().values())try{
            assertTrue(new org.json.JSONObject(value.toString()).getLong("occurrence")>original);
        }catch(org.json.JSONException e){throw new AssertionError(e);}
    }
    @Test public void undoBeforeOriginalDueRestoresOriginalWeeklyOccurrence(){
        NoForgetItem item=task(103,RecurrenceUtils.WEEKLY);long original=item.dueAtMillis;
        NoteTasks.setCompleted(c,item.id,true,now);NoForgetItem undone=NoteTasks.setCompleted(c,item.id,false,now);
        assertEquals(original,undone.dueAtMillis);assertFalse(undone.taskCompleted);assertTrue(undone.reminderEnabled);
    }
    @Test public void completingAfterRingDoesNotSkipTheAlreadyScheduledNextOccurrence(){
        NoForgetItem item=task(104,RecurrenceUtils.WEEKLY);long next=item.dueAtMillis;
        item.lastFiredAtMillis=now-10000;new NoForgetStore(c).save(item);
        NoForgetItem done=NoteTasks.setCompleted(c,item.id,true,now);
        assertEquals(next,done.dueAtMillis);assertEquals(now-10000,done.taskCompletedOccurrence);assertTrue(done.reminderEnabled);
    }
    @Test public void silentTasksAndOrdinaryNotesKeepTheirSemantics(){
        NoForgetItem note=task(105,RecurrenceUtils.NONE);note.taskMode=false;new NoForgetStore(c).save(note);
        NoteTasks.setCompleted(c,note.id,true,now);assertFalse(new NoForgetStore(c).find(note.id).taskCompleted);
        note.taskMode=true;note.hasDue=false;note.reminderEnabled=false;new NoForgetStore(c).save(note);
        NoteTasks.setCompleted(c,note.id,true,now);assertTrue(new NoForgetStore(c).find(note.id).taskDone(now));
        NoteTasks.setCompleted(c,note.id,false,now);assertFalse(new NoForgetStore(c).find(note.id).reminderEnabled);
    }
    @Test public void completedTaskCannotRingFromAStaleBroadcast(){
        NoForgetItem item=task(106,RecurrenceUtils.NONE);NoteTasks.setCompleted(c,item.id,true,now);
        new NoForgetReminderReceiver().onReceive(c,new Intent(c,NoForgetReminderReceiver.class).putExtra("noteId",item.id));
        assertNull(Shadows.shadowOf((Application)c).getNextStartedService());
    }
    @Test public void checkboxAppearsOnlyForTasksAndTogglesStoredCompletion(){
        NoForgetItem item=task(107,RecurrenceUtils.NONE);
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            MainActivity host=controller.get();
            LinearLayout card=ReflectionHelpers.callInstanceMethod(host,"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,item));
            CheckBox box=card.findViewWithTag("task_check_107");assertNotNull(box);assertFalse(box.isChecked());
            box.setChecked(true);assertTrue(new NoForgetStore(c).find(item.id).taskCompleted);
            box.setChecked(false);assertFalse(new NoForgetStore(c).find(item.id).taskCompleted);
            item.taskMode=false;LinearLayout ordinary=ReflectionHelpers.callInstanceMethod(host,"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,item));assertNull(ordinary.findViewWithTag("task_check_107"));
        }
    }
    @Test public void noteAlarmButtonDisablesAllRemindersAndReactivatesTheSavedAlarm(){
        NoForgetItem item=task(108,RecurrenceUtils.WEEKLY);item.taskMode=false;new NoForgetStore(c).save(item);
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            MainActivity host=controller.get();
            LinearLayout card=ReflectionHelpers.callInstanceMethod(host,"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,item));
            Button enabled=card.findViewWithTag("note_alarm_enabled_108");assertNotNull(enabled);assertTrue(enabled.isEnabled());enabled.performClick();
            NoForgetItem disabled=new NoForgetStore(c).find(item.id);assertFalse(disabled.reminderEnabled);assertTrue(disabled.hasDue);assertEquals(item.reminderMinutesJson,disabled.reminderMinutesJson);
            assertFalse(EventReminderScheduler.hasPending(c,"note",item.id,now));
            card=ReflectionHelpers.callInstanceMethod(host,"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,disabled));
            ((Button)card.findViewWithTag("note_alarm_enabled_108")).performClick();assertTrue(new NoForgetStore(c).find(item.id).reminderEnabled);
            assertTrue(EventReminderScheduler.hasPending(c,"note",item.id,now));
            disabled.hasDue=false;card=ReflectionHelpers.callInstanceMethod(host,"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,disabled));assertNull(card.findViewWithTag("note_alarm_enabled_108"));
        }
    }
    @Test public void completedOneShotCannotBeRearmedWithoutUncheckingTheTask(){
        NoForgetItem item=task(109,RecurrenceUtils.NONE);NoForgetItem done=NoteTasks.setCompleted(c,item.id,true,now);
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            LinearLayout card=ReflectionHelpers.callInstanceMethod(controller.get(),"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,done));
            Button enabled=card.findViewWithTag("note_alarm_enabled_109");assertFalse(enabled.isEnabled());assertFalse(new NoForgetStore(c).find(item.id).reminderEnabled);
            ((CheckBox)card.findViewWithTag("task_check_109")).setChecked(false);assertTrue(enabled.isEnabled());
            assertEquals(c.getString(R.string.runtime_text_0212),enabled.getText().toString());
        }
    }
    @Test public void completedStylingCanRenderWithoutAnyAnimationOrOverlay(){
        TextView title=new TextView(c);title.setText("Task");TaskCelebration.strike(title,true,false);
        assertTrue((title.getPaintFlags()&Paint.STRIKE_THRU_TEXT_FLAG)!=0);assertEquals(.55f,title.getAlpha(),.01f);
        TaskCelebration.strike(title,false,false);assertEquals(0,title.getPaintFlags()&Paint.STRIKE_THRU_TEXT_FLAG);assertEquals(1f,title.getAlpha(),.01f);
    }
}
