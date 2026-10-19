package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("alarmId", -1L);
        AlarmStore store = new AlarmStore(context);
        AlarmItem item = store.find(id);
        if (item == null || !item.enabled) return;

        long firedAt = item.triggerAtMillis;

        AlarmSoundService.ring(context, item);

        item.lastFiredAtMillis = firedAt;
        AlarmScheduler.schedulePostReminders(context, item, firedAt);

        long now = System.currentTimeMillis() + 1000L;
        long next = RecurrenceUtils.next(
                firedAt,
                item.recurrenceMode,
                item.intervalDays,
                item.customDatesJson,
                now);

        if (next > now) {
            item.triggerAtMillis = next;
            item.enabled = true;
            store.save(item);
            AlarmScheduler.schedule(context, item);
        } else {
            item.enabled = false;
            store.save(item);
        }
        ClockWidgetProvider.updateAll(context);
    }
}
