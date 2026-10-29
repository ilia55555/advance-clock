package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restores an enabled date card after a user dismissal, without polling. */
public final class DateNotificationDismissedReceiver extends BroadcastReceiver {
    static final String ACTION = "com.ilia.advanceclock.DATE_CARD_DISMISSED";
    @Override public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION.equals(intent.getAction())) DateNotificationService.refreshNow(context);
    }
}
