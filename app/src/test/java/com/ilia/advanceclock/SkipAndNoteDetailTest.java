package com.ilia.advanceclock;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class SkipAndNoteDetailTest {
 Context c;long now;String sketch="[{\"c\":-16777216,\"w\":4,\"p\":[[0.2,0.2],[0.7,0.8]]}]";
 @Before public void setup(){c=RuntimeEnvironment.getApplication();AppString.init(c);now=System.currentTimeMillis();Shadows.shadowOf(c.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);}
 long today(TimeZone zone){Calendar end=Calendar.getInstance(zone);end.setTimeInMillis(now);end.add(Calendar.DAY_OF_YEAR,1);end.set(Calendar.HOUR_OF_DAY,0);end.set(Calendar.MINUTE,0);end.set(Calendar.SECOND,0);end.set(Calendar.MILLISECOND,0);return now+(end.getTimeInMillis()-now)/2;}
 @Test public void dailyClockAndWorldAlarmSkipOnlyTodayAndRemainEnabledAfterReschedule(){
  for(int i=0;i<2;i++){
   AlarmItem item=new AlarmItem(301+i,"Alarm",today(TimeZone.getTimeZone(i==0?AppTime.zone().getID():"America/New_York")),RecurrenceUtils.DAILY,true);if(i==1){item.referenceZoneId="America/New_York";item.createdFromWorld=true;}
   item.reminderMode=2;item.reminderMinutesJson=EventReminderRules.encode(Arrays.asList(new EventReminderRules.Rule(false,"hour",1,false)));new AlarmStore(c).save(item);
   long original=item.triggerAtMillis;assertTrue(OccurrenceSkips.skip(c,"alarm",item.id,now));AlarmItem saved=new AlarmStore(c).find(item.id);assertTrue(saved.enabled);assertTrue(saved.triggerAtMillis>original);assertTrue(OccurrenceSkips.contains(c,"alarm",item.id,original));
   AlarmScheduler.rescheduleAll(c);assertEquals(saved.triggerAtMillis,new AlarmStore(c).find(item.id).triggerAtMillis);
   for(Object value:c.getSharedPreferences("event_reminder_schedule_v1",0).getAll().values())try{assertNotEquals(original,new org.json.JSONObject(value.toString()).getLong("occurrence"));}catch(Exception e){throw new AssertionError(e);}
  }
 }
 @Test public void intervalNoteSkipsOneOccurrenceRatherThanTheEntireDay(){
  NoForgetItem item=new NoForgetItem(303,"Note","Body","[]",2,true,today(AppTime.zone()),true,now,RecurrenceUtils.INTERVAL_MINUTES,60,"[]");new NoForgetStore(c).save(item);
  assertTrue(OccurrenceSkips.skip(c,"note",item.id,now));NoForgetItem saved=new NoForgetStore(c).find(item.id);assertTrue(saved.reminderEnabled);assertEquals(item.dueAtMillis+3600000L,saved.dueAtMillis);assertEquals(1,OccurrenceSkips.values(c,"note",item.id).size());
  assertFalse(OccurrenceSkips.skip(c,"note",999,now));
 }
 @Test public void oneShotOrNonTodayOccurrencesCannotBeSkipped(){
  AlarmItem item=new AlarmItem(304,"One shot",today(AppTime.zone()),RecurrenceUtils.NONE,true);new AlarmStore(c).save(item);assertFalse(OccurrenceSkips.skip(c,"alarm",item.id,now));
  item.recurrenceMode=RecurrenceUtils.DAILY;item.triggerAtMillis=now+2*86400000L;new AlarmStore(c).save(item);assertFalse(OccurrenceSkips.skip(c,"alarm",item.id,now));
 }
 @Test public void skipControlIsSmallerThanActiveAndDetailActionsStayBelowScrollableContent(){
  AlarmItem alarm=new AlarmItem(306,"Alarm",today(AppTime.zone()),RecurrenceUtils.DAILY,true);new AlarmStore(c).save(alarm);
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
   LinearLayout card=ReflectionHelpers.callInstanceMethod(controller.get(),"alarmCard",ReflectionHelpers.ClassParameter.from(AlarmItem.class,alarm));
   Button skip=card.findViewWithTag("skip_today_alarm_306");ViewGroup row=(ViewGroup)skip.getParent();View active=row.getChildAt(row.getChildCount()-1);
   card.measure(View.MeasureSpec.makeMeasureSpec((int)(320*c.getResources().getDisplayMetrics().density),View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2000,View.MeasureSpec.AT_MOST));card.layout(0,0,card.getMeasuredWidth(),card.getMeasuredHeight());
   assertTrue(skip.getWidth()<active.getWidth());assertTrue(skip.getHeight()<active.getHeight());
  }
  NoForgetItem note=new NoForgetItem(307,"Title",String.join(" ",Collections.nCopies(300,"Long note")),"[]",2,false,0,false,now);new NoForgetStore(c).save(note);
  try(var controller=Robolectric.buildActivity(NoteDetailActivity.class,new Intent(c,NoteDetailActivity.class).putExtra("noteId",note.id)).setup().visible()){
   View decor=controller.get().getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));decor.layout(0,0,1080,1920);
   View close=decor.findViewWithTag("note_detail_close"),edit=decor.findViewWithTag("note_detail_edit");ViewGroup footer=(ViewGroup)close.getParent(),page=(ViewGroup)footer.getParent();
   assertTrue(edit.getLeft()>close.getRight());assertEquals(page.getHeight()-page.getPaddingBottom(),footer.getBottom());assertTrue(page.getChildAt(0).getBottom()<=footer.getTop());
   edit.performClick();assertEquals(NoForgetEditorActivity.class.getName(),Shadows.shadowOf(controller.get()).getNextStartedActivity().getComponent().getClassName());
  }
 }
 @Test public void detailShowsFullTextSketchAndAttachmentsWithoutMutatingTheDrawing(){
  NoForgetItem note=new NoForgetItem(305,"Title",String.join(" ",Collections.nCopies(200,"Full note text")),sketch,2,false,0,false,now);note.attachmentsJson="[{\"kind\":\"url\",\"value\":\"https://example.com\",\"name\":\"Website\",\"mime\":\"text/html\"}]";new NoForgetStore(c).save(note);
  try(var controller=Robolectric.buildActivity(NoteDetailActivity.class,new Intent(c,NoteDetailActivity.class).putExtra("noteId",note.id)).setup().visible()){
   View root=controller.get().findViewById(android.R.id.content);assertEquals(note.body,((TextView)root.findViewWithTag("note_detail_body")).getText().toString());SketchView drawing=root.findViewWithTag("note_detail_sketch");assertNotNull(drawing);String before=drawing.serialize();drawing.layout(0,0,300,280);MotionEvent down=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,20,20,0);drawing.onTouchEvent(down);down.recycle();assertEquals(before,drawing.serialize());
  }
  try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
   LinearLayout card=ReflectionHelpers.callInstanceMethod(controller.get(),"noteCard",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,note));assertNotNull(card.findViewWithTag("note_sketch_preview_305"));card.performClick();Intent opened=Shadows.shadowOf(controller.get()).getNextStartedActivity();assertEquals(NoteDetailActivity.class.getName(),opened.getComponent().getClassName());
  }
 }
}
