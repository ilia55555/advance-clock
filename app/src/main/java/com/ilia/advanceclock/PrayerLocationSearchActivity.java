package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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
    private static final String LEGACY_CACHE_QUERY = "query";
    private static final String LEGACY_CACHE_RESPONSE = "response";
    private static final String CACHE_PREFIX = "response::";
    private static final String TZ_CACHE_PREFIX = "timezone::";
    private static final String NOMINATIM_SEARCH =
            "https://nominatim.openstreetmap.org/search";
    private static final String TIMEAPI_COORDINATE =
            "https://timeapi.io/api/timezone/coordinate";
    private static final String OPEN_METEO_FORECAST =
            "https://api.open-meteo.com/v1/forecast";
    private static final Object REQUEST_LOCK = new Object();
    private static long lastRequestStarted;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private EditText searchInput;
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
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(dp(18), dp(12), dp(18), dp(18));
        root.setBackgroundColor(AppSettings.background(this));

        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        toolbar.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = text(AppString.get(R.string.runtime_text_0441), 24, AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        root.addView(toolbar);

        Button gps = new Button(this);
        gps.setText(AppString.get(R.string.runtime_text_0442));
        gps.setAllCaps(false);
        gps.setTextColor(0xFFFFFFFF);
        gps.setBackgroundResource(R.drawable.bg_orange_button);
        gps.setOnClickListener(v -> {
            setResult(RESULT_OK, new Intent().putExtra("requestGps", true));
            finish();
        });
        LinearLayout.LayoutParams gpsParams = new LinearLayout.LayoutParams(-1, dp(50));
        gpsParams.bottomMargin = dp(8);
        root.addView(gps, gpsParams);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setHint(AppString.get(R.string.runtime_text_0100));
        searchInput.setTextColor(AppSettings.textPrimary(this));
        searchInput.setHintTextColor(AppSettings.textSecondary(this));
        searchInput.setBackgroundResource(R.drawable.bg_field);
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        root.addView(searchInput, new LinearLayout.LayoutParams(-1, dp(56)));

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(dp(30), dp(30));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(10);
        root.addView(progress, progressParams);

        status = text(AppString.get(R.string.runtime_text_0101), 13,
                AppSettings.textSecondary(this));
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(8), 0, dp(8));
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        ScrollView resultScroll = new ScrollView(this);
        resultScroll.setFillViewport(true);
        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        results.setLayoutDirection(AppSettings.layoutDirection(this));
        resultScroll.addView(results, new ScrollView.LayoutParams(-1, -2));
        root.addView(resultScroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        TextView attribution = text(
                AppString.get(R.string.runtime_text_0443),
                11,
                AppSettings.textSecondary(this));
        attribution.setGravity(Gravity.CENTER);
        attribution.setPadding(0, dp(6), 0, 0);
        root.addView(attribution, new LinearLayout.LayoutParams(-1, -2));

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleSearch(false);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        searchInput.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            scheduleSearch(true);
            return true;
        });

        setContentView(root);
        AppSettings.applyFullscreenInsets(root);
        AppSettings.playFullscreenEnter(this);
        searchInput.requestFocus();
    }

    private void scheduleSearch(boolean immediate) {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        final String query = searchInput.getText().toString().trim();
        final int generation = ++searchGeneration;

        if (query.isEmpty()) {
            progress.setVisibility(View.GONE);
            results.removeAllViews();
            status.setText(AppString.get(R.string.runtime_text_0101));
            return;
        }

        List<IranOfflineLocations.Location> offline = IranOfflineLocations.search(query, 20);
        if (!offline.isEmpty()) {
            showOfflineResults(generation, offline);
            return;
        }

        if (query.length() < 2) {
            progress.setVisibility(View.GONE);
            results.removeAllViews();
            status.setText(AppString.get(R.string.runtime_text_0444));
            return;
        }

        progress.setVisibility(View.VISIBLE);
        status.setText(hasInternetConnection()
                ? AppString.get(R.string.runtime_text_0445)
                : AppString.get(R.string.runtime_text_0446));

        pendingSearch = () -> performSearch(query, generation);
        handler.postDelayed(pendingSearch, immediate ? 0L : 450L);
    }

    private void performSearch(String query, int generation) {
        new Thread(() -> {
            SearchResponse response = searchEverywhere(query);
            runOnUiThread(() -> showResults(generation, response));
        }, "PrayerLocationSearch").start();
    }

    private void showOfflineResults(int generation, List<IranOfflineLocations.Location> found) {
        if (generation != searchGeneration) return;
        progress.setVisibility(View.GONE);
        results.removeAllViews();
        status.setText(CalendarUtils.fa(Integer.toString(found.size())) + AppString.get(R.string.runtime_text_0447));
        for (IranOfflineLocations.Location location : found) {
            addResultButton(new LocationResult(
                    location.label(),
                    location.latitude,
                    location.longitude,
                    "Asia/Tehran",
                    "IR",
                    "Iran"));
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
        boolean cached = false;
        boolean onlineFailed = false;
        try {
            String json = cachedResponse(query);
            if (json != null) {
                cached = true;
            } else if (hasInternetConnection()) {
                json = requestNominatim(query);
                cacheResponse(query, json);
            }
            if (json != null) {
                List<LocationResult> parsed = parseNominatim(json);
                if (!parsed.isEmpty()) return new SearchResponse(parsed, false, cached);
            }
        } catch (IOException | JSONException | NumberFormatException ignored) {
            onlineFailed = true;
        }

        List<LocationResult> platform = searchPlatformGeocoder(query);
        return new SearchResponse(platform,
                (onlineFailed || !hasInternetConnection()) && platform.isEmpty(), false);
    }

    private String requestNominatim(String query) throws IOException {
        waitForPublicServiceRateLimit();
        Uri uri = Uri.parse(NOMINATIM_SEARCH).buildUpon()
                .appendQueryParameter("q", query)
                .appendQueryParameter("format", "jsonv2")
                .appendQueryParameter("addressdetails", "1")
                .appendQueryParameter("limit", "20")
                .appendQueryParameter(
                        "accept-language",
                        AppSettings.language(this) + ",en")
                .build();
        HttpURLConnection connection = (HttpURLConnection) new URL(uri.toString()).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
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
            String countryCode = address == null ? "" : address.optString("country_code", "");
            String region = regionFromAddress(address);
            String timeZoneId = OfflineTimeZoneResolver.resolve(
                    countryCode, region, latitude, longitude);
            if (!isUsableTimeZone(timeZoneId)) {
                timeZoneId = cachedResolvedZone(latitude, longitude);
            }
            parsed.add(new LocationResult(
                    label, latitude, longitude, timeZoneId, countryCode, region));
        }
        return parsed;
    }

    private String regionFromAddress(JSONObject address) {
        if (address == null) return "";
        StringBuilder out = new StringBuilder();
        appendRegion(out, address.optString("state", ""));
        appendRegion(out, address.optString("province", ""));
        appendRegion(out, address.optString("region", ""));
        appendRegion(out, address.optString("state_district", ""));
        appendRegion(out, address.optString("county", ""));
        appendRegion(out, address.optString("ISO3166-2-lvl4", ""));
        return out.toString();
    }

    private void appendRegion(StringBuilder out, String value) {
        if (value == null || value.trim().isEmpty()) return;
        if (out.length() > 0) out.append(' ');
        out.append(value.trim());
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
                String countryCode = address.getCountryCode() == null ? "" : address.getCountryCode();
                String region = ((address.getAdminArea() == null ? "" : address.getAdminArea())
                        + " " + (address.getSubAdminArea() == null ? "" : address.getSubAdminArea())).trim();
                String zone = OfflineTimeZoneResolver.resolve(
                        countryCode, region, address.getLatitude(), address.getLongitude());
                if (!isUsableTimeZone(zone)) {
                    zone = cachedResolvedZone(address.getLatitude(), address.getLongitude());
                }
                found.add(new LocationResult(
                        addressLabel(address),
                        address.getLatitude(),
                        address.getLongitude(),
                        zone,
                        countryCode,
                        region));
            }
            return found;
        } catch (IOException | IllegalArgumentException error) {
            return Collections.emptyList();
        }
    }

    private String normalizedQuery(String query) {
        return query.trim().toLowerCase(Locale.ROOT);
    }

    private String cachedResponse(String query) {
        android.content.SharedPreferences cache =
                getSharedPreferences(SEARCH_PREFS, MODE_PRIVATE);
        String multi = cache.getString(cacheKey(query), "");
        return multi == null || multi.isEmpty() ? null : multi;
    }

    private void cacheResponse(String query, String response) {
        getSharedPreferences(SEARCH_PREFS, MODE_PRIVATE).edit()
                .putString(cacheKey(query), response)
                .apply();
    }

    private String cacheKey(String query) {
        return CACHE_PREFIX
                + AppSettings.language(this)
                + "::"
                + normalizedQuery(query);
    }

    private String coordinateKey(double latitude, double longitude) {
        return String.format(Locale.US, TZ_CACHE_PREFIX + "%.4f:%.4f", latitude, longitude);
    }

    private String cachedResolvedZone(double latitude, double longitude) {
        return getSharedPreferences(SEARCH_PREFS, MODE_PRIVATE)
                .getString(coordinateKey(latitude, longitude), "");
    }

    private void cacheResolvedZone(double latitude, double longitude, String zone) {
        if (!isUsableTimeZone(zone)) return;
        getSharedPreferences(SEARCH_PREFS, MODE_PRIVATE).edit()
                .putString(coordinateKey(latitude, longitude), zone)
                .apply();
    }

    private void showResults(int generation, SearchResponse response) {
        if (generation != searchGeneration || isFinishing()) return;
        progress.setVisibility(View.GONE);
        results.removeAllViews();

        if (response.results.isEmpty()) {
            String message = response.connectionFailed
                    ? AppString.get(R.string.runtime_text_0448)
                    : AppString.get(R.string.runtime_text_0449);
            status.setText(message);
            return;
        }

        String count = CalendarUtils.fa(Integer.toString(response.results.size()));
        status.setText(count + (response.fromCache ? AppString.get(R.string.runtime_text_0450) : AppString.get(R.string.runtime_text_0451)));
        for (LocationResult location : response.results) addResultButton(location);
    }

    private void addResultButton(LocationResult location) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(AppSettings.layoutDirection(this));
        row.setBackgroundResource(R.drawable.bg_card);
        row.setPadding(dp(14), 0, dp(8), 0);
        TextView label = text(location.label, 14, AppSettings.textPrimary(this));
        row.addView(label, new LinearLayout.LayoutParams(0, dp(64), 1f));
        boolean added = AppSettings.hasPrayerHorizon(
                this, location.latitude, location.longitude);
        Button toggle = new Button(this);
        toggle.setText(added ? "−" : "+");
        toggle.setTextSize(22);
        toggle.setTextColor(AppSettings.primaryColor(this));
        toggle.setBackgroundResource(R.drawable.bg_soft_button);
        toggle.setOnClickListener(v -> {
            if (AppSettings.hasPrayerHorizon(this, location.latitude, location.longitude)) {
                AppSettings.removePrayerHorizon(this, location.latitude, location.longitude);
                setResult(RESULT_OK);
                scheduleSearch(true);
            } else select(location);
        });
        row.addView(toggle, new LinearLayout.LayoutParams(dp(52), dp(46)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(72));
        params.bottomMargin = dp(8);
        results.addView(row, params);
    }

    private void select(LocationResult location) {
        String offline = OfflineTimeZoneResolver.resolve(
                location.countryCode,
                location.region,
                location.latitude,
                location.longitude);
        if (isUsableTimeZone(offline)) {
            saveLocation(location, offline);
            return;
        }
        if (isUsableTimeZone(location.timeZoneId)) {
            saveLocation(location, location.timeZoneId);
            return;
        }

        if (!hasInternetConnection()) {
            String message = AppString.get(R.string.runtime_text_0452);
            status.setText(message);
            LogoToast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        status.setText(AppString.get(R.string.runtime_text_0453));
        new Thread(() -> {
            String zone = resolveTimeZoneOnline(location.latitude, location.longitude);
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                if (!isUsableTimeZone(zone)) {
                    String message = AppString.get(R.string.runtime_text_0454);
                    status.setText(message);
                    LogoToast.makeText(this, message, Toast.LENGTH_LONG).show();
                    return;
                }
                cacheResolvedZone(location.latitude, location.longitude, zone);
                saveLocation(location, zone);
            });
        }, "PrayerTimezoneLookup").start();
    }

    private String resolveTimeZoneOnline(double latitude, double longitude) {
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
            connection = openJsonConnection(uri.toString());
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
            connection = openJsonConnection(uri.toString());
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

    private HttpURLConnection openJsonConnection(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(8_000);
        connection.setReadTimeout(8_000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty(
                "User-Agent", "AdvanceClock/1.0 (Android; com.ilia.advanceclock)");
        return connection;
    }

    private boolean isUsableTimeZone(String id) {
        if (id == null || id.trim().isEmpty()) return false;
        String normalized = id.trim();
        TimeZone zone = TimeZone.getTimeZone(normalized);
        if (!"GMT".equals(zone.getID())) return true;
        String upper = normalized.toUpperCase(Locale.US);
        return "GMT".equals(upper)
                || upper.startsWith("GMT+")
                || upper.startsWith("GMT-")
                || normalized.startsWith("Etc/GMT");
    }

    private void saveLocation(LocationResult location, String timeZoneId) {
        if (AppSettings.prayerLocationSet(this)) {
            AppSettings.addPrayerHorizon(
                    this, location.label, location.latitude, location.longitude, timeZoneId);
        } else {
            AppSettings.setPrayerLocation(
                    this, location.latitude, location.longitude, location.label, timeZoneId);
        }
        setResult(RESULT_OK);
        status.setText(AppString.get(R.string.runtime_text_0455));
        scheduleSearch(true);
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
        if (target.length() > 0) target.append(AppString.get(R.string.runtime_text_0456));
        target.append(part);
    }

    private static final class LocationResult {
        final String label;
        final double latitude;
        final double longitude;
        final String timeZoneId;
        final String countryCode;
        final String region;

        LocationResult(
                String label,
                double latitude,
                double longitude,
                String timeZoneId,
                String countryCode,
                String region) {
            this.label = label;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timeZoneId = timeZoneId;
            this.countryCode = countryCode == null ? "" : countryCode;
            this.region = region == null ? "" : region;
        }
    }

    private static final class SearchResponse {
        final List<LocationResult> results;
        final boolean connectionFailed;
        final boolean fromCache;

        SearchResponse(List<LocationResult> results, boolean connectionFailed, boolean fromCache) {
            this.results = results;
            this.connectionFailed = connectionFailed;
            this.fromCache = fromCache;
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

    @Override protected void onDestroy() {
        if (pendingSearch != null) handler.removeCallbacks(pendingSearch);
        super.onDestroy();
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }
}
