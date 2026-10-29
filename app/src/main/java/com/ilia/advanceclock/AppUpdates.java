package com.ilia.advanceclock;

import android.app.job.*;
import android.content.*;
import android.content.pm.*;
import android.os.Build;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Public release metadata is untrusted. Only a newer APK with our installed signer is exported. */
final class AppUpdates {
    static final String REPO = "https://github.com/ilia55555/advance-clock";
    static final String METADATA = REPO + "/releases/latest/download/update.json";
    static final long INTERVAL = 3L * 24 * 60 * 60 * 1000;
    static final long MAX_APK = 150L * 1024 * 1024;
    static final int JOB_ID = 8204;
    static final int DUE_JOB_ID = 8205;
    private static final AtomicBoolean CHECKING = new AtomicBoolean();

    static final class Release {
        final long code, size;
        final String version, hash, tag;
        Release(String json) throws Exception {
            JSONObject o = new JSONObject(json);
            code = o.getLong("versionCode"); size = o.getLong("size");
            version = o.getString("versionName"); hash = o.getString("sha256"); tag = o.getString("tag");
            if (code <= 0 || code > 2100000000L || version.length() > 40 || size <= 0 || size > MAX_APK || !version.matches("[0-9]+(?:\\.[0-9]+){3}")
                    || !hash.matches("[a-f0-9]{64}") || !tag.equals("v" + version))
                throw new IOException("Invalid release metadata");
        }
        String url() { return REPO + "/releases/download/" + tag + "/InfiniTime.apk"; }
    }
    static android.content.SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("app_updates", Context.MODE_PRIVATE);
    }
    static boolean production(Context c) { return c.getPackageName().equals("com.ilia.advanceclock"); }
    static long code(PackageInfo p) { return Build.VERSION.SDK_INT >= 28 ? p.getLongVersionCode() : p.versionCode; }
    static PackageInfo installed(Context c) throws Exception {
        return c.getPackageManager().getPackageInfo(c.getPackageName(), flags());
    }
    private static int flags() { return Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES; }
    static Release available(Context c) {
        if (!production(c)) return null;
        try {
            long age = System.currentTimeMillis() - prefs(c).getLong("success", 0);
            if (age < 0 || age > 7L * 24 * 60 * 60 * 1000) return null;
            Release r = new Release(prefs(c).getString("release", ""));
            return r.code > code(installed(c)) ? r : null;
        } catch (Exception e) { return null; }
    }
    static void start(Context context) {
        Context c = context.getApplicationContext();
        if (!production(c)) return;
        JobScheduler jobs = c.getSystemService(JobScheduler.class);
        if (jobs != null && jobs.getPendingJob(JOB_ID) == null) {
            jobs.schedule(new JobInfo.Builder(JOB_ID, new ComponentName(c, UpdateCheckJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPersisted(true)
                    .setPeriodic(INTERVAL, 6L * 60 * 60 * 1000).build());
        }
        if (jobs != null && due(System.currentTimeMillis(), prefs(c).getLong("attempt", 0), prefs(c).getBoolean("last_ok", false))
                && jobs.getPendingJob(DUE_JOB_ID) == null) {
            jobs.schedule(new JobInfo.Builder(DUE_JOB_ID, new ComponentName(c, UpdateCheckJob.class))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(0).build());
        }
    }
    static void check(Context c) {
        if (!production(c) || !CHECKING.compareAndSet(false, true)) return;
        try {
            long now = System.currentTimeMillis(), last = prefs(c).getLong("attempt", 0);
            if (!due(now, last, prefs(c).getBoolean("last_ok", false))) return;
            prefs(c).edit().putLong("attempt", now).putBoolean("last_ok", false).apply();
            HttpURLConnection conn = open(METADATA);
            try {
                if (conn.getResponseCode() == 404) {
                    prefs(c).edit().remove("release").putLong("success", now).putBoolean("last_ok", true).apply();
                    return; // No public release yet.
                }
                if (conn.getResponseCode() != 200) throw new IOException("Metadata unavailable");
                String json;
                try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] b = new byte[4096]; int n; long started = android.os.SystemClock.elapsedRealtime();
                    while ((n = in.read(b)) != -1) {
                        if (Thread.currentThread().isInterrupted() || android.os.SystemClock.elapsedRealtime() - started > 60000) throw new IOException("Metadata timeout");
                        if (out.size() + n > 16384) throw new IOException("Metadata too large"); out.write(b, 0, n); }
                    json = out.toString("UTF-8");
                }
                new Release(json);
                prefs(c).edit().putString("release", json).putLong("success", now).putBoolean("last_ok", true).apply();
            } finally { conn.disconnect(); }
        } catch (Exception ignored) { /* Offline must never interrupt clocks or alarms. */ }
        finally { CHECKING.set(false); }
    }
    static boolean due(long now, long last, boolean lastOk) {
        long delay = lastOk ? INTERVAL : 24L * 60 * 60 * 1000;
        return last <= 0 || now < last || now - last >= delay;
    }
    static boolean allowed(URL u) {
        if (!"https".equals(u.getProtocol()) || u.getUserInfo() != null || (u.getPort() != -1 && u.getPort() != 443)) return false;
        String host = u.getHost();
        return host.equals("github.com") || host.equals("release-assets.githubusercontent.com")
                || host.equals("objects.githubusercontent.com");
    }
    static HttpURLConnection open(String address) throws Exception {
        URL url = new URL(address);
        for (int i = 0; i < 5; i++) {
            if (!allowed(url)) throw new IOException("Untrusted download host");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "InfiniTime-Update");
            c.setRequestProperty("Accept-Encoding", "identity");
            int status = c.getResponseCode();
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String next = c.getHeaderField("Location"); c.disconnect();
                if (next == null) throw new IOException("Missing redirect");
                url = new URL(url, next);
            } else return c;
        }
        throw new IOException("Too many redirects");
    }
    static File download(Context c, Release r) throws Exception {
        File file = File.createTempFile("update-", ".apk", c.getCacheDir());
        boolean valid = false;
        try {
            HttpURLConnection conn = open(r.url());
            try {
                if (conn.getResponseCode() != 200) throw new IOException("Download unavailable");
                MessageDigest hash = MessageDigest.getInstance("SHA-256");
                long count = 0, started = android.os.SystemClock.elapsedRealtime();
                try (InputStream in = conn.getInputStream(); OutputStream out = new FileOutputStream(file)) {
                    byte[] b = new byte[32768]; int n;
                    while ((n = in.read(b)) != -1) {
                        count += n;
                        if (Thread.currentThread().isInterrupted() || count > r.size
                                || android.os.SystemClock.elapsedRealtime() - started > 10 * 60 * 1000L) throw new IOException("Download limit");
                        hash.update(b, 0, n); out.write(b, 0, n);
                    }
                }
                verifyDigest(r, count, hash.digest());
            } finally { conn.disconnect(); }
            PackageInfo apk = c.getPackageManager().getPackageArchiveInfo(file.getAbsolutePath(), flags());
            verify(installed(c), apk, r);
            valid = true; return file;
        } finally { if (!valid) file.delete(); }
    }
    static void verifyDigest(Release r, long size, byte[] digest) throws IOException {
        if (size != r.size || !MessageDigest.isEqual(hex(digest).getBytes(java.nio.charset.StandardCharsets.US_ASCII),
                r.hash.getBytes(java.nio.charset.StandardCharsets.US_ASCII))) throw new IOException("Hash mismatch");
    }
    static void verify(PackageInfo current, PackageInfo apk, Release r) throws IOException {
        if (apk == null || !"com.ilia.advanceclock".equals(apk.packageName)
                || code(apk) != r.code || code(apk) <= code(current) || !r.version.equals(apk.versionName)
                || apk.applicationInfo == null || (apk.applicationInfo.flags & (ApplicationInfo.FLAG_DEBUGGABLE | ApplicationInfo.FLAG_TEST_ONLY)) != 0
                || !signers(current).equals(signers(apk)) || signers(current).isEmpty()) throw new IOException("Untrusted APK");
    }
    private static Set<String> signers(PackageInfo info) throws IOException {
        android.content.pm.Signature[] values = Build.VERSION.SDK_INT >= 28
                ? (info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners()) : info.signatures;
        Set<String> result = new HashSet<>();
        if (values != null) for (android.content.pm.Signature s : values) {
            try { result.add(hex(MessageDigest.getInstance("SHA-256").digest(s.toByteArray()))); }
            catch (Exception e) { throw new IOException(e); }
        }
        return result;
    }
    static String hex(byte[] bytes) {
        StringBuilder b = new StringBuilder(); for (byte v : bytes) b.append(String.format(Locale.ROOT, "%02x", v & 255)); return b.toString();
    }
}
