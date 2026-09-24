package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;

import java.util.List;

public final class WorldClockWidgetProvider extends AppWidgetProvider {
    private static final int[] NAMES = {R.id.world_name_1, R.id.world_name_2, R.id.world_name_3};
    private static final int[] TIMES = {R.id.world_time_1, R.id.world_time_2, R.id.world_time_3};

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WorldClockWidgetProvider.class));
        for (int id : ids) update(context, manager, id);
    }

    private static void update(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_world_clock);
        List<String> zones = WorldClockStore.zones(context);
        for (int i = 0; i < 3; i++) {
            boolean visible = i < zones.size();
            views.setViewVisibility(NAMES[i], visible ? View.VISIBLE : View.INVISIBLE);
            views.setViewVisibility(TIMES[i], visible ? View.VISIBLE : View.INVISIBLE);
            if (visible) {
                String zone = zones.get(i);
                views.setTextViewText(NAMES[i], zone.replace('_', ' '));
                views.setString(TIMES[i], "setTimeZone", zone);
            }
        }
        Intent open = new Intent(context, MainActivity.class).putExtra("openTab", "world");
        PendingIntent pending = PendingIntent.getActivity(context, 2_420_000 + id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.world_widget_root, pending);
        manager.updateAppWidget(id, views);
    }
}
