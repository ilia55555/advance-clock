package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class ToolsDialog {
    private ToolsDialog() {}

    public static void show(Activity host) {
        Dialog dialog = new Dialog(host);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(host));
        root.setPadding(dp(host, 18), dp(host, 12), dp(host, 18), dp(host, 18));
        root.setBackground(cardBackground(host));

        LinearLayout header = new LinearLayout(host);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        ImageButton close = new ImageButton(host);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(host));
        close.setBackgroundColor(Color.TRANSPARENT);
        close.setPadding(
                dp(host, 12),
                dp(host, 12),
                dp(host, 12),
                dp(host, 12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> dialog.dismiss());
        header.addView(
                close,
                new LinearLayout.LayoutParams(
                        dp(host, 48),
                        dp(host, 48)));

        TextView title = text(
                host,
                AppString.get(R.string.runtime_text_0317),
                22,
                AppSettings.textPrimary(host));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        header.addView(
                title,
                new LinearLayout.LayoutParams(
                        0,
                        dp(host, 56),
                        1f));

        root.addView(header);

        LinearLayout compass = toolCard(
                host,
                AppString.get(R.string.runtime_text_0318),
                AppString.get(R.string.tools_dialog_compass_description));
        compass.setOnClickListener(v -> {
            dialog.dismiss();
            host.startActivity(
                    new Intent(
                            host,
                            CompassToolActivity.class));
        });

        LinearLayout.LayoutParams toolParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
        toolParams.topMargin = dp(host, 8);
        root.addView(compass, toolParams);

        dialog.setContentView(root);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
            WindowManager.LayoutParams params =
                    new WindowManager.LayoutParams();
            params.copyFrom(window.getAttributes());
            params.width =
                    WindowManager.LayoutParams.MATCH_PARENT;
            params.height =
                    WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.BOTTOM;
            window.setAttributes(params);
        }
    }

    private static LinearLayout toolCard(
            Activity host,
            String titleValue,
            String descriptionValue) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(AppSettings.layoutDirection(host));
        row.setPadding(
                dp(host, 14),
                dp(host, 12),
                dp(host, 14),
                dp(host, 12));
        row.setBackground(toolBackground(host));
        row.setClickable(true);
        row.setFocusable(true);

        TextView compassMark = text(
                host,
                "N",
                22,
                AppSettings.primaryColor(host));
        compassMark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        compassMark.setGravity(Gravity.CENTER);
        GradientDrawable markBackground =
                new GradientDrawable();
        markBackground.setColor(AppSettings.field(host));
        markBackground.setShape(GradientDrawable.OVAL);
        markBackground.setStroke(
                dp(host, 1),
                AppSettings.primaryColor(host));
        compassMark.setBackground(markBackground);

        LinearLayout.LayoutParams markParams =
                new LinearLayout.LayoutParams(
                        dp(host, 52),
                        dp(host, 52));
        row.addView(compassMark, markParams);

        LinearLayout textColumn = new LinearLayout(host);
        textColumn.setOrientation(LinearLayout.VERTICAL);
        textColumn.setGravity(Gravity.CENTER_VERTICAL);
        textColumn.setPadding(
                dp(host, 12),
                0,
                dp(host, 12),
                0);

        TextView title = text(
                host,
                titleValue,
                16,
                AppSettings.textPrimary(host));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        textColumn.addView(title);

        TextView description = text(
                host,
                descriptionValue,
                12,
                AppSettings.textSecondary(host));
        description.setPadding(0, dp(host, 3), 0, 0);
        description.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        textColumn.addView(description);

        row.addView(
                textColumn,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f));

        TextView arrow = text(
                host,
                AppSettings.isRtlLanguage(host) ? "‹" : "›",
                26,
                AppSettings.primaryColor(host));
        arrow.setGravity(Gravity.CENTER);
        row.addView(
                arrow,
                new LinearLayout.LayoutParams(
                        dp(host, 32),
                        dp(host, 52)));

        return row;
    }

    private static TextView text(
            Activity host,
            String value,
            int size,
            int color) {
        TextView view = new TextView(host);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        return view;
    }

    private static GradientDrawable cardBackground(Activity host) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(AppSettings.surface(host));
        background.setCornerRadii(new float[]{
                dp(host, 24), dp(host, 24),
                dp(host, 24), dp(host, 24),
                0, 0,
                0, 0
        });
        return background;
    }

    private static GradientDrawable toolBackground(Activity host) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(AppSettings.field(host));
        background.setCornerRadius(dp(host, 16));
        background.setStroke(
                dp(host, 1),
                AppSettings.themeMode(host)
                        == AppSettings.THEME_DARK
                        ? 0xFF343D3A
                        : 0xFFD8E8E7);
        return background;
    }

    private static int dp(Activity host, int value) {
        return Math.round(
                value
                        * host.getResources()
                        .getDisplayMetrics()
                        .density);
    }
}
