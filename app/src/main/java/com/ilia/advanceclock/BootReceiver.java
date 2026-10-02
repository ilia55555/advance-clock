package com.ilia.advanceclock;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public final class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action =
                intent == null
                        ? null
                        : intent.getAction();

        boolean bootOrUpgrade =
                Intent.ACTION_BOOT_COMPLETED.equals(action)
                        || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action);
        boolean clockChanged =
                Intent.ACTION_TIME_CHANGED.equals(action)
                        || Intent.ACTION_TIMEZONE_CHANGED.equals(action);
        boolean dateChanged =
                Intent.ACTION_DATE_CHANGED.equals(action);

        if (bootOrUpgrade || clockChanged || action == null) {
            AlarmScheduler.rescheduleAll(context);
            ToolAlarmScheduler.rescheduleAll(context);
            NoForgetScheduler.rescheduleAll(context);
            AdhanScheduler.rescheduleAll(context);

            ClockWidgetProvider.updateAll(context);
            CalendarWidgetProvider.updateAll(context);
            TimeToolsWidgetProvider.updateAll(context);
            WorldClockWidgetProvider.updateAll(context);
            NoForgetWidgetProvider.updateAll(context);
            PrayerTimesWidgetProvider.updateAll(context);
        } else if (dateChanged) {
            // Midnight only changes date-oriented surfaces. AlarmManager entries
            // already remain valid, so do not cancel/recreate every exact alarm.
            ClockWidgetProvider.updateAll(context);
            CalendarWidgetProvider.updateAll(context);
            WorldClockWidgetProvider.updateAll(context);
            NoForgetWidgetProvider.updateAll(context);
            PrayerTimesWidgetProvider.updateAll(context);
        }

        if (bootOrUpgrade) {
            MediaWidgetProvider.updateAll(context);

            AppWidgetManager widgetManager =
                    AppWidgetManager.getInstance(context);
            int[] mediaIds =
                    widgetManager.getAppWidgetIds(
                            new ComponentName(
                                    context,
                                    MediaWidgetProvider.class));
            for (int mediaId : mediaIds) {
                MediaPreviewScheduler.schedule(
                        context,
                        mediaId);
            }
        }

        // The date notification no longer needs a foreground service or a
        // minute-by-minute ticker. These system broadcasts are sufficient.
        if (AppSettings.persistentDateNotificationEnabled(context)) {
            DateNotificationService.refreshNow(context);
        } else {
            DateNotificationService.stop(context);
        }
    }
}
