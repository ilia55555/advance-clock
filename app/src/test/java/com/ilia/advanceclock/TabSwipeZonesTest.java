package com.ilia.advanceclock;

import android.app.*;
import android.view.*;
import android.widget.*;
import android.animation.ValueAnimator;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,qualifiers="w1080dp-h2600dp-mdpi",sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class TabSwipeZonesTest {
    @Test public void footerDoesNotCoverDateCellsEvenInSixWeekMonths(){
        AppString.init(RuntimeEnvironment.getApplication());TripleCalendarView view=new TripleCalendarView(RuntimeEnvironment.getApplication());view.setCalendarType(CalendarUtils.GREGORIAN);
        for(int month=0;month<12;month++){
            java.util.Calendar date=java.util.Calendar.getInstance(AppTime.zone());date.clear();date.set(2026,month,1,12,0);view.setSelectedMillis(date.getTimeInMillis());
            view.measure(View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2000,View.MeasureSpec.AT_MOST));view.layout(0,0,600,view.getMeasuredHeight());
            int rows=ReflectionHelpers.callInstanceMethod(view,"rowCount");float[] tops={161,247,334,420,506,592};
            assertFalse(view.tabSwipeFooter(tops[rows-1]+60));assertTrue(view.tabSwipeFooter(view.getHeight()-2));
        }
    }
    @Test public void screenEdgesAndPrayerMarginsKeepTheirCentersForExistingGestures(){
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()){
            Activity host=controller.get();FrameLayout root=new FrameLayout(host);host.setContentView(root);
            View prayer=new View(host);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(600,240);lp.leftMargin=100;lp.topMargin=100;root.addView(prayer,lp);
            root.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.EXACTLY));root.layout(0,0,1000,1600);
            View decor=host.getWindow().getDecorView();decor.measure(View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.EXACTLY));decor.layout(0,0,1000,1600);
            int[] at=new int[2];root.getLocationOnScreen(at);
            assertTrue(TabSwipeZones.edge(root,at[0]+1,at[1]+800));assertFalse(TabSwipeZones.edge(root,at[0]+500,at[1]+800));
            prayer.getLocationOnScreen(at);assertTrue(TabSwipeZones.prayer(prayer,at[0]+300,at[1]+2));assertFalse(TabSwipeZones.prayer(prayer,at[0]+300,at[1]+120));
        }
    }
    @Test public void calendarGridSwipesMonthButFooterAndEdgeSwipeTabs(){
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            MainActivity host=controller.get();View decor=host.getWindow().getDecorView();
            decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2600,View.MeasureSpec.EXACTLY));decor.layout(0,0,1080,2600);
            ReflectionHelpers.callInstanceMethod(host,"showTab",ReflectionHelpers.ClassParameter.from(String.class,"clock"));
            TripleCalendarView calendar=host.findViewById(R.id.clock_calendar);int[] at=new int[2];calendar.getLocationOnScreen(at);
            float x=at[0]+calendar.getWidth()/2f,y=at[1]+calendar.getWidth()*280f/600;
            int month=ReflectionHelpers.getField(calendar,"displayMonth");
            swipe(host,x,y,-250,false);assertEquals("clock",ReflectionHelpers.getField(host,"currentTab"));assertNotEquals(month,(int)ReflectionHelpers.<Integer>getField(calendar,"displayMonth"));
            float dx=host.findViewById(R.id.tab_bar).getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?450:-450;
            calendar.getLocationOnScreen(at);month=ReflectionHelpers.getField(calendar,"displayMonth");
            swipe(host,at[0]+calendar.getWidth()/2f,at[1]+calendar.getHeight()-4,-dx,false);
            assertEquals("clock",ReflectionHelpers.getField(host,"currentTab"));assertEquals(month,(int)ReflectionHelpers.<Integer>getField(calendar,"displayMonth"));
            android.graphics.Rect rect=new android.graphics.Rect();calendar.getLocalVisibleRect(rect);
            assertTrue("Footer hit: "+rect+" height="+calendar.getHeight()+" y="+at[1],TabSwipeZones.calendar(calendar,at[0]+calendar.getWidth()/2f,at[1]+calendar.getHeight()-4));
            swipe(host,at[0]+calendar.getWidth()/2f,at[1]+calendar.getHeight()-4,dx,false);
            String[] order=AppSettings.tabOrder(host);int index=java.util.Arrays.asList(order).indexOf("clock");assertEquals(order[index+1],ReflectionHelpers.getField(host,"currentTab"));assertEquals(month,(int)ReflectionHelpers.<Integer>getField(calendar,"displayMonth"));
            ReflectionHelpers.callInstanceMethod(host,"showTab",ReflectionHelpers.ClassParameter.from(String.class,"clock"));
            calendar.getLocationOnScreen(at);swipe(host,at[0]+1,at[1]+calendar.getWidth()*280f/600,dx,true);assertEquals("clock",ReflectionHelpers.getField(host,"currentTab"));
            swipe(host,at[0]+1,at[1]+calendar.getWidth()*280f/600,dx,false);assertEquals(order[index+1],ReflectionHelpers.getField(host,"currentTab"));
        }
    }
    void swipe(MainActivity host,float x,float y,float dx,boolean cancel){
        send(host,MotionEvent.ACTION_DOWN,x,y,0);send(host,MotionEvent.ACTION_MOVE,x+dx,y,100);send(host,cancel?MotionEvent.ACTION_CANCEL:MotionEvent.ACTION_UP,x+dx,y,150);
        TabMotion motion=ReflectionHelpers.getField(host,"tabMotion");ValueAnimator animator=ReflectionHelpers.getField(motion,"animator");if(animator!=null)animator.end();motion.finish();
    }
    void send(MainActivity host,int action,float x,float y,long time){MotionEvent event=MotionEvent.obtain(0,time,action,x,y,0);host.dispatchTouchEvent(event);event.recycle();}
}
