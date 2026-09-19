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
        int rowCount = Math.max(1, Math.min(8, (height - 88) / 58));

        RemoteViews root = new RemoteViews(context.getPackageName(), R.layout.widget_noforget);
        boolean dark = AppSettings.themeMode(context) == AppSettings.THEME_DARK;
        int text = dark ? 0xFFF2F5F4 : 0xFF173F3B;
        int muted = dark ? 0xFFAFBCB8 : 0xFF758783;
        int primary = AppSettings.primaryColor(context);

        root.setInt(R.id.noforget_widget_root, "setBackgroundResource",
                dark ? R.drawable.widget_background_dark : R.drawable.widget_background);
        root.setTextColor(R.id.noforget_widget_title, text);
        root.setTextColor(R.id.noforget_widget_date, muted);
        root.setTextColor(R.id.noforget_widget_add, primary);
        root.setTextColor(R.id.noforget_widget_empty, muted);
        root.setTextViewText(R.id.noforget_widget_date,
                CalendarUtils.formatDate(System.currentTimeMillis(), AppSettings.defaultCalendar(context)));

        root.removeAllViews(R.id.noforget_widget_list);
        List<NoForgetItem> items = new NoForgetStore(context).top(rowCount);
        root.setViewVisibility(R.id.noforget_widget_empty, items.isEmpty() ? View.VISIBLE : View.GONE);
        long now = System.currentTimeMillis();

        for (NoForgetItem item : items) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_noforget_row);
            row.setTextViewText(R.id.noforget_row_title,
                    item.title.trim().isEmpty() ? (item.body.trim().isEmpty() ? "دست‌نویس" : item.body) : item.title);

            String meta = item.hasDue
                    ? CalendarUtils.formatDate(item.dueAtMillis, AppSettings.defaultCalendar(context))
                    : "بدون آلارم";
            if (item.reminderEnabled) meta = "آلارم • " + meta;
            row.setTextViewText(R.id.noforget_row_meta, meta);
            row.setTextViewText(R.id.noforget_row_priority,
                    item.priority == NoForgetItem.PRIORITY_HIGH ? "زیاد"
                            : item.priority == NoForgetItem.PRIORITY_LOW ? "کم" : "عادی");
            row.setTextColor(R.id.noforget_row_title, text);
            row.setTextColor(R.id.noforget_row_meta, muted);
            row.setTextColor(R.id.noforget_row_priority,
                    item.priority == NoForgetItem.PRIORITY_HIGH ? 0xFFC84D4D : primary);

            int background;
            int urgency = item.urgency(now);
            if (urgency >= 3) background = R.drawable.noforget_row_red;
            else if (urgency == 2) background = R.drawable.noforget_row_yellow;
            else background = R.drawable.noforget_row_green;
            row.setInt(R.id.noforget_row_root, "setBackgroundResource", background);

            Intent edit = new Intent(context, NoForgetEditorActivity.class).putExtra("noteId", item.id);
            row.setOnClickPendingIntent(R.id.noforget_row_root, PendingIntent.getActivity(
                    context, 1_500_000 + (int) Math.abs(item.id % 1_000_000), edit,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            root.addView(R.id.noforget_widget_list, row);
        }

        root.setOnClickPendingIntent(R.id.noforget_widget_add, PendingIntent.getActivity(
                context, 1_600_000 + widgetId, new Intent(context, NoForgetQuickAddActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent openApp = new Intent(context, MainActivity.class)
                .putExtra("openTab", "noforget").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        root.setOnClickPendingIntent(R.id.noforget_widget_header, PendingIntent.getActivity(
                context, 1_700_000 + widgetId, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        manager.updateAppWidget(widgetId, root);
    }
}
