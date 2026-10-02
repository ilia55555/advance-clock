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
        WidgetSizeUtils.WidgetSize size =
                WidgetSizeUtils.currentSize(
                        context,
                        options,
                        5,
                        2);
        float widthDp = Math.max(180f, size.widthDp);
        float heightDp = Math.max(80f, size.heightDp);
        boolean compact =
                widthDp < 250f
                        || heightDp < 96f;

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
        views.setTextViewText(R.id.prayer_widget_title,
                AppString.get(R.string.runtime_text_0417));

        float headerScale = clamp(
                Math.min(
                        widthDp / 350f,
                        heightDp / 120f),
                0.93f,
                1.10f);
        views.setTextViewTextSize(
                R.id.prayer_widget_title,
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                compact
                        ? 13.5f * headerScale
                        : 16.5f * headerScale);
        views.setTextColor(R.id.prayer_widget_date, secondary);
        views.setTextColor(R.id.prayer_widget_manage, accent);
        views.setTextViewTextSize(
                R.id.prayer_widget_manage,
                android.util.TypedValue.COMPLEX_UNIT_DIP,
                compact
                        ? 11.5f * headerScale
                        : 13.5f * headerScale);
        views.setInt(R.id.prayer_widget_settings, "setColorFilter", secondary);

        boolean showHeader = PrayerTimesWidgetPrefs.showHeader(context, id);
        views.setViewVisibility(
                R.id.prayer_widget_header, showHeader ? View.VISIBLE : View.GONE);

        // Dates belong to each horizon because their local calendar day can differ.
        views.setViewVisibility(R.id.prayer_widget_date, View.GONE);

        views.setViewVisibility(R.id.prayer_widget_manage, View.VISIBLE);

        Intent service = new Intent(context, PrayerTimesWidgetService.class)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                .putExtra("compact", compact)
                .putExtra("widthDp", widthDp)
                .putExtra("heightDp", heightDp);
        service.setData(Uri.parse("advanceclock://prayer-widget/" + id + "/"
                + (compact ? "compact/" : "full/")
                + Math.round(widthDp) + "x" + Math.round(heightDp) + "/"
                + System.currentTimeMillis()));
        views.setRemoteAdapter(R.id.prayer_widget_list, service);

        Intent rowOpen = new Intent(context, MainActivity.class)
                .putExtra("openTab", "clock")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

        views.setOnClickPendingIntent(
                R.id.prayer_widget_root,
                PendingIntent.getActivity(
                        context,
                        73_000 + id,
                        rowOpen,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));

        manager.updateAppWidget(id, views);
        manager.notifyAppWidgetViewDataChanged(id, R.id.prayer_widget_list);
    }

    private static float clamp(
            float value,
            float min,
            float max) {
        return Math.max(
                min,
                Math.min(max, value));
    }

}
