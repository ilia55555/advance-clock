package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public final class AdvanceClockApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppSettings.applyLanguage(this);
        MainNoteTabEnhancer.install(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override public void onActivityCreated(Activity activity, Bundle state) {
                fixPrayerArrows(activity);
                NoteComposerVisibilityController.apply(activity);
            }

            @Override public void onActivityResumed(Activity activity) {
                fixPrayerArrows(activity);
                NoteComposerVisibilityController.apply(activity);
            }

            @Override public void onActivityStarted(Activity activity) {}
            @Override public void onActivityPaused(Activity activity) {}
            @Override public void onActivityStopped(Activity activity) {}
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}
            @Override public void onActivityDestroyed(Activity activity) {
                NoteComposerVisibilityController.forget(activity);
            }
        });
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
