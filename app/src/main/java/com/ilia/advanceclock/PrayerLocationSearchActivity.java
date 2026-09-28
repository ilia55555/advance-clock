package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.location.Address;
import android.location.Geocoder;
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

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class PrayerLocationSearchActivity extends Activity {
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

        int generation = ++searchGeneration;
        searchButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        status.setText("در حال جستجو…");
        results.removeAllViews();

        new Thread(() -> {
            List<Address> found;
            try {
                Geocoder geocoder = new Geocoder(
                        PrayerLocationSearchActivity.this, Locale.getDefault());
                List<Address> response = geocoder.getFromLocationName(query, 20);
                found = response == null ? Collections.emptyList() : response;
            } catch (IOException | IllegalArgumentException error) {
                found = Collections.emptyList();
            }
            List<Address> finalFound = found;
            runOnUiThread(() -> showResults(generation, finalFound));
        }).start();
    }

    private void showResults(int generation, List<Address> found) {
        if (generation != searchGeneration || isFinishing()) return;
        searchButton.setEnabled(true);
        progress.setVisibility(View.GONE);
        results.removeAllViews();

        if (found.isEmpty()) {
            status.setText("مکانی پیدا نشد؛ نام کامل‌تر یا نام کشور را هم وارد کنید");
            return;
        }

        status.setText(found.size() + " نتیجه");
        for (Address address : found) {
            String label = addressLabel(address);
            Button row = new Button(this);
            row.setAllCaps(false);
            row.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
            row.setText(label);
            row.setTextColor(AppSettings.textPrimary(this));
            row.setTextSize(14);
            row.setBackgroundResource(R.drawable.bg_card);
            row.setPadding(dp(14), 0, dp(14), 0);
            row.setOnClickListener(v -> select(address, label));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(62));
            params.bottomMargin = dp(8);
            results.addView(row, params);
        }
    }

    private void select(Address address, String label) {
        if (!address.hasLatitude() || !address.hasLongitude()) return;
        AppSettings.setPrayerLocation(
                this, address.getLatitude(), address.getLongitude(), label);
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
            return String.format(Locale.getDefault(), "%.5f, %.5f",
                    address.getLatitude(), address.getLongitude());
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
