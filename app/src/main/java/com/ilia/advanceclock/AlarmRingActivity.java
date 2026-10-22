package com.ilia.advanceclock;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;
import java.lang.ref.WeakReference;

public final class AlarmRingActivity extends Activity {
    private static WeakReference<AlarmRingActivity> showing = new WeakReference<>(null);
    private long alarmId = -1;
    private String toolKind, label;
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); ReminderScreen.applyWindowTheme(this); super.onCreate(state);
        showing = new WeakReference<>(this); ReminderScreen.prepareWindow(this); bind(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); bind(intent); }
    @Override protected void onResume() { super.onResume(); ReminderScreen.hideSystemBars(this); }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused); if (focused) ReminderScreen.hideSystemBars(this); }
    private void bind(Intent intent) {
        alarmId = intent == null ? -1 : intent.getLongExtra("alarmId", -1);
        toolKind = intent == null ? null : intent.getStringExtra("toolKind");
        label = intent == null ? "" : intent.getStringExtra("label");
        boolean tool = ToolAlarmScheduler.TIMER.equals(toolKind) || ToolAlarmScheduler.STOPWATCH.equals(toolKind);
        AlarmItem item = null;
        if (!tool) {
            String payload = intent == null ? null : intent.getStringExtra("alarmPayload");
            if (payload != null) try {
                AlarmItem snapshot = AlarmItem.fromJson(new org.json.JSONObject(payload));
                if (snapshot.id == alarmId) item = snapshot;
            } catch (org.json.JSONException ignored) {}
            if (item == null) item = new AlarmStore(this).find(alarmId);
        }
        ReminderScreen.Content data = new ReminderScreen.Content();
        data.title = label == null || label.trim().isEmpty() ? getString(R.string.runtime_text_0019) : label;
        data.openLabel = getString(R.string.reminder_open_destination, getString(tool
                ? ToolAlarmScheduler.TIMER.equals(toolKind) ? R.string.tab_timer : R.string.tab_stopwatch
                : R.string.ui_alarm));
        data.body = getString(tool ? ToolAlarmScheduler.TIMER.equals(toolKind) ? R.string.runtime_text_0584 : R.string.runtime_text_0585 : R.string.runtime_text_0586);
        if (tool) {
            android.content.SharedPreferences p = AlarmAttachments.prefs(this);
            data.image1 = p.getString(toolKind + "_active_image1", ""); data.image2 = p.getString(toolKind + "_active_image2", "");
            data.background = p.getBoolean(toolKind + "_active_background", false);
            data.attachments = NoteAttachment.parse(p.getString(toolKind + "_active_attachments", "[]"));
        } else if (item != null) {
            data.image1 = item.imageUri1; data.image2 = item.imageUri2; data.background = item.imagesAsBackground;
            data.attachments = NoteAttachment.parse(item.attachmentsJson); data.snoozeMinutes = item.snoozeMinutes;
        }
        Runnable open = tool ? () -> {
            startActivity(new Intent(this, MainActivity.class).putExtra("openTab", toolKind).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); stopAndClose();
        } : item == null ? null : () -> {
            AlarmEditorActivity.protect(alarmId);
            startActivity(new Intent(this, AlarmEditorActivity.class).putExtra("alarmId", alarmId)); stopAndClose();
        };
        setContentView(new ReminderScreen(this, data, this::stopAndClose, open, minutes -> {
            boolean ok = tool ? ToolAlarmScheduler.reschedule(this, toolKind, System.currentTimeMillis() + minutes * 60_000L, data.title)
                    : AlarmScheduler.snooze(this, alarmId, minutes * 60_000L);
            if (ok) { ClockWidgetProvider.updateAll(this); stopAndClose(); }
            else LogoToast.makeText(this, R.string.runtime_text_0587, Toast.LENGTH_LONG).show();
        }));
        AlertSoundHint.show(this, "alarm");
    }
    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_VOLUME_DOWN) { startService(AlarmSoundService.muteIntent(this)); return true; }
        if (code == KeyEvent.KEYCODE_VOLUME_UP) { startService(AlarmSoundService.unmuteIntent(this)); return true; }
        return super.onKeyDown(code, event);
    }
    @Override public boolean onKeyUp(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_VOLUME_UP) return true;
        return super.onKeyUp(code, event);
    }
    // ReminderScreen registers the Android 13+ back callback; keep older devices covered.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { /* Dismiss explicitly. */ }
    private void stopAndClose() {
        stopService(AlarmSoundService.stopIntent(this));
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null && alarmId != -1) manager.cancel(NotificationHelper.notificationId(alarmId));
        finishAndRemoveTask();
    }
    @Override protected void onDestroy() { if (showing.get() == this) showing.clear(); super.onDestroy(); }
    public static void finishIfShowing(long id) {
        AlarmRingActivity activity = showing.get();
        if (activity != null && (id < 0 || activity.alarmId == id)) activity.runOnUiThread(activity::finishAndRemoveTask);
    }
}
