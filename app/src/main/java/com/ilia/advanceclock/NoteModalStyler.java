package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared visual treatment for the note drawing, file, app and website modals. */
final class NoteModalStyler {
    private NoteModalStyler() {}

    static LinearLayout content(Context context, String title, String subtitle) {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(LinearLayout.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(context, 18), dp(context, 16), dp(context, 18), dp(context, 14));
        root.setBackgroundResource(R.drawable.bg_card);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(AppSettings.textPrimary(context));
        titleView.setTextSize(20);
        titleView.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        titleView.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        root.addView(titleView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 42)));

        if (subtitle != null && !subtitle.trim().isEmpty()) {
            TextView subtitleView = new TextView(context);
            subtitleView.setText(subtitle);
            subtitleView.setTextColor(AppSettings.textSecondary(context));
            subtitleView.setTextSize(12);
            subtitleView.setGravity(Gravity.START);
            subtitleView.setLineSpacing(0, 1.15f);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.bottomMargin = dp(context, 10);
            root.addView(subtitleView, params);
        }
        return root;
    }

    static void show(AlertDialog dialog) {
        dialog.setOnShowListener(ignored -> {
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(R.drawable.bg_card);
                int available = dialog.getContext().getResources()
                        .getDisplayMetrics().widthPixels - dp(dialog.getContext(), 32);
                window.setLayout(Math.min(available, dp(dialog.getContext(), 560)),
                        WindowManager.LayoutParams.WRAP_CONTENT);
            }
            styleAction(dialog.getButton(AlertDialog.BUTTON_POSITIVE), true);
            styleAction(dialog.getButton(AlertDialog.BUTTON_NEGATIVE), false);
            styleAction(dialog.getButton(AlertDialog.BUTTON_NEUTRAL), false);
            UiText.localize(dialog);
        });
        dialog.show();
    }

    private static void styleAction(Button button, boolean primary) {
        if (button == null) return;
        button.setAllCaps(false);
        button.setTextSize(13);
        button.setMinHeight(dp(button.getContext(), 44));
        button.setTextColor(primary ? Color.WHITE : AppSettings.primaryColor(button.getContext()));
        button.setBackgroundResource(primary
                ? R.drawable.bg_orange_button
                : R.drawable.bg_soft_button);
        button.setPadding(dp(button.getContext(), 14), 0, dp(button.getContext(), 14), 0);
    }

    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
