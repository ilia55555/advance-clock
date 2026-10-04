package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
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
            applyRowHeight(item);
            applyTextSizes(item, compact);
            item.setString(R.id.world_item_time, "setTimeZone", zone);
            item.setString(R.id.world_item_date, "setTimeZone", zone);
            item.setOnClickFillInIntent(R.id.world_item_root,
                    new Intent().putExtra("openTab", "world"));
            return item;
        }

        private void applyRowHeight(RemoteViews item) {
            float rowHeightDp =
                    WorldClockWidgetProvider.itemHeightDp(
                            context,
                            widgetId);
            int minHeightPx = Math.round(
                    TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            rowHeightDp,
                            context.getResources().getDisplayMetrics()));
            item.setInt(
                    R.id.world_item_root,
                    "setMinimumHeight",
                    minHeightPx);
            if (Build.VERSION.SDK_INT >= 31) {
                item.setViewLayoutHeight(
                        R.id.world_item_root,
                        rowHeightDp,
                        TypedValue.COMPLEX_UNIT_DIP);
            }
        }

        private void applyTextSizes(RemoteViews item, boolean compact) {
            float widthDp = WorldClockWidgetProvider.itemWidthDp(context, widgetId);
            float heightDp = WorldClockWidgetProvider.itemHeightDp(context, widgetId);

            float[] factors = {
                    0.70f, 0.80f, 0.90f, 1.00f, 1.10f, 1.20f, 1.30f
            };
            float timeFactor = factors[
                    WorldClockWidgetPrefs.timeSize(context, widgetId)];
            float nameFactor = factors[
                    WorldClockWidgetPrefs.nameSize(context, widgetId)];
            float dateFactor = factors[
                    WorldClockWidgetPrefs.dateSize(context, widgetId)];

            float widthLimit = Math.max(10f, widthDp / 3.15f);
            float heightLimit = Math.max(
                    18f,
                    compact ? heightDp * 0.55f : heightDp * 0.62f);

            float timeSize = Math.min(widthLimit, heightLimit)
                    * timeFactor / 1.30f;
            float nameSize = Math.min(
                    (compact ? 12f : 19f) * nameFactor,
                    Math.max(9f, heightDp * (compact ? 0.22f : 0.18f)));
            float dateSize = Math.min(
                    (compact ? 10f : 15f) * dateFactor,
                    Math.max(8f, heightDp * (compact ? 0.19f : 0.15f)));

            float[] gapValues = compact
                    ? new float[]{0f, 0.5f, 1f, 2f, 3f}
                    : new float[]{0f, 1f, 2f, 4f, 6f};
            float topGap = gapValues[
                    WorldClockWidgetPrefs.topGap(context, widgetId)];
            float bottomGap = gapValues[
                    WorldClockWidgetPrefs.bottomGap(context, widgetId)];

            // Protect every launcher from clipping/overlap. The independent
            // user choices are preserved proportionally, but the whole block
            // is reduced only when the requested geometry cannot fit.
            float estimatedHeight =
                    nameSize * 1.18f
                            + timeSize * 1.12f
                            + dateSize * 1.18f
                            + topGap
                            + bottomGap;
            float availableHeight = Math.max(36f, heightDp - 4f);
            if (estimatedHeight > availableHeight) {
                float fit = Math.max(
                        0.72f,
                        availableHeight / estimatedHeight);
                timeSize *= fit;
                nameSize *= fit;
                dateSize *= fit;
                topGap *= fit;
                bottomGap *= fit;
            }

            // DIP is intentional: the widget has its own size controls, so a
            // different system font-scale on MIUI/other launchers must not
            // change the visual geometry behind the user's back.
            item.setTextViewTextSize(
                    R.id.world_item_time,
                    TypedValue.COMPLEX_UNIT_DIP,
                    timeSize);
            item.setTextViewTextSize(
                    R.id.world_item_name,
                    TypedValue.COMPLEX_UNIT_DIP,
                    nameSize);
            item.setTextViewTextSize(
                    R.id.world_item_date,
                    TypedValue.COMPLEX_UNIT_DIP,
                    dateSize);

            int topPx = dp(topGap);
            int bottomPx = dp(bottomGap);
            item.setViewPadding(
                    R.id.world_item_time,
                    0,
                    topPx,
                    0,
                    bottomPx);
        }

        private int dp(float value) {
            return Math.round(
                    TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            value,
                            context.getResources().getDisplayMetrics()));
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
