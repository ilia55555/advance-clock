package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

/** A sensor-driven, antique-style compass and qibla tool. */
public final class CompassToolActivity extends Activity implements SensorEventListener {
    private static final String PREFS = "compass_tool";
    private static final String KEY_MODE = "display_mode";
    private static final int MODE_COMPASS = 0;
    private static final int MODE_QIBLA = 1;
    private static final int MODE_DIRECTIONS = 2;
    private static final double KAABA_LATITUDE = 21.422487;
    private static final double KAABA_LONGITUDE = 39.826206;

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private CompassView compassView;
    private TextView status;
    private int selectedMode;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(10), dp(18), dp(20));
        root.setBackgroundColor(AppSettings.background(this));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title = text(AppString.get(R.string.runtime_text_0318), 23, AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(58), 1f));
        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this));
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(header);

        LinearLayout selectorCard = new LinearLayout(this);
        selectorCard.setOrientation(LinearLayout.VERTICAL);
        selectorCard.setPadding(dp(14), dp(12), dp(14), dp(14));
        selectorCard.setBackgroundResource(R.drawable.bg_card);
        TextView selectorLabel = text(
                AppString.get(R.string.runtime_text_0319), 13, AppSettings.textSecondary(this));
        selectorCard.addView(selectorLabel);
        Spinner selector = new Spinner(this);
        String[] modes = UiText.translateArray(
                AppString.get(R.string.runtime_text_0320), AppString.get(R.string.runtime_text_0321), AppString.get(R.string.runtime_text_0322));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, modes);
        selector.setAdapter(adapter);
        selector.setBackgroundResource(R.drawable.bg_field);
        selector.setPadding(dp(12), 0, dp(12), 0);
        selectedMode = getSharedPreferences(PREFS, 0).getInt(KEY_MODE, MODE_COMPASS);
        if (selectedMode < MODE_COMPASS || selectedMode > MODE_DIRECTIONS) {
            selectedMode = MODE_COMPASS;
        }
        selector.setSelection(selectedMode);
        selectorCard.addView(selector, new LinearLayout.LayoutParams(-1, dp(52)));
        LinearLayout.LayoutParams selectorParams = new LinearLayout.LayoutParams(-1, -2);
        selectorParams.bottomMargin = dp(14);
        root.addView(selectorCard, selectorParams);

        LinearLayout compassCard = new LinearLayout(this);
        compassCard.setOrientation(LinearLayout.VERTICAL);
        compassCard.setGravity(Gravity.CENTER);
        compassCard.setPadding(dp(10), dp(12), dp(10), dp(14));
        compassCard.setBackground(antiqueCardBackground());
        compassView = new CompassView();
        compassCard.addView(compassView, new LinearLayout.LayoutParams(-1, 0, 1f));
        status = text(AppString.get(R.string.runtime_text_0323), 13, 0xFF5D4028);
        status.setGravity(Gravity.CENTER);
        status.setPadding(dp(8), dp(6), dp(8), 0);
        compassCard.addView(status, new LinearLayout.LayoutParams(-1, dp(46)));
        root.addView(compassCard, new LinearLayout.LayoutParams(-1, 0, 1f));

        selector.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(
                    android.widget.AdapterView<?> parent, View view, int position, long id) {
                selectedMode = position;
                getSharedPreferences(PREFS, 0).edit().putInt(KEY_MODE, position).apply();
                compassView.invalidate();
                updateStatus();
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        rotationSensor = sensorManager == null ? null
                : sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        setContentView(root);
        AppSettings.applyFullscreenInsets(root);
        AppSettings.playFullscreenEnter(this);
    }

    @Override protected void onResume() {
        super.onResume();
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        } else if (status != null) {
            status.setText(AppString.get(R.string.runtime_text_0324));
        }
    }

    @Override protected void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        super.onPause();
    }

    @Override public void onSensorChanged(SensorEvent event) {
        float[] matrix = new float[9];
        float[] adjusted = new float[9];
        SensorManager.getRotationMatrixFromVector(matrix, event.values);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int axisX = SensorManager.AXIS_X;
        int axisY = SensorManager.AXIS_Y;
        if (rotation == Surface.ROTATION_90) {
            axisX = SensorManager.AXIS_Y;
            axisY = SensorManager.AXIS_MINUS_X;
        } else if (rotation == Surface.ROTATION_180) {
            axisX = SensorManager.AXIS_MINUS_X;
            axisY = SensorManager.AXIS_MINUS_Y;
        } else if (rotation == Surface.ROTATION_270) {
            axisX = SensorManager.AXIS_MINUS_Y;
            axisY = SensorManager.AXIS_X;
        }
        SensorManager.remapCoordinateSystem(matrix, axisX, axisY, adjusted);
        float[] orientation = new float[3];
        SensorManager.getOrientation(adjusted, orientation);
        float azimuth = (float) Math.toDegrees(orientation[0]);
        if (azimuth < 0) azimuth += 360f;
        compassView.setAzimuth(azimuth);
        updateStatus();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateStatus() {
        if (status == null || compassView == null || rotationSensor == null) return;
        float heading = compassView.azimuth;
        if (selectedMode == MODE_QIBLA) {
            if (!AppSettings.prayerLocationSet(this)) {
                status.setText(AppString.get(R.string.runtime_text_0325));
            } else {
                status.setText(String.format(Locale.getDefault(),
                        AppString.get(R.string.runtime_text_0326),
                        qiblaBearing(), signedAngle((float) qiblaBearing() - heading)));
            }
        } else if (selectedMode == MODE_DIRECTIONS) {
            status.setText(directionName(heading) + "  •  " + Math.round(heading) + "°");
        } else {
            status.setText(AppString.get(R.string.runtime_text_0327)
                    + Math.round(heading) + "°");
        }
    }

    private double qiblaBearing() {
        double latitude = Math.toRadians(AppSettings.prayerLatitude(this));
        double longitudeDifference = Math.toRadians(
                KAABA_LONGITUDE - AppSettings.prayerLongitude(this));
        double kaabaLatitude = Math.toRadians(KAABA_LATITUDE);
        double y = Math.sin(longitudeDifference) * Math.cos(kaabaLatitude);
        double x = Math.cos(latitude) * Math.sin(kaabaLatitude)
                - Math.sin(latitude) * Math.cos(kaabaLatitude)
                * Math.cos(longitudeDifference);
        return (Math.toDegrees(Math.atan2(y, x)) + 360d) % 360d;
    }

    private float signedAngle(float angle) {
        return (angle + 540f) % 360f - 180f;
    }

    private String directionName(float angle) {
        String[] names = {AppString.get(R.string.runtime_text_0328), AppString.get(R.string.runtime_text_0329), AppString.get(R.string.runtime_text_0330), AppString.get(R.string.runtime_text_0331),
                AppString.get(R.string.runtime_text_0332), AppString.get(R.string.runtime_text_0333), AppString.get(R.string.runtime_text_0334), AppString.get(R.string.runtime_text_0335)};
        return UiText.tr(this, names[Math.round(angle / 45f) % 8]);
    }

    private GradientDrawable antiqueCardBackground() {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF4E4BD, 0xFFD8B77A});
        background.setCornerRadius(dp(18));
        background.setStroke(dp(2), 0xFF795331);
        return background;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private final class CompassView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private float azimuth;

        CompassView() {
            super(CompassToolActivity.this);
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            setContentDescription(AppString.get(R.string.runtime_text_0340));
        }

        void setAzimuth(float value) {
            float difference = signedAngle(value - azimuth);
            azimuth = (azimuth + difference * 0.18f + 360f) % 360f;
            invalidate();
        }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(getWidth(), getHeight()) * 0.43f;
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(dp(10), 0, dp(5), 0x66000000);
            paint.setColor(0xFF6A4326);
            canvas.drawCircle(cx, cy, radius + dp(11), paint);
            paint.clearShadowLayer();
            paint.setColor(0xFFF7E8C3);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(2));
            paint.setColor(0xFF8B6239);
            canvas.drawCircle(cx, cy, radius - dp(5), paint);
            canvas.drawCircle(cx, cy, radius * .72f, paint);

            canvas.save();
            canvas.rotate(-azimuth, cx, cy);
            for (int degree = 0; degree < 360; degree += 5) {
                boolean major = degree % 45 == 0;
                paint.setStrokeWidth(dp(major ? 3 : degree % 15 == 0 ? 2 : 1));
                paint.setColor(major ? 0xFF4A2D1A : 0xFF8B6239);
                float inner = radius - dp(major ? 25 : degree % 15 == 0 ? 17 : 10);
                double radians = Math.toRadians(degree - 90);
                canvas.drawLine(
                        cx + (float) Math.cos(radians) * inner,
                        cy + (float) Math.sin(radians) * inner,
                        cx + (float) Math.cos(radians) * (radius - dp(7)),
                        cy + (float) Math.sin(radians) * (radius - dp(7)), paint);
            }
            String[] labels = UiText.translateArray(
                    AppString.get(R.string.runtime_text_0457), AppString.get(R.string.runtime_text_0336), AppString.get(R.string.runtime_text_0458), AppString.get(R.string.runtime_text_0337), AppString.get(R.string.runtime_text_0459), AppString.get(R.string.runtime_text_0338), AppString.get(R.string.runtime_text_0460), AppString.get(R.string.runtime_text_0339));
            paint.setStyle(Paint.Style.FILL);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.create(Typeface.SERIF, Typeface.BOLD));
            paint.setTextSize(radius * .105f);
            for (int index = 0; index < labels.length; index++) {
                double radians = Math.toRadians(index * 45 - 90);
                paint.setColor(index == 0 ? 0xFF9D2017 : 0xFF3F2A1B);
                canvas.drawText(labels[index],
                        cx + (float) Math.cos(radians) * radius * .79f,
                        cy + (float) Math.sin(radians) * radius * .79f
                                + paint.getTextSize() * .35f, paint);
            }
            canvas.restore();

            if (selectedMode == MODE_QIBLA && AppSettings.prayerLocationSet(
                    CompassToolActivity.this)) {
                drawQiblaNeedle(canvas, cx, cy, radius,
                        signedAngle((float) qiblaBearing() - azimuth));
            } else {
                drawNorthNeedle(canvas, cx, cy, radius);
            }
            paint.setColor(0xFF6A4326);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, dp(10), paint);
            paint.setColor(0xFFD8B15B);
            canvas.drawCircle(cx, cy, dp(5), paint);
        }

        private void drawNorthNeedle(Canvas canvas, float cx, float cy, float radius) {
            path.reset();
            path.moveTo(cx, cy - radius * .62f);
            path.lineTo(cx - dp(10), cy + radius * .25f);
            path.lineTo(cx, cy + radius * .12f);
            path.close();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(0xFF9D2017);
            canvas.drawPath(path, paint);
            path.reset();
            path.moveTo(cx, cy - radius * .62f);
            path.lineTo(cx + dp(10), cy + radius * .25f);
            path.lineTo(cx, cy + radius * .12f);
            path.close();
            paint.setColor(0xFF33261E);
            canvas.drawPath(path, paint);
        }

        private void drawQiblaNeedle(
                Canvas canvas, float cx, float cy, float radius, float relativeAngle) {
            canvas.save();
            canvas.rotate(relativeAngle, cx, cy);
            paint.setColor(0xFFC18A19);
            paint.setStyle(Paint.Style.FILL);
            path.reset();
            path.moveTo(cx, cy - radius * .67f);
            path.lineTo(cx - dp(13), cy - radius * .48f);
            path.lineTo(cx - dp(4), cy - radius * .51f);
            path.lineTo(cx - dp(4), cy + radius * .18f);
            path.lineTo(cx + dp(4), cy + radius * .18f);
            path.lineTo(cx + dp(4), cy - radius * .51f);
            path.lineTo(cx + dp(13), cy - radius * .48f);
            path.close();
            canvas.drawPath(path, paint);
            paint.setColor(0xFF17120E);
            RectF kaaba = new RectF(cx - dp(13), cy - radius * .78f,
                    cx + dp(13), cy - radius * .64f);
            canvas.drawRoundRect(kaaba, dp(2), dp(2), paint);
            paint.setColor(0xFFD6AD4D);
            canvas.drawRect(kaaba.left, kaaba.top + dp(5), kaaba.right,
                    kaaba.top + dp(8), paint);
            canvas.restore();
        }
    }
}
