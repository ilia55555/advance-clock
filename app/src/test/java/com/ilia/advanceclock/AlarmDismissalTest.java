package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlarmDismissalTest {
    @Before public void init(){
        AppString.init(RuntimeEnvironment.getApplication());
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(Duration.ofMillis(16));
    }
    @Test public void swipeIsDefaultAndAllThreeMethodsAreIndependentOfScreenStyle() {
        Application app=RuntimeEnvironment.getApplication();assertEquals(AlarmDismissal.SWIPE,AppSettings.alarmDismissMode(app));
        for(int style=0;style<5;style++)for(int mode=0;mode<3;mode++) {
            AppSettings.setAlarmScreenStyle(app,style);AppSettings.setAlarmDismissMode(app,mode);
            try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
                Activity host=controller.get();AppSettings.applyTheme(host);
                ReminderScreen screen=new ReminderScreen(host,new ReminderScreen.Content(),()->{},()->{},m->{});
                host.setContentView(screen);
                assertEquals(mode==AlarmDismissal.SWIPE,screen.getChildAt(0) instanceof SwipeAlarmSurface);
                assertMode(screen,mode);
            }
        }
    }
    @Test public void holdRequiresFullDurationAndEarlyReleaseNeverDismisses() {
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            int[] dismissed={0};HoldToDismissView hold=new HoldToDismissView(controller.get(),0xff006699,()->dismissed[0]++);
            controller.get().setContentView(hold);hold.layout(0,0,100,100);
            hold.performClick();assertEquals(0,dismissed[0]);
            touch(hold,MotionEvent.ACTION_DOWN,50,50);idle(2499);assertEquals(0,dismissed[0]);
            touch(hold,MotionEvent.ACTION_UP,50,50);idle(3000);assertEquals(0,dismissed[0]);
            touch(hold,MotionEvent.ACTION_DOWN,50,50);idle(2500);assertEquals(1,dismissed[0]);
            touch(hold,MotionEvent.ACTION_UP,50,50);idle(3000);assertEquals(1,dismissed[0]);
        }
    }
    @Test public void movingOutsideCancellingAndDetachingResetTheHold() {
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            int[] dismissed={0};HoldToDismissView hold=new HoldToDismissView(controller.get(),0xff006699,()->dismissed[0]++);
            controller.get().setContentView(hold);hold.layout(0,0,100,100);
            touch(hold,MotionEvent.ACTION_DOWN,50,50);idle(1000);
            touch(hold,MotionEvent.ACTION_MOVE,hold.getWidth()+10,50);idle(3000);assertEquals(0,dismissed[0]);
            touch(hold,MotionEvent.ACTION_DOWN,50,50);idle(1000);
            touch(hold,MotionEvent.ACTION_CANCEL,50,50);idle(3000);assertEquals(0,dismissed[0]);
            touch(hold,MotionEvent.ACTION_DOWN,50,50);idle(1000);
            controller.get().setContentView(new TextView(controller.get()));idle(3000);assertEquals(0,dismissed[0]);
        }
    }
    @Test public void disablingAnimationsCannotBypassTimedHold() {
        float previous=ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,"getDurationScale");
        ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,"setDurationScale",ReflectionHelpers.ClassParameter.from(float.class,0f));
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            int[] dismissed={0};HoldToDismissView hold=new HoldToDismissView(controller.get(),0xff006699,()->dismissed[0]++);
            controller.get().setContentView(hold);hold.layout(0,0,100,100);
            touch(hold,MotionEvent.ACTION_DOWN,50,50);idle(1000);assertEquals(0,dismissed[0]);
            idle(1500);assertEquals(1,dismissed[0]);
        } finally {ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,"setDurationScale",ReflectionHelpers.ClassParameter.from(float.class,previous));}
    }
    @Test public void settingsOpensFullScreenPreviewAndSelectionPersists() {
        Application app=RuntimeEnvironment.getApplication();
        Intent intent=new Intent(app,SettingsActivity.class).putExtra(SettingsActivity.EXTRA_SECTION,"alarm");
        try(var controller=Robolectric.buildActivity(SettingsActivity.class,intent).setup().visible()) {
            SettingsActivity host=controller.get();Spinner selector=host.findViewById(android.R.id.content).findViewWithTag("alarm_dismiss_mode_selector");
            selector.setSelection(AlarmDismissal.HOLD);idle(1);
            assertNull(host.findViewById(android.R.id.content).findViewWithTag("alarm_dismiss_preview"));
            host.findViewById(android.R.id.content).findViewWithTag("alarm_preview_button").performClick();
            assertEquals(AlarmPreviewActivity.class.getName(),Shadows.shadowOf(host).getNextStartedActivity().getComponent().getClassName());
            assertEquals(AlarmDismissal.HOLD,AppSettings.alarmDismissMode(host));
        }
    }
    @Test public void fullScreenPreviewUsesAllStylesAndMethodsWithoutStartingServices(){
        Application app=RuntimeEnvironment.getApplication();
        for(int style=0;style<5;style++) for(int mode=0;mode<3;mode++) {
            AppSettings.setAlarmScreenStyle(app,style);AppSettings.setAlarmDismissMode(app,mode);
            try(var controller=Robolectric.buildActivity(AlarmPreviewActivity.class).setup().visible()) {
                AlarmPreviewActivity host=controller.get();
                View content=host.findViewById(android.R.id.content);
                assertNotNull(content.findViewWithTag(mode==0?"alarm_dismiss_tap":mode==1?"alarm_dismiss_hold":"alarm_dismiss_swipe_hint"));
                assertNull(Shadows.shadowOf(host).getNextStartedService());
                if(mode==AlarmDismissal.TAP){
                    content.findViewWithTag("alarm_dismiss_tap").performClick();assertTrue(host.isFinishing());
                }
            }
        }
    }
    @Test public void everyAlarmNoteAndAdhanEntryPointUsesSelectedMethod() {
        Application app=RuntimeEnvironment.getApplication();
        AlarmItem alarm=new AlarmItem(42,"Alarm",System.currentTimeMillis()+60000,0,true);new AlarmStore(app).save(alarm);
        NoForgetItem note=new NoForgetItem(7,"Note","","[]",2,true,System.currentTimeMillis()+60000,true,System.currentTimeMillis());new NoForgetStore(app).save(note);
        for(int mode=0;mode<3;mode++) {
            AppSettings.setAlarmDismissMode(app,mode);
            for(String kind:new String[]{"",ToolAlarmScheduler.TIMER,ToolAlarmScheduler.STOPWATCH}) {
                Intent intent=new Intent(app,AlarmRingActivity.class).putExtra("alarmId",kind.isEmpty()?42:ToolAlarmScheduler.alarmId(kind)).putExtra("toolKind",kind);
                try(var controller=Robolectric.buildActivity(AlarmRingActivity.class,intent).setup()) {assertMode(controller.get().findViewById(android.R.id.content),mode);}
            }
            try(var controller=Robolectric.buildActivity(NoteReminderActivity.class,new Intent(app,NoteReminderActivity.class).putExtra("noteId",7L)).setup()) {
                assertMode(controller.get().findViewById(android.R.id.content),mode);
            }
            try(var controller=Robolectric.buildActivity(AdhanRingActivity.class).setup()) {assertMode(controller.get().findViewById(android.R.id.content),mode);}
        }
    }
    private void assertMode(View root,int mode) {
        assertEquals(mode==AlarmDismissal.TAP,root.findViewWithTag("alarm_dismiss_tap")!=null);
        assertEquals(mode==AlarmDismissal.HOLD,root.findViewWithTag("alarm_dismiss_hold")!=null);
        assertEquals(mode==AlarmDismissal.SWIPE,root.findViewWithTag("alarm_dismiss_swipe_hint")!=null);
    }
    private static void idle(long millis){Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(millis));}
    private static void touch(View view,int action,float x,float y){
        long now=android.os.SystemClock.uptimeMillis();MotionEvent event=MotionEvent.obtain(now,now,action,x,y,0);view.onTouchEvent(event);event.recycle();
    }
    private static void layout(View view,int width,int height){
        view.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));view.layout(0,0,width,height);
    }
}
