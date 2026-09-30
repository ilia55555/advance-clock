package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class WorldClockPanelController {
    private static final Locale PERSIAN = new Locale("fa");
    private static final String SEARCH_PREFS = "world_clock_search_cache";
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

    private final Activity host;
    private final LinearLayout list;
    private final TextView referenceSummary;
    private final Button nowButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<ZoneOption> allZones = new ArrayList<>();
    private final ArrayList<ZoneOption> filteredZones = new ArrayList<>();
    private Runnable pendingZoneSearch;
    private int searchGeneration;
    private long referenceMillis;
    private boolean referenceMode;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            renderClocks();
            long now = System.currentTimeMillis();
            handler.postDelayed(this, 60_000L - now % 60_000L + 50L);
        }
    };

    WorldClockPanelController(Activity host, View root) {
        this.host = host;
        list = root.findViewById(R.id.world_clock_list);
        referenceSummary = root.findViewById(R.id.world_reference_summary);
        nowButton = root.findViewById(R.id.world_reference_now);
        buildZoneIndex();

        root.findViewById(R.id.world_fab).setOnClickListener(v -> showAddDialog());
        root.findViewById(R.id.world_pick_reference).setOnClickListener(v -> pickReferenceDate());
        root.findViewById(R.id.world_alarm).setOnClickListener(v ->
                MainActivity.launchCreateEditor(host, AlarmEditorActivity.class));
        nowButton.setOnClickListener(v -> {
            referenceMode = false;
            referenceMillis = 0L;
            renderClocks();
        });
        renderClocks();
    }

    private void buildZoneIndex() {
        Map<String, LinkedHashSet<String>> aliases = new HashMap<>();
        for (String id : TimeZone.getAvailableIDs()) {
            String canonical = android.icu.util.TimeZone.getCanonicalID(id);
            if (canonical == null || canonical.isEmpty()) canonical = id;
            aliases.computeIfAbsent(canonical, ignored -> new LinkedHashSet<>()).add(id);
        }
        for (Map.Entry<String, LinkedHashSet<String>> entry : aliases.entrySet()) {
            String id = entry.getKey();
            String city = cityName(id);
            String continent = continentName(id);
            String countryEnglish = "";
            String countryPersian = "";
            try {
                String region = android.icu.util.TimeZone.getRegion(id);
                if (region != null && !"001".equals(region)) {
                    Locale country = new Locale("", region);
                    countryEnglish = country.getDisplayCountry(Locale.ENGLISH);
                    countryPersian = country.getDisplayCountry(PERSIAN);
                }
            } catch (IllegalArgumentException ignored) {
            }
            String aliasText = android.text.TextUtils.join(" ", entry.getValue());
            String extra = explicitSearchAliases(id);
            String searchText = normalize(id + " " + city + " " + continent + " "
                    + countryEnglish + " " + countryPersian + " " + aliasText + " " + extra);
            String label = city;
            if (!countryPersian.isEmpty()) label += " • " + countryPersian;
            else if (!countryEnglish.isEmpty()) label += " • " + countryEnglish;
            label += " • " + continent;
            allZones.add(new ZoneOption(
                    id, label, searchText, city, Double.NaN, Double.NaN, "", ""));
        }
        allZones.add(new ZoneOption(
                "Asia/Shanghai",
                AppString.get(R.string.runtime_text_0498),
                normalize(AppString.get(R.string.runtime_text_0499)),
                AppString.get(R.string.runtime_text_0500),
                Double.NaN,
                Double.NaN,
                "CN",
                ""));
        Collections.sort(allZones, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void showAddDialog() {
        LinearLayout content = new LinearLayout(host);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));
        content.setBackgroundColor(AppSettings.background(host));

        LinearLayout header = new LinearLayout(host);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER);
        header.setPadding(dp(16), dp(12), dp(16), dp(12));
        header.setBackgroundResource(R.drawable.bg_header);

        TextView title = text(AppString.get(R.string.runtime_text_0103), 19, AppSettings.textPrimary(host));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(0xFFFFFFFF);
        title.setGravity(Gravity.CENTER);
        header.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = text(AppString.get(R.string.runtime_text_0373), 12,
                0xFFD9EFED);
        hint.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(3);
        header.addView(hint, hintParams);
        content.addView(header, new LinearLayout.LayoutParams(-1, dp(86)));

        EditText dialogSearch = new EditText(host);
        dialogSearch.setSingleLine(true);
        dialogSearch.setHint(AppString.get(R.string.runtime_text_0375));
        dialogSearch.setTextColor(AppSettings.textPrimary(host));
        dialogSearch.setHintTextColor(AppSettings.textSecondary(host));
        dialogSearch.setBackgroundResource(R.drawable.bg_field);
        dialogSearch.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(56));
        searchParams.topMargin = dp(12);
        content.addView(dialogSearch, searchParams);

        ProgressBar progress = new ProgressBar(host);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(dp(28), dp(28));
        progressParams.gravity = Gravity.CENTER_HORIZONTAL;
        progressParams.topMargin = dp(6);
        content.addView(progress, progressParams);

        TextView status = text(AppString.get(R.string.runtime_text_0501), 12,
                AppSettings.textSecondary(host));
        status.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(-1, -2);
        statusParams.topMargin = dp(3);
        content.addView(status, statusParams);

        Spinner dialogSpinner = new Spinner(host);
        dialogSpinner.setBackgroundResource(R.drawable.bg_field);
        dialogSpinner.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(-1, dp(58));
        spinnerParams.topMargin = dp(8);
        content.addView(dialogSpinner, spinnerParams);

        Button addButton = new Button(host);
        addButton.setText(AppString.get(R.string.runtime_text_0104));
        addButton.setTextColor(0xFFFFFFFF);
        addButton.setTextSize(15);
        addButton.setAllCaps(false);
        addButton.setBackgroundResource(R.drawable.bg_teal_button);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(56));
        buttonParams.topMargin = dp(10);
        content.addView(addButton, buttonParams);

        Button cancelButton = new Button(host);
        cancelButton.setText(AppString.get(R.string.runtime_text_0003));
        cancelButton.setTextColor(AppSettings.primaryColor(host));
        cancelButton.setTextSize(14);
        cancelButton.setAllCaps(false);
        cancelButton.setBackgroundResource(R.drawable.bg_soft_button);
        LinearLayout.LayoutParams cancelParams = new LinearLayout.LayoutParams(-1, dp(52));
        cancelParams.topMargin = dp(8);
        content.addView(cancelButton, cancelParams);

        AlertDialog dialog = new AlertDialog.Builder(host)
                .setView(content)
                .create();

        dialogSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                scheduleZoneSearch(
                        dialogSpinner,
                        status,
                        progress,
                        s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        showOfflineMatches(dialogSpinner, status, progress, "");
        addButton.setOnClickListener(v ->
                addSelected(dialogSpinner, dialog, addButton, status, progress));
        cancelButton.setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(ignored -> {
            searchGeneration++;
            if (pendingZoneSearch != null) handler.removeCallbacks(pendingZoneSearch);
        });

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(AppSettings.background(host)));
        }
        dialog.show();
        if (window != null) {
            window.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
        }
        ViewGroup.LayoutParams contentParams = content.getLayoutParams();
        if (contentParams != null) {
            contentParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
            contentParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
            content.setLayoutParams(contentParams);
        }
        content.setAlpha(0f);
        content.setTranslationY(dp(16));
        content.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(240L)
                .start();
    }

    private void scheduleZoneSearch(
            Spinner spinner,
            TextView status,
            ProgressBar progress,
            String query) {
        if (pendingZoneSearch != null) handler.removeCallbacks(pendingZoneSearch);
        final int generation = ++searchGeneration;
        final String trimmed = query == null ? "" : query.trim();

        List<ZoneOption> offline = offlineMatches(trimmed);
        if (trimmed.isEmpty() || !offline.isEmpty()) {
            showOptions(spinner, offline, trimmed.isEmpty()
                    ? AppString.get(R.string.runtime_text_0501)
                    : CalendarUtils.fa(Integer.toString(offline.size())) + AppString.get(R.string.runtime_text_0447));
            progress.setVisibility(View.GONE);
            status.setText(trimmed.isEmpty()
                    ? AppString.get(R.string.runtime_text_0501)
                    : CalendarUtils.fa(Integer.toString(offline.size())) + AppString.get(R.string.runtime_text_0447));
            return;
        }

        if (trimmed.length() < 2) {
            showOptions(spinner, Collections.emptyList(), "");
            progress.setVisibility(View.GONE);
            status.setText(AppString.get(R.string.runtime_text_0377));
            return;
        }

        String cached = cachedResponse(trimmed);
        if (cached != null) {
            try {
                List<ZoneOption> cachedResults = parseNominatim(cached);
                if (!cachedResults.isEmpty()) {
                    showOptions(spinner, cachedResults, "");
                    progress.setVisibility(View.GONE);
                    status.setText(CalendarUtils.fa(Integer.toString(cachedResults.size()))
                            + AppString.get(R.string.runtime_text_0450));
                    return;
                }
            } catch (JSONException | NumberFormatException ignored) {
            }
        }

        if (!hasInternetConnection()) {
            showOptions(spinner, Collections.emptyList(), "");
            progress.setVisibility(View.GONE);
            status.setText(AppString.get(R.string.runtime_text_0378));
            return;
        }

        showOptions(spinner, Collections.emptyList(), "");
        progress.setVisibility(View.VISIBLE);
        status.setText(AppString.get(R.string.runtime_text_0376));
        pendingZoneSearch = () -> performOnlineSearch(
                spinner, status, progress, trimmed, generation);
        handler.postDelayed(pendingZoneSearch, 400L);
    }

    private void showOfflineMatches(
            Spinner spinner,
            TextView status,
            ProgressBar progress,
            String query) {
        List<ZoneOption> offline = offlineMatches(query);
        showOptions(spinner, offline, "");
        progress.setVisibility(View.GONE);
        status.setText(AppString.get(R.string.runtime_text_0501));
    }

    private List<ZoneOption> offlineMatches(String query) {
        String normalized = normalize(query);
        ArrayList<ZoneOption> matches = new ArrayList<>();
        for (ZoneOption option : allZones) {
            if (normalized.isEmpty() || option.searchText.contains(normalized)) {
                matches.add(option);
            }
        }
        return matches;
    }

    private void performOnlineSearch(
            Spinner spinner,
            TextView status,
            ProgressBar progress,
            String query,
            int generation) {
        new Thread(() -> {
            ArrayList<ZoneOption> found = new ArrayList<>();
            boolean failed = false;
            try {
                String json = requestNominatim(query);
                cacheResponse(query, json);
                found.addAll(parseNominatim(json));
            } catch (IOException | JSONException | NumberFormatException error) {
                failed = true;
            }

            if (found.isEmpty()) {
                List<ZoneOption> platform = searchPlatformGeocoder(query);
                found.addAll(platform);
            }

            final boolean requestFailed = failed;
            host.runOnUiThread(() -> {
                if (generation != searchGeneration || host.isFinishing()) return;
                progress.setVisibility(View.GONE);
                showOptions(spinner, found, "");
                if (found.isEmpty()) {
                    status.setText(requestFailed
                            ? AppString.get(R.string.runtime_text_0502)
                            : AppString.get(R.string.runtime_text_0449));
                } else {
                    status.setText(CalendarUtils.fa(Integer.toString(found.size()))
                            + AppString.get(R.string.runtime_text_0503));
                }
            });
        }, "WorldClockSearch").start();
    }

    private void showOptions(Spinner spinner, List<ZoneOption> options, String unused) {
        filteredZones.clear();
        filteredZones.addAll(options);
        ArrayList<String> labels = new ArrayList<>();
        for (ZoneOption option : filteredZones) labels.add(option.label);
        if (labels.isEmpty()) labels.add(AppString.get(R.string.runtime_text_0102));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                host, android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void addSelected(
            Spinner spinner,
            AlertDialog dialog,
            Button addButton,
            TextView status,
            ProgressBar progress) {
        int position = spinner.getSelectedItemPosition();
        if (position < 0 || position >= filteredZones.size()) {
            LogoToast.makeText(
                    host,
                    AppString.get(R.string.runtime_text_0374),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        ZoneOption option = filteredZones.get(position);
        String zone = option.id;
        if (!isUsableTimeZone(zone)
                && !Double.isNaN(option.latitude)
                && !Double.isNaN(option.longitude)) {
            zone = OfflineTimeZoneResolver.resolve(
                    option.countryCode,
                    option.region,
                    option.latitude,
                    option.longitude);
        }

        if (isUsableTimeZone(zone)) {
            saveSelectedZone(zone, option);
            dialog.dismiss();
            return;
        }

        if (!hasInternetConnection()) {
            String message =
                    AppString.get(R.string.runtime_text_0504);
            status.setText(message);
            LogoToast.makeText(host, message, Toast.LENGTH_LONG).show();
            return;
        }

        if (Double.isNaN(option.latitude) || Double.isNaN(option.longitude)) {
            LogoToast.makeText(host, AppString.get(R.string.runtime_text_0505), Toast.LENGTH_SHORT).show();
            return;
        }

        addButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        status.setText(AppString.get(R.string.runtime_text_0453));
        new Thread(() -> {
            String resolved = resolveTimeZoneOnline(option.latitude, option.longitude);
            host.runOnUiThread(() -> {
                addButton.setEnabled(true);
                progress.setVisibility(View.GONE);
                if (!isUsableTimeZone(resolved)) {
                    String message = AppString.get(R.string.runtime_text_0454);
                    status.setText(message);
                    LogoToast.makeText(host, message, Toast.LENGTH_LONG).show();
                    return;
                }
                cacheResolvedZone(option.latitude, option.longitude, resolved);
                saveSelectedZone(resolved, option);
                dialog.dismiss();
            });
        }, "WorldClockTimezoneLookup").start();
    }

    private void saveSelectedZone(String zone, ZoneOption option) {
        List<String> zones = WorldClockStore.zones(host);
        if (zones.contains(zone)) {
            LogoToast.makeText(host, AppString.get(R.string.runtime_text_0379), Toast.LENGTH_SHORT).show();
            return;
        }
        zones.add(zone);
        if (option.cityLabel != null && !option.cityLabel.trim().isEmpty()) {
            WorldClockStore.saveLabel(host, zone, option.cityLabel);
        }
        WorldClockStore.save(host, zones);
        renderClocks();
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
        HttpURLConnection connection =
                (HttpURLConnection) new URL(uri.toString()).openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty(
                "User-Agent", "AdvanceClock/1.0 (Android; com.ilia.advanceclock)");
        connection.setRequestProperty("Accept", "application/json");
        try {
            int statusCode = connection.getResponseCode();
            if (statusCode < 200 || statusCode >= 300) {
                throw new IOException("World clock search HTTP " + statusCode);
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

    private List<ZoneOption> parseNominatim(String json)
            throws JSONException, NumberFormatException {
        JSONArray array = new JSONArray(json);
        ArrayList<ZoneOption> parsed = new ArrayList<>();
        LinkedHashSet<String> seen = new LinkedHashSet<>();

        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.getJSONObject(index);
            double latitude = Double.parseDouble(item.getString("lat"));
            double longitude = Double.parseDouble(item.getString("lon"));
            String display = item.optString("display_name", "").trim();
            if (display.isEmpty()) continue;

            JSONObject address = item.optJSONObject("address");
            String city = cityFromAddress(address, display);
            String country = address == null ? "" : address.optString("country", "").trim();
            String countryCode =
                    address == null ? "" : address.optString("country_code", "").trim();
            String region = regionFromAddress(address);
            String zone = OfflineTimeZoneResolver.resolve(
                    countryCode, region, latitude, longitude);
            if (!isUsableTimeZone(zone)) {
                zone = cachedResolvedZone(latitude, longitude);
            }

            String key = normalize(city + "|" + country + "|" + zone);
            if (!seen.add(key)) continue;

            String label = city;
            if (!country.isEmpty() && !normalize(country).equals(normalize(city))) {
                label += " • " + country;
            }
            parsed.add(new ZoneOption(
                    zone,
                    label,
                    normalize(display + " " + city + " " + country + " " + zone),
                    city,
                    latitude,
                    longitude,
                    countryCode,
                    region));
        }
        return parsed;
    }

    @SuppressWarnings("deprecation")
    private List<ZoneOption> searchPlatformGeocoder(String query) {
        try {
            Geocoder geocoder = new Geocoder(host, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocationName(query, 20);
            if (addresses == null) return Collections.emptyList();

            ArrayList<ZoneOption> found = new ArrayList<>();
            LinkedHashSet<String> seen = new LinkedHashSet<>();
            for (Address address : addresses) {
                if (!address.hasLatitude() || !address.hasLongitude()) continue;

                String city = firstNonEmpty(
                        address.getLocality(),
                        address.getSubAdminArea(),
                        address.getAdminArea(),
                        query);
                String country = address.getCountryName() == null
                        ? "" : address.getCountryName();
                String countryCode = address.getCountryCode() == null
                        ? "" : address.getCountryCode();
                String region = ((address.getAdminArea() == null ? "" : address.getAdminArea())
                        + " "
                        + (address.getSubAdminArea() == null ? "" : address.getSubAdminArea()))
                        .trim();
                String zone = OfflineTimeZoneResolver.resolve(
                        countryCode,
                        region,
                        address.getLatitude(),
                        address.getLongitude());
                if (!isUsableTimeZone(zone)) {
                    zone = cachedResolvedZone(address.getLatitude(), address.getLongitude());
                }

                String key = normalize(city + "|" + country + "|" + zone);
                if (!seen.add(key)) continue;
                String label = city + (country.isEmpty() ? "" : " • " + country);
                found.add(new ZoneOption(
                        zone,
                        label,
                        normalize(label + " " + zone),
                        city,
                        address.getLatitude(),
                        address.getLongitude(),
                        countryCode,
                        region));
            }
            return found;
        } catch (IOException | IllegalArgumentException error) {
            return Collections.emptyList();
        }
    }

    private String cityFromAddress(JSONObject address, String display) {
        if (address != null) {
            String city = firstNonEmpty(
                    address.optString("city", ""),
                    address.optString("town", ""),
                    address.optString("village", ""),
                    address.optString("municipality", ""),
                    address.optString("hamlet", ""),
                    address.optString("county", ""),
                    address.optString("state", ""));
            if (!city.isEmpty()) return city;
        }
        int comma = display.indexOf(',');
        return comma > 0 ? display.substring(0, comma).trim() : display;
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

    private String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private boolean hasInternetConnection() {
        ConnectivityManager manager = host.getSystemService(ConnectivityManager.class);
        if (manager == null) return false;
        Network network = manager.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private String cachedResponse(String query) {
        String cached = host.getSharedPreferences(SEARCH_PREFS, Activity.MODE_PRIVATE)
                .getString(CACHE_PREFIX + normalize(query), "");
        return cached == null || cached.isEmpty() ? null : cached;
    }

    private void cacheResponse(String query, String response) {
        host.getSharedPreferences(SEARCH_PREFS, Activity.MODE_PRIVATE).edit()
                .putString(CACHE_PREFIX + normalize(query), response)
                .apply();
    }

    private String coordinateKey(double latitude, double longitude) {
        return String.format(
                Locale.US,
                TZ_CACHE_PREFIX + "%.4f:%.4f",
                latitude,
                longitude);
    }

    private String cachedResolvedZone(double latitude, double longitude) {
        return host.getSharedPreferences(SEARCH_PREFS, Activity.MODE_PRIVATE)
                .getString(coordinateKey(latitude, longitude), "");
    }

    private void cacheResolvedZone(double latitude, double longitude, String zone) {
        if (!isUsableTimeZone(zone)) return;
        host.getSharedPreferences(SEARCH_PREFS, Activity.MODE_PRIVATE).edit()
                .putString(coordinateKey(latitude, longitude), zone)
                .apply();
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

    private void pickReferenceDate() {
        long initial = referenceMode ? referenceMillis : System.currentTimeMillis();
        CalendarPickerDialog.showDateAny(host, initial, AppSettings.defaultCalendar(host),
                (picked, calendarType) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.setTimeInMillis(picked);
                    Calendar initialTime = Calendar.getInstance();
                    initialTime.setTimeInMillis(initial);
                    selected.set(Calendar.HOUR_OF_DAY, initialTime.get(Calendar.HOUR_OF_DAY));
                    selected.set(Calendar.MINUTE, initialTime.get(Calendar.MINUTE));
                    new TimePickerDialog(host, (view, hour, minute) -> {
                        selected.set(Calendar.HOUR_OF_DAY, hour);
                        selected.set(Calendar.MINUTE, minute);
                        selected.set(Calendar.SECOND, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        referenceMillis = selected.getTimeInMillis();
                        referenceMode = true;
                        renderClocks();
                    }, initialTime.get(Calendar.HOUR_OF_DAY),
                            initialTime.get(Calendar.MINUTE), true).show();
                });
    }

    private void renderClocks() {
        list.removeAllViews();
        List<String> zones = WorldClockStore.zones(host);
        long shownMillis = referenceMode ? referenceMillis : System.currentTimeMillis();
        referenceSummary.setText(referenceMode
                ? AppString.get(R.string.runtime_text_0506) + localDateTime(shownMillis)
                : AppString.get(R.string.runtime_text_0507));
        nowButton.setVisibility(referenceMode ? View.VISIBLE : View.GONE);

        for (String zoneId : zones) {
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            row.setBackgroundResource(R.drawable.bg_card);

            String displayCity =
                    WorldClockStore.label(host, zoneId, cityName(zoneId));
            LinearLayout details = new LinearLayout(host);
            details.setOrientation(LinearLayout.VERTICAL);
            TextView name = text(displayCity, 16, AppSettings.textPrimary(host));
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            details.addView(name);
            TextView geography = text(geography(zoneId), 11, AppSettings.textSecondary(host));
            details.addView(geography);
            row.addView(details, new LinearLayout.LayoutParams(0, dp(58), 1f));

            TimeZone zone = TimeZone.getTimeZone(zoneId);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeFormat.setTimeZone(zone);
            SimpleDateFormat dateFormat =
                    new SimpleDateFormat(AppString.get(R.string.runtime_text_0508), Locale.getDefault());
            dateFormat.setTimeZone(zone);
            LinearLayout converted = new LinearLayout(host);
            converted.setOrientation(LinearLayout.VERTICAL);
            converted.setGravity(Gravity.CENTER);
            TextView time = text(
                    timeFormat.format(new Date(shownMillis)),
                    23,
                    AppSettings.primaryColor(host));
            time.setGravity(Gravity.CENTER);
            converted.addView(time);
            TextView date = text(
                    dateFormat.format(new Date(shownMillis)),
                    11,
                    AppSettings.textSecondary(host));
            date.setGravity(Gravity.CENTER);
            converted.addView(date);
            row.addView(converted, new LinearLayout.LayoutParams(dp(122), dp(58)));

            ImageButton remove = new ImageButton(host);
            remove.setImageResource(R.drawable.ic_delete_red);
            remove.setBackgroundResource(R.drawable.bg_delete_outline);
            remove.setContentDescription(AppString.get(R.string.runtime_text_0509) + displayCity);
            remove.setPadding(dp(5), dp(5), dp(5), dp(5));
            boolean[] deleteArmed = {false};
            remove.setOnClickListener(v -> {
                if (!deleteArmed[0]) {
                    deleteArmed[0] = true;
                    remove.setImageResource(R.drawable.ic_md_delete);
                    remove.setBackgroundResource(R.drawable.bg_delete_confirm);
                    remove.setContentDescription(AppString.get(R.string.runtime_text_0510) + displayCity);
                    LogoToast.makeText(
                            host,
                            AppString.get(R.string.runtime_text_0511),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                removeZone(zoneId);
            });
            row.addView(remove, new LinearLayout.LayoutParams(dp(20), dp(20)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            list.addView(row, lp);
        }
    }

    private void removeZone(String zoneId) {
        List<String> updated = WorldClockStore.zones(host);
        if (updated.size() == 1) {
            LogoToast.makeText(
                    host,
                    AppString.get(R.string.runtime_text_0380),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        updated.remove(zoneId);
        WorldClockStore.removeLabel(host, zoneId);
        WorldClockStore.save(host, updated);
        renderClocks();
    }

    private String geography(String id) {
        String country = "";
        try {
            String region = android.icu.util.TimeZone.getRegion(id);
            if (region != null && !"001".equals(region)) {
                country = new Locale("", region).getDisplayCountry(PERSIAN);
            }
        } catch (IllegalArgumentException ignored) {
        }
        String continent = continentName(id);
        return country.isEmpty() ? continent : country + " • " + continent;
    }

    private String cityName(String id) {
        if ("America/Regina".equals(id) || "Canada/Saskatchewan".equals(id)) return "Regina";
        int slash = id.lastIndexOf('/');
        return (slash >= 0 ? id.substring(slash + 1) : id).replace('_', ' ');
    }

    private String continentName(String id) {
        String prefix = id.contains("/") ? id.substring(0, id.indexOf('/')) : id;
        switch (prefix) {
            case "Asia": return AppString.get(R.string.runtime_text_0512);
            case "Europe": return AppString.get(R.string.runtime_text_0513);
            case "Africa": return AppString.get(R.string.runtime_text_0514);
            case "America": return AppString.get(R.string.runtime_text_0515);
            case "Australia": return AppString.get(R.string.runtime_text_0516);
            case "Pacific": return AppString.get(R.string.runtime_text_0517);
            case "Atlantic": return AppString.get(R.string.runtime_text_0518);
            case "Indian": return AppString.get(R.string.runtime_text_0519);
            case "Antarctica": return AppString.get(R.string.runtime_text_0520);
            default: return prefix;
        }
    }

    private String explicitSearchAliases(String id) {
        if ("America/Regina".equals(id)) {
            return AppString.get(R.string.runtime_text_0521);
        }
        if ("Asia/Tehran".equals(id)) {
            return AppString.get(R.string.runtime_text_0522);
        }
        if ("Asia/Kuwait".equals(id)) {
            return AppString.get(R.string.runtime_text_0523);
        }
        if ("Asia/Shanghai".equals(id)) {
            return AppString.get(R.string.runtime_text_0524);
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('ي', 'ی')
                .replace('ك', 'ک');
    }

    private String localDateTime(long millis) {
        return new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
                .format(new Date(millis));
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(host);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    void onResume() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    void onPause() {
        handler.removeCallbacks(ticker);
    }

    private int dp(int value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }

    private static final class ZoneOption {
        final String id;
        final String label;
        final String searchText;
        final String cityLabel;
        final double latitude;
        final double longitude;
        final String countryCode;
        final String region;

        ZoneOption(
                String id,
                String label,
                String searchText,
                String cityLabel,
                double latitude,
                double longitude,
                String countryCode,
                String region) {
            this.id = id == null ? "" : id;
            this.label = label == null ? "" : label;
            this.searchText = searchText == null ? "" : searchText;
            this.cityLabel = cityLabel == null ? "" : cityLabel;
            this.latitude = latitude;
            this.longitude = longitude;
            this.countryCode = countryCode == null ? "" : countryCode;
            this.region = region == null ? "" : region;
        }
    }
}
