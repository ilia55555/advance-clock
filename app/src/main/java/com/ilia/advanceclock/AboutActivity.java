package com.ilia.advanceclock;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
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
        int side = 18 + Math.max(0, (getResources().getConfiguration().screenWidthDp - 680) / 2);
        root.setPadding(dp(side), dp(12), dp(side), dp(32));
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

                action(root, R.string.about_credits_title, () -> openDocument("credits"));
                action(root, R.string.about_app_settings, this::openAppSettings);

                break;
            case "terms":
                sections(root, new int[][]{
                        {R.string.about_terms_use_title, R.string.about_terms_use_body},
                        {R.string.about_terms_reliability_title, R.string.about_terms_reliability_body},
                        {R.string.about_terms_external_title, R.string.about_terms_external_body},
                        {R.string.about_terms_changes_title, R.string.about_terms_changes_body}});
                action(root, R.string.about_privacy_title, () -> openDocument("privacy"));

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
        int accent = AppSettings.primaryColor(this);
        LinearLayout brand = card(root);
        brand.setGravity(Gravity.CENTER_HORIZONTAL);
        brand.setPadding(dp(24), dp(28), dp(24), dp(24));
        GradientDrawable hero = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{blend(AppSettings.surface(this), accent, .16f),
                        blend(AppSettings.surface(this), AppSettings.secondaryColor(this), .07f)});
        hero.setCornerRadius(dp(28));
        hero.setStroke(dp(1), blend(AppSettings.surface(this), accent, .22f));
        brand.setBackground(hero);
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(getApplicationInfo().loadIcon(getPackageManager()));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setBackground(shape(AppSettings.surface(this), 28));
        icon.setElevation(dp(3));
        brand.addView(icon, new LinearLayout.LayoutParams(dp(100), dp(100)));
        TextView name = text(getString(R.string.app_name), 27, true);
        name.setPadding(0, dp(18), 0, dp(10));
        name.setGravity(Gravity.CENTER);
        brand.addView(name, new LinearLayout.LayoutParams(-1, -2));
        TextView version = text(versionLabel(), 13, true);
        version.setTextColor(AppSettings.textPrimary(this));
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(14), dp(6), dp(14), dp(6));
        version.setBackground(shape(blend(AppSettings.surface(this), accent, .12f), 40));
        brand.addView(version, new LinearLayout.LayoutParams(-2, -2));
        TextView summary = text(getString(R.string.about_summary), 15, false);
        summary.setTextColor(AppSettings.textSecondary(this));
        summary.setGravity(Gravity.CENTER);
        summary.setLineSpacing(dp(5), 1f);
        summary.setPadding(0, dp(18), 0, 0);
        brand.addView(summary, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout developer = card(root);
        developer.setOrientation(LinearLayout.HORIZONTAL);
        developer.setGravity(Gravity.CENTER_VERTICAL);
        developer.addView(symbol(R.drawable.ic_about_person), new LinearLayout.LayoutParams(dp(44), dp(44)));
        TextView author = text(getString(R.string.about_developer_label,
                getString(R.string.about_developer_name)), 16, true);
        author.setPaddingRelative(dp(14), dp(4), 0, dp(4));
        developer.addView(author, new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout links = card(root);
        links.setPadding(dp(8), dp(6), dp(8), dp(6));
        action(links, R.string.about_privacy_title, () -> openDocument("privacy"));
        divider(links);
        action(links, R.string.about_terms_title, () -> openDocument("terms"));
        divider(links);
        action(links, R.string.about_permissions_title, () -> openDocument("permissions"));
        divider(links);
        action(links, R.string.about_credits_title, () -> openDocument("credits"));

    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private static int blend(int base, int accent, float amount) {
        return Color.rgb(Math.round(Color.red(base) * (1 - amount) + Color.red(accent) * amount),
                Math.round(Color.green(base) * (1 - amount) + Color.green(accent) * amount),
                Math.round(Color.blue(base) * (1 - amount) + Color.blue(accent) * amount));
    }

    private ImageView symbol(int resource) {
        ImageView icon = new ImageView(this);
        icon.setImageResource(resource);
        icon.setColorFilter(AppSettings.primaryColor(this), PorterDuff.Mode.SRC_IN);
        icon.setPadding(dp(10), dp(10), dp(10), dp(10));
        icon.setBackground(shape(blend(AppSettings.surface(this), AppSettings.primaryColor(this), .10f), 14));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return icon;
    }

    private void divider(LinearLayout root) {
        View line = new View(this);
        line.setBackgroundColor(blend(AppSettings.surface(this), AppSettings.textSecondary(this), .16f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(1));
        lp.setMarginStart(dp(70));
        lp.setMarginEnd(dp(12));
        root.addView(line, lp);
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
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(68));
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackground(new RippleDrawable(ColorStateList.valueOf(
                blend(AppSettings.surface(this), AppSettings.primaryColor(this), .18f)),
                shape(AppSettings.surface(this), 16), null));
        row.setFocusable(true);
        row.setContentDescription(getString(label));
        row.setOnClickListener(v -> action.run());
        int icon = R.drawable.ic_about_document;
        if (label == R.string.about_privacy_title || label == R.string.about_public_policy)
            icon = R.drawable.ic_about_shield;
        else if (label == R.string.about_permissions_title || label == R.string.about_app_settings)
            icon = R.drawable.ic_md_settings;
        else if (label == R.string.about_support) icon = R.drawable.ic_about_support;
        else if (label == R.string.about_credits_title) icon = R.drawable.ic_about_sources;
        row.addView(symbol(icon), new LinearLayout.LayoutParams(dp(42), dp(42)));
        TextView caption = text(getString(label), 15, true);
        caption.setPaddingRelative(dp(14), 0, dp(10), 0);
        caption.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(caption, new LinearLayout.LayoutParams(0, -2, 1));
        ImageView arrow = new ImageView(this);
        arrow.setImageResource(R.drawable.ic_about_chevron);
        arrow.setColorFilter(AppSettings.textSecondary(this), PorterDuff.Mode.SRC_IN);
        if (AppSettings.layoutDirection(this) == View.LAYOUT_DIRECTION_RTL) arrow.setScaleX(-1);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(20), dp(20)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        if (root == scroll.getChildAt(0)) lp.topMargin = dp(10);
        root.addView(row, lp);
    }

    private String versionLabel() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            return info.versionName;
        } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
            return getString(R.string.about_version_unknown);
        }
    }

    private void openDocument(String document) {
        startActivity(new Intent(this, AboutActivity.class).putExtra("document", document));
    }

    private void openLink(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (ActivityNotFoundException | SecurityException ignored) {
            LogoToast.makeText(this, R.string.about_link_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void openAppSettings() {
        try { startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null))); }
        catch (ActivityNotFoundException | SecurityException ignored) {
            LogoToast.makeText(this, R.string.about_link_error, Toast.LENGTH_SHORT).show();
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
