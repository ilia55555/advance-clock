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
public class NoteModeAndModalTest {
    @Test public void modeHeaderLabelsToggleAndModePersistsWithoutChangingNoteContent() throws Exception {
        Context c=RuntimeEnvironment.getApplication();AppString.init(c);
        LinearLayout root=new LinearLayout(c);Switch toggle=NoteModeHeader.add(c,root,0,false,false);
        LinearLayout header=(LinearLayout)root.getChildAt(0);
        assertEquals(View.LAYOUT_DIRECTION_LTR,header.getLayoutDirection());
        assertEquals(c.getString(R.string.note_mode_tasks),((TextView)header.getChildAt(0)).getText().toString());
        assertEquals(c.getString(R.string.note_mode_note),((TextView)header.getChildAt(2)).getText().toString());
        header.getChildAt(0).performClick();assertTrue(toggle.isChecked());
        NoForgetItem item=new NoForgetItem(17,"Title","Body","[]",2,true,12345,true,1);item.taskMode=toggle.isChecked();
        NoForgetItem restored=NoForgetItem.fromJson(item.toJson());assertTrue(restored.taskMode);assertEquals("Body",restored.body);assertEquals(12345,restored.dueAtMillis);
        header.getChildAt(2).performClick();assertFalse(toggle.isChecked());
        org.json.JSONObject old=item.toJson();old.remove("taskMode");assertFalse(NoForgetItem.fromJson(old).taskMode);
    }
    @Test public void noteUsesExistingGlobalFullscreenSettingsEvenForLegacyLocalFlags(){
        Context c=RuntimeEnvironment.getApplication();AppString.init(c);
        var controller=Robolectric.buildService(NoForgetSoundService.class).create();
        try{
            NoForgetItem item=new NoForgetItem(17,"Title","Body","[]",2,true,12345,true,1);item.fullscreenUnlocked=false;item.fullscreenLocked=false;
            AppSettings.setAlarmFullscreenUnlocked(c,true);AppSettings.setAlarmFullscreenLocked(c,true);
            assertNotNull(notification(controller.get(),item).fullScreenIntent);
            AppSettings.setAlarmFullscreenUnlocked(c,false);AppSettings.setAlarmFullscreenLocked(c,false);
            assertNull(notification(controller.get(),item).fullScreenIntent);
        }finally{controller.destroy();AppSettings.setAlarmFullscreenUnlocked(c,true);AppSettings.setAlarmFullscreenLocked(c,true);}
    }
    private Notification notification(NoForgetSoundService service,NoForgetItem item){return ReflectionHelpers.callInstanceMethod(service,"buildNotification",ReflectionHelpers.ClassParameter.from(NoForgetItem.class,item),ReflectionHelpers.ClassParameter.from(boolean.class,true));}
    @Test public void expandedReminderActionsHaveSpaceForTheirFullHeight(){
        Context c=RuntimeEnvironment.getApplication();AppString.init(c);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()){
            Activity host=controller.get();AlarmReminderDialog.show(host,0,"[]",(m,j)->{});
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            dialog.findViewById(android.R.id.content).findViewWithTag("settings_header_before").performClick();
            View decor=dialog.getWindow().getDecorView();
            decor.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1500,View.MeasureSpec.AT_MOST));
            decor.layout(0,0,decor.getMeasuredWidth(),decor.getMeasuredHeight());
            for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE}){
                Button b=dialog.getButton(which);assertTrue(b.getHeight()>=ReminderModalStyle.dp(host,48));
                View parent=(View)b.getParent();assertTrue(b.getBottom()<=parent.getHeight());assertTrue(b.getTop()>=0);
            }
        }
    }
}
