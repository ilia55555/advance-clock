package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
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
import java.util.TimeZone;

public final class PrayerSettingsActivity extends Activity {
    private static final int REQ_LOCATION = 740;

    private Button locationButton;
    private TextView locationStatus;
    private TextView fajrTime;
    private TextView sunriseTime;
    private TextView dhuhrTime;
    private TextView asrTime;
    private TextView sunsetTime;
    private TextView maghribTime;
    private TextView ishaTime;
    private TextView midnightTime;

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

        TextView intro = text(
                "محاسبه آفلاین بر پایه روش مرکز تقویم مؤسسه ژئوفیزیک دانشگاه تهران "
                        + "(فجر ۱۷٫۷° و مغرب ۴٫۵°) و مختصات دقیق دستگاه انجام می‌شود.",
                12,
                AppSettings.textSecondary(this));
        intro.setPadding(0, 0, 0, dp(10));
        root.addView(intro);

        LinearLayout masterCard = card();
        TextView masterTitle = text(
                "نمایش اوقات شرعی",
                18,
                AppSettings.textPrimary(this));
        masterTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        masterCard.addView(masterTitle);

        Switch master = toggle(
                "نمایش باکس اوقات شرعی زیر تقویم",
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

        locationButton = fieldButton("دریافت موقعیت دقیق فعلی");
        locationButton.setOnClickListener(v -> requestPreciseLocation());
        locationCard.addView(locationButton, new LinearLayout.LayoutParams(-1, dp(52)));

        Button method = fieldButton(
                "روش محاسبه: ژئوفیزیک دانشگاه تهران • همسان با اوقات رسمی ایران");
        method.setEnabled(false);
        method.setAlpha(0.82f);
        LinearLayout.LayoutParams methodParams =
                new LinearLayout.LayoutParams(-1, dp(52));
        methodParams.topMargin = dp(8);
        locationCard.addView(method, methodParams);
        root.addView(locationCard, cardParams());

        LinearLayout prayersCard = card();
        TextView prayersTitle = text(
                "اوقات امروز",
                17,
                AppSettings.textPrimary(this));
        prayersTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        prayersCard.addView(prayersTitle);

        TextView timesHint = text(
                "زمان‌ها هر روز از نو برای مختصات ذخیره‌شده و منطقه زمانی دستگاه محاسبه می‌شوند.",
                12,
                AppSettings.textSecondary(this));
        timesHint.setPadding(0, dp(3), 0, dp(6));
        prayersCard.addView(timesHint);

        fajrTime = addTimeRow(prayersCard, "اذان صبح");
        sunriseTime = addTimeRow(prayersCard, "طلوع آفتاب");
        dhuhrTime = addTimeRow(prayersCard, "اذان ظهر");
        asrTime = addTimeRow(prayersCard, "عصر");
        sunsetTime = addTimeRow(prayersCard, "غروب آفتاب");
        maghribTime = addTimeRow(prayersCard, "اذان مغرب");
        ishaTime = addTimeRow(prayersCard, "عشاء");
        midnightTime = addTimeRow(prayersCard, "نیمه‌شب شرعی");
        root.addView(prayersCard, cardParams());

        LinearLayout azanCard = card();
        TextView azanTitle = text(
                "اذان‌های فعال",
                17,
                AppSettings.textPrimary(this));
        azanTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        azanCard.addView(azanTitle);

        Switch fajr = toggle(
                "اذان صبح",
                AppSettings.fajrAdhanEnabled(this));
        fajr.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setFajrAdhanEnabled(this, checked));
        azanCard.addView(fajr);

