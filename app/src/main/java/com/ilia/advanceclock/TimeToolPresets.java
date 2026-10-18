package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Named, reusable configurations. Running state and lap history are kept separately. */
final class TimeToolPresets {
    interface Editor {
        JSONObject capture() throws JSONException;
        void load(JSONObject config, boolean run) throws JSONException;
        boolean busy();
    }
    private final Activity host;
    private final String kind;
    private final Editor editor;
    private final SharedPreferences prefs;
    private final LinearLayout controls;
    private final Button[] photoButtons = new Button[2];
    private final ImageView[] previews = new ImageView[2];
    private String editingId = "";

    TimeToolPresets(Activity host, android.widget.EditText label, String kind, Editor editor) {
        this.host = host;
        this.kind = kind;
        this.editor = editor;
        prefs = host.getSharedPreferences("time_tools", Activity.MODE_PRIVATE);
        LinearLayout parent = (LinearLayout) label.getParent();
        controls = new LinearLayout(host);
        controls.setOrientation(LinearLayout.VERTICAL);
        parent.addView(controls, parent.indexOfChild(label) + 1,
                new LinearLayout.LayoutParams(-1, -2));
        LinearLayout photos = new LinearLayout(host);
        controls.addView(photos);
        for (int i = 0; i < 2; i++) {
            final int slot = i + 1;
            LinearLayout column = new LinearLayout(host);
            column.setOrientation(LinearLayout.VERTICAL);
            photos.addView(column, new LinearLayout.LayoutParams(0, -2, 1));
            photoButtons[i] = button(i == 0 ? R.string.runtime_text_0141 : R.string.runtime_text_0142);
            column.addView(photoButtons[i]);
            photoButtons[i].setOnClickListener(v -> ToolImagePickerFragment.pick(host, kind, slot));
            previews[i] = new ImageView(host);
            previews[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
            previews[i].setContentDescription(host.getString(R.string.tool_remove_image));
            column.addView(previews[i], new LinearLayout.LayoutParams(-1, dp(96)));
            previews[i].setOnClickListener(v -> new AlertDialog.Builder(host)
                    .setMessage(R.string.tool_remove_image)
                    .setPositiveButton(android.R.string.ok, (d, w) -> {
                        prefs.edit().remove(kind + "_image" + slot).apply(); refresh();
                    }).setNegativeButton(android.R.string.cancel, null).show());
        }
        LinearLayout actions = new LinearLayout(host);
        controls.addView(actions);
        Button save = button(R.string.tool_save);
        Button saved = button(R.string.tool_saved);
        actions.addView(save, new LinearLayout.LayoutParams(0, -2, 1));
        actions.addView(saved, new LinearLayout.LayoutParams(0, -2, 1));
        save.setOnClickListener(v -> savePreset());
        saved.setOnClickListener(v -> showSaved());
        refresh();
    }

    private Button button(int text) {
        Button button = new Button(host);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(AppSettings.primaryColor(host));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        return button;
    }

    void refresh() {
        boolean busy = editor.busy();
        for (int i = 0; i < 2; i++) {
            String uri = prefs.getString(kind + "_image" + (i + 1), "");
            photoButtons[i].setEnabled(!busy);
            previews[i].setEnabled(!busy);
            previews[i].setImageDrawable(null);
            previews[i].setVisibility(uri.isEmpty() ? View.GONE : View.VISIBLE);
            if (!uri.isEmpty()) try { previews[i].setImageURI(Uri.parse(uri)); }
            catch (Exception ignored) {}
        }
    }

    private JSONArray read() {
        return TimeToolPresetStore.read(prefs.getString(kind + "_presets", "[]"));
    }

    private void savePreset() {
        try {
            JSONObject config = editor.capture();
            if (config == null) return;
            for (int i = 1; i <= 2; i++) config.put("image" + i, prefs.getString(kind + "_image" + i, ""));
            if (editingId.isEmpty()) editingId = java.util.UUID.randomUUID().toString();
            config.put("id", editingId);
            prefs.edit().putString(kind + "_presets", TimeToolPresetStore.save(
                    prefs.getString(kind + "_presets", "[]"), config)).apply();
            editingId = "";
            Toast.makeText(host, R.string.tool_saved_success, Toast.LENGTH_SHORT).show();
        } catch (JSONException ignored) { showError(); }
    }

    private void showSaved() {
        JSONArray items = read();
        if (items.length() == 0) {
            Toast.makeText(host, R.string.tool_no_saved, Toast.LENGTH_SHORT).show(); return;
        }
        String[] names = new String[items.length()];
        for (int i = 0; i < names.length; i++) names[i] = items.optJSONObject(i).optString("label", "");
        new AlertDialog.Builder(host).setTitle(R.string.tool_saved).setItems(names, (d, index) -> {
            JSONObject item = items.optJSONObject(index);
            new AlertDialog.Builder(host).setTitle(names[index]).setItems(new String[]{
                    host.getString(R.string.tool_run), host.getString(R.string.tool_edit),
                    host.getString(R.string.tool_delete)}, (dialog, action) -> {
                if (action == 2) {
                    new AlertDialog.Builder(host).setMessage(R.string.tool_delete_confirm)
                            .setPositiveButton(android.R.string.ok, (confirm, which) -> {
                                // Read again so deleting cannot discard a more recent edit.
                                prefs.edit().putString(kind + "_presets", TimeToolPresetStore.delete(
                                        prefs.getString(kind + "_presets", "[]"), item.optString("id"))).apply();
                                if (item.optString("id").equals(editingId)) editingId = "";
                            }).setNegativeButton(android.R.string.cancel, null).show();
                } else if (editor.busy()) {
                    new AlertDialog.Builder(host).setMessage(R.string.tool_replace_running)
                            .setPositiveButton(android.R.string.ok, (confirm, which) -> load(item, action == 0))
                            .setNegativeButton(android.R.string.cancel, null).show();
                } else load(item, action == 0);
            }).show();
        }).show();
    }

    private void load(JSONObject item, boolean run) {
        try {
            // The editor validates dates before replacing the active run or its images.
            editor.load(item, run);
            editingId = run ? "" : item.optString("id");
            refresh();
        } catch (JSONException ignored) { showError(); }
    }

    static void restoreImages(Activity host, String kind, JSONObject item) {
        host.getSharedPreferences("time_tools", Activity.MODE_PRIVATE).edit()
                .putString(kind + "_image1", item.optString("image1"))
                .putString(kind + "_image2", item.optString("image2")).apply();
    }

    private void showError() { Toast.makeText(host, R.string.tool_invalid, Toast.LENGTH_SHORT).show(); }
    private int dp(int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
}
