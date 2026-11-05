package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Context application = context.getApplicationContext();
        PendingResult pending = goAsync();
        AlarmReceiverWork.execute(pending, () -> dispatch(application, intent));
    }

    private void dispatch(Context context, Intent intent) {
        synchronized (AlarmStore.transactionLock()) {
            long id = intent.getLongExtra("alarmId", -1L);
            AlarmStore store = new AlarmStore(context);
            AlarmItem item = store.find(id);
            if (item == null || !item.enabled) return;

            long firedAt = item.triggerAtMillis;
            if(firedAt>System.currentTimeMillis()+1000 || OccurrenceSkips.contains(context,"alarm",id,firedAt))return;

            AlarmSoundService.ring(context, item);

            item.lastFiredAtMillis = firedAt;
            AlarmScheduler.schedulePostReminders(context, item, firedAt);

            long now = System.currentTimeMillis() + 1000L;
            long next = OccurrenceSkips.next(context,"alarm",id,
                    firedAt,
                    item.recurrenceMode,
                    item.intervalDays,
                    item.customDatesJson,
                    now,item.timeZone());

            if (next > now) {
                item.triggerAtMillis = next;
                item.enabled = true;
                store.save(item);
                AlarmScheduler.schedule(context, item);
            } else {
                item.enabled = false;
                store.save(item);
            }
        }
        ClockWidgetProvider.updateAll(context);

    }
}
