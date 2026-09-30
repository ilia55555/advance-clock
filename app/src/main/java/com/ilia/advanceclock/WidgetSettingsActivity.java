package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

public final class WidgetSettingsActivity extends Activity {
    private int widgetId;
    private String kind;
    private boolean editExisting;

    private Spinner theme;
    private Spinner palette;
    private Spinner opacity;
    private Spinner fontSize;
    private Spinner timeFormat;
    private Spinner sortMode;
    private NumberPicker maxItems;
    private WidgetPreviewView preview;

    private Switch showHeader;
    private Switch showTime;
    private Switch showDate;
    private Switch showSeconds;
    private Switch showAdd;
    private Switch showSettings;
    private Switch showSection;
    private Switch showPriority;
    private Switch showMetadata;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        kind = getIntent().getStringExtra("widgetKind");
        editExisting = getIntent().getBooleanExtra("editExisting", false)
                || WidgetPrefs.hasSavedConfig(this, widgetId);

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        if (kind == null || kind.trim().isEmpty()) {
            kind = inferKind(widgetId);
        }

        if ("media".equals(kind)) {
            Intent media = new Intent(this, MediaWidgetConfigActivity.class)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                    .putExtra("editExisting", editExisting);
            startActivity(media);
            finish();
            return;
        }

