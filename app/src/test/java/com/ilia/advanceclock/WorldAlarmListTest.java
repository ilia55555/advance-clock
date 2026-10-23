package com.ilia.advanceclock;

import android.app.Application;
import android.view.View;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class WorldAlarmListTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    @Test public void bothTabsShareSummaryAndWorldActionsUpdateBothLists(){
        Application app=RuntimeEnvironment.getApplication();
        AppSettings.setLanguage(app,"fa");AppString.init(app);
        WorldClockStore.save(app,Collections.singletonList("Asia/Tehran"));WorldClockStore.saveLabel(app,"Asia/Tehran","اصفهان");
        AlarmItem alarm=new AlarmItem(7,"Test",System.currentTimeMillis()+86_400_000L,0,true);
        alarm.referenceZoneId="Asia/Tehran";alarm.createdFromWorld=true;new AlarmStore(app).save(alarm);
        new AlarmStore(app).save(new AlarmItem(8,"Phone",System.currentTimeMillis()+86_400_000L,0,true));
        AlarmItem independent=new AlarmItem(9,"Other source",System.currentTimeMillis()+86_400_000L,0,true);
        independent.referenceZoneId="Asia/Tehran";new AlarmStore(app).save(independent);
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            MainActivity host=controller.get();host.findViewById(R.id.tab_world).performClick();
            LinearLayout world=host.findViewById(R.id.world_alarm_list),clock=host.findViewById(R.id.alarm_list);
            assertNotNull(world);assertEquals(1,world.getChildCount());assertEquals(3,clock.getChildCount());
            assertNull(world.findViewWithTag("alarm_summary_8"));assertNull(world.findViewWithTag("alarm_summary_9"));
            LinearLayout parent=(LinearLayout)world.getParent();
            assertTrue(parent.indexOfChild(host.findViewById(R.id.world_clock_list))<parent.indexOfChild(host.findViewById(R.id.world_alarms_divider)));
            assertTrue(parent.indexOfChild(host.findViewById(R.id.world_alarms_divider))<parent.indexOfChild(world));
            TextView worldText=world.findViewWithTag("alarm_summary_7"),clockText=clock.findViewWithTag("alarm_summary_7");
            assertEquals(clockText.getText().toString(),worldText.getText().toString());
            String text=worldText.getText().toString();assertEquals(1,text.split("اصفهان",-1).length-1);
            assertFalse(text.contains("GMT"));
            LinearLayout card=(LinearLayout)world.getChildAt(0),actions=(LinearLayout)card.getChildAt(card.getChildCount()-1);
            actions.getChildAt(actions.getChildCount()-1).performClick();assertFalse(new AlarmStore(app).find(7).enabled);
            assertNotNull(clock.findViewWithTag("alarm_summary_7"));
            card=(LinearLayout)world.getChildAt(0);actions=(LinearLayout)card.getChildAt(card.getChildCount()-1);
            View delete=actions.getChildAt(0);delete.performClick();assertNotNull(new AlarmStore(app).find(7));delete.performClick();
            assertNull(new AlarmStore(app).find(7));assertNull(world.findViewWithTag("alarm_summary_7"));assertNull(clock.findViewWithTag("alarm_summary_7"));
        }
    }
    @Test public void phoneReferenceUsesOriginalCityWhileExplicitCityUsesSavedLabel(){
        Application app=RuntimeEnvironment.getApplication();AppSettings.setLanguage(app,"fa");AppString.init(app);
        TimeZone previous=TimeZone.getDefault();
        try{
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tehran"));WorldClockStore.saveLabel(app,"Asia/Tehran","اصفهان");
            AlarmItem item=new AlarmItem(1,"",System.currentTimeMillis(),0,true);
            assertEquals("تهران",WorldAlarmReference.referenceName(app,item));
            item.referenceZoneId="Asia/Tehran";assertEquals("اصفهان",WorldAlarmReference.referenceName(app,item));
            item.referenceZoneId="";TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            assertEquals("گرینویچ",WorldAlarmReference.referenceName(app,item));
        }finally{TimeZone.setDefault(previous);}
    }
}
