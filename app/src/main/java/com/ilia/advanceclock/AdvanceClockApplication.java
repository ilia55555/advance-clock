package com.ilia.advanceclock;

import android.app.Application;

public final class AdvanceClockApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        AppSettings.applyLanguage(this);
    }
}
