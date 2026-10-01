package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public final class CalendarWidgetConfigActivity extends Activity {
    private int widgetId;
    private Switch showEvents;
    private Switch showPrayer;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setResult(RESULT_CANCELED);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(AppSettings.background(this));
        page.setPadding(dp(18), dp(12), dp(18), dp(18));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText(R.string.calendar_widget_settings_title);
        title.setTextSize(23);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));
        page.addView(top);

        TextView hint = new TextView(this);
        hint.setText(R.string.calendar_widget_settings_description);
        hint.setTextSize(13);
        hint.setTextColor(AppSettings.textSecondary(this));
        hint.setPadding(0, dp(4), 0, dp(12));
        page.addView(hint);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(AppSettings.layoutDirection(this));
        card.setPadding(dp(14), dp(10), dp(14), dp(10));
        card.setBackgroundResource(R.drawable.bg_card);

        showEvents = new Switch(this);
        showEvents.setText(R.string.calendar_widget_show_events);
        showEvents.setTextSize(15);
        showEvents.setTextColor(AppSettings.textPrimary(this));
        showEvents.setChecked(CalendarWidgetPrefs.showEvents(this, widgetId));
        showEvents.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(showEvents, new LinearLayout.LayoutParams(-1, dp(54)));

        showPrayer = new Switch(this);
        showPrayer.setText(R.string.calendar_widget_show_prayer);
        showPrayer.setTextSize(15);
        showPrayer.setTextColor(AppSettings.textPrimary(this));
        showPrayer.setChecked(CalendarWidgetPrefs.showPrayerTimes(this, widgetId));
        showPrayer.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(showPrayer, new LinearLayout.LayoutParams(-1, dp(54)));

        page.addView(card, new LinearLayout.LayoutParams(-1, -2));

        View spacer = new View(this);
        page.addView(spacer, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button save = new Button(this);
        save.setText(R.string.runtime_text_0005);
        save.setAllCaps(false);
        save.setTextSize(15);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundColor(AppSettings.primaryColor(this));
        save.setOnClickListener(v -> saveAndFinish());
        LinearLayout.LayoutParams saveParams =
                new LinearLayout.LayoutParams(-1, dp(52));
        saveParams.topMargin = dp(12);
        page.addView(save, saveParams);

        setContentView(page);
        AppSettings.applyFullscreenInsets(page);
        AppSettings.playFullscreenEnter(this);
    }

    private void saveAndFinish() {
        CalendarWidgetPrefs.setShowEvents(
                this, widgetId, showEvents.isChecked());
        CalendarWidgetPrefs.setShowPrayerTimes(
                this, widgetId, showPrayer.isChecked());

        CalendarWidgetProvider.updateAll(this);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private int dp(int value) {
        return Math.round(
                value * getResources().getDisplayMetrics().density);
    }
}
