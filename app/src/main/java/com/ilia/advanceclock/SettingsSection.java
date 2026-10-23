package com.ilia.advanceclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** One accessible section of the settings accordion. Expansion is owned by the page. */
final class SettingsSection extends LinearLayout {
    final String key;
    final LinearLayout content;
    final LinearLayout header;
    private final ImageView arrow;
    private final View divider;
    private final GradientDrawable background;

    SettingsSection(Context context, String key, int title, int icon, Runnable onClick) {
        super(context);
        this.key = key;
        setTag("settings_section_" + key);
        setOrientation(VERTICAL);
        setLayoutDirection(AppSettings.layoutDirection(context));
        background = new GradientDrawable();
        background.setColor(AppSettings.surface(context));
        background.setCornerRadius(dp(20));
        setBackground(background);
        setClipToOutline(true);
        setElevation(dp(2));

        header = new LinearLayout(context);
        header.setTag("settings_header_" + key);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(14), dp(16), dp(14));
        header.setMinimumHeight(dp(68));
        header.setBackground(new RippleDrawable(ColorStateList.valueOf(
                (AppSettings.primaryColor(context) & 0x00ffffff) | 0x20000000), null, null));
        header.setFocusable(true);
        header.setOnClickListener(v -> onClick.run());
        ImageView image = new ImageView(context);
        image.setImageResource(icon);
        image.setColorFilter(AppSettings.primaryColor(context));
        image.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(image, new LayoutParams(dp(26), dp(26)));
        TextView label = new TextView(context);
        label.setText(AppString.get(title));
        label.setTextSize(17);
        label.setTextColor(AppSettings.textPrimary(context));
        label.setTypeface(null, Typeface.BOLD);
        LayoutParams textParams = new LayoutParams(0, -2, 1f);
        textParams.setMarginStart(dp(14));
        textParams.setMarginEnd(dp(10));
        header.addView(label, textParams);
        arrow = new ImageView(context);
        arrow.setImageResource(android.R.drawable.arrow_down_float);
        arrow.setColorFilter(AppSettings.primaryColor(context));
        arrow.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        header.addView(arrow, new LayoutParams(dp(22), dp(22)));
        if (Build.VERSION.SDK_INT >= 28) header.setAccessibilityHeading(true);
        addView(header, new LayoutParams(-1, -2));
        divider = new View(context);
        divider.setBackgroundColor(AppSettings.field(context));
        LayoutParams lineParams = new LayoutParams(-1, dp(1));
        lineParams.setMargins(dp(16), 0, dp(16), 0);
        addView(divider, lineParams);
        content = new LinearLayout(context);
        content.setTag("settings_content_" + key);
        content.setOrientation(VERTICAL);
        content.setPadding(dp(16), dp(4), dp(16), dp(16));
        addView(content, new LayoutParams(-1, -2));
        setExpanded(false);
    }

    boolean isExpanded() { return content.getVisibility() == VISIBLE; }

    void setExpanded(boolean expanded) {
        content.setVisibility(expanded ? VISIBLE : GONE);
        divider.setVisibility(expanded ? VISIBLE : GONE);
        arrow.animate().rotation(expanded ? 180f : 0f).setDuration(180).start();
        background.setStroke(dp(1), expanded ? AppSettings.primaryColor(getContext())
                : AppSettings.field(getContext()));
        header.setSelected(expanded);
        if (Build.VERSION.SDK_INT >= 30) header.setStateDescription(AppString.get(expanded
                ? R.string.settings_expanded : R.string.settings_collapsed));
        header.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED);
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
