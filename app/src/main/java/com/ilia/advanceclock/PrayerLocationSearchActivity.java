package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public final class PrayerLocationSearchActivity extends Activity {
    private static final String SEARCH_PREFS = "prayer_location_search_cache";
    private static final String CACHE_QUERY = "query";
    private static final String CACHE_RESPONSE = "response";
    private static final String NOMINATIM_SEARCH =
            "https://nominatim.openstreetmap.org/search";
    private static final String TIMEAPI_COORDINATE =
            "https://timeapi.io/api/timezone/coordinate";
    private static final String OPEN_METEO_FORECAST =
            "https://api.open-meteo.com/v1/forecast";
    private static final Object REQUEST_LOCK = new Object();
    private static long lastRequestStarted;

    private EditText searchInput;
    private Button searchButton;
    private ProgressBar progress;
    private TextView status;
    private LinearLayout results;
    private int searchGeneration;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(18));
        root.setBackgroundColor(AppSettings.background(this));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = text("انتخاب شهر یا روستا", 24, AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        toolbar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(toolbar);

        LinearLayout searchBar = new LinearLayout(this);
        searchBar.setOrientation(LinearLayout.HORIZONTAL);
        searchBar.setGravity(Gravity.CENTER_VERTICAL);
        searchBar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint("نام شهر، روستا، استان یا کشور");
        searchInput.setTextColor(AppSettings.textPrimary(this));
        searchInput.setHintTextColor(AppSettings.textSecondary(this));
        searchInput.setBackgroundResource(R.drawable.bg_field);
        searchInput.setPadding(dp(12), 0, dp(12), 0);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        searchBar.addView(searchInput, new LinearLayout.LayoutParams(0, dp(54), 1f));

        searchButton = new Button(this);
        searchButton.setText("جستجو");
        searchButton.setAllCaps(false);
        searchButton.setTextColor(0xFFFFFFFF);
        searchButton.setBackgroundResource(R.drawable.bg_teal_button);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(dp(88), dp(54));
        buttonParams.setMarginStart(dp(8));
        searchBar.addView(searchButton, buttonParams);
        root.addView(searchBar);

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(12);
        root.addView(progress, progressParams);

        status = text("نام مکان را وارد کنید", 13, AppSettings.textSecondary(this));
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(10), 0, dp(8));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.setFillViewport(true);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        resultScroll.addView(results, new ScrollView.LayoutParams(-1, -2));
        root.addView(resultScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView attribution = text(
                "داده‌های جستجو © مشارکت‌کنندگان OpenStreetMap",
                11,
                AppSettings.textSecondary(this));
        attribution.setGravity(Gravity.CENTER);
        attribution.setPadding(0, dp(6), 0, 0);
        root.addView(attribution, new LinearLayout.LayoutParams(-1, -2));

        searchButton.setOnClickListener(v -> search());
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            boolean keyboardSearch = actionId == EditorInfo.IME_ACTION_SEARCH;
            boolean enter = event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_DOWN;
            if (!keyboardSearch && !enter) return false;
            search();
            return true;
        });

        setContentView(root);
        AppSettings.applyFullscreenInsets(root);
        AppSettings.playFullscreenEnter(this);
        searchInput.requestFocus();
    }

    private void search() {
        String query = searchInput.getText().toString().trim();
        if (query.isEmpty()) {
            status.setText("نام مکان را وارد کنید");
            return;
        }

        List<IranOfflineLocations.Location> offline =
                IranOfflineLocations.search(query, 20);
        if (!offline.isEmpty()) {
            showOfflineResults(offline);
            return;
        }

        if (!hasInternetConnection()) {
            String message = "این مکان در فهرست آفلاین نیست؛ اینترنت را روشن کنید";
            status.setText(message);
            LogoToast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        int generation = ++searchGeneration;
        searchButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        status.setText("در حال جستجو…");
        results.removeAllViews();

        new Thread(() -> {
            SearchResponse response = searchEverywhere(query);
            runOnUiThread(() -> showResults(generation, response));
        }).start();
    }

    private void showOfflineResults(List<IranOfflineLocations.Location> found) {
        progress.setVisibility(View.GONE);
        results.removeAllViews();
        status.setText(found.size() + " نتیجه آفلاین");
        for (IranOfflineLocations.Location location : found) {
            addResultButton(new LocationResult(
                    location.label,
                    location.latitude,
                    location.longitude,
                    "Asia/Tehran"));
        }
    }

    private boolean hasInternetConnection() {
        ConnectivityManager manager = getSystemService(ConnectivityManager.class);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private SearchResponse searchEverywhere(String query) {
        boolean onlineFailed = false;
        try {
            String json = cachedResponse(query);
            if (json == null) {
                json = requestNominatim(query);
                cacheResponse(query, json);
            }
            List<LocationResult> online = parseNominatim(json);
            if (!online.isEmpty()) return new SearchResponse(online, false);
        } catch (IOException | JSONException | NumberFormatException ignored) {
            onlineFailed = true;
        }

        List<LocationResult> platform = searchPlatformGeocoder(query);
        return new SearchResponse(platform, onlineFailed && platform.isEmpty());
    }

    private String requestNominatim(String query) throws IOException {
        waitForPublicServiceRateLimit();
        Uri uri = Uri.parse(NOMINATIM_SEARCH).buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("format", "jsonv2")
                .appendQueryParameter("addressdetails", "1")
                .appendQueryParameter("limit", "20")
                .appendQueryParameter("accept-language", "fa,en")
                .build();
        HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
        connection.setConnectTimeout(12_000);
        connection.setReadTimeout(12_000);
        connection.setRequestProperty(
                "User-Agent", "AdvanceClock/1.0 (Android; com.ilia.advanceclock)");
        connection.setRequestProperty("Accept", "application/json");
        try {
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("Location search HTTP " + statusCode);
            }
            return readFully(connection.getInputStream());
        } finally {
            connection.disconnect();
        }
    }

    private void waitForPublicServiceRateLimit() {
        synchronized (REQUEST_LOCK) {
            long wait = 1_000L - (System.currentTimeMillis() - lastRequestStarted);
            if (wait > 0L) {
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            lastRequestStarted = System.currentTimeMillis();
        }
    }

    private String readFully(InputStream stream) throws IOException {
        StringBuilder value = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) value.append(line);
        }
        return value.toString();
    }

    private List<LocationResult> parseNominatim(String json) throws JSONException {
        JSONArray array = new JSONArray(json);
        ArrayList<LocationResult> parsed = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.getJSONObject(index);
            double latitude = Double.parseDouble(item.getString("lat"));
            double longitude = Double.parseDouble(item.getString("lon"));
            String label = item.optString("display_name", "").trim();
            if (label.isEmpty()) continue;
            JSONObject address = item.optJSONObject("address");
            String countryCode = address == null
                    ? "" : address.optString("country_code", "");
            String timeZoneId = "ir".equalsIgnoreCase(countryCode)
                    ? "Asia/Tehran" : "";
            parsed.add(new LocationResult(label, latitude, longitude, timeZoneId));
        }
        return parsed;
    }

    @SuppressWarnings("deprecation")
    private List<LocationResult> searchPlatformGeocoder(String query) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(query, 20);
            if (addresses == null) return Collections.emptyList();
            ArrayList<LocationResult> found = new ArrayList<>();
            for (Address address : addresses) {
                if (!address.hasLatitude() || !address.hasLongitude()) continue;
                String timeZoneId = "IR".equalsIgnoreCase(address.getCountryCode())
                        ? "Asia/Tehran" : "";
                found.add(new LocationResult(
                        addressLabel(address),
                        address.getLatitude(),
                        address.getLongitude(),
                        timeZoneId));
            }
            return found;
        } catch (IOException | IllegalArgumentException error) {
            return Collections.emptyList();
        }
    }

    private String cachedResponse(String query) {
        android.content.SharedPreferences cache =
                getSharedPreferences(SEARCH_PREFS, MODE_PRIVATE);
        if (!query.equalsIgnoreCase(cache.getString(CACHE_QUERY, ""))) return null;
        String response = cache.getString(CACHE_RESPONSE, "");
        return response == null || response.isEmpty() ? null : response;
    }

    private void cacheResponse(String query, String response) {
        getSharedPreferences(SEARCH_PREFS, MODE_PRIVATE).edit()
                .putString(CACHE_QUERY, query)
                .putString(CACHE_RESPONSE, response)
                .apply();
    }

    private void showResults(int generation, SearchResponse response) {
        if (generation != searchGeneration || isFinishing()) return;
        searchButton.setEnabled(true);
        progress.setVisibility(View.GONE);
        results.removeAllViews();

        if (response.results.isEmpty()) {
            String message = response.connectionFailed
                    ? "اتصال جستجو برقرار نشد؛ اینترنت را روشن و دوباره تلاش کنید"
                    : "مکانی پیدا نشد؛ نام کامل‌تر یا نام کشور را هم وارد کنید";
            status.setText(message);
            if (response.connectionFailed) {
                LogoToast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
            return;
        }

        status.setText(response.results.size() + " نتیجه");
        for (LocationResult location : response.results) addResultButton(location);
    }

    private void addResultButton(LocationResult location) {
        Button row = new Button(this);
        row.setAllCaps(false);
        row.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        row.setText(location.label);
        row.setTextColor(AppSettings.textPrimary(this));
        row.setTextSize(14);
        row.setBackgroundResource(R.drawable.bg_card);
        row.setPadding(dp(14), 0, dp(14), 0);
        row.setOnClickListener(v -> select(location));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(72));
        params.bottomMargin = dp(8);
        results.addView(row, params);
    }

    private void select(LocationResult location) {
        if (isUsableTimeZone(location.timeZoneId)) {
            saveLocation(location, location.timeZoneId);
            return;
        }

        progress.setVisibility(View.VISIBLE);
        status.setText("در حال تشخیص منطقه زمانی شهر…");
        new Thread(() -> {
            String zone = resolveTimeZone(location.latitude, location.longitude);
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (!isUsableTimeZone(zone)) {
                    String message = "منطقه زمانی این مکان تشخیص داده نشد؛ دوباره تلاش کنید";
                    status.setText(message);
                    LogoToast.makeText(this, message, Toast.LENGTH_LONG).show();
                    return;
                }
                saveLocation(location, zone);
            });
        }).start();
    }

    private String resolveTimeZone(double latitude, double longitude) {
        String zone = resolveTimeZoneViaTimeApi(latitude, longitude);
        if (isUsableTimeZone(zone)) return zone;
        zone = resolveTimeZoneViaOpenMeteo(latitude, longitude);
        return isUsableTimeZone(zone) ? zone : "";
    }

    private String resolveTimeZoneViaTimeApi(double latitude, double longitude) {
        HttpURLConnection connection = null;
        try {
            Uri uri = Uri.parse(TIMEAPI_COORDINATE).buildUpon()
                    .appendQueryParameter("latitude", Double.toString(latitude))
                    .appendQueryParameter("longitude", Double.toString(longitude))
                    .build();
            connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(8_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty(
                    "User-Agent", "AdvanceClock/1.0 (Android; com.ilia.advanceclock)");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) return "";
            return new JSONObject(readFully(connection.getInputStream()))
                    .optString("timeZone", "").trim();
        } catch (Exception ignored) {
            return "";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String resolveTimeZoneViaOpenMeteo(double latitude, double longitude) {
        HttpURLConnection connection = null;
        try {
            Uri uri = Uri.parse(OPEN_METEO_FORECAST).buildUpon()
                    .appendQueryParameter("latitude", Double.toString(latitude))
                    .appendQueryParameter("longitude", Double.toString(longitude))
                    .appendQueryParameter("current", "temperature_2m")
                    .appendQueryParameter("timezone", "auto")
                    .appendQueryParameter("forecast_days", "1")
                    .build();
            connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
            connection.setConnectTimeout(8_000);
            connection.setReadTimeout(8_000);
            connection.setRequestProperty("Accept", "application/json");
            connection.setRequestProperty(
                    "User-Agent", "AdvanceClock/1.0 (Android; com.ilia.advanceclock)");
            int code = connection.getResponseCode();
            if (code < 200 || code >= 300) return "";
            return new JSONObject(readFully(connection.getInputStream()))
                    .optString("timezone", "").trim();
        } catch (Exception ignored) {
            return "";
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private boolean isUsableTimeZone(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        String normalized = id.trim();
        TimeZone zone = TimeZone.getTimeZone(normalized);
        if (!"GMT".equals(zone.getID())) return true;
        return "GMT".equalsIgnoreCase(normalized)
                || normalized.toUpperCase(Locale.US).startsWith("GMT+")
                || normalized.toUpperCase(Locale.US).startsWith("GMT-")
                || normalized.startsWith("Etc/GMT");
    }

    private void saveLocation(LocationResult location, String timeZoneId) {
        AppSettings.setPrayerLocation(
                this,
                location.latitude,
                location.longitude,
                location.label,
                timeZoneId);
        setResult(RESULT_OK);
        finish();
    }

    private String addressLabel(Address address) {
        StringBuilder label = new StringBuilder();
        appendPart(label, address.getLocality());
        appendPart(label, address.getSubAdminArea());
        appendPart(label, address.getAdminArea());
        appendPart(label, address.getCountryName());
        if (label.length() == 0) {
            for (int line = 0; line <= address.getMaxAddressLineIndex(); line++) {
                appendPart(label, address.getAddressLine(line));
            }
        }
        if (label.length() == 0) {
            return String.format(
                    Locale.getDefault(),
                    "%.5f, %.5f",
                    address.getLatitude(),
                    address.getLongitude());
        }
        return label.toString();
    }

    private void appendPart(StringBuilder target, String value) {
        if (value == null || value.trim().isEmpty()) return;
        String part = value.trim();
        if (target.toString().contains(part)) return;
        if (target.length() > 0) target.append("، ");
        target.append(part);
    }

    private static final class LocationResult {
        final String label;
        final double latitude;
        final double longitude;
        final String timeZoneId;

        LocationResult(
                String label,
                double latitude,
                double longitude,
                String timeZoneId) {
            this.label = label;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timeZoneId = timeZoneId;
        }
    }

    private static final class SearchResponse {
        final List<LocationResult> results;
        final boolean connectionFailed;

        SearchResponse(List<LocationResult> results, boolean connectionFailed) {
            this.results = results;
            this.connectionFailed = connectionFailed;
        }
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }
}
