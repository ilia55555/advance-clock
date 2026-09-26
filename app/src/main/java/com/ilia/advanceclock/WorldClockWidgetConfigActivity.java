package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public final class WorldClockWidgetConfigActivity extends Activity {
    private static final int[] COLORS = {
            0xFFFFFFFF, 0xFF111418, 0xFF005FA8, 0xFF36BFEC, 0xFFECCE36,
            0xFFE53935, 0xFF43A047, 0xFF8E24AA, 0xFFFB8C00, 0xFFD81B60,
            0xFFB0BEC5, 0xFF6D4C41
    };
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Toast.makeText(this, "شناسه ویجت معتبر نیست", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(20));
        root.setBackgroundColor(AppSettings.background(this));
        root.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        TextView title = label("تنظیمات ویجت ساعت جهانی", 23);
        title.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(60), 1f));
        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);

        WidgetPreviewView preview = new WidgetPreviewView(this);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(126));
        previewLp.bottomMargin = dp(12);
        root.addView(preview, previewLp);

        root.addView(label("پس‌زمینه", 13));
        Spinner background = spinner(new String[]{"شفاف (پیش‌فرض)", "مشکی ۷۰٪", "مشکی", "سفید",
                "آبی", "فیروزه‌ای", "بنفش", "زرشکی", "سبز"});
        background.setSelection(WorldClockWidgetPrefs.background(this, widgetId));
        root.addView(background, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(label("رنگ نام شهر و تاریخ", 13));
        Spinner text = colorSpinner();
        text.setSelection(colorPosition(WorldClockWidgetPrefs.textColor(this, widgetId)));
        root.addView(text, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(label("رنگ ساعت", 13));
        Spinner time = colorSpinner();
        time.setSelection(colorPosition(WorldClockWidgetPrefs.timeColor(this, widgetId)));
        root.addView(time, new LinearLayout.LayoutParams(-1, dp(54)));

        setContentView(root);
        AppSettings.applyFullscreenInsets(root);
        AppSettings.playFullscreenEnter(this);

        Runnable refreshPreview = () -> {
            preview.configure("world",
                    previewBackground(background.getSelectedItemPosition()),
                    COLORS[text.getSelectedItemPosition()], COLORS[time.getSelectedItemPosition()],
                    true, true, 3);
            WorldClockWidgetPrefs.save(this, widgetId,
                    background.getSelectedItemPosition(), COLORS[text.getSelectedItemPosition()],
                    COLORS[time.getSelectedItemPosition()]);
            WorldClockWidgetProvider.updateAll(this);
            setResult(RESULT_OK, new Intent().putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
        };
        watch(background, refreshPreview);
        watch(text, refreshPreview);
        watch(time, refreshPreview);
        refreshPreview.run();

    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private int previewBackground(int position) {
        switch (position) {
            case 1: return 0xB3111418;
            case 2: return 0xFF111418;
            case 3: return 0xFFFFFFFF;
            case 4: return 0xFF005FA8;
            case 5: return 0xFF00796B;
            case 6: return 0xFF6A1B9A;
            case 7: return 0xFF8E2430;
            case 8: return 0xFF2E7D32;
            default: return 0x22111418;
        }
    }

    private void watch(Spinner spinner, Runnable changed) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                changed.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private Spinner colorSpinner() {
        return spinner(new String[]{"سفید (پیش‌فرض)", "مشکی", "آبی", "فیروزه‌ای", "طلایی",
                "قرمز", "سبز", "بنفش", "نارنجی", "صورتی", "نقره‌ای", "قهوه‌ای"});
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private int colorPosition(int color) {
        for (int i = 0; i < COLORS.length; i++) if (COLORS[i] == color) return i;
        return 0;
    }

    private TextView label(String value, int size) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(AppSettings.textPrimary(this));
        label.setPadding(0, dp(8), 0, dp(4));
        return label;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
