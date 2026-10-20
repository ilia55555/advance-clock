package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.EditText;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class TimeToolControlsTest {
    @Test public void startIsRightmostInBothPanelsAndBothLayoutDirections() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        AppSettings.applyTheme(host);
        for (boolean timer : new boolean[]{true, false}) {
            for (int direction : new int[]{View.LAYOUT_DIRECTION_RTL, View.LAYOUT_DIRECTION_LTR}) {
                View root = host.getLayoutInflater().inflate(timer ? R.layout.activity_timer : R.layout.activity_stopwatch, null);
                root.setLayoutDirection(direction);
                root.measure(View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY));
                root.layout(0, 0, 1080, 1920);
                View start = root.findViewById(timer ? R.id.timer_start : R.id.stopwatch_start);
                View reset = root.findViewById(timer ? R.id.timer_reset : R.id.stopwatch_reset);
                assertTrue(start.getLeft() > reset.getRight());
            }
        }
        host.finish();
    }

    @Test public void tappingZeroClearsItButPreservesNonzeroInput() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        EditText h = new EditText(host), m = new EditText(host), s = new EditText(host);
        DurationInputs fields = new DurationInputs(h, m, s);
        for (EditText field : new EditText[]{h, m, s}) {
            for (String zero : new String[]{"0", "۰", "٠", "00"}) {
                field.setText(zero);
                field.performClick();
                assertEquals("", field.getText().toString());
            }
            field.setText("1000000"); field.performClick();
            assertEquals("1000000", field.getText().toString());
        }
        assertEquals(3_661_000_000_000L, fields.readMillis());
        host.finish();
    }
}
