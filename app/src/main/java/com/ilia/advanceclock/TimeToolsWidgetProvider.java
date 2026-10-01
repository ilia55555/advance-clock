package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Locale;

public final class TimeToolsWidgetProvider extends AppWidgetProvider {
    private static final String PREFS = "time_tools";

    private static final String ACTION_TAB_STOPWATCH =
            "com.ilia.advanceclock.TIME_TOOLS_TAB_STOPWATCH";
    private static final String ACTION_TAB_TIMER =
            "com.ilia.advanceclock.TIME_TOOLS_TAB_TIMER";
    private static final String ACTION_TOGGLE =
            "com.ilia.advanceclock.TIME_TOOLS_TOGGLE";
    private static final String ACTION_RESET =
            "com.ilia.advanceclock.TIME_TOOLS_RESET";
    private static final String ACTION_LAP =
            "com.ilia.advanceclock.TIME_TOOLS_LAP";

    @Override public void onUpdate(
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

    @Override public void onAppWidgetOptionsChanged(
            Context context,
            AppWidgetManager manager,
            int appWidgetId,
            Bundle newOptions) {
        update(context, manager, appWidgetId, newOptions);
    }

    @Override public void onRestored(
            Context context,
            int[] oldWidgetIds,
            int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
        int count = Math.min(oldWidgetIds.length, newWidgetIds.length);
        for (int i = 0; i < count; i++) {
            TimeToolsWidgetPrefs.migrate(
                    context,
                    oldWidgetIds[i],
                    newWidgetIds[i]);
        }
        updateAll(context);
    }

    @Override public void onDeleted(
            Context context,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            TimeToolsWidgetPrefs.clear(context, id);
        }
        super.onDeleted(context, appWidgetIds);
    }

