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
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Locale;

public final class AlarmEditorActivity extends Activity {
    private static volatile long editingAlarmId = Long.MIN_VALUE;
    static void protect(long id) { editingAlarmId = id; }
    static boolean isEditing(long id) { return editingAlarmId == id; }
    private static final int REQ_IMAGE_1 = 410;
    private static final int REQ_IMAGE_2 = 411;
    private static final int REQ_SOUND = 412;
    private final Calendar selected = Calendar.getInstance(AppTime.zone());

    private long alarmId = -1L;
    private EditText label;
    private Button dateButton;
    private DateOption dateOption;
    private Button timeButton;
    private Button repeatButton;
    private Button remindersButton;
    private Spinner priority;
    private boolean worldReference,createdFromWorld;
    private int storedPriority=PriorityUtils.MEDIUM;
    private java.util.List<String> referenceZones=java.util.Collections.emptyList();
    private String referenceZone="";
    private Button deleteButton;
    private Switch vibrate;
    private Switch autoDelete;
    private Button image1Button;
    private Button image2Button;
    private ImageView image1Preview;
    private ImageView image2Preview;
    private View imagePreviewRow;
    private View imagePreviewSpace;
    private TextView soundValue;

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
    private String soundUri = "";
    private boolean createMode;
    private String attachmentKey;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        createMode = getIntent().getBooleanExtra("modalCreate", false);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_editor);
        applySystemBarInsets(findViewById(R.id.alarm_editor_root));

        label = findViewById(R.id.alarm_label);
        dateButton = findViewById(R.id.pick_date);
        dateOption = new DateOption(this, dateButton, R.id.date_option_alarm_editor);
        autoDelete = dateOption.addAutoDelete(R.id.auto_delete_alarm_editor);
        timeButton = findViewById(R.id.pick_time);
        repeatButton = findViewById(R.id.alarm_repeat);
        remindersButton = findViewById(R.id.alarm_reminders);
        priority = findViewById(R.id.alarm_priority);
        deleteButton = findViewById(R.id.delete_alarm);
        ButtonStyles.destructive(deleteButton);
        vibrate = findViewById(R.id.alarm_vibrate);
        image1Button = findViewById(R.id.alarm_image1_button);
        image2Button = findViewById(R.id.alarm_image2_button);
        image1Preview = findViewById(R.id.alarm_image1_preview);
        image2Preview = findViewById(R.id.alarm_image2_preview);
        imagePreviewRow = findViewById(R.id.alarm_image_preview_row);
        imagePreviewSpace = findViewById(R.id.alarm_image_preview_space);
        soundValue = findViewById(R.id.alarm_sound_value);
        dateCalendarType = AppSettings.defaultCalendar(this);

        String[] priorityValues = PriorityUtils.displayLabels();

        ArrayAdapter<String> pr = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                priorityValues);
        pr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(pr);
        priority.setSelection(PriorityUtils.MEDIUM);

        alarmId = getIntent().getLongExtra("alarmId", -1L);
        configureReference(savedInstanceState);
        if (alarmId >= 0L) protect(alarmId);
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

        if (savedInstanceState != null && savedInstanceState.containsKey("selectedTime")) {
            selected.setTimeInMillis(savedInstanceState.getLong("selectedTime"));
            updateButtons();
        }
        dateButton.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this,
                pickerDateMillis(),
                dateCalendarType,
                (picked, type) -> {
                    dateCalendarType = type;
                    Calendar source = Calendar.getInstance(AppTime.zone());
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
                customDatesJson,selected.getTimeZone(),
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

        attachmentKey = savedInstanceState == null ? null : savedInstanceState.getString("attachmentKey");
        if (attachmentKey == null || attachmentKey.isEmpty()) {
            attachmentKey = "alarm_editor_" + java.util.UUID.randomUUID();
            AlarmItem existing = alarmId >= 0 ? new AlarmStore(this).find(alarmId) : null;
            if (existing == null) existing = new AlarmItem(-1, "", 0, 0, true);
            AlarmAttachments.write(this, attachmentKey, existing);
        }
        AlarmAttachments.bindButton(this, image1Button, attachmentKey);
        imagePreviewRow.setVisibility(View.GONE);
        findViewById(R.id.alarm_sound_button).setOnClickListener(v ->
                startActivityForResult(new Intent(this, SoundPickerActivity.class), REQ_SOUND));

        findViewById(R.id.save_alarm).setOnClickListener(v -> save());
        deleteButton.setOnClickListener(v -> delete());
        findViewById(R.id.cancel).setOnClickListener(v -> finish());
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putLong("selectedTime", selected.getTimeInMillis());
        out.putString("referenceZone",referenceZone);
        out.putString("attachmentKey", attachmentKey); super.onSaveInstanceState(out);
    }

    @Override protected void onDestroy() {
        if (!isChangingConfigurations() && editingAlarmId == alarmId) {
            editingAlarmId = Long.MIN_VALUE;
            if (new AlarmStore(this).deleteExpired(System.currentTimeMillis()) > 0)
                ClockWidgetProvider.updateAll(this);
        }
        super.onDestroy();
    }

    private void applySystemBarInsets(View root) {
        EditorHeaderInsets.apply(getWindow(),root,findViewById(R.id.alarm_editor_header));
    }

    @Override public void finish() {
        super.finish();
        if (createMode) {
            overridePendingTransition(R.anim.editor_stay, R.anim.editor_exit);
        }
    }

    private void loadExisting() {
        AlarmItem item = new AlarmStore(this).find(alarmId);
        if (item == null) {
            finish();
            return;
        }

        dateOption.setApplied(item.applyDate);
        autoDelete.setChecked(item.autoDeleteAfterCompletion);
        label.setText(item.label);
        selected.setTimeInMillis(item.triggerAtMillis);
        storedPriority=item.priority;
        if(!worldReference)priority.setSelection(item.priority);
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
        soundUri = item.soundUri;
        soundValue.setText(SoundLibrary.name(this, soundUri));
        updateReminderButton();
        updateButtons();
        updateImagePreviews();
    }

    private void configureReference(Bundle state){
        AlarmItem existing=alarmId>=0?new AlarmStore(this).find(alarmId):null;
        createdFromWorld=existing==null?getIntent().getBooleanExtra("worldTimeReference",false):existing.createdFromWorld;
        worldReference=getIntent().getBooleanExtra("worldTimeReference",false)
                || (existing!=null&&!existing.referenceZoneId.isEmpty());
        if(!worldReference)return;
        referenceZones=WorldClockStore.zones(this);
        String preferred=state!=null?state.getString("referenceZone",""):existing!=null?existing.referenceZoneId:"";
        int index=Math.max(0,referenceZones.indexOf(preferred));
        if(referenceZones.isEmpty()){findViewById(R.id.save_alarm).setEnabled(false);return;}
        referenceZone=referenceZones.get(index);selected.setTimeZone(java.util.TimeZone.getTimeZone(referenceZone));
        priority.setAdapter(referenceAdapter());priority.setSelection(index);
        priority.setPrompt(AppString.get(R.string.world_alarm_reference));
        priority.setContentDescription(AppString.get(R.string.world_alarm_reference));
        priority.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> p,View view,int position,long id){
                String zone=referenceZones.get(position);
                if(zone.equals(referenceZone))return;
                WorldAlarmReference.changeZone(selected,zone);referenceZone=zone;updateButtons();
            }
            public void onNothingSelected(android.widget.AdapterView<?> p){}
        });
    }
    private ArrayAdapter<String> referenceAdapter(){
        ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,
                new java.util.ArrayList<>(java.util.Arrays.asList(WorldAlarmReference.labels(this,referenceZones,selected.getTimeInMillis()))));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);return adapter;
    }
    private long pickerDateMillis(){
        return WorldAlarmReference.pickerDate(selected.getTimeInMillis(),selected.getTimeZone());
    }

    private void updateReminderButton() {
        remindersButton.setText(
                AlarmReminderUtils.summary(
                        reminderMode,
                        reminderMinutesJson));
    }

    private void updateButtons() {
        if(worldReference&&!referenceZones.isEmpty()){
            @SuppressWarnings("unchecked") ArrayAdapter<String> adapter=(ArrayAdapter<String>)priority.getAdapter();
            adapter.setNotifyOnChange(false);adapter.clear();
            adapter.addAll(new java.util.ArrayList<>(java.util.Arrays.asList(WorldAlarmReference.labels(this,referenceZones,selected.getTimeInMillis()))));adapter.notifyDataSetChanged();
        }
        dateButton.setText(
                CalendarUtils.formatDate(
                        selected.getTimeInMillis(),
                        dateCalendarType,selected.getTimeZone()));

        String time = String.format(
                Locale.US,
                "%02d:%02d",
                selected.get(Calendar.HOUR_OF_DAY),
                selected.get(Calendar.MINUTE));
        timeButton.setText(CalendarUtils.fa(time));
    }

    private void save() {
        long trigger = AlarmTimePolicy.next(selected.getTimeInMillis(), dateOption.isApplied(),
                recurrenceMode, intervalDays, customDatesJson, System.currentTimeMillis(),selected.getTimeZone());

        if (trigger <= System.currentTimeMillis()) {
            LogoToast.makeText(
                    this,
                    AppString.get(R.string.runtime_text_0147),
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
                worldReference?storedPriority:priority.getSelectedItemPosition(),
                recurrenceMode,
                intervalDays,
                customDatesJson,
                snoozeMinutes,
                reminderMode,
                reminderMinutesJson);
        item.referenceZoneId=worldReference?referenceZone:"";
        item.createdFromWorld=createdFromWorld;
        item.applyDate = dateOption.isApplied();
        item.autoDeleteAfterCompletion = autoDelete.isChecked();
        AlarmItem previous = alarmId >= 0 ? new AlarmStore(this).find(alarmId) : null;
        if (previous != null) item.pendingSnoozeAtMillis = previous.pendingSnoozeAtMillis;
        item.lastFiredAtMillis = lastFiredAtMillis;
        item.imageUri1 = imageUri1;
        item.imageUri2 = imageUri2;
        AlarmAttachments.read(this, attachmentKey, item);
        item.soundUri = soundUri;

        new AlarmStore(this).save(item);
        boolean scheduled = AlarmScheduler.schedule(this, item);
        if (item.lastFiredAtMillis > 0L) {
            AlarmScheduler.schedulePostReminders(this, item, item.lastFiredAtMillis);
        }
        ClockWidgetProvider.updateAll(this);

        if (!scheduled
                && !PermissionHelper.exactAlarmsGranted(this)
                && Build.VERSION.SDK_INT >= 31) {
            LogoToast.makeText(
                    this,
                    AppString.get(R.string.runtime_text_0423),
                    Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {}
        } else {
            LogoToast.makeText(this, AppString.get(R.string.runtime_text_0145), Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    @Override protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_SOUND && resultCode == RESULT_OK && data != null) {
            soundUri = data.getStringExtra(SoundPickerActivity.EXTRA_URI);
            soundValue.setText(data.getStringExtra(SoundPickerActivity.EXTRA_NAME));
            return;
        }

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

        image1Button.setText(has1 ? AppString.get(R.string.runtime_text_0588) : AppString.get(R.string.runtime_text_0141));
        image2Button.setText(has2 ? AppString.get(R.string.runtime_text_0589) : AppString.get(R.string.runtime_text_0142));

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
