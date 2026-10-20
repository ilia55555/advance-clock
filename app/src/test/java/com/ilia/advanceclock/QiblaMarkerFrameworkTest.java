package com.ilia.advanceclock;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {28, 35})
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public final class QiblaMarkerFrameworkTest {
    @Test public void goldenLineFollowsBearingInEveryDirection() {
        QiblaMarker marker = new QiblaMarker(RuntimeEnvironment.getApplication());
        for (int angle : new int[]{0, 90, 180, 270}) {
            Bitmap output = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
            marker.draw(new Canvas(output), 200, 200, 100, angle);
            double radians = Math.toRadians(angle - 90);
            int x = 200 + (int) Math.round(Math.cos(radians) * 30);
            int y = 200 + (int) Math.round(Math.sin(radians) * 30);
            assertEquals(QiblaMarker.GOLD, output.getPixel(x, y));
            assertEquals(0, output.getPixel(400 - x, 400 - y));
            output.recycle();
        }
    }
}
