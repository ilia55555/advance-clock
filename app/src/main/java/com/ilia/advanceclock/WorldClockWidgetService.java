package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.*;

/** Legacy launchers get immutable size/page snapshots instead of shared mutable geometry. */
public final class WorldClockWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) { return new Factory(this, intent); }
    private static final class Factory implements RemoteViewsFactory {
        private final Context context;
        private final int widgetId, page, capacity;
        private final float width, height;
        private List<String> visible = Collections.emptyList();
        private WorldClockWidgetLayout layout;
        Factory(Context context, Intent intent) {
            this.context = context;
            widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            width = intent.getFloatExtra("widthDp", 320); height = intent.getFloatExtra("heightDp", 110);
            page = intent.getIntExtra("page", 0); capacity = Math.max(1, intent.getIntExtra("capacity", 1));
        }
        @Override public void onCreate() { onDataSetChanged(); }
        @Override public void onDestroy() {}
        @Override public void onDataSetChanged() {
            List<String> zones = WorldClockStore.zones(context);
            int maxPage = Math.max(0, (zones.size() + capacity - 1) / capacity - 1);
            int selected = Math.max(0, Math.min(page, maxPage));
            int start = Math.min(zones.size(), selected * capacity);
            visible = new ArrayList<>(zones.subList(start, Math.min(zones.size(), start + capacity)));
            layout = WorldClockWidgetLayout.forSize(width, height, zones.size()).forPage(selected);
        }
        @Override public int getCount() { return visible.size(); }
        @Override public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= visible.size()) return null;
            return new WorldClockWidgetRenderer(context, widgetId, layout).render(visible.get(position));
        }
        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 6; }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean hasStableIds() { return false; }
    }
}