        Switch dhuhr = toggle(
                "اذان ظهر",
                AppSettings.dhuhrAdhanEnabled(this));
        dhuhr.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setDhuhrAdhanEnabled(this, checked));
        azanCard.addView(dhuhr);

        Switch maghrib = toggle(
                "اذان مغرب",
                AppSettings.maghribAdhanEnabled(this));
        maghrib.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setMaghribAdhanEnabled(this, checked));
        azanCard.addView(maghrib);

        Switch vibrate = toggle(
                "لرزش همراه اذان/اعلان",
                AppSettings.adhanVibrate(this));
        vibrate.setOnCheckedChangeListener((button, checked) ->
                AppSettings.setAdhanVibrate(this, checked));
        azanCard.addView(vibrate);

        TextView schedulingNote = text(
                "این سوییچ‌ها برای موتور پخش اذان ذخیره می‌شوند؛ "
                        + "نمایش اوقات شرعی مستقل از انتخاب صدای مؤذن است.",
                11,
                AppSettings.textSecondary(this));
        schedulingNote.setPadding(0, dp(6), 0, 0);
        azanCard.addView(schedulingNote);

        root.addView(azanCard, cardParams());

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private void refresh() {
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
            locationStatus.setText(
                    "برای محاسبه دقیقه‌به‌دقیقه، یک‌بار موقعیت فعلی را ثبت کنید.");
            locationButton.setText("دریافت موقعیت دقیق فعلی");
        }

        if (!hasLocation) {
            setAllTimes("—:—");
            return;
        }

        PrayerTimeCalculator.Times times = PrayerTimeCalculator.calculate(
                System.currentTimeMillis(),
                AppSettings.prayerLatitude(this),
                AppSettings.prayerLongitude(this),
                TimeZone.getDefault());

        fajrTime.setText(times.fajr());
        sunriseTime.setText(times.sunrise());
        dhuhrTime.setText(times.dhuhr());
        asrTime.setText(times.asr());
        sunsetTime.setText(times.sunset());
        maghribTime.setText(times.maghrib());
        ishaTime.setText(times.isha());
        midnightTime.setText(times.midnight());
    }

    private void setAllTimes(String value) {
        fajrTime.setText(value);
        sunriseTime.setText(value);
        dhuhrTime.setText(value);
        asrTime.setText(value);
        sunsetTime.setText(value);
        maghribTime.setText(value);
        ishaTime.setText(value);
        midnightTime.setText(value);
    }

    private TextView addTimeRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(2), dp(2), dp(2), dp(2));

        TextView name = text(label, 14, AppSettings.textPrimary(this));
        row.addView(name, new LinearLayout.LayoutParams(0, dp(42), 1f));

        TextView time = text("—:—", 16, AppSettings.primaryColor(this));
        time.setGravity(Gravity.CENTER);
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(time, new LinearLayout.LayoutParams(dp(92), dp(42)));

        parent.addView(row, new LinearLayout.LayoutParams(-1, dp(46)));
        return time;
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
            Toast.makeText(
                    this,
                    "بدون دسترسی موقعیت، محاسبه دقیق اوقات شرعی ممکن نیست.",
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
            Toast.makeText(this, "سرویس موقعیت در دسترس نیست.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(
                    this,
                    "مکان دستگاه را روشن کنید و دوباره تلاش کنید.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        locationButton.setEnabled(false);
        locationButton.setText("در حال دریافت موقعیت…");

        final String selectedProvider = provider;
        if (Build.VERSION.SDK_INT >= 30) {
            manager.getCurrentLocation(
                    selectedProvider,
                    null,
                    command -> runOnUiThread(command),
                    location -> handleLocation(location));
            return;
        }

        manager.requestSingleUpdate(
                selectedProvider,
                new LocationListener() {
                    @Override public void onLocationChanged(Location location) {
                        handleLocation(location);
                    }

                    @Override public void onStatusChanged(
                            String provider, int status, Bundle extras) {}

                    @Override public void onProviderEnabled(String provider) {}

                    @Override public void onProviderDisabled(String provider) {}
                },
                Looper.getMainLooper());
    }

    private void handleLocation(Location location) {
        if (location == null) {
            locationButton.setEnabled(true);
            refresh();
            Toast.makeText(
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

        new Thread(() -> resolveLocationName(lat, lon, fallback)).start();
    }

    @SuppressWarnings("deprecation")
    private void resolveLocationName(double lat, double lon, String fallback) {
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
