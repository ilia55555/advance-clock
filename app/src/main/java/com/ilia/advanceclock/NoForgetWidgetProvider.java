package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

public final class NoForgetWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                                     int appWidgetId, Bundle newOptions) {
        update(context, manager, appWidgetId);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, NoForgetWidgetProvider.class));
        for (int id : ids) update(context, manager, id);
    }

    private static void update(Context context, AppWidgetManager manager, int widgetId) {
        Bundle options = manager.getAppWidgetOptions(widgetId);
        int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160);
        int rowCount = Math.max(0, Math.min(10, (height - 86) / 58));

        RemoteViews root = new RemoteViews(context.getPackageName(), R.layout.widget_noforget);
        boolean dark = WidgetPrefs.isDark(context, widgetId);
        int text = dark ? 0xFFF2F5F4 : 0xFF173F3B;
        int muted = dark ? 0xFFAFBCB8 : 0xFF758783;
        int primary = WidgetPrefs.primary(context, widgetId);
        int secondary = WidgetPrefs.secondary(context, widgetId);

        root.setInt(R.id.noforget_widget_root, "setBackgroundResource",
                dark ? R.drawable.widget_background_dark : R.drawable.widget_background);
        root.setTextColor(R.id.noforget_widget_time, primary);
        root.setTextColor(R.id.noforget_widget_date, muted);
        root.setTextColor(R.id.noforget_widget_section_label, primary);
        root.setTextColor(R.id.noforget_widget_add, secondary);
        root.setInt(R.id.noforget_widget_theme, "setColorFilter", secondary);
        root.setTextColor(R.id.noforget_widget_empty, muted);
        root.setTextViewText(R.id.noforget_widget_date,
                CalendarUtils.formatDate(System.currentTimeMillis(), AppSettings.defaultCalendar(context)));

        root.removeAllViews(R.id.noforget_widget_list);
        List<NoForgetItem> items = widgetOrder(new NoForgetStore(context).all(), rowCount);
        root.setViewVisibility(R.id.noforget_widget_empty,
                rowCount > 0 && items.isEmpty() ? View.VISIBLE : View.GONE);

        for (NoForgetItem item : items) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_noforget_row);
            row.setTextViewText(R.id.noforget_row_title,
                    item.title.trim().isEmpty()
                            ? (item.body.trim().isEmpty() ? "دست‌نویس" : item.body)
                            : item.title);

            String meta = item.hasDue
                    ? CalendarUtils.formatDate(item.dueAtMillis, AppSettings.defaultCalendar(context))
                    : "بدون آلارم";
            if (item.reminderEnabled) meta = "آلارم • " + meta;
            row.setTextViewText(R.id.noforget_row_meta, meta);
            row.setTextViewText(R.id.noforget_row_priority,
                    PriorityUtils.label(item.priority));

            row.setTextColor(R.id.noforget_row_title, text);
            row.setTextColor(R.id.noforget_row_meta, muted);
            row.setTextColor(R.id.noforget_row_priority,
                    item.priority >= PriorityUtils.HIGH ? 0xFFC84D4D : primary);

            Intent edit = new Intent(context, NoForgetEditorActivity.class).putExtra("noteId", item.id);
            row.setOnClickPendingIntent(R.id.noforget_row_root, PendingIntent.getActivity(
                    context,
                    1_500_000 + (int) Math.abs(item.id % 1_000_000),
                    edit,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            root.addView(R.id.noforget_widget_list, row);
        }

        root.setOnClickPendingIntent(R.id.noforget_widget_add, PendingIntent.getActivity(
                context,
                1_600_000 + widgetId,
                new Intent(context, NoForgetQuickAddActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent openApp = new Intent(context, MainActivity.class)
                .putExtra("openTab", "noforget")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        root.setOnClickPendingIntent(R.id.noforget_widget_header, PendingIntent.getActivity(
                context,
                1_700_000 + widgetId,
                openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent themeIntent = new Intent(context, WidgetSettingsActivity.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .putExtra("widgetKind", "note");
        root.setOnClickPendingIntent(R.id.noforget_widget_theme, PendingIntent.getActivity(
                context,
                1_800_000 + widgetId,
                themeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        manager.updateAppWidget(widgetId, root);
    }

    @Override public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) WidgetPrefs.clear(context, id);
        super.onDeleted(context, appWidgetIds);
    }

    private static List<NoForgetItem> widgetOrder(List<NoForgetItem> source, int limit) {
        if (limit <= 0 || source.isEmpty()) return new ArrayList<>();

        long now = System.currentTimeMillis();
        NoForgetItem nearest = null;
        long nearestDistance = Long.MAX_VALUE;

        for (NoForgetItem item : source) {
            if (!item.hasDue || item.dueAtMillis <= 0) continue;
            long distance = Math.abs(item.dueAtMillis - now);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = item;
            }
        }

        ArrayList<NoForgetItem> rest = new ArrayList<>(source);
        if (nearest != null) rest.remove(nearest);

        rest.sort((a, b) -> {
            int p = Integer.compare(b.priority, a.priority);
            if (p != 0) return p;

            if (a.hasDue && b.hasDue) return Long.compare(a.dueAtMillis, b.dueAtMillis);
            if (a.hasDue != b.hasDue) return a.hasDue ? -1 : 1;
            return Long.compare(b.createdAt, a.createdAt);
        });

        ArrayList<NoForgetItem> out = new ArrayList<>();
        if (nearest != null) out.add(nearest);
        for (NoForgetItem item : rest) {
            if (out.size() >= limit) break;
            out.add(item);
        }
        return out;
    }
}
