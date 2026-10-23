package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class, sdk={28,35},
        shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class SettingsAccordionTest {
    @Before public void init() {
        Application app=RuntimeEnvironment.getApplication();
        AppString.init(app);
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS);
    }

    @Test public void sixSectionsKeepTheirOrderAndOnlyOneRemainsOpenAfterRecreation() {
        try (var controller=Robolectric.buildActivity(SettingsActivity.class).setup()) {
            SettingsActivity host=controller.get();
            String[] keys={"general","tabs","notification","calendar","alarm","prayer"};
            LinearLayout root=(LinearLayout)section(host,"general").getParent();
            for(int i=0;i<keys.length;i++) assertEquals("settings_section_"+keys[i],root.getChildAt(i+1).getTag());
            assertTrue(section(host,"general").isExpanded());
            for(String key:keys) {
                section(host,key).header.performClick();
                // General initially closes on a second tap; all others open exclusively.
                for(String other:keys) if(!other.equals(key)) assertFalse(section(host,other).isExpanded());
            }
            assertTrue(section(host,"prayer").isExpanded());
            controller.recreate();
            assertTrue(section(controller.get(),"prayer").isExpanded());
            assertFalse(section(controller.get(),"general").isExpanded());
            section(controller.get(),"prayer").header.performClick();
            assertFalse(section(controller.get(),"prayer").isExpanded());
        }
    }

    @Test public void notificationSwitchesSaveInlineAndSoundResultReturnsToSameSection() {
        Intent intent=new Intent(RuntimeEnvironment.getApplication(),SettingsActivity.class)
                .putExtra(SettingsActivity.EXTRA_SECTION,"notification");
        try(var controller=Robolectric.buildActivity(SettingsActivity.class,intent).setup()) {
            SettingsActivity host=controller.get();
            SettingsSection section=section(host,"notification");
            Switch persistent=(Switch)findText(section.content,AppString.get(R.string.runtime_text_0071));
            assertTrue(persistent.isEnabled());
            persistent.setChecked(!persistent.isChecked());
            assertEquals(persistent.isChecked(),AppSettings.persistentDateNotificationEnabled(host));
            assertNull(Shadows.shadowOf(host).getNextStartedActivity());
            host.onActivityResult(902,Activity.RESULT_OK,new Intent().putExtra(SoundPickerActivity.EXTRA_URI,"content://test/audio"));
            assertEquals("content://test/audio",AppSettings.defaultAlarmSoundUri(host));
            assertTrue(section.isExpanded());
        }
    }

    @Test public void prayerOutputAndExceptionsOpenInsidePageAndSaveWithoutSettingsDialog() {
        Intent intent=new Intent(RuntimeEnvironment.getApplication(),SettingsActivity.class)
                .putExtra(SettingsActivity.EXTRA_SECTION,"prayer");
        try(var controller=Robolectric.buildActivity(SettingsActivity.class,intent).setup()) {
            SettingsActivity host=controller.get();
            SettingsSection section=section(host,"prayer");
            TextView all=findText(section.content,AppString.get(R.string.runtime_text_0343));
            assertNotNull(all);
            ((View)all.getParent()).performClick();
            TextView output=findText(section.content,AppString.get(R.string.runtime_text_0345));
            assertNotNull(output);
            assertTrue(allParentsVisible(output));
            Switch vibrate=(Switch)findText(section.content,AppString.get(R.string.runtime_text_0094));
            vibrate.setChecked(!vibrate.isChecked());
            for(int type=AdhanScheduler.FAJR;type<=AdhanScheduler.ISHA;type++)
                assertEquals(vibrate.isChecked(),AppSettings.adhanVibrate(host,type));
            assertNotNull(findText(section.content,AppString.get(R.string.adhan_skip_title)));
            assertNull(Shadows.shadowOf(host).getNextStartedActivity());
            section(host,"calendar").header.performClick();
            assertFalse(allParentsVisible(vibrate));
            assertTrue(section(host,"calendar").isExpanded());
        }
    }

    @Test public void existingWidgetShortcutsOpenMatchingUnifiedSection() {
        try(var controller=Robolectric.buildActivity(PrayerSettingsActivity.class).setup()) {
            assertRedirect(controller.get(),"prayer");
        }
        try(var controller=Robolectric.buildActivity(NotificationSettingsActivity.class).setup()) {
            assertRedirect(controller.get(),"notification");
        }
    }

    private void assertRedirect(Activity activity,String key) {
        Intent next=Shadows.shadowOf(activity).getNextStartedActivity();
        assertEquals(SettingsActivity.class.getName(),next.getComponent().getClassName());
        assertEquals(key,next.getStringExtra(SettingsActivity.EXTRA_SECTION));
        assertTrue(activity.isFinishing());
    }
    private SettingsSection section(Activity host,String key) {
        return host.findViewById(android.R.id.content).findViewWithTag("settings_section_"+key);
    }
    private TextView findText(View view,String text) {
        if(view instanceof TextView && ((TextView)view).getText().toString().equals(text)) return (TextView)view;
        if(view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) {
                TextView found=findText(group.getChildAt(i),text);
                if(found!=null) return found;
            }
        }
        return null;
    }
    private boolean allParentsVisible(View view) {
        if(view.getVisibility()!=View.VISIBLE) return false;
        return !(view.getParent() instanceof View) || allParentsVisible((View)view.getParent());
    }
}
