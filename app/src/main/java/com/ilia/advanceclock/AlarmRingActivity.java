package com.ilia.advanceclock;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AlarmRingActivity extends Activity {
    private static WeakReference<AlarmRingActivity> showing = new WeakReference<>(null);
    private long alarmId = -1L;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (android.os.Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_alarm_ring);
        showing = new WeakReference<>(this);
        bind(getIntent());
        findViewById(R.id.stop_alarm).setOnClickListener(v -> stopAndClose());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bind(intent);
    }

    private void bind(Intent intent) {
        alarmId = intent == null ? -1L : intent.getLongExtra("alarmId", -1L);
        String label = intent == null ? "" : intent.getStringExtra("label");
        TextView title = findViewById(R.id.ring_title);
        title.setText(label == null || label.trim().isEmpty() ? "آلارم" : label);
        TextView time = findViewById(R.id.ring_time);
        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
    }

    @Override public void onBackPressed() {
        // Alarm must be stopped explicitly with the stop button or notification action.
    }

    private void stopAndClose() {
        stopService(AlarmSoundService.stopIntent(this));
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && alarmId >= 0) nm.cancel(NotificationHelper.notificationId(alarmId));
        finishAndRemoveTask();
    }

    @Override protected void onDestroy() {
        AlarmRingActivity current = showing.get();
        if (current == this) showing.clear();
        super.onDestroy();
    }

    public static void finishIfShowing(long id) {
        AlarmRingActivity activity = showing.get();
        if (activity != null && (id < 0 || activity.alarmId == id)) {
            activity.runOnUiThread(activity::finishAndRemoveTask);
        }
    }
}
