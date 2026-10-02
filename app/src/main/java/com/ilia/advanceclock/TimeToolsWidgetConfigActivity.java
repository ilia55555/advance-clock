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
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public final class TimeToolsWidgetConfigActivity extends Activity {
    private int widgetId;

    private Spinner defaultTab;
    private Switch showReset;
    private Switch showLap;
    private Switch showOpen;

    private NumberPicker timerHours;
    private NumberPicker timerMinutes;
    private NumberPicker timerSeconds;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setResult(RESULT_CANCELED);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(AppSettings.background(this));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(dp(18), dp(12), dp(18), dp(18));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        addTopBar(root);

        TextView intro = text(
                AppString.get(R.string.time_tools_widget_settings_description),
                13,
                AppSettings.textSecondary(this));
        intro.setPadding(0, 0, 0, dp(12));
        root.addView(intro);

        root.addView(sectionTitle(
                AppString.get(R.string.time_tools_widget_default_tab)));

        defaultTab = spinner(new String[]{
                AppString.get(R.string.tab_stopwatch),
                AppString.get(R.string.tab_timer)
        });
        defaultTab.setSelection(
                TimeToolsWidgetPrefs.activeTab(this, widgetId));
        root.addView(defaultTab, fieldLp());

        root.addView(sectionTitle(
                AppString.get(R.string.runtime_text_0232)));

        showReset = addSwitch(
                root,
                AppString.get(R.string.time_tools_widget_show_reset),
                TimeToolsWidgetPrefs.showReset(this, widgetId));

        showLap = addSwitch(
                root,
                AppString.get(R.string.time_tools_widget_show_lap),
                TimeToolsWidgetPrefs.showLap(this, widgetId));

        showOpen = addSwitch(
                root,
                AppString.get(R.string.time_tools_widget_show_open),
                TimeToolsWidgetPrefs.showOpen(this, widgetId));

        root.addView(sectionTitle(
                AppString.get(R.string.time_tools_widget_timer_default)));

        long defaultMillis =
                TimeToolsWidgetPrefs.timerDefaultMillis(
                        this,
                        widgetId);
        long totalSeconds = defaultMillis / 1000L;

        LinearLayout duration = new LinearLayout(this);
        duration.setOrientation(LinearLayout.HORIZONTAL);
        duration.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        duration.setGravity(Gravity.CENTER);
        duration.setPadding(dp(8), dp(8), dp(8), dp(8));
        duration.setBackgroundResource(R.drawable.bg_card);

        timerHours = picker(
                (int) Math.min(999L, totalSeconds / 3600L),
                0,
                999);
        timerMinutes = picker(
                (int) ((totalSeconds / 60L) % 60L),
                0,
                59);
        timerSeconds = picker(
                (int) (totalSeconds % 60L),
                0,
                59);

        duration.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0013),
                        timerHours),
                new LinearLayout.LayoutParams(0, dp(118), 1f));
        duration.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0037),
                        timerMinutes),
                new LinearLayout.LayoutParams(0, dp(118), 1f));
        duration.addView(
                pickerColumn(
                        AppString.get(R.string.runtime_text_0038),
                        timerSeconds),
                new LinearLayout.LayoutParams(0, dp(118), 1f));

        root.addView(duration, new LinearLayout.LayoutParams(-1, -2));

        TextView resizeHint = text(
                AppString.get(R.string.runtime_text_0542),
                12,
                AppSettings.textSecondary(this));
        resizeHint.setPadding(0, dp(14), 0, dp(4));
        root.addView(resizeHint);

        page.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f));

        Button save = new Button(this);
        save.setText(AppString.get(R.string.runtime_text_0005));
        save.setAllCaps(false);
        save.setTextSize(15);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundColor(AppSettings.primaryColor(this));
        save.setOnClickListener(v -> saveAndFinish());

        LinearLayout.LayoutParams saveLp =
                new LinearLayout.LayoutParams(-1, dp(54));
        saveLp.setMargins(dp(18), dp(8), dp(18), dp(14));
        page.addView(save, saveLp);

        setContentView(page);
        AppSettings.applyFullscreenInsets(page);
        AppSettings.playFullscreenEnter(this);
    }

    private void addTopBar(LinearLayout root) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(
                AppSettings.textPrimary(this),
                PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(
                AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        top.addView(
                close,
                new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText(
                AppString.get(
                        R.string.time_tools_widget_settings_title));
        title.setTextSize(23);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD);
        title.setGravity(
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextDirection(
                View.TEXT_DIRECTION_FIRST_STRONG);
        top.addView(
                title,
                new LinearLayout.LayoutParams(0, dp(56), 1f));

        root.addView(top);
    }

    private TextView sectionTitle(String value) {
        TextView title = text(
                value,
                16,
                AppSettings.textPrimary(this));
        title.setTypeface(
                null,
                android.graphics.Typeface.BOLD);
        title.setPadding(0, dp(12), 0, dp(6));
        return title;
    }

    private Switch addSwitch(
            LinearLayout root,
            String title,
            boolean checked) {
        Switch control = new Switch(this);
        control.setText(title);
        control.setTextSize(14);
        control.setTextColor(AppSettings.textPrimary(this));
        control.setChecked(checked);
        control.setGravity(Gravity.CENTER_VERTICAL);
        control.setPadding(dp(12), 0, dp(12), 0);
        control.setBackgroundResource(R.drawable.bg_card);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, dp(52));
        lp.bottomMargin = dp(7);
        root.addView(control, lp);
        return control;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setLayoutDirection(
                AppSettings.layoutDirection(this));
        return spinner;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, dp(54));
        lp.bottomMargin = dp(8);
        return lp;
    }

    private NumberPicker picker(
            int value,
            int min,
            int max) {
        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(Math.max(min, Math.min(max, value)));
        picker.setWrapSelectorWheel(false);
        return picker;
    }

    private LinearLayout pickerColumn(
            String labelValue,
            NumberPicker picker) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);

        TextView label = text(
                labelValue,
                11,
                AppSettings.textSecondary(this));
        label.setGravity(Gravity.CENTER);
        box.addView(
                label,
                new LinearLayout.LayoutParams(-1, dp(24)));
        box.addView(
                picker,
                new LinearLayout.LayoutParams(-1, 0, 1f));
        return box;
    }

    private void saveAndFinish() {
        TimeToolsWidgetPrefs.setActiveTab(
                this,
                widgetId,
                defaultTab.getSelectedItemPosition());
        TimeToolsWidgetPrefs.setShowReset(
                this,
                widgetId,
                showReset.isChecked());
        TimeToolsWidgetPrefs.setShowLap(
                this,
                widgetId,
                showLap.isChecked());
        TimeToolsWidgetPrefs.setShowOpen(
                this,
                widgetId,
                showOpen.isChecked());

        long totalSeconds =
                timerHours.getValue() * 3600L
                        + timerMinutes.getValue() * 60L
                        + timerSeconds.getValue();

        TimeToolsWidgetPrefs.setTimerDefaultMillis(
                this,
                widgetId,
                Math.max(1L, totalSeconds) * 1000L);

        TimeToolsWidgetProvider.updateAll(this);

        Intent result = new Intent();
        result.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                widgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private TextView text(
            String value,
            int size,
            int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        return view;
    }

    private int dp(int value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density);
    }
}
