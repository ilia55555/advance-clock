package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Explicit delivery also refreshes the day icon while the app is not running. */
public final class DateNotificationRefreshReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        DateNotificationService.refreshNow(context);
    }
}
