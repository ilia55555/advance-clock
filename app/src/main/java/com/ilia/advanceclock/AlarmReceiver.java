package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("alarmId", -1L);
        AlarmStore store = new AlarmStore(context);
        AlarmItem item = store.find(id);
        if (item == null || !item.enabled) return;

        Intent service = AlarmSoundService.startIntent(context, item.id, item.label);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
        else context.startService(service);

        if (item.repeatType == AlarmItem.REPEAT_NONE) {
            item.enabled = false;
            store.save(item);
        } else {
            item.triggerAtMillis = TimeUtils.nextOccurrence(item.triggerAtMillis, item.repeatType);
            item.triggerAtMillis = TimeUtils.normalizeFuture(item.triggerAtMillis, item.repeatType, System.currentTimeMillis());
            store.save(item);
            AlarmScheduler.schedule(context, item);
        }
        ClockWidgetProvider.updateAll(context);
    }
}
