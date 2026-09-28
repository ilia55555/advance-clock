package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public final class PrayerSettingsActivity extends Activity {
    private static final int REQ_LOCATION = 740;
    private static final int REQ_NOTIFICATIONS = 741;

    private Button locationButton;
    private TextView locationStatus;
    private TextView adhanScheduleStatus;
    private final Handler locationHandler = new Handler(Looper.getMainLooper());
    private CancellationSignal locationCancellation;
    private Runnable locationTimeout;
    private LocationManager activeLocationManager;
    private LocationListener activeLocationListener;
    private int locationRequestGeneration;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(24));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("تنظیمات اذان و اوقات شرعی");
        title.setTextSize(24);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);

        LinearLayout masterCard = card();
        TextView masterTitle = text(
                "نمایش اوقات شرعی",
                18,
                AppSettings.textPrimary(this));
        masterTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        masterCard.addView(masterTitle);

        Switch master = toggle(
                "نمایش نوار اوقات شرعی زیر تقویم",
                AppSettings.adhanEnabled(this));
        master.setOnCheckedChangeListener((button, checked) -> {
            AppSettings.setAdhanEnabled(this, checked);
            setResult(RESULT_OK);
        });
        masterCard.addView(master);
        root.addView(masterCard, cardParams());

        LinearLayout locationCard = card();
        TextView locationTitle = text(
                "موقعیت جغرافیایی",
                17,
                AppSettings.textPrimary(this));
        locationTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        locationCard.addView(locationTitle);

        locationStatus = text("", 12, AppSettings.textSecondary(this));
        locationStatus.setPadding(0, dp(4), 0, dp(8));
        locationCard.addView(locationStatus);

        Button searchLocation = fieldButton("جستجوی شهر یا روستا");
        searchLocation.setOnClickListener(v ->
                startActivity(new Intent(this, PrayerLocationSearchActivity.class)));
        locationCard.addView(searchLocation, new LinearLayout.LayoutParams(-1, dp(52)));

        locationButton = fieldButton("دریافت موقعیت دقیق فعلی");
        locationButton.setOnClickListener(v -> requestPreciseLocation());
        LinearLayout.LayoutParams locationButtonParams =
                new LinearLayout.LayoutParams(-1, dp(52));
        locationButtonParams.topMargin = dp(8);
        locationCard.addView(locationButton, locationButtonParams);

        root.addView(locationCard, cardParams());

        LinearLayout azanCard = card();
        TextView azanTitle = text(
                "اذان‌های فعال",
                17,
                AppSettings.textPrimary(this));
        azanTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        azanCard.addView(azanTitle);

        Switch fajr = adhanSwitch("اذان صبح", AppSettings.fajrAdhanEnabled(this),
                value -> AppSettings.setFajrAdhanEnabled(this, value));
        Switch dhuhr = adhanSwitch("اذان ظهر", AppSettings.dhuhrAdhanEnabled(this),
                value -> AppSettings.setDhuhrAdhanEnabled(this, value));
        Switch asr = adhanSwitch("عصر", AppSettings.asrAdhanEnabled(this),
                value -> AppSettings.setAsrAdhanEnabled(this, value));
        Switch maghrib = adhanSwitch("اذان مغرب", AppSettings.maghribAdhanEnabled(this),
                value -> AppSettings.setMaghribAdhanEnabled(this, value));
        Switch isha = adhanSwitch("عشاء", AppSettings.ishaAdhanEnabled(this),
                value -> AppSettings.setIshaAdhanEnabled(this, value));
        azanCard.addView(fajr); azanCard.addView(dhuhr); azanCard.addView(asr);
        azanCard.addView(maghrib); azanCard.addView(isha);

        addSoundButton(azanCard, "موذن اذان صبح", AdhanScheduler.FAJR);
        addSoundButton(azanCard, "موذن اذان ظهر", AdhanScheduler.DHUHR);
        addSoundButton(azanCard, "موذن عصر", AdhanScheduler.ASR);
        addSoundButton(azanCard, "موذن اذان مغرب", AdhanScheduler.MAGHRIB);
        addSoundButton(azanCard, "موذن عشاء", AdhanScheduler.ISHA);

        LinearLayout outputCard = card();
        TextView outputTitle = text("نحوه اعلام اذان", 17, AppSettings.textPrimary(this));
        outputTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        outputCard.addView(outputTitle);
        Switch fullscreen = toggle("صفحه تمام‌صفحه اذان", AppSettings.adhanFullscreen(this));
        fullscreen.setOnCheckedChangeListener((button, checked) -> AppSettings.setAdhanFullscreen(this, checked));
        Switch notification = toggle("اعلان اذان", AppSettings.adhanNotification(this));
        notification.setOnCheckedChangeListener((button, checked) -> AppSettings.setAdhanNotification(this, checked));
        Switch vibrate = toggle("لرزش اذان", AppSettings.adhanVibrate(this));
        vibrate.setOnCheckedChangeListener((button, checked) -> AppSettings.setAdhanVibrate(this, checked));
        Switch sound = toggle("صدای اذان", AppSettings.adhanSound(this));
        sound.setOnCheckedChangeListener((button, checked) -> AppSettings.setAdhanSound(this, checked));
        outputCard.addView(fullscreen); outputCard.addView(notification);
        outputCard.addView(vibrate); outputCard.addView(sound);

        adhanScheduleStatus = text(
                "",
                11,
                AppSettings.textSecondary(this));
        adhanScheduleStatus.setPadding(0, dp(6), 0, 0);
        adhanScheduleStatus.setOnClickListener(v -> resolveAdhanScheduleStatus());
        azanCard.addView(adhanScheduleStatus);

        root.addView(azanCard, cardParams());
        root.addView(outputCard, cardParams());

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        boolean permissionGranted = PermissionHelper.exactAlarmsGranted(this);
        if (permissionGranted
                && AdhanScheduler.status(this) == AdhanScheduler.Status.SCHEDULE_FAILED) {
            AdhanScheduler.rescheduleAll(this);
        }
        refresh();
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private void refresh() {
        refreshAdhanScheduleStatus();
        boolean hasLocation = AppSettings.prayerLocationSet(this);
        if (hasLocation) {
            String label = AppSettings.prayerLocationLabel(this);
            locationStatus.setText(
                    label
                            + "\n"
                            + coordinateText(
                            AppSettings.prayerLatitude(this),
                            AppSettings.prayerLongitude(this)));
            locationButton.setText("به‌روزرسانی موقعیت دقیق فعلی");
        } else {
            locationStatus.setText("موقعیت تنظیم نشده");
            locationButton.setText("دریافت موقعیت دقیق فعلی");
        }

    }

    private void updateAdhanSetting(Runnable update, boolean enabled) {
        update.run();
        refreshAdhanScheduleStatus();
        if (enabled && AdhanScheduler.status(this) != AdhanScheduler.Status.SCHEDULED) {
            resolveAdhanScheduleStatus();
        }
    }

    private void refreshAdhanScheduleStatus() {
        if (adhanScheduleStatus == null) return;
        AdhanScheduler.Status status = AdhanScheduler.status(this);
        String message;
        switch (status) {
            case DISABLED:
                message = "همه اعلان‌های اذان خاموش‌اند.";
                break;
            case LOCATION_MISSING:
                message = "⚠ اذان زمان‌بندی نشده است؛ برای ثبت موقعیت اینجا بزنید.";
                break;
            case EXACT_PERMISSION_MISSING:
                message = "⚠ اذان زمان‌بندی نشده است؛ مجوز آلارم دقیق را فعال کنید.";
                break;
            case NOTIFICATION_PERMISSION_MISSING:
                message = "⚠ زمان اذان ثبت شده، اما مجوز نمایش اعلان داده نشده است.";
                break;
            case SCHEDULE_FAILED:
                message = "⚠ زمان‌بندی اذان ناموفق بود؛ برای تلاش دوباره اینجا بزنید.";
                break;
            case SCHEDULED:
            default:
                message = "اذان فعال است";
                break;
        }
        adhanScheduleStatus.setText(message);
        boolean problem = status != AdhanScheduler.Status.SCHEDULED
                && status != AdhanScheduler.Status.DISABLED;
        adhanScheduleStatus.setTextColor(problem
                ? 0xFFC44C4C : AppSettings.textSecondary(this));
        adhanScheduleStatus.setClickable(problem);
    }

    private void resolveAdhanScheduleStatus() {
        AdhanScheduler.Status status = AdhanScheduler.status(this);
        if (status == AdhanScheduler.Status.LOCATION_MISSING) {
            requestPreciseLocation();
        } else if (status == AdhanScheduler.Status.EXACT_PERMISSION_MISSING) {
            try {
                startActivity(new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {
            }
        } else if (status == AdhanScheduler.Status.NOTIFICATION_PERMISSION_MISSING
                && Build.VERSION.SDK_INT >= 33) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_NOTIFICATIONS);
        } else if (status == AdhanScheduler.Status.SCHEDULE_FAILED) {
            AdhanScheduler.rescheduleAll(this);
            refreshAdhanScheduleStatus();
        }
    }

    private Switch adhanSwitch(
            String label, boolean checked, java.util.function.Consumer<Boolean> update) {
        Switch value = toggle(label, checked);
        value.setOnCheckedChangeListener((button, enabled) ->
                updateAdhanSetting(() -> update.accept(enabled), enabled));
        return value;
    }

    private void addSoundButton(LinearLayout parent, String label, int type) {
        Button button = fieldButton(label + " • "
                + SoundLibrary.name(this, AppSettings.adhanSoundUri(this, type)));
        button.setOnClickListener(v -> startActivityForResult(
                new Intent(this, SoundPickerActivity.class), 800 + type));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.topMargin = dp(6);
        parent.addView(button, params);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode < 800 || requestCode > 804 || resultCode != RESULT_OK || data == null) return;
        AppSettings.setAdhanSoundUri(
                this, requestCode - 800, data.getStringExtra(SoundPickerActivity.EXTRA_URI));
        recreate();
    }

    private void requestPreciseLocation() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQ_LOCATION);
            return;
        }
        fetchCurrentLocation();
    }

    @Override public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_NOTIFICATIONS) {
            refreshAdhanScheduleStatus();
            return;
        }
        if (requestCode != REQ_LOCATION) return;

        boolean granted = false;
        for (int value : grantResults) {
            if (value == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }
        if (granted) {
            fetchCurrentLocation();
        } else {
            LogoToast.makeText(
                    this,
                    "مجوز موقعیت داده نشد؛ شهر یا روستا را جستجو کنید.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("deprecation")
    private void fetchCurrentLocation() {
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationManager manager =
                (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            LogoToast.makeText(this, "سرویس موقعیت در دسترس نیست.", Toast.LENGTH_SHORT).show();
            return;
        }

        String provider = null;
        try {
            if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                provider = LocationManager.GPS_PROVIDER;
            } else if (manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                provider = LocationManager.NETWORK_PROVIDER;
            }
        } catch (Exception ignored) {}

        if (provider == null) {
            LogoToast.makeText(
                    this,
                    "مکان دستگاه را روشن کنید و دوباره تلاش کنید.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        locationButton.setEnabled(false);
        locationButton.setText("در حال دریافت موقعیت…");

        final String selectedProvider = provider;
        final int requestGeneration = ++locationRequestGeneration;
        activeLocationManager = manager;
        locationTimeout = () -> {
            if (requestGeneration != locationRequestGeneration) return;
            cancelLocationRequest();
            locationButton.setEnabled(true);
            refresh();
            LogoToast.makeText(
                    this,
                    "دریافت موقعیت بیش از حد طول کشید؛ دوباره تلاش کنید.",
                    Toast.LENGTH_LONG).show();
        };
        locationHandler.postDelayed(locationTimeout, 20_000L);
        if (Build.VERSION.SDK_INT >= 30) {
            locationCancellation = new CancellationSignal();
            try {
                manager.getCurrentLocation(
                        selectedProvider,
                        locationCancellation,
                        command -> runOnUiThread(command),
                        location -> {
                            if (requestGeneration != locationRequestGeneration) return;
                            clearLocationTimeout();
                            handleLocation(location, requestGeneration);
                        });
            } catch (RuntimeException error) {
                failLocationRequest(requestGeneration);
            }
            return;
        }

        activeLocationListener = new LocationListener() {
                    @Override public void onLocationChanged(Location location) {
                        if (requestGeneration != locationRequestGeneration) return;
                        handleLocation(location, requestGeneration);
                    }

                    @Override public void onStatusChanged(
                            String provider, int status, Bundle extras) {}

                    @Override public void onProviderEnabled(String provider) {}

                    @Override public void onProviderDisabled(String provider) {}
                };
        try {
            manager.requestSingleUpdate(
                    selectedProvider, activeLocationListener, Looper.getMainLooper());
        } catch (RuntimeException error) {
            failLocationRequest(requestGeneration);
        }
    }

    private void failLocationRequest(int requestGeneration) {
        if (requestGeneration != locationRequestGeneration) return;
        cancelLocationRequest();
        locationButton.setEnabled(true);
        refresh();
        LogoToast.makeText(
                this,
                "شروع دریافت موقعیت ممکن نشد؛ وضعیت مکان و مجوز را بررسی کنید.",
                Toast.LENGTH_LONG).show();
    }

    private void handleLocation(Location location, int requestGeneration) {
        clearLocationTimeout();
        if (location == null) {
            locationButton.setEnabled(true);
            refresh();
            LogoToast.makeText(
                    this,
                    "موقعیت دقیق دریافت نشد؛ دوباره تلاش کنید.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        double lat = location.getLatitude();
        double lon = location.getLongitude();
        String fallback = coordinateText(lat, lon);
        AppSettings.setPrayerLocation(this, lat, lon, fallback);
        locationButton.setEnabled(true);
        refresh();
        setResult(RESULT_OK);

        new Thread(() -> resolveLocationName(
                lat, lon, fallback, requestGeneration)).start();
    }

    private void clearLocationTimeout() {
        if (locationTimeout != null) locationHandler.removeCallbacks(locationTimeout);
        locationTimeout = null;
        locationCancellation = null;
        if (activeLocationManager != null && activeLocationListener != null) {
            activeLocationManager.removeUpdates(activeLocationListener);
        }
        activeLocationManager = null;
        activeLocationListener = null;
    }

    private void cancelLocationRequest() {
        locationRequestGeneration++;
        if (locationCancellation != null) locationCancellation.cancel();
        clearLocationTimeout();
    }

    @Override protected void onDestroy() {
        cancelLocationRequest();
        super.onDestroy();
    }

    @SuppressWarnings("deprecation")
    private void resolveLocationName(
            double lat, double lon, String fallback, int requestGeneration) {
        String label = fallback;
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address a = addresses.get(0);
                String locality = firstNotEmpty(
                        a.getLocality(),
                        a.getSubAdminArea(),
                        a.getAdminArea(),
                        a.getCountryName());
                if (locality != null) label = locality;
            }
        } catch (IOException | RuntimeException ignored) {}

        final String finalLabel = label;
        runOnUiThread(() -> {
            if (requestGeneration != locationRequestGeneration || isFinishing()
                    || (Build.VERSION.SDK_INT >= 17 && isDestroyed())) {
                return;
            }
            AppSettings.setPrayerLocation(this, lat, lon, finalLabel);
            refresh();
        });
    }

    private String firstNotEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return null;
    }

    private String coordinateText(double lat, double lon) {
        return String.format(
                Locale.US,
                "عرض %.5f°  •  طول %.5f°",
                lat,
                lon);
    }

    private Switch toggle(String value, boolean checked) {
        Switch control = new Switch(this);
        control.setText(value);
        control.setTextColor(AppSettings.textPrimary(this));
        control.setTextSize(14);
        control.setChecked(checked);
        control.setPadding(0, dp(3), 0, dp(3));
        return control;
    }

    private Button fieldButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackgroundResource(R.drawable.bg_field);
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        return params;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
