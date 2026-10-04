package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.util.TypedValue;
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
            visible = new java.util.ArrayList<>(zones.subList(start, end));
        }
        @Override public int getCount() { return visible.size(); }
        @Override public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= visible.size()) return null;
            String zone = visible.get(position);
            boolean compact =
                    WorldClockWidgetProvider.compact(
                            context,
                            widgetId);
            int weight =
                    WorldClockWidgetPrefs.timeWeight(
                            context,
                            widgetId);

            int layout;
            if (compact) {
                if (weight == 0) {
                    layout =
                            R.layout.widget_world_clock_item_compact;
                } else if (weight == 1) {
                    layout =
                            R.layout.widget_world_clock_item_compact_normal;
                } else {
                    layout =
                            R.layout.widget_world_clock_item_compact_bold;
                }
            } else {
                if (weight == 0) {
                    layout =
                            R.layout.widget_world_clock_item;
                } else if (weight == 1) {
                    layout =
                            R.layout.widget_world_clock_item_normal;
                } else {
                    layout =
                            R.layout.widget_world_clock_item_bold;
                }
            }
            RemoteViews item = new RemoteViews(context.getPackageName(), layout);
            item.setViewVisibility(R.id.world_item_root, android.view.View.VISIBLE);
            item.setTextViewText(
                    R.id.world_item_name,
                    WorldClockStore.label(context, zone, cityName(zone)));
            int textColor = WorldClockWidgetPrefs.textColor(context, widgetId);
            item.setTextColor(R.id.world_item_name, textColor);
            item.setTextColor(R.id.world_item_date, textColor);
            item.setTextColor(R.id.world_item_time,
                    WorldClockWidgetPrefs.timeColor(context, widgetId));
            applyTextSizes(item, compact);
            item.setString(R.id.world_item_time, "setTimeZone", zone);
            item.setString(R.id.world_item_date, "setTimeZone", zone);
            item.setOnClickFillInIntent(R.id.world_item_root,
                    new Intent().putExtra("openTab", "world"));
            return item;
        }

        private void applyTextSizes(RemoteViews item, boolean compact) {
            float widthDp = WorldClockWidgetProvider.itemWidthDp(context, widgetId);
            float heightDp = WorldClockWidgetProvider.itemHeightDp(context, widgetId);
            int preset = WorldClockWidgetPrefs.timeSize(context, widgetId);
            float[] factors = {0.70f, 0.80f, 0.90f, 1.00f, 1.10f, 1.20f, 1.30f};
            float factor = factors[preset];

            float widthLimitSp = Math.max(10f, widthDp / 3.15f);
            float heightLimitSp = Math.max(18f,
                    compact ? heightDp * 0.55f : heightDp * 0.62f);
            float timeSp = Math.min(widthLimitSp, heightLimitSp) * factor / 1.30f;
            float nameSp = Math.min((compact ? 12f : 19f) * factor,
                    Math.max(9f, heightDp * 0.18f));
            float dateSp = Math.min((compact ? 10f : 15f) * factor,
                    Math.max(8f, heightDp * 0.15f));

            item.setTextViewTextSize(R.id.world_item_time, TypedValue.COMPLEX_UNIT_SP, timeSp);
            item.setTextViewTextSize(R.id.world_item_name, TypedValue.COMPLEX_UNIT_SP, nameSp);
            item.setTextViewTextSize(R.id.world_item_date, TypedValue.COMPLEX_UNIT_SP, dateSp);
        }
        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 6; }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean hasStableIds() { return true; }

        private String cityName(String zone) {
            if ("Canada/Saskatchewan".equals(zone)) return "Regina";
            int slash = zone.lastIndexOf('/');
            String city = slash >= 0 ? zone.substring(slash + 1) : zone;
            return city.replace('_', ' ');
        }
    }
}
