package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;

public final class PrayerTimesWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(
            Context context, AppWidgetManager manager, int id, Bundle options) {
        update(context, manager, id);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, PrayerTimesWidgetProvider.class));
        for (int id : ids) update(context, manager, id);
    }

    private static void update(Context context, AppWidgetManager manager, int id) {
        RemoteViews views = new RemoteViews(
                context.getPackageName(), R.layout.widget_prayer_times);
        Intent service = new Intent(context, PrayerTimesWidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
        service.setData(Uri.parse("advanceclock://prayer-widget/" + id + "/"
                + System.currentTimeMillis()));
        views.setRemoteAdapter(R.id.prayer_widget_list, service);
        Intent open = new Intent(context, PrayerSettingsActivity.class);
        views.setPendingIntentTemplate(R.id.prayer_widget_list,
                PendingIntent.getActivity(context, 70_000 + id, open,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
        manager.updateAppWidget(id, views);
        manager.notifyAppWidgetViewDataChanged(id, R.id.prayer_widget_list);
    }
}
