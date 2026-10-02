package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

public final class QiblaAdhanBackgroundView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final String[] directionLabels;

    private float azimuth;
    private float qiblaBearing;
    private boolean hasQiblaBearing;

    public QiblaAdhanBackgroundView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        directionLabels = new String[]{
                AppString.get(R.string.runtime_text_0457),
                AppString.get(R.string.runtime_text_0336),
                AppString.get(R.string.runtime_text_0458),
                AppString.get(R.string.runtime_text_0337),
                AppString.get(R.string.runtime_text_0459),
                AppString.get(R.string.runtime_text_0338),
                AppString.get(R.string.runtime_text_0460),
                AppString.get(R.string.runtime_text_0339)
        };
        android.location.Location location =
                QiblaUtils.bestKnownLocation(context);
        if (location != null) {
            setLocation(
                    location.getLatitude(),
                    location.getLongitude());
        }
    }

    public void setLocation(
            double latitude,
            double longitude) {
        qiblaBearing =
                (float) QiblaUtils.bearing(
                        latitude,
                        longitude);
        hasQiblaBearing = true;
        invalidate();
    }

    public boolean hasQiblaBearing() {
        return hasQiblaBearing;
    }

    public void setAzimuth(float value) {
        float normalized =
                (value % 360f + 360f) % 360f;
        float difference =
                signedAngle(normalized - azimuth);
        azimuth =
                (azimuth
                        + difference * 0.18f
                        + 360f) % 360f;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f;
        float cy = getHeight() * 0.48f;
        float radius =
                Math.min(
                        getWidth() * 0.47f,
                        getHeight() * 0.36f);
        if (radius <= 0f) return;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0D3146);
        paint.setAlpha(205);
        canvas.drawCircle(
                cx,
                cy,
                radius + dp(16),
                paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(dp(2));
        paint.setColor(0xFF8ED8D0);
        paint.setAlpha(150);
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.drawCircle(
                cx,
                cy,
                radius * 0.72f,
                paint);

        canvas.save();
        canvas.rotate(-azimuth, cx, cy);

        for (int degree = 0;
             degree < 360;
             degree += 5) {
            boolean major = degree % 45 == 0;
            boolean medium = degree % 15 == 0;

            paint.setStrokeWidth(
                    dp(major ? 3 : medium ? 2 : 1));
            paint.setColor(
                    major
                            ? 0xFFE6FBF8
                            : 0xFF8ED8D0);
            paint.setAlpha(
                    major
                            ? 125
                            : medium
                            ? 80
                            : 45);

            float inner =
                    radius
                            - dp(
                            major
                                    ? 28
                                    : medium
                                    ? 18
                                    : 10);
            double radians =
                    Math.toRadians(
                            degree - 90);

            canvas.drawLine(
                    cx
                            + (float) Math.cos(radians)
                            * inner,
                    cy
                            + (float) Math.sin(radians)
                            * inner,
                    cx
                            + (float) Math.cos(radians)
                            * (radius - dp(6)),
                    cy
                            + (float) Math.sin(radians)
                            * (radius - dp(6)),
                    paint);
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setFakeBoldText(true);
        paint.setTextSize(
                Math.max(
                        dp(12),
                        radius * 0.09f));
        paint.setAlpha(125);

        for (int index = 0;
             index < directionLabels.length;
             index++) {
            double radians =
                    Math.toRadians(
                            index * 45 - 90);
            paint.setColor(
                    index == 0
                            ? 0xFFFF8C7A
                            : 0xFFD7F3EF);
            canvas.drawText(
                    directionLabels[index],
                    cx
                            + (float) Math.cos(radians)
                            * radius * 0.79f,
                    cy
                            + (float) Math.sin(radians)
                            * radius * 0.79f
                            + paint.getTextSize()
                            * 0.35f,
                    paint);
        }

        canvas.restore();

        if (hasQiblaBearing) {
            drawQiblaNeedle(
                    canvas,
                    cx,
                    cy,
                    radius,
                    signedAngle(
                            qiblaBearing - azimuth));
        }

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFECC75A);
        paint.setAlpha(180);
        canvas.drawCircle(
                cx,
                cy,
                dp(7),
                paint);

        paint.setAlpha(255);
        paint.setFakeBoldText(false);
    }

    private void drawQiblaNeedle(
            Canvas canvas,
            float cx,
            float cy,
            float radius,
            float relativeAngle) {
        canvas.save();
        canvas.rotate(
                relativeAngle,
                cx,
                cy);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFECC75A);
        paint.setAlpha(190);

        path.reset();
        path.moveTo(
                cx,
                cy - radius * 0.69f);
        path.lineTo(
                cx - dp(12),
                cy - radius * 0.48f);
        path.lineTo(
                cx - dp(4),
                cy - radius * 0.51f);
        path.lineTo(
                cx - dp(4),
                cy + radius * 0.20f);
        path.lineTo(
                cx + dp(4),
                cy + radius * 0.20f);
        path.lineTo(
                cx + dp(4),
                cy - radius * 0.51f);
        path.lineTo(
                cx + dp(12),
                cy - radius * 0.48f);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(0xFF12110E);
        paint.setAlpha(210);
        RectF kaaba =
                new RectF(
                        cx - dp(14),
                        cy - radius * 0.80f,
                        cx + dp(14),
                        cy - radius * 0.65f);
        canvas.drawRoundRect(
                kaaba,
                dp(2),
                dp(2),
                paint);

        paint.setColor(0xFFE3BE4E);
        paint.setAlpha(230);
        canvas.drawRect(
                kaaba.left,
                kaaba.top + dp(5),
                kaaba.right,
                kaaba.top + dp(8),
                paint);

        canvas.restore();
        paint.setAlpha(255);
    }

    private float signedAngle(float angle) {
        return (angle + 540f) % 360f - 180f;
    }

    private int dp(int value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density);
    }
}
