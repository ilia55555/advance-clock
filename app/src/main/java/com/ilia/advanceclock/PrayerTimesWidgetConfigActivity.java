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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public final class PrayerTimesWidgetConfigActivity extends Activity {
    private static final int[] COLORS = {
            0xFFF2F8FF, 0xFFA9C9E8, 0xFF56D8FF, 0xFFFFC857,
            0xFFFF9F43, 0xFFE85D75, 0xFF65D6A6, 0xFFB58CFF,
            0xFFFF7EB6, 0xFFB8C7D9, 0xFF173F5F, 0xFF081D33
    };

    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private Spinner background;
    private Spinner mainColor;
    private Spinner secondaryColor;
    private Spinner accentColor;
    private Spinner activeColor;
    private Spinner fontSize;
    private Switch showHeader;
    private Switch showDate;
    private Spinner timeMode;
    private Switch showIcons;
    private final Switch[] shownPrayerTimes = new Switch[8];
    private static final String[] PRAYER_TIME_KEYS = {
            "fajr", "sunrise", "dhuhr", "asr",
            "sunset", "maghrib", "isha", "midnight"
    };
    private static final String[] PRAYER_TIME_LABELS = {
            "صبح", "طلوع", "ظهر", "عصر",
            "غروب", "مغرب", "عشاء", "نیمه‌شب"
    };
    private Switch showCurrentBadge;
    private Switch showManage;
    private Switch showScrollHint;
    private TextView preview;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(state);

        setResult(RESULT_CANCELED);
        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(AppSettings.background(this));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(20));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = label("تنظیمات ویجت اوقات شرعی", 23);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(58), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);

        preview = new TextView(this);
        preview.setText("اوقات شرعی\nافق تهران\nصبح ۰۴:۵۱   طلوع ۰۶:۱۸   ظهر ۱۲:۰۳   عصر ۱۵:۲۴\nغروب ۱۸:۰۲   مغرب ۱۸:۲۰   عشاء ۱۹:۴۸   نیمه‌شب ۲۳:۳۱");
        preview.setTextDirection(View.TEXT_DIRECTION_RTL);
        preview.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        preview.setPadding(dp(16), dp(12), dp(16), dp(12));
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(148));
        previewLp.bottomMargin = dp(12);
        root.addView(preview, previewLp);

        root.addView(section("ظاهر و رنگ"));
        root.addView(label("پس‌زمینه", 12));
        background = spinner(new String[]{
                "سرمه‌ای شیشه‌ای",
                "سرمه‌ای شفاف",
                "مشکی شیشه‌ای",
                "روشن"
        });
        background.setSelection(PrayerTimesWidgetPrefs.background(this, widgetId));
        root.addView(background, fieldLp());

        root.addView(label("رنگ متن اصلی", 12));
        mainColor = colorSpinner();
        mainColor.setSelection(colorPosition(
                PrayerTimesWidgetPrefs.mainTextColor(this, widgetId)));
        root.addView(mainColor, fieldLp());

        root.addView(label("رنگ متن فرعی", 12));
        secondaryColor = colorSpinner();
        secondaryColor.setSelection(colorPosition(
                PrayerTimesWidgetPrefs.secondaryTextColor(this, widgetId)));
        root.addView(secondaryColor, fieldLp());

        root.addView(label("رنگ تأکیدی و افق فعلی", 12));
        accentColor = colorSpinner();
        accentColor.setSelection(colorPosition(
                PrayerTimesWidgetPrefs.accentColor(this, widgetId)));
        root.addView(accentColor, fieldLp());

        root.addView(label("رنگ وقت فعال / اذان بعدی", 12));
        activeColor = colorSpinner();
        activeColor.setSelection(colorPosition(
                PrayerTimesWidgetPrefs.activePrayerColor(this, widgetId)));
        root.addView(activeColor, fieldLp());

        root.addView(label("اندازه نوشته‌ها", 12));
        fontSize = spinner(new String[]{"کوچک", "معمولی", "بزرگ"});
        fontSize.setSelection(PrayerTimesWidgetPrefs.fontSize(this, widgetId));
        root.addView(fontSize, fieldLp());

        root.addView(section("محتوا"));
        showHeader = addSwitch(root, "نمایش هدر «اوقات شرعی»",
                PrayerTimesWidgetPrefs.showHeader(this, widgetId));
        showDate = addSwitch(root, "نمایش تاریخ شمسی و قمری",
                PrayerTimesWidgetPrefs.showDate(this, widgetId));
        root.addView(label("نمایش زمان", 12));
        timeMode = spinner(new String[]{"ساعت محلی افق", "شمارش معکوس تا اذان بعدی"});
        timeMode.setSelection(PrayerTimesWidgetPrefs.timeMode(this, widgetId));
        root.addView(timeMode, fieldLp());
        showIcons = addSwitch(root, "نمایش آیکون‌های اوقات",
                PrayerTimesWidgetPrefs.showIcons(this, widgetId));
        root.addView(label("اوقات قابل نمایش (هر ۸ مورد به‌صورت پیش‌فرض فعال‌اند)", 12));
        for (int index = 0; index < shownPrayerTimes.length; index++) {
            shownPrayerTimes[index] = addSwitch(
                    root,
                    PRAYER_TIME_LABELS[index],
                    PrayerTimesWidgetPrefs.showPrayerTime(
                            this, widgetId, PRAYER_TIME_KEYS[index]));
        }
        showCurrentBadge = addSwitch(root, "برجسته‌کردن افق اصلی",
                PrayerTimesWidgetPrefs.showCurrentBadge(this, widgetId));
        showManage = addSwitch(root, "نمایش دکمه مدیریت افق‌ها",
                PrayerTimesWidgetPrefs.showManageButton(this, widgetId));
        showScrollHint = addSwitch(root, "نمایش راهنمای اسکرول",
                PrayerTimesWidgetPrefs.showScrollHint(this, widgetId));

        TextView hint = label(
                "در اندازه پیش‌فرض ۵×۲، افق اصلی به‌صورت کامل نمایش داده می‌شود. "
                        + "هر افق بعدی نیز همین اندازه کامل را دارد و با اسکرول عمودی دیده می‌شود؛ "
                        + "تنها با کوچک‌کردن ویجت، چیدمان فشرده فعال خواهد شد.",
                12);
        hint.setTextColor(AppSettings.textSecondary(this));
        hint.setPadding(0, dp(12), 0, dp(12));
        root.addView(hint);

        Button manage = new Button(this);
        manage.setText("مدیریت افق‌ها");
        manage.setAllCaps(false);
        manage.setTextColor(AppSettings.primaryColor(this));
        manage.setBackgroundResource(R.drawable.bg_soft_button);
        manage.setOnClickListener(v ->
                startActivity(new Intent(this, PrayerSettingsActivity.class)));
        root.addView(manage, new LinearLayout.LayoutParams(-1, dp(50)));

        Button reset = new Button(this);
        reset.setText("بازگردانی تنظیمات پیش‌فرض ویجت");
        reset.setAllCaps(false);
        reset.setTextColor(AppSettings.textSecondary(this));
        reset.setBackgroundResource(R.drawable.bg_field);
        LinearLayout.LayoutParams resetLp = new LinearLayout.LayoutParams(-1, dp(50));
        resetLp.topMargin = dp(8);
        root.addView(reset, resetLp);
        reset.setOnClickListener(v -> {
            PrayerTimesWidgetPrefs.clear(this, widgetId);
            recreate();
        });

        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        Button save = new Button(this);
        save.setText("ذخیره و اعمال");
        save.setAllCaps(false);
        save.setTextColor(0xFFFFFFFF);
        save.setTextSize(15);
        save.setBackgroundResource(R.drawable.bg_teal_button);
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(56));
        saveLp.setMargins(dp(18), dp(8), dp(18), dp(14));
        page.addView(save, saveLp);
        save.setOnClickListener(v -> saveAndFinish());

        setContentView(page);
        AppSettings.applyFullscreenInsets(page);
        AppSettings.playFullscreenEnter(this);

        bindPreview();
        refreshPreview();
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private void saveAndFinish() {
        PrayerTimesWidgetPrefs.setBackground(
                this, widgetId, background.getSelectedItemPosition());
        PrayerTimesWidgetPrefs.setMainTextColor(
                this, widgetId, COLORS[mainColor.getSelectedItemPosition()]);
        PrayerTimesWidgetPrefs.setSecondaryTextColor(
                this, widgetId, COLORS[secondaryColor.getSelectedItemPosition()]);
        PrayerTimesWidgetPrefs.setAccentColor(
                this, widgetId, COLORS[accentColor.getSelectedItemPosition()]);
        PrayerTimesWidgetPrefs.setActivePrayerColor(
                this, widgetId, COLORS[activeColor.getSelectedItemPosition()]);
        PrayerTimesWidgetPrefs.setFontSize(
                this, widgetId, fontSize.getSelectedItemPosition());
        PrayerTimesWidgetPrefs.setShowHeader(this, widgetId, showHeader.isChecked());
        PrayerTimesWidgetPrefs.setShowDate(this, widgetId, showDate.isChecked());
        PrayerTimesWidgetPrefs.setTimeMode(
                this, widgetId, timeMode.getSelectedItemPosition());
        PrayerTimesWidgetPrefs.setShowIcons(this, widgetId, showIcons.isChecked());
        for (int index = 0; index < shownPrayerTimes.length; index++) {
            PrayerTimesWidgetPrefs.setShowPrayerTime(
                    this,
                    widgetId,
                    PRAYER_TIME_KEYS[index],
                    shownPrayerTimes[index].isChecked());
        }
        PrayerTimesWidgetPrefs.setShowCurrentBadge(
                this, widgetId, showCurrentBadge.isChecked());
        PrayerTimesWidgetPrefs.setShowManageButton(
                this, widgetId, showManage.isChecked());
        PrayerTimesWidgetPrefs.setShowScrollHint(
                this, widgetId, showScrollHint.isChecked());

        PrayerTimesWidgetProvider.updateAll(this);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    private void bindPreview() {
        android.widget.AdapterView.OnItemSelectedListener listener =
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        refreshPreview();
                    }
                    @Override public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                };
        background.setOnItemSelectedListener(listener);
        mainColor.setOnItemSelectedListener(listener);
        secondaryColor.setOnItemSelectedListener(listener);
        accentColor.setOnItemSelectedListener(listener);
        activeColor.setOnItemSelectedListener(listener);
        fontSize.setOnItemSelectedListener(listener);
    }

    private void refreshPreview() {
        if (preview == null || background == null) return;
        int bg;
        switch (background.getSelectedItemPosition()) {
            case 1: bg = 0xCC0A1D33; break;
            case 2: bg = 0xEE0B0E13; break;
            case 3: bg = 0xFFF4F9FD; break;
            case 0:
            default: bg = 0xF20A1D33; break;
        }
        preview.setBackgroundColor(bg);
        preview.setTextColor(COLORS[mainColor.getSelectedItemPosition()]);
        float size = fontSize.getSelectedItemPosition() == 0 ? 11f
                : (fontSize.getSelectedItemPosition() == 2 ? 14f : 12.5f);
        preview.setTextSize(size);
    }

    private Switch addSwitch(LinearLayout root, String title, boolean checked) {
        Switch value = new Switch(this);
        value.setText(title);
        value.setTextSize(14);
        value.setTextColor(AppSettings.textPrimary(this));
        value.setChecked(checked);
        value.setGravity(Gravity.CENTER_VERTICAL);
        value.setPadding(dp(8), 0, dp(8), 0);
        value.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.bottomMargin = dp(6);
        root.addView(value, lp);
        return value;
    }

    private Spinner colorSpinner() {
        return spinner(new String[]{
                "سفید یخی", "آبی روشن", "فیروزه‌ای", "طلایی",
                "نارنجی", "قرمز", "سبز", "بنفش",
                "صورتی", "نقره‌ای", "سرمه‌ای", "سرمه‌ای تیره"
        });
    }

    private int colorPosition(int color) {
        for (int i = 0; i < COLORS.length; i++) {
            if (COLORS[i] == color) return i;
        }
        return 0;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private TextView section(String value) {
        TextView text = label(value, 17);
        text.setTypeface(null, android.graphics.Typeface.BOLD);
        text.setPadding(0, dp(18), 0, dp(8));
        return text;
    }

    private TextView label(String value, int size) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(size);
        text.setTextColor(AppSettings.textPrimary(this));
        text.setGravity(Gravity.START);
        text.setPadding(0, dp(7), 0, dp(4));
        return text;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.bottomMargin = dp(4);
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
