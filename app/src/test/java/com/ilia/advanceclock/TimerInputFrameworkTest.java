package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.view.inputmethod.EditorInfo;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
public class TimerInputFrameworkTest {
    private Activity host;
    private View root;
    private TimerPanelController controller;

    private void setup() {
        host = Robolectric.buildActivity(Activity.class).setup().get();
        AppString.init(host);
        AppSettings.applyTheme(host);
        root = host.getLayoutInflater().inflate(R.layout.activity_timer, null);
        host.setContentView(root);
        controller = new TimerPanelController(host, root);
    }
    private EditText field(int id) { return root.findViewById(id); }
    private void duration(String seconds) {
        field(R.id.timer_hours).setText("0");
        field(R.id.timer_minutes).setText("0");
        field(R.id.timer_seconds).setText(seconds);
    }
    private JSONObject capture() throws Exception {
        java.lang.reflect.Method method = TimerPanelController.class.getDeclaredMethod("capturePreset");
        method.setAccessible(true);
        return (JSONObject) method.invoke(controller);
    }
    private void load(JSONObject config) throws Exception {
        java.lang.reflect.Method method = TimerPanelController.class.getDeclaredMethod("loadPreset", JSONObject.class, boolean.class);
        method.setAccessible(true);
        method.invoke(controller, config, false);
    }

    @Test public void millionSecondsStayInTheirFieldThroughDoneSavingAndReloading() throws Exception {
        setup();
        duration("1000000");
        field(R.id.timer_seconds).onEditorAction(EditorInfo.IME_ACTION_DONE);
        assertEquals("1000000", field(R.id.timer_seconds).getText().toString());
        assertEquals("277:46:40", ((TextView) root.findViewById(R.id.timer_time)).getText().toString());
        JSONObject config = capture();
        assertEquals("277:46:40", config.getString("label"));
        assertEquals(1_000_000_000L, config.getLong("duration"));
        duration("2");
        load(config);
        assertEquals("1000000", field(R.id.timer_seconds).getText().toString());
        assertEquals("0", field(R.id.timer_hours).getText().toString());
        controller.onPause();
        controller.onResume();
        assertEquals("1000000", field(R.id.timer_seconds).getText().toString());
        host.finish();
    }

    @Test public void idleClearZerosFieldsAndRunningOrPausedResetRestoresInitialTime() {
        setup();
        duration("120");
        root.findViewById(R.id.timer_reset).performClick();
        for (int id : new int[]{R.id.timer_hours,R.id.timer_minutes,R.id.timer_seconds})
            assertEquals("0", field(id).getText().toString());
        assertEquals("00:00:00", ((TextView) root.findViewById(R.id.timer_time)).getText().toString());
        for (boolean paused : new boolean[]{false, true}) {
            duration("120");
            root.findViewById(R.id.timer_start).performClick();
            if (paused) root.findViewById(R.id.timer_start).performClick();
            root.findViewById(R.id.timer_reset).performClick();
            assertEquals("120", field(R.id.timer_seconds).getText().toString());
            assertEquals("00:02:00", ((TextView) root.findViewById(R.id.timer_time)).getText().toString());
            assertFalse(ReflectionHelpers.<Boolean>getField(controller, "running"));
            assertEquals(0L, (long) ReflectionHelpers.<Long>getField(controller, "remainingMillis"));
        }
        host.finish();
    }

    @Test public void namedAndLegacyPresetsStillLoad() throws Exception {
        setup();
        duration("90");
        field(R.id.timer_label).setText("Tea");
        JSONObject config = capture();
        assertEquals("Tea", config.getString("label"));
        config.remove("inputHours"); config.remove("inputMinutes"); config.remove("inputSeconds");
        load(config);
        assertEquals("1", field(R.id.timer_minutes).getText().toString());
        assertEquals("30", field(R.id.timer_seconds).getText().toString());
        assertEquals("Tea", field(R.id.timer_label).getText().toString());
        host.finish();
    }
}
