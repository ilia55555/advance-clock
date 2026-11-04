package com.ilia.advanceclock;

import android.app.*;
import android.widget.*;
import android.view.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class EventReminderDialogTest {
    @Test public void accordionChoicesSaveBothDirectionsAndLimitCustomRowsToTen(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();String[] result={null};int[] mode={-1};
            AlarmReminderDialog.show(host,0,"[]",false,System.currentTimeMillis()+3*86400000L,AppTime.zone(),(m,j)->{mode[0]=m;result[0]=j;});
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            ViewGroup root=dialog.findViewById(android.R.id.content);
            SettingsSection before=root.findViewWithTag("settings_section_before");
            assertFalse(before.isExpanded());root.findViewWithTag("settings_header_before").performClick();assertTrue(before.isExpanded());
            root.findViewWithTag("settings_header_before_hours").performClick();
            SettingsSection hours=root.findViewWithTag("settings_section_before_hours");assertTrue(hours.isExpanded());
            root.findViewWithTag("settings_header_before_days").performClick();assertFalse(hours.isExpanded());
            ((CheckBox)root.findViewWithTag("event_rule_before_day_1")).setChecked(true);
            ((CheckBox)root.findViewWithTag("event_rule_after_hour_6")).setChecked(true);
            assertNull(root.findViewWithTag("event_before_amount"));
            root.findViewWithTag("event_before_date").performClick();
            AlertDialog calendar=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertNotSame(dialog,calendar);calendar.dismiss();
            for(int i=1;i<=11;i++){
                root.findViewWithTag("event_before_time").performClick();
                TimePickerDialog time=(TimePickerDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
                time.updateTime(10,i);time.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
                root.findViewWithTag("event_before_add").performClick();
            }
            assertEquals(host.getString(R.string.reminder_count_summary,CalendarUtils.fa(12)),
                    ((TextView)root.findViewWithTag("event_reminder_count")).getText().toString());
            assertNull(result[0]);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            assertEquals(AlarmReminderUtils.MODE_CUSTOM,mode[0]);
            List<EventReminderRules.Rule> rules=EventReminderRules.parse(mode[0],result[0],false);assertEquals(12,rules.size());
            assertEquals(10,rules.stream().filter(r->r.custom).count());
            assertTrue(rules.stream().anyMatch(r->r.before && r.unit.equals("day") && r.amount==1));
            assertTrue(rules.stream().anyMatch(r->!r.before && r.unit.equals("hour") && r.amount==6));
            assertEquals(host.getString(R.string.reminder_count_summary,CalendarUtils.fa(12)),AlarmReminderUtils.summary(mode[0],result[0]));
        }
    }
    @Test public void customCalendarAndClockSaveExactSelectedTimeAndRepeatItsOffset(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            Activity host=controller.get();TimeZone zone=AppTime.zone();
            Calendar event=Calendar.getInstance(zone);event.setTimeInMillis(System.currentTimeMillis()+10*86400000L);
            event.set(Calendar.HOUR_OF_DAY,9);event.set(Calendar.MINUTE,0);event.set(Calendar.SECOND,0);event.set(Calendar.MILLISECOND,0);
            Calendar target=(Calendar)event.clone();target.add(Calendar.DAY_OF_YEAR,-3);target.set(Calendar.HOUR_OF_DAY,18);target.set(Calendar.MINUTE,25);
            String[] saved={null};
            AlarmReminderDialog.show(host,0,"[]",false,event.getTimeInMillis(),zone,(m,j)->saved[0]=j);
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();View root=dialog.findViewById(android.R.id.content);
            root.findViewWithTag("event_before_date").performClick();
            AlertDialog calendar=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            ((TripleCalendarView)calendar.findViewById(android.R.id.content).findViewWithTag("date_picker_calendar")).setSelectedMillis(target.getTimeInMillis());
            calendar.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            root.findViewWithTag("event_before_time").performClick();
            TimePickerDialog clock=(TimePickerDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            clock.updateTime(18,25);clock.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            root.findViewWithTag("event_before_add").performClick();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            EventReminderRules.Rule rule=EventReminderRules.parse(2,saved[0],false).get(0);
            assertTrue(rule.before);assertTrue(rule.custom);assertEquals(target.getTimeInMillis(),rule.at(event.getTimeInMillis(),zone));
            long next=rule.next(event.getTimeInMillis(),RecurrenceUtils.WEEKLY,1,"[]",zone,target.getTimeInMillis());
            assertEquals(target.getTimeInMillis()+7*86400000L,rule.at(next,zone));
        }
    }

    @Test public void compactHourAndMinutePresetsKeepOldSelectionsRemovable(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()){
            AlarmReminderDialog.show(controller.get(),2,EventReminderRules.encode(java.util.Arrays.asList(new EventReminderRules.Rule(true,"hour",8,false))),(m,j)->{});
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();View root=dialog.findViewById(android.R.id.content);
            for(int hour:new int[]{1,2,3,6,12,18,24})assertNotNull(root.findViewWithTag("event_rule_after_hour_"+hour));
            assertNull(root.findViewWithTag("event_rule_after_hour_4"));assertNotNull(root.findViewWithTag("event_rule_before_minute_20"));assertNotNull(root.findViewWithTag("event_rule_before_minute_40"));
            assertTrue(((CheckBox)root.findViewWithTag("event_rule_before_hour_8")).isChecked());
            dialog.dismiss();
        }
    }
    @Test public void recurrenceHoursRoundTripAsMinutesWithoutChangingStoredMode(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            int[] value={0};
            RecurrenceDialog.show(controller.get(),System.currentTimeMillis()+86400000L,RecurrenceUtils.INTERVAL_MINUTES,120,"[]",
                    (mode,interval,dates)->{assertEquals(RecurrenceUtils.INTERVAL_MINUTES,mode);value[0]=interval;});
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            View root=dialog.findViewById(android.R.id.content);
            assertEquals(1,((Spinner)root.findViewWithTag("recurrence_interval_unit")).getSelectedItemPosition());
            NumberPicker number=root.findViewWithTag("recurrence_interval_value");assertEquals(2,number.getValue());
            number.setValue(3);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(180,value[0]);
        }
    }

    @Test public void multimonthRecurrenceCanBeEditedAndSaved(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            String[] result={null};
            RecurrenceDialog.show(controller.get(),System.currentTimeMillis()+86400000L,RecurrenceUtils.MONTHLY,1,RecurrenceUtils.periodJson(3),
                    (mode,interval,dates)->result[0]=dates);
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            NumberPicker picker=dialog.findViewById(android.R.id.content).findViewWithTag("recurrence_interval_value");
            assertEquals(3,picker.getValue());picker.setValue(2);dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();assertEquals(2,RecurrenceUtils.period(result[0]));
        }
    }

    @Test public void cancellingKeepsExistingConfigurationAndLegacyNoteRemindersStayBeforeDue(){
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
            int[] applied={0};
            AlarmReminderDialog.show(controller.get(),2,"[60]",true,(m,j)->applied[0]++);
            AlertDialog dialog=(AlertDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
            assertTrue(((CheckBox)dialog.findViewById(android.R.id.content).findViewWithTag("event_rule_before_hour_1")).isChecked());
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();assertEquals(0,applied[0]);
        }
    }
}
