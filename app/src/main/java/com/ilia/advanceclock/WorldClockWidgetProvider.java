package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

public final class WorldClockWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_NEXT = "com.ilia.advanceclock.WORLD_WIDGET_NEXT";
    private static final String ACTION_PREVIOUS = "com.ilia.advanceclock.WORLD_WIDGET_PREVIOUS";
    private static final String PREFS = "world_clock_widget";

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
        if (!ACTION_NEXT.equals(action) && !ACTION_PREVIOUS.equals(action)) return;
        int id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return;
        int page = page(context, id) + (ACTION_NEXT.equals(action) ? 1 : -1);
        int count = WorldClockStore.zones(context).size();
        int pages = Math.max(1, (count + capacity(context, id) - 1) / capacity(context, id));
        if (page < 0) page = pages - 1;
        if (page >= pages) page = 0;
        setPage(context, id, page);
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        manager.notifyAppWidgetViewDataChanged(id, R.id.world_widget_grid);
        update(context, manager, id, manager.getAppWidgetOptions(id));
    }

    @Override public void onDeleted(Context context, int[] ids) {
        for (int id : ids) {
            WorldClockWidgetPrefs.delete(context, id);
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .remove("capacity_" + id).remove("page_" + id)
                    .remove("triple_" + id).remove("columns_" + id).apply();
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

    static boolean tripleLayout(Context context, int id) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getBoolean("triple_" + id, false);
    }

    static int columns(Context context, int id) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt("columns_" + id, 1);
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
        WidgetSizeUtils.WidgetSize size = WidgetSizeUtils.currentSize(context, options, 4, 2);
        int widthCells = WidgetSizeUtils.dpToCells(size.widthDp);
        int zoneCount = WorldClockStore.zones(context).size();
        boolean triple = zoneCount % 2 == 1 && widthCells >= 6;
        int clocksPerRow = triple ? 3 : Math.min(2, Math.max(1, widthCells / 2));
        int rows = Math.max(1, (int) ((size.heightDp - 40f) / 66f));
        int capacity = clocksPerRow * rows;
        int visualColumns = triple || clocksPerRow == 2 ? 3 : 1;
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putInt("capacity_" + id, capacity)
                .putBoolean("triple_" + id, triple)
                .putInt("columns_" + id, visualColumns).apply();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_world_clock);
        views.setInt(R.id.world_widget_grid, "setNumColumns", visualColumns);
        views.setInt(R.id.world_widget_root, "setBackgroundResource",
                WorldClockWidgetPrefs.backgroundResource(context, id));
        boolean showTitle = WorldClockWidgetPrefs.showTitle(context, id);
        views.setViewVisibility(R.id.world_widget_title, showTitle ? View.VISIBLE : View.GONE);
        views.setTextColor(R.id.world_widget_title,
                WorldClockWidgetPrefs.textColor(context, id));
        Intent service = new Intent(context, WorldClockWidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        service.setData(Uri.parse("advanceclock://world-widget/" + id + "/" + capacity));
        views.setRemoteAdapter(R.id.world_widget_grid, service);

        views.setOnClickPendingIntent(R.id.world_widget_previous,
                action(context, id, ACTION_PREVIOUS, 10_000 + id));
        views.setOnClickPendingIntent(R.id.world_widget_next,
                action(context, id, ACTION_NEXT, 20_000 + id));
        Intent open = new Intent(context, MainActivity.class).putExtra("openTab", "world");
        views.setPendingIntentTemplate(R.id.world_widget_grid,
                PendingIntent.getActivity(context, 30_000 + id, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(id, views);
        manager.notifyAppWidgetViewDataChanged(id, R.id.world_widget_grid);
    }

    private static PendingIntent action(Context context, int id, String action, int request) {
        Intent intent = new Intent(context, WorldClockWidgetProvider.class)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        return PendingIntent.getBroadcast(context, request, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
