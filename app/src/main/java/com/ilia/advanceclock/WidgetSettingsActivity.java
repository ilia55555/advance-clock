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
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;

public final class WidgetSettingsActivity extends Activity {
    private int widgetId;
    private String kind;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        kind = getIntent().getStringExtra("widgetKind");

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            returnToHome();
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
        close.setOnClickListener(v -> returnToHome());
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

        root.addView(label("اندازه ویجت"));

        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sizeRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sizeRowLp = new LinearLayout.LayoutParams(-1, dp(132));
        root.addView(sizeRow, sizeRowLp);

        LinearLayout widthBox = pickerBox("عرض", WidgetPrefs.widthCells(this, widgetId), 2, 6);
        NumberPicker widthPicker = (NumberPicker) widthBox.getChildAt(1);
        sizeRow.addView(widthBox, new LinearLayout.LayoutParams(0, -1, 1f));

        View spacer = new View(this);
        sizeRow.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));

        LinearLayout heightBox = pickerBox("ارتفاع", WidgetPrefs.heightCells(this, widgetId), 1, 6);
        NumberPicker heightPicker = (NumberPicker) heightBox.getChildAt(1);
        sizeRow.addView(heightBox, new LinearLayout.LayoutParams(0, -1, 1f));

        TextView sizeHint = new TextView(this);
        sizeHint.setText("واحد اندازه، خانه‌های لانچر است. اندازه پیش‌فرض ۳ × ۲ است.");
        sizeHint.setTextColor(AppSettings.textSecondary(this));
        sizeHint.setTextSize(12);
        sizeHint.setPadding(0, dp(4), 0, dp(12));
        root.addView(sizeHint);

        Button save = new Button(this);
        save.setText("ذخیره");
        save.setTextColor(0xFFFFFFFF);
        save.setAllCaps(false);
        save.setBackgroundColor(AppSettings.secondaryColor(this));
        root.addView(save, new LinearLayout.LayoutParams(-1, dp(56)));

        Button cancel = new Button(this);
        cancel.setText("انصراف");
        cancel.setAllCaps(false);
        cancel.setTextColor(AppSettings.primaryColor(this));
        cancel.setBackgroundResource(R.drawable.bg_soft_button);
        LinearLayout.LayoutParams cancelLp = new LinearLayout.LayoutParams(-1, dp(50));
        cancelLp.topMargin = dp(8);
        root.addView(cancel, cancelLp);

        setContentView(root);

        save.setOnClickListener(v -> {
            int widthCells = widthPicker.getValue();
            int heightCells = heightPicker.getValue();

            WidgetPrefs.setThemeMode(this, widgetId, theme.getSelectedItemPosition());
            WidgetPrefs.setAccent(this, widgetId, accent.getSelectedItemPosition());
            WidgetPrefs.setSizeCells(this, widgetId, widthCells, heightCells);

            applyRequestedSize(widthCells, heightCells);
            updateWidget();
            returnToHome();
        });

        cancel.setOnClickListener(v -> returnToHome());
    }

    private LinearLayout pickerBox(String title, int value, int min, int max) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundResource(R.drawable.bg_field);
        box.setPadding(dp(10), dp(6), dp(10), dp(6));

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(AppSettings.textSecondary(this));
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        box.addView(label, new LinearLayout.LayoutParams(-1, dp(28)));

        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(Math.max(min, Math.min(max, value)));
        picker.setWrapSelectorWheel(false);
        box.addView(picker, new LinearLayout.LayoutParams(-1, 0, 1f));
        return box;
    }

    private void applyRequestedSize(int widthCells, int heightCells) {
        AppWidgetManager manager = AppWidgetManager.getInstance(this);

        // A widget provider cannot force the launcher to move its outer frame,
        // but updating size options immediately redraws our widget using the
        // requested cell size. Launchers that honor provider option updates use
        // these values directly; other launchers keep their existing frame.
        int widthDp = cellToDp(widthCells);
        int heightDp = cellToDp(heightCells);

        Bundle options = new Bundle();
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp);

        try {
            manager.updateAppWidgetOptions(widgetId, options);
        } catch (Exception ignored) {
        }
    }

    private int cellToDp(int cells) {
        return Math.max(40, cells * 70 - 30);
    }

    private void updateWidget() {
        if ("note".equals(kind)) {
            NoForgetWidgetProvider.updateAll(this);
        } else {
            ClockWidgetProvider.updateAll(this);
        }
    }

    private void returnToHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(home);

        try {
            finishAndRemoveTask();
        } catch (Exception ignored) {
            finish();
        }
    }

    @Override public void onBackPressed() {
        returnToHome();
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
