package com.ilia.advanceclock;

import android.app.*;
import android.view.*;
import android.animation.ValueAnimator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class TabBarSwipeTest {
    @Test public void horizontalGestureOnTabLabelsNavigatesAndCancellationStaysOnCurrentTab(){
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            MainActivity host=controller.get();View decor=host.getWindow().getDecorView();
            decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));decor.layout(0,0,1080,1920);
            String[] order=AppSettings.tabOrder(host);ReflectionHelpers.callInstanceMethod(host,"showTab",ReflectionHelpers.ClassParameter.from(String.class,order[0]));
            View bar=host.findViewById(R.id.tab_bar);int[] xy=new int[2];bar.getLocationOnScreen(xy);
            float x=xy[0]+bar.getWidth()*.5f,y=xy[1]+bar.getHeight()*.5f;
            float dx=bar.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?450:-450;
            send(host,MotionEvent.ACTION_DOWN,x,y,0);send(host,MotionEvent.ACTION_MOVE,x+dx,y,80);send(host,MotionEvent.ACTION_UP,x+dx,y,120);
            finish(host);assertEquals(order[1],ReflectionHelpers.getField(host,"currentTab"));
            send(host,MotionEvent.ACTION_DOWN,x,y,200);send(host,MotionEvent.ACTION_MOVE,x-dx,y,280);send(host,MotionEvent.ACTION_CANCEL,x-dx,y,300);
            finish(host);assertEquals(order[1],ReflectionHelpers.getField(host,"currentTab"));
        }
    }
    void finish(MainActivity host){TabMotion motion=ReflectionHelpers.getField(host,"tabMotion");ValueAnimator animator=ReflectionHelpers.getField(motion,"animator");if(animator!=null)animator.end();motion.finish();}
    void send(MainActivity host,int action,float x,float y,long time){MotionEvent e=MotionEvent.obtain(0,time,action,x,y,0);host.dispatchTouchEvent(e);e.recycle();}
}
