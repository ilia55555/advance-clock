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
import java.util.List;
import java.util.TimeZone;

public final class CalendarWidgetProvider extends AppWidgetProvider {
    private static final int BASE_WIDTH = 600;
    private static final int PRAYER_ROW_HEIGHT = 106;
    private static final int PRAYER_ROW_GAP = 10;
    private static final int EVENT_GAP = 10;
    private static final int EVENT_HORIZONTAL_PADDING = 22;
    private static final int EVENT_VERTICAL_PADDING = 18;
    private static final int EVENT_TEXT_SIZE = 17;
    private static final int EVENT_LINE_GAP = 9;
    private static final int BOTTOM_PADDING = 12;

    @Override public void onUpdate(
            Context context,
            AppWidgetManager manager,
            int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            update(context, manager, id, manager.getAppWidgetOptions(id));
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
            update(context, manager, id, manager.getAppWidgetOptions(id));
        }
    }

    private static void update(
            Context context,
            AppWidgetManager manager,
            int widgetId,
            Bundle options) {
        RemoteViews views = new RemoteViews(
                context.getPackageName(),
                R.layout.widget_calendar);

        Bitmap image = render(context, widgetId);
        views.setImageViewBitmap(R.id.calendar_widget_image, image);

        Intent open = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(
                context,
                9_100_000 + widgetId,
                open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.calendar_widget_root, openPi);
        views.setOnClickPendingIntent(R.id.calendar_widget_image, openPi);

        manager.updateAppWidget(widgetId, views);
    }

    private static Bitmap render(Context context, int widgetId) {
        TripleCalendarView calendar = new TripleCalendarView(context);
        calendar.setCalendarType(AppSettings.defaultCalendar(context));
        calendar.setSelectedMillis(System.currentTimeMillis());

        int widthSpec = View.MeasureSpec.makeMeasureSpec(
                BASE_WIDTH, View.MeasureSpec.EXACTLY);
        int heightSpec = View.MeasureSpec.makeMeasureSpec(
                0, View.MeasureSpec.UNSPECIFIED);
        calendar.measure(widthSpec, heightSpec);
        int calendarHeight = calendar.getMeasuredHeight();
        calendar.layout(0, 0, BASE_WIDTH, calendarHeight);

        boolean showPrayer =
                CalendarWidgetPrefs.showPrayerTimes(context, widgetId);
        boolean showEvents =
                CalendarWidgetPrefs.showEvents(context, widgetId);

        List<AppSettings.PrayerHorizon> horizons =
                showPrayer
                        ? AppSettings.prayerHorizons(context)
                        : java.util.Collections.emptyList();
        int rows = Math.min(2, horizons.size());

        ArrayList<String> eventLines = showEvents
                ? buildEventLines(context, System.currentTimeMillis())
                : new ArrayList<>();
        int eventHeight = showEvents
                ? eventBoxHeight(eventLines)
                : 0;

        int prayerBlockHeight = rows == 0
                ? 0
                : PRAYER_ROW_GAP
                        + rows * PRAYER_ROW_HEIGHT
                        + Math.max(0, rows - 1) * PRAYER_ROW_GAP;

        int totalHeight = calendarHeight
                + prayerBlockHeight
                + (showEvents ? EVENT_GAP + eventHeight : 0)
                + BOTTOM_PADDING;

        Bitmap bitmap = Bitmap.createBitmap(
                BASE_WIDTH,
                Math.max(1, totalHeight),
                Bitmap.Config.RGB_565);
        bitmap.setDensity(Bitmap.DENSITY_NONE);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(AppSettings.surface(context));
        calendar.draw(canvas);

        float y = calendarHeight;
        if (rows > 0) y += PRAYER_ROW_GAP;

        for (int i = 0; i < rows; i++) {
            drawPrayerRow(
                    context,
                    canvas,
                    y,
                    horizons.get(i),
                    System.currentTimeMillis());
            y += PRAYER_ROW_HEIGHT;
            if (i < rows - 1) y += PRAYER_ROW_GAP;
        }

        if (showEvents) {
            y += EVENT_GAP;
            drawEventBox(context, canvas, y, eventHeight, eventLines);
        }

        return bitmap;
    }

    private static ArrayList<String> buildEventLines(
            Context context,
            long millis) {
        ArrayList<String> raw = new ArrayList<>();
        int primaryType = AppSettings.defaultCalendar(context);

        for (int source = CalendarUtils.PERSIAN;
             source <= CalendarUtils.HIJRI;
             source++) {
            if (!CalendarEventRepository.sourceEnabled(
                    context, primaryType, source)) {
                continue;
            }
            List<CalendarEventRepository.Event> events =
                    CalendarEventRepository.eventsFor(
                            context, millis, source);
            for (CalendarEventRepository.Event event : events) {
                if (event.title != null
                        && !event.title.trim().isEmpty()) {
                    raw.add(event.title.trim());
                }
            }
        }

        if (CalendarEventRepository.isWeekend(millis, primaryType)) {
            raw.add(context.getString(R.string.runtime_text_0391));
        }

        if (raw.isEmpty()) {
            raw.add(context.getString(R.string.runtime_text_0392));
        }

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextSize(EVENT_TEXT_SIZE);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));

        float maxWidth =
                BASE_WIDTH - 2f * (12f + EVENT_HORIZONTAL_PADDING);
        ArrayList<String> wrapped = new ArrayList<>();
        for (String value : raw) {
            wrapText(value, paint, maxWidth, wrapped);
        }
        return wrapped;
    }

    private static void wrapText(
            String text,
            Paint paint,
            float maxWidth,
            ArrayList<String> out) {
        String remaining = text == null ? "" : text.trim();
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

            String line = remaining.substring(0, count).trim();
            if (!line.isEmpty()) out.add(line);
            remaining = remaining.substring(
                    Math.min(count, remaining.length())).trim();
        }
    }

    private static int eventBoxHeight(List<String> lines) {
        int count = Math.max(1, lines.size());
        return EVENT_VERTICAL_PADDING * 2
                + count * (EVENT_TEXT_SIZE + EVENT_LINE_GAP);
    }

    private static void drawEventBox(
            Context context,
            Canvas canvas,
            float top,
            int height,
            List<String> lines) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        int surface = AppSettings.surface(context);
        int text = AppSettings.textPrimary(context);
        int border = AppSettings.secondaryColor(context);

        RectF card = new RectF(
                5f,
                top,
                BASE_WIDTH - 5f,
                top + height);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(surface);
        canvas.drawRoundRect(card, 28f, 28f, paint);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.5f);
        stroke.setColor(withAlpha(border, 0x28));
        canvas.drawRoundRect(card, 28f, 28f, stroke);

        boolean rtl = AppSettings.isRtlLanguage(context);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        paint.setTextSize(EVENT_TEXT_SIZE);
        paint.setColor(text);
        paint.setTextAlign(rtl ? Paint.Align.RIGHT : Paint.Align.LEFT);

        float x = rtl
                ? BASE_WIDTH - 12f - EVENT_HORIZONTAL_PADDING
                : 12f + EVENT_HORIZONTAL_PADDING;
        float y = top + EVENT_VERTICAL_PADDING + EVENT_TEXT_SIZE;

        for (String line : lines) {
            canvas.drawText(line, x, y, paint);
            y += EVENT_TEXT_SIZE + EVENT_LINE_GAP;
        }
    }

    private static void drawPrayerRow(
            Context context,
            Canvas canvas,
            float top,
            AppSettings.PrayerHorizon horizon,
            long millis) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);

        int surface = AppSettings.surface(context);
        int primary = AppSettings.primaryColor(context);
        int text = AppSettings.textPrimary(context);
        int muted = AppSettings.textSecondary(context);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(surface);
        RectF card = new RectF(
                5f,
                top,
                BASE_WIDTH - 5f,
                top + PRAYER_ROW_HEIGHT);
        canvas.drawRoundRect(card, 28f, 28f, paint);

        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(1.5f);
        stroke.setColor(withAlpha(primary, 0x2F));
        canvas.drawRoundRect(card, 28f, 28f, stroke);

        String city = shortHorizonLabel(
                context,
                horizon.label);

        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextSize(21f);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(text);
        drawVerticallyCentered(
                canvas,
                paint,
                city,
                BASE_WIDTH - 31f,
                top + PRAYER_ROW_HEIGHT / 2f);

        PrayerTimeCalculator.Times times =
                PrayerTimeCalculator.calculate(
                        millis,
                        horizon.latitude,
                        horizon.longitude,
                        TimeZone.getTimeZone(horizon.timeZoneId));

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
        float[] centers = {430f, 330f, 230f, 130f};

        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < labels.length; i++) {
            paint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
            paint.setTextSize(14f);
            paint.setColor(muted);
            drawVerticallyCentered(
                    canvas,
                    paint,
                    labels[i],
                    centers[i],
                    top + 37f);

            paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            paint.setTextSize(19f);
            paint.setColor(primary);
            drawVerticallyCentered(
                    canvas,
                    paint,
                    values[i],
                    centers[i],
                    top + 69f);
        }

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        paint.setTextSize(30f);
        paint.setColor(primary);
        drawVerticallyCentered(
                canvas,
                paint,
                "<",
                23f,
                top + PRAYER_ROW_HEIGHT / 2f);
    }

    private static String shortHorizonLabel(
            Context context,
            String label) {
        if (label == null || label.trim().isEmpty()) {
            return context.getString(R.string.runtime_text_0418);
        }
        String value = label.trim();
        int comma = value.indexOf('،');
        if (comma < 0) comma = value.indexOf(',');
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
        Paint.FontMetrics metrics = paint.getFontMetrics();
        float baseline = cy - (metrics.ascent + metrics.descent) / 2f;
        canvas.drawText(value == null ? "" : value, x, baseline, paint);
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }
}
