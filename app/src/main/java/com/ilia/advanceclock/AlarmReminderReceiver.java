package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Replays the configured alarm without advancing its recurrence or creating new reminders. */
public final class AlarmReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Context application = context.getApplicationContext();
        PendingResult pending = goAsync();
        AlarmReceiverWork.execute(pending, () -> dispatch(application, intent));
    }

    private void dispatch(Context context, Intent intent) {
        if (intent == null) return;
        long alarmId = intent.getLongExtra("alarmId", -1L);
        int minutes = intent.getIntExtra("delayMinutes", 0);
        long occurrence = intent.getLongExtra("firedAtMillis", 0L);
        AlarmItem item = new AlarmStore(context).find(alarmId);
        if (!AppSettings.alarmReminderNotificationsEnabled(context)
                || !AlarmReminderUtils.isScheduledReminder(item, occurrence, minutes)) return;

        // Use exactly the same sound, vibration, fullscreen UI and attachments as the primary alarm.
        // POST_NOTIFICATIONS controls notification visibility, not alarm audio playback.
        AlarmSoundService.ring(context, item);
    }
}
