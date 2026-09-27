package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class PrayerSettingsActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(24));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("تنظیمات اذان");
        title.setTextSize(25);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);

        TextView intro = text(
                "این صفحه فعلاً فقط رابط کاربری است. محاسبه اوقات شرعی، مکان و پخش اذان در مرحله بعد متصل می‌شود.",
                12,
                AppSettings.textSecondary(this));
        intro.setPadding(0, 0, 0, dp(10));
        root.addView(intro);

        LinearLayout masterCard = card();
        TextView masterTitle = text("اذان و اوقات شرعی", 18, AppSettings.textPrimary(this));
        masterTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        masterCard.addView(masterTitle);

        Switch master = toggle("فعال‌سازی اذان", false);
        masterCard.addView(master);
        root.addView(masterCard, cardParams());

        LinearLayout locationCard = card();
        TextView locationTitle = text("مکان و زمان‌بندی", 17, AppSettings.textPrimary(this));
        locationTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        locationCard.addView(locationTitle);

        TextView locationHint = text(
                "شهر یا موقعیت برای محاسبه اوقات شرعی در این بخش انتخاب خواهد شد.",
                12,
                AppSettings.textSecondary(this));
        locationHint.setPadding(0, dp(4), 0, dp(8));
        locationCard.addView(locationHint);

        Button city = fieldButton("انتخاب شهر / موقعیت");
        city.setEnabled(false);
        city.setAlpha(0.72f);
        locationCard.addView(city, new LinearLayout.LayoutParams(-1, dp(52)));

        Button method = fieldButton("روش محاسبه اوقات شرعی");
        method.setEnabled(false);
        method.setAlpha(0.72f);
        LinearLayout.LayoutParams methodParams = new LinearLayout.LayoutParams(-1, dp(52));
        methodParams.topMargin = dp(8);
        locationCard.addView(method, methodParams);
        root.addView(locationCard, cardParams());

        LinearLayout prayersCard = card();
        TextView prayersTitle = text("اذان‌ها", 17, AppSettings.textPrimary(this));
        prayersTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        prayersCard.addView(prayersTitle);

        prayersCard.addView(prayerRow("اذان صبح", "— : —"));
        prayersCard.addView(prayerRow("اذان ظهر", "— : —"));
        prayersCard.addView(prayerRow("اذان مغرب", "— : —"));
        root.addView(prayersCard, cardParams());

        LinearLayout soundCard = card();
        TextView soundTitle = text("صدا و اعلان", 17, AppSettings.textPrimary(this));
        soundTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        soundCard.addView(soundTitle);

        Button sound = fieldButton("انتخاب صدای اذان");
        sound.setEnabled(false);
        sound.setAlpha(0.72f);
        LinearLayout.LayoutParams soundParams = new LinearLayout.LayoutParams(-1, dp(52));
        soundParams.topMargin = dp(8);
        soundCard.addView(sound, soundParams);

        Button preAlert = fieldButton("یادآوری قبل از اذان");
        preAlert.setEnabled(false);
        preAlert.setAlpha(0.72f);
        LinearLayout.LayoutParams preAlertParams = new LinearLayout.LayoutParams(-1, dp(52));
        preAlertParams.topMargin = dp(8);
        soundCard.addView(preAlert, preAlertParams);

        Switch vibrate = toggle("لرزش همراه اعلان", true);
        soundCard.addView(vibrate);
        root.addView(soundCard, cardParams());

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private LinearLayout prayerRow(String label, String time) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(0, dp(4), 0, dp(4));

        Switch enabled = toggle(label, true);
        row.addView(enabled, new LinearLayout.LayoutParams(0, dp(48), 1f));

        TextView value = text(time, 15, AppSettings.primaryColor(this));
        value.setGravity(Gravity.CENTER);
        value.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(value, new LinearLayout.LayoutParams(dp(72), dp(44)));
        return row;
    }

    private Switch toggle(String value, boolean checked) {
        Switch control = new Switch(this);
        control.setText(value);
        control.setTextColor(AppSettings.textPrimary(this));
        control.setTextSize(14);
        control.setChecked(checked);
        control.setPadding(0, dp(3), 0, dp(3));
        return control;
    }

    private Button fieldButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setTextSize(13);
        button.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        button.setPadding(dp(14), 0, dp(14), 0);
        button.setBackgroundResource(R.drawable.bg_field);
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        return params;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
