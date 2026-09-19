package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

public final class SettingsActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        super.onCreate(savedInstanceState);

        int pad = dp(18);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(AppSettings.background(this));

        TextView title = new TextView(this);
        title.setText("تنظیمات");
        title.setTextSize(27);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.START);
        root.addView(title, new LinearLayout.LayoutParams(-1, dp(60)));

        root.addView(label("رنگ اصلی کل اپ"));
        Spinner accent = spinner(new String[]{"سبزآبی", "آبی", "بنفش", "سبز", "نارنجی"});
        accent.setSelection(AppSettings.accent(this));
        root.addView(accent, new LinearLayout.LayoutParams(-1, dp(54)));

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

        root.addView(label("چیدمان صفحه ساعت"));
        Spinner clockLayout = spinner(new String[]{
                "پیش‌فرض: افزودن هشدار سپس تقویم",
                "تقویم و هشدارها در ابتدا"
        });
        clockLayout.setSelection(AppSettings.clockLayoutMode(this));
        root.addView(clockLayout, new LinearLayout.LayoutParams(-1, dp(54)));

        root.addView(label("استایل صفحه زنگ"));
        Spinner alarmStyle = spinner(new String[]{"کلاسیک روشن", "تمرکز تیره", "طلوع گرم"});
        alarmStyle.setSelection(AppSettings.alarmScreenStyle(this));
        root.addView(alarmStyle, new LinearLayout.LayoutParams(-1, dp(54)));

        Button save = new Button(this);
        save.setText("ذخیره تنظیمات");
        save.setTextColor(0xFFFFFFFF);
        save.setAllCaps(false);
        save.setBackgroundColor(AppSettings.primaryColor(this));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(58));
        saveLp.topMargin = dp(24);
        root.addView(save, saveLp);

        setContentView(root);

        save.setOnClickListener(v -> {
            AppSettings.setAccent(this, accent.getSelectedItemPosition());
            AppSettings.setDefaultCalendar(this, calendar.getSelectedItemPosition());
            AppSettings.setClockLayoutMode(this, clockLayout.getSelectedItemPosition());
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
        ArrayAdapter<String> a = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, values);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(a);
        return spinner;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
