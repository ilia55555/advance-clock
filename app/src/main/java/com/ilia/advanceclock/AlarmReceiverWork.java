package com.ilia.advanceclock;

import android.content.BroadcastReceiver;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Finish goAsync broadcasts reliably while keeping preference, recurrence and widget work off UI. */
final class AlarmReceiverWork {
    private static final ExecutorService WORK = Executors.newFixedThreadPool(2, task -> {
        Thread thread = new Thread(task, "alarm-dispatch");
        thread.setDaemon(true);
        return thread;
    });
    static void execute(BroadcastReceiver.PendingResult pending, Runnable task) {
        try {
            WORK.execute(() -> {
                try { task.run(); }
                catch (RuntimeException error) { android.util.Log.e("AlarmDispatch", "Alarm task failed", error); }
                finally { if (pending != null) pending.finish(); }
            });
        } catch (RuntimeException error) {
            if (pending != null) pending.finish();
            throw error;
        }
    }
}
