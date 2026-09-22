package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.List;

public final class NoForgetWidgetProvider extends AppWidgetProvider {
    @Override public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] ids) {
        for (int id : ids) {
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
            WidgetPrefs.migrate(
                    context,
                    oldWidgetIds[i],
                    newWidgetIds[i]);
        }
        updateAll(context);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new android.content.ComponentName(
                        context,
                        NoForgetWidgetProvider.class));
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
                (widthDp, heightDp) ->
                        createRemoteViews(
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
        int width = Math.max(1, Math.round(widthDp));
        int height = Math.max(1, Math.round(heightDp));

        boolean showHeader =
                WidgetPrefs.showHeader(context, widgetId);
        boolean showSection =
                WidgetPrefs.showSectionLabel(context, widgetId);

        int reserved = 24
                + (showHeader ? 45 : 0)
                + (showSection ? 20 : 0);

        int rowCount = Math.max(
                0,
                (height - reserved) / 62);
        rowCount = Math.min(
                WidgetPrefs.maxItems(context, widgetId),
                Math.min(10, rowCount));

        RemoteViews root = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_noforget);

        boolean dark =
                WidgetPrefs.isDark(context, widgetId);
        int text = dark ? 0xFFF2F5F4 : 0xFF173F3B;
        int muted = dark ? 0xFFAFBCB8 : 0xFF758783;
        int primary =
                WidgetPrefs.primary(context, widgetId);
        int secondary =
                WidgetPrefs.secondary(context, widgetId);

        root.setInt(
                R.id.noforget_widget_root,
                "setBackgroundResource",
                backgroundResource(
                        dark,
                        WidgetPrefs.backgroundOpacityMode(
                                context,
                                widgetId)));

        root.setViewVisibility(
                R.id.noforget_widget_header,
                showHeader ? View.VISIBLE : View.GONE);

        boolean showTime =
                showHeader
                        && WidgetPrefs.showTime(
                        context,
                        widgetId);
        root.setViewVisibility(
                R.id.noforget_widget_time,
                showTime ? View.VISIBLE : View.GONE);

        root.setViewVisibility(
                R.id.noforget_widget_date,
                showHeader
                        && WidgetPrefs.showDate(
                        context,
                        widgetId)
                        && (width >= 180 || !showTime)
                        ? View.VISIBLE
                        : View.GONE);

        root.setViewVisibility(
                R.id.noforget_widget_add,
                showHeader
                        && WidgetPrefs.showAddButton(
                        context,
                        widgetId)
                        ? View.VISIBLE
                        : View.GONE);

        root.setViewVisibility(
                R.id.noforget_widget_theme,
                showHeader
                        && WidgetPrefs.showSettingsButton(
                        context,
                        widgetId)
                        && width >= 145
                        ? View.VISIBLE
                        : View.GONE);

        root.setViewVisibility(
                R.id.noforget_widget_section_label,
                showSection ? View.VISIBLE : View.GONE);

        root.setTextColor(
                R.id.noforget_widget_time,
                primary);
        root.setTextColor(
                R.id.noforget_widget_date,
                muted);

        applyTimeFormat(
                root,
                R.id.noforget_widget_time,
                WidgetPrefs.timeFormatMode(
                        context,
                        widgetId),
                WidgetPrefs.showSeconds(
                        context,
                        widgetId));

        root.setTextColor(
                R.id.noforget_widget_section_label,
                primary);
        root.setTextColor(
                R.id.noforget_widget_add,
                secondary);
        root.setInt(
                R.id.noforget_widget_theme,
                "setColorFilter",
                secondary);
        root.setTextColor(
                R.id.noforget_widget_empty,
                muted);

        float[] sizes = fontSizes(
                WidgetPrefs.fontSizeMode(
                        context,
                        widgetId));

        root.setTextViewTextSize(
                R.id.noforget_widget_time,
                TypedValue.COMPLEX_UNIT_SP,
                sizes[0]);
        root.setTextViewTextSize(
                R.id.noforget_widget_date,
                TypedValue.COMPLEX_UNIT_SP,
                sizes[1]);
        root.setTextViewTextSize(
                R.id.noforget_widget_section_label,
                TypedValue.COMPLEX_UNIT_SP,
                sizes[2]);

        root.setTextViewText(
                R.id.noforget_widget_date,
                CalendarUtils.formatDate(
                        System.currentTimeMillis(),
                        AppSettings.defaultCalendar(context)));

        root.removeAllViews(
                R.id.noforget_widget_list);

        List<NoForgetItem> items = widgetOrder(
                new NoForgetStore(context).all(),
                rowCount,
                WidgetPrefs.sortMode(
                        context,
                        widgetId));

        if (rowCount <= 0) {
            root.setTextViewText(
                    R.id.noforget_widget_empty,
                    "برای نمایش یادداشت‌ها، ارتفاع ویجت را بیشتر کنید");
            root.setViewVisibility(
                    R.id.noforget_widget_empty,
                    View.VISIBLE);
        } else {
            root.setTextViewText(
                    R.id.noforget_widget_empty,
                    "یادداشتی برای نمایش نیست");
            root.setViewVisibility(
                    R.id.noforget_widget_empty,
                    items.isEmpty()
                            ? View.VISIBLE
                            : View.GONE);
        }

        boolean showPriority =
                WidgetPrefs.showPriority(
                        context,
                        widgetId);
        boolean showMetadata =
                WidgetPrefs.showMetadata(
                        context,
                        widgetId);

        for (NoForgetItem item : items) {
            RemoteViews row = new RemoteViews(
                    context.getPackageName(),
                    R.layout.widget_noforget_row);

            row.setTextViewText(
                    R.id.noforget_row_title,
                    item.title.trim().isEmpty()
                            ? (item.body.trim().isEmpty()
                            ? "دست‌نویس"
                            : item.body)
                            : item.title);

            String meta = item.hasDue
                    ? CalendarUtils.formatDate(
                    item.dueAtMillis,
                    AppSettings.defaultCalendar(context))
                    : "بدون آلارم";
            if (item.reminderEnabled) {
                meta = "آلارم • " + meta;
            }

            row.setTextViewText(
                    R.id.noforget_row_meta,
                    meta);
            row.setTextViewText(
                    R.id.noforget_row_priority,
                    PriorityUtils.label(item.priority));

            row.setViewVisibility(
                    R.id.noforget_row_meta,
                    showMetadata
                            ? View.VISIBLE
                            : View.GONE);
            row.setViewVisibility(
                    R.id.noforget_row_priority,
                    showPriority
                            ? View.VISIBLE
                            : View.GONE);

            row.setTextColor(
                    R.id.noforget_row_title,
                    text);
            row.setTextColor(
                    R.id.noforget_row_meta,
                    muted);
            row.setTextColor(
                    R.id.noforget_row_priority,
                    item.priority >= PriorityUtils.HIGH
                            ? 0xFFC84D4D
                            : primary);

            row.setTextViewTextSize(
                    R.id.noforget_row_title,
                    TypedValue.COMPLEX_UNIT_SP,
                    sizes[3]);
            row.setTextViewTextSize(
                    R.id.noforget_row_meta,
                    TypedValue.COMPLEX_UNIT_SP,
                    sizes[4]);
            row.setTextViewTextSize(
                    R.id.noforget_row_priority,
                    TypedValue.COMPLEX_UNIT_SP,
                    sizes[5]);

            Intent edit = new Intent(
                    context,
                    NoForgetEditorActivity.class)
                    .putExtra("noteId", item.id);

            row.setOnClickPendingIntent(
                    R.id.noforget_row_root,
                    PendingIntent.getActivity(
                            context,
                            1_500_000
                                    + (int) Math.abs(
                                    item.id % 1_000_000),
                            edit,
                            PendingIntent.FLAG_UPDATE_CURRENT
                                    | PendingIntent.FLAG_IMMUTABLE));

            root.addView(
                    R.id.noforget_widget_list,
                    row);
        }

        root.setOnClickPendingIntent(
                R.id.noforget_widget_add,
                PendingIntent.getActivity(
                        context,
                        1_600_000 + widgetId,
                        new Intent(
                                context,
                                NoForgetQuickAddActivity.class),
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));

        Intent openApp = new Intent(
                context,
                MainActivity.class)
                .putExtra("openTab", "noforget")
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        root.setOnClickPendingIntent(
                R.id.noforget_widget_header,
                PendingIntent.getActivity(
                        context,
                        1_700_000 + widgetId,
                        openApp,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));

        Intent settings = new Intent(
                context,
                WidgetSettingsActivity.class)
                .putExtra(
                        AppWidgetManager.EXTRA_APPWIDGET_ID,
                        widgetId)
                .putExtra("widgetKind", "note")
                .putExtra("editExisting", true);

        root.setOnClickPendingIntent(
                R.id.noforget_widget_theme,
                PendingIntent.getActivity(
                        context,
                        1_800_000 + widgetId,
                        settings,
                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE));

        return root;
    }

    @Override public void onDeleted(
            Context context,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            WidgetPrefs.clear(context, id);
        }
        super.onDeleted(context, appWidgetIds);
    }

    private static void applyTimeFormat(
            RemoteViews root,
            int viewId,
            int mode,
            boolean seconds) {
        String format24 =
                seconds ? "HH:mm:ss" : "HH:mm";
        String format12 =
                seconds ? "hh:mm:ss a" : "hh:mm a";

        if (mode == 1) {
            format12 = format24;
        } else if (mode == 2) {
            format24 = format12;
        }

        root.setCharSequence(
                viewId,
                "setFormat24Hour",
                format24);
        root.setCharSequence(
                viewId,
                "setFormat12Hour",
                format12);
    }

    private static int backgroundResource(
            boolean dark,
            int mode) {
        if (dark) {
            if (mode == 1) {
                return R.drawable.widget_background_dark_85;
            }
            if (mode == 2) {
                return R.drawable.widget_background_dark_70;
            }
            return R.drawable.widget_background_dark;
        }

        if (mode == 1) {
            return R.drawable.widget_background_light_85;
        }
        if (mode == 2) {
            return R.drawable.widget_background_light_70;
        }
        return R.drawable.widget_background;
    }

    private static float[] fontSizes(int mode) {
        if (mode == 0) {
            return new float[]{
                    19f, 10f, 9f,
                    12f, 9f, 9f
            };
        }
        if (mode == 2) {
            return new float[]{
                    26f, 13f, 11f,
                    15f, 12f, 11f
            };
        }
        return new float[]{
                22f, 11f, 10f,
                13f, 10f, 10f
        };
    }

    private static List<NoForgetItem> widgetOrder(
            List<NoForgetItem> source,
            int limit,
            int sortMode) {
        if (limit <= 0 || source.isEmpty()) {
            return new ArrayList<>();
        }

        ArrayList<NoForgetItem> items =
                new ArrayList<>(source);

        if (sortMode == 1) {
            items.sort((a, b) ->
                    Long.compare(
                            b.createdAt,
                            a.createdAt));
        } else if (sortMode == 2) {
            items.sort((a, b) -> {
                int priority =
                        Integer.compare(
                                b.priority,
                                a.priority);
                if (priority != 0) return priority;
                return Long.compare(
                        b.createdAt,
                        a.createdAt);
            });
        } else {
            long now = System.currentTimeMillis();
            NoForgetItem nearest = null;
            long nearestDistance = Long.MAX_VALUE;

            for (NoForgetItem item : items) {
                if (!item.hasDue
                        || item.dueAtMillis <= 0) {
                    continue;
                }

                long distance = Math.abs(
                        item.dueAtMillis - now);
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearest = item;
                }
            }

            if (nearest != null) {
                items.remove(nearest);
            }

            items.sort((a, b) -> {
                int priority =
                        Integer.compare(
                                b.priority,
                                a.priority);
                if (priority != 0) return priority;

                if (a.hasDue && b.hasDue) {
                    return Long.compare(
                            a.dueAtMillis,
                            b.dueAtMillis);
                }

                if (a.hasDue != b.hasDue) {
                    return a.hasDue ? -1 : 1;
                }

                return Long.compare(
                        b.createdAt,
                        a.createdAt);
            });

            if (nearest != null) {
                items.add(0, nearest);
            }
        }

        if (items.size() <= limit) {
            return items;
        }

        return new ArrayList<>(
                items.subList(0, limit));
    }
}
