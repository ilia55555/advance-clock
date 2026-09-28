package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class PrayerTimesWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) update(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(
            Context context, AppWidgetManager manager, int id, Bundle options) {
        update(context, manager, id);
    }

    @Override public void onDeleted(Context context, int[] appWidgetIds) {
        for (int id : appWidgetIds) PrayerTimesWidgetPrefs.clear(context, id);
        super.onDeleted(context, appWidgetIds);
    }

    static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, PrayerTimesWidgetProvider.class));
        for (int id : ids) update(context, manager, id);
    }

    private static void update(Context context, AppWidgetManager manager, int id) {
        Bundle options = manager.getAppWidgetOptions(id);
        int minHeight = options == null ? 110
                : options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110);
        boolean compact = minHeight < 175;

        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                compact
                        ? R.layout.widget_prayer_times_compact
                        : R.layout.widget_prayer_times);

        views.setInt(
                R.id.prayer_widget_root,
                "setBackgroundResource",
                PrayerTimesWidgetPrefs.rootBackgroundResource(context, id));

        int main = PrayerTimesWidgetPrefs.mainTextColor(context, id);
        int secondary = PrayerTimesWidgetPrefs.secondaryTextColor(context, id);
        int accent = PrayerTimesWidgetPrefs.accentColor(context, id);

        views.setTextColor(R.id.prayer_widget_title, main);
        views.setTextColor(R.id.prayer_widget_date, secondary);
        views.setTextColor(R.id.prayer_widget_manage, accent);
        views.setTextColor(R.id.prayer_widget_settings, secondary);
        views.setTextColor(R.id.prayer_widget_scroll_hint, secondary);

        boolean showHeader = PrayerTimesWidgetPrefs.showHeader(context, id);
        views.setViewVisibility(
                R.id.prayer_widget_header, showHeader ? View.VISIBLE : View.GONE);

        boolean showDate = showHeader && PrayerTimesWidgetPrefs.showDate(context, id);
        views.setViewVisibility(
                R.id.prayer_widget_date, showDate ? View.VISIBLE : View.GONE);
        if (showDate) views.setTextViewText(R.id.prayer_widget_date, dateLine());

        boolean showManage = showHeader
                && PrayerTimesWidgetPrefs.showManageButton(context, id);
        views.setViewVisibility(
                R.id.prayer_widget_manage, showManage ? View.VISIBLE : View.GONE);

        int horizonCount = AppSettings.prayerHorizons(context).size();
        boolean showHint = !compact
                && PrayerTimesWidgetPrefs.showScrollHint(context, id)
                && horizonCount > 1;
        views.setViewVisibility(
                R.id.prayer_widget_scroll_hint, showHint ? View.VISIBLE : View.GONE);

        Intent service = new Intent(context, PrayerTimesWidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra("compact", compact);
        service.setData(Uri.parse("advanceclock://prayer-widget/" + id + "/"
                + (compact ? "compact/" : "full/")
                + System.currentTimeMillis()));
        views.setRemoteAdapter(R.id.prayer_widget_list, service);

        Intent rowOpen = new Intent(context, PrayerSettingsActivity.class);
        views.setPendingIntentTemplate(
                R.id.prayer_widget_list,
                PendingIntent.getActivity(
                        context,
                        70_000 + id,
                        rowOpen,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent manage = new Intent(context, PrayerSettingsActivity.class);
        views.setOnClickPendingIntent(
                R.id.prayer_widget_manage,
                PendingIntent.getActivity(
                        context,
                        71_000 + id,
                        manage,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        Intent settings = new Intent(context, PrayerTimesWidgetConfigActivity.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra("editExisting", true);
        views.setOnClickPendingIntent(
                R.id.prayer_widget_settings,
                PendingIntent.getActivity(
                        context,
                        72_000 + id,
                        settings,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        manager.updateAppWidget(id, views);
        manager.notifyAppWidgetViewDataChanged(id, R.id.prayer_widget_list);
    }

    private static String dateLine() {
        long now = System.currentTimeMillis();
        String weekDay;
        try {
            weekDay = new SimpleDateFormat("EEEE", new Locale("fa", "IR"))
                    .format(new Date(now));
        } catch (Exception ignored) {
            weekDay = "";
        }
        String persian = CalendarUtils.formatDate(now, CalendarUtils.PERSIAN);
        String hijri = CalendarUtils.formatDate(now, CalendarUtils.HIJRI);
        return (weekDay.isEmpty() ? "" : weekDay + "  ") + persian + "  •  " + hijri;
    }
}
