package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

/** Optional five-second warm-up. It never replaces or fires the real alarm. */
public final class AlarmPreparationReceiver extends BroadcastReceiver {
    static final long LEAD_MILLIS = 5000L;
    static final String CLOCK = "clock";

    static String key(String kind, long id) { return kind + ":" + id; }

    private static PendingIntent operation(Context context, String kind, long id, long deadline, int flags) {
        Intent intent = new Intent(context, AlarmPreparationReceiver.class)
                .setData(Uri.parse("advanceclock://alarm-prepare/" + kind + "/" + id))
                .putExtra("kind", kind).putExtra("alarmId", id).putExtra("deadline", deadline);
        return PendingIntent.getBroadcast(context, 0, intent, flags | PendingIntent.FLAG_IMMUTABLE);
    }

    static void schedule(Context context, String kind, long id, long deadline) {
        cancel(context, kind, id);
        long when = deadline - LEAD_MILLIS;
        if (when <= System.currentTimeMillis()) return;
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager == null || !PermissionHelper.exactAlarmsGranted(context)) return;
        try {
            // Do not consume idle-alarm quotas or advertise an earlier alarm-clock time.
            manager.setExact(AlarmManager.RTC_WAKEUP, when,
                    operation(context, kind, id, deadline, PendingIntent.FLAG_UPDATE_CURRENT));
        } catch (RuntimeException ignored) { /* The real setAlarmClock remains independent. */ }
    }

    static void cancel(Context context, String kind, long id) {
        AlarmAudioEngine.cancelPrepared(key(kind, id));
        PendingIntent existing = operation(context, kind, id, 0L, PendingIntent.FLAG_NO_CREATE);
        if (existing == null) return;
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        if (manager != null) manager.cancel(existing);
        existing.cancel();
    }

    @Override public void onReceive(Context source, Intent intent) {
        if (intent == null) return;
        Context context = source.getApplicationContext();
        PendingResult pending = goAsync();
        AlarmReceiverWork.execute(pending, () -> prepare(context, intent));
    }

    static void prepare(Context context, Intent intent) {
        String kind = intent.getStringExtra("kind");
        long id = intent.getLongExtra("alarmId", -1L);
        long deadline = intent.getLongExtra("deadline", 0L);
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0 || remaining > 10_000L) return;
        String sound = "";
        if (CLOCK.equals(kind)) {
            AlarmItem item = new AlarmStore(context).find(id);
            if (item == null || !((item.enabled && item.triggerAtMillis == deadline)
                    || item.pendingSnoozeAtMillis == deadline)) return;
            sound = item.soundUri;
        } else if (ToolAlarmScheduler.TIMER.equals(kind) || ToolAlarmScheduler.STOPWATCH.equals(kind)) {
            if (context.getSharedPreferences("time_tools", 0).getLong(kind + "_deadline", 0L) != deadline) return;
        } else return;
        NotificationHelper.ensureChannels(context);
        AlarmAudioEngine.prime(context, key(kind, id), sound, deadline);
    }
}
