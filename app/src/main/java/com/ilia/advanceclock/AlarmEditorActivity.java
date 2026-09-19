package com.ilia.advanceclock;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Intent;
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
    private static final int[] SNOOZE_VALUES = {5,15,30,60,120,180,360,720,1440};
    private static final String[] SNOOZE_LABELS = {
            "۵ دقیقه","۱۵ دقیقه","۳۰ دقیقه","۱ ساعت","۲ ساعت","۳ ساعت","۶ ساعت","۱۲ ساعت","۲۴ ساعت"
    };

    private final Calendar selected = Calendar.getInstance();
    private long alarmId = -1L;
    private EditText label;
    private Button dateButton;
    private Button timeButton;
    private Button repeatButton;
    private Spinner priority;
    private Spinner snooze;
    private Button deleteButton;
    private Switch vibrate;

    private int dateCalendarType;
    private int recurrenceMode = RecurrenceUtils.NONE;
    private int intervalDays = 1;
    private String customDatesJson = "[]";

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_editor);

        label = findViewById(R.id.alarm_label);
        dateButton = findViewById(R.id.pick_date);
        timeButton = findViewById(R.id.pick_time);
        repeatButton = findViewById(R.id.alarm_repeat);
        priority = findViewById(R.id.alarm_priority);
        snooze = findViewById(R.id.alarm_snooze);
        deleteButton = findViewById(R.id.delete_alarm);
        vibrate = findViewById(R.id.alarm_vibrate);
        dateCalendarType = AppSettings.defaultCalendar(this);

        ArrayAdapter<String> pr = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"اهمیت کم","اهمیت عادی","اهمیت زیاد"});
        pr.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(pr);
        priority.setSelection(AlarmItem.PRIORITY_NORMAL);

        ArrayAdapter<String> sn = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, SNOOZE_LABELS);
        sn.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        snooze.setAdapter(sn);
        snooze.setSelection(1);

        alarmId = getIntent().getLongExtra("alarmId",-1L);
        if (alarmId >= 0) loadExisting();
        else {
            selected.add(Calendar.MINUTE,1);
            selected.set(Calendar.SECOND,0);
            selected.set(Calendar.MILLISECOND,0);
            deleteButton.setVisibility(View.GONE);
            updateButtons();
        }

        dateButton.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this, selected.getTimeInMillis(), dateCalendarType, (picked,type) -> {
                    dateCalendarType = type;
                    Calendar source = Calendar.getInstance();
                    source.setTimeInMillis(picked);
                    selected.set(Calendar.YEAR, source.get(Calendar.YEAR));
                    selected.set(Calendar.MONTH, source.get(Calendar.MONTH));
                    selected.set(Calendar.DAY_OF_MONTH, source.get(Calendar.DAY_OF_MONTH));
                    updateButtons();
                }));

        timeButton.setOnClickListener(v -> new TimePickerDialog(this,(view,hour,minute) -> {
            selected.set(Calendar.HOUR_OF_DAY,hour);
            selected.set(Calendar.MINUTE,minute);
            selected.set(Calendar.SECOND,0);
            selected.set(Calendar.MILLISECOND,0);
            updateButtons();
        },selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE),true).show());

        repeatButton.setOnClickListener(v -> RecurrenceDialog.show(
                this, selected.getTimeInMillis(), recurrenceMode, intervalDays, customDatesJson,
                (mode,interval,dates) -> {
                    recurrenceMode=mode; intervalDays=interval; customDatesJson=dates;
                    repeatButton.setText(RecurrenceUtils.summary(mode,interval,dates));
                }));

        findViewById(R.id.save_alarm).setOnClickListener(v -> save());
        deleteButton.setOnClickListener(v -> delete());
        findViewById(R.id.cancel).setOnClickListener(v -> finish());
    }

    private void loadExisting() {
        AlarmItem item = new AlarmStore(this).find(alarmId);
        if (item == null) { finish(); return; }
        label.setText(item.label);
        selected.setTimeInMillis(item.triggerAtMillis);
        priority.setSelection(item.priority);
        vibrate.setChecked(item.vibrate);
        recurrenceMode=item.recurrenceMode;
        intervalDays=item.intervalDays;
        customDatesJson=item.customDatesJson;
        repeatButton.setText(RecurrenceUtils.summary(recurrenceMode,intervalDays,customDatesJson));
        setSnooze(item.snoozeMinutes);
        updateButtons();
    }

    private void setSnooze(int minutes) {
        int best=0,delta=Integer.MAX_VALUE;
        for(int i=0;i<SNOOZE_VALUES.length;i++){
            int d=Math.abs(SNOOZE_VALUES[i]-minutes);
            if(d<delta){delta=d;best=i;}
        }
        snooze.setSelection(best);
    }

    private void updateButtons() {
        dateButton.setText(CalendarUtils.formatDate(selected.getTimeInMillis(),dateCalendarType));
        String time=String.format(Locale.US,"%02d:%02d",
                selected.get(Calendar.HOUR_OF_DAY),selected.get(Calendar.MINUTE));
        timeButton.setText(CalendarUtils.fa(time));
    }

    private void save() {
        long trigger=selected.getTimeInMillis();
        if(trigger<=System.currentTimeMillis()&&recurrenceMode==RecurrenceUtils.NONE){
            Toast.makeText(this,"تاریخ و ساعت باید در آینده باشد",Toast.LENGTH_LONG).show();
            return;
        }

        long id=alarmId>=0?alarmId:System.currentTimeMillis();
        int compat=recurrenceMode<=RecurrenceUtils.YEARLY?recurrenceMode:AlarmItem.REPEAT_NONE;
        AlarmItem item=new AlarmItem(
                id,label.getText().toString().trim(),trigger,compat,true,vibrate.isChecked(),
                priority.getSelectedItemPosition(),recurrenceMode,intervalDays,customDatesJson,
                SNOOZE_VALUES[snooze.getSelectedItemPosition()]
        );
        new AlarmStore(this).save(item);
        boolean scheduled=AlarmScheduler.schedule(this,item);
        ClockWidgetProvider.updateAll(this);

        if(!scheduled&&!PermissionHelper.exactAlarmsGranted(this)&&Build.VERSION.SDK_INT>=31){
            Toast.makeText(this,"زنگ ذخیره شد؛ دسترسی آلارم دقیق را فعال کنید.",Toast.LENGTH_LONG).show();
            try{
                startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:"+getPackageName())));
            }catch(Exception ignored){}
        }else Toast.makeText(this,"زنگ ذخیره شد",Toast.LENGTH_SHORT).show();
        finish();
    }

    private void delete(){
        AlarmScheduler.cancel(this,alarmId);
        new AlarmStore(this).delete(alarmId);
        ClockWidgetProvider.updateAll(this);
        finish();
    }
}
