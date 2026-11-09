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
        if(type<0 || type>4)return;
        if(intent!=null && intent.getBooleanExtra("munajatBefore",false)){
            long prayerAt=intent.getLongExtra("prayerAt",0),now=System.currentTimeMillis();MunajatConfig before=MunajatConfig.load(context,type,true);
            if(!AdhanScheduler.isEnabled(context,type) || !before.playable()
                    || now>=prayerAt || now<prayerAt-before.minutes*60_000L || AppSettings.isAdhanSuppressedNow(context,type))return;
            Intent service=AdhanSoundService.command(context,AdhanSoundService.ACTION_MUNAJAT_BEFORE).putExtra("adhanType",type).putExtra("prayerAt",prayerAt);
            if(Build.VERSION.SDK_INT>=26)context.startForegroundService(service);else context.startService(service);return;
        }

        if (AppSettings.isAdhanSuppressedNow(context, type)) {
            AdhanScheduler.scheduleNext(context, type);
            return;
        }

        if (!AppSettings.adhanSound(context, type)
                && !AppSettings.adhanVibrate(context, type)
                && !AppSettings.adhanNotification(context, type)
                && !AppSettings.adhanFullscreenLocked(context, type)
                && !AppSettings.adhanFullscreenUnlocked(context, type)
                && !MunajatConfig.load(context,type,false).playable()) {
            AdhanScheduler.scheduleNext(context, type);
            return;
        }
        Intent service = AdhanSoundService.startIntent(context, type);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(service);
        } else {
            context.startService(service);
        }
        Context application=context.getApplicationContext();
        AlarmReceiverWork.execute(goAsync(),()->AdhanScheduler.scheduleNext(application,type));
    }
}
