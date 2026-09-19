package com.ilia.advanceclock;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.icu.util.PersianCalendar;
import android.net.Uri;
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
    private final Calendar selected = Calendar.getInstance();
    private long alarmId = -1L;
    private EditText label;
    private Button dateButton;
    private Button timeButton;
    private Spinner repeat;
    private Button deleteButton;
    private Switch vibrate;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_editor);

        label = findViewById(R.id.alarm_label);
        dateButton = findViewById(R.id.pick_date);
        timeButton = findViewById(R.id.pick_time);
        repeat = findViewById(R.id.repeat_spinner);
        deleteButton = findViewById(R.id.delete_alarm);
        vibrate = findViewById(R.id.alarm_vibrate);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"بدون تکرار", "هر روز", "هر هفته", "هر ماه", "هر سال"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repeat.setAdapter(adapter);

        alarmId = getIntent().getLongExtra("alarmId", -1L);
        if (alarmId >= 0) {
            loadExisting();
        } else {
            selected.add(Calendar.MINUTE, 1);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            deleteButton.setVisibility(View.GONE);
            updateButtons();
        }

        dateButton.setOnClickListener(v -> chooseDate());
        timeButton.setOnClickListener(v -> chooseTime());
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
        repeat.setSelection(Math.max(0, Math.min(4, item.repeatType)));
        vibrate.setChecked(item.vibrate);
        updateButtons();
    }

    private void chooseDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, day);
            updateButtons();
        }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void chooseTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            selected.set(Calendar.HOUR_OF_DAY, hour);
            selected.set(Calendar.MINUTE, minute);
            selected.set(Calendar.SECOND, 0);
            selected.set(Calendar.MILLISECOND, 0);
            updateButtons();
        }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), true).show();
    }

    private void updateButtons() {
        PersianCalendar pc = new PersianCalendar();
        pc.setTimeInMillis(selected.getTimeInMillis());
        String[] months = {
                "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        };
        dateButton.setText(
                fa(pc.get(android.icu.util.Calendar.DAY_OF_MONTH))
                        + " " + months[pc.get(android.icu.util.Calendar.MONTH)]
                        + " " + fa(pc.get(android.icu.util.Calendar.YEAR))
        );
        String time = String.format(Locale.US, "%02d:%02d",
                selected.get(Calendar.HOUR_OF_DAY),
                selected.get(Calendar.MINUTE));
        timeButton.setText(fa(time));
    }

    private void save() {
        int repeatType = repeat.getSelectedItemPosition();
        long trigger = selected.getTimeInMillis();

        if (trigger <= System.currentTimeMillis() && repeatType == AlarmItem.REPEAT_NONE) {
            Toast.makeText(this, "تاریخ و ساعت باید در آینده باشد", Toast.LENGTH_LONG).show();
            return;
        }

        trigger = TimeUtils.normalizeFuture(trigger, repeatType, System.currentTimeMillis());
        long id = alarmId >= 0 ? alarmId : System.currentTimeMillis();

        AlarmItem item = new AlarmItem(
                id,
                label.getText().toString().trim(),
                trigger,
                repeatType,
                true,
                vibrate.isChecked()
        );
        new AlarmStore(this).save(item);

        boolean scheduled = AlarmScheduler.schedule(this, item);
        ClockWidgetProvider.updateAll(this);

        if (!scheduled && !PermissionHelper.exactAlarmsGranted(this) && Build.VERSION.SDK_INT >= 31) {
            Toast.makeText(this, "زنگ ذخیره شد؛ دسترسی آلارم دقیق را فعال کنید.", Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())
                ));
            } catch (Exception ignored) {
            }
        } else {
            Toast.makeText(this, "زنگ ذخیره شد", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private void delete() {
        AlarmScheduler.cancel(this, alarmId);
        new AlarmStore(this).delete(alarmId);
        ClockWidgetProvider.updateAll(this);
        finish();
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
