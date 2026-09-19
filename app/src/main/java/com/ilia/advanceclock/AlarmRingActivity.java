package com.ilia.advanceclock;

import android.app.Activity;
import android.app.NotificationManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AlarmRingActivity extends Activity {
    private static final int[] SNOOZE_VALUES = {5,15,30,60,120,180,360,720,1440};
    private static final String[] SNOOZE_LABELS = {
            "۵ دقیقه","۱۵ دقیقه","۳۰ دقیقه","۱ ساعت","۲ ساعت","۳ ساعت","۶ ساعت","۱۲ ساعت","۲۴ ساعت"
    };

    private static WeakReference<AlarmRingActivity> showing = new WeakReference<>(null);
    private long alarmId = -1L;
    private Spinner snooze;

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

        snooze = findViewById(R.id.ring_snooze);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, SNOOZE_LABELS);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        snooze.setAdapter(adapter);

        applyStyle(AppSettings.alarmScreenStyle(this));
        bind(getIntent());

        findViewById(R.id.stop_alarm).setOnClickListener(v -> stopAndClose());
        findViewById(R.id.snooze_alarm).setOnClickListener(v -> snoozeAndClose());
    }

    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        bind(intent);
    }

    private void bind(android.content.Intent intent) {
        alarmId = intent == null ? -1L : intent.getLongExtra("alarmId", -1L);
        String label = intent == null ? "" : intent.getStringExtra("label");

        TextView title = findViewById(R.id.ring_title);
        title.setText(label == null || label.trim().isEmpty() ? "آلارم" : label);

        TextView time = findViewById(R.id.ring_time);
        time.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));

        TextView date = findViewById(R.id.ring_date);
        long now = System.currentTimeMillis();
        date.setText(CalendarUtils.formatDate(now, AppSettings.defaultCalendar(this)));

        AlarmItem item = new AlarmStore(this).find(alarmId);
        int minutes = item == null ? 15 : item.snoozeMinutes;
        setSnoozeSelection(minutes);
        renderAlarmImages(item);
    }

    private void renderAlarmImages(AlarmItem item) {
        LinearLayout container = findViewById(R.id.ring_images);
        View spacer = findViewById(R.id.ring_spacer);
        container.removeAllViews();

        java.util.ArrayList<String> uris = new java.util.ArrayList<>();
        if (item != null) {
            if (item.imageUri1 != null && !item.imageUri1.trim().isEmpty()) uris.add(item.imageUri1);
            if (item.imageUri2 != null && !item.imageUri2.trim().isEmpty()) uris.add(item.imageUri2);
        }

        if (uris.isEmpty()) {
            container.setVisibility(View.GONE);
            spacer.setVisibility(View.VISIBLE);
            return;
        }

        container.setVisibility(View.VISIBLE);
        spacer.setVisibility(View.GONE);

        for (int i = 0; i < uris.size(); i++) {
            ImageView image = new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setAdjustViewBounds(true);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f);
            if (uris.size() == 2) {
                if (i == 0) lp.setMarginEnd(dp(4));
                else lp.setMarginStart(dp(4));
            }

            image.setLayoutParams(lp);
            image.setClipToOutline(true);

            GradientDrawable bg = new GradientDrawable();
            bg.setColor(0x11000000);
            bg.setCornerRadius(dp(18));
            image.setBackground(bg);

            try {
                image.setImageURI(Uri.parse(uris.get(i)));
            } catch (Exception ignored) {}

            container.addView(image);
        }
    }

    private void applyStyle(int style) {
        LinearLayout root = findViewById(R.id.ring_root);
        TextView time = findViewById(R.id.ring_time);
        TextView title = findViewById(R.id.ring_title);
        TextView date = findViewById(R.id.ring_date);
        TextView subtitle = findViewById(R.id.ring_subtitle);
        TextView snoozeLabel = findViewById(R.id.snooze_label);
        Button snoozeButton = findViewById(R.id.snooze_alarm);
        Button stopButton = findViewById(R.id.stop_alarm);

        int bg, primary, text, muted, secondary;
        if (style == 1) {
            // Focus dark
            bg = 0xFF080B0D;
            primary = 0xFF53C7C0;
            text = 0xFFF7FAF9;
            muted = 0xFF9BAAA6;
            secondary = 0xFF2A3533;
        } else if (style == 2) {
            // Warm sunrise
            bg = 0xFFFFF5EA;
            primary = 0xFFB85C00;
            text = 0xFF4B2A12;
            muted = 0xFF8B6A50;
            secondary = 0xFFFFDFC0;
        } else {
            // Classic clean
            bg = 0xFFF5FAF9;
            primary = AppSettings.primaryColor(this);
            text = 0xFF173F3B;
            muted = 0xFF758783;
            secondary = 0xFFE7F1EF;
        }

        root.setBackgroundColor(bg);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);

        time.setTextColor(primary);
        title.setTextColor(text);
        date.setTextColor(muted);
        subtitle.setTextColor(muted);
        snoozeLabel.setTextColor(muted);

        snoozeButton.setTextColor(primary);
        snoozeButton.setBackground(roundRect(secondary, 18, primary, 1));
        stopButton.setTextColor(Color.WHITE);
        stopButton.setBackground(roundRect(primary, 20, primary, 0));
    }

    private GradientDrawable roundRect(int fill, int radiusDp, int stroke, int strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radiusDp));
        if (strokeDp > 0) d.setStroke(dp(strokeDp), stroke);
        return d;
    }

    private void setSnoozeSelection(int minutes) {
        int best = 0;
        int diff = Integer.MAX_VALUE;
        for (int i = 0; i < SNOOZE_VALUES.length; i++) {
            int d = Math.abs(SNOOZE_VALUES[i] - minutes);
            if (d < diff) { diff = d; best = i; }
        }
        snooze.setSelection(best);
    }

    private void snoozeAndClose() {
        int minutes = SNOOZE_VALUES[snooze.getSelectedItemPosition()];
        stopService(AlarmSoundService.stopIntent(this));
        boolean ok = AlarmScheduler.snooze(this, alarmId, minutes * 60_000L);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null && alarmId >= 0) nm.cancel(NotificationHelper.notificationId(alarmId));
        ClockWidgetProvider.updateAll(this);
        if (!ok) Toast.makeText(this, "برای یادآوری مجدد، دسترسی آلارم دقیق لازم است.", Toast.LENGTH_LONG).show();
        finishAndRemoveTask();
    }

    @Override public void onBackPressed() {
        // Alarm must be stopped explicitly.
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

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
