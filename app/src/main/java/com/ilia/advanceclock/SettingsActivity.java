package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
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

        root.addView(label(getString(R.string.language_label)));
        Spinner language = spinner(getResources().getStringArray(R.array.language_options));
        language.setSelection(AppSettings.languagePosition(this));
        root.addView(language, new LinearLayout.LayoutParams(-1, dp(54)));

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

        root.addView(label("تب‌های قابل نمایش در صفحه اصلی"));
        Switch tabClock = tabSwitch("ساعت", "clock");
        Switch tabNotes = tabSwitch("یادداشت‌ها", "noforget");
        Switch tabStopwatch = tabSwitch("کرنومتر", "stopwatch");
        Switch tabTimer = tabSwitch("تایمر", "timer");
        Switch tabWorld = tabSwitch("ساعت جهانی", "world");
        root.addView(tabClock);
        root.addView(tabNotes);
        root.addView(tabStopwatch);
        root.addView(tabTimer);
        root.addView(tabWorld);

        root.addView(label("استایل صفحه زنگ"));
        Spinner alarmStyle = spinner(new String[]{
                "کلاسیک روشن",
                "تمرکز تیره",
                "طلوع گرم"
        });
        alarmStyle.setSelection(AppSettings.alarmScreenStyle(this));
        root.addView(alarmStyle, new LinearLayout.LayoutParams(-1, dp(54)));

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);

        Runnable saveSettings = () -> {
            if (!tabClock.isChecked() && !tabNotes.isChecked()
                    && !tabStopwatch.isChecked() && !tabTimer.isChecked()
                    && !tabWorld.isChecked()) {
                Toast.makeText(this, "حداقل یک تب باید فعال باشد", Toast.LENGTH_SHORT).show();
                return;
            }
            String selectedLanguage =
                    AppSettings.languageCodes()[language.getSelectedItemPosition()];
            boolean changed = AppSettings.tabEnabled(this, "clock") != tabClock.isChecked()
                    || AppSettings.tabEnabled(this, "noforget") != tabNotes.isChecked()
                    || AppSettings.tabEnabled(this, "stopwatch") != tabStopwatch.isChecked()
                    || AppSettings.tabEnabled(this, "timer") != tabTimer.isChecked()
                    || AppSettings.tabEnabled(this, "world") != tabWorld.isChecked()
                    || AppSettings.palette(this) != palette.getSelectedItemPosition()
                    || !AppSettings.language(this).equals(selectedLanguage)
                    || AppSettings.defaultCalendar(this) != calendar.getSelectedItemPosition()
                    || AppSettings.clockLayoutMode(this) != layout.getSelectedItemPosition()
                    || AppSettings.alarmScreenStyle(this)
                    != alarmStyle.getSelectedItemPosition();
            if (!changed) return;
            AppSettings.setTabEnabled(this, "clock", tabClock.isChecked());
            AppSettings.setTabEnabled(this, "noforget", tabNotes.isChecked());
            AppSettings.setTabEnabled(this, "stopwatch", tabStopwatch.isChecked());
            AppSettings.setTabEnabled(this, "timer", tabTimer.isChecked());
            AppSettings.setTabEnabled(this, "world", tabWorld.isChecked());
            AppSettings.setPalette(this, palette.getSelectedItemPosition());
            AppSettings.setLanguage(this, selectedLanguage);
            AppSettings.setDefaultCalendar(this, calendar.getSelectedItemPosition());
            AppSettings.setClockLayoutMode(this, layout.getSelectedItemPosition());
            AppSettings.setAlarmScreenStyle(this, alarmStyle.getSelectedItemPosition());

            try { DateNotificationService.start(this); } catch (Exception ignored) {}
            ClockWidgetProvider.updateAll(this);
            NoForgetWidgetProvider.updateAll(this);

            setResult(RESULT_OK);
        };
        watch(palette, saveSettings);
        watch(language, saveSettings);
        watch(calendar, saveSettings);
        watch(layout, saveSettings);
        watch(alarmStyle, saveSettings);
        CompoundButton.OnCheckedChangeListener saveTabs = (button, checked) -> {
            if (!checked && !tabClock.isChecked() && !tabNotes.isChecked()
                    && !tabStopwatch.isChecked() && !tabTimer.isChecked()
                    && !tabWorld.isChecked()) {
                button.setChecked(true);
                Toast.makeText(this, "حداقل یک تب باید فعال باشد", Toast.LENGTH_SHORT).show();
                return;
            }
            saveSettings.run();
        };
        tabClock.setOnCheckedChangeListener(saveTabs);
        tabNotes.setOnCheckedChangeListener(saveTabs);
        tabStopwatch.setOnCheckedChangeListener(saveTabs);
        tabTimer.setOnCheckedChangeListener(saveTabs);
        tabWorld.setOnCheckedChangeListener(saveTabs);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private TextView label(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(AppSettings.textSecondary(this));
        v.setTextSize(13);
        v.setPadding(0, dp(12), 0, dp(4));
        return v;
    }

    private Switch tabSwitch(String label, String key) {
        Switch control = new Switch(this);
        control.setText(label);
        control.setTextColor(AppSettings.textPrimary(this));
        control.setChecked(AppSettings.tabEnabled(this, key));
        control.setPadding(0, dp(4), 0, dp(4));
        control.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(48)));
        return control;
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

    private void watch(Spinner spinner, Runnable changed) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                changed.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
