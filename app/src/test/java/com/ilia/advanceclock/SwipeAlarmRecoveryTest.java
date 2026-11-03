package com.ilia.advanceclock;

import android.app.Application;
import android.view.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.time.Duration;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class SwipeAlarmRecoveryTest {
    private void event(SwipeAlarmSurface view,int action,float y) {
        MotionEvent e=MotionEvent.obtain(0,10,action,40,y,0);view.onTouchEvent(e);e.recycle();
    }
    private SwipeAlarmSurface surface(int[] stopped) {
        SwipeAlarmSurface view=new SwipeAlarmSurface(RuntimeEnvironment.getApplication(),()->stopped[0]++);
        view.layout(0,0,400,800);return view;
    }
    private void finishSwipe(SwipeAlarmSurface view) {
        event(view,0,700);event(view,2,400);event(view,1,400);
    }
    @Test public void extraTouchAndCancelledAnimationCannotPreventCommittedDismissal() {
        int[] stopped={0};SwipeAlarmSurface view=surface(stopped);finishSwipe(view);
        MotionEvent extra=MotionEvent.obtain(0,20,0,40,500,0);
        view.dispatchTouchEvent(extra);extra.recycle();
        view.animate().cancel(); // Reproduce OEM/interrupted animation independently of a new tap.
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(600));
        assertEquals(1,stopped[0]);
        view.performAccessibilityAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_DISMISS,null);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(1,stopped[0]);
    }
    @Test public void interruptedSpringIsResetBeforeNextGestureAndNextSwipeStillWorks() {
        int[] stopped={0};SwipeAlarmSurface view=surface(stopped);
        event(view,0,700);event(view,2,650);event(view,1,650);
        view.setTranslationY(-100); // Simulate a spring interrupted midway.
        MotionEvent down=MotionEvent.obtain(0,20,0,40,700,0);
        view.onInterceptTouchEvent(down);down.recycle();
        assertEquals(0,view.getTranslationY(),.01f);assertEquals(1,view.getAlpha(),.01f);
        event(view,2,400);event(view,1,400);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(600));
        assertEquals(1,stopped[0]);
    }
    @Test public void cancelledGestureReturnsCompletelyWithoutStoppingAlarm() {
        int[] stopped={0};SwipeAlarmSurface view=surface(stopped);
        event(view,0,700);event(view,2,500);event(view,3,500);view.animate().cancel();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(600));
        assertEquals(0,view.getTranslationY(),.01f);assertEquals(0,stopped[0]);
    }
}
