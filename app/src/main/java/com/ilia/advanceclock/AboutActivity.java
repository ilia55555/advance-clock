package com.ilia.advanceclock;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Offline about/legal reader. External pages open only when the user selects a link. */
public final class AboutActivity extends Activity {
    private ScrollView scroll;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(state);
        String document = getIntent().getStringExtra("document");
        if (document == null) document = "";
        int title = R.string.about_title;
        switch (document) {
            case "privacy": title = R.string.about_privacy_title; break;
            case "terms": title = R.string.about_terms_title; break;
            case "permissions": title = R.string.about_permissions_title; break;
            case "credits": title = R.string.about_credits_title; break;
            case "unicode": title = R.string.about_unicode_license; break;
            case "qamari": title = R.string.about_qamari_license; break;
        }
        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(dp(18), dp(12), dp(18), dp(24));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        header(root, title);
        switch (document) {
            case "privacy":
                sections(root, new int[][]{
                        {R.string.about_privacy_intro_title, R.string.about_privacy_intro_body},
                        {R.string.about_privacy_local_title, R.string.about_privacy_local_body},
                        {R.string.about_privacy_location_title, R.string.about_privacy_location_body},
                        {R.string.about_privacy_providers_title, R.string.about_privacy_providers_body},
                        {R.string.about_privacy_media_title, R.string.about_privacy_media_body},
                        {R.string.about_privacy_security_title, R.string.about_privacy_security_body},
                        {R.string.about_privacy_delete_title, R.string.about_privacy_delete_body},
                        {R.string.about_privacy_contact_title, R.string.about_privacy_contact_body}});
                action(root, R.string.about_public_policy, () -> openPublicPolicy());
                action(root, R.string.about_credits_title, () -> openDocument("credits"));
                action(root, R.string.about_app_settings, this::openAppSettings);
                action(root, R.string.about_support, () -> openLink(getString(R.string.about_support_url)));
                break;
            case "terms":
                sections(root, new int[][]{
                        {R.string.about_terms_use_title, R.string.about_terms_use_body},
                        {R.string.about_terms_reliability_title, R.string.about_terms_reliability_body},
                        {R.string.about_terms_external_title, R.string.about_terms_external_body},
                        {R.string.about_terms_changes_title, R.string.about_terms_changes_body}});
                action(root, R.string.about_privacy_title, () -> openDocument("privacy"));
                action(root, R.string.about_support, () -> openLink(getString(R.string.about_support_url)));
                break;
            case "permissions":
                sections(root, new int[][]{
                        {R.string.about_permissions_alerts_title, R.string.about_permissions_alerts_body},
                        {R.string.about_permissions_location_title, R.string.about_permissions_location_body},
                        {R.string.about_permissions_media_title, R.string.about_permissions_media_body},
                        {R.string.about_permissions_network_title, R.string.about_permissions_network_body}});
                action(root, R.string.about_app_settings, this::openAppSettings);
                action(root, R.string.about_privacy_title, () -> openDocument("privacy"));
                break;
            case "credits":
                body(card(root), getString(R.string.about_credits_body));
                action(root, R.string.about_unicode_license, () -> openDocument("unicode"));
                action(root, R.string.about_qamari_license, () -> openDocument("qamari"));
                action(root, R.string.about_osm_attribution, () -> openLink("https://www.openstreetmap.org/copyright"));
                action(root, R.string.about_osm_privacy, () -> openLink("https://osmfoundation.org/wiki/Privacy_Policy"));
                action(root, R.string.about_timeapi_privacy, () -> openLink("https://www.timeapi.io/Home/Privacy"));
                action(root, R.string.about_openmeteo_terms, () -> openLink("https://open-meteo.com/en/terms"));
                break;
            case "unicode": body(card(root), readLicense("legal/unicode-LICENSE.txt")); break;
            case "qamari": body(card(root), readLicense("legal/qamari-CC0.txt")); break;
            default: showAbout(root);
        }
        if (document.equals("privacy") || document.equals("terms")) {
            TextView updated = text(getString(R.string.about_updated,
                    CalendarUtils.fa(getString(R.string.about_policy_date))), 13, false);
            updated.setTextColor(AppSettings.textSecondary(this));
            updated.setPadding(0, dp(18), 0, 0);
            root.addView(updated);
        }
        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
        if (state != null) {
            int savedScroll = state.getInt("aboutScroll", 0);
            scroll.post(() -> scroll.scrollTo(0, savedScroll));
        }
    }

    private void showAbout(LinearLayout root) {
        LinearLayout brand = card(root);
        brand.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        brand.addView(icon, new LinearLayout.LayoutParams(dp(88), dp(88)));
        TextView name = text(getString(R.string.app_name), 25, true);
        name.setPadding(0, dp(10), 0, dp(4));
        name.setGravity(Gravity.CENTER);
        brand.addView(name);
        TextView version = text(versionLabel(), 13, false);
        version.setTextColor(AppSettings.textSecondary(this));
        version.setGravity(Gravity.CENTER);
        brand.addView(version);
        TextView developer = text(getString(R.string.about_developer_label,
                getString(R.string.about_developer_name)), 16, false);
        developer.setPadding(0, dp(12), 0, 0);
        developer.setGravity(Gravity.CENTER);
        brand.addView(developer);
        body(card(root), getString(R.string.about_summary));
        LinearLayout links = card(root);
        action(links, R.string.about_privacy_title, () -> openDocument("privacy"));
        action(links, R.string.about_terms_title, () -> openDocument("terms"));
        action(links, R.string.about_permissions_title, () -> openDocument("permissions"));
        action(links, R.string.about_credits_title, () -> openDocument("credits"));
        action(links, R.string.about_support, () -> openLink(getString(R.string.about_support_url)));
    }

    private void header(LinearLayout root, int titleResource) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0);
        close.setContentDescription(getString(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        row.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text(getString(titleResource), 23, true);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        row.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(row, new LinearLayout.LayoutParams(-1, -2));
    }

    private void sections(LinearLayout root, int[][] resources) {
        for (int[] section : resources) {
            LinearLayout card = card(root);
            TextView heading = text(getString(section[0]), 17, true);
            heading.setTextColor(AppSettings.primaryColor(this));
            if (Build.VERSION.SDK_INT >= 28) heading.setAccessibilityHeading(true);
            card.addView(heading);
            body(card, getString(section[1]));
        }
    }

    private LinearLayout card(LinearLayout root) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16);
        root.addView(card, lp);
        return card;
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(AppSettings.textPrimary(this));
        view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private void body(LinearLayout root, String value) {
        TextView view = text(value, 15, false);
        view.setTextIsSelectable(true);
        view.setLineSpacing(dp(3), 1f);
        view.setPadding(0, dp(8), 0, 0);
        root.addView(view, new LinearLayout.LayoutParams(-1, -2));
    }

    private void action(LinearLayout root, int label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        button.setMinHeight(dp(52));
        button.setPadding(dp(12), dp(10), dp(12), dp(10));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        root.addView(button, lp);
    }

    private String versionLabel() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            return getString(R.string.about_version_value, info.versionName, code);
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
            return getString(R.string.about_version_unknown);
        }
    }

    private void openDocument(String document) {
        startActivity(new Intent(this, AboutActivity.class).putExtra("document", document));
    }

    private void openPublicPolicy() {
        String language = AppSettings.language(this);
        String anchor = "fa".equals(language) ? "persian" : "ar".equals(language) ? "arabic" : "english";
        openLink(getString(R.string.about_privacy_url) + "#" + anchor);
    }

    private void openLink(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException | SecurityException ignored) {
            Toast.makeText(this, R.string.about_link_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppSettings() {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null))); }
        catch (ActivityNotFoundException | SecurityException ignored) {
            Toast.makeText(this, R.string.about_link_error, Toast.LENGTH_SHORT).show();
        }
    }

    private String readLicense(String asset) {
        StringBuilder out = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                getAssets().open(asset), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) out.append(line).append('\n');
            return out.toString();
        } catch (IOException ignored) { return getString(R.string.about_license_error); }
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("aboutScroll", scroll.getScrollY());
        super.onSaveInstanceState(out);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
