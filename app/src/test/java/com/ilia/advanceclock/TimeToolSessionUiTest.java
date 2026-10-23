package com.ilia.advanceclock;
import android.app.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class TimeToolSessionUiTest {
 @Test public void stopwatchHasEndDateOnlyInTimedModesAndLapOnlyAfterStart() {
  AppString.init(RuntimeEnvironment.getApplication());
  try(var controller=Robolectric.buildActivity(StopwatchActivity.class).setup()) {
   StopwatchActivity host=controller.get();
   TextView summary=host.findViewById(R.id.stopwatch_target_summary);
   View lap=host.findViewById(R.id.stopwatch_lap);
   StopwatchPanelController panel=ReflectionHelpers.getField(host,"controller");
   TimeToolPresets presets=ReflectionHelpers.getField(panel,"presets");
   View extras=ReflectionHelpers.getField(presets,"controls");
   assertEquals(View.GONE,summary.getVisibility());assertEquals(View.GONE,lap.getVisibility());
   host.findViewById(R.id.stopwatch_mode_duration).performClick();
   ((EditText)host.findViewById(R.id.stopwatch_limit_hours)).setText("2");
   assertEquals(View.VISIBLE,summary.getVisibility());assertFalse(summary.getText().toString().isEmpty());
   host.findViewById(R.id.stopwatch_start).performClick();
   assertEquals(View.GONE,extras.getVisibility());assertEquals(View.VISIBLE,lap.getVisibility());assertTrue(lap.isEnabled());
   host.findViewById(R.id.stopwatch_reset).performClick();
   assertEquals(View.VISIBLE,extras.getVisibility());assertEquals(View.GONE,lap.getVisibility());
  }
 }
 @Test public void timerExtrasDisappearDuringSessionAndReturnOnReset() {
  AppString.init(RuntimeEnvironment.getApplication());
  try(var controller=Robolectric.buildActivity(TimerActivity.class).setup().visible()) {
   TimerActivity host=controller.get();TimerPanelController panel=ReflectionHelpers.getField(host,"controller");
   TimeToolPresets presets=ReflectionHelpers.getField(panel,"presets");
   View extras=ReflectionHelpers.getField(presets,"controls");
   assertEquals(View.VISIBLE,extras.getVisibility());host.findViewById(R.id.timer_start).performClick();
   assertEquals(View.GONE,extras.getVisibility());host.findViewById(R.id.timer_start).performClick();
   assertEquals(View.GONE,extras.getVisibility());host.findViewById(R.id.timer_reset).performClick();
   assertEquals(View.VISIBLE,extras.getVisibility());
  }
 }
 @Test public void touchingOutsideInputClearsFocusWithoutConsumingTheTap() {
  AppString.init(RuntimeEnvironment.getApplication());
  try(var controller=Robolectric.buildActivity(TimerActivity.class).setup().visible()) {
   TimerActivity host=controller.get();View decor=host.getWindow().getDecorView();
   decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));
   decor.layout(0,0,1080,1920);
   EditText input=host.findViewById(R.id.timer_minutes);input.requestFocus();assertTrue(input.hasFocus());
   int[] location=new int[2];input.getLocationOnScreen(location);
   MotionEvent inside=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,location[0]+5,location[1]+5,0);
   KeyboardDismiss.onTouch(host,inside);assertTrue(input.hasFocus());inside.recycle();
   MotionEvent outside=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,0,0,0);
   KeyboardDismiss.onTouch(host,outside);assertFalse("focus="+host.getCurrentFocus()+" rect="+new android.graphics.Rect(location[0],location[1],location[0]+input.getWidth(),location[1]+input.getHeight()),input.hasFocus());outside.recycle();
   host.findViewById(R.id.timer_start).performClick();
   TimerPanelController panel=ReflectionHelpers.getField(host,"controller");
   assertTrue(ReflectionHelpers.<Boolean>getField(panel,"running"));
  }
 }
}
