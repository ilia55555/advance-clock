package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

public final class CalendarPrayerWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        return new Factory(
                getApplicationContext(),
                widgetId);
    }

    private static final class Factory
            implements RemoteViewsFactory {
        private final Context context;
        private final int widgetId;
        private List<AppSettings.PrayerHorizon> horizons =
                new ArrayList<>();

        Factory(
                Context context,
                int widgetId) {
            this.context = context;
            this.widgetId = widgetId;
        }

        @Override
        public void onCreate() {
            reload();
        }

        @Override
        public void onDataSetChanged() {
            reload();
        }

        @Override
        public void onDestroy() {
            horizons = new ArrayList<>();
        }

        @Override
        public int getCount() {
            return horizons.size();
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (position < 0
                    || position >= horizons.size()) {
                return null;
            }

            AppSettings.PrayerHorizon horizon =
                    horizons.get(position);
            long selectedMillis =
                    CalendarWidgetPrefs.selectedMillis(
                            context,
                            widgetId);

            PrayerTimeCalculator.Times times =
                    PrayerTimeCalculator.calculate(
                            selectedMillis,
                            horizon.latitude,
                            horizon.longitude,
                            TimeZone.getTimeZone(
                                    horizon.timeZoneId));

            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_calendar_prayer_item);

            int primary =
                    AppSettings.primaryColor(context);
            int text =
                    AppSettings.textPrimary(context);
            int muted =
                    AppSettings.textSecondary(context);

            row.setTextViewText(
                    R.id.calendar_prayer_item_city,
                    CalendarWidgetProvider.shortHorizonLabel(
                            context,
                            horizon.label));

            row.setTextViewText(
                    R.id.calendar_prayer_fajr_label,
                    context.getString(
                            R.string.runtime_text_0078));
            row.setTextViewText(
                    R.id.calendar_prayer_sunrise_label,
                    context.getString(
                            R.string.runtime_text_0083));
            row.setTextViewText(
                    R.id.calendar_prayer_dhuhr_label,
                    context.getString(
                            R.string.runtime_text_0079));
            row.setTextViewText(
                    R.id.calendar_prayer_asr_label,
                    context.getString(
                            R.string.adhan_asr_name));

            row.setTextViewText(
                    R.id.calendar_prayer_fajr_time,
                    CalendarUtils.fa(times.fajr()));
            row.setTextViewText(
                    R.id.calendar_prayer_sunrise_time,
                    CalendarUtils.fa(times.sunrise()));
            row.setTextViewText(
                    R.id.calendar_prayer_dhuhr_time,
                    CalendarUtils.fa(times.dhuhr()));
            row.setTextViewText(
                    R.id.calendar_prayer_asr_time,
                    CalendarUtils.fa(times.asr()));

            row.setTextColor(
                    R.id.calendar_prayer_item_city,
                    text);

            int[] labelIds = {
                    R.id.calendar_prayer_fajr_label,
                    R.id.calendar_prayer_sunrise_label,
                    R.id.calendar_prayer_dhuhr_label,
                    R.id.calendar_prayer_asr_label
            };
            for (int id : labelIds) {
                row.setTextColor(id, muted);
            }

            int[] timeIds = {
                    R.id.calendar_prayer_fajr_time,
                    R.id.calendar_prayer_sunrise_time,
                    R.id.calendar_prayer_dhuhr_time,
                    R.id.calendar_prayer_asr_time
            };
            for (int id : timeIds) {
                row.setTextColor(id, primary);
            }

            row.setViewVisibility(
                    R.id.calendar_prayer_item_root,
                    View.VISIBLE);
            return row;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        private void reload() {
            horizons =
                    CalendarWidgetProvider.orderedHorizons(
                            context);
        }
    }
}
