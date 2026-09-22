package com.ilia.advanceclock;

import android.app.job.JobParameters;
import android.app.job.JobService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MediaPreviewJobService extends JobService {
    private final ExecutorService executor =
            Executors.newSingleThreadExecutor();

    @Override public boolean onStartJob(JobParameters params) {
        final int widgetId = params.getExtras().getInt(
                "widgetId",
                android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID);

        if (widgetId == android.appwidget.AppWidgetManager.INVALID_APPWIDGET_ID) {
            return false;
        }

        executor.execute(() -> {
            try {
                MediaPreviewCache.prepareWidget(
                        getApplicationContext(),
                        widgetId);
            } finally {
                jobFinished(params, false);
            }
        });
        return true;
    }

    @Override public boolean onStopJob(JobParameters params) {
        return true;
    }

    @Override public void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }
}
