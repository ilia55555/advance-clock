package com.ilia.advanceclock;

import android.app.*;
import android.content.Context;
import android.graphics.PorterDuffColorFilter;
import android.view.View;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class DateNotificationHolidayTest {
    private Context app;
    @Before public void setup() {
        app=RuntimeEnvironment.getApplication();AppString.init(app);
        AppSettings.setDefaultCalendar(app,CalendarUtils.PERSIAN);
    }
    private long date(int year,int month,int day) { return CalendarUtils.toMillis(CalendarUtils.GREGORIAN,year,month-1,day,12,0); }
    private void assertCard(long date, String label, boolean red) {
        Notification n=DateNotificationService.buildNotification(app,date);
        String title=n.extras.getCharSequence(Notification.EXTRA_TITLE).toString();
        assertEquals(!label.isEmpty(),title.endsWith(" ("+label+")"));
        for(RemoteViews rv:new RemoteViews[]{n.contentView,n.bigContentView}) {
            View view=rv.apply(app,new FrameLayout(app));
            ImageView border=view.findViewById(R.id.date_notification_day_border);
            int color=Shadows.shadowOf((PorterDuffColorFilter)border.getColorFilter()).getColor();
            assertEquals(red ? 0xFFC62828 : AppSettings.primaryColor(app),color);
            assertEquals(AppSettings.primaryColor(app),((TextView)view.findViewById(R.id.date_notification_day)).getCurrentTextColor());
            assertEquals(title,((TextView)view.findViewById(R.id.date_notification_title)).getText().toString());
        }
    }
    @Test public void officialHolidayUsesOfficialLabelAndRedInnerRingInBothStates() {
        long nowruz=CalendarUtils.toMillis(CalendarUtils.PERSIAN,1405,0,1,12,0);
        assertCard(nowruz,AppString.get(R.string.calendar_event_text_007),true);
    }
    @Test public void weeklyHolidayAndOrdinaryDayAreDifferentAndDoNotRetainRed() {
        assertCard(date(2026,10,30),AppString.get(R.string.date_notification_holiday),true);
        assertCard(date(2026,10,28),"",false);
    }
    @Test public void enabledSourcesAndSelectedCalendarsFollowMainCalendarRules() {
        AppSettings.setDefaultCalendar(app,CalendarUtils.GREGORIAN);
        long nowruz=CalendarUtils.toMillis(CalendarUtils.PERSIAN,1405,0,1,12,0);
        AppSettings.setAdditionalCalendarEventsEnabled(app,CalendarUtils.PERSIAN,false);
        assertEquals("",DateNotificationService.holidayLabel(app,nowruz,CalendarUtils.GREGORIAN));
        AppSettings.setAdditionalCalendarEventsEnabled(app,CalendarUtils.PERSIAN,true);
        assertEquals(AppString.get(R.string.calendar_event_text_007),DateNotificationService.holidayLabel(app,nowruz,CalendarUtils.GREGORIAN));
    }
}
