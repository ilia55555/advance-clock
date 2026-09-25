package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public final class WorldClockWidgetConfigActivity extends Activity {
    private static final int[] COLORS = {
            0xFFFFFFFF, 0xFF111418, 0xFF005FA8, 0xFF36BFEC, 0xFFECCE36
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
        TextView title = label("تنظیمات ویجت ساعت جهانی", 23);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        root.addView(label("پس‌زمینه", 13));
        Spinner background = spinner(new String[]{"شفاف (پیش‌فرض)", "مشکی ۷۰٪", "مشکی", "سفید"});
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

        Button save = new Button(this);
        save.setText("ذخیره تنظیمات ویجت");
        save.setAllCaps(false);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundResource(R.drawable.bg_teal_button);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(58));
        lp.topMargin = dp(16);
        root.addView(save, lp);
        setContentView(root);

        save.setOnClickListener(v -> {
            WorldClockWidgetPrefs.save(this, widgetId,
                    background.getSelectedItemPosition(), COLORS[text.getSelectedItemPosition()],
                    COLORS[time.getSelectedItemPosition()]);
            WorldClockWidgetProvider.updateAll(this);
            setResult(RESULT_OK, new Intent().putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId));
            finish();
        });
    }

    private Spinner colorSpinner() {
        return spinner(new String[]{"سفید (پیش‌فرض)", "مشکی", "آبی", "فیروزه‌ای", "طلایی"});
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
