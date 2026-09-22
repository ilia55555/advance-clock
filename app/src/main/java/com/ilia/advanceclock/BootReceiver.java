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

        // Refresh/start only when the user has enabled the persistent date
        // notification. Otherwise make sure an old service/notification is gone.
        if (AppSettings.persistentDateNotificationEnabled(context)) {
            DateNotificationService.refreshNow(context);
        } else {
            DateNotificationService.stop(context);
        }

        String action = intent == null ? null : intent.getAction();
        if ((Intent.ACTION_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action))
                && AppSettings.persistentDateNotificationEnabled(context)) {
            try {
                DateNotificationService.start(context);
            } catch (Exception ignored) {
            }
        }
    }
}
