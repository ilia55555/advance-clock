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
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class NoteAlertDetailsTest {
 @Test public void everyNoteAlertStyleShowsFullScrollableContentsWithoutViewerButtonsOrDrawingCanvas(){
  Application c=RuntimeEnvironment.getApplication();AppString.init(c);
  String body=String.join("\n",java.util.Collections.nCopies(80,"Full private note text that must remain readable."));
  NoForgetItem note=new NoForgetItem(901,"Meeting",body,"[{\"c\":-16777216,\"w\":4,\"p\":[[0.2,0.2],[0.8,0.7]]}]",2,true,System.currentTimeMillis()+3600000,true,System.currentTimeMillis());
  note.attachmentsJson="[{\"kind\":\"url\",\"value\":\"https://example.com\",\"name\":\"Meeting file\",\"mime\":\"text/html\"}]";new NoForgetStore(c).save(note);
  for(int style=0;style<=4;style++){
   AppSettings.setAlarmScreenStyle(c,style);
   var service=Robolectric.buildService(NoForgetSoundService.class).create();ReflectionHelpers.setField(service.get(),"currentItem",note);ReflectionHelpers.setField(service.get(),"active",true);
   try(var controller=Robolectric.buildActivity(NoteReminderActivity.class,new Intent(c,NoteReminderActivity.class).putExtra("noteId",note.id)).setup().visible()){
    NoteReminderActivity host=controller.get();View root=host.findViewById(android.R.id.content);
    for(String tag:new String[]{"alert_drawing_board","alert_note_details","note_detail_close","note_detail_edit","note_detail_actions"})assertNull(tag,root.findViewWithTag(tag));
    assertEquals(note.title,((TextView)root.findViewWithTag("note_detail_title")).getText().toString());
    assertEquals(body,((TextView)root.findViewWithTag("note_detail_body")).getText().toString());
    assertNotNull(root.findViewWithTag("note_detail_sketch"));assertTrue(hasText(root,"Meeting file"));assertNotNull(root.findViewWithTag("alert_snooze"));
    for(int pass=0;pass<3;pass++){
     root.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));root.layout(0,0,360,800);root.getViewTreeObserver().dispatchOnGlobalLayout();
    }
    ScrollView scroll=root.findViewWithTag("note_detail_scroll");assertTrue(scroll.getHeight()>100);assertTrue(scroll.canScrollVertically(1));scroll.fullScroll(View.FOCUS_DOWN);
    assertTrue(ReflectionHelpers.<Boolean>getField(service.get(),"active"));assertFalse(host.isFinishing());assertNull(Shadows.shadowOf(host).getNextStartedActivity());
    assertNotNull(root.findViewWithTag("alert_sound"));assertNotNull(root.findViewWithTag("alert_vibration"));assertNotNull(root.findViewWithTag("alert_open"));
    // Coming back from an attachment refreshes the same inline viewer.
    note.body="Updated body";new NoForgetStore(c).save(note);controller.pause().resume();assertEquals("Updated body",((TextView)root.findViewWithTag("note_detail_body")).getText().toString());note.body=body;new NoForgetStore(c).save(note);
    root.findViewWithTag("alert_open").performClick();Dialog dialog=ReflectionHelpers.getField(host,"detailsDialog");assertNotNull(dialog);assertTrue(dialog.isShowing());
    View details=dialog.getWindow().getDecorView();assertEquals(body,((TextView)details.findViewWithTag("note_detail_body")).getText().toString());assertNotNull(details.findViewWithTag("note_detail_sketch"));assertTrue(hasText(details,"Meeting file"));
    assertNotNull(details.findViewWithTag("note_detail_edit"));assertNotNull(details.findViewWithTag("note_detail_close"));
    assertNull(Shadows.shadowOf(host).getNextStartedActivity());assertFalse(host.isFinishing());assertTrue(ReflectionHelpers.<Boolean>getField(service.get(),"active"));
    details.findViewWithTag("note_detail_close").performClick();assertFalse(dialog.isShowing());assertFalse(host.isFinishing());assertTrue(ReflectionHelpers.<Boolean>getField(service.get(),"active"));
    root.findViewWithTag("alert_open").performClick();Dialog reopened=ReflectionHelpers.getField(host,"detailsDialog");assertTrue(reopened.isShowing());host.finish();controller.pause().stop().destroy();assertFalse(reopened.isShowing());
   }finally{service.destroy();}
  }
 }
 @Test public void scrollingInlineNoteDoesNotDragOrDismissTheAlarmSurface(){
  Application c=RuntimeEnvironment.getApplication();AppString.init(c);
  NoForgetItem note=new NoForgetItem(902,"Note",String.join("\n",java.util.Collections.nCopies(100,"Scrollable note contents")),"[]",2,true,System.currentTimeMillis()+100000,true,1);new NoForgetStore(c).save(note);
  try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()){
   int[] stopped={0};SwipeAlarmSurface surface=new SwipeAlarmSurface(controller.get(),()->stopped[0]++);
   NoteDetailView detail=new NoteDetailView(controller.get(),note.id,android.graphics.Color.BLACK);detail.render();surface.addView(detail,new android.widget.FrameLayout.LayoutParams(-1,-1));controller.get().setContentView(surface);
   surface.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));surface.layout(0,0,320,480);
   for(int i=0;i<4;i++){
    MotionEvent event=MotionEvent.obtain(0,i*100,new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}[i],160,new int[]{350,250,100,100}[i],0);surface.dispatchTouchEvent(event);event.recycle();
   }
   ScrollView scroll=surface.findViewWithTag("note_detail_scroll");assertTrue("scroll="+scroll.getScrollY()+", surface="+surface.getTranslationY()+", viewport="+scroll.getHeight()+", content="+scroll.getChildAt(0).getHeight(),scroll.getScrollY()>0);assertEquals(0f,surface.getTranslationY(),0f);assertEquals(0,stopped[0]);
  }
 }
 boolean hasText(View view,String value){if(view instanceof TextView && ((TextView)view).getText().toString().equals(value))return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),value))return true;}return false;}
}
