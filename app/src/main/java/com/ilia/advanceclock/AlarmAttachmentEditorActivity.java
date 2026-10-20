package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Typeface;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.*;
import java.util.*;

/** One entry point for images, camera, documents, apps and links. */
public final class AlarmAttachmentEditorActivity extends Activity {
    private String key, cameraUri = "", pendingOriginal = "";
    private int pendingSlot = 1;
    private SharedPreferences prefs;
    private LinearLayout list;
    private ScrollView pageScroll;
    private static final int IMAGE = 1, FILE = 2, CAMERA = 3, CROP = 4;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); super.onCreate(state);
        key = getIntent().getStringExtra("key");
        if (key == null || key.isEmpty()) { finish(); return; }
        prefs = AlarmAttachments.prefs(this);
        if (state != null) { cameraUri = state.getString("camera", ""); pendingSlot = state.getInt("slot", 1); pendingOriginal = state.getString("original", ""); }
        ScrollView scroll = new ScrollView(this);
        pageScroll = scroll;
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        int side = 18 + Math.max(0, (getResources().getConfiguration().screenWidthDp - 680) / 2);
        root.setPadding(dp(side), dp(12), dp(side), dp(28));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0);
        close.setContentDescription(getString(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        header.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = text(getString(R.string.media_items_title), 21, true);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout add = card(root);
        LinearLayout photos = horizontal(add);
        source(photos, R.string.media_photo, R.drawable.ic_media_photo, () -> chooseImage(false));
        source(photos, R.string.media_camera, R.drawable.ic_media_camera, () -> chooseImage(true));
        LinearLayout shortcuts = horizontal(add);
        source(shortcuts, R.string.media_file, R.drawable.ic_about_document, () -> pick("*/*", FILE));
        source(shortcuts, R.string.media_app, R.drawable.ic_app, this::chooseApp);
        source(shortcuts, R.string.media_link, R.drawable.ic_media_link, this::addLink);

        LinearLayout options = card(root);
        Switch background = new Switch(this);
        background.setText(R.string.media_background);
        background.setTextSize(14);
        background.setTextColor(AppSettings.textPrimary(this));
        background.setMinHeight(dp(48));
        background.setPadding(dp(4), dp(8), dp(4), dp(8));
        background.setChecked(prefs.getBoolean(key + "_background", false));
        background.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(key + "_background", checked).apply());
        options.addView(background, new LinearLayout.LayoutParams(-1, -2));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list, new LinearLayout.LayoutParams(-1, -2));
        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
        render();
        if (state != null) scroll.post(() -> scroll.scrollTo(0, state.getInt("scroll", 0)));
    }

    private TextView text(String label, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(label);
        view.setTextSize(size);
        view.setTextColor(AppSettings.textPrimary(this));
        view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        return view;
    }

    private LinearLayout card(LinearLayout parent) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(14);
        parent.addView(card, lp);
        return card;
    }

    private LinearLayout horizontal(LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBaselineAligned(false);
        parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private Button button(int label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(primary ? 0xFFFFFFFF : AppSettings.primaryColor(this));
        button.setBackgroundTintList(null);
        button.setBackgroundResource(primary ? R.drawable.bg_teal_button : R.drawable.bg_soft_button);
        button.setMinHeight(dp(52));
        button.setPadding(dp(10), dp(10), dp(10), dp(10));
        return button;
    }

    private void source(LinearLayout parent, int label, int icon, Runnable action) {
        Button button = button(label, false);
        android.graphics.drawable.Drawable drawable = getDrawable(icon).mutate();
        drawable.setTint(AppSettings.primaryColor(this));
        drawable.setBounds(0, 0, dp(26), dp(26));
        button.setCompoundDrawables(null, drawable, null, null);
        button.setCompoundDrawablePadding(dp(8));
        button.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -1, 1);
        lp.setMargins(dp(4), dp(4), dp(4), dp(4));
        parent.addView(button, lp);
    }

    private void chooseImage(boolean camera) {
        String first = prefs.getString(key + "_image1", "");
        if (first.isEmpty()) pendingSlot = 1;
        else if (prefs.getString(key + "_image2", "").isEmpty()) pendingSlot = 2;
        else {
            new AlertDialog.Builder(this).setItems(new String[]{getString(R.string.media_top), getString(R.string.media_bottom)},
                    (d, i) -> { pendingSlot = i + 1; launchImage(camera); }).show(); return;
        }
        launchImage(camera);
    }

    private void launchImage(boolean camera) {
        if (!camera) { pick("image/*", IMAGE); return; }
        try {
            Uri output = AlarmMediaProvider.create(this); cameraUri = output.toString();
            Intent capture = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
                    .putExtra(android.provider.MediaStore.EXTRA_OUTPUT, output)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            capture.setClipData(ClipData.newRawUri("photo", output));
            startActivityForResult(capture, CAMERA);
        } catch (Exception ignored) {
            AlarmMediaProvider.discard(this, cameraUri); cameraUri = ""; error();
        }
    }

    private void pick(String mime, int request) {
        try {
            startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                    .setType(mime).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), request);
        } catch (ActivityNotFoundException ignored) { error(); }
    }

    private void crop(int slot) {
        pendingSlot = slot;
        pendingOriginal = prefs.getString(key + "_source" + slot, prefs.getString(key + "_image" + slot, ""));
        launchCrop();
    }

    private void launchCrop() {
        int count = pendingSlot == 2 || !prefs.getString(key + "_image2", "").isEmpty() ? 2 : 1;
        startActivityForResult(new Intent(this, AlarmPhotoCropActivity.class)
                .putExtra("uri", pendingOriginal).putExtra("count", count), CROP);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putInt("scroll", pageScroll == null ? 0 : pageScroll.getScrollY()); out.putString("original", pendingOriginal); out.putString("camera", cameraUri); out.putInt("slot", pendingSlot); super.onSaveInstanceState(out);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CAMERA && !cameraUri.isEmpty()) revokeUriPermission(Uri.parse(cameraUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (result != RESULT_OK) {
            if (request == CAMERA) { AlarmMediaProvider.discard(this, cameraUri); cameraUri = ""; }
            else if (request == CROP && pendingOriginal.equals(cameraUri)
                    && !prefs.getAll().containsValue(cameraUri)) {
                AlarmMediaProvider.discard(this, cameraUri); cameraUri = "";
            }
            return;
        }
        Uri uri = request == CAMERA ? (cameraUri.isEmpty() ? null : Uri.parse(cameraUri))
                : data == null ? null : data.getData();
        if (uri == null) return;
        if (request == CROP) {
            boolean changedToTwo = pendingSlot == 2 && prefs.getString(key + "_image2", "").isEmpty();
            SharedPreferences.Editor edit = prefs.edit();
            if (changedToTwo)
                edit.putString(key + "_image1", prefs.getString(key + "_source1", prefs.getString(key + "_image1", "")));
            edit.putString(key + "_image" + pendingSlot, uri.toString())
                    .putString(key + "_source" + pendingSlot, pendingOriginal).apply();
            render();
            if (changedToTwo) crop(1);
            return;
        }
        if (request == IMAGE || request == FILE) try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {}
        if (request == IMAGE || request == CAMERA) {
            pendingOriginal = uri.toString();
            launchCrop();
        } else if (request == FILE) {
            String mime = null;
            try { mime = getContentResolver().getType(uri); } catch (RuntimeException ignored) {}
            add(new NoteAttachment(NoteAttachment.KIND_FILE, uri.toString(), name(uri), mime == null ? "*/*" : mime));
        }
    }

    private String name(Uri uri) {
        try (Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) return c.getString(0);
        } catch (Exception ignored) {}
        return getString(R.string.media_file);
    }

    private void add(NoteAttachment item) {
        List<NoteAttachment> items = NoteAttachment.parse(prefs.getString(key + "_attachments", "[]"));
        items.add(item); prefs.edit().putString(key + "_attachments", NoteAttachment.encode(items)).apply(); render();
    }

    private void chooseApp() {
        Intent launcher = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = getPackageManager().queryIntentActivities(launcher, 0);
        apps.sort((a,b) -> String.CASE_INSENSITIVE_ORDER.compare(a.loadLabel(getPackageManager()).toString(), b.loadLabel(getPackageManager()).toString()));
        if (apps.isEmpty()) { error(); return; }
        List<String> labels = new ArrayList<>();
        for (ResolveInfo app : apps) labels.add(app.loadLabel(getPackageManager()).toString());
        List<Integer> visible = new ArrayList<>();
        for (int i = 0; i < apps.size(); i++) visible.add(i);

        LinearLayout picker = new LinearLayout(this);
        picker.setOrientation(LinearLayout.VERTICAL);
        picker.setLayoutDirection(AppSettings.layoutDirection(this));
        picker.setPadding(dp(12), dp(4), dp(12), dp(8));
        SearchView search = new SearchView(this);
        search.setIconifiedByDefault(false);
        search.setQueryHint(getString(R.string.media_search_apps));
        picker.addView(search, new LinearLayout.LayoutParams(-1, -2));
        ListView appList = new ListView(this);
        appList.setTextFilterEnabled(false);
        BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return visible.size(); }
            @Override public Object getItem(int position) { return apps.get(visible.get(position)); }
            @Override public long getItemId(int position) { return visible.get(position); }
            @Override public View getView(int position, View recycled, android.view.ViewGroup parent) {
                LinearLayout row;
                if (recycled instanceof LinearLayout) row = (LinearLayout) recycled;
                else {
                    row = new LinearLayout(AlarmAttachmentEditorActivity.this);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(8), dp(8), dp(8), dp(8));
                    ImageView logo = new ImageView(AlarmAttachmentEditorActivity.this);
                    logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                    row.addView(logo, new LinearLayout.LayoutParams(dp(36), dp(36)));
                    TextView name = text("", 16, false);
                    name.setSingleLine(true);
                    name.setEllipsize(android.text.TextUtils.TruncateAt.END);
                    name.setPaddingRelative(dp(12), 0, dp(4), 0);
                    row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
                }
                int index = visible.get(position);
                ((TextView) row.getChildAt(1)).setText(labels.get(index));
                ((ImageView) row.getChildAt(0)).setImageDrawable(apps.get(index).loadIcon(getPackageManager()));
                return row;
            }
        };
        appList.setAdapter(adapter);
        int height = Math.min(420, Math.max(160, getResources().getConfiguration().screenHeightDp / 2));
        picker.addView(appList, new LinearLayout.LayoutParams(-1, dp(height)));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.media_app)
                .setView(picker).setNegativeButton(android.R.string.cancel, null).create();
        appList.setOnItemClickListener((parent, view, position, id) -> {
            add(NoteAttachment.app(this, apps.get(visible.get(position)).activityInfo.packageName));
            dialog.dismiss();
        });
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String query) { search.clearFocus(); return true; }
            @Override public boolean onQueryTextChange(String query) {
                String needle = query.trim().toLowerCase(Locale.ROOT);
                visible.clear();
                for (int i = 0; i < apps.size(); i++) {
                    if (labels.get(i).toLowerCase(Locale.ROOT).contains(needle)
                            || apps.get(i).activityInfo.packageName.toLowerCase(Locale.ROOT).contains(needle))
                        visible.add(i);
                }
                adapter.notifyDataSetChanged();
                appList.setSelection(0);
                return true;
            }
        });
        dialog.setOnShowListener(v -> dialog.getWindow().setSoftInputMode(
                android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                        | android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE));
        dialog.show();
        search.clearFocus();
    }

    private void addLink() {
        EditText input = new EditText(this); input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        input.setTextColor(AppSettings.textPrimary(this));
        input.setBackgroundResource(R.drawable.bg_field);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout holder = new LinearLayout(this);
        holder.setPadding(dp(20), dp(12), dp(20), 0);
        holder.addView(input, new LinearLayout.LayoutParams(-1, -2));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.media_link).setView(holder)
                .setPositiveButton(R.string.media_add, null).setNegativeButton(android.R.string.cancel, null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
            String value = input.getText().toString().trim();
            if (!value.matches("(?i)^https?://.*")) value = "https://" + value;
            Uri uri = Uri.parse(value);
            if (uri.getHost() == null || !android.util.Patterns.WEB_URL.matcher(value).matches()) { input.setError(getString(R.string.runtime_text_0132)); return; }
            add(NoteAttachment.url(value)); dialog.dismiss();
        })); dialog.show();
    }

    private void render() {
        list.removeAllViews();
        boolean two = !prefs.getString(key + "_image2", "").isEmpty();
        for (int i = 1; i <= 2; i++) {
            final int slot = i;
            String image = prefs.getString(key + "_image" + i, "");
            if (image.isEmpty()) continue;
            LinearLayout photo = card(list);
            TextView position = text(getString(two ? (slot == 1 ? R.string.media_top : R.string.media_bottom)
                    : R.string.media_fullscreen_photo), 15, true);
            position.setTextColor(AppSettings.primaryColor(this));
            position.setPadding(0, 0, 0, dp(10));
            photo.addView(position, new LinearLayout.LayoutParams(-1, -2));
            AlarmPhotoView preview = new AlarmPhotoView(this, false);
            GradientDrawable frameBackground = new GradientDrawable();
            frameBackground.setColor(AppSettings.field(this));
            frameBackground.setCornerRadius(dp(16));
            preview.setBackground(frameBackground);
            preview.setClipToOutline(true);
            preview.setContentDescription(position.getText());
            preview.load(image);
            photo.addView(preview, new LinearLayout.LayoutParams(-1, dp(220)));
            LinearLayout actions = horizontal(photo);
            Button frame = button(R.string.media_crop, false);
            LinearLayout.LayoutParams first = new LinearLayout.LayoutParams(0, -2, 1);
            first.topMargin = dp(12);
            first.setMarginEnd(dp(8));
            actions.addView(frame, first);
            frame.setOnClickListener(v -> crop(slot));
            Button remove = button(R.string.tool_remove_image, false);
            ButtonStyles.destructive(remove);
            LinearLayout.LayoutParams second = new LinearLayout.LayoutParams(0, -2, 1);
            second.topMargin = dp(12);
            actions.addView(remove, second);
            remove.setOnClickListener(v -> {
                SharedPreferences.Editor edit = prefs.edit();
                if (slot == 1) {
                    String source = prefs.getString(key + "_source2", prefs.getString(key + "_image2", ""));
                    edit.putString(key + "_image1", source).putString(key + "_source1", source)
                            .remove(key + "_image2").remove(key + "_source2");
                } else edit.remove(key + "_image2").remove(key + "_source2")
                        .putString(key + "_image1", prefs.getString(key + "_source1", prefs.getString(key + "_image1", "")));
                edit.apply(); render();
                if (!prefs.getString(key + "_image1", "").isEmpty()) crop(1);
            });
        }
        List<NoteAttachment> items = NoteAttachment.parse(prefs.getString(key + "_attachments", "[]"));
        for (NoteAttachment item : items) {
            LinearLayout entry = card(list);
            LinearLayout details = horizontal(entry);
            ImageView icon = new ImageView(this);
            boolean app = NoteAttachment.KIND_APP.equals(item.kind);
            icon.setImageResource(app ? R.drawable.ic_app : NoteAttachment.KIND_URL.equals(item.kind)
                    ? R.drawable.ic_media_link : R.drawable.ic_about_document);
            String displayName = item.name == null || item.name.trim().isEmpty() ? item.value : item.name;
            if (app) {
                try {
                    android.content.pm.ApplicationInfo info = getPackageManager().getApplicationInfo(item.value, 0);
                    icon.setImageDrawable(getPackageManager().getApplicationIcon(info));
                    displayName = getPackageManager().getApplicationLabel(info).toString();
                } catch (android.content.pm.PackageManager.NameNotFoundException ignored) {
                    icon.setColorFilter(AppSettings.primaryColor(this), PorterDuff.Mode.SRC_IN);
                }
            } else icon.setColorFilter(AppSettings.primaryColor(this), PorterDuff.Mode.SRC_IN);
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            details.addView(icon, new LinearLayout.LayoutParams(dp(44), dp(44)));
            TextView label = text(displayName, 16, true);
            label.setPaddingRelative(dp(app ? 8 : 14), dp(4), dp(app ? 6 : 0), dp(4));
            if (app) {
                label.setSingleLine(true);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
            }
            details.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            LinearLayout actions = app ? details : horizontal(entry);
            Button open = button(R.string.media_open, false);
            open.setOnClickListener(v -> item.open(this));
            LinearLayout.LayoutParams openParams = new LinearLayout.LayoutParams(0, -2, 1);
            if (app) {
                compactAction(open);
                openParams = new LinearLayout.LayoutParams(-2, dp(48));
            } else openParams.topMargin = dp(12);
            openParams.setMarginEnd(dp(8));
            actions.addView(open, openParams);
            Button remove = button(R.string.tool_delete, false);
            ButtonStyles.destructive(remove);
            remove.setOnClickListener(v -> {
                List<NoteAttachment> current = NoteAttachment.parse(prefs.getString(key + "_attachments", "[]"));
                for (int index = 0; index < current.size(); index++) {
                    NoteAttachment candidate = current.get(index);
                    if (candidate.kind.equals(item.kind) && candidate.value.equals(item.value)) { current.remove(index); break; }
                }
                prefs.edit().putString(key + "_attachments", NoteAttachment.encode(current)).apply();
                render();
            });
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(0, -2, 1);
            if (app) {
                compactAction(remove);
                removeParams = new LinearLayout.LayoutParams(-2, dp(48));
            } else removeParams.topMargin = dp(12);
            actions.addView(remove, removeParams);
        }
        if (list.getChildCount() == 0) {
            LinearLayout empty = card(list);
            TextView hint = text(getString(R.string.media_empty), 15, false);
            hint.setGravity(Gravity.CENTER);
            hint.setTextColor(AppSettings.textSecondary(this));
            hint.setPadding(dp(8), dp(22), dp(8), dp(22));
            empty.addView(hint, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private void compactAction(Button action) {
        action.setMinWidth(0);
        action.setMinimumWidth(0);
        action.setMinHeight(0);
        action.setMinimumHeight(0);
        action.setTextSize(13);
        action.setSingleLine(true);
        action.setPadding(dp(8), 0, dp(8), 0);
    }

    @Override public void finish() { super.finish(); AppSettings.playFullscreenExit(this); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void error() { Toast.makeText(this, R.string.media_image_error, Toast.LENGTH_SHORT).show(); }
}
