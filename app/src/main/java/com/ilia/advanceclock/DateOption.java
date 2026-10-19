package com.ilia.advanceclock;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;

/** Shared optional-date control for alarm/note composers and editors. */
final class DateOption {
    private final CheckBox checkbox;
    private Runnable changed;
    private boolean updating;

    DateOption(Activity host, Button date, int id) {
        checkbox = new CheckBox(host);
        checkbox.setId(id);
        checkbox.setText(R.string.apply_alarm_date);
        checkbox.setTextColor(AppSettings.textPrimary(host));
        checkbox.setMinHeight(Math.round(48 * host.getResources().getDisplayMetrics().density));
        View row = (View) date.getParent();
        LinearLayout parent = (LinearLayout) row.getParent();
        parent.addView(checkbox, parent.indexOfChild(row), new LinearLayout.LayoutParams(-1, -2));
        checkbox.setOnCheckedChangeListener((button, checked) -> {
            date.setEnabled(checked);
            date.setAlpha(checked ? 1f : .45f);
            if (!updating && changed != null) changed.run();
        });
        date.setEnabled(false);
        date.setAlpha(.45f);
    }

    boolean isApplied() { return checkbox.isChecked(); }
    void setApplied(boolean applied) {
        updating = true;
        try { checkbox.setChecked(applied); } finally { updating = false; }
    }
    void onChanged(Runnable callback) { changed = callback; }
    void setVisible(boolean visible) { checkbox.setVisibility(visible ? View.VISIBLE : View.GONE); }
}
