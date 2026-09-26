package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class WidgetCenterActivity extends Activity {
    private LinearLayout list;

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
        title.setText("ویجت‌ها و تنظیمات");
        title.setTextSize(25);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setBackgroundColor(0x00000000);
        close.setColorFilter(AppSettings.textPrimary(this));
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);

        TextView intro = text(
                "همهٔ ویجت‌های Advance از اینجا قابل افزودن و مدیریت هستند. "
                        + "برای هر نمونهٔ نصب‌شده می‌توانید تنظیمات جداگانه داشته باشید.",
                12,
                AppSettings.textSecondary(this));
        intro.setPadding(0, 0, 0, dp(10));
        root.addView(intro);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, new LinearLayout.LayoutParams(-1, -2));

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    @Override protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        list.removeAllViews();

        addWidgetCard(
                "ساعت و هشدارها",
                "نمایش ساعت، تاریخ و هشدارهای پیش رو",
                ClockWidgetProvider.class,
                "clock");

        addWidgetCard(
                "یادداشت‌ها",
                "نمایش یادداشت‌ها و یادآوری‌های NoForget",
                NoForgetWidgetProvider.class,
                "note");

        addWidgetCard(
                "ساعت جهانی",
                "نمایش هم‌زمان سه منطقه زمانی اول تب ساعت جهانی",
                WorldClockWidgetProvider.class,
                "world");

        addWidgetCard(
                "یادآوری فایل‌ها",
                "دسترسی سریع و یادآوری عکس، صوت، ویدیو، متن، PDF و فایل‌های دیگر",
                MediaWidgetProvider.class,
                "media");
    }

    private void addWidgetCard(
            String titleText,
            String description,
            Class<?> provider,
            String kind) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);
        int[] ids = manager.getAppWidgetIds(new ComponentName(this, provider));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(15), dp(14), dp(15), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = text(titleText, 18, AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1f));

        TextView count = text(ids.length + " نصب‌شده", 11, AppSettings.primaryColor(this));
        count.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(count);
        card.addView(header);

        TextView desc = text(description, 12, AppSettings.textSecondary(this));
        desc.setPadding(0, dp(4), 0, dp(10));
        card.addView(desc);

        ImageView preview = new ImageView(this);
        preview.setImageResource(previewResource(kind));
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setAdjustViewBounds(true);
        preview.setContentDescription("پیش‌نمایش " + titleText);
        LinearLayout.LayoutParams previewLp = new LinearLayout.LayoutParams(-1, dp(170));
        previewLp.bottomMargin = dp(10);
        card.addView(preview, previewLp);

        Button add = new Button(this);
        add.setText("افزودن به صفحه اصلی");
        add.setAllCaps(false);
        add.setTextColor(0xFFFFFFFF);
        add.setBackgroundColor(AppSettings.primaryColor(this));
        add.setOnClickListener(v -> pin(provider));
        card.addView(add, new LinearLayout.LayoutParams(-1, dp(52)));

        if (ids.length > 0) {
            TextView installed = text(
                    "نمونه‌های نصب‌شده",
                    12,
                    AppSettings.textSecondary(this));
            installed.setPadding(0, dp(12), 0, dp(4));
            card.addView(installed);

            LinearLayout buttons = new LinearLayout(this);
            buttons.setOrientation(LinearLayout.HORIZONTAL);
            buttons.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            buttons.setGravity(Gravity.START);
            card.addView(buttons, new LinearLayout.LayoutParams(-1, -2));

            for (int i = 0; i < ids.length; i++) {
                final int widgetId = ids[i];
                Button edit = new Button(this);
                edit.setText("ویرایش " + (i + 1));
                edit.setAllCaps(false);
                edit.setTextSize(11);
                edit.setTextColor(AppSettings.primaryColor(this));
                edit.setBackgroundResource(R.drawable.bg_soft_button);
                edit.setPadding(dp(9), 0, dp(9), 0);
                edit.setOnClickListener(v -> editWidget(kind, widgetId));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        dp(42));
                lp.setMargins(0, 0, dp(6), dp(6));
                buttons.addView(edit, lp);
            }
        }

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.bottomMargin = dp(12);
        list.addView(card, cardLp);
    }

    private int previewResource(String kind) {
        if ("note".equals(kind)) return R.drawable.preview_widget_notes;
        if ("world".equals(kind)) return R.drawable.preview_widget_world;
        if ("media".equals(kind)) return R.drawable.preview_widget_media;
        return R.drawable.preview_widget_clock;
    }

    private void pin(Class<?> provider) {
        if (Build.VERSION.SDK_INT < 26) {
            Toast.makeText(
                    this,
                    "ویجت را از فهرست ویجت‌های لانچر اضافه کنید.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(
                    this,
                    "لانچر شما افزودن مستقیم ویجت را پشتیبانی نمی‌کند؛ "
                            + "از فهرست ویجت‌های صفحه اصلی استفاده کنید.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        boolean opened = manager.requestPinAppWidget(
                new ComponentName(this, provider),
                null,
                null);

        Toast.makeText(
                this,
                opened
                        ? "درخواست افزودن ویجت به لانچر ارسال شد."
                        : "لانچر درخواست افزودن ویجت را نپذیرفت.",
                Toast.LENGTH_SHORT).show();
    }

    private void editWidget(String kind, int widgetId) {
        Intent intent;
        if ("world".equals(kind)) {
            intent = new Intent(this, WorldClockWidgetConfigActivity.class);
        } else if ("media".equals(kind)) {
            intent = new Intent(this, MediaWidgetConfigActivity.class)
                    .putExtra("editExisting", true)
                    .putExtra("returnToCenter", true);
        } else {
            intent = new Intent(this, WidgetSettingsActivity.class)
                    .putExtra("widgetKind", kind)
                    .putExtra("returnToCenter", true);
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        startActivity(intent);
    }

    private TextView text(String value, int size, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        v.setGravity(Gravity.START);
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
