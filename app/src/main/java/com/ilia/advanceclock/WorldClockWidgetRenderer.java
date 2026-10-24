package com.ilia.advanceclock;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.TypedValue;
import android.view.View;
import android.view.Gravity;
import android.widget.RemoteViews;

/** One item renderer for responsive collections and pre-Android-12 remote services. */
final class WorldClockWidgetRenderer {
    private final Context context;
    private final int widgetId;
    private final WorldClockWidgetLayout layout;
    WorldClockWidgetRenderer(Context context, int widgetId, WorldClockWidgetLayout layout) {
        this.context = context; this.widgetId = widgetId; this.layout = layout;
    }
    RemoteViews render(String zone) {
        int weight = WorldClockWidgetPrefs.timeWeight(context, widgetId);
        int resource = layout.compact
                ? weight == 0 ? R.layout.widget_world_clock_item_compact : weight == 1 ? R.layout.widget_world_clock_item_compact_normal : R.layout.widget_world_clock_item_compact_bold
                : weight == 0 ? R.layout.widget_world_clock_item : weight == 1 ? R.layout.widget_world_clock_item_normal : R.layout.widget_world_clock_item_bold;
        RemoteViews item = new RemoteViews(context.getPackageName(), resource);
        item.setViewVisibility(R.id.world_item_root, View.VISIBLE);
        String city = "Canada/Saskatchewan".equals(zone) ? "Regina" : zone.substring(zone.lastIndexOf('/') + 1).replace('_', ' ');
        item.setTextViewText(R.id.world_item_name, WorldClockStore.label(context, zone, city));
        item.setTextColor(R.id.world_item_name, WorldClockWidgetPrefs.textColor(context, widgetId));
        item.setTextColor(R.id.world_item_date, WorldClockWidgetPrefs.textColor(context, widgetId));
        item.setTextColor(R.id.world_item_time, WorldClockWidgetPrefs.timeColor(context, widgetId));
        item.setInt(R.id.world_item_root, "setMinimumHeight", dp(layout.itemHeight));
        // A lone row in a tall widget starts at the top like the first scroll row.
        item.setInt(R.id.world_item_root, "setGravity", layout.rows == 1 && layout.height >= 100
                ? Gravity.TOP | Gravity.CENTER_HORIZONTAL : Gravity.CENTER);
        if (Build.VERSION.SDK_INT >= 31) item.setViewLayoutHeight(R.id.world_item_root, layout.itemHeight, TypedValue.COMPLEX_UNIT_DIP);
        WorldClockWidgetLayout.TextSizes text = layout.textSizes(
                WorldClockWidgetPrefs.timeSize(context, widgetId), WorldClockWidgetPrefs.nameSize(context, widgetId),
                WorldClockWidgetPrefs.dateSize(context, widgetId), WorldClockWidgetPrefs.topGap(context, widgetId), WorldClockWidgetPrefs.bottomGap(context, widgetId));
        item.setTextViewTextSize(R.id.world_item_name, TypedValue.COMPLEX_UNIT_DIP, text.name);
        item.setTextViewTextSize(R.id.world_item_time, TypedValue.COMPLEX_UNIT_DIP, text.time);
        item.setTextViewTextSize(R.id.world_item_date, TypedValue.COMPLEX_UNIT_DIP, text.date);
        item.setViewPadding(R.id.world_item_time, 0, dp(text.topGap), 0, dp(text.bottomGap));
        item.setString(R.id.world_item_time, "setTimeZone", zone);
        item.setString(R.id.world_item_date, "setTimeZone", zone);
        item.setOnClickFillInIntent(R.id.world_item_root, new Intent().putExtra("openTab", "world"));
        return item;
    }
    private int dp(float value) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, context.getResources().getDisplayMetrics())); }
}
