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
    private static final String ACTION_NEXT = "com.ilia.advanceclock.WORLD_WIDGET_NEXT";
    private static final String ACTION_PREVIOUS = "com.ilia.advanceclock.WORLD_WIDGET_PREVIOUS";
    private static final String ACTION_SHOW_CONTROLS = "com.ilia.advanceclock.WORLD_WIDGET_SHOW_CONTROLS";
    private static final String PREFS = "world_clock_widget";
    private static final long CONTROLS_VISIBLE_MILLIS = 3_000L;
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id, manager.getAppWidgetOptions(id));
    }
    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager, int id, Bundle options) {
        setPage(context, id, 0);
        update(context, manager, id, options);
    }
    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent == null ? null : intent.getAction();
        if (!ACTION_NEXT.equals(action) && !ACTION_PREVIOUS.equals(action) && !ACTION_SHOW_CONTROLS.equals(action)) return;
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;
        int capacity = Math.max(1, intent.getIntExtra("capacity", 1));
        int pages = Math.max(1, (WorldClockStore.zones(context).size() + capacity - 1) / capacity);
        if (!ACTION_SHOW_CONTROLS.equals(action)) {
            int selected = Math.max(0, Math.min(page(context, id), pages - 1));
            setPage(context, id, Math.floorMod(selected + (ACTION_NEXT.equals(action) ? 1 : -1), pages));
        }
        showControlsTemporarily(context, id);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        update(context, manager, id, manager.getAppWidgetOptions(id));
    }
    @Override public void onDeleted(Context context, int[] ids) {
        for (int id : ids) {
            WorldClockWidgetPrefs.delete(context, id);
            android.content.SharedPreferences.Editor edit = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit();
            for (String key : new String[]{"capacity_", "page_", "triple_", "columns_", "compact_", "controls_until_", "item_width_dp_", "item_height_dp_"}) edit.remove(key + id);
            edit.apply();
        }
    }
    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        for (int id : manager.getAppWidgetIds(new ComponentName(context, WorldClockWidgetProvider.class)))
            update(context, manager, id, manager.getAppWidgetOptions(id));
    }
    private static int page(Context context, int id) { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt("page_" + id, 0); }
    private static void setPage(Context context, int id, int page) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt("page_" + id, page).apply(); }

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
        if (Build.VERSION.SDK_INT < 31) manager.notifyAppWidgetViewDataChanged(id, R.id.world_widget_grid);
    }

    private static RemoteViews create(Context context, int id, float width, float height, List<String> zones) {
        WorldClockWidgetLayout base = WorldClockWidgetLayout.forSize(width, height, zones.size());
        int selected = base.normalizePage(page(context, id));
        WorldClockWidgetLayout layout = base.forPage(selected);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_world_clock);
        views.setInt(R.id.world_widget_grid, "setNumColumns", layout.columns);
        views.setInt(R.id.world_widget_root, "setBackgroundResource", WorldClockWidgetPrefs.backgroundResource(context, id));
        boolean controls = base.pages() > 1 && controlsVisible(context, id);
        views.setViewVisibility(R.id.world_widget_controls, controls ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.world_widget_scroll_trigger, base.pages() > 1 && !controls ? View.VISIBLE : View.GONE);
        if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews.RemoteCollectionItems.Builder items = new RemoteViews.RemoteCollectionItems.Builder().setViewTypeCount(6).setHasStableIds(false);
            WorldClockWidgetRenderer renderer = new WorldClockWidgetRenderer(context, id, layout);
            int start = Math.min(zones.size(), selected * base.capacity);
            for (int i = start; i < Math.min(zones.size(), start + base.capacity); i++) items.addItem(i, renderer.render(zones.get(i)));
            views.setRemoteAdapter(R.id.world_widget_grid, items.build());
        } else {
            Intent service = new Intent(context, WorldClockWidgetService.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("widthDp", width).putExtra("heightDp", height)
                    .putExtra("capacity", base.capacity).putExtra("page", selected);
            // Intent extras do not participate in RemoteViews adapter identity. Include
            // width, height, page AND text styles in data to invalidate stale host caches.
            service.setData(new Uri.Builder().scheme("advanceclock").authority("world-widget-v2")
                    .appendPath(Integer.toString(id)).appendPath(base.geometryKey()).appendPath(Integer.toString(selected))
                    .appendPath(styleKey(context, id)).build());
            views.setRemoteAdapter(R.id.world_widget_grid, service);
        }
        views.setOnClickPendingIntent(R.id.world_widget_previous, action(context, id, ACTION_PREVIOUS, base));
        views.setOnClickPendingIntent(R.id.world_widget_next, action(context, id, ACTION_NEXT, base));
        views.setOnClickPendingIntent(R.id.world_widget_scroll_trigger, action(context, id, ACTION_SHOW_CONTROLS, base));
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
    private static boolean controlsVisible(Context context, int id) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("controls_until_" + id, 0) > System.currentTimeMillis();
    }
    private static void showControlsTemporarily(Context context, int id) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("controls_until_" + id, System.currentTimeMillis() + CONTROLS_VISIBLE_MILLIS).apply();
        Context app = context.getApplicationContext();
        MAIN_HANDLER.postDelayed(() -> {
            if (controlsVisible(app, id)) return;
            AppWidgetManager manager = AppWidgetManager.getInstance(app);
            update(app, manager, id, manager.getAppWidgetOptions(id));
        }, CONTROLS_VISIBLE_MILLIS + 50);
    }
    private static PendingIntent action(Context context, int id, String action, WorldClockWidgetLayout layout) {
        Intent intent = new Intent(context, WorldClockWidgetProvider.class).setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id).putExtra("capacity", layout.capacity)
                .setData(new Uri.Builder().scheme("advanceclock").authority("world-widget-action").appendPath(Integer.toString(id))
                        .appendPath(action).appendPath(layout.geometryKey()).build());
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
