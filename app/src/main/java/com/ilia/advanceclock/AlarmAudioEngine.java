package com.ilia.advanceclock;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

/** One silent warm player and one ringing player at most; all audio work stays off the UI thread. */
final class AlarmAudioEngine {
    private static Handler audioHandler;
    private static Session warm, active;

    private static final class Session {
        final String key, uri;
        MediaPlayer player;
        boolean ready, claimed, muted;
        Runnable expiry, timeout;
        Session(String key, String uri) { this.key = key; this.uri = uri; }
    }

    static synchronized Handler handler() {
        if (audioHandler == null) {
            HandlerThread thread = new HandlerThread("alarm-audio", android.os.Process.THREAD_PRIORITY_DEFAULT);
            thread.start();
            audioHandler = new Handler(thread.getLooper());
        }
        return audioHandler;
    }

    private static String resolve(Context context, String selected) {
        String value = selected == null || selected.isEmpty() ? AppSettings.defaultAlarmSoundUri(context) : selected;
        if (value != null && !value.isEmpty()) return value;
        Uri fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (fallback == null) fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        return fallback == null ? "" : fallback.toString();
    }

    static void prime(Context source, String key, String selected, long deadline) {
        Context context = source.getApplicationContext();
        handler().post(() -> {
            if (active != null || deadline <= System.currentTimeMillis()) return;
            String uri = resolve(context, selected);
            release(warm);
            Session session = new Session(key, uri);
            warm = session;
            prepare(context, session);
            if (warm != session) return;
            session.expiry = () -> { if (warm == session) release(session); };
            handler().postDelayed(session.expiry, Math.max(1L, deadline - System.currentTimeMillis()) + 5000L);
        });
    }

    static void play(Context source, String key, String selected) {
        Context context = source.getApplicationContext();
        handler().post(() -> {
            String uri = resolve(context, selected);
            release(active);
            Session session = warm;
            if (session == null || !session.key.equals(key) || !session.uri.equals(uri)) {
                release(warm);
                session = new Session(key, uri);
            } else {
                warm = null;
                if (session.expiry != null) handler().removeCallbacks(session.expiry);
            }
            active = session;
            session.claimed = true;
            if (session.player == null) prepare(context, session);
            else if (session.ready) start(session);
            else armTimeout(session);
        });
    }

    private static void prepare(Context context, Session session) {
        if (session.uri.isEmpty()) { release(session); return; }
        try {
            MediaPlayer player = new MediaPlayer();
            session.player = player;
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            player.setLooping(true);
            player.setOnPreparedListener(value -> {
                if (session != warm && session != active) return;
                session.ready = true;
                if (session.timeout != null) handler().removeCallbacks(session.timeout);
                if (session.claimed) start(session);
            });
            player.setOnErrorListener((value, what, extra) -> { release(session); return true; });
            player.setDataSource(context, Uri.parse(session.uri));
            armTimeout(session);
            player.prepareAsync();
        } catch (Exception ignored) { release(session); }
    }

    private static void armTimeout(Session session) {
        if (session.timeout != null) handler().removeCallbacks(session.timeout);
        session.timeout = () -> { if (!session.ready) release(session); };
        handler().postDelayed(session.timeout, session.claimed ? 8000L : 3000L);
    }

    private static void start(Session session) {
        if (active != session || !session.claimed || !session.ready) return;
        try {
            session.player.setVolume(session.muted ? 0f : 1f, session.muted ? 0f : 1f);
            session.player.start();
        } catch (RuntimeException ignored) { release(session); }
    }

    static void mute(boolean muted) {
        Handler target;
        synchronized (AlarmAudioEngine.class) { target = audioHandler; }
        if (target == null) return;
        target.post(() -> {
            if (active == null) return;
            active.muted = muted;
            if (active.ready) try {
                active.player.setVolume(muted ? 0f : 1f, muted ? 0f : 1f);
            } catch (RuntimeException ignored) {}
        });
    }

    static void cancelPrepared(String key) {
        Handler target;
        synchronized (AlarmAudioEngine.class) { target = audioHandler; }
        if (target != null) target.post(() -> { if (warm != null && warm.key.equals(key)) release(warm); });
    }

    static void stop() {
        Handler target;
        synchronized (AlarmAudioEngine.class) { target = audioHandler; }
        if (target != null) target.post(() -> { release(active); release(warm); });
    }

    private static void release(Session session) {
        if (session == null) return;
        if (session == warm) warm = null;
        if (session == active) active = null;
        if (session.expiry != null) handler().removeCallbacks(session.expiry);
        if (session.timeout != null) handler().removeCallbacks(session.timeout);
        if (session.player != null) {
            try {
                session.player.setOnPreparedListener(null);
                session.player.setOnErrorListener(null);
                session.player.release();
            } catch (RuntimeException ignored) {}
            session.player = null;
        }
    }
}
