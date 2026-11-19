package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/** Invisible trampoline for clicks inside the scrollable notes widget list. */
public final class NoteWidgetClickActivity extends Activity {
    static final String EXTRA_KIND = "noteWidgetClick";
    static final int KIND_OPEN = 1;
    static final int KIND_TASK = 2;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        Intent intent = getIntent();
        int kind = intent == null ? 0 : intent.getIntExtra(EXTRA_KIND, 0);
        if (kind == KIND_OPEN) {
            startActivity(new Intent(this, NoteDetailActivity.class)
                    .putExtra("noteId", intent.getLongExtra("noteId", -1))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (kind == KIND_TASK) {
            Context app = getApplicationContext();
            Intent copy = new Intent(intent);
            NoteTasks.WORK.execute(() -> {
                try {
                    if (!NoteWidgetTaskReceiver.apply(app, copy)) NoForgetWidgetProvider.updateAll(app);
                } catch (RuntimeException error) {
                    android.util.Log.e("NoteWidgetTasks", "Cannot update task", error);
                    NoForgetWidgetProvider.updateAll(app);
                }
            });
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
