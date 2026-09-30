package com.ilia.advanceclock;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

public final class NoForgetQuickAddActivity extends Activity {
    private static final int REQ_FILES = 730;
    private static final int REQ_APP = 731;
    private static final int REQ_SOUND = 732;
    private String pickerMode = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        super.onCreate(savedInstanceState);

        pickerMode = getIntent().getStringExtra("pickerMode");
        if (pickerMode == null) pickerMode = "";
        if (!pickerMode.isEmpty()) {
            launchPicker();
            return;
        }

        setContentView(R.layout.activity_noforget_quick_add);

        EditText text = findViewById(R.id.quick_note_text);
        Spinner priority = findViewById(R.id.quick_note_priority);

        String[] labels = PriorityUtils.labels();
        String[] values = new String[labels.length];
        for (int i = 0; i < labels.length; i++) values[i] = AppString.get(R.string.runtime_text_0420) + labels[i];

        ArrayAdapter<String> p = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, values);
        p.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(p);
        priority.setSelection(PriorityUtils.MEDIUM);

        findViewById(R.id.quick_note_save).setOnClickListener(v -> {
            String value = text.getText().toString().trim();
            if (value.isEmpty()) {
                LogoToast.makeText(this, AppString.get(R.string.runtime_text_0385), Toast.LENGTH_SHORT).show();
                return;
            }

            long now = System.currentTimeMillis();
            NoForgetItem item = new NoForgetItem(
                    now,
                    value,
                    "",
                    "[]",
                    priority.getSelectedItemPosition(),
                    false,
                    0L,
                    false,
                    now);

            new NoForgetStore(this).save(item);
            NoForgetWidgetProvider.updateAll(this);
            finish();
        });

        findViewById(R.id.quick_note_more).setOnClickListener(v -> {
            startActivity(new Intent(this, NoForgetEditorActivity.class));
            finish();
        });
        findViewById(R.id.quick_note_cancel).setOnClickListener(v -> finish());
    }

    private void launchPicker() {
        if ("files".equals(pickerMode)) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT)
                    .addCategory(Intent.CATEGORY_OPENABLE)
                    .setType("*/*")
                    .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                            | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(intent, REQ_FILES);
            return;
        }
        if ("app".equals(pickerMode)) {
            Intent base = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            startActivityForResult(
                    new Intent(Intent.ACTION_PICK_ACTIVITY)
                            .putExtra(Intent.EXTRA_INTENT, base)
                            .putExtra(Intent.EXTRA_TITLE, AppString.get(R.string.runtime_text_0206)),
                    REQ_APP);
            return;
        }
        if ("sound".equals(pickerMode)) {
            startActivityForResult(new Intent(this, SoundPickerActivity.class), REQ_SOUND);
            return;
        }
        finish();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) {
            finish();
            return;
        }

        if (requestCode == REQ_SOUND) {
            getSharedPreferences(MainNoteTabEnhancer.BRIDGE_PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(MainNoteTabEnhancer.BRIDGE_SOUND_URI,
                            data.getStringExtra(SoundPickerActivity.EXTRA_URI))
                    .putString(MainNoteTabEnhancer.BRIDGE_SOUND_NAME,
                            data.getStringExtra(SoundPickerActivity.EXTRA_NAME))
                    .apply();
            finish();
            return;
        }

        ArrayList<NoteAttachment> picked = new ArrayList<>();
        if (requestCode == REQ_APP && data.getComponent() != null) {
            picked.add(NoteAttachment.app(this, data.getComponent().getPackageName()));
        } else if (requestCode == REQ_FILES) {
            ClipData clips = data.getClipData();
            if (clips != null) {
                for (int i = 0; i < clips.getItemCount(); i++) {
                    NoteAttachment item = fileAttachment(clips.getItemAt(i).getUri());
                    if (item != null) picked.add(item);
                }
            } else if (data.getData() != null) {
                NoteAttachment item = fileAttachment(data.getData());
                if (item != null) picked.add(item);
            }
        }

        getSharedPreferences(MainNoteTabEnhancer.BRIDGE_PREFS, MODE_PRIVATE)
                .edit()
                .putString(MainNoteTabEnhancer.BRIDGE_ATTACHMENTS, NoteAttachment.encode(picked))
                .apply();
        finish();
    }

    private NoteAttachment fileAttachment(Uri uri) {
        if (uri == null) return null;
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (Exception ignored) {}
        String mime = getContentResolver().getType(uri);
        return new NoteAttachment(
                NoteAttachment.KIND_FILE,
                uri.toString(),
                fileName(uri),
                mime == null ? "*/*" : mime);
    }

    private String fileName(Uri uri) {
        try (Cursor cursor = getContentResolver().query(
                uri,
                new String[]{OpenableColumns.DISPLAY_NAME},
                null,
                null,
                null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (Exception ignored) {}
        return AppString.get(R.string.runtime_text_0059);
    }
}
