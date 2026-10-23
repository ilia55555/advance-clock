package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import android.view.View;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class DateNotificationBadgeLayoutTest {
    private View badge(Context context,int layout,int day) {
        RemoteViews views=ReflectionHelpers.callStaticMethod(DateNotificationService.class,"dateView",
                ReflectionHelpers.ClassParameter.from(Context.class,context),
                ReflectionHelpers.ClassParameter.from(int.class,layout),
                ReflectionHelpers.ClassParameter.from(String.class,"Date"),
                ReflectionHelpers.ClassParameter.from(String.class,"Other dates"),
                ReflectionHelpers.ClassParameter.from(int.class,day));
        View root=views.apply(context,new FrameLayout(context));
        int width=Math.round(320*context.getResources().getDisplayMetrics().density);
        root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.AT_MOST));
        root.layout(0,0,width,root.getMeasuredHeight());
        return root;
    }

    @Test public void allLatinDaysFitAsOneCompleteCenteredLineInBothNotificationStates() {
        Context context=RuntimeEnvironment.getApplication(); AppString.init(context);
        AppSettings.setLanguage(context,"en");
        for(int layout:new int[]{R.layout.notification_date,R.layout.notification_date_expanded}) {
            for(int day=1;day<=31;day++) {
                View root=badge(context,layout,day);
                TextView number=root.findViewById(R.id.date_notification_day_latin);
                assertEquals(View.VISIBLE,number.getVisibility());
                assertEquals(View.GONE,root.findViewById(R.id.date_notification_day).getVisibility());
                String expected=Integer.toString(day);
                assertEquals(expected,number.getText().toString());
                android.text.Layout text=number.getLayout();
                assertNotNull(text);
                assertEquals(1,text.getLineCount());
                assertEquals(expected.length(),text.getLineEnd(0));
                assertEquals(0,text.getEllipsisCount(0));
                float lineWidth=text.getLineWidth(0);
                float density=context.getResources().getDisplayMetrics().density;
                assertTrue("Both digits must fit comfortably inside border: "+day,
                        lineWidth<=number.getWidth()-8*density);
                float center=number.getCompoundPaddingLeft()
                        +(text.getLineLeft(0)+text.getLineRight(0))/2;
                assertEquals(number.getWidth()/2f,center,1.5f*density);
            }
        }
    }

    @Test public void persianBadgeKeepsItsExistingLayoutAndPadding() {
        Context context=RuntimeEnvironment.getApplication(); AppString.init(context);
        AppSettings.setLanguage(context,"fa");
        AppSettings.setDefaultCalendar(context,CalendarUtils.PERSIAN);
        for(int layout:new int[]{R.layout.notification_date,R.layout.notification_date_expanded}) {
            View root=badge(context,layout,21);
            TextView number=root.findViewById(R.id.date_notification_day);
            assertEquals(View.VISIBLE,number.getVisibility());
            assertEquals("۲۱",number.getText().toString());
            assertEquals(0,number.getPaddingTop());
            assertEquals(0,number.getPaddingBottom());
            assertEquals(View.GONE,root.findViewById(R.id.date_notification_day_latin).getVisibility());
        }
    }
}
