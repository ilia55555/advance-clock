package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class SmartAlarmActivity extends Activity {
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
        title.setText("هشدار هوشمند");
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
                "متن معمولی یا JSON را وارد کنید. فعلاً فقط چیدمان آماده است و تحلیل متن و ساخت هشدار در مرحله بعد متصل می‌شود.",
                12,
                AppSettings.textSecondary(this));
        intro.setPadding(0, 0, 0, dp(10));
        root.addView(intro);

        LinearLayout inputCard = card();

        TextView inputTitle = text("ورودی هوشمند", 18, AppSettings.textPrimary(this));
        inputTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        inputCard.addView(inputTitle);

        LinearLayout modes = new LinearLayout(this);
        modes.setOrientation(LinearLayout.HORIZONTAL);
        modes.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        modes.setGravity(Gravity.START);
        modes.setPadding(0, dp(8), 0, dp(8));

        TextView textMode = chip("متن");
        modes.addView(textMode, chipParams());

        TextView jsonMode = chip("JSON");
        LinearLayout.LayoutParams jsonParams = chipParams();
        jsonParams.setMarginStart(dp(6));
        modes.addView(jsonMode, jsonParams);

        inputCard.addView(modes);

        EditText input = new EditText(this);
        input.setMinHeight(dp(220));
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackgroundResource(R.drawable.bg_field);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setTextColor(AppSettings.textPrimary(this));
        input.setHintTextColor(AppSettings.textSecondary(this));
        input.setTextSize(14);
        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(
                "متن برنامه خاموشی، جلسه، یادآوری یا JSON را اینجا وارد کنید…\n\n"
                        + "مثال: تاریخ، ساعت شروع، ساعت پایان، عنوان و توضیحات");
        inputCard.addView(input, new LinearLayout.LayoutParams(-1, dp(230)));

        Button analyze = primaryButton("تحلیل و ساخت هشدارها");
        analyze.setEnabled(false);
        analyze.setAlpha(0.58f);
        LinearLayout.LayoutParams analyzeParams = new LinearLayout.LayoutParams(-1, dp(54));
        analyzeParams.topMargin = dp(10);
        inputCard.addView(analyze, analyzeParams);

        root.addView(inputCard, cardParams());

        LinearLayout previewCard = card();

        TextView previewTitle = text(
                "پیش‌نمایش هشدارهای شناسایی‌شده",
                17,
                AppSettings.textPrimary(this));
        previewTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        previewCard.addView(previewTitle);

        TextView previewHint = text(
                "بعد از اتصال منطق، هر تاریخ و ساعت شروع به صورت یک هشدار جدا در این بخش نمایش داده می‌شود.",
                12,
                AppSettings.textSecondary(this));
        previewHint.setPadding(0, dp(4), 0, dp(10));
        previewCard.addView(previewHint);

        previewCard.addView(previewRow("هشدار ۱", "تاریخ  —  ساعت شروع — ساعت پایان"));
        previewCard.addView(previewRow("هشدار ۲", "تاریخ  —  ساعت شروع — ساعت پایان"));

        Button saveAll = primaryButton("ذخیره همه هشدارها");
        saveAll.setEnabled(false);
        saveAll.setAlpha(0.58f);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(54));
        saveParams.topMargin = dp(10);
        previewCard.addView(saveAll, saveParams);

        root.addView(previewCard, cardParams());

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private LinearLayout previewRow(String titleText, String detailText) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(12), dp(10), dp(12), dp(10));
        row.setBackgroundResource(R.drawable.bg_field);

        TextView title = text(titleText, 14, AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(title);

        TextView detail = text(detailText, 12, AppSettings.textSecondary(this));
        detail.setPadding(0, dp(2), 0, 0);
        row.addView(detail);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(7);
        row.setLayoutParams(params);
        return row;
    }

    private TextView chip(String value) {
        TextView chip = text(value, 12, AppSettings.primaryColor(this));
        chip.setGravity(Gravity.CENTER);
        chip.setBackgroundResource(R.drawable.bg_soft_button);
        chip.setPadding(dp(12), 0, dp(12), 0);
        return chip;
    }

    private LinearLayout.LayoutParams chipParams() {
        return new LinearLayout.LayoutParams(-2, dp(36));
    }

    private Button primaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_orange_button);
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
