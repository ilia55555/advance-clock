package com.ilia.advanceclock;

import android.app.*;
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
public class AppMotionTest {
    @Test public void dialogAndPickerUseDistinctMotionAndDismissSynchronously(){
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()){
            Activity host=controller.get();AlertDialog dialog=new MotionDialogs.Builder(host).setMessage("Test").create();dialog.show();
            assertEquals(R.style.MotionDialog,dialog.getWindow().getAttributes().windowAnimations);
            TimePickerDialog picker=new MotionDialogs.TimePicker(host,(v,h,m)->{},12,30,true);picker.show();
            assertEquals(R.style.MotionPicker,picker.getWindow().getAttributes().windowAnimations);
            picker.dismiss();dialog.dismiss();assertFalse(picker.isShowing());assertFalse(dialog.isShowing());
        }
    }
    @Test public void disabledMotionDoesNotLeaveWindowsOrAccordionContentsHidden(){
        AppString.init(RuntimeEnvironment.getApplication());
        ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,"setDurationScale",ReflectionHelpers.ClassParameter.from(float.class,0f));
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()){
            Activity host=controller.get();AlertDialog dialog=new MotionDialogs.Builder(host).setMessage("Test").create();assertEquals(0,dialog.getWindow().getAttributes().windowAnimations);
            LinearLayout root=new LinearLayout(host);SettingsSection section=new SettingsSection(host,"test",R.string.event_before,R.drawable.ic_settings_calendar,()->{});root.addView(section);host.setContentView(root);
            section.setExpanded(true);assertEquals(View.VISIBLE,section.content.getVisibility());section.setExpanded(false);assertEquals(View.GONE,section.content.getVisibility());
            dialog.show();dialog.dismiss();assertFalse(dialog.isShowing());
        }finally{ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,"setDurationScale",ReflectionHelpers.ClassParameter.from(float.class,1f));}
    }
    @Test public void quickAccordionTogglesAlwaysEndInRequestedState(){
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()){
            Activity host=controller.get();LinearLayout root=new LinearLayout(host);host.setContentView(root);
            SettingsSection section=new SettingsSection(host,"test",R.string.event_before,R.drawable.ic_settings_calendar,()->{});root.addView(section);
            root.measure(View.MeasureSpec.makeMeasureSpec(900,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.EXACTLY));root.layout(0,0,900,1600);
            for(int i=0;i<12;i++)section.setExpanded(i%2==0);
            section.setExpanded(true);Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertTrue(section.isExpanded());
        }
    }
}
