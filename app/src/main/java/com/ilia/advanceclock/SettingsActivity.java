package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        int pad = dp(18);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(pad, dp(12), pad, pad);
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("تنظیمات");
        title.setTextSize(25);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        close.setOnClickListener(v -> finish());

        root.addView(top);

        root.addView(label("رنگ اپ"));
        Spinner palette = spinner(AppSettings.paletteNames());
        palette.setSelection(AppSettings.palette(this));
        root.addView(palette, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView paletteHint = new TextView(this);
        paletteHint.setText("هر گزینه یک جفت رنگ کامل برای هدر، کنترل‌ها و دکمه‌های اصلی است.");
        paletteHint.setTextColor(AppSettings.textSecondary(this));
        paletteHint.setTextSize(12);
        paletteHint.setPadding(0, dp(4), 0, dp(8));
        root.addView(paletteHint);

        root.addView(label("تقویم پیش‌فرض"));
        Spinner calendar = spinner(new String[]{"شمسی", "میلادی", "قمری"});
        calendar.setSelection(AppSettings.defaultCalendar(this));
        root.addView(calendar, new LinearLayout.LayoutParams(-1, dp(54)));

        TextView calendarHint = new TextView(this);
        calendarHint.setText("تقویم انتخاب‌شده برای فیلدهای تاریخ، تقویم اصلی و تاریخ نوار وضعیت استفاده می‌شود.");
        calendarHint.setTextColor(AppSettings.textSecondary(this));
        calendarHint.setTextSize(12);
        calendarHint.setPadding(0, dp(4), 0, dp(8));
        root.addView(calendarHint);

        root.addView(label("چیدمان صفحه ساعت و یادداشت"));
        Spinner layout = spinner(new String[]{
                "پیش‌فرض: فرم ایجاد داخل صفحه، بدون دکمه +",
                "فشرده: فرم ایجاد در مودال با دکمه +"
        });
        layout.setSelection(AppSettings.clockLayoutMode(this));
        root.addView(layout, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(label("استایل صفحه زنگ"));
        Spinner alarmStyle = spinner(new String[]{
                "کلاسیک روشن",
                "تمرکز تیره",
                "طلوع گرم"
        });
        alarmStyle.setSelection(AppSettings.alarmScreenStyle(this));
        root.addView(alarmStyle, new LinearLayout.LayoutParams(-1, dp(54)));

        Button save = new Button(this);
        save.setText("ذخیره تنظیمات");
        save.setTextColor(0xFFFFFFFF);
        save.setAllCaps(false);
        save.setBackgroundColor(AppSettings.secondaryColor(this));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(58));
        saveLp.topMargin = dp(22);
        root.addView(save, saveLp);

        setContentView(scroll);

        save.setOnClickListener(v -> {
            AppSettings.setPalette(this, palette.getSelectedItemPosition());
            AppSettings.setDefaultCalendar(this, calendar.getSelectedItemPosition());
            AppSettings.setClockLayoutMode(this, layout.getSelectedItemPosition());
            AppSettings.setAlarmScreenStyle(this, alarmStyle.getSelectedItemPosition());

            try { DateNotificationService.start(this); } catch (Exception ignored) {}
            ClockWidgetProvider.updateAll(this);
            NoForgetWidgetProvider.updateAll(this);

            setResult(RESULT_OK);
            finish();
        });
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(AppSettings.textSecondary(this));
        v.setTextSize(13);
        v.setPadding(0, dp(12), 0, dp(4));
        return v;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
        return spinner;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
