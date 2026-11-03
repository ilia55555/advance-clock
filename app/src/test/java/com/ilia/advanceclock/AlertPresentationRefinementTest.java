package com.ilia.advanceclock;

import android.app.*;
import android.content.Context;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlertPresentationRefinementTest {
    private void layout(View view) {
        view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));
        view.layout(0,0,360,800);
        view.getViewTreeObserver().dispatchOnGlobalLayout();
        view.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));
        view.layout(0,0,360,800);
    }
    @Test public void digitalStylesKeepDateCompactBelowAnIndependentlyLargerClockAndOmitEmptyRows() {
        Context app=RuntimeEnvironment.getApplication();AppString.init(app);
        for(int style:new int[]{0,1,2,4})try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
            AppSettings.setAlarmScreenStyle(app,style);
            ReminderScreen.Content data=new ReminderScreen.Content();data.body="Must not appear without a title";
            ReminderScreen screen=new ReminderScreen(controller.get(),data,()->{},()->{},values->{});
            controller.get().setContentView(screen);layout(screen);
            TextView clock=screen.findViewWithTag("alert_clock"),date=screen.findViewWithTag("alert_date");
            assertTrue(date.getTop()>=clock.getBottom());
            assertTrue(date.getTop()-clock.getBottom()<=3*app.getResources().getDisplayMetrics().density);
            assertEquals(27*app.getResources().getDisplayMetrics().scaledDensity,date.getTextSize(),.1f);
            assertTrue(clock.getTextSize()>date.getTextSize());
            assertNull(screen.findViewWithTag("alert_title"));assertNull(screen.findViewWithTag("alert_priority"));
        }
    }
    @Test public void priorityAndTitleAreIndependentAndBoardStaysBetweenTextAndControls() {
        Context app=RuntimeEnvironment.getApplication();AppString.init(app);
        for(int style:new int[]{0,1})try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
            AppSettings.setAlarmScreenStyle(app,style);AppSettings.setAlarmDismissMode(app,AlarmDismissal.TAP);
            AppSettings.setAlarmDismissSeconds(app,AlarmDismissal.TAP,0);
            ReminderScreen.Content data=new ReminderScreen.Content();data.priority=4;data.title="My reminder";
            int[] stopped={0};
            ReminderScreen screen=new ReminderScreen(controller.get(),data,()->stopped[0]++,()->{},values->{});
            controller.get().setContentView(screen);layout(screen);
            assertEquals(PriorityUtils.label(4),((TextView)screen.findViewWithTag("alert_priority")).getText().toString());
            assertEquals("My reminder",((TextView)screen.findViewWithTag("alert_title")).getText().toString());
            View board=screen.findViewWithTag("alert_drawing_board");assertNotNull(board);
            long now=android.os.SystemClock.uptimeMillis();
            for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP}) {
                MotionEvent event=MotionEvent.obtain(now,now+20,action,30,30,0);assertTrue(board.dispatchTouchEvent(event));event.recycle();
            }
            assertEquals(0,stopped[0]);
            assertTrue(board.getTop()>=((View)screen.findViewWithTag("alert_title").getParent()).getHeight());
            screen.findViewWithTag("alarm_dismiss_tap").performClick();assertEquals(1,stopped[0]);
        }
    }
}
