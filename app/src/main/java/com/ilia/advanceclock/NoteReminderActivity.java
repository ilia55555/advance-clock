package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

public final class NoteReminderActivity extends Activity {
    private static java.lang.ref.WeakReference<NoteReminderActivity> showing = new java.lang.ref.WeakReference<>(null);
    private long noteId = -1;
    static void finishIfShowing(long id) {
        NoteReminderActivity activity = showing.get();
        if (activity != null && activity.noteId == id) activity.runOnUiThread(activity::finishAndRemoveTask);
    }
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); super.onCreate(state);
        showing = new java.lang.ref.WeakReference<>(this);
        ReminderScreen.prepareWindow(this); bind(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); bind(intent); }
    @Override protected void onResume() { super.onResume(); ReminderScreen.hideSystemBars(this); }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused); if (focused) ReminderScreen.hideSystemBars(this); }
    private void bind(Intent intent) {
        noteId = intent == null ? -1 : intent.getLongExtra("noteId", -1);
        NoForgetItem item = new NoForgetStore(this).find(noteId);
        if (item == null) { finish(); return; }
        ReminderScreen.Content data = new ReminderScreen.Content();
        data.title = item.title.trim().isEmpty() ? getString(R.string.runtime_text_0311) : item.title;
        data.body = item.body; data.attachments = NoteAttachment.parse(item.attachmentsJson);
        data.image1 = item.imageUri1; data.image2 = item.imageUri2; data.background = item.imagesAsBackground;
        setContentView(new ReminderScreen(this, data, this::stopAndClose, () -> {
            startActivity(new Intent(this, NoForgetEditorActivity.class).putExtra("noteId", noteId));
            stopAndClose();
        }, minutes -> {
            if (NoForgetScheduler.snooze(this, noteId, minutes * 60_000L)) stopAndClose();
            else Toast.makeText(this, R.string.runtime_text_0587, Toast.LENGTH_LONG).show();
        }));
    }
    @Override public boolean onKeyDown(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_VOLUME_UP) {
            startService(NoForgetSoundService.volumeIntent(this, code == KeyEvent.KEYCODE_VOLUME_DOWN)); return true;
        }
        return super.onKeyDown(code, event);
    }
    @Override public boolean onKeyUp(int code, KeyEvent event) {
        if (code == KeyEvent.KEYCODE_VOLUME_DOWN || code == KeyEvent.KEYCODE_VOLUME_UP) return true;
        return super.onKeyUp(code, event);
    }
    private void stopAndClose() { stopService(NoForgetSoundService.stopIntent(this)); finishAndRemoveTask(); }
    @Override public void onBackPressed() { /* Dismiss explicitly, consistently with other alerts. */ }
    @Override protected void onDestroy() { if (showing.get() == this) showing.clear(); super.onDestroy(); }
}
