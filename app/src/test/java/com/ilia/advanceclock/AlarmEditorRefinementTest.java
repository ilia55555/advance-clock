package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class AlarmEditorRefinementTest {
    @Test public void unsetPriorityPersistsAndFieldsFollowRequestedOrder() throws Exception {
        Context app=RuntimeEnvironment.getApplication();AppString.init(app);
        AlarmItem item=new AlarmItem(88,"Unset",System.currentTimeMillis()+600000,0,true,true,-1,0,1,"[]",15);
        assertEquals(-1,AlarmItem.fromJson(item.toJson()).priority);
        try(var controller=Robolectric.buildActivity(AlarmEditorActivity.class).setup()) {
            AlarmEditorActivity host=controller.get();
            Spinner priority=host.findViewById(R.id.alarm_priority);
            assertEquals(0,priority.getSelectedItemPosition());
            assertEquals(host.getString(R.string.priority_unset),priority.getSelectedItem());
            ListAdapter adapter=(ListAdapter)priority.getAdapter();
            assertFalse(adapter.isEnabled(0));assertTrue(adapter.isEnabled(1));
            View hidden=priority.getAdapter().getDropDownView(0,null,new LinearLayout(host));
            assertEquals(View.GONE,hidden.getVisibility());
            View option=priority.getAdapter().getDropDownView(1,hidden,new LinearLayout(host));
            assertEquals(View.VISIBLE,option.getVisibility());
            View sound=host.findViewById(R.id.alarm_sound_button);
            View vibration=(View)host.findViewById(R.id.alarm_vibrate).getParent();
            assertSame(sound.getParent(),vibration.getParent());
            assertSame(host.findViewById(R.id.alarm_repeat).getParent(),host.findViewById(R.id.alarm_reminders).getParent());
            ViewGroup card=(ViewGroup)priority.getParent();
            assertTrue(card.indexOfChild((View)sound.getParent())<card.indexOfChild(priority));
            Button attach=host.findViewById(R.id.alarm_image1_button);
            String key=ReflectionHelpers.getField(host,"attachmentKey");
            AlarmAttachments.prefs(host).edit().putString(key+"_image1","content://test/photo").apply();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertEquals(host.getString(R.string.attachment_count,CalendarUtils.fa(1)),attach.getText().toString());
        }
    }
    @Test public void photoTrashDeletesOnceAndPreservesOtherCropWithoutOpeningAnotherScreen() {
        Context app=RuntimeEnvironment.getApplication();AppString.init(app);
        String key="delete_test";
        AlarmAttachments.prefs(app).edit().putString(key+"_image1","content://test/one")
                .putString(key+"_source1","content://test/original-one")
                .putString(key+"_image2","content://test/two")
                .putString(key+"_source2","content://test/original-two").apply();
        try(var controller=Robolectric.buildActivity(AlarmAttachmentEditorActivity.class,
                new android.content.Intent(app,AlarmAttachmentEditorActivity.class).putExtra("key",key)).setup()) {
            AlarmAttachmentEditorActivity host=controller.get();
            View trash=findTrash(host.findViewById(android.R.id.content),host.getString(R.string.tool_delete));
            assertNotNull(trash);trash.performClick();
            assertEquals("content://test/two",AlarmAttachments.prefs(app).getString(key+"_image1",""));
            assertEquals("content://test/original-two",AlarmAttachments.prefs(app).getString(key+"_source1",""));
            assertFalse(AlarmAttachments.prefs(app).contains(key+"_image2"));
            assertNull(Shadows.shadowOf(host).getNextStartedActivity());
        }
    }
    private View findTrash(View view,String description) {
        if(view instanceof ImageButton && description.contentEquals(view.getContentDescription()))return view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++) {
            View found=findTrash(((ViewGroup)view).getChildAt(i),description);if(found!=null)return found;
        }
        return null;
    }

    @Test public void zoomOutAndPanExportLeavesBlackSpaceAndKeepsFraming() {
        Context app=RuntimeEnvironment.getApplication();
        AlarmPhotoView view=new AlarmPhotoView(app,true);
        Bitmap source=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);source.eraseColor(Color.RED);
        ReflectionHelpers.setField(view,"bitmap",source);
        view.restoreFraming(new float[]{.5f,.2f,.1f});
        Bitmap output=view.export(100,100);
        assertEquals(Color.BLACK,output.getPixel(0,0));
        assertEquals(Color.RED,output.getPixel(70,60));
        assertEquals(.5f,view.framing()[0],.001f);
        assertEquals(.2f,view.framing()[1],.001f);
        output.recycle();source.recycle();
    }
    @Test public void alertPhotoKeepsSavedFrameAspectInsteadOfCompressingIntoAStrip() {
        AlarmPhotoView photo=new AlarmPhotoView(RuntimeEnvironment.getApplication(),false);
        photo.setFitWithinBounds(true);
        Bitmap savedFrame=Bitmap.createBitmap(100,200,Bitmap.Config.ARGB_8888);
        ReflectionHelpers.setField(photo,"bitmap",savedFrame);
        photo.measure(View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        assertEquals(300,photo.getMeasuredWidth());assertEquals(600,photo.getMeasuredHeight());
        savedFrame.recycle();
    }

    @Test public void twoPhotoAttachmentsAreStackedRatherThanSideBySide() {
        Context app=RuntimeEnvironment.getApplication();AppString.init(app);
        try(var controller=Robolectric.buildActivity(android.app.Activity.class).setup()) {
            ReminderScreen.Content data=new ReminderScreen.Content();
            ReminderScreen screen=new ReminderScreen(controller.get(),data,()->{},()->{},values->{});
            java.util.List<NoteAttachment> photos=new java.util.ArrayList<>();
            photos.add(new NoteAttachment(NoteAttachment.KIND_FILE,"content://test/one","one","image/jpeg"));
            photos.add(new NoteAttachment(NoteAttachment.KIND_FILE,"content://test/two","two","image/jpeg"));
            View view=ReflectionHelpers.callInstanceMethod(screen,"attachmentStrip",ReflectionHelpers.ClassParameter.from(java.util.List.class,photos));
            assertTrue(view instanceof LinearLayout);
            assertEquals(LinearLayout.VERTICAL,((LinearLayout)view).getOrientation());
            ScrollView gallery=view.findViewWithTag("alert_photo_gallery");assertNotNull(gallery);
            LinearLayout stack=(LinearLayout)gallery.getChildAt(0);
            assertEquals(LinearLayout.VERTICAL,stack.getOrientation());
            assertEquals(2,stack.getChildCount());
            assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT,stack.getChildAt(0).getLayoutParams().height);
        }
    }
}
