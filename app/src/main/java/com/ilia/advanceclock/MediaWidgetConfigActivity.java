package com.ilia.advanceclock;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
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

import java.util.ArrayList;
import java.util.Locale;

public final class MediaWidgetConfigActivity extends Activity {
    private static final int REQ_FILES = 4101;

    private int widgetId;
    private boolean editExisting;
    private final ArrayList<MediaWidgetPrefs.Item> items =
            new ArrayList<>();

    private LinearLayout fileList;

    private Spinner themeSpinner;
    private Spinner paletteSpinner;
    private Spinner opacitySpinner;
    private Spinner fontSpinner;

    private Switch showHeader;
    private Switch showSettings;
    private Switch showPreview;
    private Switch showName;
    private Switch showMetadata;

    private NumberPicker maxItems;
    private NumberPicker widthPicker;
    private NumberPicker heightPicker;

    private TextView actualSize;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        editExisting = getIntent().getBooleanExtra(
                "editExisting",
                false);

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setResult(RESULT_CANCELED);
        items.addAll(MediaWidgetPrefs.load(this, widgetId));

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
        addFilesSection(root);
        addAppearanceSection(root);
        addDisplaySection(root);
        addSizeSection(root);
        addActions(root);

        setContentView(scroll);
        renderItems();
        updateActualSize();
    }

    private void addTopBar(LinearLayout root) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText(editExisting
                ? "تنظیمات ویجت فایل و رسانه"
                : "ساخت ویجت فایل و رسانه");
        title.setTextSize(23);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(
                0,
                dp(56),
                1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(
                AppSettings.textPrimary(this),
                PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(
                dp(48),
                dp(48)));

        root.addView(top);
    }

    private void addFilesSection(LinearLayout root) {
        root.addView(sectionTitle("فایل‌ها"));

        root.addView(hint(
                "تا " + MediaWidgetPrefs.MAX_ITEMS
                        + " فایل ترکیبی قابل انتخاب است. عکس، صوت، ویدیو، متن، PDF و "
                        + "سایر فایل‌ها به‌صورت خودکار تشخیص داده می‌شوند. ترتیب همین فهرست، "
                        + "ترتیب نمایش در ویجت است."));

        Button addFiles = primaryButton(
                "＋ افزودن عکس، صوت یا فایل");
        addFiles.setOnClickListener(v -> pickFiles());
        root.addView(addFiles, new LinearLayout.LayoutParams(
                -1,
                dp(56)));

        Button clear = softButton("پاک کردن همه فایل‌ها");
        clear.setOnClickListener(v -> {
            items.clear();
            renderItems();
        });
        root.addView(clear, buttonLp());

        fileList = new LinearLayout(this);
        fileList.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams listLp =
                new LinearLayout.LayoutParams(-1, -2);
        listLp.topMargin = dp(10);
        root.addView(fileList, listLp);
    }

    private void addAppearanceSection(LinearLayout root) {
        root.addView(sectionTitle("ظاهر"));

        root.addView(fieldLabel("تم"));
        themeSpinner = spinner(new String[]{
                "هماهنگ با تم برنامه",
                "روشن",
                "تیره"
        });
        themeSpinner.setSelection(
                WidgetPrefs.themeMode(this, widgetId));
        root.addView(themeSpinner, fieldLp());

        root.addView(fieldLabel("پالت رنگ"));
        paletteSpinner = spinner(AppSettings.paletteNames());
        paletteSpinner.setSelection(
                WidgetPrefs.palette(this, widgetId));
        root.addView(paletteSpinner, fieldLp());

        root.addView(fieldLabel("شفافیت پس‌زمینه"));
        opacitySpinner = spinner(new String[]{
                "۱۰۰٪",
                "۸۵٪",
                "۷۰٪"
        });
        opacitySpinner.setSelection(
                WidgetPrefs.backgroundOpacityMode(this, widgetId));
        root.addView(opacitySpinner, fieldLp());

        root.addView(fieldLabel("اندازه متن"));
        fontSpinner = spinner(new String[]{
                "کوچک",
                "معمولی",
                "بزرگ"
        });
        fontSpinner.setSelection(
                WidgetPrefs.fontSizeMode(this, widgetId));
        root.addView(fontSpinner, fieldLp());
    }

    private void addDisplaySection(LinearLayout root) {
        root.addView(sectionTitle("نمایش محتوا"));

        showHeader = addSwitch(
                root,
                "نمایش هدر",
                "عنوان ویجت، تعداد فایل‌ها و دکمه تنظیمات",
                WidgetPrefs.showHeader(this, widgetId));

        showSettings = addSwitch(
                root,
                "نمایش دکمه تنظیمات",
                "دسترسی مستقیم به تنظیمات همین ویجت",
                WidgetPrefs.showSettingsButton(this, widgetId));

        showPreview = addSwitch(
                root,
                "نمایش پیش‌نمایش",
                "thumbnail تصویر، فریم ویدیو یا کاور فایل صوتی",
                WidgetPrefs.mediaShowPreview(this, widgetId));

        showName = addSwitch(
                root,
                "نمایش نام فایل",
                "نام هر فایل در کنار پیش‌نمایش",
                WidgetPrefs.mediaShowFileName(this, widgetId));

        showMetadata = addSwitch(
                root,
                "نمایش نوع و جزئیات فایل",
                "نوع فایل و برای فایل متنی، خلاصه‌ای از محتوا",
                WidgetPrefs.showMetadata(this, widgetId));

        showHeader.setOnCheckedChangeListener((button, checked) -> {
            showSettings.setEnabled(checked);
            showSettings.setAlpha(checked ? 1f : 0.5f);
        });
        showSettings.setEnabled(showHeader.isChecked());
        showSettings.setAlpha(showHeader.isChecked() ? 1f : 0.5f);

        root.addView(fieldLabel("حداکثر فایل قابل نمایش"));
        LinearLayout countBox = pickerBox(
                "تعداد",
                WidgetPrefs.maxItems(this, widgetId),
                1,
                10);
        maxItems = (NumberPicker) countBox.getChildAt(1);
        root.addView(countBox, new LinearLayout.LayoutParams(
                -1,
                dp(122)));
    }

    private void addSizeSection(LinearLayout root) {
        root.addView(sectionTitle("اندازه و ریسایز"));

        actualSize = hint("");
        actualSize.setBackgroundResource(R.drawable.bg_card);
        actualSize.setPadding(
                dp(12),
                dp(10),
                dp(12),
                dp(10));
        root.addView(actualSize, new LinearLayout.LayoutParams(
                -1,
                -2));

        root.addView(hint(
                "قاب واقعی ویجت توسط لانچر Android کنترل می‌شود. پس از ریسایز روی صفحه اصلی، "
                        + "ویجت فایل و رسانه فوراً تعداد آیتم و چیدمان خودش را با اندازه واقعی تطبیق می‌دهد."));

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
        sizeRow.addView(
                widthBox,
                new LinearLayout.LayoutParams(
                        0,
                        dp(132),
                        1f));

        View spacer = new View(this);
        sizeRow.addView(
                spacer,
                new LinearLayout.LayoutParams(
                        dp(10),
                        1));

        LinearLayout heightBox = pickerBox(
                "ارتفاع (خانه)",
                WidgetPrefs.heightCells(this, widgetId),
                1,
                6);
        heightPicker = (NumberPicker) heightBox.getChildAt(1);
        sizeRow.addView(
                heightBox,
                new LinearLayout.LayoutParams(
                        0,
                        dp(132),
                        1f));

        root.addView(sizeRow);

        Button sync = softButton(
                "همگام‌سازی عددها با اندازه فعلی لانچر");
        sync.setOnClickListener(v -> syncTargetFromActual());
        root.addView(sync, buttonLp());

        Button resize = softButton(
                "رفتن به صفحه اصلی برای ریسایز واقعی");
        resize.setOnClickListener(v -> {
            save(false);
            Toast.makeText(
                    this,
                    "روی ویجت لمس طولانی کنید و دسته‌های ریسایز را بکشید.",
                    Toast.LENGTH_LONG).show();

            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(home);
        });
        root.addView(resize, buttonLp());
    }

    private void addActions(LinearLayout root) {
        Button save = primaryButton(
                editExisting
                        ? "ذخیره و اعمال"
                        : "ساخت و ذخیره ویجت");
        save.setBackgroundColor(
                AppSettings.secondaryColor(this));
        LinearLayout.LayoutParams saveLp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(58));
        saveLp.topMargin = dp(20);
        root.addView(save, saveLp);
        save.setOnClickListener(v -> save(true));

        Button reset = softButton(
                "بازگردانی تنظیمات ظاهری این ویجت");
        LinearLayout.LayoutParams resetLp = buttonLp();
        resetLp.topMargin = dp(8);
        root.addView(reset, resetLp);
        reset.setOnClickListener(v -> {
            WidgetPrefs.clear(this, widgetId);
            MediaWidgetProvider.update(this, widgetId);
            recreate();
        });
    }

    private void pickFiles() {
        if (items.size() >= MediaWidgetPrefs.MAX_ITEMS) {
            Toast.makeText(
                    this,
                    "حداکثر "
                            + MediaWidgetPrefs.MAX_ITEMS
                            + " فایل قابل انتخاب است.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQ_FILES);
    }

    @Override protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data);

        if (requestCode != REQ_FILES
                || resultCode != RESULT_OK
                || data == null) {
            return;
        }

        int takeFlags = data.getFlags()
                & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0;
                 i < clip.getItemCount()
                         && items.size()
                         < MediaWidgetPrefs.MAX_ITEMS;
                 i++) {
                addUri(
                        clip.getItemAt(i).getUri(),
                        takeFlags);
            }
        } else if (data.getData() != null) {
            addUri(data.getData(), takeFlags);
        }

        renderItems();
    }

    private void addUri(Uri uri, int takeFlags) {
        if (uri == null
                || containsUri(uri.toString())) {
            return;
        }

        try {
            if (takeFlags != 0) {
                getContentResolver()
                        .takePersistableUriPermission(
                                uri,
                                takeFlags);
            }
        } catch (Exception ignored) {
        }

        String mime =
                getContentResolver().getType(uri);
        if (mime == null || mime.trim().isEmpty()) {
            mime = "application/octet-stream";
        }

        items.add(new MediaWidgetPrefs.Item(
                uri.toString(),
                displayName(uri),
                mime));
    }

    private boolean containsUri(String uri) {
        for (MediaWidgetPrefs.Item item : items) {
            if (item.uri.equals(uri)) return true;
        }
        return false;
    }

    private String displayName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null)) {
            if (cursor != null
                    && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null
                            && !value.trim().isEmpty()) {
                        return value;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        String last = uri.getLastPathSegment();
        return last == null || last.trim().isEmpty()
                ? "فایل"
                : last;
    }

    private void renderItems() {
        if (fileList == null) return;

        fileList.removeAllViews();

        if (items.isEmpty()) {
            TextView empty = hint(
                    "هنوز فایلی انتخاب نشده است.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(
                    dp(12),
                    dp(22),
                    dp(12),
                    dp(22));
            fileList.addView(empty);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            MediaWidgetPrefs.Item item = items.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.setPadding(
                    dp(10),
                    dp(8),
                    dp(10),
                    dp(8));
            row.setBackgroundResource(R.drawable.bg_card);

            TextView name = new TextView(this);
            name.setText((i + 1) + ". " + item.name);
            name.setTextColor(
                    AppSettings.textPrimary(this));
            name.setTextSize(14);
            name.setMaxLines(1);
            name.setEllipsize(
                    android.text.TextUtils.TruncateAt.END);
            name.setTypeface(
                    null,
                    android.graphics.Typeface.BOLD);
            row.addView(name);

            TextView type = new TextView(this);
            type.setText(
                    typeLabel(item.mime)
                            + "  •  "
                            + item.mime);
            type.setTextColor(
                    AppSettings.textSecondary(this));
            type.setTextSize(11);
            type.setMaxLines(1);
            type.setEllipsize(
                    android.text.TextUtils.TruncateAt.END);
            row.addView(type);

            LinearLayout actions =
                    new LinearLayout(this);
            actions.setOrientation(
                    LinearLayout.HORIZONTAL);
            actions.setLayoutDirection(
                    View.LAYOUT_DIRECTION_RTL);
            actions.setGravity(Gravity.START);

            Button up = miniButton("↑ بالا");
            up.setEnabled(index > 0);
            up.setAlpha(index > 0 ? 1f : 0.4f);
            up.setOnClickListener(v -> {
                if (index > 0) {
                    MediaWidgetPrefs.Item current =
                            items.remove(index);
                    items.add(index - 1, current);
                    renderItems();
                }
            });

            Button down = miniButton("↓ پایین");
            down.setEnabled(index < items.size() - 1);
            down.setAlpha(
                    index < items.size() - 1
                            ? 1f
                            : 0.4f);
            down.setOnClickListener(v -> {
                if (index < items.size() - 1) {
                    MediaWidgetPrefs.Item current =
                            items.remove(index);
                    items.add(index + 1, current);
                    renderItems();
                }
            });

            Button remove = miniButton("حذف");
            remove.setTextColor(0xFFD24A43);
            remove.setOnClickListener(v -> {
                if (index >= 0
                        && index < items.size()) {
                    items.remove(index);
                    renderItems();
                }
            });

            actions.addView(up, miniLp());
            actions.addView(down, miniLp());
            actions.addView(remove, miniLp());

            LinearLayout.LayoutParams actionLp =
                    new LinearLayout.LayoutParams(
                            -1,
                            -2);
            actionLp.topMargin = dp(7);
            row.addView(actions, actionLp);

            LinearLayout.LayoutParams lp =
                    new LinearLayout.LayoutParams(
                            -1,
                            -2);
            lp.bottomMargin = dp(7);
            fileList.addView(row, lp);
        }

        TextView count = hint(
                items.size()
                        + " از "
                        + MediaWidgetPrefs.MAX_ITEMS
                        + " مورد انتخاب شده");
        count.setGravity(Gravity.CENTER);
        fileList.addView(count);
    }

    private void save(boolean finishAfter) {
        MediaWidgetPrefs.save(
                this,
                widgetId,
                items);

        WidgetPrefs.setThemeMode(
                this,
                widgetId,
                themeSpinner.getSelectedItemPosition());
        WidgetPrefs.setPalette(
                this,
                widgetId,
                paletteSpinner.getSelectedItemPosition());
        WidgetPrefs.setBackgroundOpacityMode(
                this,
                widgetId,
                opacitySpinner.getSelectedItemPosition());
        WidgetPrefs.setFontSizeMode(
                this,
                widgetId,
                fontSpinner.getSelectedItemPosition());

        WidgetPrefs.setShowHeader(
                this,
                widgetId,
                showHeader.isChecked());
        WidgetPrefs.setShowSettingsButton(
                this,
                widgetId,
                showSettings.isChecked());
        WidgetPrefs.setMediaShowPreview(
                this,
                widgetId,
                showPreview.isChecked());
        WidgetPrefs.setMediaShowFileName(
                this,
                widgetId,
                showName.isChecked());
        WidgetPrefs.setShowMetadata(
                this,
                widgetId,
                showMetadata.isChecked());
        WidgetPrefs.setMaxItems(
                this,
                widgetId,
                maxItems.getValue());

        WidgetPrefs.setSizeCells(
                this,
                widgetId,
                widthPicker.getValue(),
                heightPicker.getValue());

        MediaWidgetProvider.update(
                this,
                widgetId);

        Intent result = new Intent();
        result.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                widgetId);
        setResult(RESULT_OK, result);

        if (finishAfter) finish();
    }

    private void updateActualSize() {
        Bundle options =
                AppWidgetManager.getInstance(this)
                        .getAppWidgetOptions(widgetId);

        int minWidth = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                0);
        int minHeight = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                0);
        int maxWidth = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,
                minWidth);
        int maxHeight = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,
                minHeight);

        if (minWidth <= 0 || minHeight <= 0) {
            actualSize.setText(
                    "اندازه فعلی: لانچر هنوز اندازه دقیق را گزارش نکرده است.");
            return;
        }

        String range =
                (maxWidth != minWidth
                        || maxHeight != minHeight)
                        ? " تا "
                        + maxWidth
                        + "×"
                        + maxHeight
                        + "dp"
                        : "";

        actualSize.setText(
                "اندازه واقعی گزارش‌شده: "
                        + minWidth
                        + "×"
                        + minHeight
                        + "dp"
                        + range
                        + "\nتقریب شبکه: "
                        + dpToCells(minWidth)
                        + " × "
                        + dpToCells(minHeight)
                        + " خانه");
    }

    private void syncTargetFromActual() {
        Bundle options =
                AppWidgetManager.getInstance(this)
                        .getAppWidgetOptions(widgetId);

        int width = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,
                0);
        int height = options.getInt(
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,
                0);

        if (width <= 0 || height <= 0) {
            Toast.makeText(
                    this,
                    "لانچر اندازه دقیق را گزارش نکرده است.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        widthPicker.setValue(
                Math.max(2, dpToCells(width)));
        heightPicker.setValue(
                dpToCells(height));

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
        return Math.max(
                1,
                Math.min(
                        6,
                        Math.round((dp + 30f) / 70f)));
    }

    private Switch addSwitch(
            LinearLayout root,
            String title,
            String description,
            boolean checked) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(
                View.LAYOUT_DIRECTION_RTL);
        card.setPadding(
                dp(12),
                dp(6),
                dp(12),
                dp(9));
        card.setBackgroundResource(R.drawable.bg_card);

        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(14);
        sw.setTextColor(
                AppSettings.textPrimary(this));
        sw.setChecked(checked);
        sw.setGravity(Gravity.CENTER_VERTICAL);
        card.addView(
                sw,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(46)));

        TextView d = hint(description);
        d.setPadding(0, 0, 0, 0);
        card.addView(d);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        -1,
                        -2);
        lp.bottomMargin = dp(7);
        root.addView(card, lp);

        return sw;
    }

    private LinearLayout pickerBox(
            String title,
            int value,
            int min,
            int max) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundResource(R.drawable.bg_field);
        box.setPadding(
                dp(8),
                dp(6),
                dp(8),
                dp(6));

        TextView label = new TextView(this);
        label.setText(title);
        label.setTextColor(
                AppSettings.textSecondary(this));
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        box.addView(
                label,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(28)));

        NumberPicker picker = new NumberPicker(this);
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(
                Math.max(
                        min,
                        Math.min(max, value)));
        picker.setWrapSelectorWheel(false);
        box.addView(
                picker,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f));

        return box;
    }

    private TextView sectionTitle(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(
                AppSettings.textPrimary(this));
        v.setTextSize(16);
        v.setTypeface(
                null,
                android.graphics.Typeface.BOLD);
        v.setPadding(
                0,
                dp(18),
                0,
                dp(7));
        return v;
    }

    private TextView fieldLabel(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(
                AppSettings.textSecondary(this));
        v.setTextSize(12);
        v.setPadding(
                0,
                dp(9),
                0,
                dp(3));
        return v;
    }

    private TextView hint(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(
                AppSettings.textSecondary(this));
        v.setTextSize(12);
        v.setPadding(
                dp(2),
                dp(4),
                dp(2),
                dp(9));
        return v;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        values);
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setBackgroundColor(
                AppSettings.primaryColor(this));
        return button;
    }

    private Button softButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(
                AppSettings.primaryColor(this));
        button.setBackgroundResource(
                R.drawable.bg_soft_button);
        return button;
    }

    private Button miniButton(String text) {
        Button button = softButton(text);
        button.setTextSize(11);
        button.setMinWidth(0);
        button.setPadding(
                dp(8),
                0,
                dp(8),
                0);
        return button;
    }

    private LinearLayout.LayoutParams miniLp() {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        -2,
                        dp(40));
        lp.setMargins(
                0,
                0,
                dp(5),
                0);
        return lp;
    }

    private LinearLayout.LayoutParams fieldLp() {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(52));
        lp.bottomMargin = dp(3);
        return lp;
    }

    private LinearLayout.LayoutParams buttonLp() {
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(
                        -1,
                        dp(50));
        lp.topMargin = dp(7);
        return lp;
    }

    private String typeLabel(String mime) {
        String value = mime == null
                ? ""
                : mime.toLowerCase(Locale.ROOT);

        if (value.startsWith("image/")) return "تصویر";
        if (value.startsWith("audio/")) return "صوت";
        if (value.startsWith("video/")) return "ویدیو";
        if (value.startsWith("text/")) return "متن";
        if (value.contains("pdf")) return "PDF";
        if (value.contains("zip")
                || value.contains("rar")
                || value.contains("7z")) {
            return "فشرده";
        }
        if (value.contains("word")
                || value.contains("document")) {
            return "سند";
        }
        if (value.contains("sheet")
                || value.contains("excel")) {
            return "صفحه گسترده";
        }

        return "فایل";
    }

    private int dp(int value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density);
    }
}
