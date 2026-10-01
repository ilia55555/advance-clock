package com.ilia.advanceclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

public final class CalendarWidgetProvider extends AppWidgetProvider {
    private static final int BASE_WIDTH = 600;

    // These values intentionally mirror activity_main.xml.
    private static final int PRAYER_ROW_HEIGHT = 72;
    private static final int PRAYER_FIRST_GAP = 8;
    private static final int PRAYER_ADDITIONAL_GAP = 6;
    private static final int EVENT_GAP = 8;
    private static final int CARD_RADIUS = 22;
    private static final int EVENT_PADDING = 14;
    private static final int EVENT_TEXT_SIZE = 14;
    private static final int EVENT_LINE_GAP = 6;
    private static final int BOTTOM_PADDING = 8;

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
            CalendarWidgetPrefs.migrate(
                    context, oldWidgetIds[i], newWidgetIds[i]);
        }
        updateAll(context);
    }

    @Override public void onDeleted(
            Context context,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            CalendarWidgetPrefs.clear(context, id);
        }
        super.onDeleted(context, appWidgetIds);
    }

    public static void updateAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(
                new ComponentName(context, CalendarWidgetProvider.class));
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
    }

    private static RemoteViews createRemoteViews(
            Context context,
            int widgetId,
            float widthDp,
            float heightDp) {
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_calendar);

        Bitmap image = render(
                context,
                widgetId,
                widthDp,
                heightDp);
        views.setImageViewBitmap(
                R.id.calendar_widget_image,
                image);

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(
                context,
                9_100_000 + widgetId,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT
                        | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(
                R.id.calendar_widget_root,
                openPi);
        views.setOnClickPendingIntent(
                R.id.calendar_widget_image,
                openPi);

        return views;
    }

    private static Bitmap render(
            Context context,
            int widgetId,
            float widthDp,
            float heightDp) {
        Bitmap natural = renderNatural(context, widgetId);

        float safeWidth = Math.max(1f, widthDp);
        float safeHeight = Math.max(1f, heightDp);
        float aspect = safeHeight / safeWidth;

        int outWidth = 600;
        int outHeight = Math.max(
                80,
                Math.round(outWidth * aspect));

        if (outHeight > 1800) {
            outHeight = 1800;
            outWidth = Math.max(
                    80,
                    Math.round(outHeight / aspect));
        }

        Bitmap output = Bitmap.createBitmap(
                Math.max(1, outWidth),
                Math.max(1, outHeight),
                Bitmap.Config.RGB_565);
        output.setDensity(Bitmap.DENSITY_NONE);

        Canvas canvas = new Canvas(output);
        canvas.drawColor(AppSettings.background(context));

        float scale = Math.min(
                outWidth / (float) natural.getWidth(),
                outHeight / (float) natural.getHeight());

        float drawnWidth = natural.getWidth() * scale;
        float drawnHeight = natural.getHeight() * scale;
        float left = (outWidth - drawnWidth) / 2f;
        float top = 0f;

        RectF destination = new RectF(
                left,
                top,
                left + drawnWidth,
                top + drawnHeight);

        Paint bitmapPaint = new Paint(
                Paint.ANTI_ALIAS_FLAG
                        | Paint.FILTER_BITMAP_FLAG);
        canvas.drawBitmap(
                natural,
                null,
                destination,
                bitmapPaint);

        natural.recycle();
        return output;
    }

    private static Bitmap renderNatural(
            Context context,
            int widgetId) {
        TripleCalendarView calendar = new TripleCalendarView(context);
        calendar.setCalendarType(
                AppSettings.defaultCalendar(context));
        calendar.setSelectedMillis(
                System.currentTimeMillis());

        int widthSpec = View.MeasureSpec.makeMeasureSpec(
                BASE_WIDTH,
                View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(
                0,
                View.MeasureSpec.UNSPECIFIED);
        calendar.measure(widthSpec, heightSpec);
        int calendarHeight = calendar.getMeasuredHeight();
        calendar.layout(
                0,
                0,
                BASE_WIDTH,
                calendarHeight);

        boolean showPrayer =
                CalendarWidgetPrefs.showPrayerTimes(
                        context, widgetId);
        boolean showEvents =
                CalendarWidgetPrefs.showEvents(
                        context, widgetId);

        List<AppSettings.PrayerHorizon> horizons =
                showPrayer
                        ? orderedHorizons(context)
                        : Collections.emptyList();

        ArrayList<String> eventLines = showEvents
                ? buildEventLines(
                        context,
                        System.currentTimeMillis())
                : new ArrayList<>();

        int prayerHeight = 0;
        if (!horizons.isEmpty()) {
            prayerHeight = PRAYER_FIRST_GAP
                    + horizons.size() * PRAYER_ROW_HEIGHT
                    + Math.max(
                            0,
                            horizons.size() - 1)
                    * PRAYER_ADDITIONAL_GAP;
        }

        int eventHeight = showEvents
                ? eventBoxHeight(eventLines)
                : 0;

        int totalHeight = calendarHeight
                + prayerHeight
                + (showEvents ? EVENT_GAP + eventHeight : 0)
                + BOTTOM_PADDING;

        Bitmap bitmap = Bitmap.createBitmap(
                BASE_WIDTH,
                Math.max(1, totalHeight),
                Bitmap.Config.RGB_565);
        bitmap.setDensity(Bitmap.DENSITY_NONE);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(AppSettings.background(context));
        calendar.draw(canvas);

        float y = calendarHeight;

        if (!horizons.isEmpty()) {
            y += PRAYER_FIRST_GAP;
            boolean multiple = horizons.size() > 1;
            for (int i = 0; i < horizons.size(); i++) {
                drawPrayerRow(
                        context,
                        canvas,
                        y,
                        horizons.get(i),
                        System.currentTimeMillis(),
                        i == 0,
                        multiple);
                y += PRAYER_ROW_HEIGHT;
                if (i < horizons.size() - 1) {
                    y += PRAYER_ADDITIONAL_GAP;
                }
            }
        }

        if (showEvents) {
            y += EVENT_GAP;
            drawEventBox(
                    context,
                    canvas,
                    y,
                    eventHeight,
                    eventLines);
        }

        return bitmap;
    }

    private static List<AppSettings.PrayerHorizon> orderedHorizons(
            Context context) {
        List<AppSettings.PrayerHorizon> source =
                AppSettings.prayerHorizons(context);
        if (source.isEmpty()) return source;

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

        if (primary != null) ordered.add(primary);

        for (AppSettings.PrayerHorizon item : source) {
            if (primary != null
                    && Math.abs(item.latitude - primary.latitude) < 0.0001
                    && Math.abs(item.longitude - primary.longitude) < 0.0001) {
                continue;
            }
            ordered.add(item);
        }

        return ordered;
    }

    private static ArrayList<String> buildEventLines(
            Context context,
            long millis) {
        ArrayList<String> raw = new ArrayList<>();
        int primaryType =
                AppSettings.defaultCalendar(context);

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
                    raw.add(event.title.trim());
                }
            }
        }

        if (CalendarEventRepository.isWeekend(
                millis,
                primaryType)) {
            raw.add(context.getString(
                    R.string.runtime_text_0391));
        }

        if (raw.isEmpty()) {
            raw.add(context.getString(
                    R.string.runtime_text_0392));
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(EVENT_TEXT_SIZE);
        paint.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL));

        float maxWidth = BASE_WIDTH
                - 2f * EVENT_PADDING;

        ArrayList<String> wrapped = new ArrayList<>();
        for (String value : raw) {
            wrapText(
                    value,
                    paint,
                    maxWidth,
                    wrapped);
        }
        return wrapped;
    }

    private static void wrapText(
            String text,
            Paint paint,
            float maxWidth,
            ArrayList<String> out) {
        String remaining =
                text == null ? "" : text.trim();
        if (remaining.isEmpty()) return;

        while (!remaining.isEmpty()) {
            int count = paint.breakText(
                    remaining,
                    true,
                    maxWidth,
                    null);
            if (count <= 0) break;

            if (count < remaining.length()) {
                int cut = remaining.lastIndexOf(' ', count);
                if (cut > 0) count = cut;
            }

            String line = remaining
                    .substring(0, count)
                    .trim();
            if (!line.isEmpty()) out.add(line);

            remaining = remaining.substring(
                    Math.min(
                            count,
                            remaining.length()))
                    .trim();
        }
    }

    private static int eventBoxHeight(
            List<String> lines) {
        int count = Math.max(1, lines.size());
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(EVENT_TEXT_SIZE);
        Paint.FontMetrics metrics = paint.getFontMetrics();
        int lineHeight = Math.max(
                EVENT_TEXT_SIZE,
                Math.round(metrics.descent - metrics.ascent));
        return EVENT_PADDING * 2
                + count * lineHeight
                + Math.max(0, count - 1) * EVENT_LINE_GAP;
    }

    private static void drawEventBox(
            Context context,
            Canvas canvas,
            float top,
            int height,
            List<String> lines) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        RectF card = new RectF(
                0f,
                top,
                BASE_WIDTH,
                top + height);

        fill.setStyle(Paint.Style.FILL);
        fill.setColor(AppSettings.surface(context));
        canvas.drawRoundRect(
                card,
                CARD_RADIUS,
                CARD_RADIUS,
                fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1f);
        stroke.setColor(borderColor(context));
        canvas.drawRoundRect(
                new RectF(
                        0.5f,
                        top + 0.5f,
                        BASE_WIDTH - 0.5f,
                        top + height - 0.5f),
                CARD_RADIUS,
                CARD_RADIUS,
                stroke);

        boolean rtl = AppSettings.isRtlLanguage(context);

        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL));
        textPaint.setTextSize(EVENT_TEXT_SIZE);
        textPaint.setColor(
                AppSettings.textPrimary(context));
        textPaint.setTextAlign(
                rtl
                        ? Paint.Align.RIGHT
                        : Paint.Align.LEFT);

        float x = rtl
                ? BASE_WIDTH - EVENT_PADDING
                : EVENT_PADDING;
        Paint.FontMetrics metrics =
                textPaint.getFontMetrics();
        float lineHeight =
                metrics.descent - metrics.ascent;
        float baseline =
                top + EVENT_PADDING - metrics.ascent;

        for (String line : lines) {
            canvas.drawText(
                    line,
                    x,
                    baseline,
                    textPaint);
            baseline += lineHeight + EVENT_LINE_GAP;
        }
    }

    private static void drawPrayerRow(
            Context context,
            Canvas canvas,
            float top,
            AppSettings.PrayerHorizon horizon,
            long millis,
            boolean primaryRow,
            boolean multipleHorizons) {
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        RectF card = new RectF(
                0f,
                top,
                BASE_WIDTH,
                top + PRAYER_ROW_HEIGHT);

        fill.setStyle(Paint.Style.FILL);
        fill.setColor(AppSettings.surface(context));
        canvas.drawRoundRect(
                card,
                CARD_RADIUS,
                CARD_RADIUS,
                fill);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1f);
        stroke.setColor(borderColor(context));
        canvas.drawRoundRect(
                new RectF(
                        0.5f,
                        top + 0.5f,
                        BASE_WIDTH - 0.5f,
                        top + PRAYER_ROW_HEIGHT - 0.5f),
                CARD_RADIUS,
                CARD_RADIUS,
                stroke);

        PrayerTimeCalculator.Times times =
                PrayerTimeCalculator.calculate(
                        millis,
                        horizon.latitude,
                        horizon.longitude,
                        TimeZone.getTimeZone(
                                horizon.timeZoneId));

        boolean showCity =
                !primaryRow || multipleHorizons;

        if (showCity) {
            Paint cityPaint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);
            cityPaint.setTypeface(
                    Typeface.create(
                            "sans-serif",
                            Typeface.BOLD));
            cityPaint.setTextSize(15f);
            cityPaint.setTextAlign(Paint.Align.CENTER);
            cityPaint.setColor(
                    AppSettings.textPrimary(context));
            drawVerticallyCentered(
                    canvas,
                    cityPaint,
                    shortHorizonLabel(
                            context,
                            horizon.label),
                    BASE_WIDTH - 44f,
                    top + PRAYER_ROW_HEIGHT / 2f);
        }

        String[] labels = {
                context.getString(R.string.runtime_text_0078),
                context.getString(R.string.runtime_text_0083),
                context.getString(R.string.runtime_text_0079),
                context.getString(R.string.adhan_asr_name)
        };
        String[] values = {
                CalendarUtils.fa(times.fajr()),
                CalendarUtils.fa(times.sunrise()),
                CalendarUtils.fa(times.dhuhr()),
                CalendarUtils.fa(times.asr())
        };

        float start = showCity ? 484f : 556f;
        float[] centers = {
                start,
                start - 72f,
                start - 144f,
                start - 216f
        };

        Paint labelPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.NORMAL));
        labelPaint.setTextSize(10f);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setColor(
                AppSettings.textSecondary(context));

        Paint timePaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);
        timePaint.setTypeface(
                Typeface.create(
                        "sans-serif",
                        Typeface.BOLD));
        timePaint.setTextSize(15f);
        timePaint.setTextAlign(Paint.Align.CENTER);
        timePaint.setColor(
                AppSettings.primaryColor(context));

        for (int i = 0; i < labels.length; i++) {
            drawVerticallyCentered(
                    canvas,
                    labelPaint,
                    labels[i],
                    centers[i],
                    top + 25f);
            drawVerticallyCentered(
                    canvas,
                    timePaint,
                    values[i],
                    centers[i],
                    top + 49f);
        }

        if (showCity) {
            Paint arrowPaint =
                    new Paint(Paint.ANTI_ALIAS_FLAG);
            arrowPaint.setTypeface(
                    Typeface.create(
                            "sans-serif",
                            Typeface.BOLD));
            arrowPaint.setTextSize(22f);
            arrowPaint.setTextAlign(Paint.Align.CENTER);
            arrowPaint.setColor(
                    AppSettings.primaryColor(context));
            drawVerticallyCentered(
                    canvas,
                    arrowPaint,
                    "<",
                    12f,
                    top + PRAYER_ROW_HEIGHT / 2f);
        }
    }

    private static String shortHorizonLabel(
            Context context,
            String label) {
        if (label == null
                || label.trim().isEmpty()) {
            return context.getString(
                    R.string.runtime_text_0418);
        }

        String value = label.trim();
        int comma = value.indexOf('،');
        if (comma < 0) {
            comma = value.indexOf(',');
        }

        return comma > 0
                ? value.substring(0, comma).trim()
                : value;
    }

    private static void drawVerticallyCentered(
            Canvas canvas,
            Paint paint,
            String value,
            float x,
            float cy) {
        Paint.FontMetrics metrics =
                paint.getFontMetrics();
        float baseline = cy
                - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(
                value == null ? "" : value,
                x,
                baseline,
                paint);
    }

    private static int borderColor(Context context) {
        return AppSettings.themeMode(context)
                == AppSettings.THEME_DARK
                ? 0xFF343D3A
                : 0xFFDCE8E6;
    }
}
