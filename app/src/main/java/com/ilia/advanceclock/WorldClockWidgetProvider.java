package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;
import java.util.*;

public final class WorldClockWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id, manager.getAppWidgetOptions(id));
    }
    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        Bundle merged = new Bundle(manager.getAppWidgetOptions(id));
        if (options != null) merged.putAll(options);
        update(context, manager, id, merged);
    }
    @Override public void onDeleted(Context context, int[] ids) {
        for (int id : ids) {
            WorldClockWidgetPrefs.delete(context, id);
            android.content.SharedPreferences.Editor edit = context.getSharedPreferences("world_clock_widget", Context.MODE_PRIVATE).edit();
            for (String key : new String[]{"capacity_", "page_", "triple_", "columns_", "compact_", "controls_until_", "item_width_dp_", "item_height_dp_"}) edit.remove(key + id);
            edit.apply();
        }
    }
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int id : manager.getAppWidgetIds(new ComponentName(context, WorldClockWidgetProvider.class)))
            update(context, manager, id, manager.getAppWidgetOptions(id));
    }
    private static void update(Context context, AppWidgetManager manager, int id, Bundle options) {
        List<String> zones = WorldClockStore.zones(context);
        if (Build.VERSION.SDK_INT >= 31) {
            Map<SizeF, RemoteViews> alternatives = new LinkedHashMap<>();
            for (SizeF size : WidgetSizeUtils.exactSizes(options)) {
                if (size == null || !Float.isFinite(size.getWidth()) || !Float.isFinite(size.getHeight()) || size.getWidth() <= 0 || size.getHeight() <= 0) continue;
                alternatives.put(size, create(context, id, size.getWidth(), size.getHeight(), zones));
                if (alternatives.size() == 16) break;
            }
            if (!alternatives.isEmpty()) {
                // The host chooses its actual size; no guessed orientation/aspect ratio.
                manager.updateAppWidget(id, new RemoteViews(alternatives));
                if (zones.size() > 64) manager.notifyAppWidgetViewDataChanged(id, R.id.world_widget_grid);
                return;
            }
        }
        int minW = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int minH = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        int maxW = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minW);
        int maxH = options == null ? 0 : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minH);
        if (minW > 0 && minH > 0 && maxW > 0 && maxH > 0) {
            RemoteViews portrait = create(context, id, minW, maxH, zones);
            RemoteViews landscape = create(context, id, maxW, minH, zones);
            manager.updateAppWidget(id, new RemoteViews(landscape, portrait));
        } else {
            WidgetSizeUtils.WidgetSize size = WidgetSizeUtils.currentSize(context, options, 5, 2);
            manager.updateAppWidget(id, create(context, id, size.widthDp, size.heightDp, zones));
        }
        if (Build.VERSION.SDK_INT < 31 || zones.size() > 64) manager.notifyAppWidgetViewDataChanged(id, R.id.world_widget_grid);
    }

    private static RemoteViews create(Context context, int id, float width, float height, List<String> zones) {
        WorldClockWidgetLayout layout = WorldClockWidgetLayout.forSize(width, height, zones.size());
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_world_clock);
        views.setInt(R.id.world_widget_grid, "setNumColumns", layout.columns);
        views.setInt(R.id.world_widget_root, "setBackgroundResource", WorldClockWidgetPrefs.backgroundResource(context, id));
        views.setViewVisibility(R.id.world_widget_controls, View.GONE);
        views.setViewVisibility(R.id.world_widget_scroll_trigger, View.GONE);
        // Large collections stay lazy to avoid the Binder transaction size limit.
        if (Build.VERSION.SDK_INT >= 31 && zones.size() <= 64) {
            RemoteViews.RemoteCollectionItems.Builder items = new RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(6).setHasStableIds(false);
            WorldClockWidgetRenderer renderer = new WorldClockWidgetRenderer(context, id, layout);
            for (int i = 0; i < zones.size(); i++) items.addItem(i, renderer.render(zones.get(i)));
            views.setRemoteAdapter(R.id.world_widget_grid, items.build());
        } else {
            Intent service = new Intent(context, WorldClockWidgetService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("widthDp", width).putExtra("heightDp", height);
            // Intent extras do not participate in RemoteViews adapter identity. Include
            // width, height and text styles in data to invalidate stale host caches.
            service.setData(new Uri.Builder().scheme("advanceclock").authority("world-widget-v2")
                    .appendPath(Integer.toString(id)).appendPath(layout.geometryKey())
                    .appendPath(styleKey(context, id)).build());
            views.setRemoteAdapter(R.id.world_widget_grid, service);
        }
        Intent open = new Intent(context, MainActivity.class).putExtra("openTab", "world");
        int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
        views.setPendingIntentTemplate(R.id.world_widget_grid, PendingIntent.getActivity(context, 30_000 + id, open, flags));
        return views;
    }
    private static String styleKey(Context context, int id) {
        return AppSettings.language(context) + "_" + WorldClockWidgetPrefs.background(context, id)
                + "_" + WorldClockWidgetPrefs.textColor(context, id) + "_" + WorldClockWidgetPrefs.timeColor(context, id)
                + "_" + WorldClockWidgetPrefs.timeWeight(context, id) + "_" + WorldClockWidgetPrefs.timeSize(context, id)
                + "_" + WorldClockWidgetPrefs.nameSize(context, id) + "_" + WorldClockWidgetPrefs.dateSize(context, id)
                + "_" + WorldClockWidgetPrefs.topGap(context, id) + "_" + WorldClockWidgetPrefs.bottomGap(context, id);
    }
}
