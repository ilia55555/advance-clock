package com.ilia.advanceclock;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class SoundLibrary {
    static final class Sound {
        final String name;
        final String uri;
        Sound(String name, String uri) { this.name = name; this.uri = uri; }
    }

    private static final String PREFS = "advance_clock_sound_library";
    private static final String KEY = "user_sounds";

    private SoundLibrary() {}

    static List<Sound> all(Context context) {
        ArrayList<Sound> sounds = new ArrayList<>();
        sounds.add(new Sound("پیش‌فرض سیستم", ""));
        addBundledSounds(context, sounds, "adhan", "اذان برنامه", 10);
        addBundledSounds(context, sounds, "alarm", "زنگ برنامه", 10);
        try {
            JSONArray array = new JSONArray(context.getSharedPreferences(PREFS, 0)
                    .getString(KEY, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                String uri = item.optString("uri", "");
                if (!uri.isEmpty()) sounds.add(new Sound(item.optString("name", "صدا"), uri));
            }
        } catch (Exception ignored) {}
        return sounds;
    }

    private static void addBundledSounds(
            Context context, List<Sound> sounds, String baseName, String label, int count) {
        for (int index = 1; index <= count; index++) {
            String resourceName = index == 1 ? baseName : baseName + "_" + index;
            int resource = context.getResources().getIdentifier(
                    resourceName, "raw", context.getPackageName());
            if (resource != 0) sounds.add(new Sound(
                    index == 1 ? label : label + " " + index,
                    "android.resource://" + context.getPackageName() + "/" + resource));
        }
    }

    static void add(Context context, Uri uri) {
        if (uri == null) return;
        String value = uri.toString();
        List<Sound> current = all(context);
        for (Sound sound : current) if (value.equals(sound.uri)) return;
        JSONArray array = new JSONArray();
        for (Sound sound : current) {
            if (sound.uri.isEmpty() || sound.uri.startsWith("android.resource://")) continue;
            JSONObject item = new JSONObject();
            try { item.put("name", sound.name); item.put("uri", sound.uri); array.put(item); }
            catch (Exception ignored) {}
        }
        JSONObject added = new JSONObject();
        try { added.put("name", displayName(context, uri)); added.put("uri", value); array.put(added); }
        catch (Exception ignored) {}
        context.getSharedPreferences(PREFS, 0).edit().putString(KEY, array.toString()).apply();
    }

    static String name(Context context, String uri) {
        for (Sound sound : all(context)) if (sound.uri.equals(uri == null ? "" : uri)) return sound.name;
        return "صدای انتخابی";
    }

    private static String displayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(0);
                if (name != null && !name.trim().isEmpty()) return name;
            }
        } catch (Exception ignored) {}
        return uri.getLastPathSegment() == null ? "صدای کاربر" : uri.getLastPathSegment();
    }
}
