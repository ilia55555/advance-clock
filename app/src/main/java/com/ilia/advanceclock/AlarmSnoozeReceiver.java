package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmSnoozeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Context application = context.getApplicationContext();
        PendingResult pending = goAsync();
        AlarmReceiverWork.execute(pending, () -> dispatch(application, intent));
    }

    private void dispatch(Context context, Intent intent) {
        long alarmId = intent.getLongExtra("alarmId", -1L);
        AlarmItem item = new AlarmStore(context).find(alarmId);
        if (item == null) return;

        AlarmSoundService.ring(context, item);
        item.pendingSnoozeAtMillis = 0L;
        new AlarmStore(context).save(item);
    }
}
