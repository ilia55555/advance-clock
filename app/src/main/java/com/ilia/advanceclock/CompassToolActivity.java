package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

/** A sensor-driven, antique-style compass and qibla tool. */
public final class CompassToolActivity extends Activity implements SensorEventListener {
    public static final String EXTRA_START_MODE = "compass_start_mode";

    public static final int MODE_COMPASS = 0;
    public static final int MODE_QIBLA = 1;
    public static final int MODE_DIRECTIONS = 2;

    private static final String PREFS = "compass_tool";
    private static final String KEY_MODE = "display_mode";
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private CompassView compassView;
    private TextView status;
    private int selectedMode;
    private Location qiblaLocation;
    private QiblaLocationClient qiblaLocationClient;
    private LinearLayout locationAccessCard;
    private TextView locationAccessStatus;
    private Button locationPermissionAction;
    private Button locationGpsAction;
    private Button locationRetryAction;
    private int locationAccessErrorRes;
    private static final int REQ_QIBLA_LOCATION = 911;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(dp(18), dp(10), dp(18), dp(20));
        root.setBackgroundColor(AppSettings.background(this));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this));
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = text(AppString.get(R.string.runtime_text_0318), 23, AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(58), 1f));

        root.addView(header);

        LinearLayout selectorCard = new LinearLayout(this);
        selectorCard.setOrientation(LinearLayout.VERTICAL);
        selectorCard.setPadding(dp(14), dp(12), dp(14), dp(14));
        selectorCard.setBackgroundResource(R.drawable.bg_card);
        TextView selectorLabel = text(
                AppString.get(R.string.runtime_text_0319), 13, AppSettings.textSecondary(this));
        selectorCard.addView(selectorLabel);
        Spinner selector = new Spinner(this);
        String[] modes = new String[]{
                AppString.get(R.string.runtime_text_0320), AppString.get(R.string.runtime_text_0321), AppString.get(R.string.runtime_text_0322)};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, modes);
        selector.setAdapter(adapter);
        selector.setBackgroundResource(R.drawable.bg_field);
        selector.setPadding(dp(12), 0, dp(12), 0);
        int requestedMode = getIntent().getIntExtra(
                EXTRA_START_MODE,
                -1);
        if (requestedMode >= MODE_COMPASS
                && requestedMode <= MODE_DIRECTIONS) {
            selectedMode = requestedMode;
        } else {
            selectedMode = getSharedPreferences(
                    PREFS,
                    0).getInt(
                    KEY_MODE,
                    MODE_COMPASS);
        }
        if (selectedMode < MODE_COMPASS
                || selectedMode > MODE_DIRECTIONS) {
            selectedMode = MODE_COMPASS;
        }
        selector.setSelection(selectedMode);
        selectorCard.addView(selector, new LinearLayout.LayoutParams(-1, dp(52)));
        LinearLayout.LayoutParams selectorParams = new LinearLayout.LayoutParams(-1, -2);
        selectorParams.bottomMargin = dp(14);
        root.addView(selectorCard, selectorParams);

        locationAccessCard = new LinearLayout(this);
        locationAccessCard.setOrientation(LinearLayout.VERTICAL);
        locationAccessCard.setLayoutDirection(
                AppSettings.layoutDirection(this));
        locationAccessCard.setPadding(
                dp(14),
                dp(12),
                dp(14),
                dp(12));
        locationAccessCard.setBackgroundResource(
                R.drawable.bg_card);

        TextView accessTitle = text(
                AppString.get(R.string.location_access_title),
                14,
                AppSettings.textPrimary(this));
        accessTitle.setTypeface(
                Typeface.DEFAULT,
                Typeface.BOLD);
        locationAccessCard.addView(accessTitle);

        locationAccessStatus = text(
                "",
                12,
                AppSettings.textSecondary(this));
        locationAccessStatus.setPadding(
                0,
                dp(3),
                0,
                dp(6));
        locationAccessCard.addView(locationAccessStatus);

        locationPermissionAction =
                accessButton("");
        locationPermissionAction.setVisibility(View.GONE);
        locationPermissionAction.setOnClickListener(v ->
                handleLocationPermissionAction());
        locationAccessCard.addView(
                locationPermissionAction,
                new LinearLayout.LayoutParams(-1, dp(46)));

        locationGpsAction =
                accessButton(
                        AppString.get(
                                R.string.location_access_enable_gps));
        locationGpsAction.setVisibility(View.GONE);
        locationGpsAction.setOnClickListener(v ->
                openLocationSettings());
        LinearLayout.LayoutParams gpsParams =
                new LinearLayout.LayoutParams(-1, dp(46));
        gpsParams.topMargin = dp(6);
        locationAccessCard.addView(
                locationGpsAction,
                gpsParams);

        locationRetryAction =
                accessButton(
                        AppString.get(
                                R.string.location_access_retry));
        locationRetryAction.setVisibility(View.GONE);
        locationRetryAction.setOnClickListener(v -> {
            locationAccessErrorRes = 0;
            ensureQiblaLocation();
        });
        LinearLayout.LayoutParams retryParams =
                new LinearLayout.LayoutParams(-1, dp(46));
        retryParams.topMargin = dp(6);
        locationAccessCard.addView(
                locationRetryAction,
                retryParams);

        LinearLayout.LayoutParams accessParams =
                new LinearLayout.LayoutParams(-1, -2);
        accessParams.bottomMargin = dp(12);
        root.addView(
                locationAccessCard,
                accessParams);

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
                refreshLocationAccessUi();
                if (selectedMode == MODE_QIBLA) {
                    ensureQiblaLocation();
                }
                compassView.invalidate();
                updateStatus();
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        qiblaLocation = QiblaUtils.bestKnownLocation(this);
        qiblaLocationClient = new QiblaLocationClient(this);
        refreshLocationAccessUi();

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
        if (selectedMode == MODE_QIBLA) {
            ensureQiblaLocation();
        }
    }

    @Override protected void onPause() {
        if (sensorManager != null) sensorManager.unregisterListener(this);
        if (qiblaLocationClient != null) {
            qiblaLocationClient.cancel();
        }
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
        float azimuth =
                (float) Math.toDegrees(
                        orientation[0]);
        if (azimuth < 0f) {
            azimuth += 360f;
        }

        float trueAzimuth =
                QiblaUtils.trueHeading(
                        azimuth,
                        qiblaLocation);
        compassView.setAzimuth(
                trueAzimuth);
        updateStatus();
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void updateStatus() {
        if (status == null || compassView == null || rotationSensor == null) return;
        float heading = compassView.azimuth;
        if (selectedMode == MODE_QIBLA) {
            if (qiblaLocation == null) {
                status.setText(AppString.get(R.string.runtime_text_0325));
            } else {
                double bearing = qiblaBearing();
                status.setText(String.format(
                        AppString.locale(),
                        AppString.get(R.string.runtime_text_0326),
                        bearing,
                        signedAngle((float) bearing - heading)));
            }
        } else if (selectedMode == MODE_DIRECTIONS) {
            status.setText(directionName(heading) + "  •  " + Math.round(heading) + "°");
        } else {
            status.setText(AppString.get(R.string.runtime_text_0327)
                    + Math.round(heading) + "°");
        }
    }

    private double qiblaBearing() {
        if (qiblaLocation == null) return 0d;
        return QiblaUtils.bearing(
                qiblaLocation.getLatitude(),
                qiblaLocation.getLongitude());
    }

    private void ensureQiblaLocation() {
        Location known =
                QiblaUtils.bestKnownLocation(this);
        if (known != null) {
            qiblaLocation = known;
            QiblaUtils.remember(
                    this,
                    known.getLatitude(),
                    known.getLongitude());
            if (compassView != null) {
                compassView.invalidate();
            }
            updateStatus();
        }

        refreshLocationAccessUi();

        if (!QiblaUtils.hasLocationPermission(this)
                || !QiblaUtils.isLocationEnabled(this)) {
            return;
        }

        requestFreshLocation();
    }

    private void requestLocationPermission() {
        if (Build.VERSION.SDK_INT < 23) {
            ensureQiblaLocation();
            return;
        }

        if (QiblaUtils.locationPermissionBlocked(this)) {
            openAppPermissionSettings();
            return;
        }

        QiblaUtils.markLocationPermissionRequested(this);
        requestPermissions(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },
                REQ_QIBLA_LOCATION);
    }

    private void handleLocationPermissionAction() {
        if (QiblaUtils.locationPermissionBlocked(this)) {
            openAppPermissionSettings();
        } else {
            requestLocationPermission();
        }
    }

    private void openAppPermissionSettings() {
        try {
            startActivity(
                    new Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse(
                                    "package:"
                                            + getPackageName())));
        } catch (Exception ignored) {
        }
    }

    private void openLocationSettings() {
        try {
            startActivity(
                    new Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        } catch (Exception ignored) {
        }
    }

    private void refreshLocationAccessUi() {
        if (locationAccessCard == null) return;

        boolean qiblaMode =
                selectedMode == MODE_QIBLA;
        locationAccessCard.setVisibility(
                qiblaMode
                        ? View.VISIBLE
                        : View.GONE);
        if (!qiblaMode) return;

        boolean permission =
                QiblaUtils.hasLocationPermission(this);
        boolean gpsEnabled =
                QiblaUtils.isLocationEnabled(this);
        boolean blocked =
                QiblaUtils.locationPermissionBlocked(this);
        boolean requested =
                QiblaUtils.locationPermissionWasRequested(this);

        locationPermissionAction.setVisibility(View.GONE);
        locationGpsAction.setVisibility(View.GONE);
        locationRetryAction.setVisibility(View.GONE);

        if (!permission) {
            locationAccessStatus.setText(
                    AppString.get(
                            blocked
                                    ? R.string.location_access_permission_blocked
                                    : requested
                                    ? R.string.location_access_permission_denied
                                    : R.string.location_access_permission_needed));
            locationAccessStatus.setTextColor(0xFFC44C4C);

            locationPermissionAction.setText(
                    AppString.get(
                            blocked
                                    ? R.string.location_access_open_app_settings
                                    : requested
                                    ? R.string.location_access_request_again
                                    : R.string.location_access_grant));
            locationPermissionAction.setVisibility(View.VISIBLE);

            if (!gpsEnabled) {
                locationGpsAction.setVisibility(View.VISIBLE);
            }
            return;
        }

        if (!gpsEnabled) {
            locationAccessStatus.setText(
                    AppString.get(
                            R.string.location_access_gps_off));
            locationAccessStatus.setTextColor(0xFFC44C4C);
            locationGpsAction.setVisibility(View.VISIBLE);
            return;
        }

        if (qiblaLocation != null) {
            locationAccessErrorRes = 0;
        }

        if (locationAccessErrorRes != 0) {
            locationAccessStatus.setText(
                    AppString.get(locationAccessErrorRes));
            locationAccessStatus.setTextColor(0xFFC44C4C);
            locationRetryAction.setVisibility(View.VISIBLE);
            return;
        }

        locationAccessStatus.setText(
                AppString.get(
                        R.string.location_access_ready));
        locationAccessStatus.setTextColor(
                AppSettings.textSecondary(this));
    }

    private void requestFreshLocation() {
        if (qiblaLocationClient == null) {
            qiblaLocationClient =
                    new QiblaLocationClient(this);
        }

        locationAccessErrorRes = 0;
        if (locationAccessStatus != null) {
            locationAccessStatus.setText(
                    AppString.get(
                            R.string.runtime_text_0098));
            locationAccessStatus.setTextColor(
                    AppSettings.textSecondary(this));
            locationRetryAction.setVisibility(View.GONE);
        }

        qiblaLocationClient.request(
                new QiblaLocationClient.Callback() {
                    @Override
                    public void onLocation(
                            Location location) {
                        applyQiblaLocation(location);
                    }

                    @Override
                    public void onError(
                            int stringRes) {
                        if (qiblaLocation != null
                                && QiblaUtils.hasLocationPermission(
                                CompassToolActivity.this)
                                && QiblaUtils.isLocationEnabled(
                                CompassToolActivity.this)) {
                            locationAccessErrorRes = 0;
                        } else {
                            locationAccessErrorRes = stringRes;
                        }
                        refreshLocationAccessUi();
                    }
                });
    }

    private void applyQiblaLocation(Location location) {
        if (location == null) return;

        locationAccessErrorRes = 0;
        qiblaLocation = location;
        QiblaUtils.remember(
                this,
                location.getLatitude(),
                location.getLongitude());

        if (compassView != null) {
            compassView.invalidate();
        }

        updateStatus();
        refreshLocationAccessUi();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode != REQ_QIBLA_LOCATION) {
            return;
        }

        boolean granted = false;
        for (int result : grantResults) {
            if (result
                    == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }

        if (granted) {
            locationAccessErrorRes = 0;
            ensureQiblaLocation();
        } else {
            qiblaLocation =
                    QiblaUtils.bestKnownLocation(this);
            if (compassView != null) {
                compassView.invalidate();
            }
            refreshLocationAccessUi();
            updateStatus();
        }
    }

    private float signedAngle(float angle) {
        return (angle + 540f) % 360f - 180f;
    }

    private String directionName(float angle) {
        String[] names = {AppString.get(R.string.runtime_text_0328), AppString.get(R.string.runtime_text_0329), AppString.get(R.string.runtime_text_0330), AppString.get(R.string.runtime_text_0331),
                AppString.get(R.string.runtime_text_0332), AppString.get(R.string.runtime_text_0333), AppString.get(R.string.runtime_text_0334), AppString.get(R.string.runtime_text_0335)};
        return names[Math.round(angle / 45f) % 8];
    }

    private GradientDrawable antiqueCardBackground() {
        GradientDrawable background = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFFF4E4BD, 0xFFD8B77A});
        background.setCornerRadius(dp(18));
        background.setStroke(dp(2), 0xFF795331);
        return background;
    }

    private Button accessButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(10), 0, dp(10), 0);
        button.setBackgroundResource(R.drawable.bg_field);
        return button;
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
            String[] labels = new String[]{
                    AppString.get(R.string.runtime_text_0457), AppString.get(R.string.runtime_text_0336), AppString.get(R.string.runtime_text_0458), AppString.get(R.string.runtime_text_0337), AppString.get(R.string.runtime_text_0459), AppString.get(R.string.runtime_text_0338), AppString.get(R.string.runtime_text_0460), AppString.get(R.string.runtime_text_0339)};
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

            if (selectedMode == MODE_QIBLA) {
                if (qiblaLocation != null) {
                    drawQiblaNeedle(
                            canvas,
                            cx,
                            cy,
                            radius,
                            signedAngle(
                                    (float) qiblaBearing()
                                            - azimuth));
                }
            } else {
                drawNorthNeedle(
                        canvas,
                        cx,
                        cy,
                        radius);
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
