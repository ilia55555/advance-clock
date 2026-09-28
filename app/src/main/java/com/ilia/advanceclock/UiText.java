package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

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
    private static List<String> cachedKeysByLength = Collections.emptyList();
    private static final WeakHashMap<Activity, ViewTreeObserver.OnGlobalLayoutListener> LISTENERS =
            new WeakHashMap<>();
    private static final WeakHashMap<Activity, Boolean> LOCALIZING = new WeakHashMap<>();

    private UiText() {}

    public static void init(Context context) {
        if (context != null) appContext = context.getApplicationContext();
        invalidate();
    }

    public static void invalidate() {
        synchronized (CACHE_LOCK) {
            cachedLanguage = "";
            cachedTranslations = Collections.emptyMap();
            cachedKeysByLength = Collections.emptyList();
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

    /** Translate completed labels while keeping dynamic numbers and separators around known text. */
    public static String trComposite(Context context, String source) {
        if (source == null || source.isEmpty() || context == null) return source;
        String exact = tr(context, source);
        if (!exact.equals(source)) return exact;

        String result = source;
        Map<String, String> translations = catalog(context);
        for (String key : keysByLength(context)) {
            if (key.length() < 2 || !result.contains(key)) continue;
            String translated = translations.get(key);
            if (translated == null) continue;
            if (!key.equals(translated)) result = result.replace(key, translated);
        }
        return result;
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

    public static void install(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        final View root = activity.getWindow().getDecorView();
        if (root == null) return;

        root.post(() -> localizeTree(activity, root));
        synchronized (LISTENERS) {
            if (LISTENERS.containsKey(activity)) return;
            ViewTreeObserver.OnGlobalLayoutListener listener = () -> {
                Boolean running = LOCALIZING.get(activity);
                if (Boolean.TRUE.equals(running)) return;
                root.post(() -> localizeTree(activity, root));
            };
            LISTENERS.put(activity, listener);
            if (root.getViewTreeObserver().isAlive())
                root.getViewTreeObserver().addOnGlobalLayoutListener(listener);
        }
    }

    public static void uninstall(Activity activity) {
        if (activity == null || activity.getWindow() == null) return;
        View root = activity.getWindow().getDecorView();
        synchronized (LISTENERS) {
            ViewTreeObserver.OnGlobalLayoutListener listener = LISTENERS.remove(activity);
            LOCALIZING.remove(activity);
            if (listener != null && root != null && root.getViewTreeObserver().isAlive())
                root.getViewTreeObserver().removeOnGlobalLayoutListener(listener);
        }
    }

    private static void localizeTree(Activity activity, View root) {
        if (activity == null || root == null || activity.isFinishing()) return;
        synchronized (LISTENERS) {
            if (Boolean.TRUE.equals(LOCALIZING.get(activity))) return;
            LOCALIZING.put(activity, true);
        }
        try {
            localizeView(activity, root);
        } finally {
            synchronized (LISTENERS) { LOCALIZING.put(activity, false); }
        }
    }

    private static void localizeView(Context context, View view) {
        CharSequence description = view.getContentDescription();
        if (!TextUtils.isEmpty(description)) {
            String old = description.toString();
            String value = trComposite(context, old);
            if (!old.equals(value)) view.setContentDescription(value);
        }

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            CharSequence hint = textView.getHint();
            if (!TextUtils.isEmpty(hint)) {
                String old = hint.toString();
                String value = trComposite(context, old);
                if (!old.equals(value)) textView.setHint(value);
            }
            // Never rewrite user-entered text in editable fields; only their hint/description.
            if (!(view instanceof EditText)) {
                CharSequence text = textView.getText();
                if (!TextUtils.isEmpty(text)) {
                    String old = text.toString();
                    String value = trComposite(context, old);
                    if (!old.equals(value)) textView.setText(value);
                }
            }
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++)
                localizeView(context, group.getChildAt(i));
        }
    }

    private static Map<String, String> catalog(Context context) {
        ensureCatalog(context);
        return cachedTranslations;
    }

    private static List<String> keysByLength(Context context) {
        ensureCatalog(context);
        return cachedKeysByLength;
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
            ArrayList<String> keys = new ArrayList<>(translations.keySet());
            keys.sort(Comparator.comparingInt(String::length).reversed());
            cachedLanguage = language;
            cachedTranslations = translations;
            cachedKeysByLength = keys;
        }
    }
}
