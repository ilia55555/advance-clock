package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

public final class NoteReminderActivity extends Activity {
    private static java.lang.ref.WeakReference<NoteReminderActivity> showing = new java.lang.ref.WeakReference<>(null);
    private boolean screenVisible;
    static boolean isVisible(long id) {
        NoteReminderActivity activity=showing.get();
        return activity!=null && !activity.isFinishing() && !activity.isDestroyed() && activity.screenVisible && activity.noteId==id;
    }
    private long noteId = -1;
    static void finishIfShowing(long id) {
        NoteReminderActivity activity = showing.get();
        if (activity != null && activity.noteId == id) activity.runOnUiThread(activity::finishAndRemoveTask);
    }
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); ReminderScreen.applyWindowTheme(this); super.onCreate(state);
        showing = new java.lang.ref.WeakReference<>(this);
        ReminderScreen.prepareWindow(this); bind(getIntent());
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); bind(intent); NoForgetSoundService.onRingVisibilityChanged(); }
    @Override protected void onResume() { super.onResume(); screenVisible=true; NoForgetSoundService.onRingVisibilityChanged(); ReminderScreen.hideSystemBars(this);refreshNoteContents(); }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused); if (focused) ReminderScreen.hideSystemBars(this); }
    private void bind(Intent intent) {
        noteId = intent == null ? -1 : intent.getLongExtra("noteId", -1);
        NoForgetItem item = new NoForgetStore(this).find(noteId);
        if (item == null) { finish(); return; }
        ReminderScreen.Content data = new ReminderScreen.Content();
        data.title = item.title;
        data.priority = item.priority;
        data.output = AlertOutput.note(noteId);
        data.openLabel=getString(R.string.reminder_open_destination,getString(R.string.tab_notes));
        long eventTime=intent==null?0:intent.getLongExtra(EventReminderScheduler.OCCURRENCE,0);
        data.body = eventTime>0?getString(R.string.event_at,CalendarUtils.formatDate(eventTime,AppSettings.defaultCalendar(this))+" "+TimeFormat.clock(this,eventTime)):"";
        data.allowDrawing=false;data.noteId=noteId;
        setContentView(new ReminderScreen(this, data, this::stopAndClose, ()->{
            startActivity(new Intent(this,NoForgetEditorActivity.class).putExtra("noteId",noteId));
            stopAndClose();
        }, minutes -> {
            if (AlertSnoozes.schedule(this,"note",noteId,data.title,"",minutes)) stopAndClose();
            else LogoToast.makeText(this, R.string.runtime_text_0587, Toast.LENGTH_LONG).show();
        }));
    }
    private void refreshNoteContents(){
        android.view.View view=findViewById(android.R.id.content).findViewWithTag("note_detail_view");
        if(view instanceof NoteDetailView)((NoteDetailView)view).render();
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
    // ReminderScreen registers the Android 13+ back callback; keep older devices covered.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { /* Dismiss explicitly, consistently with other alerts. */ }
    @Override protected void onStop() { screenVisible=false; NoForgetSoundService.onRingVisibilityChanged(); super.onStop(); }
    @Override protected void onDestroy() { screenVisible=false; if (showing.get() == this) showing.clear(); NoForgetSoundService.onRingVisibilityChanged(); super.onDestroy(); }
}
