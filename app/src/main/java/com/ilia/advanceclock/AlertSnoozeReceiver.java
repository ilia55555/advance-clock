package com.ilia.advanceclock;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlertSnoozeReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if(intent==null)return;
        Context application=context.getApplicationContext();
        PendingResult pending=goAsync();
        AlarmReceiverWork.execute(pending,()->AlertSnoozes.deliver(application,intent.getData()));
    }
}
