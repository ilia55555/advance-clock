package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public final class LogoToast {
    private LogoToast() {}

    public static Toast makeText(Context context, CharSequence message, int duration) {
        Context appContext = context.getApplicationContext();
        int horizontalPadding = dp(appContext, 16);
        int verticalPadding = dp(appContext, 11);

        LinearLayout content = new LinearLayout(appContext);
        content.setOrientation(LinearLayout.HORIZONTAL);
        content.setGravity(Gravity.CENTER_VERTICAL);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        content.setPadding(horizontalPadding, verticalPadding,
                horizontalPadding, verticalPadding);

        GradientDrawable background = new GradientDrawable();
        background.setColor(AppSettings.surface(appContext));
        background.setCornerRadius(dp(appContext, 18));
        background.setStroke(dp(appContext, 1), AppSettings.primaryColor(appContext));
        content.setBackground(background);
        content.setElevation(dp(appContext, 6));

        ImageView logo = new ImageView(appContext);
        logo.setImageResource(R.drawable.clock_pro_icon);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        LinearLayout.LayoutParams logoParams =
                new LinearLayout.LayoutParams(dp(appContext, 36), dp(appContext, 36));
        logoParams.setMarginEnd(dp(appContext, 10));
        content.addView(logo, logoParams);

        TextView text = new TextView(appContext);
        text.setText(UiText.trComposite(appContext,
                message == null ? "" : message.toString()));
        text.setTextColor(AppSettings.textPrimary(appContext));
        text.setTextSize(14);
        text.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        text.setMaxWidth(dp(appContext, 280));
        content.addView(text, new LinearLayout.LayoutParams(-2, -2));

        Toast toast = new Toast(appContext);
        toast.setDuration(duration);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, dp(appContext, 72));
        toast.setView(content);
        return toast;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
