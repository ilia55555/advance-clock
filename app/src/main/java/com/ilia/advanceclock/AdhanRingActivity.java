package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class AdhanRingActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
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
        setContentView(R.layout.activity_adhan_ring);

        int type = getIntent().getIntExtra("adhanType", AdhanScheduler.FAJR);
        ((TextView) findViewById(R.id.adhan_ring_title)).setText(AdhanScheduler.title(type));
        ((TextView) findViewById(R.id.adhan_ring_time)).setText(
                new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
        ((TextView) findViewById(R.id.adhan_ring_location)).setText(
                AppSettings.prayerLocationLabel(this));
        Button sound = findViewById(R.id.adhan_sound_toggle);
        sound.setOnClickListener(view -> {
            boolean muted = AppString.get(R.string.runtime_text_0306).contentEquals(sound.getText());
            send(muted ? AdhanSoundService.ACTION_UNMUTE : AdhanSoundService.ACTION_MUTE);
            sound.setText(muted ? AppString.get(R.string.runtime_text_0305) : AppString.get(R.string.runtime_text_0306));
        });
        findViewById(R.id.adhan_stop).setOnClickListener(view -> stopAndClose());
    }

    @Override public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            send(AdhanSoundService.ACTION_MUTE);
            ((Button) findViewById(R.id.adhan_sound_toggle))
                    .setText(AppString.get(R.string.runtime_text_0306));
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            send(AdhanSoundService.ACTION_RAISE);
            ((Button) findViewById(R.id.adhan_sound_toggle))
                    .setText(AppString.get(R.string.runtime_text_0305));
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN
                || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override public void onBackPressed() {
        // The dedicated adhan must be stopped explicitly.
    }

    private void send(String action) {
        startService(AdhanSoundService.command(this, action));
    }

    private void stopAndClose() {
        send(AdhanSoundService.ACTION_STOP);
        finishAndRemoveTask();
    }

}
