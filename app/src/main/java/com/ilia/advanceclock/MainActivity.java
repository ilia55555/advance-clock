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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

public final class MainActivity extends Activity {
    private LinearLayout alarmList;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NotificationHelper.ensureChannel(this);
        alarmList = findViewById(R.id.alarm_list);

        findViewById(R.id.add_alarm).setOnClickListener(v ->
                startActivity(new Intent(this, AlarmEditorActivity.class)));
        findViewById(R.id.add_widget).setOnClickListener(v -> pinWidget());
        findViewById(R.id.alarm_permissions).setOnClickListener(v -> openAlarmPermissions());
        requestNotificationPermission();
    }

    @Override protected void onResume() {
        super.onResume();
        AlarmScheduler.rescheduleAll(this);
        render();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void openAlarmPermissions() {
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null && !manager.canUseFullScreenIntent()) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:" + getPackageName())));
                    return;
                } catch (Exception ignored) {}
            }
        }
        Intent app = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        try { startActivity(app); }
        catch (Exception e) { Toast.makeText(this, "مجوزهای آلارم فعال هستند", Toast.LENGTH_SHORT).show(); }
    }

    private void pinWidget() {
        if (Build.VERSION.SDK_INT < 26) {
            Toast.makeText(this, "ویجت را از فهرست ویجت‌های لانچر اضافه کنید", Toast.LENGTH_LONG).show();
            return;
        }
        AppWidgetManager manager = getSystemService(AppWidgetManager.class);
        if (manager == null || !manager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(this, "ویجت را از فهرست ویجت‌های لانچر اضافه کنید", Toast.LENGTH_LONG).show();
            return;
        }
        manager.requestPinAppWidget(new ComponentName(this, ClockWidgetProvider.class), null, null);
    }

    private void render() {
        alarmList.removeAllViews();
        List<AlarmItem> items = new AlarmStore(this).all();
        if (items.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("هنوز آلارمی تنظیم نشده است.");
            empty.setTextSize(16);
            empty.setTextColor(0xFFCBD5E1);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(16), dp(40), dp(16), dp(40));
            alarmList.addView(empty);
            return;
        }
        for (AlarmItem item : items) alarmList.addView(alarmCard(item));
    }

    private LinearLayout alarmCard(AlarmItem item) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardParams);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(item.enabled ? 0xFF172033 : 0xFF111827);
        bg.setCornerRadius(dp(18));
        bg.setStroke(dp(1), item.enabled ? 0xFF38BDF8 : 0xFF334155);
        card.setBackground(bg);

        TextView title = new TextView(this);
        title.setText((item.label == null || item.label.trim().isEmpty()) ? "آلارم" : item.label);
        title.setTextSize(18);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(title);

        TextView time = new TextView(this);
        time.setText(TimeUtils.formatDateTime(item.triggerAtMillis) + "  •  " + TimeUtils.repeatLabel(item.repeatType));
        time.setTextSize(14);
        time.setTextColor(item.enabled ? 0xFFBAE6FD : 0xFF94A3B8);
        time.setPadding(0, dp(5), 0, dp(12));
        card.addView(time);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.END);

        Button enabled = actionButton(item.enabled ? "خاموش" : "فعال");
        enabled.setOnClickListener(v -> {
            item.enabled = !item.enabled;
            AlarmStore store = new AlarmStore(this);
            store.save(item);
            if (item.enabled) AlarmScheduler.schedule(this, item);
            else AlarmScheduler.cancel(this, item.id);
            ClockWidgetProvider.updateAll(this);
            render();
        });

        Button edit = actionButton("ویرایش");
        edit.setOnClickListener(v -> startActivity(new Intent(this, AlarmEditorActivity.class)
                .putExtra("alarmId", item.id)));

        Button delete = actionButton("حذف");
        delete.setOnClickListener(v -> {
            AlarmScheduler.cancel(this, item.id);
            new AlarmStore(this).delete(item.id);
            ClockWidgetProvider.updateAll(this);
            render();
        });

        actions.addView(enabled);
        actions.addView(edit);
        actions.addView(delete);
        card.addView(actions);
        return card;
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