    @Override public void onReceive(
            Context context,
            Intent intent) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_TAB_STOPWATCH.equals(action)
                || ACTION_TAB_TIMER.equals(action)
                || ACTION_TOGGLE.equals(action)
                || ACTION_RESET.equals(action)
                || ACTION_LAP.equals(action)) {
            int widgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

            if (ACTION_TAB_STOPWATCH.equals(action)) {
                TimeToolsWidgetPrefs.setActiveTab(
                        context,
                        widgetId,
                        TimeToolsWidgetPrefs.TAB_STOPWATCH);
            } else if (ACTION_TAB_TIMER.equals(action)) {
                TimeToolsWidgetPrefs.setActiveTab(
                        context,
                        widgetId,
                        TimeToolsWidgetPrefs.TAB_TIMER);
            } else {
                int tab = TimeToolsWidgetPrefs.activeTab(
                        context,
                        widgetId);
                if (ACTION_TOGGLE.equals(action)) {
                    if (tab == TimeToolsWidgetPrefs.TAB_TIMER) {
                        toggleTimer(context, widgetId);
                    } else {
                        toggleStopwatch(context);
                    }
                } else if (ACTION_RESET.equals(action)) {
                    if (tab == TimeToolsWidgetPrefs.TAB_TIMER) {
                        resetTimer(context);
                    } else {
                        resetStopwatch(context);
                    }
                } else if (ACTION_LAP.equals(action)
                        && tab == TimeToolsWidgetPrefs.TAB_STOPWATCH) {
                    addStopwatchLap(context);
                }
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

    public static void updateAll(Context context) {
        normalizeExpiredState(context);
        AppWidgetManager manager =
                AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(
                        context,
                        TimeToolsWidgetProvider.class));
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
        normalizeExpiredState(context);
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
    }

    private static RemoteViews createRemoteViews(
            Context context,
            int widgetId,
            float widthDp,
            float heightDp) {
        RemoteViews root = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_time_tools);

        boolean dark =
                AppSettings.themeMode(context)
                        == AppSettings.THEME_DARK;
        int primary = AppSettings.primaryColor(context);
        int text = AppSettings.textPrimary(context);
        int muted = AppSettings.textSecondary(context);

        int tab = TimeToolsWidgetPrefs.activeTab(
                context,
                widgetId);
        boolean stopwatch =
                tab == TimeToolsWidgetPrefs.TAB_STOPWATCH;

        root.setInt(
                R.id.time_tools_widget_root,
                "setBackgroundResource",
                dark
                        ? R.drawable.time_tools_widget_background_dark
                        : R.drawable.time_tools_widget_background_light);

        styleTab(
                root,
                R.id.time_tools_tab_stopwatch,
                stopwatch,
                primary,
                text);
        styleTab(
                root,
                R.id.time_tools_tab_timer,
                !stopwatch,
                primary,
                text);

        float width = Math.max(1f, widthDp);
        float height = Math.max(1f, heightDp);
        boolean tinyHeight = height < 92f;
        boolean compactHeight = height < 135f;
        boolean narrow = width < 190f;
        boolean veryNarrow = width < 145f;

        float timeSize = veryNarrow ? 18f
                : narrow ? 22f
                : width > 300f ? 34f : 28f;
        float tabSize = veryNarrow ? 9f : 12f;
        float controlSize = veryNarrow ? 8f : 10f;

        root.setTextViewTextSize(
                R.id.time_tools_static_time,
                TypedValue.COMPLEX_UNIT_SP,
                timeSize);
        root.setTextViewTextSize(
                R.id.time_tools_chronometer,
                TypedValue.COMPLEX_UNIT_SP,
                timeSize);
        root.setTextViewTextSize(
                R.id.time_tools_tab_stopwatch,
                TypedValue.COMPLEX_UNIT_SP,
                tabSize);
        root.setTextViewTextSize(
                R.id.time_tools_tab_timer,
                TypedValue.COMPLEX_UNIT_SP,
                tabSize);

        for (int id : new int[]{
                R.id.time_tools_reset,
                R.id.time_tools_primary,
                R.id.time_tools_lap,
                R.id.time_tools_open}) {
            root.setTextViewTextSize(
                    id,
                    TypedValue.COMPLEX_UNIT_SP,
                    controlSize);
        }

        root.setTextColor(
                R.id.time_tools_static_time,
                text);
        root.setTextColor(
                R.id.time_tools_chronometer,
                text);
        root.setTextColor(
                R.id.time_tools_detail,
                muted);
        root.setTextColor(
                R.id.time_tools_reset,
                text);
        root.setTextColor(
                R.id.time_tools_lap,
                primary);
        root.setTextColor(
                R.id.time_tools_open,
                primary);

        root.setOnClickPendingIntent(
                R.id.time_tools_tab_stopwatch,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_TAB_STOPWATCH,
                        1));
        root.setOnClickPendingIntent(
                R.id.time_tools_tab_timer,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_TAB_TIMER,
                        2));
        root.setOnClickPendingIntent(
                R.id.time_tools_primary,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_TOGGLE,
                        3));
        root.setOnClickPendingIntent(
                R.id.time_tools_reset,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_RESET,
                        4));
        root.setOnClickPendingIntent(
                R.id.time_tools_lap,
                actionPendingIntent(
                        context,
                        widgetId,
                        ACTION_LAP,
                        5));

        root.setOnClickPendingIntent(
                R.id.time_tools_open,
                openTabPendingIntent(
                        context,
                        widgetId,
                        stopwatch ? "stopwatch" : "timer"));
        root.setOnClickPendingIntent(
                R.id.time_tools_static_time,
                openTabPendingIntent(
                        context,
                        widgetId,
                        stopwatch ? "stopwatch" : "timer"));
        root.setOnClickPendingIntent(
                R.id.time_tools_chronometer,
                openTabPendingIntent(
                        context,
                        widgetId,
                        stopwatch ? "stopwatch" : "timer"));
        root.setOnClickPendingIntent(
                R.id.time_tools_detail,
                openTabPendingIntent(
                        context,
                        widgetId,
                        stopwatch ? "stopwatch" : "timer"));

        if (stopwatch) {
            renderStopwatch(
                    context,
                    root,
                    widgetId);
        } else {
            renderTimer(
                    context,
                    root,
                    widgetId);
        }

        boolean showDetails =
                TimeToolsWidgetPrefs.showDetails(
                        context,
                        widgetId)
                        && !compactHeight;
        root.setViewVisibility(
                R.id.time_tools_detail,
                showDetails
                        ? View.VISIBLE
                        : View.GONE);

        boolean showReset =
                TimeToolsWidgetPrefs.showReset(
                        context,
                        widgetId)
                        && !veryNarrow;
        root.setViewVisibility(
                R.id.time_tools_reset,
                showReset
                        ? View.VISIBLE
                        : View.GONE);

        boolean showOpen =
                TimeToolsWidgetPrefs.showOpen(
                        context,
                        widgetId)
                        && !narrow;
        root.setViewVisibility(
                R.id.time_tools_open,
                showOpen
                        ? View.VISIBLE
                        : View.GONE);

        if (stopwatch) {
            boolean showLap =
                    TimeToolsWidgetPrefs.showLap(
                            context,
                            widgetId)
                            && !veryNarrow;
            root.setViewVisibility(
                    R.id.time_tools_lap,
                    showLap
                            ? View.VISIBLE
                            : View.GONE);
        } else {
            root.setViewVisibility(
                    R.id.time_tools_lap,
                    View.GONE);
        }

        root.setViewVisibility(
                R.id.time_tools_controls,
                tinyHeight
                        ? View.GONE
                        : View.VISIBLE);

        return root;
    }

    private static void renderStopwatch(
            Context context,
            RemoteViews root,
            int widgetId) {
        SharedPreferences p = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);

        boolean running = p.getBoolean(
                "stopwatch_running",
                false);
        long elapsed = stopwatchElapsed(p);
        long limit = p.getLong(
                "stopwatch_limit",
                0L);

        root.setTextViewText(
                R.id.time_tools_primary,
                context.getString(
                        running
                                ? R.string.runtime_text_0011
                                : elapsed > 0L
                                ? R.string.runtime_text_0012
                                : R.string.runtime_text_0010));

        if (running) {
            long base = SystemClock.elapsedRealtime() - elapsed;
            root.setChronometer(
                    R.id.time_tools_chronometer,
                    base,
                    null,
                    true);
            root.setChronometerCountDown(
                    R.id.time_tools_chronometer,
                    false);
            root.setViewVisibility(
                    R.id.time_tools_chronometer,
                    View.VISIBLE);
            root.setViewVisibility(
                    R.id.time_tools_static_time,
                    View.GONE);
        } else {
            root.setTextViewText(
                    R.id.time_tools_static_time,
                    formatStopwatch(elapsed));
            root.setViewVisibility(
                    R.id.time_tools_chronometer,
                    View.GONE);
            root.setViewVisibility(
                    R.id.time_tools_static_time,
                    View.VISIBLE);
        }

        String detail = latestLap(context, p);
        if (detail.isEmpty() && limit > 0L) {
            detail = context.getString(
                    R.string.time_tools_widget_limit)
                    + " "
                    + formatTimer(limit);
        }
        if (detail.isEmpty()) {
            detail = context.getString(
                    R.string.time_tools_widget_stopwatch_ready);
        }

        root.setTextViewText(
                R.id.time_tools_detail,
                detail);
        root.setTextViewText(
                R.id.time_tools_reset,
                context.getString(
                        R.string.runtime_text_0167));
        root.setTextViewText(
                R.id.time_tools_lap,
                context.getString(
                        R.string.runtime_text_0168));
    }

    private static void renderTimer(
            Context context,
            RemoteViews root,
            int widgetId) {
        SharedPreferences p = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);

        boolean running = p.getBoolean(
                "timer_running",
                false);
        long remaining = timerRemaining(p);
        boolean dateMode = p.getBoolean(
                "timer_date_mode",
                false);

        long idleValue = remaining > 0L
                ? remaining
                : dateMode
                ? Math.max(
                        0L,
                        p.getLong("timer_target", 0L)
                                - System.currentTimeMillis())
                : TimeToolsWidgetPrefs.timerDefaultMillis(
                        context,
                        widgetId);

        root.setTextViewText(
                R.id.time_tools_primary,
                context.getString(
                        running
                                ? R.string.runtime_text_0011
                                : remaining > 0L
                                ? R.string.runtime_text_0012
                                : R.string.runtime_text_0010));

        if (running) {
            long base = SystemClock.elapsedRealtime()
                    + remaining;
            root.setChronometer(
                    R.id.time_tools_chronometer,
                    base,
                    null,
                    true);
            root.setChronometerCountDown(
                    R.id.time_tools_chronometer,
                    true);
            root.setViewVisibility(
                    R.id.time_tools_chronometer,
                    View.VISIBLE);
            root.setViewVisibility(
                    R.id.time_tools_static_time,
                    View.GONE);
        } else {
            root.setTextViewText(
                    R.id.time_tools_static_time,
                    formatTimer(idleValue));
            root.setViewVisibility(
                    R.id.time_tools_chronometer,
                    View.GONE);
            root.setViewVisibility(
                    R.id.time_tools_static_time,
                    View.VISIBLE);
        }

        String detail;
        if (running) {
            long deadline = p.getLong(
                    "timer_end",
                    0L);
            detail = context.getString(
                    R.string.runtime_text_0479)
                    + CalendarUtils.formatDate(
                            deadline,
                            p.getInt(
                                    "timer_calendar",
                                    AppSettings.defaultCalendar(context)))
                    + "  "
                    + String.format(
                            Locale.US,
                            "%tR",
                            deadline);
        } else if (remaining > 0L) {
            detail = context.getString(
                    R.string.time_tools_widget_timer_paused);
        } else if (dateMode) {
            detail = context.getString(
                    R.string.runtime_text_0480);
        } else {
            detail = context.getString(
                    R.string.time_tools_widget_default_duration)
                    + " "
                    + formatTimer(
                            TimeToolsWidgetPrefs.timerDefaultMillis(
                                    context,
                                    widgetId));
        }

        root.setTextViewText(
                R.id.time_tools_detail,
                detail);
        root.setTextViewText(
                R.id.time_tools_reset,
                context.getString(
                        R.string.runtime_text_0171));
    }

    private static void styleTab(
            RemoteViews root,
            int id,
            boolean active,
            int primary,
            int text) {
        root.setInt(
                id,
                "setBackgroundResource",
                active
                        ? R.drawable.bg_teal_button
                        : R.drawable.bg_soft_button);
        root.setTextColor(
                id,
                active ? 0xFFFFFFFF : text);
    }

    private static PendingIntent actionPendingIntent(
            Context context,
            int widgetId,
            String action,
            int actionId) {
        Intent intent = new Intent(
                context,
                TimeToolsWidgetProvider.class)
                .setAction(action)
                .putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        widgetId);

        return PendingIntent.getBroadcast(
                context,
                9_200_000
                        + Math.abs(widgetId % 100_000) * 10
                        + actionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent openTabPendingIntent(
            Context context,
            int widgetId,
            String tab) {
        Intent intent = new Intent(
                context,
                MainActivity.class)
                .putExtra("openTab", tab)
                .addFlags(
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        return PendingIntent.getActivity(
                context,
                9_300_000
                        + Math.abs(widgetId % 100_000)
                        + ("timer".equals(tab)
                        ? 100_000
                        : 0),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void toggleStopwatch(
            Context context) {
        SharedPreferences p = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);

        boolean running = p.getBoolean(
                "stopwatch_running",
                false);
        long accumulated = p.getLong(
                "stopwatch_accumulated",
                0L);
        long started = p.getLong(
                "stopwatch_started",
                0L);
        long limit = p.getLong(
                "stopwatch_limit",
                0L);
        int mode = p.getInt(
                "stopwatch_mode",
                0);

        if (running) {
            long elapsed = accumulated
                    + Math.max(
                            0L,
                            System.currentTimeMillis() - started);
            p.edit()
                    .putBoolean("stopwatch_running", false)
                    .putLong("stopwatch_accumulated", elapsed)
                    .putLong("stopwatch_started", 0L)
                    .apply();
            ToolAlarmScheduler.cancel(
                    context,
                    ToolAlarmScheduler.STOPWATCH);
            return;
        }

        long now = System.currentTimeMillis();

        if (accumulated == 0L) {
            if (mode == 0) {
                limit = 0L;
            } else if (mode == 2) {
                long target = p.getLong(
                        "stopwatch_target",
                        0L);
                limit = target - now;
                if (limit <= 0L) return;
            } else if (limit <= 0L) {
                return;
            }
        }

        if (limit > 0L
                && accumulated >= limit) {
            return;
        }

        if (mode == 2) {
            long target = p.getLong(
                    "stopwatch_target",
                    0L);
            if (target <= now) return;
            limit = accumulated
                    + target
                    - now;
        }

        p.edit()
                .putBoolean("stopwatch_running", true)
                .putLong("stopwatch_started", now)
                .putLong("stopwatch_limit", limit)
                .apply();

        if (limit > 0L) {
            ToolAlarmScheduler.schedule(
                    context,
                    ToolAlarmScheduler.STOPWATCH,
                    now + (limit - accumulated),
                    ToolAlarmScheduler.defaultLabel(
                            ToolAlarmScheduler.STOPWATCH));
        }
    }

    private static void resetStopwatch(
            Context context) {
        context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE)
                .edit()
                .putBoolean("stopwatch_running", false)
                .putLong("stopwatch_accumulated", 0L)
                .putLong("stopwatch_started", 0L)
                .putLong("stopwatch_limit", 0L)
                .putString("stopwatch_laps", "")
                .apply();

        ToolAlarmScheduler.cancel(
                context,
                ToolAlarmScheduler.STOPWATCH);
    }

    private static void addStopwatchLap(
            Context context) {
        SharedPreferences p = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);
        if (!p.getBoolean(
                "stopwatch_running",
                false)) {
            return;
        }

        long elapsed = stopwatchElapsed(p);
        if (elapsed <= 0L) return;

        String encoded = p.getString(
                "stopwatch_laps",
                "");
        String next = Long.toString(elapsed);
        if (encoded != null
                && !encoded.isEmpty()) {
            next += "," + encoded;
        }

        p.edit()
                .putString(
                        "stopwatch_laps",
                        next)
                .apply();
    }

    private static void toggleTimer(
            Context context,
            int widgetId) {
        SharedPreferences p = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);

        boolean running = p.getBoolean(
                "timer_running",
                false);
        long remaining = p.getLong(
                "timer_remaining",
                0L);
        long deadline = p.getLong(
                "timer_end",
                0L);

        if (running) {
            remaining = Math.max(
                    0L,
                    deadline - System.currentTimeMillis());
            p.edit()
                    .putBoolean("timer_running", false)
                    .putLong("timer_remaining", remaining)
                    .putLong("timer_end", 0L)
                    .apply();
            ToolAlarmScheduler.cancel(
                    context,
                    ToolAlarmScheduler.TIMER);
            return;
        }

        long value = remaining;
        if (value <= 0L) {
            boolean dateMode = p.getBoolean(
                    "timer_date_mode",
                    false);
            if (dateMode) {
                value = p.getLong(
                        "timer_target",
                        0L)
                        - System.currentTimeMillis();
            } else {
                value = TimeToolsWidgetPrefs.timerDefaultMillis(
                        context,
                        widgetId);
            }
        }

        if (value <= 0L) return;

        long now = System.currentTimeMillis();
        deadline = now + value;

        p.edit()
                .putBoolean("timer_running", true)
                .putLong("timer_remaining", value)
                .putLong("timer_end", deadline)
                .apply();

        ToolAlarmScheduler.schedule(
                context,
                ToolAlarmScheduler.TIMER,
                deadline,
                ToolAlarmScheduler.defaultLabel(
                        ToolAlarmScheduler.TIMER));
    }

    private static void resetTimer(
            Context context) {
        context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE)
                .edit()
                .putBoolean("timer_running", false)
                .putLong("timer_remaining", 0L)
                .putLong("timer_end", 0L)
                .apply();

        ToolAlarmScheduler.cancel(
                context,
                ToolAlarmScheduler.TIMER);
    }

    private static void normalizeExpiredState(
            Context context) {
        SharedPreferences p = context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE);

        long now = System.currentTimeMillis();

        if (p.getBoolean(
                "timer_running",
                false)) {
            long deadline = p.getLong(
                    "timer_end",
                    0L);
            if (deadline > 0L
                    && deadline <= now) {
                p.edit()
                        .putBoolean("timer_running", false)
                        .putLong("timer_remaining", 0L)
                        .putLong("timer_end", 0L)
                        .apply();
            }
        }

        if (p.getBoolean(
                "stopwatch_running",
                false)) {
            long limit = p.getLong(
                    "stopwatch_limit",
                    0L);
            long elapsed = stopwatchElapsed(p);
            if (limit > 0L
                    && elapsed >= limit) {
                p.edit()
                        .putBoolean("stopwatch_running", false)
                        .putLong("stopwatch_accumulated", limit)
                        .putLong("stopwatch_started", 0L)
                        .apply();
            }
        }
    }

    private static long stopwatchElapsed(
            SharedPreferences p) {
        long accumulated = p.getLong(
                "stopwatch_accumulated",
                0L);
        if (!p.getBoolean(
                "stopwatch_running",
                false)) {
            return Math.max(0L, accumulated);
        }

        return Math.max(
                0L,
                accumulated
                        + Math.max(
                        0L,
                        System.currentTimeMillis()
                                - p.getLong(
                                "stopwatch_started",
                                0L)));
    }

    private static long timerRemaining(
            SharedPreferences p) {
        if (!p.getBoolean(
                "timer_running",
                false)) {
            return Math.max(
                    0L,
                    p.getLong(
                            "timer_remaining",
                            0L));
        }

        return Math.max(
                0L,
                p.getLong(
                        "timer_end",
                        0L)
                        - System.currentTimeMillis());
    }

    private static String latestLap(
            Context context,
            SharedPreferences p) {
        String encoded = p.getString(
                "stopwatch_laps",
                "");
        if (encoded == null
                || encoded.isEmpty()) {
            return "";
        }

        String[] values = encoded.split(",");
        if (values.length == 0) return "";

        try {
            long lap = Long.parseLong(values[0]);
            return String.format(
                    Locale.US,
                    context.getString(
                            R.string.runtime_text_0437),
                    values.length,
                    formatStopwatch(lap));
        } catch (Exception ignored) {
            return "";
        }
    }

    private static String formatStopwatch(
            long millis) {
        long cs = Math.max(0L, millis) / 10L;
        return String.format(
                Locale.US,
                "%02d:%02d:%02d.%02d",
                cs / 360000L,
                (cs / 6000L) % 60L,
                (cs / 100L) % 60L,
                cs % 100L);
    }

    private static String formatTimer(
            long millis) {
        long total = (Math.max(
                0L,
                millis) + 999L) / 1000L;
        return String.format(
                Locale.US,
                "%02d:%02d:%02d",
                total / 3600L,
                (total / 60L) % 60L,
                total % 60L);
    }
}
