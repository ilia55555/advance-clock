package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Shown once after the first permission flow when adhan is on: the full adhan
 * settings from Settings, with "Later" (keep defaults) and "Done".
 */
public final class AdhanSetupActivity extends Activity {
    static final String PREFS = "advance_clock_app";
    static final String KEY_PROMPTED = "adhan_setup_prompted";
    private PrayerSettingsPanel panel;

    /** True once per install, only while the adhan master switch is on. */
    static boolean shouldPrompt(Activity host) {
        boolean anyAdhan = false;
        for (int t = AdhanScheduler.FAJR; t <= AdhanScheduler.ISHA; t++) anyAdhan |= AdhanScheduler.isEnabled(host, t);
        return (AppSettings.showPrayerTimes(host) || anyAdhan)
                && !host.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_PROMPTED, false);
    }

    static void markPrompted(Activity host) {
        host.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(KEY_PROMPTED, true).apply();
    }

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setLayoutDirection(AppSettings.layoutDirection(this));
        page.setBackgroundColor(AppSettings.background(this));
        page.setPadding(dp(16), dp(16), dp(16), dp(12));

        TextView title = new TextView(this);
        title.setText(AppString.get(R.string.runtime_text_0342));
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppSettings.textPrimary(this));
        page.addView(title);
        TextView hint = new TextView(this);
        hint.setText(AppString.get(R.string.adhan_setup_hint));
        hint.setTextSize(13);
        hint.setTextColor(AppSettings.textSecondary(this));
        hint.setPadding(0, dp(4), 0, dp(10));
        page.addView(hint);

        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));
        panel = new PrayerSettingsPanel(this, content);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        Button later = new Button(this);
        later.setText(AppString.get(R.string.update_later));
        later.setAllCaps(false);
        later.setBackgroundResource(R.drawable.bg_field);
        later.setTextColor(AppSettings.primaryColor(this));
        later.setOnClickListener(v -> finish());
        actions.addView(later, new LinearLayout.LayoutParams(0, dp(54), 1f));
        Button done = new Button(this);
        done.setText(AppString.get(R.string.runtime_text_0004));
        done.setAllCaps(false);
        done.setBackgroundResource(R.drawable.bg_teal_button);
        done.setTextColor(0xFFFFFFFF);
        done.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, dp(54), 1f);
        doneParams.setMarginStart(dp(8));
        actions.addView(done, doneParams);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, -2);
        actionParams.topMargin = dp(8);
        page.addView(actions, actionParams);

        setContentView(page);
        AppSettings.applyFullscreenInsets(page);
        markPrompted(this);
    }

    @Override public void finish() {
        // Whatever was changed (or left as default) takes effect now.
        AdhanScheduler.rescheduleAll(this);
        super.finish();
    }

    @Override protected void onResume() { super.onResume(); if (panel != null) panel.onResume(); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (panel != null) panel.onActivityResult(request, result, data);
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (panel != null) panel.onRequestPermissionsResult(request, permissions, results);
    }
    @Override protected void onStop() { if (panel != null) panel.onStop(); super.onStop(); }
    @Override protected void onDestroy() { if (panel != null) panel.onDestroy(); super.onDestroy(); }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
