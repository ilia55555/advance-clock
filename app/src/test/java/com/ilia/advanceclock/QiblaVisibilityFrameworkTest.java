package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import java.lang.reflect.Field;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {28, 35},
        shadows = QiblaVisibilityFrameworkTest.MarkerCalls.class)
public final class QiblaVisibilityFrameworkTest {
    @Implements(value = QiblaMarker.class, isInAndroidSdk = false)
    public static class MarkerCalls {
        static int directions, pending;
        @Implementation protected void draw(Canvas c, float x, float y, float r, float a) {
            directions++;
        }
        @Implementation protected void drawPending(Canvas c, float x, float y, float r) {
            pending++;
        }
    }

    private void draw(View view) {
        MarkerCalls.directions = MarkerCalls.pending = 0;
        view.layout(0, 0, 600, 600);
        view.draw(new Canvas(Bitmap.createBitmap(600, 600, Bitmap.Config.ARGB_8888)));
    }

    @Test public void onlyQiblaModeDrawsMarkerWhenLocationIsKnown() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        AppString.init(context);
        context.getSharedPreferences("advance_clock_settings", 0).edit()
                .putString("prayer_horizons", "[{\"label\":\"Tehran\",\"lat\":35.6892,\"lon\":51.3890,\"zone\":\"Asia/Tehran\"}]").commit();
        for (int mode = 0; mode <= 1; mode++) {
            Intent intent = new Intent(context, CompassToolActivity.class)
                    .putExtra(CompassToolActivity.EXTRA_START_MODE, mode);
            org.robolectric.android.controller.ActivityController<CompassToolActivity> controller =
                    Robolectric.buildActivity(CompassToolActivity.class, intent).setup();
            Field field = CompassToolActivity.class.getDeclaredField("compassView");
            field.setAccessible(true);
            draw((View) field.get(controller.get()));
            assertEquals(mode == CompassToolActivity.MODE_QIBLA ? 1 : 0, MarkerCalls.directions);
            assertEquals(0, MarkerCalls.pending);
            controller.pause().stop().destroy();
        }
    }


    @Test public void compassWithoutLocationDoesNotDrawPendingQiblaArtwork() throws Exception {
        Context context = RuntimeEnvironment.getApplication(); AppString.init(context);
        context.getSharedPreferences("qibla_location", 0).edit().clear().commit();
        context.getSharedPreferences("advance_clock_settings", 0).edit().clear().commit();
        for (int mode=0; mode<=1; mode++) {
            Intent intent=new Intent(context,CompassToolActivity.class)
                    .putExtra(CompassToolActivity.EXTRA_START_MODE,mode);
            try(var controller=Robolectric.buildActivity(CompassToolActivity.class,intent).setup()) {
                Field field=CompassToolActivity.class.getDeclaredField("compassView");field.setAccessible(true);
                draw((View)field.get(controller.get()));
                assertEquals(0,MarkerCalls.directions);
                assertEquals(mode==CompassToolActivity.MODE_QIBLA ? 1 : 0,MarkerCalls.pending);
            }
        }
    }

    @Test public void adhanShowsArtworkWhileWaitingAndDirectionOnceLocationArrives() {
        Context context = RuntimeEnvironment.getApplication();
        AppString.init(context);
        context.getSharedPreferences("qibla_location", 0).edit().clear().commit();
        context.getSharedPreferences("advance_clock_settings", 0).edit().clear().commit();
        QiblaAdhanBackgroundView view = new QiblaAdhanBackgroundView(context);
        draw(view);
        assertEquals(1, MarkerCalls.pending);
        assertEquals(0, MarkerCalls.directions);
        view.setLocation(35.6892, 51.3890);
        draw(view);
        assertEquals(1, MarkerCalls.directions);
        assertEquals(0, MarkerCalls.pending);
    }
}
