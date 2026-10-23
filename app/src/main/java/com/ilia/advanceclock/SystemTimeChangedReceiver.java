package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Only protected Android clock broadcasts; no timer or polling service. */
public final class SystemTimeChangedReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();
        if (!Intent.ACTION_TIME_CHANGED.equals(action)
                && !Intent.ACTION_TIMEZONE_CHANGED.equals(action)
                && !Intent.ACTION_DATE_CHANGED.equals(action)) return;
        DateNotificationService.refreshNow(context);
        AlarmReceiverWork.execute(goAsync(), () -> new BootReceiver().onReceive(context, intent));
    }
}
