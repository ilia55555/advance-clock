package com.ilia.advanceclock;

import android.app.Application;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class AlertPhotoGestureTest {
    private void touch(View root,int action,float x,float y) {
        long now=android.os.SystemClock.uptimeMillis();
        MotionEvent event=MotionEvent.obtain(now,now,action,x,y,0);
        root.dispatchTouchEvent(event);event.recycle();
    }
    @Test public void photoScrollDoesNotDragAlarmAndNextGestureOutsidePhotosCanDismiss() {
        Application app=RuntimeEnvironment.getApplication();
        int[] stopped={0};SwipeAlarmSurface surface=new SwipeAlarmSurface(app,()->stopped[0]++);
        FrameLayout content=new FrameLayout(app);surface.addView(content,new FrameLayout.LayoutParams(-1,-1));
        AlertPhotoGallery gallery=new AlertPhotoGallery(app);
        View photo=new View(app);photo.setMinimumHeight(1800);photo.setClickable(true);
        gallery.addView(photo,new ScrollView.LayoutParams(-1,1800));
        FrameLayout.LayoutParams bounds=new FrameLayout.LayoutParams(300,300);bounds.topMargin=100;
        content.addView(gallery,bounds);
        surface.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));
        surface.layout(0,0,400,800);
        touch(surface,MotionEvent.ACTION_DOWN,100,350);
        touch(surface,MotionEvent.ACTION_MOVE,100,300);
        touch(surface,MotionEvent.ACTION_MOVE,100,150);
        touch(surface,MotionEvent.ACTION_UP,100,150);
        assertTrue("Photos must scroll: gallery="+gallery.getHeight()+" photo="+photo.getHeight()+" scrollable="+gallery.canScrollVertically(1)+" translated="+surface.getTranslationY(),gallery.getScrollY()>0);
        assertEquals(0,surface.getTranslationY(),.1f);assertEquals(0,stopped[0]);
        // A separate gesture starting in the bottom stop area belongs to the alarm.
        touch(surface,MotionEvent.ACTION_DOWN,350,750);
        touch(surface,MotionEvent.ACTION_MOVE,350,500);
        assertTrue(surface.getTranslationY()<0);
        touch(surface,MotionEvent.ACTION_UP,350,400);
        assertTrue((Boolean)org.robolectric.util.ReflectionHelpers.getField(surface,"finished"));
    }
}
