package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class UnifiedDatePickerTest {
    @Test public void widgetDateModesAlsoOpenTheSameCalendar() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        app.getSharedPreferences("time_tools",0).edit().putBoolean("timer_date_mode",true).putInt("stopwatch_mode",2).commit();
        for(String tool:new String[]{"timer","stopwatch"}) {
            Intent intent=new Intent(app,TimeToolsWidgetPickerActivity.class)
                    .putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,22).putExtra("tool",tool);
            try(var controller=Robolectric.buildActivity(TimeToolsWidgetPickerActivity.class,intent).setup()) {
                controller.get().findViewById(android.R.id.content).findViewWithTag("time_tools_date_picker").performClick();
                checkCalendar();
            }
        }
    }

    private void checkCalendar() {
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull(dialog);
        assertNotNull(dialog.findViewById(android.R.id.content).findViewWithTag("date_picker_calendar"));
        dialog.dismiss();
    }
    @Test public void alarmAndNoteEditorsOpenTheSameCalendar() {
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(AlarmEditorActivity.class).setup()) {
            android.view.View date=org.robolectric.util.ReflectionHelpers.getField(controller.get(),"dateButton");
            date.setEnabled(true);date.performClick();checkCalendar();
        }
        try(var controller=Robolectric.buildActivity(NoForgetEditorActivity.class).setup()) {
            controller.get().findViewById(R.id.note_date).setEnabled(true);
            controller.get().findViewById(R.id.note_date).performClick();checkCalendar();
        }
    }
    @Test public void timerAndStopwatchDateModesOpenTheSameCalendar() {
        AppString.init(RuntimeEnvironment.getApplication());
        RuntimeEnvironment.getApplication().getSharedPreferences("advance_clock_app",0).edit()
                .putBoolean("language_calendar_setup_v1",true).putBoolean("permission_onboarding_v2",true).commit();
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity host=controller.get();
            host.findViewById(R.id.tab_timer).performClick();
            host.findViewById(R.id.timer_mode_datetime).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            host.findViewById(R.id.timer_date).performClick();checkCalendar();
            host.findViewById(R.id.tab_stopwatch).performClick();
            host.findViewById(R.id.stopwatch_mode_datetime).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            host.findViewById(R.id.stopwatch_date).performClick();checkCalendar();
        }
    }
}
