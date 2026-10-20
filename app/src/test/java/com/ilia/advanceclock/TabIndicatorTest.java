package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = 35)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class TabIndicatorTest {
    @Test public void underlineUsesExactPageFractionInBothDirectionsWithoutPixelRounding() {
        Activity host = Robolectric.buildActivity(Activity.class).setup().get();
        FrameLayout bar = new FrameLayout(host);
        Map<String, View> markers = new LinkedHashMap<>();
        RecordingDrawable background = new RecordingDrawable();
        for (String key : new String[]{"clock", "world"}) {
            FrameLayout tab = new FrameLayout(host);
            View marker = new View(host);
            marker.setBackground(key.equals("clock") ? background : new RecordingDrawable());
            tab.addView(marker); bar.addView(tab); markers.put(key, marker);
        }
        bar.layout(0, 0, 400, 64);
        ((View) markers.get("clock").getParent()).layout(300, 0, 400, 64);
        ((View) markers.get("world").getParent()).layout(0, 0, 100, 64);
        for (View marker : markers.values()) marker.layout(0, 60, 100, 64);
        TabIndicator indicator = new TabIndicator(bar, markers);
        Drawable underline = ReflectionHelpers.getField(indicator, "underline");
        for (float fraction : new float[]{0, .001f, .17f, .5f, .93f, 1}) {
            indicator.update("clock", "world", fraction);
            underline.draw(new Canvas(android.graphics.Bitmap.createBitmap(400, 64, android.graphics.Bitmap.Config.ARGB_8888)));
            assertEquals(300 * (1 - fraction), background.x, .0001f);
            indicator.update("world", "clock", fraction);
            underline.draw(new Canvas(android.graphics.Bitmap.createBitmap(400, 64, android.graphics.Bitmap.Config.ARGB_8888)));
            assertEquals(300 * fraction, background.x, .0001f);
        }
        for (View marker : markers.values()) assertEquals(View.INVISIBLE, marker.getVisibility());
        host.finish();
    }

    private static class RecordingDrawable extends Drawable {
        float x;
        @Override public void draw(Canvas canvas) {
            float[] values = new float[9];
            canvas.getMatrix().getValues(values);
            x = values[Matrix.MTRANS_X];
        }
        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter filter) {}
        @Override public int getOpacity() { return PixelFormat.OPAQUE; }
    }
}
