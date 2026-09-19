package com.ilia.advanceclock;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.icu.util.ULocale;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public final class NoForgetEditorActivity extends Activity {
    private final Calendar due = Calendar.getInstance();
    private long noteId = -1L;
    private long createdAt;
    private EditText title;
    private EditText body;
    private Spinner priority;
    private CheckBox hasDue;
    private CheckBox reminder;
    private Button dateButton;
    private Button timeButton;
    private Button deleteButton;
    private SketchView sketch;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noforget_editor);

        title = findViewById(R.id.note_title);
        body = findViewById(R.id.note_body);
        priority = findViewById(R.id.note_priority);
        hasDue = findViewById(R.id.note_has_due);
        reminder = findViewById(R.id.note_reminder);
        dateButton = findViewById(R.id.note_date);
        timeButton = findViewById(R.id.note_time);
        deleteButton = findViewById(R.id.delete_note);
        sketch = findViewById(R.id.note_sketch);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"کم", "عادی", "زیاد"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(adapter);
        priority.setSelection(NoForgetItem.PRIORITY_NORMAL);

        due.add(Calendar.HOUR_OF_DAY, 1);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);
        createdAt = System.currentTimeMillis();

        noteId = getIntent().getLongExtra("noteId", -1L);
        if (noteId >= 0) loadExisting();
        else deleteButton.setVisibility(View.GONE);

        hasDue.setOnCheckedChangeListener((button, checked) -> updateDueControls());
        dateButton.setOnClickListener(v -> pickDate());
        timeButton.setOnClickListener(v -> pickTime());

        findViewById(R.id.editor_undo).setOnClickListener(v -> sketch.undo());
        findViewById(R.id.editor_redo).setOnClickListener(v -> sketch.redo());
        findViewById(R.id.clear_sketch).setOnClickListener(v -> sketch.clearSketch());

        findViewById(R.id.editor_pen_teal).setOnClickListener(v -> sketch.setPenColor(0xFF087C77));
        findViewById(R.id.editor_pen_orange).setOnClickListener(v -> sketch.setPenColor(0xFFF17600));
        findViewById(R.id.editor_pen_black).setOnClickListener(v -> sketch.setPenColor(0xFF1B2423));
        findViewById(R.id.editor_pen_blue).setOnClickListener(v -> sketch.setPenColor(0xFF2457D6));

        findViewById(R.id.editor_pen_thin).setOnClickListener(v -> sketch.setPenWidthDp(2f));
        findViewById(R.id.editor_pen_medium).setOnClickListener(v -> sketch.setPenWidthDp(4f));
        findViewById(R.id.editor_pen_thick).setOnClickListener(v -> sketch.setPenWidthDp(7f));

        findViewById(R.id.save_note).setOnClickListener(v -> save());
        deleteButton.setOnClickListener(v -> delete());
        findViewById(R.id.cancel_note).setOnClickListener(v -> finish());

        updateDueButtons();
        updateDueControls();
    }

    private void loadExisting() {
        NoForgetItem item = new NoForgetStore(this).find(noteId);
        if (item == null) {
            finish();
            return;
        }
        title.setText(item.title);
        body.setText(item.body);
        priority.setSelection(Math.max(0, Math.min(2, item.priority)));
        hasDue.setChecked(item.hasDue);
        reminder.setChecked(item.reminderEnabled);
        createdAt = item.createdAt;
        if (item.hasDue && item.dueAtMillis > 0) due.setTimeInMillis(item.dueAtMillis);
        sketch.load(item.sketchJson);
    }

    private void updateDueControls() {
        boolean enabled = hasDue.isChecked();
        dateButton.setEnabled(enabled);
        timeButton.setEnabled(enabled);
        reminder.setEnabled(enabled);
        dateButton.setAlpha(enabled ? 1f : 0.45f);
        timeButton.setAlpha(enabled ? 1f : 0.45f);
        reminder.setAlpha(enabled ? 1f : 0.45f);
        if (!enabled) reminder.setChecked(false);
    }

    private void pickDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            due.set(Calendar.YEAR, year);
            due.set(Calendar.MONTH, month);
            due.set(Calendar.DAY_OF_MONTH, day);
            updateDueButtons();
        }, due.get(Calendar.YEAR), due.get(Calendar.MONTH), due.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            due.set(Calendar.HOUR_OF_DAY, hour);
            due.set(Calendar.MINUTE, minute);
            due.set(Calendar.SECOND, 0);
            due.set(Calendar.MILLISECOND, 0);
            updateDueButtons();
        }, due.get(Calendar.HOUR_OF_DAY), due.get(Calendar.MINUTE), true).show();
    }

    private void updateDueButtons() {
        android.icu.util.Calendar pc = newPersianCalendar();
        pc.setTimeInMillis(due.getTimeInMillis());
        String[] months = {
                "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        };
        dateButton.setText(
                fa(pc.get(android.icu.util.Calendar.DAY_OF_MONTH))
                        + " " + months[pc.get(android.icu.util.Calendar.MONTH)]
                        + " " + fa(pc.get(android.icu.util.Calendar.YEAR))
        );
        timeButton.setText(fa(new SimpleDateFormat("HH:mm", Locale.US).format(due.getTime())));
    }

    private void save() {
        String titleText = title.getText().toString().trim();
        String bodyText = body.getText().toString().trim();
        String sketchJson = sketch.serialize();

        if (titleText.isEmpty() && bodyText.isEmpty() && "[]".equals(sketchJson)) {
            Toast.makeText(this, "یک متن یا نقاشی وارد کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean dueEnabled = hasDue.isChecked();
        boolean reminderEnabled = dueEnabled && reminder.isChecked();
        long dueAt = dueEnabled ? due.getTimeInMillis() : 0L;

        if (reminderEnabled && dueAt <= System.currentTimeMillis()) {
            Toast.makeText(this, "زمان یادآوری باید در آینده باشد", Toast.LENGTH_LONG).show();
            return;
        }

        long id = noteId >= 0 ? noteId : System.currentTimeMillis();
        NoForgetItem item = new NoForgetItem(
                id,
                titleText,
                bodyText,
                sketchJson,
                priority.getSelectedItemPosition(),
                dueEnabled,
                dueAt,
                reminderEnabled,
                createdAt
        );

        new NoForgetStore(this).save(item);
        boolean scheduled = !reminderEnabled || NoForgetScheduler.schedule(this, item);
        NoForgetWidgetProvider.updateAll(this);

        if (!scheduled) {
            Toast.makeText(this, "یادداشت ذخیره شد؛ دسترسی آلارم دقیق برای یادآوری لازم است.", Toast.LENGTH_LONG).show();
        }
        finish();
    }

    private void delete() {
        NoForgetScheduler.cancel(this, noteId);
        new NoForgetStore(this).delete(noteId);
        NoForgetWidgetProvider.updateAll(this);
        finish();
    }

    private static android.icu.util.Calendar newPersianCalendar() {
        return android.icu.util.Calendar.getInstance(
                new ULocale("fa_IR@calendar=persian")
        );
    }

    private static String fa(int value) {
        return fa(String.valueOf(value));
    }

    private static String fa(String value) {
        char[] en = {'0','1','2','3','4','5','6','7','8','9'};
        char[] pe = {'۰','۱','۲','۳','۴','۵','۶','۷','۸','۹'};
        String out = value;
        for (int i = 0; i < en.length; i++) out = out.replace(en[i], pe[i]);
        return out;
    }
}
