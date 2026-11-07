package com.ilia.advanceclock;

import android.app.*;
import android.os.Looper;
import android.view.*;
import java.time.Instant;
import java.time.Duration;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class WorldTimeRulerTest {
    private long at(String text){return Instant.parse(text).toEpochMilli();}
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());AppTime.configure("Asia/Tehran");}
    @After public void reset(){AppTime.configure("");}
    @Test public void magnetsUseLocalHoursAndHalfHoursWithStrictFiveMinuteRadius(){
        // Tehran 11:57 -> 12:00, and 12:03 -> 12:00.
        assertEquals(at("2026-11-07T08:30:00Z"),WorldTimeRuler.anchor(at("2026-11-07T08:27:00Z")));
        assertEquals(at("2026-11-07T08:30:00Z"),WorldTimeRuler.anchor(at("2026-11-07T08:33:00Z")));
        assertEquals(at("2026-11-07T09:00:00Z"),WorldTimeRuler.anchor(at("2026-11-07T08:57:00Z")));
        assertEquals(at("2026-11-07T08:35:00Z"),WorldTimeRuler.anchor(at("2026-11-07T08:35:00Z")));
        AppTime.configure("Asia/Kathmandu");
        assertEquals(at("2026-11-07T18:15:00Z"),WorldTimeRuler.anchor(at("2026-11-07T18:12:00Z"))); // midnight
    }
    private void touch(WorldTimeRuler view,int action,float x){
        MotionEvent e=MotionEvent.obtain(0,0,action,x,44,0);assertTrue(view.onTouchEvent(e));e.recycle();
    }
    @Test public void dragIsContinuousClampedAndSettlesOnlyAfterRelease(){
        try(var activity=Robolectric.buildActivity(Activity.class).setup()){
            WorldTimeRuler view=new WorldTimeRuler(activity.get(),null);activity.get().setContentView(view);view.layout(0,0,1000,200);
            long[] value={0};view.setListener((time,now)->value[0]=time);
            touch(view,MotionEvent.ACTION_DOWN,500);
            long origin=ReflectionHelpers.getField(view,"origin");
            float padding=6*view.getResources().getDisplayMetrics().density;
            // Pick a known wall-clock half-hour plus three minutes, away from now.
            java.util.Calendar c=java.util.Calendar.getInstance(AppTime.zone());c.setTimeInMillis(origin);
            c.set(java.util.Calendar.MINUTE,33);c.set(java.util.Calendar.SECOND,0);c.set(java.util.Calendar.MILLISECOND,0);c.add(java.util.Calendar.HOUR_OF_DAY,2);long target=c.getTimeInMillis();
            float x=500-(float)((target-origin)/(2.0*WorldTimeRuler.VISIBLE))*(1000-2*padding);
            touch(view,MotionEvent.ACTION_MOVE,x);assertTrue(Math.abs(value[0]-target)<100);
            long raw=value[0];assertNotEquals(WorldTimeRuler.anchor(raw),raw);
            touch(view,MotionEvent.ACTION_UP,x);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(300));
            assertEquals(WorldTimeRuler.anchor(raw),value[0]);
            touch(view,MotionEvent.ACTION_DOWN,500);origin=ReflectionHelpers.getField(view,"origin");
            touch(view,MotionEvent.ACTION_MOVE,5000);assertEquals(origin-WorldTimeRuler.RANGE,value[0]);
            touch(view,MotionEvent.ACTION_MOVE,-5000);assertEquals(origin+WorldTimeRuler.RANGE,value[0]);
            touch(view,MotionEvent.ACTION_CANCEL,1100);long stopped=value[0];Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));assertEquals(stopped,value[0]);
        }
    }
    @Test public void rulerUpdatesAllCityClocksWithoutChangingAlarmDataAndNowResetsIt(){
        try(var activity=Robolectric.buildActivity(Activity.class).setup()){
            Activity host=activity.get();View root=host.getLayoutInflater().inflate(R.layout.panel_world_clock,null);host.setContentView(root);
            WorldClockPanelController panel=new WorldClockPanelController(host,root);
            WorldTimeRuler view=root.findViewById(R.id.world_time_ruler);view.layout(0,0,1000,200);
            android.view.ViewGroup parent=(android.view.ViewGroup)view.getParent();
            assertEquals(parent.indexOfChild(root.findViewById(R.id.world_reference_now))+1,parent.indexOfChild(view));
            assertEquals(12*60*60_000L,WorldTimeRuler.VISIBLE);
            int alarms=new AlarmStore(host).all().size();
            touch(view,MotionEvent.ACTION_DOWN,500);touch(view,MotionEvent.ACTION_MOVE,-100);long shown=ReflectionHelpers.getField(panel,"referenceMillis");
            assertTrue(ReflectionHelpers.<Boolean>getField(panel,"referenceMode"));
            assertTrue(shown>System.currentTimeMillis()+12*60*60_000L);
            java.util.ArrayList<?> rows=ReflectionHelpers.getField(panel,"clockRows");
            for(Object row:rows){
                android.widget.TextView time=ReflectionHelpers.getField(row,"timeView");
                java.util.TimeZone zone=ReflectionHelpers.getField(row,"zone");
                assertEquals(TimeFormat.formatter(host,false,zone).format(new java.util.Date(shown)),time.getText().toString());
            }
            assertEquals(View.VISIBLE,root.findViewById(R.id.world_reference_now).getVisibility());
            root.findViewById(R.id.world_reference_now).performClick();
            assertFalse(ReflectionHelpers.<Boolean>getField(panel,"referenceMode"));
            assertEquals(alarms,new AlarmStore(host).all().size());
            touch(view,MotionEvent.ACTION_CANCEL,900);panel.onPause();
        }
    }
    @Test public void draggingRulerEvenAtScreenEdgeDoesNotSwitchTabs(){
        try(var activity=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            MainActivity host=activity.get();host.findViewById(R.id.tab_world).performClick();
            TabMotion motion=ReflectionHelpers.getField(host,"tabMotion");motion.finish();
            View decor=host.getWindow().getDecorView();
            decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));decor.layout(0,0,1080,1920);
            View ruler=host.findViewById(R.id.world_time_ruler);android.graphics.Rect bounds=new android.graphics.Rect();assertTrue(ruler.getGlobalVisibleRect(bounds));
            TabBarSwipeTest events=new TabBarSwipeTest();
            events.send(host,MotionEvent.ACTION_DOWN,bounds.left+1,bounds.centerY(),0);
            assertFalse(ReflectionHelpers.<Boolean>getField(host,"tabSwipeAllowed"));
            events.send(host,MotionEvent.ACTION_MOVE,bounds.left+300,bounds.centerY(),80);
            events.send(host,MotionEvent.ACTION_UP,bounds.left+300,bounds.centerY(),120);motion.finish();
            assertEquals("world",ReflectionHelpers.getField(host,"currentTab"));
        }
    }
}
