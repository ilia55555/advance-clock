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
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MediaWidgetConfigActivity extends Activity {
    private static final int REQ_FILES = 4101;

    private int widgetId;
    private boolean editExisting;
    private boolean returnToCenter;
    private final ArrayList<MediaWidgetPrefs.Item> items = new ArrayList<>();

    private LinearLayout fileList;
    private Spinner themeSpinner;
    private Spinner paletteSpinner;
    private NumberPicker widthPicker;
    private NumberPicker heightPicker;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        widgetId = getIntent().getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        editExisting = getIntent().getBooleanExtra("editExisting", false);
        returnToCenter = getIntent().getBooleanExtra("returnToCenter", false);

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish();
            return;
        }

        if (!editExisting) {
            setResult(RESULT_CANCELED);
        }

        items.addAll(MediaWidgetPrefs.load(this, widgetId));

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
        title.setText(editExisting ? "ویرایش ویجت فایل و رسانه" : "ساخت ویجت فایل و رسانه");
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

        TextView intro = hint(
                "می‌توانید تا " + MediaWidgetPrefs.MAX_ITEMS
                        + " فایل ترکیبی انتخاب کنید. تصویر، ویدیو، صوت، متن، PDF و سایر فایل‌ها "
                        + "به‌صورت خودکار تشخیص داده می‌شوند.");
        root.addView(intro);

        Button addFiles = new Button(this);
        addFiles.setText("＋ افزودن عکس، صوت یا فایل");
        addFiles.setAllCaps(false);
        addFiles.setTextColor(0xFFFFFFFF);
        addFiles.setBackgroundColor(AppSettings.primaryColor(this));
        addFiles.setOnClickListener(v -> pickFiles());
        root.addView(addFiles, new LinearLayout.LayoutParams(-1, dp(56)));

        Button clear = new Button(this);
        clear.setText("پاک کردن همه فایل‌ها");
        clear.setAllCaps(false);
        clear.setTextColor(AppSettings.primaryColor(this));
        clear.setBackgroundResource(R.drawable.bg_soft_button);
        LinearLayout.LayoutParams clearLp = new LinearLayout.LayoutParams(-1, dp(48));
        clearLp.topMargin = dp(8);
        root.addView(clear, clearLp);
        clear.setOnClickListener(v -> {
            items.clear();
            renderItems();
        });

        TextView filesTitle = sectionTitle("محتوای ویجت");
        root.addView(filesTitle);

        fileList = new LinearLayout(this);
        fileList.setOrientation(LinearLayout.VERTICAL);
        root.addView(fileList, new LinearLayout.LayoutParams(-1, -2));

        root.addView(sectionTitle("ظاهر"));

        themeSpinner = spinner(new String[]{
                "هماهنگ با تم اپ",
                "روشن",
                "تیره"
        });
        themeSpinner.setSelection(WidgetPrefs.themeMode(this, widgetId));
        root.addView(fieldLabel("تم ویجت"));
        root.addView(themeSpinner, new LinearLayout.LayoutParams(-1, dp(52)));

        paletteSpinner = spinner(AppSettings.paletteNames());
        paletteSpinner.setSelection(WidgetPrefs.palette(this, widgetId));
        root.addView(fieldLabel("رنگ ویجت"));
        root.addView(paletteSpinner, new LinearLayout.LayoutParams(-1, dp(52)));

        root.addView(sectionTitle("اندازه پیشنهادی"));

        LinearLayout sizeRow = new LinearLayout(this);
        sizeRow.setOrientation(LinearLayout.HORIZONTAL);
        sizeRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout widthBox = pickerBox(
                "عرض",
                WidgetPrefs.widthCells(this, widgetId),
                2,
                6);
        widthPicker = (NumberPicker) widthBox.getChildAt(1);
        sizeRow.addView(widthBox, new LinearLayout.LayoutParams(0, dp(132), 1f));

        View spacer = new View(this);
        sizeRow.addView(spacer, new LinearLayout.LayoutParams(dp(10), 1));

        LinearLayout heightBox = pickerBox(
                "ارتفاع",
                WidgetPrefs.heightCells(this, widgetId),
                1,
                6);
        heightPicker = (NumberPicker) heightBox.getChildAt(1);
        sizeRow.addView(heightBox, new LinearLayout.LayoutParams(0, dp(132), 1f));
        root.addView(sizeRow);

        TextView sizeHint = hint(
                "لانچر Android اندازه نهایی قاب را کنترل می‌کند؛ این مقدار برای چیدمان داخلی ویجت هم ذخیره می‌شود.");
        root.addView(sizeHint);

        Button save = new Button(this);
        save.setText(editExisting ? "ذخیره تغییرات" : "ساخت ویجت");
        save.setAllCaps(false);
        save.setTextColor(0xFFFFFFFF);
        save.setBackgroundColor(AppSettings.secondaryColor(this));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(58));
        saveLp.topMargin = dp(18);
        root.addView(save, saveLp);
        save.setOnClickListener(v -> save());

        setContentView(scroll);
        renderItems();
    }

    private void pickFiles() {
        if (items.size() >= MediaWidgetPrefs.MAX_ITEMS) {
            Toast.makeText(
                    this,
                    "حداکثر " + MediaWidgetPrefs.MAX_ITEMS + " فایل قابل انتخاب است.",
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
        super.onActivityResult(requestCode, resultCode, data);
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
                         && items.size() < MediaWidgetPrefs.MAX_ITEMS;
                 i++) {
                addUri(clip.getItemAt(i).getUri(), takeFlags);
            }
        } else if (data.getData() != null) {
            addUri(data.getData(), takeFlags);
        }

        renderItems();
    }

    private void addUri(Uri uri, int takeFlags) {
        if (uri == null || containsUri(uri.toString())) return;

        try {
            if (takeFlags != 0) {
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            }
        } catch (Exception ignored) {
        }

        String mime = getContentResolver().getType(uri);
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
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.trim().isEmpty()) return value;
                }
            }
        } catch (Exception ignored) {
        }
        String last = uri.getLastPathSegment();
        return last == null || last.trim().isEmpty() ? "فایل" : last;
    }

    private void renderItems() {
        if (fileList == null) return;
        fileList.removeAllViews();

        if (items.isEmpty()) {
            TextView empty = hint("هنوز فایلی انتخاب نشده است.");
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(22), dp(12), dp(22));
            fileList.addView(empty);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            MediaWidgetPrefs.Item item = items.get(i);

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            row.setPadding(dp(10), dp(7), dp(10), dp(7));
            row.setBackgroundResource(R.drawable.bg_card);

            LinearLayout texts = new LinearLayout(this);
            texts.setOrientation(LinearLayout.VERTICAL);

            TextView name = new TextView(this);
            name.setText(item.name);
            name.setTextColor(AppSettings.textPrimary(this));
            name.setTextSize(14);
            name.setMaxLines(1);
            name.setEllipsize(android.text.TextUtils.TruncateAt.END);
            name.setTypeface(null, android.graphics.Typeface.BOLD);
            texts.addView(name, new LinearLayout.LayoutParams(-1, -2));

            TextView type = new TextView(this);
            type.setText(typeLabel(item.mime) + "  •  " + item.mime);
            type.setTextColor(AppSettings.textSecondary(this));
            type.setTextSize(11);
            type.setMaxLines(1);
            type.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(type, new LinearLayout.LayoutParams(-1, -2));

            row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1f));

            Button remove = new Button(this);
            remove.setText("حذف");
            remove.setAllCaps(false);
            remove.setTextColor(0xFFD24A43);
            remove.setBackgroundResource(R.drawable.bg_soft_button);
            remove.setOnClickListener(v -> {
                if (index >= 0 && index < items.size()) {
                    items.remove(index);
                    renderItems();
                }
            });
            row.addView(remove, new LinearLayout.LayoutParams(dp(72), dp(44)));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(7);
            fileList.addView(row, lp);
        }

        TextView count = hint(
                items.size() + " از " + MediaWidgetPrefs.MAX_ITEMS + " مورد انتخاب شده");
        count.setGravity(Gravity.CENTER);
        fileList.addView(count);
    }

    private void save() {
        MediaWidgetPrefs.save(this, widgetId, items);
        WidgetPrefs.setThemeMode(
                this,
                widgetId,
                themeSpinner.getSelectedItemPosition());
        WidgetPrefs.setPalette(
                this,
                widgetId,
                paletteSpinner.getSelectedItemPosition());
        WidgetPrefs.setSizeCells(
                this,
                widgetId,
                widthPicker.getValue(),
                heightPicker.getValue());

        applyRequestedSize(widthPicker.getValue(), heightPicker.getValue());
        MediaWidgetProvider.update(this, widgetId);

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);

        if (returnToCenter) {
            finish();
        } else {
            finish();
        }
    }

    private void applyRequestedSize(int widthCells, int heightCells) {
        Bundle options = new Bundle();
        int widthDp = cellToDp(widthCells);
        int heightDp = cellToDp(heightCells);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, widthDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, heightDp);
        options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, heightDp);
        try {
            AppWidgetManager.getInstance(this)
                    .updateAppWidgetOptions(widgetId, options);
        } catch (Exception ignored) {
        }
    }

    private int cellToDp(int cells) {
        return Math.max(40, cells * 70 - 30);
    }

    private LinearLayout pickerBox(String title, int value, int min, int max) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundResource(R.drawable.bg_field);
        box.setPadding(dp(8), dp(6), dp(8), dp(6));

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
        v.setPadding(dp(2), dp(4), dp(2), dp(10));
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

    private String typeLabel(String mime) {
        String value = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if (value.startsWith("image/")) return "تصویر";
        if (value.startsWith("audio/")) return "صوت";
        if (value.startsWith("video/")) return "ویدیو";
        if (value.startsWith("text/")) return "متن";
        if (value.contains("pdf")) return "PDF";
        if (value.contains("zip") || value.contains("rar")) return "فشرده";
        return "فایل";
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
