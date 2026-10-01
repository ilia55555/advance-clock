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

import java.util.List;
import java.util.TimeZone;

public final class CalendarWidgetProvider extends AppWidgetProvider {
    private static final int BASE_WIDTH = 600;
    private static final int PRAYER_ROW_HEIGHT = 106;
    private static final int PRAYER_ROW_GAP = 10;
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

        Bitmap image = render(context);
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

    private static Bitmap render(Context context) {
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

        List<AppSettings.PrayerHorizon> horizons =
                AppSettings.prayerHorizons(context);
        int rows = Math.min(2, horizons.size());
        int totalHeight = calendarHeight
                + (rows == 0 ? 0 : PRAYER_ROW_GAP)
                + rows * (PRAYER_ROW_HEIGHT + PRAYER_ROW_GAP)
                + BOTTOM_PADDING;

        Bitmap bitmap = Bitmap.createBitmap(
                BASE_WIDTH,
                Math.max(1, totalHeight),
                Bitmap.Config.RGB_565);
        bitmap.setDensity(Bitmap.DENSITY_NONE);

        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(AppSettings.surface(context));
        calendar.draw(canvas);

        float y = calendarHeight + PRAYER_ROW_GAP;
        for (int i = 0; i < rows; i++) {
            drawPrayerRow(
                    context,
                    canvas,
                    y,
                    horizons.get(i),
                    System.currentTimeMillis());
            y += PRAYER_ROW_HEIGHT + PRAYER_ROW_GAP;
        }
        return bitmap;
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
