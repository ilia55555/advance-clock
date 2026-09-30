package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.WeakHashMap;

public final class AdvanceClockApplication extends Application {
    private static final WeakHashMap<Activity, Boolean> OPEN_ACTIVITIES = new WeakHashMap<>();
    private static boolean refreshScheduled;

    @Override public void onCreate() {
        super.onCreate();
        AppSettings.applyLanguage(this);
        UiText.init(this);
        PrayerTimeZoneRepair.repairIfNeeded(this);
        MainNoteTabEnhancer.install(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle state) {
                synchronized (OPEN_ACTIVITIES) { OPEN_ACTIVITIES.put(activity, true); }
                fixPrayerArrows(activity);
                NoteComposerVisibilityController.apply(activity);
                UiText.install(activity);
            }

            @Override public void onActivityResumed(Activity activity) {
                fixPrayerArrows(activity);
                NoteComposerVisibilityController.apply(activity);
                UiText.install(activity);
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {
                synchronized (OPEN_ACTIVITIES) { OPEN_ACTIVITIES.remove(activity); }
                NoteComposerVisibilityController.forget(activity);
                UiText.uninstall(activity);
            }
        });
    }

    /**
     * Recreates already-open screens so locale, palette, calendar/layout and tab changes become
     * visible immediately instead of waiting for the app to be reopened.
     */
    static void refreshOpenActivities(Activity source, boolean includeSource) {
        synchronized (OPEN_ACTIVITIES) {
            if (refreshScheduled) return;
            refreshScheduled = true;
        }
        new Handler(Looper.getMainLooper()).post(() -> {
            ArrayList<Activity> snapshot;
            synchronized (OPEN_ACTIVITIES) {
                snapshot = new ArrayList<>(OPEN_ACTIVITIES.keySet());
            }
            // Recreate background screens first; the settings screen (source) last.
            for (Activity activity : snapshot) {
                if (activity == null || activity == source || activity.isFinishing()
                        || activity.isDestroyed()) continue;
                AppSettings.applyLanguage(activity);
                activity.recreate();
            }
            if (includeSource && source != null
                    && !source.isFinishing() && !source.isDestroyed()) {
                AppSettings.applyLanguage(source);
                source.recreate();
            }
            synchronized (OPEN_ACTIVITIES) { refreshScheduled = false; }
        });
    }

    /**
     * Rebuild the whole Activity task after a locale change. Recreating individual Activities
     * leaves windows from the old configuration in the back stack on some Android versions;
     * starting a fresh task guarantees that every Context is created with the selected locale.
     */
    static void restartForLanguage(Activity source) {
        if (source == null || source.isFinishing() || source.isDestroyed()) return;
        AppSettings.applyLanguage(source.getApplicationContext());
        AppSettings.applyLanguage(source);
        UiText.invalidate();
        Intent restart = new Intent(source, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        source.startActivity(restart);
        source.overridePendingTransition(0, 0);
    }

    private static void fixPrayerArrows(Activity activity) {
        if (!(activity instanceof MainActivity)) return;
        TextView left = activity.findViewById(R.id.prayer_scroll_left);
        TextView right = activity.findViewById(R.id.prayer_scroll_right);
        if (left != null) {
            left.setText("<");
            left.setTextDirection(View.TEXT_DIRECTION_LTR);
            left.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
        if (right != null) {
            right.setText(">");
            right.setTextDirection(View.TEXT_DIRECTION_LTR);
            right.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        }
    }
}
