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
@Config(application = Application.class, sdk = {28, 33, 35})
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
}
