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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
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
    private boolean savedSuccessfully;
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
    private WidgetPreviewView preview;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        editExisting = getIntent().getBooleanExtra(
                "editExisting",
                false)
                || MediaWidgetPrefs.hasSavedConfig(this, widgetId)
                || WidgetPrefs.hasSavedConfig(this, widgetId);

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        setResult(RESULT_CANCELED);
        items.addAll(MediaWidgetPrefs.load(this, widgetId));

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setBackgroundColor(AppSettings.background(this));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(20));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        addTopBar(root);
        root.addView(sectionTitle("پیش‌نمایش زنده"));
        preview = new WidgetPreviewView(this);
        root.addView(preview, new LinearLayout.LayoutParams(-1, dp(150)));
        addFilesSection(root);
        addAppearanceSection(root);
        addDisplaySection(root);
        addResizeSection(root);
        addActions(root);
        bindPreviewUpdates();

        page.addView(
                scroll,
                new LinearLayout.LayoutParams(
                        -1,
                        0,
                        1f));

        LinearLayout stickyBar = new LinearLayout(this);
        stickyBar.setPadding(
                dp(18),
                dp(8),
                dp(18),
                dp(12));
        stickyBar.setBackgroundColor(AppSettings.background(this));

        Button save = primaryButton(
                editExisting
                        ? "ذخیره تغییرات"
                        : "ساخت و ذخیره ویجت");
        save.setBackgroundColor(
                AppSettings.secondaryColor(this));
        save.setOnClickListener(v -> save(true));
        stickyBar.addView(
                save,
                new LinearLayout.LayoutParams(
                        -1,
                        dp(56)));

        page.addView(
                stickyBar,
                new LinearLayout.LayoutParams(
                        -1,
                        -2));

        setContentView(page);
        renderItems();
        refreshPreview();
    }

    private void addTopBar(LinearLayout root) {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText(editExisting
                ? "تنظیمات یادآوری فایل‌ها"
                : "ساخت یادآوری فایل‌ها");
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
                "نام ویجت و دکمه تنظیمات",
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
                "اگر خاموش باشد، پیش‌نمایش فایل بزرگ و واضح نمایش داده می‌شود.",
                WidgetPrefs.mediaShowFileName(this, widgetId));

        showMetadata = addSwitch(
                root,
                "نمایش نوع و جزئیات فایل",
                "نوع فایل و برای فایل متنی، خلاصه‌ای از محتوا",
                WidgetPrefs.showMetadata(this, widgetId));

        showName.setOnCheckedChangeListener((button, checked) -> {
            if (!checked) {
                showPreview.setChecked(true);
            }
            showPreview.setEnabled(checked);
            showPreview.setAlpha(checked ? 1f : 0.55f);
            refreshPreview();
        });
        if (!showName.isChecked()) {
            showPreview.setChecked(true);
            showPreview.setEnabled(false);
            showPreview.setAlpha(0.55f);
        }

        showHeader.setOnCheckedChangeListener((button, checked) -> {
            showSettings.setEnabled(checked);
            showSettings.setAlpha(checked ? 1f : 0.5f);
            refreshPreview();
        });
        showSettings.setEnabled(showHeader.isChecked());
        showSettings.setAlpha(showHeader.isChecked() ? 1f : 0.5f);
    }

    private void addResizeSection(LinearLayout root) {
        root.addView(hint(
                "برای تغییر اندازه ویجت انگشتتان را روی ویجت نگهدارید و کمی جابه‌جا کنید و رها کنید تا تغییر سایز فعال شود"));
    }

    private void addActions(LinearLayout root) {
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

    private void bindPreviewUpdates() {
        watch(themeSpinner);
        watch(paletteSpinner);
        watch(opacitySpinner);
        watch(fontSpinner);
        showSettings.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        showPreview.setOnCheckedChangeListener((button, checked) -> refreshPreview());
        showMetadata.setOnCheckedChangeListener((button, checked) -> refreshPreview());
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
        if (preview == null || themeSpinner == null || paletteSpinner == null) return;
        boolean dark = themeSpinner.getSelectedItemPosition() == 2
                || (themeSpinner.getSelectedItemPosition() == 0
                && AppSettings.themeMode(this) == AppSettings.THEME_DARK);
        int alpha = opacitySpinner.getSelectedItemPosition() == 0 ? 0xFF
                : (opacitySpinner.getSelectedItemPosition() == 1 ? 0xD9 : 0xB3);
        int background = (dark ? 0x00111418 : 0x00FFFFFF) | (alpha << 24);
        int text = dark ? 0xFFF2F5F4 : 0xFF173F3B;
        int accent = AppSettings.primaryColorForPalette(paletteSpinner.getSelectedItemPosition());
        preview.configure("media", background, text, accent, showHeader.isChecked(),
                showMetadata.isChecked(), Math.max(1, items.size()));
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
                & Intent.FLAG_GRANT_READ_URI_PERMISSION;

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
        refreshPreview();

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
        java.util.List<MediaWidgetPrefs.Item> previousItems =
                MediaWidgetPrefs.load(this, widgetId);

        MediaWidgetPrefs.save(
                this,
                widgetId,
                items);
        savedSuccessfully = true;

        MediaUriPermissionUtils.releaseUnused(
                this,
                previousItems);

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

        if (!editExisting) {
            WidgetPrefs.setSizeCells(
                    this,
                    widgetId,
                    6,
                    3);
        }

        MediaWidgetProvider.update(
                this,
                widgetId);
        MediaPreviewScheduler.schedule(
                this,
                widgetId);

        Intent result = new Intent();
        result.putExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                widgetId);
        setResult(RESULT_OK, result);

        if (finishAfter) {
            finishToHome();
        }
    }

    private void finishToHome() {
        Intent home = new Intent(Intent.ACTION_MAIN);
        home.addCategory(Intent.CATEGORY_HOME);
        home.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startActivity(home);
        } catch (Exception ignored) {
        }
        finish();
    }

    @Override protected void onDestroy() {
        if (!savedSuccessfully) {
            MediaUriPermissionUtils.releaseUnused(
                    this,
                    new ArrayList<>(items));
        }
        super.onDestroy();
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
