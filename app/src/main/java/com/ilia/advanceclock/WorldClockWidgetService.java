package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.Collections;
import java.util.List;

public final class WorldClockWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(this, intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID));
    }

    private static final class Factory implements RemoteViewsFactory {
        private final Context context;
        private final int widgetId;
        private List<String> visible = Collections.emptyList();
        private boolean triple;

        Factory(Context context, int widgetId) {
            this.context = context;
            this.widgetId = widgetId;
        }

        @Override public void onCreate() {}
        @Override public void onDestroy() {}
        @Override public void onDataSetChanged() {
            List<String> zones = WorldClockStore.zones(context);
            int capacity = WorldClockWidgetProvider.capacity(context, widgetId);
            int page = WorldClockWidgetProvider.normalizedPage(context, widgetId, zones.size());
            int start = Math.min(zones.size(), page * capacity);
            int end = Math.min(zones.size(), start + capacity);
            List<String> pageZones = new java.util.ArrayList<>(zones.subList(start, end));
            triple = WorldClockWidgetProvider.tripleLayout(context, widgetId);
            int columns = WorldClockWidgetProvider.columns(context, widgetId);
            if (triple || columns == 1) {
                visible = pageZones;
            } else {
                java.util.ArrayList<String> slots = new java.util.ArrayList<>();
                for (int i = 0; i < pageZones.size(); i += 2) {
                    if (i + 1 < pageZones.size()) {
                        slots.add(pageZones.get(i));
                        slots.add("");
                        slots.add(pageZones.get(i + 1));
                    } else {
                        slots.add("");
                        slots.add(pageZones.get(i));
                        slots.add("");
                    }
                }
                visible = slots;
            }
        }
        @Override public int getCount() { return visible.size(); }
        @Override public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= visible.size()) return null;
            String zone = visible.get(position);
            RemoteViews item = new RemoteViews(context.getPackageName(), R.layout.widget_world_clock_item);
            if (zone.isEmpty()) {
                item.setViewVisibility(R.id.world_item_root, android.view.View.INVISIBLE);
                return item;
            }
            item.setViewVisibility(R.id.world_item_root, android.view.View.VISIBLE);
            item.setTextViewText(R.id.world_item_name, zone.replace('_', ' '));
            int textColor = WorldClockWidgetPrefs.textColor(context, widgetId);
            item.setTextColor(R.id.world_item_name, textColor);
            item.setTextColor(R.id.world_item_date, textColor);
            item.setTextColor(R.id.world_item_time,
                    WorldClockWidgetPrefs.timeColor(context, widgetId));
            item.setString(R.id.world_item_time, "setTimeZone", zone);
            item.setString(R.id.world_item_date, "setTimeZone", zone);
            item.setOnClickFillInIntent(R.id.world_item_root,
                    new Intent().putExtra("openTab", "world"));
            return item;
        }
        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean hasStableIds() { return true; }
    }
}
