package com.ilia.advanceclock;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public final class PrayerTimesWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(this);
    }

    private static final class Factory implements RemoteViewsFactory {
        private final Context context;
        private List<AppSettings.PrayerHorizon> horizons = Collections.emptyList();

        Factory(Context context) { this.context = context; }
        @Override public void onCreate() {}
        @Override public void onDestroy() {}
        @Override public void onDataSetChanged() {
            horizons = AppSettings.prayerHorizons(context);
        }
        @Override public int getCount() { return Math.max(1, horizons.size()); }
        @Override public RemoteViews getViewAt(int position) {
            RemoteViews row = new RemoteViews(
                    context.getPackageName(), R.layout.widget_prayer_times_row);
            if (horizons.isEmpty()) {
                row.setTextViewText(R.id.prayer_widget_row,
                        "افقی انتخاب نشده • برای افزودن لمس کنید");
            } else {
                AppSettings.PrayerHorizon horizon = horizons.get(position);
                PrayerTimeCalculator.Times times = PrayerTimeCalculator.calculate(
                        System.currentTimeMillis(), horizon.latitude, horizon.longitude,
                        TimeZone.getTimeZone(horizon.timeZoneId));
                row.setTextViewText(R.id.prayer_widget_row,
                        shortName(horizon.label) + ":  صبح " + times.fajr()
                                + "   طلوع " + times.sunrise()
                                + "   ظهر " + times.dhuhr()
                                + "   مغرب " + times.maghrib()
                                + "   عشاء " + times.isha());
            }
            row.setOnClickFillInIntent(R.id.prayer_widget_row, new Intent());
            return row;
        }
        @Override public RemoteViews getLoadingView() { return null; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public long getItemId(int position) { return position; }
        @Override public boolean hasStableIds() { return true; }

        private String shortName(String label) {
            if (label == null || label.trim().isEmpty()) return "افق";
            int comma = label.indexOf('،');
            return comma > 0 ? label.substring(0, comma) : label;
        }
    }
}
