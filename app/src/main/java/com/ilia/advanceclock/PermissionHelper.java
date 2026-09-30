package com.ilia.advanceclock;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

public final class PermissionHelper {
    private PermissionHelper() {}

    public static boolean notificationsGranted(Context context) {
        return Build.VERSION.SDK_INT < 33
                || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean exactAlarmsGranted(Context context) {
        if (Build.VERSION.SDK_INT < 31) return true;
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        return manager != null && manager.canScheduleExactAlarms();
    }

    public static boolean fullScreenGranted(Context context) {
        if (Build.VERSION.SDK_INT < 34) return true;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        return manager != null && manager.canUseFullScreenIntent();
    }

    public static boolean allCriticalGranted(Context context) {
        return notificationsGranted(context)
                && exactAlarmsGranted(context)
                && fullScreenGranted(context);
    }

    public static String statusText(Context context) {
        StringBuilder text = new StringBuilder();
        text.append(notificationsGranted(context) ? AppString.get(R.string.runtime_text_0439) : AppString.get(R.string.runtime_text_0440));
        text.append("   ");
        text.append(exactAlarmsGranted(context) ? AppString.get(R.string.runtime_text_0395) : AppString.get(R.string.runtime_text_0396));
        text.append("   ");
        text.append(fullScreenGranted(context) ? AppString.get(R.string.runtime_text_0397) : AppString.get(R.string.runtime_text_0398));
        return text.toString();
    }
}
