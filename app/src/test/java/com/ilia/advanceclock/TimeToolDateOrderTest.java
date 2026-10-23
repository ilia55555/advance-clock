package com.ilia.advanceclock;

import android.app.Application;
import android.graphics.Path;
import android.graphics.RectF;
import android.text.Layout;
import android.view.View;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(application=Application.class,sdk={28,35})
public class TimeToolDateOrderTest {
    @Test public void bothSummariesRenderWrittenDatesInReadingOrderAndNumericDatesYearFirst() {
        Application app=RuntimeEnvironment.getApplication();
        AppString.init(app);
        java.util.TimeZone previous=java.util.TimeZone.getDefault();
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
        long target=java.time.Instant.parse("2026-10-23T11:00:00Z").toEpochMilli();
        try {
            for(String language:new String[]{"fa","ar","en"}) {
                AppSettings.setLanguage(app,language);
                try(var timer=Robolectric.buildActivity(TimerActivity.class).setup();
                    var stopwatch=Robolectric.buildActivity(StopwatchActivity.class).setup()) {
                    Object timerPanel=ReflectionHelpers.getField(timer.get(),"controller");
                    Object stopwatchPanel=ReflectionHelpers.getField(stopwatch.get(),"controller");
                    ReflectionHelpers.setField(timerPanel,"dateTimeMode",true);
                    ReflectionHelpers.setField(timerPanel,"remainingMillis",0L);
                    ReflectionHelpers.setField(stopwatchPanel,"mode",2);
                    for(Object panel:new Object[]{timerPanel,stopwatchPanel}) {
                        ReflectionHelpers.setField(panel,"selectedTarget",target);
                        for(int calendar:new int[]{CalendarUtils.PERSIAN,CalendarUtils.GREGORIAN,CalendarUtils.HIJRI}) {
                            ReflectionHelpers.setField(panel,"calendarType",calendar);
                            for(int format:new int[]{0,1}) {
                                AppSettings.setDateDisplayFormat(app,format);
                                for(boolean full:new boolean[]{true,false}) {
                                    AppSettings.setClock24Hour(app,full);
                                    ReflectionHelpers.callInstanceMethod(panel,"renderEndTime");
                                    TextView summary=ReflectionHelpers.getField(panel,"summary");
                                    assertVisualOrder(summary,target,calendar,format,!language.equals("en"));
                                }
                            }
                        }
                    }
                }
            }
        } finally { java.util.TimeZone.setDefault(previous); }
    }

    private void assertVisualOrder(TextView view,long target,int calendar,int format,boolean rtl) {
        String date=CalendarUtils.formatDate(target,calendar);
        String value=view.getText().toString();
        assertTrue(value.contains(date));
        view.setTextSize(18);
        view.measure(View.MeasureSpec.makeMeasureSpec(1600,View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED));
        view.layout(0,0,1600,view.getMeasuredHeight());
        Layout layout=view.getLayout();
        assertEquals(1,layout.getLineCount());
        int start=value.indexOf(date);
        android.icu.util.Calendar c=CalendarUtils.fromMillis(calendar,target);
        String day=CalendarUtils.fa(c.get(android.icu.util.Calendar.DAY_OF_MONTH));
        String year=CalendarUtils.fa(c.get(android.icu.util.Calendar.YEAR));
        int yearStart=start+date.indexOf(year);
        float yearX=center(layout,yearStart,year.length());
        if(format==1) {
            assertTrue("numeric year must precede month/day: "+value,
                    yearX<center(layout,start+date.indexOf('/'),date.length()-date.indexOf('/')));
        } else {
            float dayX=center(layout,start,day.length());
            int monthStart=start+day.length()+1;
            float monthX=center(layout,monthStart,yearStart-monthStart-1);
            if(rtl) {
                assertTrue("day must be at the reading start: "+value,dayX>monthX);
                assertTrue("year must follow month: "+value,monthX>yearX);
            } else {
                assertTrue(dayX<monthX);
                assertTrue(monthX<yearX);
            }
        }
    }
    private float center(Layout layout,int start,int length) {
        Path path=new Path();
        layout.getSelectionPath(start,start+length,path);
        RectF bounds=new RectF();path.computeBounds(bounds,true);
        return bounds.centerX();
    }
}
