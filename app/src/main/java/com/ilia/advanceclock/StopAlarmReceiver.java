package com.ilia.advanceclock;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class StopAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("alarmId", -1L);
        context.stopService(AlarmSoundService.stopIntent(context));
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm != null && id >= 0) nm.cancel(NotificationHelper.notificationId(id));
        AlarmRingActivity.finishIfShowing(id);
    }
}
