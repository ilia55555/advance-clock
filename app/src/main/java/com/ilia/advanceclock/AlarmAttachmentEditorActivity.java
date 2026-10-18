package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
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
    private static final int IMAGE = 1, FILE = 2, CAMERA = 3, CROP = 4;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); super.onCreate(state);
        key = getIntent().getStringExtra("key");
        if (key == null || key.isEmpty()) { finish(); return; }
        prefs = AlarmAttachments.prefs(this);
        if (state != null) { cameraUri = state.getString("camera", ""); pendingSlot = state.getInt("slot", 1); pendingOriginal = state.getString("original", ""); }
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(this)); root.setPadding(dp(24), dp(28), dp(24), dp(36));
        root.setBackgroundColor(AppSettings.background(this)); scroll.addView(root);
        TextView title = new TextView(this); title.setText(R.string.media_attachments); title.setTextSize(23);
        title.setTextColor(AppSettings.textPrimary(this)); root.addView(title);
        Button add = new Button(this); add.setText(R.string.media_add); root.addView(add);
        add.setOnClickListener(v -> new AlertDialog.Builder(this).setItems(new String[]{
                getString(R.string.media_photo), getString(R.string.media_camera), getString(R.string.media_file),
                getString(R.string.media_app), getString(R.string.media_link)}, (d, which) -> {
            if (which == 0 || which == 1) chooseImage(which == 1);
            else if (which == 2) pick("*/*", FILE);
            else if (which == 3) chooseApp();
            else addLink();
        }).show());
        Switch background = new Switch(this); background.setText(R.string.media_background);
        background.setTextColor(AppSettings.textPrimary(this)); background.setChecked(prefs.getBoolean(key + "_background", false));
        background.setOnCheckedChangeListener((v, checked) -> prefs.edit().putBoolean(key + "_background", checked).apply());
        root.addView(background);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list);
        Button close = new Button(this); close.setText(R.string.runtime_text_0002); root.addView(close);
        close.setOnClickListener(v -> finish());
        setContentView(scroll); AppSettings.applyFullscreenInsets(scroll); render();
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
        } catch (Exception ignored) { error(); }
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
        out.putString("original", pendingOriginal); out.putString("camera", cameraUri); out.putInt("slot", pendingSlot); super.onSaveInstanceState(out);
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
            String mime = getContentResolver().getType(uri);
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
        String[] names = new String[apps.size()];
        for (int i = 0; i < names.length; i++) names[i] = apps.get(i).loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this).setTitle(R.string.media_app).setItems(names,
                (d,i) -> add(NoteAttachment.app(this, apps.get(i).activityInfo.packageName))).show();
    }

    private void addLink() {
        EditText input = new EditText(this); input.setSingleLine(true);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(R.string.media_link).setView(input)
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
        for (int i = 1; i <= 2; i++) {
            final int slot = i; String image = prefs.getString(key + "_image" + i, "");
            if (image.isEmpty()) continue;
            AlarmPhotoView preview = new AlarmPhotoView(this, false); preview.load(image);
            list.addView(preview, new LinearLayout.LayoutParams(-1, dp(200)));
            Button frame = new Button(this); frame.setText(R.string.media_crop); list.addView(frame);
            frame.setOnClickListener(v -> crop(slot));
            Button remove = new Button(this); remove.setText(R.string.tool_remove_image); list.addView(remove);
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
        for (int i = 0; i < items.size(); i++) {
            final int index = i; NoteAttachment item = items.get(i);
            Button row = new Button(this); row.setText(item.name); list.addView(row);
            row.setOnClickListener(v -> new AlertDialog.Builder(this).setItems(new String[]{getString(R.string.media_open), getString(R.string.tool_delete)}, (d, action) -> {
                if (action == 0) item.open(this);
                else { items.remove(index); prefs.edit().putString(key + "_attachments", NoteAttachment.encode(items)).apply(); render(); }
            }).show());
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void error() { Toast.makeText(this, R.string.media_image_error, Toast.LENGTH_SHORT).show(); }
}
