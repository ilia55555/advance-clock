package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Shared direction marker for the compass and prayer background. */
final class QiblaMarker {
    private final Bitmap image;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds = new RectF();

    QiblaMarker(Context context) {
        image = BitmapFactory.decodeResource(context.getResources(), R.drawable.al_kaaba);
    }

    void draw(Canvas canvas, float cx, float cy, float radius, float angle, int color) {
        float height = radius * .32f;
        float width = height * image.getWidth() / image.getHeight();
        float distance = radius * .69f;
        double radians = Math.toRadians(angle - 90);
        float x = cx + (float) Math.cos(radians) * distance;
        float y = cy + (float) Math.sin(radians) * distance;
        paint.setColor(color);
        paint.setStrokeWidth(Math.max(2f, radius * .018f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx, cy, x, y, paint);
        bounds.set(x - width / 2, y - height / 2, x + width / 2, y + height / 2);
        // Keep the artwork upright while its position follows the Qibla bearing.
        canvas.drawBitmap(image, null, bounds, paint);
    }
}
