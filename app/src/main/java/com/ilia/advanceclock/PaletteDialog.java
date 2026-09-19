package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public final class PaletteDialog {
    public interface Callback { void onColor(int color); }

    private static final int[] COLORS = {
            0xFF111827, 0xFF374151, 0xFF6B7280, 0xFFFFFFFF,
            0xFFB91C1C, 0xFFEF4444, 0xFFF97316, 0xFFF59E0B,
            0xFF84CC16, 0xFF16A34A, 0xFF0F766E, 0xFF06B6D4,
            0xFF2563EB, 0xFF4F46E5, 0xFF7C3AED, 0xFF9333EA,
            0xFFDB2777, 0xFFBE185D, 0xFF7F1D1D, 0xFF78350F,
            0xFF365314, 0xFF14532D, 0xFF134E4A, 0xFF164E63,
            0xFF1E3A8A, 0xFF312E81, 0xFF581C87, 0xFF831843
    };

    private PaletteDialog() {}

    public static void show(Context context, int initialColor, Callback callback) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(context, 18), dp(context, 14), dp(context, 18), dp(context, 8));
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        GridLayout grid = new GridLayout(context);
        grid.setColumnCount(4);
        root.addView(grid);

        final int[] selected = {initialColor};

        for (int color : COLORS) {
            TextView chip = new TextView(context);
            chip.setGravity(Gravity.CENTER);
            chip.setText(color == 0xFFFFFFFF ? "○" : "");
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.OVAL);
            bg.setColor(color);
            bg.setStroke(dp(context, 1), color == 0xFFFFFFFF ? 0xFFB8C3C0 : color);
            chip.setBackground(bg);
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = dp(context, 48);
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(context, 6), dp(context, 6), dp(context, 6), dp(context, 6));
            chip.setLayoutParams(lp);
            chip.setOnClickListener(v -> {
                selected[0] = color;
                callback.onColor(color);
            });
            grid.addView(chip);
        }

        TextView custom = new TextView(context);
        custom.setText("رنگ سفارشی");
        custom.setTextColor(AppSettings.textPrimary(context));
        custom.setTextSize(15);
        custom.setPadding(0, dp(context, 12), 0, dp(context, 4));
        root.addView(custom);

        SeekBar hue = new SeekBar(context);
        hue.setMax(360);
        float[] hsv = new float[3];
        Color.colorToHSV(initialColor, hsv);
        hue.setProgress(Math.round(hsv[0]));
        root.addView(hue);

        TextView preview = new TextView(context);
        preview.setText("●");
        preview.setGravity(Gravity.CENTER);
        preview.setTextSize(46);
        preview.setTextColor(initialColor);
        root.addView(preview);

        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int color = Color.HSVToColor(new float[]{progress, 0.82f, 0.82f});
                selected[0] = color;
                preview.setTextColor(color);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        new AlertDialog.Builder(context)
                .setTitle("پالت رنگ قلم")
                .setView(root)
                .setNegativeButton("بستن", null)
                .setPositiveButton("انتخاب رنگ سفارشی", (d, which) -> callback.onColor(selected[0]))
                .show();
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
