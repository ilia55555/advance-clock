package com.ilia.advanceclock;

import android.app.job.JobParameters;
import android.app.job.JobService;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public final class MediaPreviewJobService extends JobService {
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    private final Map<Integer, Future<?>> running =
            new ConcurrentHashMap<>();

    private final Set<Integer> stopped =
            ConcurrentHashMap.newKeySet();

    @Override public boolean onStartJob(JobParameters params) {
        final int widgetId = params.getExtras().getInt(
                "widgetId",
                android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID);

        if (widgetId
                == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
            return false;
        }

        final int jobId = params.getJobId();
        stopped.remove(jobId);

        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                MediaPreviewCache.prepareWidget(
                        getApplicationContext(),
                        widgetId);
            } finally {
                running.remove(jobId);

                // Once onStopJob() is called, the system owns the reschedule
                // decision. Calling jobFinished() afterward is incorrect.
                if (!stopped.remove(jobId)) {
                    jobFinished(params, false);
                }
            }
            return null;
        });

        running.put(jobId, task);
        executor.execute(task);
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        int jobId = params.getJobId();
        stopped.add(jobId);

        Future<?> future = running.remove(jobId);
        if (future != null) {
            future.cancel(true);
        }

        return true;
    }

    @Override public void onDestroy() {
        for (Future<?> future : running.values()) {
            future.cancel(true);
        }
        running.clear();
        executor.shutdownNow();
        super.onDestroy();
    }
}
