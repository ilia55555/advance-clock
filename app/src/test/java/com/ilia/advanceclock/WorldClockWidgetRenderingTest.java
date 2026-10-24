package com.ilia.advanceclock;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {28, 31})
public class WorldClockWidgetRenderingTest {
    @Test public void serviceExposesAllCitiesForScrolling() {
        Context context = RuntimeEnvironment.getApplication();
        String zones = String.join("|", java.util.Collections.nCopies(1000, "UTC"));
        context.getSharedPreferences("world_clocks", Context.MODE_PRIVATE).edit().putString("zones", zones).commit();
        WorldClockWidgetService service = org.robolectric.Robolectric.buildService(WorldClockWidgetService.class).create().get();
        android.widget.RemoteViewsService.RemoteViewsFactory factory = service.onGetViewFactory(
                new android.content.Intent().putExtra("widthDp", 400f).putExtra("heightDp", 180f));
        factory.onCreate();
        assertEquals(1000, factory.getCount());
        assertNotNull(factory.getViewAt(999));
        assertNull(factory.getViewAt(1000));
        context.getSharedPreferences("world_clocks", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test public void largestClocksFitTheirCellsAndBoldIncludesCity() {
        Context context = RuntimeEnvironment.getApplication();
        context.getSharedPreferences("world_clock_widget_style", Context.MODE_PRIVATE).edit().clear().commit();
        // Exercise all six RemoteViews layouts, including the legacy height path.
        int[] resources = {R.layout.widget_world_clock_item, R.layout.widget_world_clock_item_normal,
                R.layout.widget_world_clock_item_bold, R.layout.widget_world_clock_item_compact,
                R.layout.widget_world_clock_item_compact_normal, R.layout.widget_world_clock_item_compact_bold};
        for (int resource : resources) {
            View view = new android.widget.RemoteViews(context.getPackageName(), resource)
                    .apply(context, new FrameLayout(context));
            TextView name = view.findViewById(R.id.world_item_name);
            TextView time = view.findViewById(R.id.world_item_time);
            assertEquals(time.getTypeface().getStyle(), name.getTypeface().getStyle());
        }
        for (int cities : new int[]{2, 4, 1000}) {
            for (float height : new float[]{40, 80, 110, 180}) {
                WorldClockWidgetPrefs.save(context, 42, 0, -1, -1, 2, 6, 6, 6, 0, 0);
                WorldClockWidgetLayout layout = WorldClockWidgetLayout.forSize(400, height, cities);
                View view = new WorldClockWidgetRenderer(context, 42, layout).render("Asia/Tehran")
                        .apply(context, new FrameLayout(context));
                float density = context.getResources().getDisplayMetrics().density;
                view.measure(View.MeasureSpec.makeMeasureSpec((int)(layout.itemWidth * density), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec((int)(layout.itemHeight * density), View.MeasureSpec.EXACTLY));
                view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
                TextView date = view.findViewById(R.id.world_item_date);
                assertTrue("Date clipped at " + cities + "/" + height, date.getBottom() <= view.getHeight());
                assertTrue(view.findViewById(R.id.world_item_name).getTop() >= 0);
            }
        }
    }
}
