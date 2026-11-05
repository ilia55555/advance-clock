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
 @Test public void ringingNoteOpensSharedDetailsWithoutStoppingOrShowingInlineAttachments(){
  Application c=RuntimeEnvironment.getApplication();AppString.init(c);
  NoForgetItem note=new NoForgetItem(901,"Meeting","Full private note text","[{\"c\":-16777216,\"w\":4,\"p\":[[0.2,0.2],[0.8,0.7]]}]",2,true,System.currentTimeMillis()+3600000,true,System.currentTimeMillis());
  note.attachmentsJson="[{\"kind\":\"url\",\"value\":\"https://example.com\",\"name\":\"Meeting file\",\"mime\":\"text/html\"}]";new NoForgetStore(c).save(note);
  for(int style=0;style<=4;style++){
   AppSettings.setAlarmScreenStyle(c,style);
   var service=Robolectric.buildService(NoForgetSoundService.class).create();ReflectionHelpers.setField(service.get(),"currentItem",note);ReflectionHelpers.setField(service.get(),"active",true);
   try(var controller=Robolectric.buildActivity(NoteReminderActivity.class,new Intent(c,NoteReminderActivity.class).putExtra("noteId",note.id)).setup().visible()){
    NoteReminderActivity host=controller.get();View root=host.findViewById(android.R.id.content);
    assertNull(root.findViewWithTag("alert_drawing_board"));assertFalse(hasText(root,note.body));assertFalse(hasText(root,"Meeting file"));
    root.findViewWithTag("alert_note_details").performClick();Dialog dialog=ReflectionHelpers.getField(host,"detailsDialog");assertNotNull(dialog);assertTrue(dialog.isShowing());
    View detailRoot=dialog.getWindow().getDecorView();assertEquals(note.body,((TextView)detailRoot.findViewWithTag("note_detail_body")).getText().toString());assertNotNull(detailRoot.findViewWithTag("note_detail_sketch"));assertTrue(hasText(detailRoot,"Meeting file"));
    View details=detailRoot.findViewWithTag("note_detail_view");
    details.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(480,View.MeasureSpec.EXACTLY));details.layout(0,0,320,480);
    View body=detailRoot.findViewWithTag("note_detail_body"),footer=detailRoot.findViewWithTag("note_detail_actions");
    assertTrue(body.getHeight()>0);assertTrue(footer.getHeight()>0);assertTrue(footer.getBottom()<=details.getHeight());
    assertTrue(((ViewGroup)details).getChildAt(0).getHeight()>100);
    assertEquals(ViewGroup.LayoutParams.MATCH_PARENT,details.getLayoutParams().height);
    assertTrue((dialog.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)!=0);
    assertTrue(ReflectionHelpers.<Boolean>getField(service.get(),"active"));assertFalse(host.isFinishing());assertNull(Shadows.shadowOf(host).getNextStartedActivity());
    detailRoot.findViewWithTag("note_detail_close").performClick();assertFalse(dialog.isShowing());
    ViewGroup openColumn=(ViewGroup)root.findViewWithTag("alert_open").getParent();openColumn.getChildAt(1).performClick();
    Dialog reopened=ReflectionHelpers.getField(host,"detailsDialog");assertNotNull(reopened);assertTrue(reopened.isShowing());reopened.dismiss();
    root.findViewWithTag("alert_title").performClick();reopened=ReflectionHelpers.getField(host,"detailsDialog");assertNotNull(reopened);assertTrue(reopened.isShowing());reopened.dismiss();assertTrue(ReflectionHelpers.<Boolean>getField(service.get(),"active"));assertFalse(host.isFinishing());
   }finally{service.destroy();}
  }
 }
 @Test public void dismissingTheAlertAlsoRemovesItsAttachedDetailsWindow(){
  Application c=RuntimeEnvironment.getApplication();AppString.init(c);NoForgetItem note=new NoForgetItem(902,"Note","Text","[]",2,true,System.currentTimeMillis()+100000,true,1);new NoForgetStore(c).save(note);
  var controller=Robolectric.buildActivity(NoteReminderActivity.class,new Intent(c,NoteReminderActivity.class).putExtra("noteId",note.id)).setup().visible();
  NoteReminderActivity host=controller.get();host.findViewById(android.R.id.content).findViewWithTag("alert_open").performClick();Dialog dialog=ReflectionHelpers.getField(host,"detailsDialog");assertTrue(dialog.isShowing());controller.pause().stop().destroy();assertFalse(dialog.isShowing());
 }
 boolean hasText(View view,String value){if(view instanceof TextView && ((TextView)view).getText().toString().equals(value))return true;if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount();i++)if(hasText(group.getChildAt(i),value))return true;}return false;}
}
