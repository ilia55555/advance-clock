package com.ilia.advanceclock;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

/** One deadline per ringing session; mute/unmute never restarts it. Excludes adhan. */
final class AlarmRingTimeout {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PowerManager.WakeLock wakeLock;

    void start(Context context, Runnable stop) {
        cancel();
        int minutes = AppSettings.alarmRingMinutes(context);
        if (minutes == 0) return;
        long duration = minutes * 60_000L;
        PowerManager manager = context.getSystemService(PowerManager.class);
        if (manager != null) {
            wakeLock = manager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "AdvanceClock:RingTimeout");
            // Keep the deadline running even when the display is switched off.
            wakeLock.acquire(duration + 30_000L);
        }
        handler.postDelayed(() -> {
            cancel();
            stop.run();
        }, duration);
    }

    void cancel() {
        handler.removeCallbacksAndMessages(null);
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        wakeLock = null;
    }
}
