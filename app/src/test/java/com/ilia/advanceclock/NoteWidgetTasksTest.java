package com.ilia.advanceclock;

import android.app.*;
import android.appwidget.*;
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
public class NoteWidgetTasksTest {
    Context c;long now;
    @Before public void init(){
        c=RuntimeEnvironment.getApplication();AppString.init(c);now=System.currentTimeMillis();
        Shadows.shadowOf(c.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);
        AppWidgetProviderInfo info=new AppWidgetProviderInfo();info.provider=new ComponentName(c,NoForgetWidgetProvider.class);
        Shadows.shadowOf(AppWidgetManager.getInstance(c)).putWidgetInfo(42,info);
    }
    NoForgetItem task(long id,int recurrence){
        NoForgetItem item=new NoForgetItem(id,"Task "+id,"Body","[]",2,true,now+86400000L,true,now,recurrence,1,"[]");item.taskMode=true;
        item.reminderMode=2;item.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(new EventReminderRules.Rule(false,"hour",1,false)));
        new NoForgetStore(c).save(item);NoForgetScheduler.schedule(c,item);return item;
    }
    @Test public void completeAndReopenWidgetTaskUsesTheSameStateAndCancelsReminders(){
        NoForgetItem item=task(200,RecurrenceUtils.NONE);Intent complete=NoteWidgetTaskReceiver.intent(c,42,item);
        assertTrue(NoteWidgetTaskReceiver.apply(c,complete));NoForgetItem done=new NoForgetStore(c).find(item.id);
        assertTrue(done.taskDone(now));assertFalse(done.reminderEnabled);assertFalse(EventReminderScheduler.hasPending(c,"note",item.id,now));
        assertFalse(NoteWidgetTaskReceiver.apply(c,complete));assertTrue(new NoForgetStore(c).find(item.id).taskCompleted);
        assertTrue(NoteWidgetTaskReceiver.apply(c,NoteWidgetTaskReceiver.intent(c,42,done)));
        assertFalse(new NoForgetStore(c).find(item.id).taskCompleted);assertTrue(new NoForgetStore(c).find(item.id).reminderEnabled);
    }
    @Test public void staleWidgetCannotAffectTheNextWeeklyOccurrenceOrAnEditedNote(){
        NoForgetItem item=task(201,RecurrenceUtils.WEEKLY);Intent old=NoteWidgetTaskReceiver.intent(c,42,item);
        assertTrue(NoteWidgetTaskReceiver.apply(c,old));NoForgetItem next=new NoForgetStore(c).find(item.id);assertTrue(next.dueAtMillis>item.dueAtMillis);assertTrue(next.reminderEnabled);
        assertFalse(NoteWidgetTaskReceiver.apply(c,old));assertEquals(next.dueAtMillis,new NoForgetStore(c).find(item.id).dueAtMillis);
        Intent stale=NoteWidgetTaskReceiver.intent(c,42,next);next.dueAtMillis+=3600000L;new NoForgetStore(c).save(next);assertFalse(NoteWidgetTaskReceiver.apply(c,stale));
    }
    @Test public void ordinaryNotesOtherProvidersAndDeletedWidgetsCannotBeToggled() throws Exception {
        NoForgetItem item=task(202,0);Intent action=NoteWidgetTaskReceiver.intent(c,42,item);
        item.taskMode=false;new NoForgetStore(c).save(item);assertFalse(NoteWidgetTaskReceiver.apply(c,action));
        item.taskMode=true;new NoForgetStore(c).save(item);
        AppWidgetProviderInfo other=new AppWidgetProviderInfo();other.provider=new ComponentName(c,ClockWidgetProvider.class);Shadows.shadowOf(AppWidgetManager.getInstance(c)).putWidgetInfo(42,other);
        assertFalse(NoteWidgetTaskReceiver.apply(c,action));assertFalse(NoteWidgetTaskReceiver.apply(c,NoteWidgetTaskReceiver.intent(c,999,item)));
        android.content.pm.ActivityInfo receiver=c.getPackageManager().getReceiverInfo(new ComponentName(c,NoteWidgetTaskReceiver.class),0);assertFalse(receiver.exported);
    }
    @Test public void renderedWidgetHasTaskCheckboxAndCompletedStylingButOrdinaryNotesHaveNeither(){
        NoForgetItem item=task(203,0);View pending=widget();assertEquals(View.VISIBLE,pending.findViewById(R.id.noforget_row_task).getVisibility());
        assertEquals(0,((TextView)pending.findViewById(R.id.noforget_row_title)).getPaintFlags()&Paint.STRIKE_THRU_TEXT_FLAG);
        NoteTasks.setCompleted(c,item.id,true);View done=widget();assertTrue((((TextView)done.findViewById(R.id.noforget_row_title)).getPaintFlags()&Paint.STRIKE_THRU_TEXT_FLAG)!=0);
        assertTrue(done.findViewById(R.id.noforget_row_task).getContentDescription().toString().startsWith(c.getString(R.string.task_reopen)));
        NoForgetItem plain=new NoForgetStore(c).find(item.id);plain.taskMode=false;new NoForgetStore(c).save(plain);assertEquals(View.GONE,widget().findViewById(R.id.noforget_row_task).getVisibility());
    }
    @Test public void widgetCheckboxClickCompletesTaskWithoutOpeningAnActivity() throws Exception {
        NoteWidgetTaskReceiver receiver=new NoteWidgetTaskReceiver();
        IntentFilter filter=new IntentFilter(NoteWidgetTaskReceiver.action(c));
        // Robolectric does not auto-register explicit manifest receivers without intent filters.
        if(android.os.Build.VERSION.SDK_INT>=33)c.registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else c.registerReceiver(receiver,filter);
        try{
            NoForgetItem item=task(206,0);View view=widget();
            assertTrue(view.findViewById(R.id.noforget_row_task).performClick());
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            NoteTasks.WORK.submit(()->{}).get(10,java.util.concurrent.TimeUnit.SECONDS);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertTrue(new NoForgetStore(c).find(item.id).taskCompleted);
            assertNull(Shadows.shadowOf((Application)c).getNextStartedActivity());
        }finally{c.unregisterReceiver(receiver);}
    }
    @Test public void completedTasksStayBelowPendingItemsInEveryWidgetSortMode(){
        NoForgetItem done=task(204,0);done.taskCompleted=true;NoForgetItem pending=task(205,0);
        for(int mode=0;mode<3;mode++){
            java.util.List<NoForgetItem> ordered=ReflectionHelpers.callStaticMethod(NoForgetWidgetProvider.class,"widgetOrder",ReflectionHelpers.ClassParameter.from(java.util.List.class,Arrays.asList(done,pending)),ReflectionHelpers.ClassParameter.from(int.class,1),ReflectionHelpers.ClassParameter.from(int.class,mode));assertEquals(pending.id,ordered.get(0).id);
        }
    }
    View widget(){
        RemoteViews remote=ReflectionHelpers.callStaticMethod(NoForgetWidgetProvider.class,"createRemoteViews",ReflectionHelpers.ClassParameter.from(Context.class,c),ReflectionHelpers.ClassParameter.from(int.class,42),ReflectionHelpers.ClassParameter.from(float.class,360f),ReflectionHelpers.ClassParameter.from(float.class,500f));
        return remote.apply(c,new FrameLayout(c));
    }
}
