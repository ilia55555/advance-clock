package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

public final class NotificationSettingsActivity extends Activity {
    private static final int REQ_POST_NOTIFICATIONS = 901;

    private Switch persistentDate;
    private Switch persistentExtraCalendars;
    private Switch alarmReminders;
    private Switch noteReminders;
    private Switch lockscreenDetails;
    private boolean permissionRequestInFlight;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        int pad = dp(18);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(pad, dp(12), pad, dp(24));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = new TextView(this);
        title.setText("تنظیمات اعلان");
        title.setTextSize(25);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(
                AppSettings.textPrimary(this),
                PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        root.addView(top);

        TextView label = new TextView(this);
        label.setText("اعلان‌ها");
        label.setTextSize(16);
        label.setTextColor(AppSettings.textPrimary(this));
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, dp(14), 0, dp(8));
        root.addView(label);

        persistentDate = addSwitch(
                root,
                "اعلان دائمی تاریخ",
                AppSettings.persistentDateNotificationEnabled(this));

        persistentExtraCalendars = addSwitch(
                root,
                "نمایش تقویم‌های دیگر",
                AppSettings.persistentDateExtraCalendars(this));

        alarmReminders = addSwitch(
                root,
                "یادآوری هشدارها",
                AppSettings.alarmReminderNotificationsEnabled(this));

        noteReminders = addSwitch(
                root,
                "یادآوری یادداشت‌ها",
                AppSettings.noteReminderNotificationsEnabled(this));

        lockscreenDetails = addSwitch(
                root,
                "نمایش جزئیات روی صفحه قفل",
                AppSettings.notificationLockscreenDetails(this));

        persistentDate.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    persistentExtraCalendars.setEnabled(isChecked);
                    persistentExtraCalendars.setAlpha(
                            isChecked ? 1f : 0.5f);
                    saveSettings();
                });
        persistentExtraCalendars.setEnabled(
                persistentDate.isChecked());
        persistentExtraCalendars.setAlpha(
                persistentDate.isChecked() ? 1f : 0.5f);

        persistentExtraCalendars.setOnCheckedChangeListener(
                (button, checked) -> saveSettings());
        alarmReminders.setOnCheckedChangeListener(
                (button, checked) -> saveSettings());
        noteReminders.setOnCheckedChangeListener(
                (button, checked) -> saveSettings());
        lockscreenDetails.setOnCheckedChangeListener(
                (button, checked) -> saveSettings());

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private Switch addSwitch(
            LinearLayout root,
            String title,
            boolean checked) {
        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(15);
        sw.setTextColor(AppSettings.textPrimary(this));
        sw.setChecked(checked);
        sw.setGravity(Gravity.CENTER_VERTICAL);
        sw.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        sw.setPadding(dp(12), 0, dp(12), 0);
        sw.setBackgroundResource(R.drawable.bg_card);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, dp(54));
        lp.bottomMargin = dp(7);
        root.addView(sw, lp);
        return sw;
    }

    private void saveSettings() {
        AppSettings.setPersistentDateNotificationEnabled(
                this,
                persistentDate.isChecked());
        AppSettings.setPersistentDateExtraCalendars(
                this,
                persistentExtraCalendars.isChecked());
        AppSettings.setAlarmReminderNotificationsEnabled(
                this,
                alarmReminders.isChecked());
        AppSettings.setNoteReminderNotificationsEnabled(
                this,
                noteReminders.isChecked());
        AppSettings.setNotificationLockscreenDetails(
                this,
                lockscreenDetails.isChecked());

        if (shouldRequestRuntimePermission() && !permissionRequestInFlight) {
            permissionRequestInFlight = true;
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_POST_NOTIFICATIONS);
        }

        applyPersistentNotificationState();
        setResult(RESULT_OK);
    }

    private boolean shouldRequestRuntimePermission() {
        if (Build.VERSION.SDK_INT < 33) {
            return false;
        }

        boolean wantsNotifications =
                persistentDate.isChecked()
                        || alarmReminders.isChecked()
                        || noteReminders.isChecked();

        return wantsNotifications
                && checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    private boolean notificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationManager manager =
                getSystemService(NotificationManager.class);
        return manager == null
                || manager.areNotificationsEnabled();
    }

    private void applyPersistentNotificationState() {
        if (persistentDate.isChecked()
                && notificationPermissionGranted()) {
            try {
                DateNotificationService.start(this);
                DateNotificationService.refreshNow(this);
            } catch (Exception ignored) {
            }
        } else {
            DateNotificationService.stop(this);
        }
    }

    @Override public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults);

        if (requestCode != REQ_POST_NOTIFICATIONS) {
            return;
        }

        permissionRequestInFlight = false;
        applyPersistentNotificationState();
        setResult(RESULT_OK);
    }

    private int dp(int value) {
        return Math.round(
                value
                        * getResources()
                        .getDisplayMetrics()
                        .density);
    }
}
