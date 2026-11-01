package com.ilia.advanceclock;

import android.Manifest;
import android.app.Application;
import android.app.AlarmManager;
import android.content.Intent;
import android.provider.Settings;
import android.view.ViewGroup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class PermissionCenterTest {
    @Test public void opensAllPermissionStatusesDirectlyWithoutRequestingPermissions() {
        AppString.init(RuntimeEnvironment.getApplication());
        try(var controller=Robolectric.buildActivity(PermissionCenterActivity.class).setup()) {
            PermissionCenterActivity activity=controller.get();
            ViewGroup root=activity.findViewById(android.R.id.content);
            assertNull(root.findViewWithTag("permission_review"));
            for(int i=0;i<5;i++)assertNotNull(root.findViewWithTag("permission_"+i));
            assertNull(Shadows.shadowOf(activity).getLastRequestedPermission());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
    @Test public void alarmAndInstallSettingsOpenOnlyTheirOwnPackageOnTap() {
        AppString.init(RuntimeEnvironment.getApplication());
        Intent intent=new Intent(RuntimeEnvironment.getApplication(),PermissionCenterActivity.class).putExtra("review",true);
        try(var controller=Robolectric.buildActivity(PermissionCenterActivity.class,intent).setup()) {
            PermissionCenterActivity activity=controller.get();
            ViewGroup root=activity.findViewById(android.R.id.content);
            root.findViewWithTag("permission_4").performClick();
            Intent install=Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,install.getAction());
            assertEquals("package:"+activity.getPackageName(),install.getDataString());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
    @Test public void oneTapContinuesAfterDenialSkipsGrantedAndNeverLoops() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS,
                Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
        Shadows.shadowOf(app.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);
        try(var controller=Robolectric.buildActivity(PermissionCenterActivity.class).setup()) {
            PermissionCenterActivity activity=controller.get();
            ((ViewGroup)activity.findViewById(android.R.id.content)).findViewWithTag("permission_3").performClick();
            Shadows.shadowOf(activity).getNextStartedActivity(); // System location prompt.
            activity.onRequestPermissionsResult(103,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},new int[]{-1});
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            int next=org.robolectric.util.ReflectionHelpers.getField(activity,"pendingKind");
            assertNotEquals(3,next);
            int seen=1 << 3;
            for(int step=0;step<5 && next>=0;step++) {
                assertEquals(0,seen & (1 << next));seen |= 1 << next;
                if(next==0 && android.os.Build.VERSION.SDK_INT>=33)
                    activity.onRequestPermissionsResult(100,new String[]{Manifest.permission.POST_NOTIFICATIONS},new int[]{-1});
                else activity.onActivityResult(620,android.app.Activity.RESULT_CANCELED,null);
                Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
                next=org.robolectric.util.ReflectionHelpers.getField(activity,"pendingKind");
            }
            assertEquals(-1,next);
            assertFalse((Boolean)org.robolectric.util.ReflectionHelpers.getField(activity,"batchRunning"));
            assertEquals(0,seen & (1 << 1)); // Exact alarms already allowed.
        }
    }

    @Test public void deniedPermissionsWarningIsDismissibleAndMenuEntryRemains() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        app.getSharedPreferences("advance_clock_app",0).edit()
                .putBoolean("language_calendar_setup_v1",true)
                .putBoolean("permission_onboarding_v2",true).commit();
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity=controller.get();
            activity.onActivityResult(PermissionCenterActivity.REQUEST,android.app.Activity.RESULT_CANCELED,null);
            Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
            android.widget.PopupWindow warning=org.robolectric.util.ReflectionHelpers.getField(activity,"permissionWarning");
            assertNotNull(warning);assertTrue(warning.isShowing());
            warning.getContentView().findViewWithTag("permissions_warning_close").performClick();
            assertFalse(warning.isShowing());
            activity.findViewById(R.id.header_menu).performClick();
            android.widget.PopupWindow menu=Shadows.shadowOf(app).getLatestPopupWindow();
            android.widget.ScrollView scroll=(android.widget.ScrollView)menu.getContentView();
            ViewGroup list=(ViewGroup)scroll.getChildAt(0);
            ViewGroup first=(ViewGroup)list.getChildAt(0);
            assertEquals(activity.getString(R.string.permissions_menu),((android.widget.TextView)first.getChildAt(1)).getText().toString());
            first.performClick();
            Intent target=Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals(PermissionCenterActivity.class.getName(),target.getComponent().getClassName());
        }
    }

    @Test public void locationRequestDoesNotRequestBackgroundLocationOrStartGps() {
        Application app=RuntimeEnvironment.getApplication();AppString.init(app);
        Shadows.shadowOf(app).denyPermissions(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION);
        try(var controller=Robolectric.buildActivity(PermissionCenterActivity.class,
                new Intent(app,PermissionCenterActivity.class).putExtra("review",true)).setup()) {
            PermissionCenterActivity activity=controller.get();
            ((ViewGroup)activity.findViewById(android.R.id.content)).findViewWithTag("permission_3").performClick();
            assertArrayEquals(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},
                    Shadows.shadowOf(activity).getLastRequestedPermission().requestedPermissions);
            Intent request=Shadows.shadowOf(activity).getNextStartedActivity();
            assertEquals("android.content.pm.action.REQUEST_PERMISSIONS",request.getAction());
            assertNull(Shadows.shadowOf(activity).getNextStartedActivity());
        }
    }
}
