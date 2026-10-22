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
    private final Button attachments;

    TimeToolPresets(Activity host, android.widget.EditText label, String kind, Editor editor) {
        this.host = host;
        this.kind = kind;
        this.editor = editor;
        prefs = host.getSharedPreferences("time_tools", Activity.MODE_PRIVATE);
        LinearLayout parent = (LinearLayout) label.getParent();
        controls = new LinearLayout(host);
        controls.setOrientation(LinearLayout.VERTICAL);
        boolean stopwatch = ToolAlarmScheduler.STOPWATCH.equals(kind);
        View anchor = parent.findViewById(stopwatch ? R.id.stopwatch_lap : R.id.timer_start);
        if (!stopwatch && anchor != null) anchor = (View) anchor.getParent();
        LinearLayout.LayoutParams extrasLayout = new LinearLayout.LayoutParams(-1, -2);
        extrasLayout.topMargin = Math.round(16 * host.getResources().getDisplayMetrics().density);
        parent.addView(controls, anchor == null ? parent.getChildCount()
                : parent.indexOfChild(anchor) + 1, extrasLayout);
        attachments = AlarmAttachments.button(host, kind);
        controls.addView(attachments);
        LinearLayout actions = new LinearLayout(host);
        LinearLayout.LayoutParams actionsLayout = new LinearLayout.LayoutParams(-1, -2);
        actionsLayout.topMargin = Math.round(12 * host.getResources().getDisplayMetrics().density);
        controls.addView(actions, actionsLayout);
        Button save = button(R.string.tool_save);
        Button saved = button(R.string.tool_saved);
        actions.addView(save, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams savedLayout = new LinearLayout.LayoutParams(0, -2, 1);
        savedLayout.setMarginStart(Math.round(10 * host.getResources().getDisplayMetrics().density));
        actions.addView(saved, savedLayout);
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
        controls.setVisibility(busy ? View.GONE : View.VISIBLE);
        attachments.setEnabled(!busy);
    }

    private JSONArray read() {
        return TimeToolPresetStore.ordered(prefs.getString(kind + "_presets", "[]"),
                ToolAlarmScheduler.STOPWATCH.equals(kind), System.currentTimeMillis(),
                prefs.getBoolean(kind + "_running", false)
                        ? prefs.getString(kind + "_active_preset_id", "") : "");
    }

    private void savePreset() {
        try {
            JSONObject config = editor.capture();
            if (config == null) return;
            for (int i = 1; i <= 2; i++) config.put("image" + i, prefs.getString(kind + "_image" + i, ""));
            config.put("attachments", prefs.getString(kind + "_attachments", "[]"));
            config.put("background", prefs.getBoolean(kind + "_background", false));
            for (int i = 1; i <= 2; i++) config.put("source" + i, prefs.getString(kind + "_source" + i, prefs.getString(kind + "_image" + i, "")));
            String editingId = prefs.getString(kind + "_editing_id", "");
            if (editingId.isEmpty()) editingId = java.util.UUID.randomUUID().toString();
            config.put("id", editingId);
            config.put("savedAt", System.currentTimeMillis());
            prefs.edit().remove(kind + "_editing_id").putString(kind + "_presets", TimeToolPresetStore.save(
                    prefs.getString(kind + "_presets", "[]"), config)).apply();
            LogoToast.makeText(host, R.string.tool_saved_success, Toast.LENGTH_SHORT).show();
        } catch (JSONException ignored) { showError(); }
    }

    private void showSaved() {
        JSONArray items = read();
        if (items.length() == 0) {
            LogoToast.makeText(host, R.string.tool_no_saved, Toast.LENGTH_SHORT).show(); return;
        }
        String[] names = new String[items.length()];
        for (int i = 0; i < names.length; i++) names[i] = items.optJSONObject(i).optString("label", "");
        new AlertDialog.Builder(host).setTitle(R.string.tool_saved).setItems(names, (d, index) -> {
            JSONObject item = items.optJSONObject(index);
            String[] choices = {host.getString(R.string.tool_run), host.getString(R.string.tool_edit),
                    host.getString(R.string.tool_delete)};
            android.widget.ArrayAdapter<String> options = new android.widget.ArrayAdapter<String>(
                    host, android.R.layout.simple_list_item_1, choices) {
                @Override public View getView(int position, View recycled, android.view.ViewGroup parent) {
                    android.widget.TextView row = (android.widget.TextView) super.getView(position, recycled, parent);
                    if (position == 2) ButtonStyles.destructive(row);
                    else { row.setBackgroundColor(0); row.setTextColor(AppSettings.textPrimary(host)); }
                    return row;
                }
            };
            new AlertDialog.Builder(host).setTitle(names[index]).setAdapter(options, (dialog, action) -> {
                if (action == 2) {
                    AlertDialog confirmation = new AlertDialog.Builder(host).setMessage(R.string.tool_delete_confirm)
                            .setPositiveButton(android.R.string.ok, (confirm, which) -> {
                                // Read again so deleting cannot discard a more recent edit.
                                prefs.edit().putString(kind + "_presets", TimeToolPresetStore.delete(
                                        prefs.getString(kind + "_presets", "[]"), item.optString("id"))).apply();
                                if (item.optString("id").equals(prefs.getString(kind + "_editing_id", "")))
                                    prefs.edit().remove(kind + "_editing_id").apply();
                            }).setNegativeButton(android.R.string.cancel, null).create();
                    confirmation.setOnShowListener(ignored -> ButtonStyles.destructive(
                            confirmation.getButton(AlertDialog.BUTTON_POSITIVE)));
                    confirmation.show();
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
            if (run && prefs.getBoolean(kind + "_running", false))
                prefs.edit().putString(kind + "_active_preset_id", item.optString("id")).apply();
            prefs.edit().putString(kind + "_editing_id", run ? "" : item.optString("id")).apply();
            refresh();
        } catch (JSONException ignored) { showError(); }
    }

    static void restoreImages(Activity host, String kind, JSONObject item) {
        host.getSharedPreferences("time_tools", Activity.MODE_PRIVATE).edit()
                .putString(kind + "_image1", item.optString("image1"))
                .putString(kind + "_image2", item.optString("image2"))
                .putString(kind + "_source1", item.optString("source1", item.optString("image1")))
                .putString(kind + "_source2", item.optString("source2", item.optString("image2")))
                .putString(kind + "_attachments", item.optString("attachments", "[]"))
                .putBoolean(kind + "_background", item.optBoolean("background", false)).apply();
    }

    private void showError() { LogoToast.makeText(host, R.string.tool_invalid, Toast.LENGTH_SHORT).show(); }
    private int dp(int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
}
