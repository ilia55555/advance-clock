package com.ilia.advanceclock;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public final class NoForgetEditorActivity extends Activity {
    private final Calendar due = Calendar.getInstance();

    private long noteId = -1L;
    private long createdAt;

    private EditText title;
    private EditText body;
    private Spinner priority;
    private Switch alarmEnabled;
    private View alarmControls;
    private Button dateButton;
    private Button timeButton;
    private Button repeatButton;
    private Button deleteButton;
    private SketchView sketch;
    private Spinner penSize;
    private ImageButton gridToggle;

    private int dateCalendarType;
    private int recurrenceMode = RecurrenceUtils.NONE;
    private int intervalDays = 1;
    private String customDatesJson = "[]";
    private boolean createMode;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        createMode = getIntent().getBooleanExtra("modalCreate", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noforget_editor);
        applySystemBarInsets(findViewById(R.id.note_editor_root));

        title = findViewById(R.id.note_title);
        body = findViewById(R.id.note_body);
        priority = findViewById(R.id.note_priority);
        alarmEnabled = findViewById(R.id.note_alarm_enabled);
        alarmControls = findViewById(R.id.note_alarm_controls);
        dateButton = findViewById(R.id.note_date);
        timeButton = findViewById(R.id.note_time);
        repeatButton = findViewById(R.id.note_repeat);
        deleteButton = findViewById(R.id.delete_note);
        sketch = findViewById(R.id.note_sketch);
        penSize = findViewById(R.id.editor_pen_size);
        gridToggle = findViewById(R.id.editor_grid_toggle);
        dateCalendarType = AppSettings.defaultCalendar(this);

        String[] labels = PriorityUtils.labels();
        String[] priorityValues = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            priorityValues[i] = "اهمیت " + labels[i];
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                priorityValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(adapter);
        priority.setSelection(PriorityUtils.MEDIUM);

        due.add(Calendar.HOUR_OF_DAY, 1);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);
        createdAt = System.currentTimeMillis();

        noteId = getIntent().getLongExtra("noteId", -1L);
        if (noteId >= 0) {
            loadExisting();
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        alarmEnabled.setOnCheckedChangeListener((button, checked) ->
                alarmControls.setVisibility(checked ? View.VISIBLE : View.GONE));

        dateButton.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this,
                due.getTimeInMillis(),
                dateCalendarType,
                (picked, type) -> {
                    dateCalendarType = type;
                    Calendar source = Calendar.getInstance();
                    source.setTimeInMillis(picked);
                    due.set(Calendar.YEAR, source.get(Calendar.YEAR));
                    due.set(Calendar.MONTH, source.get(Calendar.MONTH));
                    due.set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH));
                    updateDueButtons();
                }));

        timeButton.setOnClickListener(v -> new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    due.set(Calendar.HOUR_OF_DAY, hour);
                    due.set(Calendar.MINUTE, minute);
                    due.set(Calendar.SECOND, 0);
                    due.set(Calendar.MILLISECOND, 0);
                    updateDueButtons();
                },
                due.get(Calendar.HOUR_OF_DAY),
                due.get(Calendar.MINUTE),
                true).show());

        repeatButton.setOnClickListener(v -> RecurrenceDialog.show(
                this,
                due.getTimeInMillis(),
                recurrenceMode,
                intervalDays,
                customDatesJson,
                (mode, interval, dates) -> {
                    recurrenceMode = mode;
                    intervalDays = interval;
                    customDatesJson = dates;
                    repeatButton.setText(
                            RecurrenceUtils.summary(mode, interval, dates));
                }));

        findViewById(R.id.editor_undo).setOnClickListener(v -> sketch.undo());
        findViewById(R.id.clear_sketch).setOnClickListener(v -> sketch.clearSketch());
        findViewById(R.id.editor_redo).setOnClickListener(v -> sketch.redo());
        findViewById(R.id.editor_palette).setOnClickListener(v ->
                PaletteDialog.show(this, sketch.getPenColor(), sketch::setPenColor));

        penSize.setAdapter(new PenSizeAdapter(this));
        penSize.setSelection(1);
        penSize.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id) {
                        float[] widths = {2f, 4f, 7f, 10f};
                        sketch.setPenWidthDp(
                                widths[Math.max(0, Math.min(widths.length - 1, position))]);
                    }
                    @Override public void onNothingSelected(
                            android.widget.AdapterView<?> parent) {}
                });

        final boolean[] gridVisible = {true};
        sketch.setGridVisible(true);
        gridToggle.setImageResource(R.drawable.ic_grid);
        gridToggle.setSelected(false);
        gridToggle.setOnClickListener(v -> {
            gridVisible[0] = !gridVisible[0];
            sketch.setGridVisible(gridVisible[0]);
            gridToggle.setSelected(!gridVisible[0]);
            gridToggle.setImageResource(
                    gridVisible[0] ? R.drawable.ic_grid : R.drawable.ic_grid_off);
        });

        findViewById(R.id.save_note).setOnClickListener(v -> save());
        deleteButton.setOnClickListener(v -> delete());
        findViewById(R.id.cancel_note).setOnClickListener(v -> finish());

        updateDueButtons();
        alarmControls.setVisibility(
                alarmEnabled.isChecked() ? View.VISIBLE : View.GONE);
    }

    private void applySystemBarInsets(View root) {
        int start = root.getPaddingStart();
        int top = root.getPaddingTop();
        int end = root.getPaddingEnd();
        int bottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPaddingRelative(
                    start,
                    top + insets.getSystemWindowInsetTop(),
                    end,
                    bottom + insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
    }

    @Override public void finish() {
        super.finish();
        if (createMode) {
            overridePendingTransition(R.anim.editor_stay, R.anim.editor_exit);
        }
    }

    private void loadExisting() {
        NoForgetItem item = new NoForgetStore(this).find(noteId);
        if (item == null) {
            finish();
            return;
        }

        title.setText(item.title);
        body.setText(item.body);
        priority.setSelection(PriorityUtils.clamp(item.priority));
        alarmEnabled.setChecked(item.reminderEnabled && item.hasDue);
        createdAt = item.createdAt;

        if (item.hasDue && item.dueAtMillis > 0) {
            due.setTimeInMillis(item.dueAtMillis);
        }

        recurrenceMode = item.recurrenceMode;
        intervalDays = item.intervalDays;
        customDatesJson = item.customDatesJson;
        repeatButton.setText(
                RecurrenceUtils.summary(
                        recurrenceMode,
                        intervalDays,
                        customDatesJson));
        sketch.load(item.sketchJson);
    }

    private void updateDueButtons() {
        dateButton.setText(
                CalendarUtils.formatDate(
                        due.getTimeInMillis(),
                        dateCalendarType));

        String time = String.format(
                Locale.US,
                "%02d:%02d",
                due.get(Calendar.HOUR_OF_DAY),
                due.get(Calendar.MINUTE));
        timeButton.setText(CalendarUtils.fa(time));
    }

    private void save() {
        String titleText = title.getText().toString().trim();
        String bodyText = body.getText().toString().trim();
        String sketchJson = sketch.serialize();

        if (titleText.isEmpty()
                && bodyText.isEmpty()
                && "[]".equals(sketchJson)) {
            Toast.makeText(
                    this,
                    "یک متن یا نقاشی وارد کنید",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        boolean enabled = alarmEnabled.isChecked();
        long dueAt = enabled ? due.getTimeInMillis() : 0L;

        if (enabled && dueAt <= System.currentTimeMillis()) {
            Toast.makeText(
                    this,
                    "آلارم یادداشت را نمی‌توان برای گذشته تنظیم کرد",
                    Toast.LENGTH_LONG).show();
            return;
        }

        long id = noteId >= 0 ? noteId : System.currentTimeMillis();
        NoForgetItem item = new NoForgetItem(
                id,
                titleText,
                bodyText,
                sketchJson,
                priority.getSelectedItemPosition(),
                enabled,
                dueAt,
                enabled,
                createdAt,
                recurrenceMode,
                intervalDays,
                customDatesJson);

        new NoForgetStore(this).save(item);

        boolean scheduled = !enabled || NoForgetScheduler.schedule(this, item);
        NoForgetWidgetProvider.updateAll(this);

        if (!scheduled) {
            Toast.makeText(
                    this,
                    "یادداشت ذخیره شد؛ برای آلارم، دسترسی آلارم دقیق لازم است.",
                    Toast.LENGTH_LONG).show();
        }
        finish();
    }

    private void delete() {
        NoForgetScheduler.cancel(this, noteId);
        new NoForgetStore(this).delete(noteId);
        NoForgetWidgetProvider.updateAll(this);
        finish();
    }
}
