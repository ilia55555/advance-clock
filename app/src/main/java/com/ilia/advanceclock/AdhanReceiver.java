package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class AdhanReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        int type = intent == null
                ? AdhanScheduler.FAJR
                : intent.getIntExtra("adhanType", AdhanScheduler.FAJR);
        Intent service = AdhanSoundService.startIntent(context, type);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
        AdhanScheduler.scheduleNext(context, type);
    }
}
