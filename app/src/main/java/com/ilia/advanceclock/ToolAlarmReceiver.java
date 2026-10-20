package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ToolAlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        Context application = context.getApplicationContext();
        PendingResult pending = goAsync();
        AlarmReceiverWork.execute(pending, () -> dispatch(application, intent));
    }

    private void dispatch(Context context, Intent intent) {
        String kind = intent == null ? null : intent.getStringExtra("toolKind");
        if (!ToolAlarmScheduler.TIMER.equals(kind) && !ToolAlarmScheduler.STOPWATCH.equals(kind)) return;
        String label = ToolAlarmScheduler.savedLabel(context, kind);
        Intent service = AlarmSoundService.startIntent(
                context, ToolAlarmScheduler.alarmId(kind), label, true)
                .putExtra("toolKind", kind);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(service);
        else context.startService(service);
        ToolAlarmScheduler.markFired(context, kind);
        TimeToolsWidgetProvider.updateAll(context);
    }
}
