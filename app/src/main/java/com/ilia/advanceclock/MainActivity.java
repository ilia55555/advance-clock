package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.icu.util.PersianCalendar;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final int REQ_NOTIFICATIONS = 100;
    private static final String PREFS = "advance_clock_app";
    private static final String PERMISSION_ONBOARDING = "permission_onboarding_v2";

    private final Calendar quickAlarm = Calendar.getInstance();

    private LinearLayout alarmList;
    private LinearLayout noForgetList;
    private View clockPanel;
    private View noForgetPanel;
    private TextView clockTab;
    private TextView noForgetTab;
    private View clockIndicator;
    private View noForgetIndicator;
    private TextView appTitle;

    private Button quickAlarmDate;
    private Button quickAlarmTime;
    private Spinner quickAlarmRepeat;
    private EditText quickAlarmLabel;
    private Switch quickAlarmVibrate;
    private TripleCalendarView clockCalendar;

    private EditText quickNoteTitle;
    private EditText quickNoteBody;
    private SketchView quickNoteSketch;

    private int permissionStage = -1;
    private boolean waitingForSettings;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationHelper.ensureChannels(this);

        alarmList = findViewById(R.id.alarm_list);
        noForgetList = findViewById(R.id.noforget_list);
        clockPanel = findViewById(R.id.clock_panel);
        noForgetPanel = findViewById(R.id.noforget_panel);
        clockTab = findViewById(R.id.tab_clock);
        noForgetTab = findViewById(R.id.tab_noforget);
        clockIndicator = findViewById(R.id.clock_indicator);
        noForgetIndicator = findViewById(R.id.noforget_indicator);
        appTitle = findViewById(R.id.app_title);
        findViewById(R.id.permissions_icon).setOnClickListener(v -> startPermissionFlow());
        findViewById(R.id.header_menu).setOnClickListener(v -> {
            if (clockPanel.getVisibility() == View.VISIBLE) {
                pinWidget(ClockWidgetProvider.class);
            } else {
                pinWidget(NoForgetWidgetProvider.class);
            }
        });

        quickAlarmDate = findViewById(R.id.quick_alarm_date);
        quickAlarmTime = findViewById(R.id.quick_alarm_time);
        quickAlarmRepeat = findViewById(R.id.quick_alarm_repeat);
        quickAlarmLabel = findViewById(R.id.quick_alarm_label);
        quickAlarmVibrate = findViewById(R.id.quick_alarm_vibrate);
        clockCalendar = findViewById(R.id.clock_calendar);

        quickNoteTitle = findViewById(R.id.quick_note_title);
        quickNoteBody = findViewById(R.id.quick_note_body);
        quickNoteSketch = findViewById(R.id.quick_note_sketch);

        setupAlarmComposer();
        setupNoteComposer();

        clockTab.setOnClickListener(v -> showTab("clock"));
        noForgetTab.setOnClickListener(v -> showTab("noforget"));

        findViewById(R.id.add_clock_widget).setOnClickListener(v ->
                pinWidget(ClockWidgetProvider.class));
        findViewById(R.id.add_noforget_widget).setOnClickListener(v ->
                pinWidget(NoForgetWidgetProvider.class));
        findViewById(R.id.open_full_note_editor).setOnClickListener(v ->
                startActivity(new Intent(this, NoForgetEditorActivity.class)));

        String requestedTab = getIntent().getStringExtra("openTab");
        showTab("noforget".equals(requestedTab) ? "noforget" : "clock");

        boolean onboardingDone = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PERMISSION_ONBOARDING, false);
        if (!onboardingDone) {
            getWindow().getDecorView().postDelayed(this::startPermissionFlow, 450);
        }
    }

    private void setupAlarmComposer() {
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"بدون تکرار", "هر روز", "هر هفته", "هر ماه", "هر سال"}
        );
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        quickAlarmRepeat.setAdapter(repeatAdapter);

        quickAlarm.add(Calendar.MINUTE, 1);
        quickAlarm.set(Calendar.SECOND, 0);
        quickAlarm.set(Calendar.MILLISECOND, 0);
        updateQuickAlarmLabels();
        clockCalendar.setSelectedMillis(quickAlarm.getTimeInMillis());

        clockCalendar.setOnDateSelectedListener(millis -> {
            Calendar picked = Calendar.getInstance();
            picked.setTimeInMillis(millis);
            quickAlarm.set(Calendar.YEAR, picked.get(Calendar.YEAR));
            quickAlarm.set(Calendar.MONTH, picked.get(Calendar.MONTH));
            quickAlarm.set(Calendar.DAY_OF_MONTH, picked.get(Calendar.DAY_OF_MONTH));
            updateQuickAlarmLabels();
        });

        quickAlarmDate.setOnClickListener(v -> new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    quickAlarm.set(Calendar.YEAR, year);
                    quickAlarm.set(Calendar.MONTH, month);
                    quickAlarm.set(Calendar.DAY_OF_MONTH, day);
                    clockCalendar.setSelectedMillis(quickAlarm.getTimeInMillis());
                    updateQuickAlarmLabels();
                },
                quickAlarm.get(Calendar.YEAR),
                quickAlarm.get(Calendar.MONTH),
                quickAlarm.get(Calendar.DAY_OF_MONTH)
        ).show());

        quickAlarmTime.setOnClickListener(v -> new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    quickAlarm.set(Calendar.HOUR_OF_DAY, hour);
                    quickAlarm.set(Calendar.MINUTE, minute);
                    quickAlarm.set(Calendar.SECOND, 0);
                    quickAlarm.set(Calendar.MILLISECOND, 0);
                    updateQuickAlarmLabels();
                },
                quickAlarm.get(Calendar.HOUR_OF_DAY),
                quickAlarm.get(Calendar.MINUTE),
                true
        ).show());

        findViewById(R.id.save_quick_alarm).setOnClickListener(v -> saveQuickAlarm());
    }

    private void setupNoteComposer() {
        findViewById(R.id.save_quick_note).setOnClickListener(v -> saveQuickNote());
        findViewById(R.id.note_undo).setOnClickListener(v -> quickNoteSketch.undo());
        findViewById(R.id.note_redo).setOnClickListener(v -> quickNoteSketch.redo());
        findViewById(R.id.note_clear).setOnClickListener(v -> quickNoteSketch.clearSketch());

        findViewById(R.id.pen_teal).setOnClickListener(v -> quickNoteSketch.setPenColor(0xFF087C77));
        findViewById(R.id.pen_orange).setOnClickListener(v -> quickNoteSketch.setPenColor(0xFFF17600));
        findViewById(R.id.pen_black).setOnClickListener(v -> quickNoteSketch.setPenColor(0xFF1B2423));
        findViewById(R.id.pen_blue).setOnClickListener(v -> quickNoteSketch.setPenColor(0xFF2457D6));

        findViewById(R.id.pen_thin).setOnClickListener(v -> quickNoteSketch.setPenWidthDp(2f));
        findViewById(R.id.pen_medium).setOnClickListener(v -> quickNoteSketch.setPenWidthDp(4f));
        findViewById(R.id.pen_thick).setOnClickListener(v -> quickNoteSketch.setPenWidthDp(7f));
    }

    @Override protected void onResume() {
        super.onResume();
        NotificationHelper.ensureChannels(this);
        AlarmScheduler.rescheduleAll(this);
        NoForgetScheduler.rescheduleAll(this);
        renderAlarms();
        renderNoForget();

        if (waitingForSettings) {
            waitingForSettings = false;
            getWindow().getDecorView().postDelayed(this::advancePermissionFlow, 300);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        showTab("noforget".equals(intent.getStringExtra("openTab")) ? "noforget" : "clock");
    }

    private void showTab(String tab) {
        boolean clock = !"noforget".equals(tab);
        clockPanel.setVisibility(clock ? View.VISIBLE : View.GONE);
        noForgetPanel.setVisibility(clock ? View.GONE : View.VISIBLE);
        clockIndicator.setVisibility(clock ? View.VISIBLE : View.INVISIBLE);
        noForgetIndicator.setVisibility(clock ? View.INVISIBLE : View.VISIBLE);
        clockTab.setTextColor(clock ? 0xFFFFFFFF : 0xFFD6EFED);
        noForgetTab.setTextColor(clock ? 0xFFD6EFED : 0xFFFFFFFF);
        appTitle.setText(clock ? "ساعت پیشرفته" : "یادداشت‌ها");
    }

    private void updateQuickAlarmLabels() {
        PersianCalendar pc = new PersianCalendar();
        pc.setTimeInMillis(quickAlarm.getTimeInMillis());
        String[] months = {
                "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
                "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"
        };
        String date = fa(pc.get(android.icu.util.Calendar.DAY_OF_MONTH))
                + " " + months[pc.get(android.icu.util.Calendar.MONTH)]
                + " " + fa(pc.get(android.icu.util.Calendar.YEAR));
        String time = String.format(Locale.US, "%02d:%02d",
                quickAlarm.get(Calendar.HOUR_OF_DAY),
                quickAlarm.get(Calendar.MINUTE));
        quickAlarmDate.setText("تاریخ\n" + date);
        quickAlarmTime.setText("ساعت\n" + fa(time));
    }

    private void saveQuickAlarm() {
        int repeatType = quickAlarmRepeat.getSelectedItemPosition();
        long trigger = quickAlarm.getTimeInMillis();

        if (trigger <= System.currentTimeMillis() && repeatType == AlarmItem.REPEAT_NONE) {
            Toast.makeText(this, "تاریخ و ساعت باید در آینده باشد", Toast.LENGTH_LONG).show();
            return;
        }

        trigger = TimeUtils.normalizeFuture(trigger, repeatType, System.currentTimeMillis());
        long id = System.currentTimeMillis();
        AlarmItem item = new AlarmItem(
                id,
                quickAlarmLabel.getText().toString().trim(),
                trigger,
                repeatType,
                true,
                quickAlarmVibrate.isChecked()
        );
        new AlarmStore(this).save(item);
        boolean scheduled = AlarmScheduler.schedule(this, item);
        ClockWidgetProvider.updateAll(this);
        renderAlarms();

        quickAlarmLabel.setText("");
        quickAlarm.setTimeInMillis(System.currentTimeMillis());
        quickAlarm.add(Calendar.MINUTE, 1);
        quickAlarm.set(Calendar.SECOND, 0);
        quickAlarm.set(Calendar.MILLISECOND, 0);
        clockCalendar.setSelectedMillis(quickAlarm.getTimeInMillis());
        updateQuickAlarmLabels();

        if (!scheduled && Build.VERSION.SDK_INT >= 31 && !PermissionHelper.exactAlarmsGranted(this)) {
            Toast.makeText(this, "زنگ ذخیره شد؛ دسترسی آلارم دقیق را فعال کنید.", Toast.LENGTH_LONG).show();
            startPermissionFlow();
        } else {
            Toast.makeText(this, "زنگ ذخیره شد", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveQuickNote() {
        String title = quickNoteTitle.getText().toString().trim();
        String body = quickNoteBody.getText().toString().trim();
        String sketch = quickNoteSketch.serialize();

        if (title.isEmpty() && body.isEmpty() && "[]".equals(sketch)) {
            Toast.makeText(this, "یک متن یا نقاشی وارد کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        long now = System.currentTimeMillis();
        NoForgetItem item = new NoForgetItem(
                now,
                title,
                body,
                sketch,
                NoForgetItem.PRIORITY_NORMAL,
                false,
                0L,
                false,
                now
        );
        new NoForgetStore(this).save(item);
        NoForgetWidgetProvider.updateAll(this);

        quickNoteTitle.setText("");
        quickNoteBody.setText("");
        quickNoteSketch.clearSketch();
        renderNoForget();
        Toast.makeText(this, "یادداشت ذخیره شد", Toast.LENGTH_SHORT).show();
    }

    private void startPermissionFlow() {
        permissionStage = 0;
        waitingForSettings = false;
        advancePermissionFlow();
    }

    private void advancePermissionFlow() {
        if (permissionStage < 0) return;

        if (permissionStage == 0) {
            permissionStage = 1;
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
                return;
            }
        }

        if (permissionStage == 1) {
            permissionStage = 2;
            if (Build.VERSION.SDK_INT >= 31 && !PermissionHelper.exactAlarmsGranted(this)) {
                try {
                    waitingForSettings = true;
                    startActivity(new Intent(
                            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                            Uri.parse("package:" + getPackageName())
                    ));
                    return;
                } catch (Exception ignored) {
                    waitingForSettings = false;
                }
            }
        }

        if (permissionStage == 2) {
            permissionStage = 3;
            if (Build.VERSION.SDK_INT >= 34 && !PermissionHelper.fullScreenGranted(this)) {
                try {
                    waitingForSettings = true;
                    startActivity(new Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName())
                    ));
                    return;
                } catch (Exception ignored) {
                    waitingForSettings = false;
                }
            }
        }

        permissionStage = -1;
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(PERMISSION_ONBOARDING, true)
                .apply();

        if (PermissionHelper.exactAlarmsGranted(this)) {
            AlarmScheduler.rescheduleAll(this);
            NoForgetScheduler.rescheduleAll(this);
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_NOTIFICATIONS && permissionStage >= 0) {
            getWindow().getDecorView().post(this::advancePermissionFlow);
        }
    }

    private void pinWidget(Class<?> provider) {
        if (Build.VERSION.SDK_INT < 26) {
            Toast.makeText(this, "ویجت را از فهرست ویجت‌های لانچر اضافه کنید", Toast.LENGTH_LONG).show();
            return;
        }
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(this, "ویجت را از فهرست ویجت‌های لانچر اضافه کنید", Toast.LENGTH_LONG).show();
            return;
        }
        manager.requestPinAppWidget(new ComponentName(this, provider), null, null);
    }

    private void renderAlarms() {
        alarmList.removeAllViews();
        List<AlarmItem> items = new AlarmStore(this).all();
        if (items.isEmpty()) {
            alarmList.addView(emptyText("هنوز زنگی تنظیم نشده است."));
            return;
        }
        for (AlarmItem item : items) alarmList.addView(alarmCard(item));
    }

    private LinearLayout alarmCard(AlarmItem item) {
        LinearLayout card = baseCard(0xFFFFFFFF, item.enabled ? 0xFFD9E9E7 : 0xFFE6EAEA);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = cardTitle(item.label.trim().isEmpty() ? "زنگ هشدار" : item.label);
        top.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView state = smallText(item.enabled ? "فعال" : "خاموش");
        state.setTextColor(item.enabled ? 0xFF087C77 : 0xFF98A4A1);
        state.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(state);
        card.addView(top);

        TextView time = smallText(TimeUtils.formatDateTime(item.triggerAtMillis)
                + "  •  " + TimeUtils.repeatLabel(item.repeatType)
                + (item.vibrate ? "  •  لرزش" : ""));
        time.setPadding(0, dp(6), 0, dp(10));
        card.addView(time);

        LinearLayout actions = actionRow();
        Button enabled = actionButton(item.enabled ? "خاموش" : "فعال");
        enabled.setOnClickListener(v -> {
            item.enabled = !item.enabled;
            new AlarmStore(this).save(item);
            if (item.enabled) AlarmScheduler.schedule(this, item);
            else AlarmScheduler.cancel(this, item.id);
            ClockWidgetProvider.updateAll(this);
            renderAlarms();
        });

        Button edit = actionButton("ویرایش");
        edit.setOnClickListener(v -> startActivity(new Intent(this, AlarmEditorActivity.class)
                .putExtra("alarmId", item.id)));

        Button delete = actionButton("حذف");
        delete.setOnClickListener(v -> {
            AlarmScheduler.cancel(this, item.id);
            new AlarmStore(this).delete(item.id);
            ClockWidgetProvider.updateAll(this);
            renderAlarms();
        });

        actions.addView(enabled);
        actions.addView(edit);
        actions.addView(delete);
        card.addView(actions);
        return card;
    }

    private void renderNoForget() {
        noForgetList.removeAllViews();
        List<NoForgetItem> items = new NoForgetStore(this).all();
        if (items.isEmpty()) {
            noForgetList.addView(emptyText("هنوز یادداشتی ندارید."));
            return;
        }
        long now = System.currentTimeMillis();
        for (NoForgetItem item : items) noForgetList.addView(noteCard(item, now));
    }

    private LinearLayout noteCard(NoForgetItem item, long now) {
        int urgency = item.urgency(now);
        int stroke = urgency >= 3 ? 0xFFF6B9B9 : urgency == 2 ? 0xFFFFD7A6 : 0xFFBFE4D5;
        LinearLayout card = baseCard(0xFFFFFFFF, stroke);

        String titleText = item.title.trim().isEmpty()
                ? (item.body.trim().isEmpty() ? "دست‌نویس" : item.body)
                : item.title;
        card.addView(cardTitle(titleText));

        if (!item.body.trim().isEmpty() && !item.body.equals(titleText)) {
            TextView body = smallText(item.body);
            body.setMaxLines(2);
            body.setEllipsize(android.text.TextUtils.TruncateAt.END);
            body.setPadding(0, dp(5), 0, dp(4));
            card.addView(body);
        }

        StringBuilder meta = new StringBuilder();
        if (item.hasDue) meta.append(TimeUtils.formatDateTime(item.dueAtMillis));
        if (item.reminderEnabled) {
            if (meta.length() > 0) meta.append("  •  ");
            meta.append("یادآوری");
        }
        if (!"[]".equals(item.sketchJson)) {
            if (meta.length() > 0) meta.append("  •  ");
            meta.append("نقاشی");
        }
        if (meta.length() > 0) {
            TextView metaView = smallText(meta.toString());
            metaView.setTextColor(0xFF6E827E);
            metaView.setPadding(0, dp(5), 0, dp(9));
            card.addView(metaView);
        }

        LinearLayout actions = actionRow();
        Button edit = actionButton("ویرایش");
        edit.setOnClickListener(v -> startActivity(new Intent(this, NoForgetEditorActivity.class)
                .putExtra("noteId", item.id)));

        Button delete = actionButton("حذف");
        delete.setOnClickListener(v -> {
            NoForgetScheduler.cancel(this, item.id);
            new NoForgetStore(this).delete(item.id);
            NoForgetWidgetProvider.updateAll(this);
            renderNoForget();
        });

        actions.addView(edit);
        actions.addView(delete);
        card.addView(actions);
        return card;
    }

    private LinearLayout baseCard(int fill, int stroke) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, dp(10));
        card.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), stroke);
        card.setBackground(bg);
        card.setElevation(dp(1));
        return card;
    }

    private TextView cardTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(17);
        title.setTextColor(0xFF143D3A);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.START);
        return title;
    }

    private TextView smallText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(13);
        view.setTextColor(0xFF738580);
        view.setGravity(Gravity.START);
        return view;
    }

    private TextView emptyText(String text) {
        TextView empty = smallText(text);
        empty.setTextSize(15);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(16), dp(28), dp(16), dp(28));
        return empty;
    }

    private LinearLayout actionRow() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        actions.setGravity(Gravity.START);
        return actions;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(12);
        button.setTextColor(0xFF0B6E69);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setPadding(dp(12), 0, dp(12), 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFECF6F5);
        bg.setCornerRadius(dp(11));
        bg.setStroke(dp(1), 0xFFD8E8E7);
        button.setBackground(bg);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                dp(40)
        );
        params.setMargins(0, 0, dp(6), 0);
        button.setLayoutParams(params);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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
