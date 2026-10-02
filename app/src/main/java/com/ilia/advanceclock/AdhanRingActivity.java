package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class AdhanRingActivity
        extends Activity
        implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private QiblaAdhanBackgroundView qiblaBackgroundView;
    private boolean liveQiblaEnabled;
    private Location qiblaLocation;
    private LocationManager locationManager;
    private CancellationSignal locationCancellation;
    private LocationListener locationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        super.onCreate(savedInstanceState);

        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(
                R.layout.activity_adhan_ring);

        int type = getIntent().getIntExtra(
                "adhanType",
                AdhanScheduler.FAJR);

        ((TextView) findViewById(
                R.id.adhan_ring_title))
                .setText(
                        AdhanScheduler.title(type));

        String time =
                new SimpleDateFormat(
                        "HH:mm",
                        AppString.locale())
                        .format(new Date());
        ((TextView) findViewById(
                R.id.adhan_ring_time))
                .setText(
                        CalendarUtils.fa(time));

        ((TextView) findViewById(
                R.id.adhan_ring_location))
                .setText(
                        AppSettings.prayerLocationLabel(
                                this));

        configureQiblaDisplay();

        Button sound = findViewById(
                R.id.adhan_sound_toggle);
        sound.setOnClickListener(view -> {
            boolean muted =
                    AppString.get(
                            R.string.runtime_text_0306)
                            .contentEquals(
                                    sound.getText());

            send(
                    muted
                            ? AdhanSoundService.ACTION_UNMUTE
                            : AdhanSoundService.ACTION_MUTE);

            sound.setText(
                    muted
                            ? AppString.get(
                            R.string.runtime_text_0305)
                            : AppString.get(
                            R.string.runtime_text_0306));
        });

        findViewById(
                R.id.adhan_stop)
                .setOnClickListener(
                        view -> stopAndClose());
    }

    private void configureQiblaDisplay() {
        int mode =
                AppSettings.adhanQiblaDisplayMode(
                        this);

        Button qiblaButton =
                findViewById(
                        R.id.adhan_qibla_button);
        FrameLayout background =
                findViewById(
                        R.id.adhan_qibla_background_container);

        qiblaButton.setVisibility(View.GONE);
        background.setVisibility(View.GONE);
        liveQiblaEnabled = false;

        if (mode
                == AppSettings.ADHAN_QIBLA_BUTTON) {
            qiblaButton.setVisibility(
                    View.VISIBLE);
            qiblaButton.setOnClickListener(v ->
                    startActivity(
                            new Intent(
                                    this,
                                    CompassToolActivity.class)
                                    .putExtra(
                                            CompassToolActivity.EXTRA_START_MODE,
                                            CompassToolActivity.MODE_QIBLA)));
            return;
        }

        if (mode
                != AppSettings.ADHAN_QIBLA_BACKGROUND) {
            return;
        }

        background.setVisibility(View.VISIBLE);
        qiblaBackgroundView =
                new QiblaAdhanBackgroundView(this);
        background.removeAllViews();
        background.addView(
                qiblaBackgroundView,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        qiblaLocation =
                QiblaUtils.bestKnownLocation(this);
        if (qiblaLocation != null) {
            qiblaBackgroundView.setLocation(
                    qiblaLocation.getLatitude(),
                    qiblaLocation.getLongitude());
        }

        liveQiblaEnabled = true;
        sensorManager =
                (SensorManager)
                        getSystemService(
                                SENSOR_SERVICE);
        rotationSensor =
                sensorManager == null
                        ? null
                        : sensorManager.getDefaultSensor(
                        Sensor.TYPE_ROTATION_VECTOR);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (liveQiblaEnabled
                && sensorManager != null
                && rotationSensor != null) {
            sensorManager.registerListener(
                    this,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI);
        }

        if (liveQiblaEnabled) {
            refreshCurrentQiblaLocation();
        }
    }

    @Override
    protected void onPause() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(
                    this);
        }
        cancelLocationRequest();
        super.onPause();
    }

    @Override
    public void onSensorChanged(
            SensorEvent event) {
        if (!liveQiblaEnabled
                || qiblaBackgroundView == null
                || event.sensor.getType()
                != Sensor.TYPE_ROTATION_VECTOR) {
            return;
        }

        float[] matrix = new float[9];
        float[] adjusted = new float[9];

        SensorManager.getRotationMatrixFromVector(
                matrix,
                event.values);

        int rotation =
                getWindowManager()
                        .getDefaultDisplay()
                        .getRotation();

        int axisX = SensorManager.AXIS_X;
        int axisY = SensorManager.AXIS_Y;

        if (rotation
                == Surface.ROTATION_90) {
            axisX = SensorManager.AXIS_Y;
            axisY = SensorManager.AXIS_MINUS_X;
        } else if (rotation
                == Surface.ROTATION_180) {
            axisX = SensorManager.AXIS_MINUS_X;
            axisY = SensorManager.AXIS_MINUS_Y;
        } else if (rotation
                == Surface.ROTATION_270) {
            axisX = SensorManager.AXIS_MINUS_Y;
            axisY = SensorManager.AXIS_X;
        }

        SensorManager.remapCoordinateSystem(
                matrix,
                axisX,
                axisY,
                adjusted);

        float[] orientation =
                new float[3];
        SensorManager.getOrientation(
                adjusted,
                orientation);

        float azimuth =
                (float) Math.toDegrees(
                        orientation[0]);

        if (azimuth < 0f) {
            azimuth += 360f;
        }

        qiblaBackgroundView.setAzimuth(
                QiblaUtils.trueHeading(
                        azimuth,
                        qiblaLocation));
    }

    @Override
    public void onAccuracyChanged(
            Sensor sensor,
            int accuracy) {}

    @SuppressWarnings("MissingPermission")
    private void refreshCurrentQiblaLocation() {
        Location known =
                QiblaUtils.bestKnownLocation(this);
        if (known != null) {
            applyQiblaLocation(known);
        }

        if (!QiblaUtils.hasLocationPermission(this)) {
            return;
        }

        cancelLocationRequest();
        locationManager =
                (LocationManager)
                        getSystemService(
                                LOCATION_SERVICE);
        if (locationManager == null) return;

        String provider = null;
        try {
            if (locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
            } else if (locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
            }
        } catch (RuntimeException ignored) {
        }

        if (provider == null) return;

        final String selectedProvider = provider;
        if (Build.VERSION.SDK_INT >= 30) {
            locationCancellation =
                    new CancellationSignal();
            try {
                locationManager.getCurrentLocation(
                        selectedProvider,
                        locationCancellation,
                        getMainExecutor(),
                        this::applyQiblaLocation);
            } catch (RuntimeException ignored) {
            }
            return;
        }

        locationListener =
                new LocationListener() {
                    @Override
                    public void onLocationChanged(
                            Location location) {
                        applyQiblaLocation(location);
                        cancelLocationRequest();
                    }

                    @Override public void onStatusChanged(
                            String provider,
                            int status,
                            Bundle extras) {}

                    @Override public void onProviderEnabled(
                            String provider) {}

                    @Override public void onProviderDisabled(
                            String provider) {}
                };

        try {
            locationManager.requestSingleUpdate(
                    selectedProvider,
                    locationListener,
                    Looper.getMainLooper());
        } catch (RuntimeException ignored) {
        }
    }

    private void applyQiblaLocation(
            Location location) {
        if (location == null) return;

        qiblaLocation = location;
        QiblaUtils.remember(
                this,
                location.getLatitude(),
                location.getLongitude());

        if (qiblaBackgroundView != null) {
            qiblaBackgroundView.setLocation(
                    location.getLatitude(),
                    location.getLongitude());
        }
    }

    private void cancelLocationRequest() {
        if (locationCancellation != null) {
            locationCancellation.cancel();
            locationCancellation = null;
        }

        if (locationManager != null
                && locationListener != null) {
            try {
                locationManager.removeUpdates(
                        locationListener);
            } catch (RuntimeException ignored) {
            }
        }

        locationListener = null;
        locationManager = null;
    }


    @Override
    public boolean onKeyDown(
            int keyCode,
            KeyEvent event) {
        if (keyCode
                == KeyEvent.KEYCODE_VOLUME_DOWN) {
            send(
                    AdhanSoundService.ACTION_MUTE);
            ((Button) findViewById(
                    R.id.adhan_sound_toggle))
                    .setText(
                            AppString.get(
                                    R.string.runtime_text_0306));
            return true;
        }

        if (keyCode
                == KeyEvent.KEYCODE_VOLUME_UP) {
            send(
                    AdhanSoundService.ACTION_RAISE);
            ((Button) findViewById(
                    R.id.adhan_sound_toggle))
                    .setText(
                            AppString.get(
                                    R.string.runtime_text_0305));
            return true;
        }

        return super.onKeyDown(
                keyCode,
                event);
    }

    @Override
    public boolean onKeyUp(
            int keyCode,
            KeyEvent event) {
        if (keyCode
                == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode
                == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        }

        return super.onKeyUp(
                keyCode,
                event);
    }

    @Override
    public void onBackPressed() {
        // The dedicated Adhan must be stopped explicitly.
    }

    private void send(String action) {
        startService(
                AdhanSoundService.command(
                        this,
                        action));
    }

    private void stopAndClose() {
        send(
                AdhanSoundService.ACTION_STOP);
        finishAndRemoveTask();
    }
}
