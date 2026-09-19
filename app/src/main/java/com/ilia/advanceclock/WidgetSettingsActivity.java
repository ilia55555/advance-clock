package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public final class WidgetSettingsActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        int widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        String kind = getIntent().getStringExtra("widgetKind");
        boolean resizeFocus = getIntent().getBooleanExtra("focusResize", false);

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(18));
        root.setBackgroundColor(AppSettings.background(this));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("تنظیمات ویجت");
        title.setTextSize(23);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(54), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);

        root.addView(label("تم ویجت"));
        Spinner theme = spinner(new String[]{
                "هماهنگ با تم اپ",
                "روشن",
                "تیره"
        });
        theme.setSelection(WidgetPrefs.themeMode(this, widgetId));
        root.addView(theme, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(label("رنگ ویجت"));
        Spinner accent = spinner(AppSettings.accentNames());
        accent.setSelection(WidgetPrefs.accent(this, widgetId));
        root.addView(accent, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView resizeTitle = label("تغییر اندازه");
        resizeTitle.setPadding(0, dp(16), 0, dp(4));
        root.addView(resizeTitle);

        TextView resizeHint = new TextView(this);
        resizeHint.setText("اندروید دکمه عمومی برای باز کردن مستقیم دستگیره‌های Resize در اختیار اپ نمی‌گذارد. این دکمه شما را به صفحه اصلی برمی‌گرداند؛ سپس ویجت را نگه دارید و دستگیره‌های اندازه را بکشید.");
        resizeHint.setTextColor(AppSettings.textSecondary(this));
        resizeHint.setTextSize(12);
        resizeHint.setPadding(0, 0, 0, dp(8));
        root.addView(resizeHint);

        Button resize = new Button(this);
        resize.setText("رفتن به صفحه اصلی برای تغییر اندازه");
        resize.setAllCaps(false);
        resize.setTextColor(AppSettings.primaryColor(this));
        resize.setBackgroundResource(R.drawable.bg_soft_button);
        root.addView(resize, new LinearLayout.LayoutParams(-1, dp(52)));

        Button save = new Button(this);
        save.setText("ذخیره ویجت");
        save.setTextColor(0xFFFFFFFF);
        save.setAllCaps(false);
        save.setBackgroundColor(AppSettings.secondaryColor(this));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(56));
        saveLp.topMargin = dp(14);
        root.addView(save, saveLp);

        setContentView(root);

        save.setOnClickListener(v -> {
            WidgetPrefs.setThemeMode(this, widgetId, theme.getSelectedItemPosition());
            WidgetPrefs.setAccent(this, widgetId, accent.getSelectedItemPosition());
            updateWidget(kind);
            finish();
        });

        resize.setOnClickListener(v -> {
            WidgetPrefs.setThemeMode(this, widgetId, theme.getSelectedItemPosition());
            WidgetPrefs.setAccent(this, widgetId, accent.getSelectedItemPosition());
            updateWidget(kind);
            Toast.makeText(this, "ویجت را نگه دارید و اندازه‌اش را تغییر دهید", Toast.LENGTH_LONG).show();
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
            finish();
        });

        if (resizeFocus) {
            resize.requestFocus();
        }
    }

    private void updateWidget(String kind) {
        if ("note".equals(kind)) {
            NoForgetWidgetProvider.updateAll(this);
        } else {
            ClockWidgetProvider.updateAll(this);
        }
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(AppSettings.textSecondary(this));
        v.setTextSize(13);
        v.setPadding(0, dp(10), 0, dp(4));
        return v;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
