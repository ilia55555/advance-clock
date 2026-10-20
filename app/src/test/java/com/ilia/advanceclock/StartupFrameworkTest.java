package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {28, 35},
        shadows = ReminderScreenFrameworkTest.PlainStore.class)
public final class StartupFrameworkTest {
    @Test public void toolLayoutsInflateOnDemandAndSurviveResume() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        AppString.init(context);
        context.getSharedPreferences("advance_clock_app", 0).edit()
                .putBoolean("language_calendar_setup_v1", true)
                .putBoolean("permission_onboarding_v2", true).commit();
        org.robolectric.android.controller.ActivityController<MainActivity> controller =
                Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity activity = controller.get();
        int[] ids = {R.id.stopwatch_panel, R.id.timer_panel, R.id.world_panel};
        for (int id : ids) {
            ViewGroup panel = activity.findViewById(id);
            assertTrue(panel.getChildAt(0) instanceof ViewStub);
        }
        Method show = MainActivity.class.getDeclaredMethod("showTab", String.class);
        show.setAccessible(true);
        String[] tabs = {"stopwatch", "timer", "world"};
        for (int i = 0; i < tabs.length; i++) {
            show.invoke(activity, tabs[i]);
            ViewGroup panel = activity.findViewById(ids[i]);
            View inflated = panel.getChildAt(0);
            assertFalse(inflated instanceof ViewStub);
            assertEquals(1, panel.getChildCount());
            show.invoke(activity, "clock");
            show.invoke(activity, tabs[i]);
            assertSame(inflated, panel.getChildAt(0));
        }
        assertNotNull(activity.findViewById(R.id.world_add_widget));
        controller.pause().stop().start().resume();
        controller.pause().stop().destroy();
    }

    @Test public void calendarSharesMonthButInvalidatesForLanguageAndMonth() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        AppSettings.setLanguage(context, "en"); AppString.init(context);
        TripleCalendarView a = new TripleCalendarView(context);
        TripleCalendarView b = new TripleCalendarView(context);
        b.setSelectedMillis(a.getSelectedMillis());
        Field days = TripleCalendarView.class.getDeclaredField("cachedDays");
        days.setAccessible(true);
        Object initial = days.get(a);
        assertSame(initial, days.get(b));
        a.refreshCalendar();
        assertSame(initial, days.get(a));
        AppSettings.setLanguage(context, "fa"); AppString.init(context);
        a.refreshCalendar();
        assertNotSame(initial, days.get(a));
        b.refreshCalendar();
        assertSame(days.get(a), days.get(b));
        java.util.Calendar next = java.util.Calendar.getInstance();
        next.setTimeInMillis(a.getSelectedMillis()); next.add(java.util.Calendar.MONTH, 2);
        b.setSelectedMillis(next.getTimeInMillis());
        assertNotSame(days.get(a), days.get(b));
    }
}
