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

        // Updating the existing notification directly is allowed from this
        // receiver and makes a manual date/time change visible immediately.
        DateNotificationService.refreshNow(context);

        String action = intent == null ? null : intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            try {
                DateNotificationService.start(context);
            } catch (Exception ignored) {
            }
        }
    }
}
