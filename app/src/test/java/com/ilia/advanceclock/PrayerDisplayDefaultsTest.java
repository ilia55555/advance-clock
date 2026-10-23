package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class PrayerDisplayDefaultsTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    private Switch find(View view,String label){
        if(view instanceof Switch && (label==null||((Switch)view).getText().toString().equals(label)))return (Switch)view;
        if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){
            Switch found=find(((ViewGroup)view).getChildAt(i),label);if(found!=null)return found;
        }
        return null;
    }
    @Test public void freshDefaultsAreOnAndExplicitOffChoicesArePreserved(){
        Application app=RuntimeEnvironment.getApplication();assertTrue(AppSettings.adhanEnabled(app));assertTrue(AppSettings.showPrayerTimes(app));
        AppSettings.setAdhanEnabled(app,false);assertTrue(AppSettings.showPrayerTimes(app));
        AppSettings.setShowPrayerTimes(app,false);assertFalse(AppSettings.showPrayerTimes(app));assertFalse(AppSettings.adhanEnabled(app));
    }
    @Test public void firstSetupShowsEnabledAdhanWithLargerLabel(){
        try(var controller=Robolectric.buildActivity(Activity.class).setup()){
            AppSettings.applyTheme(controller.get());FirstRunSetupDialog.show(controller.get(),changed->{});
            AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            Switch toggle=find(dialog.getWindow().getDecorView(),null);assertNotNull(toggle);assertTrue(toggle.isChecked());
            assertEquals(18f*controller.get().getResources().getDisplayMetrics().density,toggle.getTextSize(),.1f);dialog.dismiss();
        }
    }
    @Test public void prayerLabelsUseRequestedSizes(){
        Application app=RuntimeEnvironment.getApplication();
        try(var controller=Robolectric.buildActivity(SettingsActivity.class,new Intent(app,SettingsActivity.class).putExtra(SettingsActivity.EXTRA_SECTION,"prayer")).setup()){
            View root=controller.get().findViewById(android.R.id.content);
            Switch section=find(root,AppString.get(R.string.runtime_text_0361)),bar=find(root,AppString.get(R.string.runtime_text_0351));
            assertNotNull(section);assertNotNull(bar);assertTrue(section.isChecked());assertTrue(bar.isChecked());
            float density=root.getResources().getDisplayMetrics().density;
            assertEquals(18*density,section.getTextSize(),.1f);assertEquals(16*density,bar.getTextSize(),.1f);
        }
    }
}
