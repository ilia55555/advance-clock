package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

/** Shared direction marker for the compass and prayer background. */
final class QiblaMarker {
    static final int GOLD = 0xFFECC75A;
    private final Bitmap image;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF bounds = new RectF();

    QiblaMarker(Context context) {
        // Decode only a marker-sized bitmap, even when the owner supplies a large photo.
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), R.drawable.al_kaaba, options);
        int target = Math.min(512, Math.max(96, Math.round(
                96 * context.getResources().getDisplayMetrics().density)));
        options.inSampleSize = 1;
        while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > target * 2) {
            options.inSampleSize *= 2;
        }
        options.inJustDecodeBounds = false;
        options.inScaled = false;
        image = BitmapFactory.decodeResource(context.getResources(), R.drawable.al_kaaba, options);
    }

    void bindTo(android.widget.ImageView view) {
        view.setImageBitmap(image);
    }

    void draw(Canvas canvas, float cx, float cy, float radius, float angle) {
        if (radius <= 0f) return;
        float distance = radius * .69f;
        double radians = Math.toRadians(angle - 90);
        float x = cx + (float) Math.cos(radians) * distance;
        float y = cy + (float) Math.sin(radians) * distance;
        paint.setColor(GOLD);
        paint.setStrokeWidth(Math.max(2f, radius * .018f));
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(cx, cy, x, y, paint);
        drawImage(canvas, x, y, radius);
    }

    void drawPending(Canvas canvas, float cx, float cy, float radius) {
        drawImage(canvas, cx, cy, radius);
    }

    private void drawImage(Canvas canvas, float x, float y, float radius) {
        if (image == null || radius <= 0f) return;
        // Fit both wide and tall replacements without distortion or leaving the dial.
        float scale = radius * .32f / Math.max(image.getWidth(), image.getHeight());
        float width = image.getWidth() * scale;
        float height = image.getHeight() * scale;
        bounds.set(x - width / 2, y - height / 2, x + width / 2, y + height / 2);
        // Keep the artwork upright while its position follows the Qibla bearing.
        canvas.drawBitmap(image, null, bounds, paint);
    }
}
