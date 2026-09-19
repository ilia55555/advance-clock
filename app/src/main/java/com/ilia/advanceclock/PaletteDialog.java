package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class PaletteDialog {
    public interface Callback { void onColor(int color); }

    private PaletteDialog() {}

    public static void show(Context context, int initialColor, Callback callback) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context,16), dp(context,8), dp(context,16), dp(context,8));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView hexLabel = label(context, "رنگ‌های آماده");
        root.addView(hexLabel);

        HexPaletteView hex = new HexPaletteView(context);
        hex.setSelectedColor(initialColor);
        root.addView(hex, new LinearLayout.LayoutParams(-1, -2));

        TextView mixerLabel = label(context, "پالت کشیدنی");
        mixerLabel.setPadding(0, dp(context,10), 0, dp(context,5));
        root.addView(mixerLabel);

        ColorMixerView mixer = new ColorMixerView(context);
        mixer.setColor(initialColor);
        root.addView(mixer, new LinearLayout.LayoutParams(-1, dp(context,238)));

        TextView preview = new TextView(context);
        preview.setGravity(Gravity.CENTER);
        preview.setText("نمونه رنگ قلم");
        preview.setTextSize(15);
        preview.setTextColor(0xFFFFFFFF);
        preview.setPadding(dp(context,12),0,dp(context,12),0);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(context,48)));

        final int[] selected = {initialColor};
        Runnable updatePreview = () -> {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(selected[0]);
            bg.setCornerRadius(dp(context,14));
            preview.setBackground(bg);
        };
        updatePreview.run();

        hex.setListener(color -> {
            selected[0] = color;
            mixer.setColor(color);
            updatePreview.run();
        });

        mixer.setListener(color -> {
            selected[0] = color;
            hex.setSelectedColor(color);
            updatePreview.run();
        });

        new AlertDialog.Builder(context)
                .setTitle("پالت رنگ قلم")
                .setView(root)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("انتخاب رنگ", (d, which) -> callback.onColor(selected[0]))
                .show();
    }

    private static TextView label(Context context, String text) {
        TextView v = new TextView(context);
        v.setText(text);
        v.setTextSize(14);
        v.setTextColor(AppSettings.textSecondary(context));
        v.setPadding(0, dp(context,3), 0, dp(context,5));
        return v;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
