package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        AlarmScheduler.rescheduleAll(context);
        NoForgetScheduler.rescheduleAll(context);
        ClockWidgetProvider.updateAll(context);
        NoForgetWidgetProvider.updateAll(context);
        try { DateNotificationService.start(context); } catch (Exception ignored) {}
    }
}
