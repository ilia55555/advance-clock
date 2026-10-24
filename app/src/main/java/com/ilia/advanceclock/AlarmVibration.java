package com.ilia.advanceclock;

import android.media.AudioAttributes;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;

/** Classify ringing vibrations as alarms rather than generic touch feedback. */
final class AlarmVibration {
    static void start(Vibrator vibrator, VibrationEffect effect) {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= 33) {
            vibrator.vibrate(effect, new VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM).build());
        } else {
            vibrator.vibrate(effect, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
        }
    }
}
