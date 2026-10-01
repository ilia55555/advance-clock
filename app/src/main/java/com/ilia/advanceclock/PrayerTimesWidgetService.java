package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
            AppSettings.PrayerHorizon horizon = horizons.isEmpty()
                    ? null
                    : horizons.get(position);
            boolean primary = horizon != null
                    && AppSettings.isPrimaryPrayerHorizon(
                            context, horizon.latitude, horizon.longitude);
            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    compact
                            ? R.layout.widget_prayer_times_row_compact
                            : R.layout.widget_prayer_times_row_primary);

            int main = PrayerTimesWidgetPrefs.mainTextColor(context, widgetId);
            int secondary = PrayerTimesWidgetPrefs.secondaryTextColor(context, widgetId);
            int accent = PrayerTimesWidgetPrefs.accentColor(context, widgetId);
            int active = PrayerTimesWidgetPrefs.activePrayerColor(context, widgetId);

            if (horizons.isEmpty()) {
                row.setTextViewText(R.id.prayer_widget_empty, AppString.get(R.string.runtime_text_0405));
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
                            context, widgetId, primary
                                    && PrayerTimesWidgetPrefs.showCurrentBadge(
                                            context, widgetId)));

            row.setTextViewText(R.id.prayer_widget_city,
                    shortName(horizon.label));
            row.setTextColor(R.id.prayer_widget_city, primary ? accent : main);
            row.setTextColor(R.id.prayer_widget_local_date, secondary);
            row.setTextColor(R.id.prayer_widget_local_time, accent);

            String localDate = localDate(now, zone);
            row.setViewVisibility(R.id.prayer_widget_local_date,
                    localDate.isEmpty() ? View.GONE : View.VISIBLE);
            row.setTextViewText(R.id.prayer_widget_local_date,
                    localDate);
            row.setTextViewText(R.id.prayer_widget_local_time, localTime(now, zone));

            int[] labelIds = {R.id.prayer_label_fajr, R.id.prayer_label_sunrise,
                    R.id.prayer_label_dhuhr, R.id.prayer_label_asr, R.id.prayer_label_sunset,
                    R.id.prayer_label_maghrib, R.id.prayer_label_isha,
                    R.id.prayer_label_midnight};
            String[] labels = {AppString.get(R.string.runtime_text_0434), AppString.get(R.string.runtime_text_0083), AppString.get(R.string.runtime_text_0435), AppString.get(R.string.runtime_text_0082), AppString.get(R.string.runtime_text_0084), AppString.get(R.string.runtime_text_0436), AppString.get(R.string.runtime_text_0081), AppString.get(R.string.runtime_text_0085)};
            for (int index = 0; index < labelIds.length; index++) {
                row.setTextViewText(labelIds[index], labels[index]);
            }

            row.setViewVisibility(R.id.prayer_widget_current_chip, View.GONE);

            boolean countdown = primary
                    && PrayerTimesWidgetPrefs.timeMode(context, widgetId)
                    == PrayerTimesWidgetPrefs.TIME_MODE_COUNTDOWN
                    && next != null;
            row.setViewVisibility(R.id.prayer_widget_next_box, View.GONE);
            row.setViewVisibility(R.id.prayer_widget_local_time,
                    countdown ? View.GONE : View.VISIBLE);
            row.setViewVisibility(R.id.prayer_widget_countdown,
                    countdown ? View.VISIBLE : View.GONE);
            if (countdown) {
                row.setTextColor(R.id.prayer_widget_countdown, active);
                long remaining = Math.max(0L, next.targetMillis - now);
                long base = SystemClock.elapsedRealtime() + remaining;
                row.setChronometer(R.id.prayer_widget_countdown, base, null, true);
                row.setChronometerCountDown(R.id.prayer_widget_countdown, true);
            }

            row.setTextViewText(R.id.prayer_value_fajr, times.fajr());
            row.setTextViewText(R.id.prayer_value_sunrise, times.sunrise());
            row.setTextViewText(R.id.prayer_value_dhuhr, times.dhuhr());
            row.setTextViewText(R.id.prayer_value_asr, times.asr());
            row.setTextViewText(R.id.prayer_value_sunset, times.sunset());
            row.setTextViewText(R.id.prayer_value_maghrib, times.maghrib());
            row.setTextViewText(R.id.prayer_value_isha, times.isha());
            row.setTextViewText(R.id.prayer_value_midnight, times.midnight());

            applySelectedTimes(row);

            applyTextColors(row, main, secondary);
            resetHighlights(row);
            if (next != null) highlightNext(row, next.kind, active);

            int[] fallbackIconViews = {
                    R.id.prayer_icon_fajr,
                    R.id.prayer_icon_sunrise,
                    R.id.prayer_icon_dhuhr,
                    R.id.prayer_icon_asr,
                    R.id.prayer_icon_sunset,
                    R.id.prayer_icon_maghrib,
                    R.id.prayer_icon_isha,
                    R.id.prayer_icon_midnight
            };
            if (compact) {
                for (int viewId : fallbackIconViews) {
                    row.setViewVisibility(viewId, View.GONE);
                }
            } else {
                int[] prayerIconImageViews = {
                        R.id.prayer_icon_image_fajr,
                        R.id.prayer_icon_image_sunrise,
                        R.id.prayer_icon_image_dhuhr,
                        R.id.prayer_icon_image_asr,
                        R.id.prayer_icon_image_sunset,
                        R.id.prayer_icon_image_maghrib,
                        R.id.prayer_icon_image_isha,
                        R.id.prayer_icon_image_midnight
                };
                String[] prayerIconDrawableNames = {
                        "prayer_morning", "prayer_sunrise", "prayer_noon",
                        "prayer_afternoon", "prayer_sunset", "prayer_maghrib",
                        "prayer_isha", "prayer_midnight"
                };
                boolean showPrayerIcons = PrayerTimesWidgetPrefs.showIcons(
                        context, widgetId);
                for (int index = 0; index < prayerIconDrawableNames.length; index++) {
                    int drawable = context.getResources().getIdentifier(
                            prayerIconDrawableNames[index],
                            "drawable",
                            context.getPackageName());
                    boolean hasImage = showPrayerIcons && drawable != 0;
                    row.setViewVisibility(
                            prayerIconImageViews[index],
                            hasImage ? View.VISIBLE : View.GONE);
                    row.setViewVisibility(
                            fallbackIconViews[index],
                            showPrayerIcons && !hasImage ? View.VISIBLE : View.GONE);
                    if (hasImage) {
                        row.setImageViewResource(prayerIconImageViews[index], drawable);
                    }
                }
            }

            applyFontSize(row, PrayerTimesWidgetPrefs.fontSize(context, widgetId));

            row.setOnClickFillInIntent(
                    R.id.prayer_widget_row_root, new Intent());
            return row;
        }

        private String localDate(long millis, TimeZone zone) {
            int[] types = {
                    CalendarUtils.PERSIAN,
                    CalendarUtils.HIJRI,
                    CalendarUtils.GREGORIAN
            };
            StringBuilder result = new StringBuilder();
            for (int type : types) {
                if (!PrayerTimesWidgetPrefs.showCalendar(context, widgetId, type)) {
                    continue;
                }
                if (result.length() > 0) result.append(" • ");
                result.append(CalendarUtils.formatNumeric(millis, type, zone));
            }
            return result.toString();
        }

        private String localTime(long millis, TimeZone zone) {
            SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
            format.setTimeZone(zone);
            return CalendarUtils.fa(format.format(new Date(millis)));
        }

        private void applySelectedTimes(RemoteViews row) {
            String[] names = {
                    "fajr", "sunrise", "dhuhr", "asr",
                    "sunset", "maghrib", "isha", "midnight"
            };
            int[] cells = {
                    R.id.prayer_time_fajr,
                    R.id.prayer_time_sunrise,
                    R.id.prayer_time_dhuhr,
                    R.id.prayer_time_asr,
                    R.id.prayer_time_sunset,
                    R.id.prayer_time_maghrib,
                    R.id.prayer_time_isha,
                    R.id.prayer_time_midnight
            };
            for (int index = 0; index < names.length; index++) {
                row.setViewVisibility(
                        cells[index],
                        PrayerTimesWidgetPrefs.showPrayerTime(
                                context, widgetId, names[index])
                                ? View.VISIBLE : View.GONE);
            }
        }

        private void applyTextColors(RemoteViews row, int main, int secondary) {
            int[] labels = {
                    R.id.prayer_label_fajr,
                    R.id.prayer_label_sunrise,
                    R.id.prayer_label_dhuhr,
                    R.id.prayer_label_asr,
                    R.id.prayer_label_sunset,
                    R.id.prayer_label_maghrib,
                    R.id.prayer_label_isha,
                    R.id.prayer_label_midnight,
                    R.id.prayer_icon_fajr,
                    R.id.prayer_icon_sunrise,
                    R.id.prayer_icon_dhuhr,
                    R.id.prayer_icon_asr,
                    R.id.prayer_icon_sunset,
                    R.id.prayer_icon_maghrib,
                    R.id.prayer_icon_isha,
                    R.id.prayer_icon_midnight
            };
            int[] values = {
                    R.id.prayer_value_fajr,
                    R.id.prayer_value_sunrise,
                    R.id.prayer_value_dhuhr,
                    R.id.prayer_value_asr,
                    R.id.prayer_value_sunset,
                    R.id.prayer_value_maghrib,
                    R.id.prayer_value_isha,
                    R.id.prayer_value_midnight
            };
            for (int id : labels) row.setTextColor(id, secondary);
            for (int id : values) row.setTextColor(id, main);
        }

        private void resetHighlights(RemoteViews row) {
            int[] cells = {
                    R.id.prayer_time_fajr,
                    R.id.prayer_time_sunrise,
                    R.id.prayer_time_dhuhr,
                    R.id.prayer_time_asr,
                    R.id.prayer_time_sunset,
                    R.id.prayer_time_maghrib,
                    R.id.prayer_time_isha,
                    R.id.prayer_time_midnight
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
                    cell = R.id.prayer_time_asr;
                    label = R.id.prayer_label_asr;
                    icon = R.id.prayer_icon_asr;
                    value = R.id.prayer_value_asr;
                    break;
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
            float city;
            float date;
            float clock;
            float label;
            float value;
            float icon;
            if (compact) {
                city = mode == 0 ? 11f : (mode == 2 ? 13f : 12f);
                date = mode == 0 ? 8f : (mode == 2 ? 10f : 9f);
                clock = mode == 0 ? 10f : (mode == 2 ? 13f : 12f);
                label = mode == 0 ? 7f : (mode == 2 ? 9f : 8f);
                value = mode == 0 ? 9f : (mode == 2 ? 11f : 10f);
                icon = mode == 0 ? 9f : (mode == 2 ? 11f : 10f);
            } else {
                city = mode == 0 ? 18f : (mode == 2 ? 22f : 20f);
                date = mode == 0 ? 13f : (mode == 2 ? 17f : 15f);
                clock = mode == 0 ? 24f : (mode == 2 ? 32f : 28f);
                label = mode == 0 ? 10f : (mode == 2 ? 14f : 12f);
                value = mode == 0 ? 13f : (mode == 2 ? 17f : 15f);
                icon = mode == 0 ? 12f : (mode == 2 ? 16f : 14f);
            }
            row.setTextViewTextSize(
                    R.id.prayer_widget_city, TypedValue.COMPLEX_UNIT_SP, city);
            row.setTextViewTextSize(
                    R.id.prayer_widget_local_date, TypedValue.COMPLEX_UNIT_SP, date);
            row.setTextViewTextSize(
                    R.id.prayer_widget_local_time, TypedValue.COMPLEX_UNIT_SP, clock);
            row.setTextViewTextSize(
                    R.id.prayer_widget_countdown, TypedValue.COMPLEX_UNIT_SP, clock);

            int[] labels = {
                    R.id.prayer_label_fajr,
                    R.id.prayer_label_sunrise,
                    R.id.prayer_label_dhuhr,
                    R.id.prayer_label_asr,
                    R.id.prayer_label_sunset,
                    R.id.prayer_label_maghrib,
                    R.id.prayer_label_isha,
                    R.id.prayer_label_midnight
            };
            int[] values = {
                    R.id.prayer_value_fajr,
                    R.id.prayer_value_sunrise,
                    R.id.prayer_value_dhuhr,
                    R.id.prayer_value_asr,
                    R.id.prayer_value_sunset,
                    R.id.prayer_value_maghrib,
                    R.id.prayer_value_isha,
                    R.id.prayer_value_midnight
            };
            int[] icons = {
                    R.id.prayer_icon_fajr,
                    R.id.prayer_icon_sunrise,
                    R.id.prayer_icon_dhuhr,
                    R.id.prayer_icon_asr,
                    R.id.prayer_icon_sunset,
                    R.id.prayer_icon_maghrib,
                    R.id.prayer_icon_isha,
                    R.id.prayer_icon_midnight
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
            String[] labels = {AppString.get(R.string.runtime_text_0434), AppString.get(R.string.runtime_text_0435), AppString.get(R.string.runtime_text_0082), AppString.get(R.string.runtime_text_0436), AppString.get(R.string.runtime_text_0081)};

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
                    AppString.get(R.string.runtime_text_0434),
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
            if (label == null || label.trim().isEmpty()) return AppString.get(R.string.runtime_text_0418);
            String value = label.trim();
            int comma = value.indexOf('،');
            if (comma < 0) comma = value.indexOf(',');
            if (comma > 0) value = value.substring(0, comma);
            return value;
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
