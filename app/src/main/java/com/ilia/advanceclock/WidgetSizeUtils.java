package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.SizeF;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class WidgetSizeUtils {
    public interface RemoteViewsFactory {
        RemoteViews create(float widthDp, float heightDp);
    }

    public static final class WidgetSize {
        public final float widthDp;
        public final float heightDp;
        public final boolean exact;

        WidgetSize(float widthDp, float heightDp, boolean exact) {
            this.widthDp = widthDp;
            this.heightDp = heightDp;
            this.exact = exact;
        }
    }

    private WidgetSizeUtils() {}

    @SuppressWarnings("deprecation")
    public static ArrayList<SizeF> exactSizes(Bundle options) {
        if (Build.VERSION.SDK_INT < 31 || options == null) {
            return new ArrayList<>();
        }

        try {
            ArrayList<SizeF> sizes =
                    options.getParcelableArrayList(
                            AppWidgetManager.OPTION_APPWIDGET_SIZES);
            return sizes == null ? new ArrayList<>() : sizes;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    public static void updateResponsive(
            Context context,
            AppWidgetManager manager,
            int widgetId,
            Bundle options,
            RemoteViewsFactory factory) {
        if (Build.VERSION.SDK_INT >= 31) {
            ArrayList<SizeF> sizes = exactSizes(options);
            if (!sizes.isEmpty()) {
                Map<SizeF, RemoteViews> views = new LinkedHashMap<>();
                for (SizeF size : sizes) {
                    if (size == null
                            || size.getWidth() <= 0f
                            || size.getHeight() <= 0f) {
                        continue;
                    }
                    views.put(
                            size,
                            factory.create(
                                    size.getWidth(),
                                    size.getHeight()));
                }

                if (!views.isEmpty()) {
                    manager.updateAppWidget(
                            widgetId,
                            new RemoteViews(views));
                    return;
                }
            }
        }

        WidgetSize size = currentSize(
                context,
                options,
                WidgetPrefs.widthCells(context, widgetId),
                WidgetPrefs.heightCells(context, widgetId));

        manager.updateAppWidget(
                widgetId,
                factory.create(size.widthDp, size.heightDp));
    }

    public static WidgetSize currentSize(
            Context context,
            Bundle options,
            int fallbackWidthCells,
            int fallbackHeightCells) {
        ArrayList<SizeF> sizes = exactSizes(options);
        if (!sizes.isEmpty()) {
            boolean landscape = context.getResources()
                    .getConfiguration()
                    .orientation == Configuration.ORIENTATION_LANDSCAPE;

            SizeF best = null;
            float bestScore = Float.MAX_VALUE;

            for (SizeF size : sizes) {
                if (size == null
                        || size.getWidth() <= 0f
                        || size.getHeight() <= 0f) {
                    continue;
                }

                float ratio = size.getWidth() / size.getHeight();
                float score;
                if (landscape) {
                    score = ratio >= 1f
                            ? Math.abs(ratio - 1.8f)
                            : 100f + Math.abs(ratio - 1.8f);
                } else {
                    score = ratio <= 2.5f
                            ? Math.abs(ratio - 1.2f)
                            : 100f + Math.abs(ratio - 1.2f);
                }

                if (score < bestScore) {
                    bestScore = score;
                    best = size;
                }
            }

            if (best == null) best = sizes.get(0);
            return new WidgetSize(
                    best.getWidth(),
                    best.getHeight(),
                    true);
        }

        float fallbackWidth = Math.max(
                40f,
                fallbackWidthCells * 70f - 30f);
        float fallbackHeight = Math.max(
                40f,
                fallbackHeightCells * 70f - 30f);

        if (options == null) {
            return new WidgetSize(
                    fallbackWidth,
                    fallbackHeight,
                    false);
        }

        int minWidth = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                0);
        int minHeight = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                0);
        int maxWidth = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                minWidth);
        int maxHeight = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                minHeight);

        if (minWidth <= 0 || minHeight <= 0) {
            return new WidgetSize(
                    fallbackWidth,
                    fallbackHeight,
                    false);
        }

        boolean landscape = context.getResources()
                .getConfiguration()
                .orientation == Configuration.ORIENTATION_LANDSCAPE;

        float width = landscape && maxWidth > 0
                ? maxWidth
                : minWidth;
        float height = landscape
                ? minHeight
                : (maxHeight > 0 ? maxHeight : minHeight);

        return new WidgetSize(width, height, false);
    }

    public static int dpToCells(float dp) {
        return Math.max(
                1,
                Math.round((dp + 30f) / 70f));
    }

    public static String describe(
            Context context,
            Bundle options,
            int fallbackWidthCells,
            int fallbackHeightCells) {
        ArrayList<SizeF> sizes = exactSizes(options);
        if (!sizes.isEmpty()) {
            StringBuilder builder = new StringBuilder(
                    "اندازه‌های دقیق گزارش‌شده توسط لانچر:");
            int shown = 0;
            for (SizeF size : sizes) {
                if (size == null
                        || size.getWidth() <= 0f
                        || size.getHeight() <= 0f) {
                    continue;
                }
                builder.append("\n")
                        .append(Math.round(size.getWidth()))
                        .append("×")
                        .append(Math.round(size.getHeight()))
                        .append("dp  ≈  ")
                        .append(dpToCells(size.getWidth()))
                        .append(" × ")
                        .append(dpToCells(size.getHeight()))
                        .append(" خانه");
                shown++;
                if (shown >= 4) break;
            }
            if (shown > 0) return builder.toString();
        }

        WidgetSize size = currentSize(
                context,
                options,
                fallbackWidthCells,
                fallbackHeightCells);

        return "اندازه برآوردشده از لانچر: "
                + Math.round(size.widthDp)
                + "×"
                + Math.round(size.heightDp)
                + "dp"
                + "\nتقریب شبکه: "
                + dpToCells(size.widthDp)
                + " × "
                + dpToCells(size.heightDp)
                + " خانه";
    }
}
