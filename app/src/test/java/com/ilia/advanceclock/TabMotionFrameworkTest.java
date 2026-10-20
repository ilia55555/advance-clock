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
    @Test public void dragFollowsFingerAndCanReverseWithoutChangingSelection() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        FrameLayout parent = new FrameLayout(host);
        Map<String, View> panels = new LinkedHashMap<>();
        panels.put("clock", new View(host)); panels.put("world", new View(host));
        for (View panel : panels.values()) parent.addView(panel, new FrameLayout.LayoutParams(-1, -1));
        host.setContentView(parent);
        parent.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, 1000, 1600);
        TabMotion motion = new TabMotion(panels);
        final float[] progress = {-1};
        motion.setProgressListener((from, to, fraction) -> progress[0] = fraction);
        motion.show("clock", 1, false);
        assertTrue(motion.beginDrag("world", -20));
        motion.drag(-400);
        assertEquals(.4f, progress[0], .001f);
        assertEquals(-400f, panels.get("clock").getTranslationX(), .001f);
        assertEquals(600f, panels.get("world").getTranslationX(), .001f);
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        assertEquals(-400f, panels.get("clock").getTranslationX(), .001f);
        motion.drag(-120);
        assertEquals(.12f, progress[0], .001f);
        assertEquals(-120f, panels.get("clock").getTranslationX(), .001f);
        motion.finish();
        assertEquals(1f, progress[0], .001f);
        assertEquals(View.VISIBLE, panels.get("clock").getVisibility());
        assertNotEquals(View.VISIBLE, panels.get("world").getVisibility());
        host.finish();
    }

    @Test @org.robolectric.annotation.LooperMode(org.robolectric.annotation.LooperMode.Mode.PAUSED)
    public void underlineProgressMatchesPagesDuringTapAndCancelledSwipe() {
        org.robolectric.util.ReflectionHelpers.callStaticMethod(android.animation.ValueAnimator.class,
                "setDurationScale", org.robolectric.util.ReflectionHelpers.ClassParameter.from(float.class, 1f));
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16));
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        FrameLayout parent = new FrameLayout(host);
        Map<String, View> panels = new LinkedHashMap<>();
        panels.put("clock", new View(host)); panels.put("world", new View(host));
        for (View panel : panels.values()) parent.addView(panel, new FrameLayout.LayoutParams(-1, -1));
        host.setContentView(parent);
        parent.measure(View.MeasureSpec.makeMeasureSpec(1000, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(1600, View.MeasureSpec.EXACTLY));
        parent.layout(0, 0, 1000, 1600);
        TabMotion motion = new TabMotion(panels);
        final float[] progress = {-1};
        final String[] destination = {null};
        motion.setProgressListener((from, to, fraction) -> {
            progress[0] = fraction;
            destination[0] = to;
        });
        motion.show("clock", 1, false);
        motion.show("world", 1, true);
        org.robolectric.shadows.ShadowLooper looper =
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper());
        looper.idleFor(java.time.Duration.ofMillis(140));
        android.animation.ValueAnimator animation =
                org.robolectric.util.ReflectionHelpers.getField(motion, "animator");
        animation.setCurrentPlayTime(140);
        assertTrue("Tap progress: " + progress[0], progress[0] > 0 && progress[0] < 1);
        assertEquals(progress[0], -panels.get("clock").getTranslationX() / 1000, .001f);
        animation.end();
        assertEquals("world", destination[0]);
        assertTrue(motion.beginDrag("clock", 10));
        int dragWidth = parent.getWidth();
        motion.drag(dragWidth * .25f);
        assertEquals(.25f, progress[0], .001f);
        motion.release(true, 0, () -> fail("Cancelled swipe must not commit"));
        animation = org.robolectric.util.ReflectionHelpers.getField(motion, "animator");
        animation.setCurrentPlayTime(80);
        assertTrue(progress[0] > 0 && progress[0] < .25f);
        assertEquals(progress[0], panels.get("world").getTranslationX() / dragWidth, .001f);
        animation.end();
        assertEquals("world", destination[0]);
        assertEquals(View.VISIBLE, panels.get("world").getVisibility());
        host.finish();
    }

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
            assertEquals(entry.getKey().equals("stopwatch"), entry.getValue().getVisibility() == View.VISIBLE);
            assertEquals(0f, entry.getValue().getTranslationX(), .001f);
        }
        host.finish();
    }
}
