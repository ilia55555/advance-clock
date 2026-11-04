package com.ilia.advanceclock;

import android.content.*;

public final class EventReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        Context application=context.getApplicationContext();
        AlarmReceiverWork.execute(goAsync(),()->EventReminderScheduler.dispatch(application,intent));
    }
}
