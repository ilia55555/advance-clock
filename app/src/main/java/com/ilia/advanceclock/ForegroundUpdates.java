package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Context;
import android.net.*;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;

/** No scheduled work, connectivity listener, notification or automatic APK download. */
final class ForegroundUpdates {
    static final class Request {
        final WeakReference<Activity> host;
        volatile boolean cancelled;
        volatile HttpURLConnection connection;
        Thread thread;
        Request(Activity host) { this.host = new WeakReference<>(host); }
        void cancel() {
            cancelled = true;
            if (thread != null) thread.interrupt();
            HttpURLConnection conn = connection;
            if (conn != null) new Thread(conn::disconnect, "cancel-release-check").start();
        }
    }
    private static Request active;
    static void start(Activity host) {
        stop(host);
        Context c = host.getApplicationContext();
        AppUpdates.cancelLegacyJobs(c);
        String repository = AppUpdates.repository(c);
        if (!repository.equals(AppUpdates.prefs(c).getString("source", "")))
            AppUpdates.prefs(c).edit().remove("release").remove("attempt").remove("shown_code").putString("source", repository).apply();
        if (!AppUpdates.production(c) || !AppUpdates.validRepository(repository) || !online(c)) return;
        Request request = new Request(host); active = request;
        Runnable deliver = () -> {
            Activity activity = request.host.get();
            if (active == request && !request.cancelled && activity != null && !activity.isFinishing() && !activity.isDestroyed())
                UpdatePrompt.show(activity, AppUpdates.available(c), false);
        };
        if (!AppUpdates.due(System.currentTimeMillis(), AppUpdates.prefs(c).getLong("attempt", 0))) {
            host.getWindow().getDecorView().post(deliver); return;
        }
        request.thread = new Thread(() -> {
            AppUpdates.check(c, request, repository);
            new android.os.Handler(android.os.Looper.getMainLooper()).post(deliver);
        }, "foreground-release-check");
        request.thread.start();
    }
    static void stop(Activity host) {
        if (active != null) { active.cancel(); active = null; }
        UpdatePrompt.dismiss(host);
    }
    static boolean online(Context c) {
        ConnectivityManager manager = c.getSystemService(ConnectivityManager.class);
        if (manager == null) return false;
        NetworkCapabilities caps = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
