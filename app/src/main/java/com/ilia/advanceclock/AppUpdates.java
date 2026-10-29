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


/** Public release metadata is untrusted. Only a newer APK with our installed signer is exported. */
final class AppUpdates {
    static final long ENTRY_THROTTLE = 5L * 60 * 1000;
    static final long MAX_APK = 150L * 1024 * 1024;
    static final int JOB_ID = 8204;
    static final int DUE_JOB_ID = 8205;


    static final class Release {
        final long code, size;
        final String version, hash, tag;
        final JSONObject metadata;
        Release(String json) throws Exception {
            JSONObject o = new JSONObject(json); metadata = o;
            code = o.getLong("versionCode"); size = o.getLong("size");
            version = o.getString("versionName"); hash = o.getString("sha256"); tag = o.getString("tag");
            if (code <= 0 || code > 2100000000L || version.length() > 40 || size <= 0 || size > MAX_APK || !version.matches("[0-9]+(?:\\.[0-9]+){3}")
                    || !hash.matches("[a-f0-9]{64}") || !tag.equals("v" + version))
                throw new IOException("Invalid release metadata");
        }
        String url(String repository) throws IOException {
            if (!validRepository(repository)) throw new IOException("Update repository not configured");
            return repository + "/releases/download/" + tag + "/InfiniTime.apk";
        }
        String text(String key, String language) {
            Object value = metadata.opt(key);
            if (value instanceof JSONObject) {
                JSONObject labels = (JSONObject) value;
                value = labels.opt(language);
                if (!(value instanceof String) || ((String)value).trim().isEmpty()) value = labels.opt("en");
            }
            return value instanceof String ? ((String)value).trim().substring(0, Math.min(((String)value).trim().length(), key.equals("title") ? 200 : 4000)) : "";
        }
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
    static String repository(Context c) { return c.getString(R.string.update_repository).trim(); }
    static boolean validRepository(String value) {
        return value != null && value.matches("https://github\\.com/[A-Za-z0-9_-]+/[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*");
    }
    static Release available(Context c) { return available(c, repository(c)); }
    static Release available(Context c, String repository) {
        if (!production(c) || !validRepository(repository) || !repository.equals(prefs(c).getString("source", ""))) return null;
        try {
            long age = System.currentTimeMillis() - prefs(c).getLong("success", 0);
            if (age < 0 || age > 7L * 24 * 60 * 60 * 1000) return null;
            Release r = new Release(prefs(c).getString("release", ""));
            return r.code > code(installed(c)) ? r : null;
        } catch (Exception e) { return null; }
    }
    static void cancelLegacyJobs(Context c) {
        JobScheduler jobs = c.getSystemService(JobScheduler.class);
        if (jobs != null) { jobs.cancel(JOB_ID); jobs.cancel(DUE_JOB_ID); }
    }
    static void check(Context c, ForegroundUpdates.Request request, String repository) {
        if (!production(c) || !validRepository(repository) || request.cancelled) return;
        try {
            long now = System.currentTimeMillis();
            prefs(c).edit().putLong("attempt", now).apply();
            HttpURLConnection conn = open(repository + "/releases/latest/download/update.json", request);
            request.connection = conn;
            try {
                if (request.cancelled) return;
                if (conn.getResponseCode() == 404) {
                    prefs(c).edit().remove("release").putString("source", repository).putLong("success", now).apply();
                    return;
                }
                if (conn.getResponseCode() != 200) throw new IOException("Metadata unavailable");
                String json;
                try (InputStream in = conn.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    byte[] b = new byte[4096]; int n; long started = android.os.SystemClock.elapsedRealtime();
                    while ((n = in.read(b)) != -1) {
                        if (request.cancelled || Thread.currentThread().isInterrupted() || android.os.SystemClock.elapsedRealtime() - started > 60000) throw new IOException("Metadata cancelled");
                        if (out.size() + n > 16384) throw new IOException("Metadata too large");
                        out.write(b, 0, n);
                    }
                    json = out.toString("UTF-8");
                }
                new Release(json);
                if (!request.cancelled) prefs(c).edit().putString("release", json).putString("source", repository).putLong("success", now).apply();
            } finally { conn.disconnect(); request.connection = null; }
        } catch (Exception ignored) { /* Offline must never interrupt clocks or alarms. */ }
    }
    static boolean due(long now, long last) { return last <= 0 || now < last || now - last >= ENTRY_THROTTLE; }
    static boolean allowed(URL u) {
        if (!"https".equals(u.getProtocol()) || u.getUserInfo() != null || (u.getPort() != -1 && u.getPort() != 443)) return false;
        String host = u.getHost();
        return host.equals("github.com") || host.equals("release-assets.githubusercontent.com")
                || host.equals("objects.githubusercontent.com");
    }
    static HttpURLConnection open(String address) throws Exception { return open(address, null); }
    static HttpURLConnection open(String address, ForegroundUpdates.Request request) throws Exception {
        URL url = new URL(address);
        for (int i = 0; i < 5; i++) {
            if (request != null && request.cancelled) throw new IOException("Check cancelled");
            if (!allowed(url)) throw new IOException("Untrusted download host");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            if (request != null) request.connection = c;
            c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "InfiniTime-Update");
            c.setRequestProperty("Accept-Encoding", "identity");
            if (request != null && request.cancelled) { c.disconnect(); throw new IOException("Check cancelled"); }
            int status;
            try { status = c.getResponseCode(); } catch (Exception e) { c.disconnect(); throw e; }
            if (request != null && request.cancelled) { c.disconnect(); throw new IOException("Check cancelled"); }
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
            HttpURLConnection conn = open(r.url(repository(c)));
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
