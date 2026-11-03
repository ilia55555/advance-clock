package com.ilia.advanceclock;

import android.app.Application;
import android.content.Intent;
import android.view.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class PrayerBarTapTest {
    private void touch(View view,int action,float x,float y,long time){
        MotionEvent e=MotionEvent.obtain(0,time,action,x,y,0);view.dispatchTouchEvent(e);e.recycle();
    }
    @Test public void clickOpensPrayerAccordionDirectly() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        app.getSharedPreferences("advance_clock_app",0).edit().putBoolean("language_calendar_setup_v1",true)
                .putBoolean("permission_onboarding_v2",true).commit();
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.prayer_times_scroll).performClick();
            Intent intent=Shadows.shadowOf(controller.get()).getNextStartedActivity();
            assertEquals(SettingsActivity.class.getName(),intent.getComponent().getClassName());
            assertEquals("prayer",intent.getStringExtra(SettingsActivity.EXTRA_SECTION));
        }
    }
    @Test public void dragsAndLongPressNeverOpenSettingsButTapDoes() {
        TapOnlyHorizontalScrollView strip=new TapOnlyHorizontalScrollView(RuntimeEnvironment.getApplication());
        View child=new View(RuntimeEnvironment.getApplication());child.setMinimumWidth(1200);child.setMinimumHeight(80);child.setClickable(true);
        strip.addView(child);int[] clicks={0};strip.setOnTapListener(()->clicks[0]++);
        strip.measure(View.MeasureSpec.makeMeasureSpec(300,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY));strip.layout(0,0,300,80);
        touch(strip,0,200,40,0);touch(strip,2,50,40,30);touch(strip,1,50,40,60);assertEquals(0,clicks[0]);
        touch(strip,0,100,40,100);touch(strip,2,100,0,130);touch(strip,1,100,0,160);assertEquals(0,clicks[0]);
        touch(strip,0,100,40,200);touch(strip,1,100,40,1000);assertEquals(0,clicks[0]);
        touch(strip,0,100,40,1100);touch(strip,1,100,40,1150);assertEquals(1,clicks[0]);
    }
}
