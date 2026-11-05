package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;

/** Read-only note contents, shared with the modal on the ringing note screen. */
public final class NoteDetailActivity extends Activity {
    private NoteDetailView details;
    @Override protected void onCreate(Bundle state){
        AppSettings.applyTheme(this);super.onCreate(state);
        details=new NoteDetailView(this,getIntent().getLongExtra("noteId",-1),this::finish);
        setContentView(details);AppSettings.applyFullscreenInsets(details);
    }
    @Override protected void onResume(){super.onResume();if(!details.render())finish();}
}
