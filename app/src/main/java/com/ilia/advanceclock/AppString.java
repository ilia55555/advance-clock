package com.ilia.advanceclock;

import android.content.Context;

import java.util.Locale;

/**
 * Thin access layer for Android string resources.
 *
 * This class does not translate, match, parse, or transform text. Every caller must provide
 * an R.string resource ID; Android then resolves that ID from the currently selected locale.
 */
public final class AppString {
    private static volatile Context appContext;

    private AppString() {}

    public static void init(Context context) {
        if (context != null) appContext = context.getApplicationContext();
    }

    public static String get(int resId) {
        Context context = appContext;
        if (context == null) {
            throw new IllegalStateException("AppString is not initialized");
        }
        return AppSettings.localizedContext(context).getString(resId);
    }

    public static String get(int resId, Object... formatArgs) {
        Context context = appContext;
        if (context == null) {
            throw new IllegalStateException("AppString is not initialized");
        }
        return AppSettings.localizedContext(context).getString(resId, formatArgs);
    }

    public static Locale locale() {
        Context context = appContext;
        if (context == null) {
            throw new IllegalStateException("AppString is not initialized");
        }
        return Locale.forLanguageTag(AppSettings.language(context));
    }
}
