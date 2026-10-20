package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.FrameLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public final class TabMotionFrameworkTest {
    @Test public void rapidTabChangesLeaveOnlyTheLatestPanelInteractiveAndVisible() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        FrameLayout parent = new FrameLayout(host);
        Map<String, View> panels = new LinkedHashMap<>();
        for (String key : new String[]{"clock", "world", "noforget", "timer", "stopwatch"}) {
            View panel = new View(host); panels.put(key, panel);
            parent.addView(panel, new FrameLayout.LayoutParams(-1, -1));
        }
        host.setContentView(parent);
        parent.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, 1080, 1600);
        TabMotion motion = new TabMotion(panels);
        motion.show("clock", 1, false);
        motion.show("world", -1, true);
        motion.show("timer", -1, true);
        motion.show("stopwatch", -1, true);
        motion.finish();
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        for (Map.Entry<String, View> entry : panels.entrySet()) {
            assertEquals(entry.getKey().equals("stopwatch") ? View.VISIBLE : View.INVISIBLE, entry.getValue().getVisibility());
            assertEquals(0f, entry.getValue().getTranslationX(), .001f);
        }
        host.finish();
    }
}
