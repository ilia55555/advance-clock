package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;

/** Full-size presentation using the real alert UI, without touching any live alert or schedule. */
public final class AlarmPreviewActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);ReminderScreen.applyWindowTheme(this);super.onCreate(state);
        ReminderScreen.prepareWindow(this);
        ReminderScreen.Content sample=new ReminderScreen.Content();
        sample.title=AppString.get(R.string.alarm_dismiss_preview_title);
        sample.openLabel=getString(R.string.reminder_open_destination,getString(R.string.ui_alarm));
        sample.output=new AlertOutput(){
            private boolean muted=true,vibrating=false;
            public boolean muted(){return muted;}
            public boolean vibrating(){return vibrating;}
            public void mute(boolean value){muted=value;AlertOutputEvents.changed();}
            public void vibrate(boolean value){vibrating=value;AlertOutputEvents.changed();}
        };
        setContentView(new ReminderScreen(this,sample,this::finish,this::finish,minutes->finish()));
    }
    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);if(focused)ReminderScreen.hideSystemBars(this);
    }
    @Override public void onBackPressed(){finish();}
}