        // Launcher configuration flows require RESULT_OK to keep the widget.
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
        root.setPadding(dp(18), dp(12), dp(18), dp(20));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        addTopBar(root);
        root.addView(sectionTitle(AppString.get(R.string.runtime_text_0112)));
        preview = new WidgetPreviewView(this);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(150)));
        addAppearanceSection(root);
        addHeaderSection(root);
        addContentSection(root);
        addResizeSection(root);
        addActions(root);
        bindPreviewUpdates();
        refreshPreview();

        page.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f));

        setContentView(page);
        AppSettings.applyFullscreenInsets(page);
        AppSettings.playFullscreenEnter(this);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private void addTopBar(LinearLayout root) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("clock".equals(kind)
                ? AppString.get(R.string.runtime_text_0240)
                : AppString.get(R.string.runtime_text_0241));
        title.setTextSize(23);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(top);
    }

    private void addAppearanceSection(LinearLayout root) {
        root.addView(sectionTitle(AppString.get(R.string.runtime_text_0042)));

        root.addView(fieldLabel(AppString.get(R.string.runtime_text_0043)));
        theme = spinner(new String[]{
                AppString.get(R.string.runtime_text_0220),
                AppString.get(R.string.runtime_text_0044),
                AppString.get(R.string.runtime_text_0045)
        });
        theme.setSelection(WidgetPrefs.themeMode(this, widgetId));
        root.addView(theme, fieldLp());

        root.addView(fieldLabel(AppString.get(R.string.runtime_text_0046)));
        palette = spinner(AppSettings.paletteNames());
        palette.setSelection(WidgetPrefs.palette(this, widgetId));
        root.addView(palette, fieldLp());

        root.addView(fieldLabel(AppString.get(R.string.runtime_text_0047)));
        opacity = spinner(new String[]{
                AppString.get(R.string.runtime_text_0534),
                AppString.get(R.string.runtime_text_0535),
                AppString.get(R.string.runtime_text_0536)
        });
        opacity.setSelection(WidgetPrefs.backgroundOpacityMode(this, widgetId));
        root.addView(opacity, fieldLp());

        root.addView(fieldLabel(AppString.get(R.string.runtime_text_0048)));
        fontSize = spinner(new String[]{
                AppString.get(R.string.runtime_text_0049),
                AppString.get(R.string.runtime_text_0050),
                AppString.get(R.string.runtime_text_0051)
        });
        fontSize.setSelection(WidgetPrefs.fontSizeMode(this, widgetId));
        root.addView(fontSize, fieldLp());
    }

    private void addHeaderSection(LinearLayout root) {
        root.addView(sectionTitle(AppString.get(R.string.runtime_text_0225)));

        showHeader = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0052),
                AppString.get(R.string.runtime_text_0690),
                WidgetPrefs.showHeader(this, widgetId));

        showTime = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0053),
                AppString.get(R.string.runtime_text_0691),
                WidgetPrefs.showTime(this, widgetId));

        root.addView(fieldLabel(AppString.get(R.string.runtime_text_0226)));
        timeFormat = spinner(new String[]{
                AppString.get(R.string.runtime_text_0227),
                AppString.get(R.string.runtime_text_0228),
                AppString.get(R.string.runtime_text_0229)
        });
        timeFormat.setSelection(
                WidgetPrefs.timeFormatMode(this, widgetId));
        root.addView(timeFormat, fieldLp());

        showSeconds = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0230),
                AppString.get(R.string.runtime_text_0692),
                WidgetPrefs.showSeconds(this, widgetId));

        showDate = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0054),
                AppString.get(R.string.runtime_text_0693),
                WidgetPrefs.showDate(this, widgetId));

        showAdd = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0694),
                AppString.get(R.string.runtime_text_0695),
                WidgetPrefs.showAddButton(this, widgetId));

        showSettings = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0055),
                AppString.get(R.string.runtime_text_0696),
                WidgetPrefs.showSettingsButton(this, widgetId));

        showSection = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0231),
                AppString.get(R.string.runtime_text_0697),
                WidgetPrefs.showSectionLabel(this, widgetId));

        showHeader.setOnCheckedChangeListener((button, checked) -> {
            updateHeaderControlState();
            refreshPreview();
        });
        showTime.setOnCheckedChangeListener((button, checked) -> {
            updateHeaderControlState();
            refreshPreview();
        });
        updateHeaderControlState();
    }

    private void addContentSection(LinearLayout root) {
        root.addView(sectionTitle(AppString.get(R.string.runtime_text_0232)));

        showPriority = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0056),
                AppString.get(R.string.runtime_text_0698),
                WidgetPrefs.showPriority(this, widgetId));

        showMetadata = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0057),
                AppString.get(R.string.runtime_text_0699),
                WidgetPrefs.showMetadata(this, widgetId));

        root.addView(fieldLabel(AppString.get(R.string.runtime_text_0233)));
        LinearLayout countBox = pickerBox(
                AppString.get(R.string.runtime_text_0234),
                WidgetPrefs.maxItems(this, widgetId),
                1,
                10);
        maxItems = (NumberPicker) countBox.getChildAt(1);
        root.addView(countBox, new LinearLayout.LayoutParams(-1, dp(122)));

        root.addView(fieldLabel(AppString.get(R.string.runtime_text_0058)));
        sortMode = spinner("clock".equals(kind)
                ? new String[]{
                AppString.get(R.string.runtime_text_0235),
                AppString.get(R.string.runtime_text_0236),
                AppString.get(R.string.runtime_text_0237)
        }
                : new String[]{
                AppString.get(R.string.runtime_text_0238),
                AppString.get(R.string.runtime_text_0239),
                AppString.get(R.string.runtime_text_0237)
        });
        sortMode.setSelection(WidgetPrefs.sortMode(this, widgetId));
        root.addView(sortMode, fieldLp());
    }

    private void addResizeSection(LinearLayout root) {
        root.addView(hint(
                AppString.get(R.string.runtime_text_0542)));
    }

    private void addActions(LinearLayout root) {
        Button reset = softButton(AppString.get(R.string.runtime_text_0700));
        LinearLayout.LayoutParams resetLp = buttonLp();
        resetLp.topMargin = dp(8);
        root.addView(reset, resetLp);
        reset.setOnClickListener(v -> {
            WidgetPrefs.clear(this, widgetId);
            updateWidget();
            recreate();
        });
    }

    private void saveSettings() {
        WidgetPrefs.setThemeMode(this, widgetId, theme.getSelectedItemPosition());
        WidgetPrefs.setPalette(this, widgetId, palette.getSelectedItemPosition());
        WidgetPrefs.setBackgroundOpacityMode(
                this, widgetId, opacity.getSelectedItemPosition());
        WidgetPrefs.setFontSizeMode(
                this, widgetId, fontSize.getSelectedItemPosition());

        WidgetPrefs.setShowHeader(this, widgetId, showHeader.isChecked());
        WidgetPrefs.setShowTime(this, widgetId, showTime.isChecked());
        WidgetPrefs.setTimeFormatMode(
                this, widgetId, timeFormat.getSelectedItemPosition());
        WidgetPrefs.setShowSeconds(this, widgetId, showSeconds.isChecked());
        WidgetPrefs.setShowDate(this, widgetId, showDate.isChecked());
        WidgetPrefs.setShowAddButton(this, widgetId, showAdd.isChecked());
        WidgetPrefs.setShowSettingsButton(this, widgetId, showSettings.isChecked());
        WidgetPrefs.setShowSectionLabel(this, widgetId, showSection.isChecked());

        WidgetPrefs.setShowPriority(this, widgetId, showPriority.isChecked());
        WidgetPrefs.setShowMetadata(this, widgetId, showMetadata.isChecked());
        WidgetPrefs.setMaxItems(this, widgetId, maxItems.getValue());
        WidgetPrefs.setSortMode(this, widgetId, sortMode.getSelectedItemPosition());

        updateWidget();

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);

    }

    private void updateHeaderControlState() {
        boolean headerEnabled = showHeader != null && showHeader.isChecked();
        boolean timeEnabled = headerEnabled && showTime != null && showTime.isChecked();

        if (showTime != null) {
            showTime.setEnabled(headerEnabled);
            showTime.setAlpha(headerEnabled ? 1f : 0.5f);
        }
        if (showDate != null) {
            showDate.setEnabled(headerEnabled);
            showDate.setAlpha(headerEnabled ? 1f : 0.5f);
        }
        if (showAdd != null) {
            showAdd.setEnabled(headerEnabled);
            showAdd.setAlpha(headerEnabled ? 1f : 0.5f);
        }
        if (showSettings != null) {
            showSettings.setEnabled(headerEnabled);
            showSettings.setAlpha(headerEnabled ? 1f : 0.5f);
        }
        if (timeFormat != null) {
            timeFormat.setEnabled(timeEnabled);
            timeFormat.setAlpha(timeEnabled ? 1f : 0.5f);
        }
        if (showSeconds != null) {
            showSeconds.setEnabled(timeEnabled);
            showSeconds.setAlpha(timeEnabled ? 1f : 0.5f);
        }
    }

    private void updateWidget() {
        if ("note".equals(kind)) {
            NoForgetWidgetProvider.updateAll(this);
        } else {
            ClockWidgetProvider.updateAll(this);
        }
    }

    private void bindPreviewUpdates() {
        watch(theme);
        watch(palette);
        watch(opacity);
        watch(fontSize);
        watch(timeFormat);
        watch(sortMode);
        showDate.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        showSeconds.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        showAdd.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        showSettings.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        showSection.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        showPriority.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        showMetadata.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        maxItems.setOnValueChangedListener((picker, oldValue, newValue) -> refreshPreview());
    }

    private void watch(Spinner spinner) {
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                refreshPreview();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void refreshPreview() {
        if (preview == null || theme == null || palette == null || maxItems == null) return;
        boolean dark = theme.getSelectedItemPosition() == 2
                || (theme.getSelectedItemPosition() == 0
                && AppSettings.themeMode(this) == AppSettings.THEME_DARK);
        int alpha = opacity.getSelectedItemPosition() == 0 ? 0xFF
                : (opacity.getSelectedItemPosition() == 1 ? 0xD9 : 0xB3);
        int base = dark ? 0x00111418 : 0x00FFFFFF;
        int background = base | (alpha << 24);
        int text = dark ? 0xFFF2F5F4 : 0xFF173F3B;
        int accent = AppSettings.primaryColorForPalette(palette.getSelectedItemPosition());
        preview.configure(kind, background, text, accent,
                showHeader.isChecked(), showMetadata.isChecked(), maxItems.getValue());
        saveSettings();
    }

    private String inferKind(int id) {
        AppWidgetProviderInfo info = AppWidgetManager.getInstance(this)
                .getAppWidgetInfo(id);
        if (info == null || info.provider == null) return "clock";

        String className = info.provider.getClassName();
        if (className.endsWith("NoForgetWidgetProvider")) return "note";
        if (className.endsWith("MediaWidgetProvider")) return "media";
        return "clock";
    }

    private Switch addSwitch(
            LinearLayout root,
            String title,
            String description,
            boolean checked) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(AppSettings.layoutDirection(this));
        card.setPadding(dp(12), dp(6), dp(12), dp(9));
        card.setBackgroundResource(R.drawable.bg_card);

        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(14);
        sw.setTextColor(AppSettings.textPrimary(this));
        sw.setChecked(checked);
        sw.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(sw, new LinearLayout.LayoutParams(-1, dp(46)));

        TextView d = hint(description);
        d.setPadding(0, 0, 0, 0);
        card.addView(d);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(7);
        root.addView(card, lp);
        return sw;
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

    private TextView sectionTitle(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(AppSettings.textPrimary(this));
        v.setTextSize(16);
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setPadding(0, dp(18), 0, dp(7));
        return v;
    }

    private TextView fieldLabel(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(AppSettings.textSecondary(this));
        v.setTextSize(12);
        v.setPadding(0, dp(9), 0, dp(3));
        return v;
    }

    private TextView hint(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(AppSettings.textSecondary(this));
        v.setTextSize(12);
        v.setPadding(dp(2), dp(4), dp(2), dp(9));
        return v;
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
        return spinner;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.bottomMargin = dp(3);
        return lp;
    }

    private LinearLayout.LayoutParams buttonLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
        lp.topMargin = dp(7);
        return lp;
    }

    private Button softButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
