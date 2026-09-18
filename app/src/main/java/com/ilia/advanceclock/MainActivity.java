package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int REQ_NOTIFICATIONS = 100;
    private static final String PREFS = "advance_clock_app";
    private static final String PERMISSION_ONBOARDING = "permission_onboarding_v2";

    private LinearLayout alarmList;
    private LinearLayout noForgetList;
    private View clockPanel;
    private View noForgetPanel;
    private Button clockTab;
    private Button noForgetTab;
    private TextView permissionStatus;

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
        permissionStatus = findViewById(R.id.permission_status);

        clockTab.setOnClickListener(v -> showTab("clock"));
        noForgetTab.setOnClickListener(v -> showTab("noforget"));

        findViewById(R.id.add_alarm).setOnClickListener(v ->
                startActivity(new Intent(this, AlarmEditorActivity.class)));
        findViewById(R.id.add_clock_widget).setOnClickListener(v ->
                pinWidget(ClockWidgetProvider.class));

        findViewById(R.id.add_note).setOnClickListener(v ->
                startActivity(new Intent(this, NoForgetEditorActivity.class)));
        findViewById(R.id.quick_add_note).setOnClickListener(v ->
                startActivity(new Intent(this, NoForgetQuickAddActivity.class)));
        findViewById(R.id.add_noforget_widget).setOnClickListener(v ->
                pinWidget(NoForgetWidgetProvider.class));

        findViewById(R.id.alarm_permissions).setOnClickListener(v -> startPermissionFlow());

        String requestedTab = getIntent().getStringExtra("openTab");
        showTab("noforget".equals(requestedTab) ? "noforget" : "clock");

        boolean onboardingDone = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PERMISSION_ONBOARDING, false);
        if (!onboardingDone) {
            getWindow().getDecorView().postDelayed(this::startPermissionFlow, 500);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        NotificationHelper.ensureChannels(this);
        AlarmScheduler.rescheduleAll(this);
        NoForgetScheduler.rescheduleAll(this);
        updatePermissionStatus();
        renderAlarms();
        renderNoForget();

        if (waitingForSettings) {
            waitingForSettings = false;
            getWindow().getDecorView().postDelayed(this::advancePermissionFlow, 350);
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if ("noforget".equals(intent.getStringExtra("openTab"))) showTab("noforget");
    }

    private void showTab(String tab) {
        boolean clock = !"noforget".equals(tab);
        clockPanel.setVisibility(clock ? View.VISIBLE : View.GONE);
        noForgetPanel.setVisibility(clock ? View.GONE : View.VISIBLE);
        clockTab.setBackgroundColor(clock ? 0xFF38BDF8 : 0xFF172033);
        clockTab.setTextColor(clock ? 0xFF06111B : 0xFFE2E8F0);
        noForgetTab.setBackgroundColor(clock ? 0xFF172033 : 0xFF38BDF8);
        noForgetTab.setTextColor(clock ? 0xFFE2E8F0 : 0xFF06111B);
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
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
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
        updatePermissionStatus();

        if (PermissionHelper.exactAlarmsGranted(this)) {
            AlarmScheduler.rescheduleAll(this);
            NoForgetScheduler.rescheduleAll(this);
        }

        if (PermissionHelper.allCriticalGranted(this)) {
            Toast.makeText(this, "همه مجوزهای لازم فعال هستند", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this,
                    "بعضی مجوزها فعال نشدند؛ از دکمه مجوزها می‌توانید دوباره امتحان کنید.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == REQ_NOTIFICATIONS && permissionStage >= 0) {
            getWindow().getDecorView().post(this::advancePermissionFlow);
        }
    }

    private void updatePermissionStatus() {
        if (permissionStatus == null) return;
        permissionStatus.setText(PermissionHelper.statusText(this));
        permissionStatus.setTextColor(PermissionHelper.allCriticalGranted(this)
                ? 0xFF86EFAC : 0xFFFBBF24);
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
            alarmList.addView(emptyText("هنوز آلارمی تنظیم نشده است."));
            return;
        }
        for (AlarmItem item : items) alarmList.addView(alarmCard(item));
    }

    private LinearLayout alarmCard(AlarmItem item) {
        LinearLayout card = baseCard(item.enabled ? 0xFF172033 : 0xFF111827,
                item.enabled ? 0xFF38BDF8 : 0xFF334155);

        TextView title = cardTitle(item.label.trim().isEmpty() ? "آلارم" : item.label);
        card.addView(title);

        TextView time = smallText(TimeUtils.formatDateTime(item.triggerAtMillis)
                + "  •  " + TimeUtils.repeatLabel(item.repeatType));
        time.setTextColor(item.enabled ? 0xFFBAE6FD : 0xFF94A3B8);
        time.setPadding(0, dp(5), 0, dp(10));
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
            noForgetList.addView(emptyText("هنوز چیزی در NoForget ننوشته‌اید."));
            return;
        }
        long now = System.currentTimeMillis();
        for (NoForgetItem item : items) noForgetList.addView(noteCard(item, now));
    }

    private LinearLayout noteCard(NoForgetItem item, long now) {
        int urgency = item.urgency(now);
        int stroke = urgency >= 3 ? 0xFFEF4444 : urgency == 2 ? 0xFFF59E0B : 0xFF22C55E;
        LinearLayout card = baseCard(0xFF121A29, stroke);

        String titleText = item.title.trim().isEmpty()
                ? (item.body.trim().isEmpty() ? "دست‌نویس" : item.body)
                : item.title;
        card.addView(cardTitle(titleText));

        if (!item.body.trim().isEmpty() && !item.body.equals(titleText)) {
            TextView body = smallText(item.body);
            body.setMaxLines(3);
            body.setEllipsize(android.text.TextUtils.TruncateAt.END);
            body.setPadding(0, dp(5), 0, dp(5));
            card.addView(body);
        }

        StringBuilder meta = new StringBuilder();
        meta.append(item.priority == NoForgetItem.PRIORITY_HIGH ? "اهمیت زیاد"
                : item.priority == NoForgetItem.PRIORITY_LOW ? "اهمیت کم" : "اهمیت عادی");
        if (item.hasDue) meta.append("  •  ").append(TimeUtils.formatDateTime(item.dueAtMillis));
        if (item.reminderEnabled) meta.append("  •  ⏰ یادآوری");
        if (!"[]".equals(item.sketchJson)) meta.append("  •  ✎ دست‌نویس");
        TextView metaView = smallText(meta.toString());
        metaView.setTextColor(stroke);
        metaView.setPadding(0, dp(7), 0, dp(10));
        card.addView(metaView);

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
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), stroke);
        card.setBackground(bg);
        return card;
    }

    private TextView cardTitle(String text) {
        TextView title = new TextView(this);
        title.setText(text);
        title.setTextSize(18);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        return title;
    }

    private TextView smallText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(14);
        view.setTextColor(0xFFCBD5E1);
        return view;
    }

    private TextView emptyText(String text) {
        TextView empty = smallText(text);
        empty.setTextSize(16);
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(dp(16), dp(40), dp(16), dp(40));
        return empty;
    }

    private LinearLayout actionRow() {
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);
        return actions;
    }

    private Button actionButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(42));
        params.setMargins(dp(4), 0, 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
