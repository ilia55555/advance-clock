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

public final class ClockWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) update(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
                                                     int appWidgetId, Bundle newOptions) {
        update(context, manager, appWidgetId);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, ClockWidgetProvider.class));
        for (int id : ids) update(context, manager, id);
    }

    private static void update(Context context, AppWidgetManager manager, int widgetId) {
        Bundle options = manager.getAppWidgetOptions(widgetId);
        int height = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 150);
        int rowCount = Math.max(1, Math.min(8, (height - 82) / 46));

        RemoteViews root = new RemoteViews(context.getPackageName(), R.layout.widget_clock);
        root.removeAllViews(R.id.widget_alarm_list);
        List<AlarmItem> upcoming = new AlarmStore(context).upcoming(rowCount);
        root.setViewVisibility(R.id.widget_empty, upcoming.isEmpty() ? View.VISIBLE : View.GONE);

        for (AlarmItem item : upcoming) {
            RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.widget_alarm_row);
            row.setTextViewText(R.id.widget_row_title,
                    item.label == null || item.label.trim().isEmpty() ? "آلارم" : item.label);
            row.setTextViewText(R.id.widget_row_time, TimeUtils.formatDateTime(item.triggerAtMillis));
            Intent edit = new Intent(context, AlarmEditorActivity.class).putExtra("alarmId", item.id);
            PendingIntent editPi = PendingIntent.getActivity(
                    context,
                    50000 + (int) Math.abs(item.id % 1_000_000),
                    edit,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            row.setOnClickPendingIntent(R.id.widget_row_root, editPi);
            root.addView(R.id.widget_alarm_list, row);
        }

        Intent add = new Intent(context, AlarmEditorActivity.class);
        root.setOnClickPendingIntent(R.id.widget_add, PendingIntent.getActivity(
                context,
                60000 + widgetId,
                add,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));
        root.setOnClickPendingIntent(R.id.widget_header, PendingIntent.getActivity(
                context,
                70000 + widgetId,
                new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        ));
        manager.updateAppWidget(widgetId, root);
    }
}
