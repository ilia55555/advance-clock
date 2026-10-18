package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import java.io.InputStream;

/** A movable, zoomable center crop with no stretching. Also used for alarm backgrounds. */
final class AlarmPhotoView extends View {
    private Bitmap bitmap;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private float zoom = 1, offsetX, offsetY, lastX, lastY;
    private final boolean editable;
    private final ScaleGestureDetector detector;
    private int generation;

    AlarmPhotoView(Context context, boolean editable) {
        super(context); this.editable = editable;
        detector = new ScaleGestureDetector(context, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScale(ScaleGestureDetector d) {
                zoom = Math.max(1, Math.min(5, zoom * d.getScaleFactor())); clampOffsets(); invalidate(); return true;
            }
        });
    }
    void load(String value) {
        int request = ++generation;
        zoom = 1; offsetX = offsetY = 0; bitmap = null; invalidate();
        new Thread(() -> {
            Bitmap decoded = null;
            try {
                Uri uri = Uri.parse(value);
                BitmapFactory.Options options = new BitmapFactory.Options(); options.inJustDecodeBounds = true;
                try (InputStream in = getContext().getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(in, null, options); }
                options.inSampleSize = 1;
                while (Math.max(options.outWidth, options.outHeight) / options.inSampleSize > 2048) options.inSampleSize *= 2;
                options.inJustDecodeBounds = false;
                try (InputStream in = getContext().getContentResolver().openInputStream(uri)) { decoded = BitmapFactory.decodeStream(in, null, options); }
                if (decoded != null) {
                    int orientation;
                    try (InputStream in = getContext().getContentResolver().openInputStream(uri)) {
                        orientation = new android.media.ExifInterface(in).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1);
                    }
                    Matrix rotation = new Matrix();
                    switch (orientation) {
                        case 2: rotation.setScale(-1, 1); break;
                        case 3: rotation.setRotate(180); break;
                        case 4: rotation.setScale(1, -1); break;
                        case 5: rotation.setRotate(90); rotation.postScale(-1, 1); break;
                        case 6: rotation.setRotate(90); break;
                        case 7: rotation.setRotate(270); rotation.postScale(-1, 1); break;
                        case 8: rotation.setRotate(270); break;
                    }
                    if (!rotation.isIdentity()) decoded = Bitmap.createBitmap(decoded, 0, 0, decoded.getWidth(), decoded.getHeight(), rotation, true);
                }
            } catch (Exception ignored) {} catch (OutOfMemoryError ignored) {}
            Bitmap result = decoded;
            post(() -> { if (generation == request) { bitmap = result; invalidate(); } });
        }, "alarm-photo").start();
    }
    private void drawPhoto(Canvas canvas, int width, int height) {
        if (bitmap == null) return;
        float scale = (float) AlarmPhotoLayout.fillScale(bitmap.getWidth(), bitmap.getHeight(), width, height) * zoom;
        float excessX = Math.max(0, (bitmap.getWidth() * scale - width) / 2);
        float excessY = Math.max(0, (bitmap.getHeight() * scale - height) / 2);
        float x = Math.max(-excessX, Math.min(excessX, offsetX * width));
        float y = Math.max(-excessY, Math.min(excessY, offsetY * height));
        Matrix matrix = new Matrix(); matrix.setScale(scale, scale);
        matrix.postTranslate((width - bitmap.getWidth() * scale) / 2 + x, (height - bitmap.getHeight() * scale) / 2 + y);
        canvas.drawBitmap(bitmap, matrix, paint);
    }
    @Override protected void onDraw(Canvas canvas) { super.onDraw(canvas); drawPhoto(canvas, getWidth(), getHeight()); }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if (!editable) return false;
        detector.onTouchEvent(event);
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) { lastX = event.getX(); lastY = event.getY(); }
        else if (event.getActionMasked() == MotionEvent.ACTION_MOVE && !detector.isInProgress()) {
            offsetX += (event.getX() - lastX) / Math.max(1, getWidth());
            offsetY += (event.getY() - lastY) / Math.max(1, getHeight());
            clampOffsets(); invalidate();
        }
        lastX = event.getX(); lastY = event.getY(); return true;
    }
    float[] framing() { return new float[]{zoom, offsetX, offsetY}; }
    void restoreFraming(float[] frame) {
        if (frame == null || frame.length != 3) return;
        zoom = Math.max(1, Math.min(5, frame[0])); offsetX = frame[1]; offsetY = frame[2]; invalidate();
    }
    private void clampOffsets() {
        if (bitmap == null || getWidth() <= 0 || getHeight() <= 0) return;
        double scale = AlarmPhotoLayout.fillScale(bitmap.getWidth(), bitmap.getHeight(), getWidth(), getHeight()) * zoom;
        offsetX = (float) AlarmPhotoLayout.clampOffset(offsetX, bitmap.getWidth(), getWidth(), scale);
        offsetY = (float) AlarmPhotoLayout.clampOffset(offsetY, bitmap.getHeight(), getHeight(), scale);
    }
    @Override protected void onDetachedFromWindow() {
        generation++; bitmap = null; super.onDetachedFromWindow();
    }
    Bitmap export(int width, int height) {
        if (bitmap == null) return null;
        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        drawPhoto(new Canvas(result), width, height); return result;
    }
}
