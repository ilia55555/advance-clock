package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.graphics.Rect;
import android.view.View;
import android.view.WindowInsets;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class AlertWindowInsetsTest {
    private WindowInsets insets(int top,int bottom) {
        if (android.os.Build.VERSION.SDK_INT >= 30) return new WindowInsets.Builder()
                .setInsets(WindowInsets.Type.systemBars(),android.graphics.Insets.of(0,top,0,bottom)).build();
        return org.robolectric.util.ReflectionHelpers.callConstructor(WindowInsets.class,
                org.robolectric.util.ReflectionHelpers.ClassParameter.from(Rect.class,new Rect(0,top,0,bottom)));
    }
    @Test public void navigationSpaceIsReservedWithoutAccumulatingOnRepeatedDispatch() {
        Activity host=Robolectric.buildActivity(Activity.class).setup().get();
        View root=new View(host);
        root.setPadding(4,6,8,10);
        host.setContentView(root);
        AlertWindowInsets.apply(host.getWindow(),root);
        WindowInsets insets=insets(24,48);
        root.dispatchApplyWindowInsets(insets);
        assertEquals(30,root.getPaddingTop());
        assertEquals(58,root.getPaddingBottom());
        root.dispatchApplyWindowInsets(insets);
        assertEquals(58,root.getPaddingBottom());
        assertEquals(4,root.getPaddingLeft());
        root.dispatchApplyWindowInsets(insets(0,0));
        assertEquals(10,root.getPaddingBottom());
        host.finish();
    }
}
