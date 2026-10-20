package com.ilia.advanceclock;

import android.content.Context;

import java.util.Locale;

/**
 * Thin access layer for Android string resources.
 *
 * This class does not translate, match, parse, or transform text. Every caller must provide
 * a string or string-array resource ID; Android resolves it in the currently selected locale.
 */
public final class AppString {
    private static volatile Context appContext;
    private static volatile Context localized;
    private static volatile String language;

    private AppString() {}

    public static void init(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
            language = AppSettings.language(appContext);
            localized = AppSettings.localizedContext(appContext);
        }
    }

    public static String get(int resId) {
        Context context = appContext;
        if (context == null) {
            throw new IllegalStateException("AppString is not initialized");
        }
        return resourcesContext(context).getString(resId);
    }

    public static String get(int resId, Object... formatArgs) {
        Context context = appContext;
        if (context == null) {
            throw new IllegalStateException("AppString is not initialized");
        }
        return resourcesContext(context).getString(resId, formatArgs);
    }

    public static String[] getArray(int resId) {
        Context context = appContext;
        if (context == null) {
            throw new IllegalStateException("AppString is not initialized");
        }
        return resourcesContext(context).getResources().getStringArray(resId);
    }

    private static Context resourcesContext(Context context) {
        String current = AppSettings.language(context);
        if (localized == null || !current.equals(language)) {
            synchronized (AppString.class) {
                localized = AppSettings.localizedContext(context);
                language = current;
            }
        }
        return localized;
    }

    public static Locale locale() {
        Context context = appContext;
        if (context == null) {
            throw new IllegalStateException("AppString is not initialized");
        }
        return Locale.forLanguageTag(AppSettings.language(context));
    }
}
