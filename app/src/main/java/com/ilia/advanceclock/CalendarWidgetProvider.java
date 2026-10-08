package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CalendarWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_PREVIOUS_MONTH =
            "com.ilia.advanceclock.CALENDAR_WIDGET_PREVIOUS_MONTH";
    private static final String ACTION_NEXT_MONTH =
            "com.ilia.advanceclock.CALENDAR_WIDGET_NEXT_MONTH";
    private static final String ACTION_TODAY =
            "com.ilia.advanceclock.CALENDAR_WIDGET_TODAY";
    private static final String ACTION_SELECT_DAY =
            "com.ilia.advanceclock.CALENDAR_WIDGET_SELECT_DAY";
    private static final String EXTRA_SELECTED_MILLIS =
            "calendar_widget_selected_millis";

    private static final int[] WEEK_ROW_IDS = {
            R.id.calendar_week_0,
            R.id.calendar_week_1,
            R.id.calendar_week_2,
            R.id.calendar_week_3,
            R.id.calendar_week_4,
            R.id.calendar_week_5
    };

    private static final int[] WEEKDAY_VIEW_IDS = {
            R.id.calendar_weekday_0,
            R.id.calendar_weekday_1,
            R.id.calendar_weekday_2,
            R.id.calendar_weekday_3,
            R.id.calendar_weekday_4,
            R.id.calendar_weekday_5,
            R.id.calendar_weekday_6
    };

    private static final int[] WEEKDAY_IDS_IRAN_HIJRI = {
            R.string.runtime_text_0180,
            R.string.runtime_text_0181,
            R.string.runtime_text_0182,
            R.string.runtime_text_0552,
            R.string.runtime_text_0184,
            R.string.runtime_text_0185,
            R.string.runtime_text_0186
    };

    private static final int[] WEEKDAY_IDS_GREGORIAN = {
            R.string.runtime_text_0182,
            R.string.runtime_text_0552,
            R.string.runtime_text_0184,
            R.string.runtime_text_0185,
            R.string.runtime_text_0186,
            R.string.runtime_text_0180,
            R.string.runtime_text_0181
    };

    @Override
    public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            update(
                    context,
                    manager,
                    id,
                    manager.getAppWidgetOptions(id));
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager manager,
            int appWidgetId,
            Bundle newOptions) {
        update(context, manager, appWidgetId, newOptions);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_PREVIOUS_MONTH.equals(action)
                || ACTION_NEXT_MONTH.equals(action)
                || ACTION_TODAY.equals(action)
                || ACTION_SELECT_DAY.equals(action)) {
            int widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                return;
            }

            if (ACTION_PREVIOUS_MONTH.equals(action)) {
                moveMonth(context, widgetId, -1);
            } else if (ACTION_NEXT_MONTH.equals(action)) {
                moveMonth(context, widgetId, 1);
            } else if (ACTION_TODAY.equals(action)) {
                long now = System.currentTimeMillis();
                CalendarWidgetPrefs.setSelectedMillis(context, widgetId, now);
                CalendarWidgetPrefs.setVisibleMonthMillis(context, widgetId, now);
            } else {
                long selected = intent.getLongExtra(
                        EXTRA_SELECTED_MILLIS,
                        System.currentTimeMillis());
                CalendarWidgetPrefs.setSelectedMillis(
                        context,
                        widgetId,
                        selected);
                CalendarWidgetPrefs.setVisibleMonthMillis(
                        context,
                        widgetId,
                        selected);
            }

            AppWidgetManager manager =
                    AppWidgetManager.getInstance(context);
            update(
                    context,
                    manager,
                    widgetId,
                    manager.getAppWidgetOptions(widgetId));
            return;
        }

        super.onReceive(context, intent);
    }

    @Override
    public void onRestored(
            Context context,
            int[] oldWidgetIds,
            int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
        int count = Math.min(oldWidgetIds.length, newWidgetIds.length);
        for (int i = 0; i < count; i++) {
            CalendarWidgetPrefs.migrate(
                    context,
                    oldWidgetIds[i],
                    newWidgetIds[i]);
        }
        updateAll(context);
    }

    @Override
    public void onDeleted(
            Context context,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            CalendarWidgetPrefs.clear(context, id);
        }
        super.onDeleted(context, appWidgetIds);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager =
                AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(
                        context,
                        CalendarWidgetProvider.class));
        for (int id : ids) {
            update(
                    context,
                    manager,
                    id,
                    manager.getAppWidgetOptions(id));
        }
    }

    private static void update(
            Context context,
            AppWidgetManager manager,
            int widgetId,
            Bundle options) {
        WidgetSizeUtils.updateResponsive(
                context,
                manager,
                widgetId,
                options,
                (widthDp, heightDp) -> createRemoteViews(
                        context,
                        widgetId,
                        widthDp,
                        heightDp));

        manager.notifyAppWidgetViewDataChanged(
                widgetId,
                R.id.calendar_prayer_list);
    }

    private static RemoteViews createRemoteViews(
            Context context,
            int widgetId,
            float widthDp,
            float heightDp) {
        RemoteViews root = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_calendar);

        boolean dark =
                AppSettings.themeMode(context)
                        == AppSettings.THEME_DARK;
        int primary = AppSettings.primaryColor(context);
        int text = AppSettings.textPrimary(context);
        int muted = AppSettings.textSecondary(context);
        int holiday = dark
                ? 0xFFFF7B7B
                : 0xFFC62828;

        root.setInt(
                R.id.calendar_widget_root,
                "setBackgroundResource",
                dark
                        ? R.drawable.time_tools_widget_background_dark
                        : R.drawable.time_tools_widget_background_light);

        root.setTextViewText(
                R.id.calendar_widget_today,
                AppString.get(R.string.ui_today));

        int type = AppSettings.defaultCalendar(context);
        long selectedMillis =
                CalendarWidgetPrefs.selectedMillis(
                        context,
                        widgetId);
        long visibleMillis =
                CalendarWidgetPrefs.visibleMonthMillis(
                        context,
                        widgetId);

        android.icu.util.Calendar visible =
                CalendarUtils.fromMillis(
                        type,
                        visibleMillis);
        int year = visible.get(
                android.icu.util.Calendar.YEAR);
        int month = visible.get(
                android.icu.util.Calendar.MONTH);

        root.setTextViewText(
                R.id.calendar_widget_title,
                CalendarUtils.monthName(type, month)
                        + " "
                        + CalendarUtils.fa(year));
        root.setTextColor(
                R.id.calendar_widget_title,
                text);
        root.setTextColor(
                R.id.calendar_widget_prev,
                primary);
        root.setTextColor(
                R.id.calendar_widget_next,
                primary);
        root.setTextColor(
                R.id.calendar_widget_today,
                primary);

        root.setOnClickPendingIntent(
                R.id.calendar_widget_prev,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_PREVIOUS_MONTH,
                        1,
                        0L));
        root.setOnClickPendingIntent(
                R.id.calendar_widget_next,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_NEXT_MONTH,
                        2,
                        0L));
        root.setOnClickPendingIntent(
                R.id.calendar_widget_today,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_TODAY,
                        3,
                        0L));
        root.setOnClickPendingIntent(
                R.id.calendar_widget_title,
                openClockPendingIntent(
                        context,
                        widgetId));

        applyWeekdays(
                context,
                root,
                type,
                primary,
                muted,
                holiday);

        populateDays(
                context,
                root,
                widgetId,
                type,
                year,
                month,
                selectedMillis,
                widthDp,
                heightDp,
                text,
                muted,
                primary,
                holiday);

        bindSelectedDate(
                context,
                root,
                type,
                selectedMillis,
                text,
                muted);

        boolean compact = heightDp < 275f;
        boolean showSelected =
                heightDp >= 225f;
        boolean showEvents =
                !compact
                        && CalendarWidgetPrefs.showEvents(
                        context,
                        widgetId);
        boolean showPrayer =
                heightDp >= 335f
                        && CalendarWidgetPrefs.showPrayerTimes(
                        context,
                        widgetId)
                        && !orderedHorizons(context).isEmpty();

        root.setViewVisibility(
                R.id.calendar_selected_card,
                showSelected
                        ? View.VISIBLE
                        : View.GONE);

        root.setViewVisibility(
                R.id.calendar_events_text,
                showEvents
                        ? View.VISIBLE
                        : View.GONE);
        if (showEvents) {
            root.setTextViewText(
                    R.id.calendar_events_text,
                    buildEventText(
                            context,
                            selectedMillis,
                            type));
            root.setTextColor(
                    R.id.calendar_events_text,
                    text);
        }

        root.setViewVisibility(
                R.id.calendar_prayer_list,
                showPrayer
                        ? View.VISIBLE
                        : View.GONE);
        if (showPrayer) {
            Intent serviceIntent = new Intent(
                    context,
                    CalendarPrayerWidgetService.class);
            serviceIntent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    widgetId);
            serviceIntent.setData(
                    Uri.parse(
                            serviceIntent.toUri(
                                    Intent.URI_INTENT_SCHEME)));
            root.setRemoteAdapter(
                    R.id.calendar_prayer_list,
                    serviceIntent);
        }

        float cellWidthDp = Math.max(28f, (widthDp - 16f) / 7f);
        root.setTextViewTextSize(
                R.id.calendar_widget_title,
                TypedValue.COMPLEX_UNIT_DIP,
                clamp(widthDp / 24f, 13f, 20f));
        float weekdaySize = clamp(cellWidthDp * 0.24f, 9.5f, 13f);
        for (int id : WEEKDAY_VIEW_IDS) {
            root.setTextViewTextSize(
                    id,
                    TypedValue.COMPLEX_UNIT_DIP,
                    weekdaySize);
        }

        return root;
    }

    private static void applyWeekdays(
            Context context,
            RemoteViews root,
            int type,
            int primary,
            int muted,
            int holiday) {
        int[] labels = type == CalendarUtils.GREGORIAN
                ? WEEKDAY_IDS_GREGORIAN
                : WEEKDAY_IDS_IRAN_HIJRI;

        for (int i = 0; i < WEEKDAY_VIEW_IDS.length; i++) {
            root.setTextViewText(
                    WEEKDAY_VIEW_IDS[i],
                    AppString.get(labels[i]));
            root.setTextColor(
                    WEEKDAY_VIEW_IDS[i],
                    i == 6
                            ? holiday
                            : primary);
        }
    }

    private static void populateDays(
            Context context,
            RemoteViews root,
            int widgetId,
            int type,
            int year,
            int month,
            long selectedMillis,
            float widthDp,
            float heightDp,
            int text,
            int muted,
            int primary,
            int holidayColor) {
        android.icu.util.Calendar first =
                CalendarUtils.create(type);
        first.clear();
        first.set(
                year,
                month,
                1,
                12,
                0,
                0);

        int leading = leadingDays(first, type);
        int days = first.getActualMaximum(
                android.icu.util.Calendar.DAY_OF_MONTH);
        int rows = Math.max(
                4,
                Math.min(
                        6,
                        (leading + days + 6) / 7));

        for (int row = 0; row < WEEK_ROW_IDS.length; row++) {
            int rowId = WEEK_ROW_IDS[row];
            root.removeAllViews(rowId);
            root.setViewVisibility(
                    rowId,
                    row < rows
                            ? View.VISIBLE
                            : View.GONE);

            for (int col = 0; col < 7; col++) {
                int slot = row * 7 + col;
                int day = slot - leading + 1;

                if (day < 1 || day > days) {
                    root.addView(
                            rowId,
                            blankDay(
                                    context,
                                    widthDp,
                                    heightDp,
                                    text,
                                    muted));
                    continue;
                }

                long millis = CalendarUtils.toMillis(
                        type,
                        year,
                        month,
                        day,
                        12,
                        0);

                root.addView(
                        rowId,
                        dayView(
                                context,
                                widgetId,
                                slot,
                                type,
                                day,
                                millis,
                                selectedMillis,
                                widthDp,
                                heightDp,
                                text,
                                muted,
                                primary,
                                holidayColor));
            }
        }
    }

    private static RemoteViews blankDay(
            Context context,
            float widthDp,
            float heightDp,
            int text,
            int muted) {
        RemoteViews item = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_calendar_day);
        item.setTextViewText(
                R.id.calendar_day_main,
                "");
        item.setTextViewText(
                R.id.calendar_day_alt_1,
                "");
        item.setTextViewText(
                R.id.calendar_day_alt_2,
                "");
        item.setInt(
                R.id.calendar_day_root,
                "setBackgroundResource",
                android.R.color.transparent);
        sizeDayText(item, widthDp, heightDp);
        return item;
    }

    private static RemoteViews dayView(
            Context context,
            int widgetId,
            int slot,
            int type,
            int day,
            long millis,
            long selectedMillis,
            float widthDp,
            float heightDp,
            int text,
            int muted,
            int primary,
            int holidayColor) {
        RemoteViews item = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_calendar_day);

        int other1 = CalendarUtils.otherTypeOne(type);
        int other2 = CalendarUtils.otherTypeTwo(type);

        android.icu.util.Calendar alt1 =
                CalendarUtils.fromMillis(
                        other1,
                        millis);
        android.icu.util.Calendar alt2 =
                CalendarUtils.fromMillis(
                        other2,
                        millis);

        boolean selected =
                sameDate(
                        type,
                        millis,
                        selectedMillis);
        boolean today =
                sameDate(
                        type,
                        millis,
                        System.currentTimeMillis());
        boolean holiday =
                CalendarEventRepository.isWeekend(
                        millis,
                        type)
                        || CalendarEventRepository
                        .isOfficialHolidayInEnabledSources(
                                context,
                                millis,
                                type);
        boolean hasEvent =
                holiday
                        || hasAnyEvent(
                                context,
                                millis,
                                type);

        String main = CalendarUtils.fa(day);
        if (hasEvent) {
            main += " •";
        }

        item.setTextViewText(
                R.id.calendar_day_main,
                main);
        item.setTextViewText(
                R.id.calendar_day_alt_1,
                CalendarUtils.fa(
                        alt1.get(
                                android.icu.util.Calendar.DAY_OF_MONTH)));
        item.setTextViewText(
                R.id.calendar_day_alt_2,
                CalendarUtils.fa(
                        alt2.get(
                                android.icu.util.Calendar.DAY_OF_MONTH)));

        int mainColor = selected
                ? 0xFFFFFFFF
                : holiday
                ? holidayColor
                : today
                ? primary
                : text;
        int secondaryColor = selected
                ? 0xFFFFFFFF
                : holiday
                ? holidayColor
                : muted;

        item.setTextColor(
                R.id.calendar_day_main,
                mainColor);
        item.setTextColor(
                R.id.calendar_day_alt_1,
                secondaryColor);
        item.setTextColor(
                R.id.calendar_day_alt_2,
                secondaryColor);

        item.setInt(
                R.id.calendar_day_root,
                "setBackgroundResource",
                selected
                        ? selectedDayBackground(context)
                        : today
                        ? R.drawable.bg_soft_button
                        : android.R.color.transparent);

        item.setOnClickPendingIntent(
                R.id.calendar_day_root,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_SELECT_DAY,
                        100 + slot,
                        millis));

        sizeDayText(item, widthDp, heightDp);
        return item;
    }

    private static int selectedDayBackground(
            Context context) {
        switch (AppSettings.palette(context)) {
            case AppSettings.PALETTE_TERRACOTTA_NAVY:
                return R.drawable.widget_calendar_selected_palette_0;
            case AppSettings.PALETTE_MAGENTA_SKY:
                return R.drawable.widget_calendar_selected_palette_1;
            case AppSettings.PALETTE_MAGENTA_CHARCOAL:
                return R.drawable.widget_calendar_selected_palette_2;
            case AppSettings.PALETTE_TEAL_RED:
                return R.drawable.widget_calendar_selected_palette_3;
            case AppSettings.PALETTE_PURPLE_GOLD:
                return R.drawable.widget_calendar_selected_palette_4;
            case AppSettings.PALETTE_NEON_MAGENTA_GRAPHITE:
                return R.drawable.widget_calendar_selected_palette_5;
            case AppSettings.PALETTE_BLUE_CYAN:
            default:
                return R.drawable.widget_calendar_selected_palette_6;
        }
    }

    private static void sizeDayText(
            RemoteViews item,
            float widthDp,
            float heightDp) {
        // Each day owns one seventh of the usable width. Use both dimensions
        // so a tall calendar does not keep tiny fixed text just because its
        // width happens to match another launcher/device.
        float cellWidthDp = Math.max(
                28f,
                (widthDp - 16f) / 7f);
        float estimatedRowHeightDp = Math.max(
                30f,
                (heightDp - 72f) / 6f);

        float main = clamp(
                Math.min(
                        cellWidthDp * 0.38f,
                        estimatedRowHeightDp * 0.30f),
                14f,
                22f);
        float alt = clamp(
                Math.min(
                        cellWidthDp * 0.23f,
                        estimatedRowHeightDp * 0.18f),
                9f,
                13f);

        // DIP keeps the calendar visually consistent when two phones use
        // different system font-scale settings.
        item.setTextViewTextSize(
                R.id.calendar_day_main,
                TypedValue.COMPLEX_UNIT_DIP,
                main);
        item.setTextViewTextSize(
                R.id.calendar_day_alt_1,
                TypedValue.COMPLEX_UNIT_DIP,
                alt);
        item.setTextViewTextSize(
                R.id.calendar_day_alt_2,
                TypedValue.COMPLEX_UNIT_DIP,
                alt);
    }

    private static float clamp(
            float value,
            float min,
            float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static void bindSelectedDate(
            Context context,
            RemoteViews root,
            int type,
            long selectedMillis,
            int text,
            int muted) {
        int other1 = CalendarUtils.otherTypeOne(type);
        int other2 = CalendarUtils.otherTypeTwo(type);

        root.setTextViewText(
                R.id.calendar_selected_date,
                CalendarUtils.calendarName(type)
                        + ": "
                        + CalendarUtils.formatDate(
                                selectedMillis,
                                type));
        root.setTextViewText(
                R.id.calendar_selected_alt,
                CalendarUtils.calendarName(other1)
                        + ": "
                        + CalendarUtils.formatDate(
                                selectedMillis,
                                other1)
                        + "    "
                        + CalendarUtils.calendarName(other2)
                        + ": "
                        + CalendarUtils.formatDate(
                                selectedMillis,
                                other2));

        root.setTextColor(
                R.id.calendar_selected_date,
                text);
        root.setTextColor(
                R.id.calendar_selected_alt,
                muted);
    }

    private static String buildEventText(
            Context context,
            long millis,
            int primaryType) {
        ArrayList<String> values =
                new ArrayList<>();

        for (int source = CalendarUtils.PERSIAN;
             source <= CalendarUtils.HIJRI;
             source++) {
            if (!CalendarEventRepository.sourceEnabled(
                    context,
                    primaryType,
                    source)) {
                continue;
            }

            List<CalendarEventRepository.Event> events =
                    CalendarEventRepository.eventsFor(
                            context,
                            millis,
                            source);
            for (CalendarEventRepository.Event event : events) {
                if (event.title != null
                        && !event.title.trim().isEmpty()) {
                    String title = "• " + event.title.trim();
                    if (!values.contains(title)) values.add(title);
                }
            }
        }

        if (CalendarEventRepository.isWeekend(
                millis,
                primaryType)) {
            values.add(
                    "• "
                            + AppString.get(R.string.runtime_text_0391));
        }

        if (values.isEmpty()) {
            return AppString.get(R.string.runtime_text_0392);
        }

        StringBuilder out =
                new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) out.append("\n");
            out.append(values.get(i));
        }
        return out.toString();
    }

    private static boolean hasAnyEvent(
            Context context,
            long millis,
            int primaryType) {
        for (int source = CalendarUtils.PERSIAN;
             source <= CalendarUtils.HIJRI;
             source++) {
            if (CalendarEventRepository.sourceEnabled(
                    context,
                    primaryType,
                    source)
                    && !CalendarEventRepository.eventsFor(
                    context,
                    millis,
                    source).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    static List<AppSettings.PrayerHorizon> orderedHorizons(
            Context context) {
        List<AppSettings.PrayerHorizon> source =
                AppSettings.prayerHorizons(context);
        if (source.isEmpty()) {
            return Collections.emptyList();
        }

        ArrayList<AppSettings.PrayerHorizon> ordered =
                new ArrayList<>();
        AppSettings.PrayerHorizon primary = null;

        for (AppSettings.PrayerHorizon item : source) {
            if (AppSettings.isPrimaryPrayerHorizon(
                    context,
                    item.latitude,
                    item.longitude)) {
                primary = item;
                break;
            }
        }

        if (primary != null) {
            ordered.add(primary);
        }

        for (AppSettings.PrayerHorizon item : source) {
            if (primary != null
                    && Math.abs(
                    item.latitude
                            - primary.latitude) < 0.0001
                    && Math.abs(
                    item.longitude
                            - primary.longitude) < 0.0001) {
                continue;
            }
            ordered.add(item);
        }

        return ordered;
    }

    static String shortHorizonLabel(
            Context context,
            String label) {
        if (label == null
                || label.trim().isEmpty()) {
            return AppString.get(R.string.runtime_text_0418);
        }

        String value = label.trim();
        int comma = value.indexOf('،');
        if (comma < 0) {
            comma = value.indexOf(',');
        }

        return comma > 0
                ? value.substring(
                0,
                comma).trim()
                : value;
    }

    private static int leadingDays(
            android.icu.util.Calendar first,
            int type) {
        int dayOfWeek = first.get(
                android.icu.util.Calendar.DAY_OF_WEEK);
        if (type == CalendarUtils.GREGORIAN) {
            return (dayOfWeek + 5) % 7;
        }
        return dayOfWeek % 7;
    }

    private static boolean sameDate(
            int type,
            long left,
            long right) {
        android.icu.util.Calendar a =
                CalendarUtils.fromMillis(
                        type,
                        left);
        android.icu.util.Calendar b =
                CalendarUtils.fromMillis(
                        type,
                        right);

        return a.get(
                android.icu.util.Calendar.YEAR)
                == b.get(
                android.icu.util.Calendar.YEAR)
                && a.get(
                android.icu.util.Calendar.MONTH)
                == b.get(
                android.icu.util.Calendar.MONTH)
                && a.get(
                android.icu.util.Calendar.DAY_OF_MONTH)
                == b.get(
                android.icu.util.Calendar.DAY_OF_MONTH);
    }

    private static void moveMonth(
            Context context,
            int widgetId,
            int delta) {
        int type =
                AppSettings.defaultCalendar(
                        context);
        long visibleMillis =
                CalendarWidgetPrefs.visibleMonthMillis(
                        context,
                        widgetId);
        android.icu.util.Calendar calendar =
                CalendarUtils.fromMillis(
                        type,
                        visibleMillis);

        int monthIndex = calendar.get(android.icu.util.Calendar.YEAR) * 12
                + calendar.get(android.icu.util.Calendar.MONTH) + delta;
        int year = Math.floorDiv(monthIndex, 12);
        int month = Math.floorMod(monthIndex, 12);
        if (year < CalendarUtils.minimumYear(type) || year > CalendarUtils.maximumYear(type)) return;
        long value = CalendarUtils.toMillis(type, year, month, 1, 12, 0);
        CalendarWidgetPrefs.setVisibleMonthMillis(
                context,
                widgetId,
                value);
        CalendarWidgetPrefs.setSelectedMillis(
                context,
                widgetId,
                value);
    }

    private static PendingIntent actionPendingIntent(
            Context context,
            int widgetId,
            String action,
            int actionId,
            long selectedMillis) {
        Intent intent = new Intent(
                context,
                CalendarWidgetProvider.class)
                .setAction(action)
                .putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        widgetId);

        if (selectedMillis > 0L) {
            intent.putExtra(
                    EXTRA_SELECTED_MILLIS,
                    selectedMillis);
        }

        intent.setData(
                Uri.parse(
                        "advanceclock://calendar-widget/"
                                + widgetId
                                + "/"
                                + actionId));

        return PendingIntent.getBroadcast(
                context,
                10_100_000
                        + Math.abs(
                        widgetId % 50_000) * 100
                        + actionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openClockPendingIntent(
            Context context,
            int widgetId) {
        Intent intent = new Intent(
                context,
                MainActivity.class)
                .putExtra(
                        "openTab",
                        "clock")
                .addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(
                context,
                10_900_000
                        + Math.abs(
                        widgetId % 100_000),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }
}
