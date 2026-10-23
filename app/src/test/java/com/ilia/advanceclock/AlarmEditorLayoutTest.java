package com.ilia.advanceclock;

import android.app.Application;
import android.content.Intent;
import android.graphics.Rect;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class AlarmEditorLayoutTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    @Test public void dateSwitchesHaveVisibleBoundsAndHeaderTouchesSheetEdges(){
        Application app=RuntimeEnvironment.getApplication();
        android.content.res.Configuration config=new android.content.res.Configuration(app.getResources().getConfiguration());config.fontScale=1.5f;
        app.getResources().updateConfiguration(config,app.getResources().getDisplayMetrics());
        for(boolean world:new boolean[]{false,true})try(var controller=Robolectric.buildActivity(AlarmEditorActivity.class,
                new Intent(app,AlarmEditorActivity.class).putExtra("modalCreate",true).putExtra("worldTimeReference",world)).setup().visible()){
            AlarmEditorActivity host=controller.get();ScrollView root=host.findViewById(R.id.alarm_editor_root);
            WindowInsets insets=android.os.Build.VERSION.SDK_INT>=29?
                    new WindowInsets.Builder().setSystemWindowInsets(android.graphics.Insets.of(0,30,0,20)).build():
                    org.robolectric.util.ReflectionHelpers.callConstructor(WindowInsets.class,
                            org.robolectric.util.ReflectionHelpers.ClassParameter.from(Rect.class,new Rect(0,30,0,20)));
            root.dispatchApplyWindowInsets(insets);
            root.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));root.layout(0,0,400,1000);
            View header=host.findViewById(R.id.alarm_editor_header);
            assertEquals(0,root.getPaddingTop());assertEquals(0,header.getTop());assertEquals(0,header.getLeft());
            assertEquals(root.getWidth(),header.getWidth());assertTrue(header.getPaddingTop()>=30);
            Switch date=host.findViewById(R.id.date_option_alarm_editor),automatic=host.findViewById(R.id.auto_delete_alarm_editor);
            for(Switch toggle:new Switch[]{date,automatic}){
                LinearLayout card=(LinearLayout)toggle.getParent();LinearLayout controls=(LinearLayout)card.getParent();
                assertTrue(controls.getHeight()>=48);
                assertEquals(LinearLayout.HORIZONTAL,card.getOrientation());
                assertEquals(1,((TextView)card.getChildAt(0)).getMaxLines());
                assertTrue(Math.abs((toggle.getTop()+toggle.getBottom())/2-(card.getChildAt(0).getTop()+card.getChildAt(0).getBottom())/2)<=1);assertTrue(toggle.getHeight()>0);assertTrue(toggle.isShown());
                assertTrue(toggle.getBottom()<=card.getHeight());assertTrue(card.getBottom()<=controls.getHeight());
            }
            LinearLayout controls=(LinearLayout)date.getParent().getParent();
            View timeRow=(View)host.findViewById(R.id.pick_time).getParent();assertTrue(controls.getBottom()<=timeRow.getTop());
            if(world){LinearLayout field=(LinearLayout)host.findViewById(R.id.alarm_priority).getParent();
                View previous=((ViewGroup)field.getParent()).getChildAt(((ViewGroup)field.getParent()).indexOfChild(field)-1);
                assertFalse(previous instanceof TextView);}
        }
    }
}
