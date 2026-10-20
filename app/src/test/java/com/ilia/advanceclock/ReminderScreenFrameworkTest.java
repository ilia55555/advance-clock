package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {28, 30, 33, 35}, shadows = ReminderScreenFrameworkTest.PlainStore.class)
public final class ReminderScreenFrameworkTest {
    @Test public void sharedAlertSurfaceOpensAndLaysOutWithoutAttachments() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        AppString.init(host);
        AppSettings.applyTheme(host);
        ReminderScreen.prepareWindow(host);
        ReminderScreen.Content data = new ReminderScreen.Content();
        data.title = "Alarm / note / timer / stopwatch";
        data.body = "Alert body";
        ReminderScreen screen = new ReminderScreen(host, data, () -> {}, () -> {}, minutes -> {});
        host.setContentView(screen);
        View decor = host.getWindow().getDecorView();
        decor.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
        decor.layout(0, 0, 1080, 1920);
        screen.getViewTreeObserver().dispatchOnGlobalLayout();
        host.finish();
    }
    @org.robolectric.annotation.Implements(value = SecurePreferences.class, isInAndroidSdk = false)
    public static class PlainStore {
        @org.robolectric.annotation.Implementation protected static String read(android.content.Context c, String p, String key, String fallback) {
            return c.getSharedPreferences(p, 0).getString(key, fallback);
        }
        @org.robolectric.annotation.Implementation protected static void write(android.content.Context c, String p, String key, String value) {
            c.getSharedPreferences(p, 0).edit().putString(key, value).commit();
        }
    }

    @Test public void notificationEntryPointsOpenForAllFourTabs() {
        android.content.Context context = org.robolectric.RuntimeEnvironment.getApplication();
        AppString.init(context);
        AlarmItem item = new AlarmItem(42L, "Alarm", System.currentTimeMillis() + 60000, 0, true);
        new AlarmStore(context).save(item);
        NoForgetItem note = new NoForgetItem(43L, "Note", "Body", "[]", 2, true,
                System.currentTimeMillis() + 60000, true, System.currentTimeMillis());
        new NoForgetStore(context).save(note);
        for (String kind : new String[]{"", ToolAlarmScheduler.TIMER, ToolAlarmScheduler.STOPWATCH}) {
            android.content.Intent intent = new android.content.Intent(context, AlarmRingActivity.class)
                    .putExtra("alarmId", kind.isEmpty() ? 42L : ToolAlarmScheduler.alarmId(kind))
                    .putExtra("label", "Test");
            if (!kind.isEmpty()) intent.putExtra("toolKind", kind);
            org.robolectric.android.controller.ActivityController<AlarmRingActivity> controller =
                    Robolectric.buildActivity(AlarmRingActivity.class, intent).setup().visible();
            controller.get().onWindowFocusChanged(true);
            controller.pause().stop().destroy();
        }
        android.content.Intent intent = new android.content.Intent(context, NoteReminderActivity.class).putExtra("noteId", 43L);
        org.robolectric.android.controller.ActivityController<NoteReminderActivity> controller =
                Robolectric.buildActivity(NoteReminderActivity.class, intent).setup().visible();
        controller.get().onWindowFocusChanged(true);
        controller.pause().stop().destroy();
    }

}
