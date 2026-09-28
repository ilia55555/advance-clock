package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public final class PrayerTimesWidgetService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        int widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        boolean compact = intent.getBooleanExtra("compact", false);
        return new Factory(this, widgetId, compact);
    }

    private static final class Factory implements RemoteViewsFactory {
        private static final int NEXT_FAJR = 0;
        private static final int NEXT_DHUHR = 1;
        private static final int NEXT_ASR = 2;
        private static final int NEXT_MAGHRIB = 3;
        private static final int NEXT_ISHA = 4;

        private final Context context;
        private final int widgetId;
        private final boolean compact;
        private List<AppSettings.PrayerHorizon> horizons = Collections.emptyList();

        Factory(Context context, int widgetId, boolean compact) {
            this.context = context;
            this.widgetId = widgetId;
            this.compact = compact;
        }

        @Override public void onCreate() {}
        @Override public void onDestroy() {}

        @Override public void onDataSetChanged() {
            ArrayList<AppSettings.PrayerHorizon> ordered =
                    new ArrayList<>(AppSettings.prayerHorizons(context));
            for (int i = 0; i < ordered.size(); i++) {
                AppSettings.PrayerHorizon item = ordered.get(i);
                if (AppSettings.isPrimaryPrayerHorizon(
                        context, item.latitude, item.longitude)) {
                    if (i > 0) {
                        ordered.remove(i);
                        ordered.add(0, item);
                    }
                    break;
                }
            }
            horizons = ordered;
        }

        @Override public int getCount() {
            return Math.max(1, horizons.size());
        }

        @Override public RemoteViews getViewAt(int position) {
            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    compact
                            ? R.layout.widget_prayer_times_row_compact
                            : R.layout.widget_prayer_times_row);

            int main = PrayerTimesWidgetPrefs.mainTextColor(context, widgetId);
            int secondary = PrayerTimesWidgetPrefs.secondaryTextColor(context, widgetId);
            int accent = PrayerTimesWidgetPrefs.accentColor(context, widgetId);
            int active = PrayerTimesWidgetPrefs.activePrayerColor(context, widgetId);

            if (horizons.isEmpty()) {
                row.setViewVisibility(R.id.prayer_widget_empty, View.VISIBLE);
                row.setViewVisibility(R.id.prayer_widget_content, View.GONE);
                row.setTextColor(R.id.prayer_widget_empty, main);
                row.setInt(
                        R.id.prayer_widget_row_root,
                        "setBackgroundResource",
                        PrayerTimesWidgetPrefs.cardBackgroundResource(
                                context, widgetId, false));
                row.setOnClickFillInIntent(
                        R.id.prayer_widget_row_root, new Intent());
                return row;
            }

            AppSettings.PrayerHorizon horizon = horizons.get(position);
            boolean primary = AppSettings.isPrimaryPrayerHorizon(
                    context, horizon.latitude, horizon.longitude);
            TimeZone zone = TimeZone.getTimeZone(horizon.timeZoneId);
            long now = System.currentTimeMillis();
            PrayerTimeCalculator.Times times = PrayerTimeCalculator.calculate(
                    now, horizon.latitude, horizon.longitude, zone);
            NextPrayer next = nextPrayer(now, horizon, times, zone);

            row.setViewVisibility(R.id.prayer_widget_empty, View.GONE);
            row.setViewVisibility(R.id.prayer_widget_content, View.VISIBLE);
            row.setInt(
                    R.id.prayer_widget_row_root,
                    "setBackgroundResource",
                    PrayerTimesWidgetPrefs.cardBackgroundResource(
                            context, widgetId, primary));

            row.setTextViewText(R.id.prayer_widget_city, shortName(horizon.label));
            row.setTextColor(R.id.prayer_widget_city, primary ? accent : main);
            row.setTextColor(R.id.prayer_widget_pin, accent);

            boolean badge = primary
                    && PrayerTimesWidgetPrefs.showCurrentBadge(context, widgetId);
            row.setViewVisibility(
                    R.id.prayer_widget_current_chip,
                    badge ? View.VISIBLE : View.GONE);
            row.setTextColor(R.id.prayer_widget_current_chip, accent);

            boolean countdown = !compact
                    && primary
                    && PrayerTimesWidgetPrefs.showCountdown(context, widgetId)
                    && next != null;
            row.setViewVisibility(
                    R.id.prayer_widget_next_box,
                    countdown ? View.VISIBLE : View.GONE);
            if (countdown) {
                row.setInt(
                        R.id.prayer_widget_next_box,
                        "setBackgroundResource",
                        PrayerTimesWidgetPrefs.nextBoxBackgroundResource(
                                context, widgetId));
                row.setTextViewText(
                        R.id.prayer_widget_next_label,
                        "اذان بعدی: " + next.label);
                row.setTextColor(R.id.prayer_widget_next_label, secondary);
                row.setTextColor(R.id.prayer_widget_countdown, active);
                long remaining = Math.max(0L, next.targetMillis - now);
                long base = SystemClock.elapsedRealtime() + remaining;
                row.setChronometer(R.id.prayer_widget_countdown, base, null, true);
                row.setChronometerCountDown(R.id.prayer_widget_countdown, true);
            }

            row.setTextViewText(R.id.prayer_value_fajr, times.fajr());
            row.setTextViewText(R.id.prayer_value_sunrise, times.sunrise());
            row.setTextViewText(R.id.prayer_value_dhuhr, times.dhuhr());
            row.setTextViewText(R.id.prayer_value_maghrib, times.maghrib());
            row.setTextViewText(R.id.prayer_value_isha, times.isha());

            applyTextColors(row, main, secondary);
            resetHighlights(row);
            if (next != null) highlightNext(row, next.kind, active);

            boolean icons = !compact
                    && PrayerTimesWidgetPrefs.showIcons(context, widgetId);
            int iconVisibility = icons ? View.VISIBLE : View.GONE;
            row.setViewVisibility(R.id.prayer_icon_fajr, iconVisibility);
            row.setViewVisibility(R.id.prayer_icon_sunrise, iconVisibility);
            row.setViewVisibility(R.id.prayer_icon_dhuhr, iconVisibility);
            row.setViewVisibility(R.id.prayer_icon_maghrib, iconVisibility);
            row.setViewVisibility(R.id.prayer_icon_isha, iconVisibility);

            applyFontSize(row, PrayerTimesWidgetPrefs.fontSize(context, widgetId));

            row.setOnClickFillInIntent(
                    R.id.prayer_widget_row_root, new Intent());
            return row;
        }

        private void applyTextColors(RemoteViews row, int main, int secondary) {
            int[] labels = {
                    R.id.prayer_label_fajr,
                    R.id.prayer_label_sunrise,
                    R.id.prayer_label_dhuhr,
                    R.id.prayer_label_maghrib,
                    R.id.prayer_label_isha,
                    R.id.prayer_icon_fajr,
                    R.id.prayer_icon_sunrise,
                    R.id.prayer_icon_dhuhr,
                    R.id.prayer_icon_maghrib,
                    R.id.prayer_icon_isha
            };
            int[] values = {
                    R.id.prayer_value_fajr,
                    R.id.prayer_value_sunrise,
                    R.id.prayer_value_dhuhr,
                    R.id.prayer_value_maghrib,
                    R.id.prayer_value_isha
            };
            for (int id : labels) row.setTextColor(id, secondary);
            for (int id : values) row.setTextColor(id, main);
        }

        private void resetHighlights(RemoteViews row) {
            int[] cells = {
                    R.id.prayer_time_fajr,
                    R.id.prayer_time_sunrise,
                    R.id.prayer_time_dhuhr,
                    R.id.prayer_time_maghrib,
                    R.id.prayer_time_isha
            };
            for (int id : cells) {
                row.setInt(id, "setBackgroundResource", android.R.color.transparent);
            }
        }

        private void highlightNext(RemoteViews row, int kind, int color) {
            int cell;
            int label;
            int icon;
            int value;
            switch (kind) {
                case NEXT_FAJR:
                    cell = R.id.prayer_time_fajr;
                    label = R.id.prayer_label_fajr;
                    icon = R.id.prayer_icon_fajr;
                    value = R.id.prayer_value_fajr;
                    break;
                case NEXT_DHUHR:
                    cell = R.id.prayer_time_dhuhr;
                    label = R.id.prayer_label_dhuhr;
                    icon = R.id.prayer_icon_dhuhr;
                    value = R.id.prayer_value_dhuhr;
                    break;
                case NEXT_MAGHRIB:
                    cell = R.id.prayer_time_maghrib;
                    label = R.id.prayer_label_maghrib;
                    icon = R.id.prayer_icon_maghrib;
                    value = R.id.prayer_value_maghrib;
                    break;
                case NEXT_ISHA:
                    cell = R.id.prayer_time_isha;
                    label = R.id.prayer_label_isha;
                    icon = R.id.prayer_icon_isha;
                    value = R.id.prayer_value_isha;
                    break;
                case NEXT_ASR:
                default:
                    return;
            }
            row.setInt(
                    cell,
                    "setBackgroundResource",
                    R.drawable.widget_prayer_active_time);
            row.setTextColor(label, color);
            row.setTextColor(icon, color);
            row.setTextColor(value, color);
        }

        private void applyFontSize(RemoteViews row, int mode) {
            float city = mode == 0 ? 13f : (mode == 2 ? 17f : 15f);
            float label = mode == 0 ? 7f : (mode == 2 ? 9f : 8f);
            float value = mode == 0 ? 9f : (mode == 2 ? 12f : 10f);
            float icon = mode == 0 ? 10f : (mode == 2 ? 14f : 12f);
            row.setTextViewTextSize(
                    R.id.prayer_widget_city, TypedValue.COMPLEX_UNIT_SP, city);

            int[] labels = {
                    R.id.prayer_label_fajr,
                    R.id.prayer_label_sunrise,
                    R.id.prayer_label_dhuhr,
                    R.id.prayer_label_maghrib,
                    R.id.prayer_label_isha
            };
            int[] values = {
                    R.id.prayer_value_fajr,
                    R.id.prayer_value_sunrise,
                    R.id.prayer_value_dhuhr,
                    R.id.prayer_value_maghrib,
                    R.id.prayer_value_isha
            };
            int[] icons = {
                    R.id.prayer_icon_fajr,
                    R.id.prayer_icon_sunrise,
                    R.id.prayer_icon_dhuhr,
                    R.id.prayer_icon_maghrib,
                    R.id.prayer_icon_isha
            };
            for (int id : labels) {
                row.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, label);
            }
            for (int id : values) {
                row.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, value);
            }
            for (int id : icons) {
                row.setTextViewTextSize(id, TypedValue.COMPLEX_UNIT_SP, icon);
            }
        }

        private NextPrayer nextPrayer(
                long now,
                AppSettings.PrayerHorizon horizon,
                PrayerTimeCalculator.Times today,
                TimeZone zone) {
            Calendar current = Calendar.getInstance(zone);
            current.setTimeInMillis(now);
            int nowMinutes = current.get(Calendar.HOUR_OF_DAY) * 60
                    + current.get(Calendar.MINUTE);

            int[] values = {
                    today.fajrMinutes,
                    today.dhuhrMinutes,
                    today.asrMinutes,
                    today.maghribMinutes,
                    today.ishaMinutes
            };
            String[] labels = {"فجر", "ظهر", "عصر", "مغرب", "عشاء"};

            for (int i = 0; i < values.length; i++) {
                if (values[i] >= 0 && values[i] >= nowMinutes) {
                    return new NextPrayer(
                            i, labels[i], targetMillis(current, values[i], false));
                }
            }

            Calendar tomorrow = (Calendar) current.clone();
            tomorrow.add(Calendar.DAY_OF_MONTH, 1);
            PrayerTimeCalculator.Times nextDay = PrayerTimeCalculator.calculate(
                    tomorrow.getTimeInMillis(),
                    horizon.latitude,
                    horizon.longitude,
                    zone);
            if (nextDay.fajrMinutes < 0) return null;
            return new NextPrayer(
                    NEXT_FAJR,
                    "فجر",
                    targetMillis(current, nextDay.fajrMinutes, true));
        }

        private long targetMillis(Calendar current, int minutes, boolean tomorrow) {
            Calendar target = (Calendar) current.clone();
            if (tomorrow) target.add(Calendar.DAY_OF_MONTH, 1);
            target.set(Calendar.HOUR_OF_DAY, minutes / 60);
            target.set(Calendar.MINUTE, minutes % 60);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
            return target.getTimeInMillis();
        }

        @Override public RemoteViews getLoadingView() {
            return null;
        }

        @Override public int getViewTypeCount() {
            return 1;
        }

        @Override public long getItemId(int position) {
            if (horizons.isEmpty() || position >= horizons.size()) return 0L;
            AppSettings.PrayerHorizon h = horizons.get(position);
            long a = Double.doubleToLongBits(h.latitude);
            long b = Double.doubleToLongBits(h.longitude);
            return a ^ Long.rotateLeft(b, 17);
        }

        @Override public boolean hasStableIds() {
            return true;
        }

        private String shortName(String label) {
            if (label == null || label.trim().isEmpty()) return "افق";
            String value = label.trim();
            int comma = value.indexOf('،');
            if (comma < 0) comma = value.indexOf(',');
            if (comma > 0) value = value.substring(0, comma);
            return value.startsWith("افق ") ? value : "افق " + value;
        }

        private static final class NextPrayer {
            final int kind;
            final String label;
            final long targetMillis;

            NextPrayer(int kind, String label, long targetMillis) {
                this.kind = kind;
                this.label = label;
                this.targetMillis = targetMillis;
            }
        }
    }
}
