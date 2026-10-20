package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class AlertSoundHintTest {
    @Test public void acknowledgmentIsSavedSeparatelyForEachScreenAndRestoresAccessibility() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        View screen = new View(host);
        host.setContentView(screen);
        AlertSoundHint.show(host, "alarm");
        ViewGroup content = host.findViewById(android.R.id.content);
        ViewGroup hint = content.findViewWithTag("alert_sound_hint");
        assertNotNull(hint);
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, screen.getImportantForAccessibility());
        AlertSoundHint.show(host, "alarm");
        assertEquals(2, content.getChildCount());
        Button confirm = (Button) ((ViewGroup) hint.getChildAt(0)).getChildAt(1);
        confirm.performClick();
        assertNull(content.findViewWithTag("alert_sound_hint"));
        assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_AUTO, screen.getImportantForAccessibility());
        AlertSoundHint.show(host, "alarm");
        assertNull(content.findViewWithTag("alert_sound_hint"));
        AlertSoundHint.show(host, "adhan");
        assertNotNull(content.findViewWithTag("alert_sound_hint"));
        host.finish();
    }

    @Test public void hintReturnsUntilAcknowledged() {
        Activity first = Robolectric.buildActivity(Activity.class).setup().get();
        first.setContentView(new View(first));
        AlertSoundHint.show(first, "alarm");
        first.finish();
        Activity next = Robolectric.buildActivity(Activity.class).setup().get();
        next.setContentView(new View(next));
        AlertSoundHint.show(next, "alarm");
        assertNotNull(next.findViewById(android.R.id.content).findViewWithTag("alert_sound_hint"));
        next.finish();
    }
}
