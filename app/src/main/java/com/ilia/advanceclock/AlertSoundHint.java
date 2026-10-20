package com.ilia.advanceclock;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Acknowledged independently for the alarm and Adhan screens. */
final class AlertSoundHint {
    private AlertSoundHint() {}

    static void show(Activity host, String kind) {
        SharedPreferences prefs = host.getSharedPreferences("alert_sound_hints", Activity.MODE_PRIVATE);
        if (prefs.getBoolean(kind, false)) return;
        FrameLayout content = host.findViewById(android.R.id.content);
        if (content.findViewWithTag("alert_sound_hint") != null) return;
        float density = host.getResources().getDisplayMetrics().density;
        int padding = Math.round(24 * density);
        FrameLayout veil = new FrameLayout(host);
        veil.setTag("alert_sound_hint");
        veil.setBackgroundColor(0xDD555555);
        veil.setClickable(true);
        veil.setFocusable(true);
        veil.setElevation(24 * density);
        veil.setPadding(padding, padding, padding, padding);
        LinearLayout message = new LinearLayout(host);
        message.setOrientation(LinearLayout.VERTICAL);
        message.setGravity(Gravity.CENTER);
        TextView text = new TextView(host);
        text.setText(R.string.alert_sound_hint);
        text.setTextColor(0xFFFFFFFF);
        text.setTextSize(18);
        text.setGravity(Gravity.CENTER);
        text.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        text.setLineSpacing(6 * density, 1);
        text.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        message.addView(text, new LinearLayout.LayoutParams(-1, -2));
        Button understood = new Button(host);
        understood.setText(R.string.alert_sound_hint_understood);
        understood.setAllCaps(false);
        understood.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-2, -2);
        buttonParams.topMargin = padding;
        message.addView(understood, buttonParams);
        veil.addView(message, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        int count = content.getChildCount();
        View[] underlying = new View[count];
        int[] accessibility = new int[count];
        for (int i = 0; i < count; i++) {
            underlying[i] = content.getChildAt(i);
            accessibility[i] = underlying[i].getImportantForAccessibility();
            underlying[i].setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        }
        understood.setOnClickListener(v -> {
            prefs.edit().putBoolean(kind, true).apply();
            content.removeView(veil);
            for (int i = 0; i < count; i++) underlying[i].setImportantForAccessibility(accessibility[i]);
        });
        content.addView(veil, new ViewGroup.LayoutParams(-1, -1));
        understood.requestFocus();
    }
}
