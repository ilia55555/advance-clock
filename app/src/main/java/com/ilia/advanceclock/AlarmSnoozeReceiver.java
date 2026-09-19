package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class AlarmSnoozeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long alarmId = intent.getLongExtra("alarmId", -1L);
        AlarmItem item = new AlarmStore(context).find(alarmId);
        if (item == null) return;

        Intent service = AlarmSoundService.startIntent(
                context,
                item.id,
                item.label,
                item.vibrate);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
    }
}
