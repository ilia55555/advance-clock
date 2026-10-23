package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class WidgetCenterActivity extends Activity {
    private LinearLayout list;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(dp(18), dp(12), dp(18), dp(24));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setBackgroundColor(0x00000000);
        close.setColorFilter(AppSettings.textPrimary(this));
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText(AppString.get(R.string.runtime_text_0110));
        title.setTextSize(25);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        root.addView(top);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, new LinearLayout.LayoutParams(-1, -2));

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    @Override protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        list.removeAllViews();

        addWidgetCard(
                AppString.get(R.string.runtime_text_0484),
                AppString.get(R.string.runtime_text_0485),
                ClockWidgetProvider.class,
                "clock");

        addWidgetCard(
                getString(R.string.calendar_widget_name),
                getString(R.string.calendar_widget_description),
                CalendarWidgetProvider.class,
                "calendar");

        addWidgetCard(
                getString(R.string.time_tools_widget_name),
                getString(R.string.time_tools_widget_description),
                TimeToolsWidgetProvider.class,
                "time_tools");

        addWidgetCard(
                AppString.get(R.string.runtime_text_0016),
                AppString.get(R.string.runtime_text_0486),
                NoForgetWidgetProvider.class,
                "note");

        addWidgetCard(
                AppString.get(R.string.runtime_text_0039),
                AppString.get(R.string.runtime_text_0487),
                WorldClockWidgetProvider.class,
                "world");

        addWidgetCard(
                AppString.get(R.string.runtime_text_0488),
                AppString.get(R.string.runtime_text_0489),
                PrayerTimesWidgetProvider.class,
                "prayer");

        addWidgetCard(
                AppString.get(R.string.runtime_text_0115),
                AppString.get(R.string.runtime_text_0490),
                MediaWidgetProvider.class,
                "media");
    }

    private void addWidgetCard(
            String titleText,
            String description,
            Class<?> provider,
            String kind) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(new ComponentName(this, provider));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(AppSettings.layoutDirection(this));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        android.graphics.drawable.GradientDrawable border = new android.graphics.drawable.GradientDrawable();
        border.setColor(AppSettings.surface(this));
        border.setCornerRadius(dp(20));
        border.setStroke(dp(1), AppSettings.field(this));
        card.setBackground(border);
        card.setElevation(0f);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(AppSettings.layoutDirection(this));
        ImageView icon = new ImageView(this);
        icon.setImageResource(widgetIcon(kind));
        icon.setColorFilter(AppSettings.primaryColor(this));
        icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(26), dp(26));
        iconParams.setMarginEnd(dp(14));
        header.addView(icon, iconParams);

        TextView title = text(titleText, 17, AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView count = text(ids.length + AppString.get(R.string.runtime_text_0491), 11, AppSettings.primaryColor(this));
        count.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(count);
        card.addView(header);

        TextView desc = text(description, 12, AppSettings.textSecondary(this));
        desc.setPadding(0, dp(4), 0, dp(10));
        card.addView(desc);

        ImageView preview = new ImageView(this);
        preview.setImageResource(previewResource(kind));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        preview.setContentDescription(AppString.get(R.string.runtime_text_0492) + titleText);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(170));
        previewLp.bottomMargin = dp(10);
        card.addView(preview, previewLp);

        Button add = new Button(this);
        add.setText(AppString.get(R.string.runtime_text_0113));
        add.setAllCaps(false);
        add.setTextColor(0xFFFFFFFF);
        android.graphics.drawable.GradientDrawable buttonShape = new android.graphics.drawable.GradientDrawable();
        buttonShape.setColor(AppSettings.primaryColor(this));
        buttonShape.setCornerRadius(dp(14));
        add.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x33FFFFFF), buttonShape, null));
        add.setStateListAnimator(null);
        add.setElevation(0f);
        add.setOnClickListener(v -> pin(provider));
        card.addView(add, new LinearLayout.LayoutParams(-1, dp(52)));

        if (ids.length > 0) {
            TextView installed = text(
                    AppString.get(R.string.runtime_text_0114),
                    12,
                    AppSettings.textSecondary(this));
            installed.setPadding(0, dp(12), 0, dp(4));
            card.addView(installed);

            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setLayoutDirection(AppSettings.layoutDirection(this));
            buttons.setGravity(Gravity.START);
            card.addView(buttons, new LinearLayout.LayoutParams(-1, -2));

            for (int i = 0; i < ids.length; i++) {
                final int widgetId = ids[i];
                Button edit = new Button(this);
                edit.setText(AppString.get(R.string.runtime_text_0493) + (i + 1));
                edit.setAllCaps(false);
                edit.setStateListAnimator(null);
                edit.setElevation(0f);
                edit.setTextSize(11);
                edit.setTextColor(AppSettings.primaryColor(this));
                edit.setBackgroundResource(R.drawable.bg_soft_button);
                edit.setPadding(dp(9), 0, dp(9), 0);
                edit.setOnClickListener(v -> editWidget(kind, widgetId));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(42));
                lp.setMargins(0, 0, dp(6), dp(6));
                buttons.addView(edit, lp);
            }
        }

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.topMargin = dp(14);
        list.addView(card, cardLp);
    }

    private int widgetIcon(String kind) {
        if ("calendar".equals(kind)) return R.drawable.ic_settings_calendar;
        if ("prayer".equals(kind)) return R.drawable.ic_settings_adhan;
        if ("time_tools".equals(kind)) return R.drawable.ic_settings_tools;
        if ("clock".equals(kind) || "world".equals(kind)) return R.drawable.ic_settings_alarm;
        return R.drawable.ic_settings_widgets;
    }

    private int previewResource(String kind) {
        if ("calendar".equals(kind)) return R.drawable.preview_widget_calendar;
        if ("time_tools".equals(kind)) return R.drawable.preview_widget_time_tools;
        if ("note".equals(kind)) return R.drawable.preview_widget_notes;
        if ("world".equals(kind)) return R.drawable.preview_widget_world;
        if ("prayer".equals(kind)) {
            int uploaded = getResources().getIdentifier(
                    "preview_widget_prayer", "drawable", getPackageName());
            return uploaded == 0 ? R.drawable.widget_prayer_bg_navy : uploaded;
        }
        if ("media".equals(kind)) return R.drawable.preview_widget_media;
        return R.drawable.preview_widget_clock;
    }

    private void pin(Class<?> provider) {
        if (Build.VERSION.SDK_INT < 26) {
            LogoToast.makeText(
                    this,
                    AppString.get(R.string.runtime_text_0494),
                    Toast.LENGTH_LONG).show();
            return;
        }

        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
            LogoToast.makeText(
                    this,
                    AppString.get(R.string.runtime_text_0495)
                            + AppString.get(R.string.runtime_text_0496),
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean opened = manager.requestPinAppWidget(
                new ComponentName(this, provider),
                null,
                null);

        LogoToast.makeText(
                this,
                opened
                        ? AppString.get(R.string.runtime_text_0497)
                        : AppString.get(R.string.runtime_text_0428),
                Toast.LENGTH_SHORT).show();
    }

    private void editWidget(String kind, int widgetId) {
        Intent intent;
        if ("calendar".equals(kind)) {
            intent = new Intent(this, CalendarWidgetConfigActivity.class)
                    .putExtra("editExisting", true);
        } else if ("time_tools".equals(kind)) {
            intent = new Intent(this, TimeToolsWidgetConfigActivity.class)
                    .putExtra("editExisting", true);
        } else if ("world".equals(kind)) {
            intent = new Intent(this, WorldClockWidgetConfigActivity.class);
        } else if ("prayer".equals(kind)) {
            intent = new Intent(this, PrayerTimesWidgetConfigActivity.class)
                    .putExtra("editExisting", true);
        } else if ("media".equals(kind)) {
            intent = new Intent(this, MediaWidgetConfigActivity.class)
                    .putExtra("editExisting", true)
                    .putExtra("returnToCenter", true);
        } else {
            intent = new Intent(this, WidgetSettingsActivity.class)
                    .putExtra("widgetKind", kind)
                    .putExtra("returnToCenter", true);
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        startActivity(intent);
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.START);
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
