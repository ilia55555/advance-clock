package com.ilia.advanceclock;

import android.app.job.*;
import java.util.concurrent.ConcurrentHashMap;

public final class UpdateCheckJob extends JobService {
    private final ConcurrentHashMap<Integer, Thread> workers = new ConcurrentHashMap<>();
    @Override public boolean onStartJob(JobParameters params) {
        Thread worker = new Thread(() -> {
            try { AppUpdates.check(getApplicationContext()); }
            finally { workers.remove(params.getJobId()); jobFinished(params, false); }
        }, "periodic-release-check");
        workers.put(params.getJobId(), worker);
        worker.start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        Thread worker = workers.remove(params.getJobId());
        if (worker != null) worker.interrupt();
        return true;
    }
}
