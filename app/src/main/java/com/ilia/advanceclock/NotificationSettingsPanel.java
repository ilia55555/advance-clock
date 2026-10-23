package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Switch;

final class NotificationSettingsPanel {
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
    private Button dateChannelButton;
    private boolean hadExactPermission;
    private boolean hadNotificationPermission;
    private boolean hadDateChannelBlocked;

    private final Activity host;

    NotificationSettingsPanel(Activity host, LinearLayout root, LinearLayout alarmRoot) {
        this.host = host;
        hadNotificationPermission = notificationPermissionGranted();
        hadDateChannelBlocked = DateNotificationService.isChannelBlocked(host);
        notificationPermissionButton = permissionButton(root, R.string.grant_notifications, () -> {
            if (Build.VERSION.SDK_INT >= 33
                    && host.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    && !host.getSharedPreferences("feature_permissions", 0).getBoolean("notifications_requested", false)) {
                host.getSharedPreferences("feature_permissions", 0).edit().putBoolean("notifications_requested", true).apply();
                permissionRequestInFlight = true;
                host.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
            } else {
                host.startActivity(new android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, host.getPackageName()));
            }
        });
        exactPermissionButton = permissionButton(root, R.string.grant_exact_alarms, () -> {
            if (Build.VERSION.SDK_INT >= 31) host.startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                    android.net.Uri.parse("package:" + host.getPackageName())));
        });
        fullscreenPermissionButton = permissionButton(root, R.string.grant_fullscreen, () -> {
            if (Build.VERSION.SDK_INT >= 34) host.startActivity(new android.content.Intent(
                    android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                    android.net.Uri.parse("package:" + host.getPackageName())));
        });
        hadExactPermission = PermissionHelper.exactAlarmsGranted(host);

        persistentDate = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0071),
                AppSettings.persistentDateNotificationEnabled(host));

        dateChannelButton = permissionButton(root, R.string.date_channel_settings, () ->
                host.startActivity(new android.content.Intent(android.provider.Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                        .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, host.getPackageName())
                        .putExtra(android.provider.Settings.EXTRA_CHANNEL_ID, DateNotificationService.CHANNEL)));

        persistentExtraCalendars = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0072),
                AppSettings.persistentDateExtraCalendars(host));

        alarmReminders = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0073),
                AppSettings.alarmReminderNotificationsEnabled(host));

        noteReminders = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0074),
                AppSettings.noteReminderNotificationsEnabled(host));

        lockscreenDetails = addSwitch(
                root,
                AppString.get(R.string.runtime_text_0075),
                AppSettings.notificationLockscreenDetails(host));

        alarmFullscreenUnlocked = addSwitch(root,
                AppString.get(R.string.runtime_text_0217),
                AppSettings.alarmFullscreenUnlocked(host));
        alarmFullscreenLocked = addSwitch(root,
                AppString.get(R.string.runtime_text_0218),
                AppSettings.alarmFullscreenLocked(host));

        Button alarmSound = new Button(host);
        alarmSound.setText(AppString.get(R.string.runtime_text_0687)
                + SoundLibrary.name(host, AppSettings.defaultAlarmSoundUri(host)));
        alarmSound.setAllCaps(false);
        alarmSound.setTextColor(AppSettings.textPrimary(host));
        alarmSound.setBackgroundResource(R.drawable.bg_card);
        alarmSound.setOnClickListener(v -> host.startActivityForResult(
                new android.content.Intent(host, SoundPickerActivity.class), 902));
        LinearLayout.LayoutParams soundParams = new LinearLayout.LayoutParams(-1, -2);
        soundParams.bottomMargin = dp(7);
        alarmSound.setMinHeight(dp(54));
        alarmRoot.addView(alarmSound, soundParams);

        persistentDate.setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    persistentExtraCalendars.setEnabled(isChecked && PermissionHelper.notificationsGranted(host));
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

    }

    private Button permissionButton(LinearLayout root, int label, Runnable action) {
        Button button = new Button(host);
        button.setText(AppString.get(label));
        button.setAllCaps(false);
        button.setOnClickListener(v -> {
            try { action.run(); } catch (RuntimeException ignored) {
                host.startActivity(new android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.parse("package:" + host.getPackageName())));
            }
        });
        root.addView(button, new LinearLayout.LayoutParams(-1, -2));
        return button;
    }

    private void refreshPermissionState() {
        boolean notifications = PermissionHelper.notificationsGranted(host);
        notificationPermissionButton.setVisibility(notifications ? View.GONE : View.VISIBLE);
        dateChannelButton.setVisibility(notifications && persistentDate.isChecked()
                && DateNotificationService.isChannelBlocked(host) ? View.VISIBLE : View.GONE);
        boolean exact = PermissionHelper.exactAlarmsGranted(host);
        exactPermissionButton.setVisibility(exact ? View.GONE : View.VISIBLE);
        boolean fullscreen = notifications && PermissionHelper.fullScreenGranted(host);
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
            AlarmScheduler.rescheduleAll(host);
            NoForgetScheduler.rescheduleAll(host);
            AdhanScheduler.rescheduleAll(host);
            AlertSnoozes.restore(host);
        }
        hadExactPermission = exact;
    }

    void onResume() {
        refreshPermissionState();
        // Opening settings is read-only. Reconcile only when returning from a
        // permission change; otherwise this reposts even a dismissed notification.
        boolean granted = notificationPermissionGranted();
        if (hadNotificationPermission != granted
                || hadDateChannelBlocked != DateNotificationService.isChannelBlocked(host)) {
            applyPersistentNotificationState();
        }
    }

    void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        if (requestCode == 902 && resultCode == Activity.RESULT_OK && data != null) {
            AppSettings.setDefaultAlarmSoundUri(
                    host, data.getStringExtra(SoundPickerActivity.EXTRA_URI));
            host.recreate();
        }
    }


    private Switch addSwitch(
            LinearLayout root,
            String title,
            boolean checked) {
        Switch sw = new Switch(host);
        sw.setText(title);
        sw.setTextSize(15);
        sw.setTextColor(AppSettings.textPrimary(host));
        sw.setChecked(checked);
        sw.setGravity(Gravity.CENTER_VERTICAL);
        sw.setLayoutDirection(AppSettings.layoutDirection(host));
        sw.setPadding(dp(12), dp(10), dp(12), dp(10));
        sw.setMinHeight(dp(54));
        sw.setBackgroundResource(R.drawable.bg_card);

        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(7);
        root.addView(sw, lp);
        return sw;
    }

    private void saveSettings() {
        AppSettings.setPersistentDateNotificationEnabled(
                host,
                persistentDate.isChecked());
        AppSettings.setPersistentDateExtraCalendars(
                host,
                persistentExtraCalendars.isChecked());
        AppSettings.setAlarmReminderNotificationsEnabled(
                host,
                alarmReminders.isChecked());
        AppSettings.setNoteReminderNotificationsEnabled(
                host,
                noteReminders.isChecked());
        AppSettings.setNotificationLockscreenDetails(
                host,
                lockscreenDetails.isChecked());
        AppSettings.setAlarmFullscreenUnlocked(host, alarmFullscreenUnlocked.isChecked());
        AppSettings.setAlarmFullscreenLocked(host, alarmFullscreenLocked.isChecked());

        if (shouldRequestRuntimePermission() && !permissionRequestInFlight) {
            permissionRequestInFlight = true;
            host.requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_POST_NOTIFICATIONS);
        }

        applyPersistentNotificationState();
        refreshPermissionState();
        host.setResult(Activity.RESULT_OK);
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
                && host.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    private boolean notificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 33
                && host.checkSelfPermission(
                Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }

        NotificationManager manager =
                host.getSystemService(NotificationManager.class);
        return manager == null
                || manager.areNotificationsEnabled();
    }

    private void applyPersistentNotificationState() {
        hadNotificationPermission = notificationPermissionGranted();
        hadDateChannelBlocked = DateNotificationService.isChannelBlocked(host);
        if (persistentDate.isChecked()
                && hadNotificationPermission) {
            try {
                DateNotificationService.start(host);
            } catch (Exception ignored) {
            }
        } else {
            DateNotificationService.stop(host);
        }
    }

    void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        if (requestCode != REQ_POST_NOTIFICATIONS) {
            return;
        }

        permissionRequestInFlight = false;
        refreshPermissionState();
        applyPersistentNotificationState();
        host.setResult(Activity.RESULT_OK);
    }

    private int dp(int value) {
        return Math.round(
                value
                        * host.getResources()
                        .getDisplayMetrics()
                        .density);
    }
}
