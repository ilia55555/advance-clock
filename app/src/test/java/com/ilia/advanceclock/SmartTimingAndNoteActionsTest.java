package com.ilia.advanceclock;

import android.app.Application;
import android.view.View;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class SmartTimingAndNoteActionsTest {
    @Test public void exactTimingShowsZeroAndRestoresOffsetWhenLeavingExact() {
        AppString.init(RuntimeEnvironment.getApplication());
        try (var controller=Robolectric.buildActivity(SmartAlarmActivity.class).setup()) {
            SmartAlarmActivity host=controller.get();
            Spinner mode=ReflectionHelpers.getField(host,"bulkMode");
            EditText minutes=ReflectionHelpers.getField(host,"bulkMinutes");
            assertEquals("0",minutes.getText().toString());
            assertFalse(minutes.isEnabled());
            select(mode,SmartAlarmParser.OFFSET_BEFORE);
            assertTrue(minutes.isEnabled());
            minutes.setText("15");
            select(mode,SmartAlarmParser.OFFSET_EXACT);
            assertEquals("0",minutes.getText().toString());
            assertFalse(minutes.isEnabled());
            select(mode,SmartAlarmParser.OFFSET_AFTER);
            assertEquals("15",minutes.getText().toString());
        }
    }
    private void select(Spinner mode,int position) {
        mode.setSelection(position);
        mode.getOnItemSelectedListener().onItemSelected(mode,null,position,position);
    }
    @Test public void noteActionsAreCompactAndLeftAlignedDeleteThenEdit() throws Exception {
        AppString.init(RuntimeEnvironment.getApplication());
        try (var controller=Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity host=controller.get();
            NoForgetItem note=new NoForgetItem(1,"Note","","[]",2,false,0,false,0);
            java.lang.reflect.Method method=MainActivity.class.getDeclaredMethod("noteCard",NoForgetItem.class);
            method.setAccessible(true);
            LinearLayout card=(LinearLayout)method.invoke(host,note);
            LinearLayout actions=(LinearLayout)card.getChildAt(card.getChildCount()-1);
            assertEquals(View.LAYOUT_DIRECTION_LTR,actions.getLayoutDirection());
            assertEquals(2,actions.getChildCount());
            assertTrue(actions.getChildAt(0) instanceof ImageButton);
            assertTrue(actions.getChildAt(1) instanceof Button);
            int expected=Math.round(36*host.getResources().getDisplayMetrics().density);
            assertEquals(expected,actions.getChildAt(0).getLayoutParams().height);
            assertEquals(expected,actions.getChildAt(1).getLayoutParams().height);
        }
    }
}
