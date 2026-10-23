package com.ilia.advanceclock;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/** A single strip of inline labels and switches. */
final class DateOption {
    private final Activity host;
    private final Switch dateSwitch;
    private final LinearLayout controls;
    private Runnable changed;
    private boolean updating;

    DateOption(Activity host, Button date, int id) {
        this.host = host;
        controls = new LinearLayout(host);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setLayoutDirection(AppSettings.layoutDirection(host));
        controls.setBaselineAligned(false);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setBackgroundResource(R.drawable.bg_field);
        controls.setPadding(dp(10),dp(6),dp(10),dp(6));
        dateSwitch = card(R.string.apply_alarm_date, id);
        View row = (View) date.getParent();
        LinearLayout parent = (LinearLayout) row.getParent();
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        parent.addView(controls, parent.indexOfChild(row), lp);
        dateSwitch.setOnCheckedChangeListener((button, checked) -> {
            date.setEnabled(checked);
            date.setAlpha(checked ? 1f : .45f);
            if (!updating && changed != null) changed.run();
        });
        date.setEnabled(false);
        date.setAlpha(.45f);
    }

    Switch addAutoDelete(int id) {
        View gap = new View(host);
        controls.addView(gap, new LinearLayout.LayoutParams(dp(8), 1));
        return card(R.string.alarm_auto_delete_finished, id);
    }

    private Switch card(int label, int id) {
        LinearLayout card = new LinearLayout(host);
        card.setLayoutDirection(AppSettings.layoutDirection(host));
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBaselineAligned(false);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setMinimumHeight(dp(48));
        TextView caption = new TextView(host);
        caption.setText(label==R.string.alarm_auto_delete_finished?R.string.alarm_auto_delete_short:label);
        caption.setTextColor(AppSettings.textSecondary(host));
        caption.setTextSize(12);
        caption.setGravity(Gravity.CENTER);
        caption.setMaxLines(1);caption.setEllipsize(null);caption.setHorizontallyScrolling(false);
        caption.setIncludeFontPadding(false);
        caption.setAutoSizeTextTypeUniformWithConfiguration(8,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        card.addView(caption,new LinearLayout.LayoutParams(0,-2,1));
        Switch toggle = new Switch(host);
        toggle.setId(id);
        toggle.setContentDescription(host.getString(label));
        toggle.setMinimumHeight(dp(48));
        LinearLayout.LayoutParams toggleParams=new LinearLayout.LayoutParams(-2,-2);toggleParams.setMarginStart(dp(4));
        card.addView(toggle,toggleParams);
        card.setOnClickListener(view->toggle.toggle());
        controls.addView(card, new LinearLayout.LayoutParams(0, -2, 1));
        return toggle;
    }

    boolean isApplied() { return dateSwitch.isChecked(); }
    void setApplied(boolean applied) {
        updating = true;
        try { dateSwitch.setChecked(applied); } finally { updating = false; }
    }
    void onChanged(Runnable callback) { changed = callback; }
    void setVisible(boolean visible) { controls.setVisibility(visible ? View.VISIBLE : View.GONE); }
    private int dp(int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
}
