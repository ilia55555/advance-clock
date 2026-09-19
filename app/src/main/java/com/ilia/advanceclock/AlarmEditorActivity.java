package com.ilia.advanceclock;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.ImageView;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public final class AlarmEditorActivity extends Activity {
    private static final int REQ_IMAGE_1 = 410;
    private static final int REQ_IMAGE_2 = 411;
    private final Calendar selected = Calendar.getInstance();

    private long alarmId = -1L;
    private EditText label;
    private Button dateButton;
    private Button timeButton;
    private Button repeatButton;
    private Button remindersButton;
    private Spinner priority;
    private Button deleteButton;
    private Switch vibrate;
    private Button image1Button;
    private Button image2Button;
    private ImageView image1Preview;
    private ImageView image2Preview;
    private View imagePreviewRow;
    private View imagePreviewSpace;

    private int dateCalendarType;
    private int recurrenceMode = RecurrenceUtils.NONE;
    private int intervalDays = 1;
    private String customDatesJson = "[]";
    private int reminderMode = AlarmReminderUtils.MODE_NONE;
    private String reminderMinutesJson = "[]";
    private int snoozeMinutes = 15;
    private long lastFiredAtMillis = 0L;
    private String imageUri1 = "";
    private String imageUri2 = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        if (getIntent().getBooleanExtra("modalCreate", false)) {
            AppSettings.applyModalOverlay(this);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_editor);

        label = findViewById(R.id.alarm_label);
        dateButton = findViewById(R.id.pick_date);
        timeButton = findViewById(R.id.pick_time);
        repeatButton = findViewById(R.id.alarm_repeat);
        remindersButton = findViewById(R.id.alarm_reminders);
        priority = findViewById(R.id.alarm_priority);
        deleteButton = findViewById(R.id.delete_alarm);
        vibrate = findViewById(R.id.alarm_vibrate);
        image1Button = findViewById(R.id.alarm_image1_button);
        image2Button = findViewById(R.id.alarm_image2_button);
        image1Preview = findViewById(R.id.alarm_image1_preview);
        image2Preview = findViewById(R.id.alarm_image2_preview);
        imagePreviewRow = findViewById(R.id.alarm_image_preview_row);
        imagePreviewSpace = findViewById(R.id.alarm_image_preview_space);
        dateCalendarType = AppSettings.defaultCalendar(this);

        String[] labels = PriorityUtils.labels();
        String[] priorityValues = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            priorityValues[i] = "اهمیت " + labels[i];
        }

        ArrayAdapter<String> pr = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                priorityValues);
        pr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(pr);
        priority.setSelection(PriorityUtils.MEDIUM);

        alarmId = getIntent().getLongExtra("alarmId", -1L);
        if (alarmId >= 0) {
            loadExisting();
        } else {
            selected.add(Calendar.MINUTE, 1);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            deleteButton.setVisibility(View.GONE);
            updateButtons();
            updateReminderButton();
        }

        dateButton.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this,
                selected.getTimeInMillis(),
                dateCalendarType,
                (picked, type) -> {
                    dateCalendarType = type;
                    Calendar source = Calendar.getInstance();
                    source.setTimeInMillis(picked);
                    selected.set(Calendar.YEAR, source.get(Calendar.YEAR));
                    selected.set(Calendar.MONTH, source.get(Calendar.MONTH));
                    selected.set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH));
                    updateButtons();
                }));

        timeButton.setOnClickListener(v -> new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    selected.set(Calendar.HOUR_OF_DAY, hour);
                    selected.set(Calendar.MINUTE, minute);
                    selected.set(Calendar.SECOND, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    updateButtons();
                },
                selected.get(Calendar.HOUR_OF_DAY),
                selected.get(Calendar.MINUTE),
                true).show());

        repeatButton.setOnClickListener(v -> RecurrenceDialog.show(
                this,
                selected.getTimeInMillis(),
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

        remindersButton.setOnClickListener(v -> AlarmReminderDialog.show(
                this,
                reminderMode,
                reminderMinutesJson,
                (mode, json) -> {
                    reminderMode = mode;
                    reminderMinutesJson = json;
                    updateReminderButton();
                }));

        image1Button.setOnClickListener(v -> pickImage(REQ_IMAGE_1));
        image2Button.setOnClickListener(v -> pickImage(REQ_IMAGE_2));

        findViewById(R.id.save_alarm).setOnClickListener(v -> save());
        deleteButton.setOnClickListener(v -> delete());
        findViewById(R.id.cancel).setOnClickListener(v -> finish());
    }

    private void loadExisting() {
        AlarmItem item = new AlarmStore(this).find(alarmId);
        if (item == null) {
            finish();
            return;
        }

        label.setText(item.label);
        selected.setTimeInMillis(item.triggerAtMillis);
        priority.setSelection(item.priority);
        vibrate.setChecked(item.vibrate);

        recurrenceMode = item.recurrenceMode;
        intervalDays = item.intervalDays;
        customDatesJson = item.customDatesJson;
        repeatButton.setText(
                RecurrenceUtils.summary(
                        recurrenceMode,
                        intervalDays,
                        customDatesJson));

        reminderMode = item.reminderMode;
        reminderMinutesJson = item.reminderMinutesJson;
        snoozeMinutes = item.snoozeMinutes;
        lastFiredAtMillis = item.lastFiredAtMillis;
        imageUri1 = item.imageUri1;
        imageUri2 = item.imageUri2;
        updateReminderButton();
        updateButtons();
        updateImagePreviews();
    }

    private void updateReminderButton() {
        remindersButton.setText(
                "یادآوری\n"
                        + AlarmReminderUtils.summary(
                        reminderMode,
                        reminderMinutesJson));
    }

    private void updateButtons() {
        dateButton.setText(
                CalendarUtils.formatDate(
                        selected.getTimeInMillis(),
                        dateCalendarType));

        String time = String.format(
                Locale.US,
                "%02d:%02d",
                selected.get(Calendar.HOUR_OF_DAY),
                selected.get(Calendar.MINUTE));
        timeButton.setText(CalendarUtils.fa(time));
    }

    private void save() {
        long trigger = selected.getTimeInMillis();

        if (trigger <= System.currentTimeMillis()) {
            Toast.makeText(
                    this,
                    "هشدار را نمی‌توان برای تاریخ یا ساعت گذشته تنظیم کرد",
                    Toast.LENGTH_LONG).show();
            return;
        }

        long id = alarmId >= 0 ? alarmId : System.currentTimeMillis();
        int compat = recurrenceMode <= RecurrenceUtils.YEARLY
                ? recurrenceMode
                : AlarmItem.REPEAT_NONE;

        AlarmItem item = new AlarmItem(
                id,
                label.getText().toString().trim(),
                trigger,
                compat,
                true,
                vibrate.isChecked(),
                priority.getSelectedItemPosition(),
                recurrenceMode,
                intervalDays,
                customDatesJson,
                snoozeMinutes,
                reminderMode,
                reminderMinutesJson);
        item.lastFiredAtMillis = lastFiredAtMillis;
        item.imageUri1 = imageUri1;
        item.imageUri2 = imageUri2;

        new AlarmStore(this).save(item);
        boolean scheduled = AlarmScheduler.schedule(this, item);
        if (item.lastFiredAtMillis > 0L) {
            AlarmScheduler.schedulePostReminders(this, item, item.lastFiredAtMillis);
        }
        ClockWidgetProvider.updateAll(this);

        if (!scheduled
                && !PermissionHelper.exactAlarmsGranted(this)
                && Build.VERSION.SDK_INT >= 31) {
            Toast.makeText(
                    this,
                    "هشدار ذخیره شد؛ دسترسی آلارم دقیق را فعال کنید.",
                    Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {}
        } else {
            Toast.makeText(this, "هشدار ذخیره شد", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ((requestCode == REQ_IMAGE_1 || requestCode == REQ_IMAGE_2)
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}

            if (requestCode == REQ_IMAGE_1) imageUri1 = uri.toString();
            else imageUri2 = uri.toString();
            updateImagePreviews();
        }
    }

    private void pickImage(int requestCode) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, requestCode);
    }

    private void updateImagePreviews() {
        boolean has1 = imageUri1 != null && !imageUri1.isEmpty();
        boolean has2 = imageUri2 != null && !imageUri2.isEmpty();

        imagePreviewRow.setVisibility(has1 || has2 ? View.VISIBLE : View.GONE);
        image1Preview.setVisibility(has1 ? View.VISIBLE : View.GONE);
        image2Preview.setVisibility(has2 ? View.VISIBLE : View.GONE);
        imagePreviewSpace.setVisibility(has1 && has2 ? View.VISIBLE : View.GONE);

        image1Button.setText(has1 ? "تغییر عکس ۱ ✓" : "افزودن عکس ۱");
        image2Button.setText(has2 ? "تغییر عکس ۲ ✓" : "افزودن عکس ۲");

        if (has1) {
            try { image1Preview.setImageURI(Uri.parse(imageUri1)); } catch (Exception ignored) {}
        } else {
            image1Preview.setImageDrawable(null);
        }

        if (has2) {
            try { image2Preview.setImageURI(Uri.parse(imageUri2)); } catch (Exception ignored) {}
        } else {
            image2Preview.setImageDrawable(null);
        }
    }

    private void delete() {
        AlarmScheduler.cancel(this, alarmId);
        new AlarmStore(this).delete(alarmId);
        ClockWidgetProvider.updateAll(this);
        finish();
    }
}
