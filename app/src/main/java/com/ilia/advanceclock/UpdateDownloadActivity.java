package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.view.Gravity;
import android.widget.*;
import java.io.*;

/** Downloads privately, validates, then exports. Never starts a package installer. */
public final class UpdateDownloadActivity extends Activity {
    private TextView status;
    private Button open;
    private Thread worker;
    private File verified;
    private AppUpdates.Release release;
    private boolean resumed, ready, opened;
    @Override protected void attachBaseContext(Context base) { super.attachBaseContext(AppSettings.localizedContext(base)); }
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); super.onCreate(state);
        LinearLayout page = new LinearLayout(this); page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER); int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        page.setPadding(padding, padding, padding, padding); page.setBackgroundColor(AppSettings.surface(this));
        status = new TextView(this); status.setTextSize(17); status.setGravity(Gravity.CENTER);
        status.setTextColor(AppSettings.textPrimary(this)); status.setText(R.string.update_downloading);
        page.addView(status, new LinearLayout.LayoutParams(-1, -2));
        open = new Button(this); open.setText(R.string.update_open_downloads); open.setVisibility(android.view.View.GONE);
        open.setOnClickListener(v -> openFiles()); page.addView(open, new LinearLayout.LayoutParams(-1, -2));
        setContentView(page);
        release = AppUpdates.available(this);
        if (release == null) { finish(); return; }
        worker = new Thread(() -> {
            try {
                verified = AppUpdates.download(getApplicationContext(), release);
                if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
                if (Build.VERSION.SDK_INT >= 29) publish();
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    ready = true; status.setText(Build.VERSION.SDK_INT >= 29 ? R.string.update_ready : R.string.update_choose_location);
                    if (Build.VERSION.SDK_INT < 29) open.setText(R.string.update_save_apk);
                    open.setVisibility(android.view.View.VISIBLE);
                    if (resumed) openFiles();
                });
            } catch (Exception e) {
                if (verified != null) verified.delete();
                runOnUiThread(() -> { if (!isDestroyed()) status.setText(R.string.update_failed); });
            }
        }, "verified-release-download"); worker.start();
    }
    @Override protected void onResume() { super.onResume(); resumed = true; if (ready && !opened) openFiles(); }
    @Override protected void onPause() { resumed = false; super.onPause(); }
    @Override protected void onDestroy() {
        if (worker != null) worker.interrupt();
        if (verified != null) verified.delete();
        super.onDestroy();
    }
    private String filename() { return "InfiniTime-" + release.version + ".apk"; }
    @android.annotation.TargetApi(29)
    private void publish() throws Exception {
        ContentValues values = new ContentValues(); values.put(MediaStore.Downloads.DISPLAY_NAME, filename());
        values.put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive");
        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/InfiniTime");
        values.put(MediaStore.Downloads.IS_PENDING, 1);
        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) throw new IOException("Downloads unavailable");
        try {
            try (InputStream in = new FileInputStream(verified); OutputStream out = getContentResolver().openOutputStream(uri)) {
                copy(in, out);
            }
            if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled");
            values.clear(); values.put(MediaStore.Downloads.IS_PENDING, 0);
            getContentResolver().update(uri, values, null, null);
        } catch (Exception e) { getContentResolver().delete(uri, null, null); throw e; }
        finally { verified.delete(); }
    }
    private void openFiles() {
        opened = true;
        try {
            if (Build.VERSION.SDK_INT < 29 && verified != null && verified.isFile()) {
                Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/vnd.android.package-archive").putExtra(Intent.EXTRA_TITLE, filename());
                startActivityForResult(save, 10);
            } else startActivity(new Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS));
        } catch (ActivityNotFoundException e) {
            LogoToast.makeText(this, R.string.update_ready, 0).show();
        }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 10 || result != RESULT_OK || data == null || data.getData() == null || verified == null) return;
        Uri destination = data.getData();
        worker = new Thread(() -> {
            try (InputStream in = new FileInputStream(verified); OutputStream out = getContentResolver().openOutputStream(destination)) {
                copy(in, out); verified.delete(); verified = null;
                runOnUiThread(() -> { status.setText(R.string.update_ready); open.setText(R.string.update_open_downloads); openFiles(); });
            } catch (Exception e) {
                try { android.provider.DocumentsContract.deleteDocument(getContentResolver(), destination); } catch (Exception ignored) {}
                runOnUiThread(() -> { if (!isDestroyed()) status.setText(R.string.update_failed); });
            }
        }, "export-verified-release"); worker.start();
    }
    private static void copy(InputStream in, OutputStream out) throws IOException {
        if (out == null) throw new IOException("No output");
        byte[] buffer = new byte[32768]; int n;
        while ((n = in.read(buffer)) != -1) { if (Thread.currentThread().isInterrupted()) throw new IOException("Cancelled"); out.write(buffer, 0, n); }
    }
}
