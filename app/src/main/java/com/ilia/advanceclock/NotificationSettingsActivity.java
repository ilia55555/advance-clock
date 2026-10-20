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
import android.widget.Button;
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
    private Switch alarmFullscreenUnlocked;
    private Switch alarmFullscreenLocked;
    private boolean permissionRequestInFlight;
    private Button notificationPermissionButton, exactPermissionButton, fullscreenPermissionButton;
    private boolean hadExactPermission;

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
        root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(pad, dp(12), pad, dp(24));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(
                AppSettings.textPrimary(this),
                PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription(AppString.get(R.string.runtime_text_0002));
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));

        TextView title = new TextView(this);
        title.setText(AppString.get(R.string.runtime_text_0069));
        title.setTextSize(25);
        title.setTextColor(AppSettings.textPrimary(this));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        root.addView(top);

        TextView label = new TextView(this);
        label.setText(AppString.get(R.string.runtime_text_0070));
        label.setTextSize(16);
        label.setTextColor(AppSettings.textPrimary(this));
        label.setTypeface(null, android.graphics.Typeface.BOLD);
        label.setPadding(0, dp(14), 0, dp(8));
        root.addView(label);

        notificationPermissionButton = permissionButton(root, R.string.grant_notifications, () -> {
            if (Build.VERSION.SDK_INT >= 33
                    && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    && !getSharedPreferences("feature_permissions", 0).getBoolean("notifications_requested", false)) {
                getSharedPreferences("feature_permissions", 0).edit().putBoolean("notifications_requested", true).apply();
                permissionRequestInFlight = true;
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
            } else {
                startActivity(new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, getPackageName()));
            }
        });
        exactPermissionButton = permissionButton(root, R.string.grant_exact_alarms, () -> {
            if (Build.VERSION.SDK_INT >= 31) startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:" + getPackageName())));
        });
        fullscreenPermissionButton = permissionButton(root, R.string.grant_fullscreen, () -> {
            if (Build.VERSION.SDK_INT >= 34) startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    android.net.Uri.parse("package:" + getPackageName())));
        });
        hadExactPermission = PermissionHelper.exactAlarmsGranted(this);

        persistentDate = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0071),
                AppSettings.persistentDateNotificationEnabled(this));

        persistentExtraCalendars = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0072),
                AppSettings.persistentDateExtraCalendars(this));

        alarmReminders = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0073),
                AppSettings.alarmReminderNotificationsEnabled(this));

        noteReminders = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0074),
                AppSettings.noteReminderNotificationsEnabled(this));

        lockscreenDetails = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0075),
                AppSettings.notificationLockscreenDetails(this));

        alarmFullscreenUnlocked = addSwitch(root,
                AppString.get(R.string.runtime_text_0217),
                AppSettings.alarmFullscreenUnlocked(this));
        alarmFullscreenLocked = addSwitch(root,
                AppString.get(R.string.runtime_text_0218),
                AppSettings.alarmFullscreenLocked(this));

        Button alarmSound = new Button(this);
        alarmSound.setText(AppString.get(R.string.runtime_text_0687)
                + SoundLibrary.name(this, AppSettings.defaultAlarmSoundUri(this)));
        alarmSound.setAllCaps(false);
        alarmSound.setTextColor(AppSettings.textPrimary(this));
        alarmSound.setBackgroundResource(R.drawable.bg_card);
        alarmSound.setOnClickListener(v -> startActivityForResult(
                new android.content.Intent(this, SoundPickerActivity.class), 902));
        LinearLayout.LayoutParams soundParams = new LinearLayout.LayoutParams(-1, dp(54));
        soundParams.bottomMargin = dp(7);
        root.addView(alarmSound, soundParams);

        persistentDate.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    persistentExtraCalendars.setEnabled(isChecked && PermissionHelper.notificationsGranted(this));
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
        alarmFullscreenUnlocked.setOnCheckedChangeListener((button, checked) -> saveSettings());
        alarmFullscreenLocked.setOnCheckedChangeListener((button, checked) -> saveSettings());

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
    }

    private Button permissionButton(LinearLayout root, int label, Runnable action) {
        Button button = new Button(this);
        button.setText(AppString.get(label));
        button.setAllCaps(false);
        button.setOnClickListener(v -> {
            try { action.run(); } catch (RuntimeException ignored) {
                startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + getPackageName())));
            }
        });
        root.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private void refreshPermissionState() {
        boolean notifications = PermissionHelper.notificationsGranted(this);
        notificationPermissionButton.setVisibility(notifications ? View.GONE : View.VISIBLE);
        boolean exact = PermissionHelper.exactAlarmsGranted(this);
        exactPermissionButton.setVisibility(exact ? View.GONE : View.VISIBLE);
        boolean fullscreen = notifications && PermissionHelper.fullScreenGranted(this);
        fullscreenPermissionButton.setVisibility(notifications && !fullscreen ? View.VISIBLE : View.GONE);
        Switch[] notificationControls = {persistentDate, alarmReminders, noteReminders, lockscreenDetails};
        for (Switch control : notificationControls) {
            control.setEnabled(notifications);
            control.setAlpha(notifications ? 1f : .5f);
        }
        persistentExtraCalendars.setEnabled(notifications && persistentDate.isChecked());
        persistentExtraCalendars.setAlpha(persistentExtraCalendars.isEnabled() ? 1f : .5f);
        for (Switch control : new Switch[]{alarmFullscreenUnlocked, alarmFullscreenLocked}) {
            control.setEnabled(fullscreen);
            control.setAlpha(fullscreen ? 1f : .5f);
        }
        if (!hadExactPermission && exact) {
            AlarmScheduler.rescheduleAll(this);
            NoForgetScheduler.rescheduleAll(this);
            AdhanScheduler.rescheduleAll(this);
        }
        hadExactPermission = exact;
    }

    @Override protected void onResume() {
        super.onResume();
        refreshPermissionState();
        applyPersistentNotificationState();
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 902 && resultCode == RESULT_OK && data != null) {
            AppSettings.setDefaultAlarmSoundUri(
                    this, data.getStringExtra(SoundPickerActivity.EXTRA_URI));
            recreate();
        }
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
        sw.setLayoutDirection(AppSettings.layoutDirection(this));
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
        AppSettings.setAlarmFullscreenUnlocked(this, alarmFullscreenUnlocked.isChecked());
        AppSettings.setAlarmFullscreenLocked(this, alarmFullscreenLocked.isChecked());

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
        refreshPermissionState();
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
