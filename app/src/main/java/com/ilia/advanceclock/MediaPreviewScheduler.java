package com.ilia.advanceclock;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.PersistableBundle;

public final class MediaPreviewScheduler {
    private static final int BASE_JOB_ID = 0x4D570000;

    private MediaPreviewScheduler() {}

    public static void schedule(Context context, int widgetId) {
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return;

        JobScheduler scheduler =
                context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;

        PersistableBundle extras = new PersistableBundle();
        extras.putInt("widgetId", widgetId);

        JobInfo job = new JobInfo.Builder(
                jobId(widgetId),
                new ComponentName(
                        context,
                        MediaPreviewJobService.class))
                .setExtras(extras)
                .setOverrideDeadline(0)
                .build();

        try {
            scheduler.schedule(job);
        } catch (Exception ignored) {
        }
    }

    public static void cancel(Context context, int widgetId) {
        JobScheduler scheduler =
                context.getSystemService(JobScheduler.class);
        if (scheduler == null) return;
        try {
            scheduler.cancel(jobId(widgetId));
        } catch (Exception ignored) {
        }
    }

    private static int jobId(int widgetId) {
        return BASE_JOB_ID ^ widgetId;
    }
}
