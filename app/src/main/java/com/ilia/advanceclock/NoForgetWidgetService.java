package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

/** Supplies every note to the scrollable notes widget list. */
public final class NoForgetWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext(),
                intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
    }

    private static final class Factory implements RemoteViewsFactory {
        private final Context context;
        private final int widgetId;
        private List<NoForgetItem> items = new ArrayList<>();

        Factory(Context context, int widgetId) {
            this.context = context;
            this.widgetId = widgetId;
        }

        @Override public void onCreate() {}

        @Override public void onDataSetChanged() {
            List<NoForgetItem> all = new NoForgetStore(context).all();
            items = NoForgetWidgetProvider.widgetOrder(all, Math.max(1, all.size()),
                    WidgetPrefs.sortMode(context, widgetId));
        }

        @Override public void onDestroy() { items = new ArrayList<>(); }
        @Override public int getCount() { return items.size(); }

        @Override public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= items.size()) return null;
            return NoForgetWidgetProvider.buildRow(context, widgetId, items.get(position));
        }

        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int position) {
            return position < items.size() ? items.get(position).id : position;
        }
        @Override public boolean hasStableIds() { return true; }
    }
}
