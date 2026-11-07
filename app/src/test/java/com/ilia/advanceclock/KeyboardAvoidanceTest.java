package com.ilia.advanceclock;

import android.app.*;
import android.graphics.Rect;
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
public class KeyboardAvoidanceTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    private KeyboardAvoidance controller(Activity host){return (KeyboardAvoidance)host.findViewById(android.R.id.content).getTag(R.id.keyboard_avoidance);}
    private void measure(View view,int width,int height){view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);}
    @Test public void reservesOnlyOccludedAreaRestoresPaddingAndDoesNotDoubleResize(){
        try(var activity=Robolectric.buildActivity(Activity.class).setup()){
            Activity host=activity.get();host.setContentView(new ScrollView(host));
            View content=host.findViewById(android.R.id.content);content.setPadding(3,4,5,6);
            KeyboardAvoidance.install(host);KeyboardAvoidance avoidance=controller(host);KeyboardAvoidance.install(host);assertSame(avoidance,controller(host));
            assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,host.getWindow().getAttributes().softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);
            measure(content,400,1000);int[] position=new int[2];content.getLocationOnScreen(position);
            avoidance.apply(position[1]+700,true);assertEquals(306,content.getPaddingBottom());
            avoidance.apply(position[1]+700,true);assertEquals(306,content.getPaddingBottom());
            avoidance.apply(position[1]+700,false);assertEquals(6,content.getPaddingBottom());
            measure(content,400,700);avoidance.apply(position[1]+700,true);assertEquals(6,content.getPaddingBottom());
            assertEquals(3,content.getPaddingLeft());assertEquals(4,content.getPaddingTop());assertEquals(5,content.getPaddingRight());
            KeyboardAvoidance.forget(host);assertNull(controller(host));
        }
    }
    static class ImmediateScroll extends ScrollView {
        ImmediateScroll(Activity host){super(host);}
        @Override public boolean requestChildRectangleOnScreen(View child,Rect rect,boolean immediate){return super.requestChildRectangleOnScreen(child,rect,true);}
    }
    @Test public void scrollsFocusedFieldAboveKeyboardWithoutReplacingFocusOrText(){
        try(var activity=Robolectric.buildActivity(Activity.class).setup().visible()){
            Activity host=activity.get();ImmediateScroll scroll=new ImmediateScroll(host);LinearLayout form=new LinearLayout(host);form.setOrientation(LinearLayout.VERTICAL);
            form.addView(new View(host),new LinearLayout.LayoutParams(-1,900));EditText field=new EditText(host);field.setText("My title");form.addView(field,new LinearLayout.LayoutParams(-1,60));scroll.addView(form);host.setContentView(scroll);
            KeyboardAvoidance.install(host);View content=host.findViewById(android.R.id.content);measure(content,400,1000);assertTrue(field.requestFocus());
            int[] position=new int[2];content.getLocationOnScreen(position);controller(host).apply(position[1]+600,true);measure(content,400,1000);
            ReflectionHelpers.callInstanceMethod(controller(host),"revealFocus");
            assertTrue(scroll.getScrollY()>0);assertTrue(field.hasFocus());assertEquals("My title",field.getText().toString());
            Rect fieldBounds=new Rect();field.getDrawingRect(fieldBounds);scroll.offsetDescendantRectToMyCoords(field,fieldBounds);
            assertTrue(fieldBounds.bottom-scroll.getScrollY()<=scroll.getHeight());KeyboardAvoidance.forget(host);
        }
    }
    @Test public void timerAndStopwatchTitlesAndTextDialogsReceiveKeyboardHandling(){
        for(Class<? extends Activity> type:new Class[]{TimerActivity.class,StopwatchActivity.class}){
            try(var activity=Robolectric.buildActivity(type).setup()){
                Activity host=activity.get();KeyboardAvoidance.install(host);assertNotNull(controller(host));
                EditText field=host.findViewById(type==TimerActivity.class?R.id.timer_label:R.id.stopwatch_label);assertNotNull(field);
                assertTrue(field.requestFocus());assertSame(field,host.findViewById(android.R.id.content).findFocus());KeyboardAvoidance.forget(host);
            }
        }
        try(var activity=Robolectric.buildActivity(Activity.class).setup()){
            AlertDialog dialog=new MotionDialogs.Builder(activity.get()).setView(new EditText(activity.get())).create();dialog.show();
            View content=dialog.getWindow().findViewById(android.R.id.content);assertNotNull(content.getTag(R.id.keyboard_avoidance));
            assertEquals(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE,dialog.getWindow().getAttributes().softInputMode & WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST);dialog.dismiss();
        }
    }
}
