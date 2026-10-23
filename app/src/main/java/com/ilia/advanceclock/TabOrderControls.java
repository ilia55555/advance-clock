package com.ilia.advanceclock;

import android.content.Context;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immediate, inline tab ordering alongside the existing visibility switches. */
final class TabOrderControls extends LinearLayout {
    private final ArrayList<String> order;
    private final Map<String, LinearLayout> rows = new LinkedHashMap<>();
    private final Map<String, ImageButton> upButtons = new LinkedHashMap<>();
    private final Map<String, ImageButton> downButtons = new LinkedHashMap<>();
    private final Runnable onChanged;

    TabOrderControls(Context context, Map<String, Switch> controls, Runnable onChanged) {
        super(context);
        this.onChanged = onChanged;
        order = new ArrayList<>(Arrays.asList(AppSettings.tabOrder(context)));
        setTag("tab_order_controls");
        setOrientation(VERTICAL);
        setLayoutDirection(AppSettings.layoutDirection(context));
        for (String key : order) {
            Switch control = controls.get(key);
            LinearLayout row = new LinearLayout(context);
            row.setTag("tab_order_row_" + key);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(4), dp(8), dp(4));
            row.setBackgroundResource(R.drawable.bg_field);
            control.setMinWidth(0);
            row.addView(control, new LayoutParams(0, -2, 1f));
            ImageButton up = moveButton(control, key, true);
            ImageButton down = moveButton(control, key, false);
            row.addView(up, new LayoutParams(dp(48), dp(48)));
            row.addView(down, new LayoutParams(dp(48), dp(48)));
            rows.put(key, row);
            upButtons.put(key, up);
            downButtons.put(key, down);
        }
        render();
    }

    private ImageButton moveButton(Switch control, String key, boolean up) {
        ImageButton button = new ImageButton(getContext());
        button.setTag("tab_order_" + (up ? "up_" : "down_") + key);
        button.setImageResource(R.drawable.ic_settings_chevron_down);
        // Rotate only the icon, so the touch target and focus order stay stable.
        button.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        if (up) {
            android.graphics.drawable.Drawable drawable = button.getDrawable();
            android.graphics.drawable.RotateDrawable rotated = new android.graphics.drawable.RotateDrawable();
            rotated.setDrawable(drawable);
            rotated.setFromDegrees(180f);
            rotated.setToDegrees(180f);
            rotated.setLevel(10000);
            button.setImageDrawable(rotated);
        }
        button.setColorFilter(AppSettings.primaryColor(getContext()));
        button.setPadding(dp(12), dp(12), dp(12), dp(12));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        button.setContentDescription(AppString.get(up ? R.string.tab_order_move_up
                : R.string.tab_order_move_down, control.getText()));
        button.setOnClickListener(view -> {
            int from = order.indexOf(key);
            int to = from + (up ? -1 : 1);
            if (to < 0 || to >= order.size()) return;
            Collections.swap(order, from, to);
            AppSettings.setTabOrder(getContext(), order);
            render();
            onChanged.run();
        });
        return button;
    }

    private void render() {
        removeAllViews();
        for (int index = 0; index < order.size(); index++) {
            String key = order.get(index);
            ImageButton up = upButtons.get(key);
            ImageButton down = downButtons.get(key);
            up.setEnabled(index > 0);
            down.setEnabled(index < order.size() - 1);
            up.setAlpha(up.isEnabled() ? 1f : .3f);
            down.setAlpha(down.isEnabled() ? 1f : .3f);
            LayoutParams params = new LayoutParams(-1, -2);
            params.topMargin = dp(8);
            addView(rows.get(key), params);
        }
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
