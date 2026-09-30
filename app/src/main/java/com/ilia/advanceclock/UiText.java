package com.ilia.advanceclock;

import android.content.Context;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Localization bridge for legacy screens that historically used hard-coded Persian UI strings.
 * New code should prefer normal Android string resources. The catalog itself is stored in the
 * locale-specific resource files, so the app remains fully offline.
 */
public final class UiText {
    private static volatile Context appContext;
    private static final Object CACHE_LOCK = new Object();
    private static String cachedLanguage = "";
    private static Map<String, String> cachedTranslations = Collections.emptyMap();

    private UiText() {}

    public static void init(Context context) {
        if (context != null) appContext = context.getApplicationContext();
        invalidate();
    }

    public static void invalidate() {
        synchronized (CACHE_LOCK) {
            cachedLanguage = "";
            cachedTranslations = Collections.emptyMap();
        }
    }

    public static String tr(String source) {
        Context context = appContext;
        return context == null ? source : tr(context, source);
    }

    public static String tr(Context context, String source) {
        if (source == null || source.isEmpty() || context == null) return source;
        String translation = catalog(context).get(source);
        if (translation != null) return translation;

        String trimmed = source.trim();
        if (!trimmed.equals(source)) {
            translation = catalog(context).get(trimmed);
            if (translation != null) {
                int start = source.indexOf(trimmed);
                String before = start > 0 ? source.substring(0, start) : "";
                int end = start + trimmed.length();
                String after = end < source.length() ? source.substring(end) : "";
                return before + translation + after;
            }
        }
        return source;
    }

    public static String[] translateArray(String... values) {
        if (values == null) return new String[0];
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) out[i] = tr(values[i]);
        return out;
    }

    public static String[] paletteNames(Context context) {
        if (context == null) context = appContext;
        if (context == null) {
            return new String[]{"نارنجی", "بنفش", "صورتی", "سبز", "بنفش روشن", "صورتی روشن", "آبی"};
        }
        return context.getResources().getStringArray(R.array.palette_names);
    }

    private static Map<String, String> catalog(Context context) {
        ensureCatalog(context);
        return cachedTranslations;
    }

    private static void ensureCatalog(Context context) {
        String language = AppSettings.language(context);
        synchronized (CACHE_LOCK) {
            if (language.equals(cachedLanguage) && !cachedTranslations.isEmpty()) return;
            String[] sources = context.getResources().getStringArray(R.array.runtime_source_fa);
            String[] localized = context.getResources().getStringArray(R.array.runtime_translation);
            HashMap<String, String> translations = new HashMap<>();
            int entryCount = Math.min(sources.length, localized.length);
            for (int i = 0; i < entryCount; i++) {
                if (sources[i] != null && !sources[i].isEmpty()) {
                    translations.put(sources[i], localized[i]);
                }
            }
            cachedLanguage = language;
            cachedTranslations = translations;
        }
    }
}
