package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
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
import android.widget.Toast;

public final class WidgetSettingsActivity extends Activity {
    private int widgetId;
    private String kind;

    private Spinner theme;
    private Spinner palette;
    private Spinner opacity;
    private Spinner fontSize;
    private Spinner sortMode;
    private NumberPicker maxItems;
    private NumberPicker widthPicker;
    private NumberPicker heightPicker;

    private Switch showHeader;
    private Switch showDate;
    private Switch showAdd;
    private Switch showSettings;
    private Switch showSection;
    private Switch showPriority;
    private Switch showMetadata;

    private TextView actualSize;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        kind = getIntent().getStringExtra("widgetKind");

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
                    .putExtra("editExisting", true);
            startActivity(media);
            finish();
            return;
        }

        // Launcher configuration flows require RESULT_OK to keep the widget.
        setResult(RESULT_CANCELED);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(26));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        addTopBar(root);
        addAppearanceSection(root);
        addHeaderSection(root);
        addContentSection(root);
        addSizeSection(root);
        addActions(root);

        setContentView(scroll);
        updateActualSize();
    }

    private void addTopBar(LinearLayout root) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("clock".equals(kind)
                ? "تنظیمات ویجت ساعت"
                : "تنظیمات ویجت یادداشت");
        title.setTextSize(23);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
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
    }

    private void addAppearanceSection(LinearLayout root) {
        root.addView(sectionTitle("ظاهر"));

        root.addView(fieldLabel("تم"));
        theme = spinner(new String[]{
                "هماهنگ با تم برنامه",
                "روشن",
                "تیره"
        });
        theme.setSelection(WidgetPrefs.themeMode(this, widgetId));
        root.addView(theme, fieldLp());

        root.addView(fieldLabel("پالت رنگ"));
        palette = spinner(AppSettings.paletteNames());
        palette.setSelection(WidgetPrefs.palette(this, widgetId));
        root.addView(palette, fieldLp());

        root.addView(fieldLabel("شفافیت پس‌زمینه"));
        opacity = spinner(new String[]{
                "۱۰۰٪",
                "۸۵٪",
                "۷۰٪"
        });
        opacity.setSelection(WidgetPrefs.backgroundOpacityMode(this, widgetId));
        root.addView(opacity, fieldLp());

        root.addView(fieldLabel("اندازه متن"));
        fontSize = spinner(new String[]{
                "کوچک",
                "معمولی",
                "بزرگ"
        });
        fontSize.setSelection(WidgetPrefs.fontSizeMode(this, widgetId));
        root.addView(fontSize, fieldLp());
    }

    private void addHeaderSection(LinearLayout root) {
        root.addView(sectionTitle("هدر و کنترل‌ها"));

        showHeader = addSwitch(
                root,
                "نمایش هدر",
                "ساعت، تاریخ و دکمه‌های بالای ویجت",
                WidgetPrefs.showHeader(this, widgetId));

        showDate = addSwitch(
                root,
                "نمایش تاریخ",
                "تاریخ تقویم پیش‌فرض در هدر",
                WidgetPrefs.showDate(this, widgetId));

        showAdd = addSwitch(
                root,
                "نمایش دکمه +",
                "ساخت سریع هشدار یا یادداشت از خود ویجت",
                WidgetPrefs.showAddButton(this, widgetId));

        showSettings = addSwitch(
                root,
                "نمایش دکمه تنظیمات",
                "باز کردن تنظیمات همین نمونهٔ ویجت",
                WidgetPrefs.showSettingsButton(this, widgetId));

        showSection = addSwitch(
                root,
                "نمایش عنوان بخش",
                "عنوان «هشدار» یا «یادداشت» بالای فهرست",
                WidgetPrefs.showSectionLabel(this, widgetId));

        showHeader.setOnCheckedChangeListener((button, checked) -> {
            showDate.setEnabled(checked);
            showAdd.setEnabled(checked);
            showSettings.setEnabled(checked);
            showDate.setAlpha(checked ? 1f : 0.5f);
            showAdd.setAlpha(checked ? 1f : 0.5f);
            showSettings.setAlpha(checked ? 1f : 0.5f);
        });
        boolean headerEnabled = showHeader.isChecked();
        showDate.setEnabled(headerEnabled);
        showAdd.setEnabled(headerEnabled);
        showSettings.setEnabled(headerEnabled);
        showDate.setAlpha(headerEnabled ? 1f : 0.5f);
        showAdd.setAlpha(headerEnabled ? 1f : 0.5f);
        showSettings.setAlpha(headerEnabled ? 1f : 0.5f);
    }

    private void addContentSection(LinearLayout root) {
        root.addView(sectionTitle("محتوا"));

        showPriority = addSwitch(
                root,
                "نمایش اهمیت",
                "برچسب سطح اهمیت هر مورد",
                WidgetPrefs.showPriority(this, widgetId));

        showMetadata = addSwitch(
                root,
                "نمایش جزئیات",
                "تاریخ/زمان هشدار یا اطلاعات یادداشت",
                WidgetPrefs.showMetadata(this, widgetId));

        root.addView(fieldLabel("حداکثر تعداد آیتم"));
        LinearLayout countBox = pickerBox(
                "تعداد",
                WidgetPrefs.maxItems(this, widgetId),
                1,
                10);
        maxItems = (NumberPicker) countBox.getChildAt(1);
        root.addView(countBox, new LinearLayout.LayoutParams(-1, dp(122)));

        root.addView(fieldLabel("مرتب‌سازی"));
        sortMode = spinner("clock".equals(kind)
                ? new String[]{
                "هوشمند: نزدیک‌ترین هشدار اول",
                "زمان: زودترین تا دیرترین",
                "اهمیت: مهم‌ترین اول"
        }
                : new String[]{
                "هوشمند: نزدیک‌ترین موعد اول",
                "جدیدترین یادداشت اول",
                "اهمیت: مهم‌ترین اول"
        });
        sortMode.setSelection(WidgetPrefs.sortMode(this, widgetId));
        root.addView(sortMode, fieldLp());
    }

    private void addSizeSection(LinearLayout root) {
        root.addView(sectionTitle("اندازه و ریسایز"));

        actualSize = hint("");
        actualSize.setBackgroundResource(R.drawable.bg_card);
        actualSize.setPadding(dp(12), dp(10), dp(12), dp(10));
        root.addView(actualSize, new LinearLayout.LayoutParams(-1, -2));

        TextView explanation = hint(
                "اندازهٔ قاب ویجت را لانچر Android کنترل می‌کند. تنظیمات برنامه نمی‌تواند "
                        + "قاب یک ویجت نصب‌شده را به زور جابه‌جا یا بزرگ کند؛ اما بازهٔ ریسایز "
                        + "کاملاً فعال شده و ویجت پس از ریسایز لانچر فوراً خودش را با اندازهٔ واقعی تطبیق می‌دهد.");
        root.addView(explanation);

        root.addView(fieldLabel("اندازه هدف / چیدمان پیشنهادی"));

        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout widthBox = pickerBox(
                "عرض (خانه)",
                WidgetPrefs.widthCells(this, widgetId),
                2,
                6);
        widthPicker = (NumberPicker) widthBox.getChildAt(1);
        sizeRow.addView(widthBox, new LinearLayout.LayoutParams(0, dp(132), 1f));

        View spacer = new View(this);
        sizeRow.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));

        LinearLayout heightBox = pickerBox(
                "ارتفاع (خانه)",
                WidgetPrefs.heightCells(this, widgetId),
                1,
                6);
        heightPicker = (NumberPicker) heightBox.getChildAt(1);
        sizeRow.addView(heightBox, new LinearLayout.LayoutParams(0, dp(132), 1f));

        root.addView(sizeRow);

        Button sync = softButton("همگام‌سازی عددها با اندازه فعلی لانچر");
        sync.setOnClickListener(v -> syncTargetFromActual());
        root.addView(sync, buttonLp());

        Button resize = softButton("رفتن به صفحه اصلی برای ریسایز واقعی");
        resize.setOnClickListener(v -> {
            saveSettings(false);
            Toast.makeText(
                    this,
                    "روی ویجت لمس طولانی کنید و دسته‌های ریسایز را بکشید؛ تغییر اندازه همان لحظه اعمال می‌شود.",
                    Toast.LENGTH_LONG).show();

            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        });
        root.addView(resize, buttonLp());
    }

    private void addActions(LinearLayout root) {
        Button save = new Button(this);
        save.setText("ذخیره و اعمال");
        save.setTextColor(0xFFFFFFFF);
        save.setAllCaps(false);
        save.setBackgroundColor(AppSettings.secondaryColor(this));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(58));
        saveLp.topMargin = dp(20);
        root.addView(save, saveLp);
        save.setOnClickListener(v -> saveSettings(true));

        Button reset = softButton("بازگردانی تنظیمات این ویجت");
        LinearLayout.LayoutParams resetLp = buttonLp();
        resetLp.topMargin = dp(8);
        root.addView(reset, resetLp);
        reset.setOnClickListener(v -> {
            WidgetPrefs.clear(this, widgetId);
            updateWidget();
            recreate();
        });
    }

    private void saveSettings(boolean finishAfter) {
        WidgetPrefs.setThemeMode(this, widgetId, theme.getSelectedItemPosition());
        WidgetPrefs.setPalette(this, widgetId, palette.getSelectedItemPosition());
        WidgetPrefs.setBackgroundOpacityMode(
                this, widgetId, opacity.getSelectedItemPosition());
        WidgetPrefs.setFontSizeMode(
                this, widgetId, fontSize.getSelectedItemPosition());

        WidgetPrefs.setShowHeader(this, widgetId, showHeader.isChecked());
        WidgetPrefs.setShowDate(this, widgetId, showDate.isChecked());
        WidgetPrefs.setShowAddButton(this, widgetId, showAdd.isChecked());
        WidgetPrefs.setShowSettingsButton(this, widgetId, showSettings.isChecked());
        WidgetPrefs.setShowSectionLabel(this, widgetId, showSection.isChecked());

        WidgetPrefs.setShowPriority(this, widgetId, showPriority.isChecked());
        WidgetPrefs.setShowMetadata(this, widgetId, showMetadata.isChecked());
        WidgetPrefs.setMaxItems(this, widgetId, maxItems.getValue());
        WidgetPrefs.setSortMode(this, widgetId, sortMode.getSelectedItemPosition());

        // Stored as the user's preferred cell profile. The actual host frame remains launcher-owned.
        WidgetPrefs.setSizeCells(
                this,
                widgetId,
                widthPicker.getValue(),
                heightPicker.getValue());

        updateWidget();

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);

        if (finishAfter) finish();
    }

    private void updateWidget() {
        if ("note".equals(kind)) {
            NoForgetWidgetProvider.updateAll(this);
        } else {
            ClockWidgetProvider.updateAll(this);
        }
    }

    private void updateActualSize() {
        Bundle options = AppWidgetManager.getInstance(this)
                .getAppWidgetOptions(widgetId);

        int minWidth = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int minHeight = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);
        int maxWidth = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth);
        int maxHeight = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, minHeight);

        if (minWidth <= 0 || minHeight <= 0) {
            actualSize.setText(
                    "اندازه فعلی: لانچر هنوز اندازه دقیق را گزارش نکرده است.");
            return;
        }

        int approxWidthCells = dpToCells(minWidth);
        int approxHeightCells = dpToCells(minHeight);

        String range = (maxWidth != minWidth || maxHeight != minHeight)
                ? " تا " + maxWidth + "×" + maxHeight + "dp"
                : "";

        actualSize.setText(
                "اندازه واقعی گزارش‌شده: "
                        + minWidth + "×" + minHeight + "dp"
                        + range
                        + "\nتقریب شبکه: "
                        + approxWidthCells + " × " + approxHeightCells
                        + " خانه");
    }

    private void syncTargetFromActual() {
        Bundle options = AppWidgetManager.getInstance(this)
                .getAppWidgetOptions(widgetId);
        int width = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0);
        int height = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0);

        if (width <= 0 || height <= 0) {
            Toast.makeText(
                    this,
                    "لانچر اندازه دقیق را گزارش نکرده است.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        widthPicker.setValue(Math.max(2, dpToCells(width)));
        heightPicker.setValue(dpToCells(height));
        WidgetPrefs.setSizeCells(
                this,
                widgetId,
                widthPicker.getValue(),
                heightPicker.getValue());
        Toast.makeText(
                this,
                "اندازه هدف با قاب فعلی همگام شد.",
                Toast.LENGTH_SHORT).show();
    }

    private int dpToCells(int dp) {
        return Math.max(1, Math.min(6, Math.round((dp + 30f) / 70f)));
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
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
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
