package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RemoteViews;

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

    @Override public void onAppWidgetOptionsChanged(
            Context context, AppWidgetManager manager, int id, Bundle options) {
        update(context, manager, id, options);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent == null ? null : intent.getAction();
        if (!ACTION_NEXT.equals(action) && !ACTION_PREVIOUS.equals(action)
                && !ACTION_SHOW_CONTROLS.equals(action)) return;
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;
        if (!ACTION_SHOW_CONTROLS.equals(action)) {
            int page = page(context, id) + (ACTION_NEXT.equals(action) ? 1 : -1);
            int count = WorldClockStore.zones(context).size();
            int pages = Math.max(1, (count + capacity(context, id) - 1) / capacity(context, id));
            if (page < 0) page = pages - 1;
            if (page >= pages) page = 0;
            setPage(context, id, page);
        }
        showControlsTemporarily(context, id);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.notifyAppWidgetViewDataChanged(id, R.id.world_widget_grid);
        update(context, manager, id, manager.getAppWidgetOptions(id));
    }

    @Override public void onDeleted(Context context, int[] ids) {
        for (int id : ids) {
            WorldClockWidgetPrefs.delete(context, id);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .remove("capacity_" + id).remove("page_" + id)
                    .remove("triple_" + id).remove("columns_" + id)
                    .remove("compact_" + id).remove("controls_until_" + id).apply();
        }
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, WorldClockWidgetProvider.class));
        for (int id : ids) {
            manager.notifyAppWidgetViewDataChanged(id, R.id.world_widget_grid);
            update(context, manager, id, manager.getAppWidgetOptions(id));
        }
    }

    static int capacity(Context context, int id) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("capacity_" + id, 1);
    }

    static int normalizedPage(Context context, int id, int count) {
        int pages = Math.max(1, (count + capacity(context, id) - 1) / capacity(context, id));
        int page = page(context, id);
        if (page >= pages) {
            page = pages - 1;
            setPage(context, id, page);
        }
        return page;
    }

    static boolean compact(Context context, int id) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("compact_" + id, false);
    }

    private static int page(Context context, int id) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("page_" + id, 0);
    }

    private static void setPage(Context context, int id, int page) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("page_" + id, page).apply();
    }

    private static void update(
            Context context, AppWidgetManager manager, int id, Bundle options) {
        WidgetSizeUtils.WidgetSize size = WidgetSizeUtils.currentSize(context, options, 5, 2);
        int widthCells = WidgetSizeUtils.dpToCells(size.widthDp);
        int zoneCount = WorldClockStore.zones(context).size();
        boolean singleColumn = widthCells <= 3;
        int visualColumns = singleColumn ? 1 : 2;
        boolean compact = size.heightDp < 106f;
        int capacity = Math.max(1, zoneCount);
        int pages = Math.max(1, (zoneCount + capacity - 1) / capacity);
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("capacity_" + id, capacity)
                .putInt("columns_" + id, visualColumns)
                .putBoolean("compact_" + id, compact).apply();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_world_clock);
        views.setInt(R.id.world_widget_grid, "setNumColumns", visualColumns);
        views.setInt(R.id.world_widget_root, "setBackgroundResource",
                WorldClockWidgetPrefs.backgroundResource(context, id));
        boolean controlsVisible = pages > 1 && controlsVisible(context, id);
        views.setViewVisibility(R.id.world_widget_controls,
                controlsVisible ? View.VISIBLE : View.GONE);
        views.setViewVisibility(R.id.world_widget_scroll_trigger,
                pages > 1 && !controlsVisible ? View.VISIBLE : View.GONE);
        Intent service = new Intent(context, WorldClockWidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        service.setData(Uri.parse("advanceclock://world-widget/" + id + "/" + capacity
                + "/" + (compact ? "compact" : "regular")));
        views.setRemoteAdapter(R.id.world_widget_grid, service);

        views.setOnClickPendingIntent(R.id.world_widget_previous,
                action(context, id, ACTION_PREVIOUS, 10_000 + id));
        views.setOnClickPendingIntent(R.id.world_widget_next,
                action(context, id, ACTION_NEXT, 20_000 + id));
        views.setOnClickPendingIntent(R.id.world_widget_scroll_trigger,
                action(context, id, ACTION_SHOW_CONTROLS, 40_000 + id));
        Intent open = new Intent(context, MainActivity.class).putExtra("openTab", "world");
        views.setPendingIntentTemplate(R.id.world_widget_grid,
                PendingIntent.getActivity(context, 30_000 + id, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(id, views);
        manager.notifyAppWidgetViewDataChanged(id, R.id.world_widget_grid);
    }

    private static boolean controlsVisible(Context context, int id) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong("controls_until_" + id, 0L) > System.currentTimeMillis();
    }

    private static void showControlsTemporarily(Context context, int id) {
        long until = System.currentTimeMillis() + CONTROLS_VISIBLE_MILLIS;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong("controls_until_" + id, until).apply();
        Context appContext = context.getApplicationContext();
        MAIN_HANDLER.postDelayed(() -> {
            long storedUntil = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getLong("controls_until_" + id, 0L);
            if (storedUntil > System.currentTimeMillis()) return;
            AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
            update(appContext, manager, id, manager.getAppWidgetOptions(id));
        }, CONTROLS_VISIBLE_MILLIS + 50L);
    }

    private static PendingIntent action(Context context, int id, String action, int request) {
        Intent intent = new Intent(context, WorldClockWidgetProvider.class)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        return PendingIntent.getBroadcast(context, request, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
