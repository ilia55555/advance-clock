package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.WeakHashMap;

final class MainNoteTabEnhancer {
    static final String BRIDGE_PREFS = "quick_note_attachment_bridge";
    static final String BRIDGE_ATTACHMENTS = "pending_attachments";
    static final String BRIDGE_SOUND_URI = "pending_sound_uri";
    static final String BRIDGE_SOUND_NAME = "pending_sound_name";

    private static final String ACTIONS_TAG = "main-note-actions-v2";
    private static final String ALARM_EXTRAS_TAG = "main-note-alarm-extras-v2";
    private static final WeakHashMap<Activity, State> STATES = new WeakHashMap<>();

    private MainNoteTabEnhancer() {}

    static void install(Application app) {
        app.registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle state) {}
            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityResumed(Activity activity) {
                if (activity instanceof MainActivity) enhance(activity);
            }
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
            @Override public void onActivityDestroyed(Activity activity) { STATES.remove(activity); }
        });
    }

    private static void enhance(Activity activity) {
        State state = STATES.get(activity);
        if (state == null) {
            state = new State();
            STATES.put(activity, state);
        }
        installActions(activity, state);
        installAlarmExtras(activity, state);
        View sketchCard = activity.findViewById(R.id.note_sketch_card);
        if (sketchCard != null) sketchCard.setVisibility(View.GONE);
        importBridgeResults(activity, state);
        overrideSave(activity, state);
        updateActionButtons(activity, state);
        updateSoundButton(activity, state);
        reopenPendingDialog(activity, state);
    }

    private static void installActions(Activity activity, State state) {
        Spinner priority = activity.findViewById(R.id.quick_note_priority);
        if (priority == null || !(priority.getParent() instanceof LinearLayout)) return;
        LinearLayout parent = (LinearLayout) priority.getParent();
        if (parent.findViewWithTag(ACTIONS_TAG) != null) return;

        LinearLayout row = new LinearLayout(activity);
        row.setTag(ACTIONS_TAG);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        state.sketchButton = actionButton(activity, AppString.get(R.string.runtime_text_0118));
        state.filesButton = actionButton(activity, AppString.get(R.string.runtime_text_0116));
        state.targetButton = actionButton(activity, AppString.get(R.string.runtime_text_0117));

        addWeighted(row, state.sketchButton, activity);
        addGap(row, activity);
        addWeighted(row, state.filesButton, activity);
        addGap(row, activity);
        addWeighted(row, state.targetButton, activity);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 54));
        params.topMargin = dp(activity, 8);
        parent.addView(row, parent.indexOfChild(priority) + 1, params);

        state.sketchButton.setOnClickListener(v -> showSketchDialog(activity, state));
        state.filesButton.setOnClickListener(v -> showFilesDialog(activity, state));
        state.targetButton.setOnClickListener(v -> showTargetsDialog(activity, state));
    }

    private static void installAlarmExtras(Activity activity, State state) {
        LinearLayout controls = activity.findViewById(R.id.quick_note_alarm_controls);
        if (controls == null) return;
        View existing = controls.findViewWithTag(ALARM_EXTRAS_TAG);
        if (existing != null) return;

        LinearLayout holder = new LinearLayout(activity);
        holder.setTag(ALARM_EXTRAS_TAG);
        holder.setOrientation(LinearLayout.VERTICAL);

        state.remindersButton = fieldButton(activity, AppString.get(R.string.runtime_text_0701));
        state.remindersButton.setOnClickListener(v -> AlarmReminderDialog.show(
                activity,
                state.reminderMode,
                state.reminderMinutesJson,
                (mode, json) -> {
                    state.reminderMode = mode;
                    state.reminderMinutesJson = json;
                    updateReminderButton(state);
                }));
        addBlock(holder, state.remindersButton, activity, 52);

        LinearLayout vibrateRow = switchRow(activity, AppString.get(R.string.runtime_text_0022));
        state.vibrate = (Switch) vibrateRow.getChildAt(1);
        state.vibrate.setChecked(true);
        addBlock(holder, vibrateRow, activity, 56);

        state.soundButton = fieldButton(activity, AppString.get(R.string.runtime_text_0702));
        state.soundButton.setOnClickListener(v -> {
            state.pendingDialog = "sound";
            launchBridge(activity, "sound");
        });
        addBlock(holder, state.soundButton, activity, 54);

        LinearLayout fullscreenRow = new LinearLayout(activity);
        fullscreenRow.setOrientation(LinearLayout.HORIZONTAL);
        fullscreenRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout unlocked = labeledSwitch(activity, AppString.get(R.string.runtime_text_0703));
        LinearLayout locked = labeledSwitch(activity, AppString.get(R.string.runtime_text_0704));
        state.fullscreenUnlocked = (Switch) unlocked.getChildAt(1);
        state.fullscreenLocked = (Switch) locked.getChildAt(1);
        state.fullscreenUnlocked.setChecked(true);
        state.fullscreenLocked.setChecked(true);

        fullscreenRow.addView(unlocked, new LinearLayout.LayoutParams(0, dp(activity, 64), 1f));
        addGap(fullscreenRow, activity);
        fullscreenRow.addView(locked, new LinearLayout.LayoutParams(0, dp(activity, 64), 1f));
        addBlock(holder, fullscreenRow, activity, 64);

        controls.addView(holder, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        updateReminderButton(state);
    }

    private static void showSketchDialog(Activity activity, State state) {
        SketchView stored = activity.findViewById(R.id.quick_note_sketch);
        if (stored == null) return;

        LinearLayout root = NoteModalStyler.content(
                activity,
                AppString.get(R.string.runtime_text_0118),
                AppString.get(R.string.runtime_text_0462));

        HorizontalScrollView scroll = new HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout toolbar = new LinearLayout(activity);
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(toolbar);

        Button undo = toolButton(activity, "↶");
        Button redo = toolButton(activity, "↷");
        Button clear = toolButton(activity, AppString.get(R.string.runtime_text_0203));
        Button palette = toolButton(activity, AppString.get(R.string.runtime_text_0202));
        Button grid = toolButton(activity, AppString.get(R.string.runtime_text_0201));
        Spinner size = new Spinner(activity);
        size.setAdapter(new PenSizeAdapter(activity));
        size.setSelection(1);

        toolbar.addView(undo, toolParams(activity));
        toolbar.addView(redo, toolParams(activity));
        toolbar.addView(clear, toolParams(activity));
        toolbar.addView(palette, toolParams(activity));
        toolbar.addView(grid, toolParams(activity));
        toolbar.addView(size, toolParams(activity));

        SketchView canvas = new SketchView(activity);
        canvas.load(stored.serialize());
        canvas.setPenColor(stored.getPenColor());
        canvas.setGridVisible(true);
        size.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                float[] widths = {2f, 4f, 7f, 10f};
                canvas.setPenWidthDp(widths[Math.max(0, Math.min(widths.length - 1, position))]);
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
        undo.setOnClickListener(v -> canvas.undo());
        redo.setOnClickListener(v -> canvas.redo());
        clear.setOnClickListener(v -> canvas.clearSketch());
        palette.setOnClickListener(v -> PaletteDialog.show(activity, canvas.getPenColor(), canvas::setPenColor));
        grid.setOnClickListener(v -> {
            boolean next = !canvas.isGridVisible();
            canvas.setGridVisible(next);
            grid.setText(next ? AppString.get(R.string.runtime_text_0201) : AppString.get(R.string.runtime_text_0200));
        });

        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 54)));
        LinearLayout.LayoutParams canvasParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 390));
        canvasParams.topMargin = dp(activity, 8);
        root.addView(canvas, canvasParams);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setNegativeButton(AppString.get(R.string.runtime_text_0003), null)
                .setPositiveButton(AppString.get(R.string.runtime_text_0204), (buttonDialog, which) -> {
                    stored.load(canvas.serialize());
                    updateActionButtons(activity, state);
                })
                .create();
        NoteModalStyler.show(dialog);
    }

    private static void showFilesDialog(Activity activity, State state) {
        if (state.dialog != null && state.dialog.isShowing()) state.dialog.dismiss();

        LinearLayout root = NoteModalStyler.content(
                activity,
                AppString.get(R.string.runtime_text_0129),
                AppString.get(R.string.runtime_text_0463));

        Button add = modalButton(activity, AppString.get(R.string.runtime_text_0464));
        addBlock(root, add, activity, 50);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 300));
        scrollParams.topMargin = dp(activity, 8);
        root.addView(scroll, scrollParams);
        renderAttachmentList(activity, state, list, NoteAttachment.KIND_FILE);

        state.dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setNegativeButton(AppString.get(R.string.runtime_text_0002), null)
                .create();
        add.setOnClickListener(v -> {
            state.pendingDialog = "files";
            state.dialog.dismiss();
            launchBridge(activity, "files");
        });
        NoteModalStyler.show(state.dialog);
    }

    private static void showTargetsDialog(Activity activity, State state) {
        if (state.dialog != null && state.dialog.isShowing()) state.dialog.dismiss();

        LinearLayout root = NoteModalStyler.content(
                activity,
                AppString.get(R.string.runtime_text_0117),
                AppString.get(R.string.runtime_text_0465));
        Button chooseApp = modalButton(activity, AppString.get(R.string.runtime_text_0130));
        root.addView(chooseApp, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 50)));

        EditText url = new EditText(activity);
        url.setHint(AppString.get(R.string.runtime_text_0208));
        url.setSingleLine(true);
        url.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        url.setBackgroundResource(R.drawable.bg_field);
        url.setTextColor(AppSettings.textPrimary(activity));
        url.setHintTextColor(AppSettings.textSecondary(activity));
        url.setPadding(dp(activity, 12), 0, dp(activity, 12), 0);
        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 52));
        urlParams.topMargin = dp(activity, 8);
        root.addView(url, urlParams);

        Button addSite = modalButton(activity, AppString.get(R.string.runtime_text_0466));
        addBlock(root, addSite, activity, 48);

        ScrollView scroll = new ScrollView(activity);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(list);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 270));
        scrollParams.topMargin = dp(activity, 8);
        root.addView(scroll, scrollParams);
        renderAttachmentList(activity, state, list, "targets");

        state.dialog = new AlertDialog.Builder(activity)
                .setView(root)
                .setNegativeButton(AppString.get(R.string.runtime_text_0002), null)
                .create();

        chooseApp.setOnClickListener(v -> {
            state.pendingDialog = "targets";
            state.dialog.dismiss();
            launchBridge(activity, "app");
        });
        addSite.setOnClickListener(v -> {
            String raw = url.getText().toString().trim();
            if (raw.isEmpty()) {
                LogoToast.makeText(activity, AppString.get(R.string.runtime_text_0131), Toast.LENGTH_SHORT).show();
                return;
            }
            NoteAttachment item = NoteAttachment.url(raw);
            if (item.value.length() < 9) {
                LogoToast.makeText(activity, AppString.get(R.string.runtime_text_0132), Toast.LENGTH_SHORT).show();
                return;
            }
            state.attachments.add(item);
            url.setText("");
            renderAttachmentList(activity, state, list, "targets");
            updateActionButtons(activity, state);
        });
        NoteModalStyler.show(state.dialog);
    }

    private static void renderAttachmentList(Activity activity, State state, LinearLayout list, String filter) {
        list.removeAllViews();
        int count = 0;
        for (NoteAttachment item : new ArrayList<>(state.attachments)) {
            boolean show = NoteAttachment.KIND_FILE.equals(filter)
                    ? NoteAttachment.KIND_FILE.equals(item.kind)
                    : !NoteAttachment.KIND_FILE.equals(item.kind);
            if (!show) continue;
            count++;
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            Button open = new Button(activity);
            open.setAllCaps(false);
            open.setText((NoteAttachment.KIND_URL.equals(item.kind) ? "🌐 " : "") + item.name);
            open.setGravity(Gravity.CENTER_VERTICAL);
            open.setBackgroundResource(R.drawable.bg_field);
            if (NoteAttachment.KIND_APP.equals(item.kind)) {
                try {
                    Drawable icon = activity.getPackageManager().getApplicationIcon(item.value);
                    icon.setBounds(0, 0, dp(activity, 34), dp(activity, 34));
                    open.setCompoundDrawables(icon, null, null, null);
                    open.setCompoundDrawablePadding(dp(activity, 8));
                } catch (Exception ignored) {}
            }
            open.setOnClickListener(v -> item.open(activity));

            Button remove = new Button(activity);
            remove.setText(AppString.get(R.string.runtime_text_0006));
            remove.setAllCaps(false);
            remove.setTextColor(0xFFC44C4C);
            remove.setBackgroundResource(R.drawable.bg_soft_button);
            remove.setOnClickListener(v -> {
                state.attachments.remove(item);
                renderAttachmentList(activity, state, list, filter);
                updateActionButtons(activity, state);
            });

            row.addView(open, new LinearLayout.LayoutParams(0, dp(activity, 58), 1f));
            LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(dp(activity, 72), dp(activity, 58));
            removeParams.setMarginStart(dp(activity, 6));
            row.addView(remove, removeParams);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, 58));
            rowParams.bottomMargin = dp(activity, 6);
            list.addView(row, rowParams);
        }
        if (count == 0) {
            TextView empty = new TextView(activity);
            empty.setGravity(Gravity.CENTER);
            empty.setTextColor(AppSettings.textPrimary(activity));
            empty.setPadding(0, dp(activity, 22), 0, dp(activity, 22));
            empty.setText(NoteAttachment.KIND_FILE.equals(filter)
                    ? AppString.get(R.string.runtime_text_0133)
                    : AppString.get(R.string.runtime_text_0134));
            list.addView(empty);
        }
    }

    private static void importBridgeResults(Activity activity, State state) {
        SharedPreferences prefs = activity.getSharedPreferences(BRIDGE_PREFS, Context.MODE_PRIVATE);
        String raw = prefs.getString(BRIDGE_ATTACHMENTS, "[]");
        String soundUri = prefs.getString(BRIDGE_SOUND_URI, "");
        String soundName = prefs.getString(BRIDGE_SOUND_NAME, "");
        boolean changed = false;
        if (raw != null && !"[]".equals(raw)) {
            state.attachments.addAll(NoteAttachment.parse(raw));
            changed = true;
        }
        if (soundUri != null && !soundUri.isEmpty()) {
            state.soundUri = soundUri;
            state.soundName = soundName;
            changed = true;
        }
        if (changed || prefs.contains(BRIDGE_ATTACHMENTS) || prefs.contains(BRIDGE_SOUND_URI)) {
            prefs.edit()
                    .remove(BRIDGE_ATTACHMENTS)
                    .remove(BRIDGE_SOUND_URI)
                    .remove(BRIDGE_SOUND_NAME)
                    .apply();
            updateActionButtons(activity, state);
            updateSoundButton(activity, state);
        }
    }

    private static void reopenPendingDialog(Activity activity, State state) {
        String pending = state.pendingDialog;
        if (pending == null || pending.isEmpty()) return;
        state.pendingDialog = "";
        if ("files".equals(pending)) showFilesDialog(activity, state);
        else if ("targets".equals(pending)) showTargetsDialog(activity, state);
    }

    private static void overrideSave(Activity activity, State state) {
        Button save = activity.findViewById(R.id.save_quick_note);
        if (save == null) return;
        save.setOnClickListener(v -> saveQuickNote(activity, state));
    }

    private static void saveQuickNote(Activity activity, State state) {
        EditText title = activity.findViewById(R.id.quick_note_title);
        EditText body = activity.findViewById(R.id.quick_note_body);
        Spinner priority = activity.findViewById(R.id.quick_note_priority);
        Switch alarmSwitch = activity.findViewById(R.id.quick_note_alarm_switch);
        SketchView sketch = activity.findViewById(R.id.quick_note_sketch);
        if (title == null || body == null || priority == null || alarmSwitch == null || sketch == null) return;

        String titleText = title.getText().toString().trim();
        String bodyText = body.getText().toString().trim();
        String sketchJson = sketch.serialize();
        if (titleText.isEmpty() && bodyText.isEmpty() && "[]".equals(sketchJson) && state.attachments.isEmpty()) {
            LogoToast.makeText(activity,
                    AppString.get(R.string.runtime_text_0135),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean enabled = alarmSwitch.isChecked();
        Calendar due = (Calendar) readField(activity, "quickNoteDue", Calendar.getInstance());
        long dueAt = enabled ? due.getTimeInMillis() : 0L;
        if (enabled && dueAt <= System.currentTimeMillis()) {
            LogoToast.makeText(activity,
                    AppString.get(R.string.runtime_text_0148),
                    Toast.LENGTH_LONG).show();
            return;
        }

        int recurrence = (Integer) readField(activity, "noteRecurrenceMode", RecurrenceUtils.NONE);
        int interval = (Integer) readField(activity, "noteIntervalDays", 1);
        String dates = (String) readField(activity, "noteCustomDates", "[]");
        long now = System.currentTimeMillis();
        NoForgetItem item = new NoForgetItem(
                now,
                titleText,
                bodyText,
                sketchJson,
                priority.getSelectedItemPosition(),
                enabled,
                dueAt,
                enabled,
                now,
                recurrence,
                interval,
                dates);
        item.attachmentsJson = NoteAttachment.encode(state.attachments);
        item.vibrate = state.vibrate == null || state.vibrate.isChecked();
        item.soundUri = state.soundUri == null ? "" : state.soundUri;
        item.fullscreenUnlocked = state.fullscreenUnlocked == null || state.fullscreenUnlocked.isChecked();
        item.fullscreenLocked = state.fullscreenLocked == null || state.fullscreenLocked.isChecked();
        item.reminderMode = state.reminderMode;
        item.reminderMinutesJson = state.reminderMinutesJson;

        new NoForgetStore(activity).save(item);
        boolean scheduled = true;
        if (enabled) scheduled = NoForgetScheduler.schedule(activity, item);
        else NoForgetScheduler.cancel(activity, item.id);
        NoForgetWidgetProvider.updateAll(activity);

        title.setText("");
        body.setText("");
        priority.setSelection(PriorityUtils.MEDIUM);
        alarmSwitch.setChecked(false);
        sketch.clearSketch();
        state.attachments.clear();
        state.reminderMode = AlarmReminderUtils.MODE_NONE;
        state.reminderMinutesJson = "[]";
        state.soundUri = "";
        state.soundName = "";
        if (state.vibrate != null) state.vibrate.setChecked(true);
        if (state.fullscreenUnlocked != null) state.fullscreenUnlocked.setChecked(true);
        if (state.fullscreenLocked != null) state.fullscreenLocked.setChecked(true);
        writeField(activity, "noteRecurrenceMode", RecurrenceUtils.NONE);
        writeField(activity, "noteIntervalDays", 1);
        writeField(activity, "noteCustomDates", "[]");
        Button repeat = activity.findViewById(R.id.quick_note_repeat);
        if (repeat != null) repeat.setText(AppString.get(R.string.runtime_text_0029));
        due.setTimeInMillis(System.currentTimeMillis());
        due.add(Calendar.HOUR_OF_DAY, 1);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);
        invoke(activity, "updateQuickNoteLabels");
        invoke(activity, "renderNoForget");
        updateReminderButton(state);
        updateSoundButton(activity, state);
        updateActionButtons(activity, state);

        if (!scheduled && Build.VERSION.SDK_INT >= 31 && !PermissionHelper.exactAlarmsGranted(activity)) {
            LogoToast.makeText(activity,
                    AppString.get(R.string.runtime_text_0705),
                    Toast.LENGTH_LONG).show();
            try {
                activity.startActivity(new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + activity.getPackageName())));
            } catch (Exception ignored) {}
        } else {
            LogoToast.makeText(activity, AppString.get(R.string.runtime_text_0136), Toast.LENGTH_SHORT).show();
        }
    }

    private static void updateActionButtons(Activity activity, State state) {
        if (state.sketchButton == null) return;
        SketchView sketch = activity.findViewById(R.id.quick_note_sketch);
        boolean hasSketch = sketch != null && !"[]".equals(sketch.serialize());
        int files = 0;
        int targets = 0;
        for (NoteAttachment item : state.attachments) {
            if (NoteAttachment.KIND_FILE.equals(item.kind)) files++;
            else targets++;
        }
        state.sketchButton.setText(hasSketch ? AppString.get(R.string.runtime_text_0199) : AppString.get(R.string.runtime_text_0118));
        state.filesButton.setText(files == 0 ? AppString.get(R.string.runtime_text_0116)
                : AppString.get(R.string.runtime_text_0467) + CalendarUtils.fa(Integer.toString(files)) + ")");
        state.targetButton.setText(targets == 0 ? AppString.get(R.string.runtime_text_0117)
                : AppString.get(R.string.runtime_text_0468) + CalendarUtils.fa(Integer.toString(targets)) + ")");
    }

    private static void updateReminderButton(State state) {
        if (state.remindersButton == null) return;
        state.remindersButton.setText(AppString.get(R.string.runtime_text_0469)
                + AlarmReminderUtils.summary(state.reminderMode, state.reminderMinutesJson));
    }

    private static void updateSoundButton(Activity activity, State state) {
        if (state.soundButton == null) return;
        String name = state.soundName;
        if (name == null || name.isEmpty()) name = SoundLibrary.name(activity, state.soundUri);
        state.soundButton.setText(AppString.get(R.string.runtime_text_0461) + name);
    }

    private static void launchBridge(Activity activity, String mode) {
        activity.startActivity(new Intent(activity, NoForgetQuickAddActivity.class)
                .putExtra("pickerMode", mode));
    }

    private static Button actionButton(Activity activity, String text) {
        Button button = new Button(activity);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(10.5f);
        button.setTextColor(AppSettings.primaryColor(activity));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        button.setPadding(dp(activity, 2), 0, dp(activity, 2), 0);
        return button;
    }

    private static Button fieldButton(Activity activity, String text) {
        Button button = new Button(activity);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(12.5f);
        button.setTextColor(AppSettings.primaryColor(activity));
        button.setBackgroundResource(R.drawable.bg_field);
        return button;
    }

    private static Button modalButton(Activity activity, String text) {
        Button button = new Button(activity);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(13);
        button.setBackgroundResource(R.drawable.bg_orange_button);
        return button;
    }

    private static Button toolButton(Activity activity, String text) {
        Button button = new Button(activity);
        button.setText(text);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setMinWidth(0);
        button.setPadding(0, 0, 0, 0);
        button.setTextColor(AppSettings.primaryColor(activity));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        return button;
    }

    private static LinearLayout.LayoutParams toolParams(Activity activity) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(activity, 58), dp(activity, 48));
        params.setMarginEnd(dp(activity, 5));
        return params;
    }

    private static LinearLayout switchRow(Activity activity, String label) {
        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_field);
        row.setPadding(dp(activity, 14), 0, dp(activity, 14), 0);
        TextView text = new TextView(activity);
        text.setText(label);
        text.setTextColor(AppSettings.textPrimary(activity));
        row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Switch toggle = new Switch(activity);
        row.addView(toggle);
        return row;
    }

    private static LinearLayout labeledSwitch(Activity activity, String label) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundResource(R.drawable.bg_field);
        TextView text = new TextView(activity);
        text.setText(label);
        text.setGravity(Gravity.CENTER);
        text.setTextSize(11);
        text.setTextColor(AppSettings.textPrimary(activity));
        Switch toggle = new Switch(activity);
        box.addView(text);
        box.addView(toggle);
        return box;
    }

    private static void addWeighted(LinearLayout row, View view, Activity activity) {
        row.addView(view, new LinearLayout.LayoutParams(0, dp(activity, 54), 1f));
    }

    private static void addGap(LinearLayout row, Activity activity) {
        row.addView(new Space(activity), new LinearLayout.LayoutParams(dp(activity, 6), 1));
    }

    private static void addBlock(LinearLayout parent, View view, Activity activity, int heightDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(activity, heightDp));
        params.topMargin = dp(activity, 8);
        parent.addView(view, params);
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }

    private static Object readField(Activity activity, String name, Object fallback) {
        try {
            Field field = MainActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            Object value = field.get(activity);
            return value == null ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static void writeField(Activity activity, String name, Object value) {
        try {
            Field field = MainActivity.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(activity, value);
        } catch (Exception ignored) {}
    }

    private static void invoke(Activity activity, String name) {
        try {
            Method method = MainActivity.class.getDeclaredMethod(name);
            method.setAccessible(true);
            method.invoke(activity);
        } catch (Exception ignored) {}
    }

    private static final class State {
        final ArrayList<NoteAttachment> attachments = new ArrayList<>();
        Button sketchButton;
        Button filesButton;
        Button targetButton;
        Button remindersButton;
        Button soundButton;
        Switch vibrate;
        Switch fullscreenUnlocked;
        Switch fullscreenLocked;
        AlertDialog dialog;
        int reminderMode = AlarmReminderUtils.MODE_NONE;
        String reminderMinutesJson = "[]";
        String soundUri = "";
        String soundName = "";
        String pendingDialog = "";
    }
}
