package com.ilia.advanceclock;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
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
    private TextView permissionStatus;
    private Button permissionButton;

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
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        close.setOnClickListener(v -> finish());

        root.addView(top);

        permissionStatus = new TextView(this);
        permissionStatus.setTextSize(13);
        permissionStatus.setTextColor(AppSettings.textSecondary(this));
        permissionStatus.setPadding(dp(12), dp(10), dp(12), dp(10));
        permissionStatus.setBackgroundResource(R.drawable.bg_card);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.bottomMargin = dp(8);
        root.addView(permissionStatus, statusLp);

        addSectionTitle(root, "اعلان دائمی تاریخ");

        persistentDate = addSwitch(
                root,
                "نمایش اعلان دائمی تاریخ",
                "تاریخ روز را همیشه در نوار وضعیت نگه می‌دارد. با خاموش کردن این گزینه، سرویس اعلان دائمی نیز متوقف می‌شود.",
                AppSettings.persistentDateNotificationEnabled(this));

        persistentExtraCalendars = addSwitch(
                root,
                "نمایش دو تقویم دیگر",
                "در متن اعلان دائمی، تاریخ دو تقویم دیگر را هم کنار تقویم پیش‌فرض نشان می‌دهد.",
                AppSettings.persistentDateExtraCalendars(this));

        persistentDate.setOnCheckedChangeListener((buttonView, isChecked) -> {
            persistentExtraCalendars.setEnabled(isChecked);
            persistentExtraCalendars.setAlpha(isChecked ? 1f : 0.55f);
        });
        persistentExtraCalendars.setEnabled(persistentDate.isChecked());
        persistentExtraCalendars.setAlpha(persistentDate.isChecked() ? 1f : 0.55f);

        addSectionTitle(root, "یادآوری‌ها");

        alarmReminders = addSwitch(
                root,
                "اعلان یادآوری هشدارها",
                "اعلان‌های یادآوری‌ای که برای هشدارها تنظیم کرده‌اید نمایش داده شوند.",
                AppSettings.alarmReminderNotificationsEnabled(this));

        noteReminders = addSwitch(
                root,
                "اعلان یادآوری یادداشت‌ها",
                "وقتی موعد یادداشت می‌رسد اعلان یادآوری نمایش داده شود. تکرار و زمان‌بندی خود یادداشت همچنان حفظ می‌شود.",
                AppSettings.noteReminderNotificationsEnabled(this));

        addSectionTitle(root, "حریم خصوصی");

        lockscreenDetails = addSwitch(
                root,
                "نمایش جزئیات روی صفحه قفل",
                "اگر خاموش باشد، اعلان‌ها با حالت خصوصی ساخته می‌شوند. تنظیمات امنیتی خود Android می‌تواند محدودیت بیشتری اعمال کند.",
                AppSettings.notificationLockscreenDetails(this));

        addSectionTitle(root, "مجوز و تنظیمات Android");

        permissionButton = actionButton("درخواست مجوز اعلان");
        permissionButton.setOnClickListener(v -> requestNotificationPermission());
        root.addView(permissionButton, actionLp());

        Button appNotificationSettings = actionButton("تنظیمات کلی اعلان در Android");
        appNotificationSettings.setOnClickListener(v -> openAppNotificationSettings());
        root.addView(appNotificationSettings, actionLp());

        TextView systemHint = hint(
                "صدا، ویبره، اهمیت، پاپ‌آپ و رفتار دقیق هر کانال از Android کنترل می‌شود. " +
                "برای دسترسی مستقیم می‌توانید هر کانال را جداگانه باز کنید.");
        root.addView(systemHint);

        Button dateChannel = actionButton("کانال اعلان دائمی تاریخ");
        dateChannel.setOnClickListener(v -> openChannel(DateNotificationService.CHANNEL));
        root.addView(dateChannel, actionLp());

        Button reminderChannel = actionButton("کانال یادآوری‌ها");
        reminderChannel.setOnClickListener(v -> openChannel(NotificationHelper.REMINDER_CHANNEL));
        root.addView(reminderChannel, actionLp());

        Button alarmChannel = actionButton("کانال آلارم‌ها");
        alarmChannel.setOnClickListener(v -> openChannel(NotificationHelper.ALARM_CHANNEL));
        root.addView(alarmChannel, actionLp());

        TextView alarmNote = hint(
                "اعلان خودِ آلارم هنگام زنگ خوردن برای اجرای سرویس foreground ضروری است؛ " +
                "بنابراین از این صفحه خاموش نمی‌شود. صدای زنگ و ویبرهٔ هر هشدار از خود هشدار و تنظیمات Android کنترل می‌شود.");
        root.addView(alarmNote);

        Button save = new Button(this);
        save.setText("ذخیره تنظیمات اعلان");
        save.setTextColor(0xFFFFFFFF);
        save.setAllCaps(false);
        save.setBackgroundColor(AppSettings.secondaryColor(this));
        LinearLayout.LayoutParams saveLp = new LinearLayout.LayoutParams(-1, dp(58));
        saveLp.topMargin = dp(22);
        root.addView(save, saveLp);

        save.setOnClickListener(v -> saveAndClose());

        setContentView(scroll);
        updatePermissionStatus();
    }

    @Override protected void onResume() {
        super.onResume();
        if (permissionStatus != null) updatePermissionStatus();
    }

    private void saveAndClose() {
        AppSettings.setPersistentDateNotificationEnabled(this, persistentDate.isChecked());
        AppSettings.setPersistentDateExtraCalendars(this, persistentExtraCalendars.isChecked());
        AppSettings.setAlarmReminderNotificationsEnabled(this, alarmReminders.isChecked());
        AppSettings.setNoteReminderNotificationsEnabled(this, noteReminders.isChecked());
        AppSettings.setNotificationLockscreenDetails(this, lockscreenDetails.isChecked());

        if (persistentDate.isChecked() && notificationPermissionGranted()) {
            try {
                DateNotificationService.start(this);
                DateNotificationService.refreshNow(this);
            } catch (Exception ignored) {
            }
        } else {
            DateNotificationService.stop(this);
        }

        setResult(RESULT_OK);
        finish();
    }

    private boolean notificationPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 33) {
            return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        NotificationManager manager = getSystemService(NotificationManager.class);
        return manager == null || manager.areNotificationsEnabled();
    }

    private void updatePermissionStatus() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        boolean appEnabled = manager == null || manager.areNotificationsEnabled();
        boolean runtimeGranted = Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;

        boolean granted = appEnabled && runtimeGranted;
        permissionStatus.setText(granted
                ? "وضعیت مجوز اعلان: فعال"
                : "وضعیت مجوز اعلان: غیرفعال — برای نمایش اعلان‌ها باید مجوز Android نیز فعال باشد.");
        permissionStatus.setTextColor(granted
                ? AppSettings.primaryColor(this)
                : 0xFFD24A43);

        if (Build.VERSION.SDK_INT >= 33 && !runtimeGranted) {
            permissionButton.setEnabled(true);
            permissionButton.setText("درخواست مجوز اعلان");
        } else {
            permissionButton.setEnabled(false);
            permissionButton.setText("مجوز اعلان صادر شده");
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQ_POST_NOTIFICATIONS);
        } else {
            openAppNotificationSettings();
        }
    }

    @Override public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_POST_NOTIFICATIONS) {
            updatePermissionStatus();
            if (notificationPermissionGranted()
                    && persistentDate != null
                    && persistentDate.isChecked()) {
                try {
                    DateNotificationService.start(this);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private void openAppNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
            startActivity(intent);
        } catch (Exception ignored) {
        }
    }

    private void openChannel(String channelId) {
        try {
            NotificationHelper.ensureChannels(this);
            Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, channelId);
            startActivity(intent);
        } catch (Exception ignored) {
            openAppNotificationSettings();
        }
    }

    private void addSectionTitle(LinearLayout root, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextSize(16);
        v.setTextColor(AppSettings.textPrimary(this));
        v.setTypeface(null, android.graphics.Typeface.BOLD);
        v.setPadding(0, dp(18), 0, dp(7));
        root.addView(v);
    }

    private Switch addSwitch(
            LinearLayout root,
            String title,
            String description,
            boolean checked) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(12), dp(8), dp(12), dp(10));
        card.setBackgroundResource(R.drawable.bg_card);

        Switch sw = new Switch(this);
        sw.setText(title);
        sw.setTextSize(15);
        sw.setTextColor(AppSettings.textPrimary(this));
        sw.setChecked(checked);
        sw.setGravity(Gravity.CENTER_VERTICAL);
        sw.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.addView(sw, new LinearLayout.LayoutParams(-1, dp(48)));

        TextView d = new TextView(this);
        d.setText(description);
        d.setTextSize(12);
        d.setTextColor(AppSettings.textSecondary(this));
        d.setPadding(0, 0, 0, dp(2));
        card.addView(d, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(8);
        root.addView(card, lp);
        return sw;
    }

    private Button actionButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextColor(AppSettings.textPrimary(this));
        b.setBackgroundColor(AppSettings.field(this));
        return b;
    }

    private LinearLayout.LayoutParams actionLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52));
        lp.bottomMargin = dp(7);
        return lp;
    }

    private TextView hint(String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(AppSettings.textSecondary(this));
        v.setTextSize(12);
        v.setPadding(dp(2), dp(4), dp(2), dp(9));
        return v;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
