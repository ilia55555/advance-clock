package com.ilia.advanceclock;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.Toast;

/** Full-size alert preview with its own audio and vibration, independent of live alerts. */
public final class AlarmPreviewActivity extends Activity {
    private AlertVolumeKeys volumeKeys;
    private PreviewAlertOutput output;
    private boolean registered;
    private final BroadcastReceiver screenOff=new BroadcastReceiver(){
        @Override public void onReceive(Context context,Intent intent){if(output!=null)output.silence();}
    };
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);ReminderScreen.applyWindowTheme(this);super.onCreate(state);
        ReminderScreen.prepareWindow(this);setVolumeControlStream(android.media.AudioManager.STREAM_ALARM);
        output=new PreviewAlertOutput(this);
        volumeKeys=new AlertVolumeKeys(this,output::silence,()->output.mute(false));
        ReminderScreen.Content sample=new ReminderScreen.Content();
        sample.title=AppString.get(R.string.alarm_dismiss_preview_title);
        sample.openLabel=getString(R.string.reminder_open_destination,getString(R.string.ui_alarm));
        sample.output=output;
        setContentView(new ReminderScreen(this,sample,this::finish,()->{
            startActivity(new Intent(this,MainActivity.class).putExtra("openTab","clock").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            finish();
        },minutes->{
            AlarmItem snapshot=new AlarmItem(System.currentTimeMillis(),sample.title,System.currentTimeMillis(),0,true);
            snapshot.soundUri=AppSettings.defaultAlarmSoundUri(this);snapshot.vibrate=true;
            try{
                if(AlertSnoozes.schedule(this,"alarm",snapshot.id,snapshot.label,snapshot.toJson().toString(),minutes))finish();
                else LogoToast.makeText(this,R.string.runtime_text_0587,Toast.LENGTH_LONG).show();
            }catch(org.json.JSONException invalid){LogoToast.makeText(this,R.string.runtime_text_0587,Toast.LENGTH_LONG).show();}
        }));
        IntentFilter filter=new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if(android.os.Build.VERSION.SDK_INT>=33)registerReceiver(screenOff,filter,RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenOff,filter);
        registered=true;output.start();
    }
    @Override public boolean onKeyDown(int code,KeyEvent event){return volumeKeys.down(code,event) || super.onKeyDown(code,event);}
    @Override public boolean onKeyUp(int code,KeyEvent event){return volumeKeys.up(code,event) || super.onKeyUp(code,event);}

    @Override public void onWindowFocusChanged(boolean focused){
        super.onWindowFocusChanged(focused);if(focused)ReminderScreen.hideSystemBars(this);
    }
    @Override public void finish(){if(output!=null)output.close();super.finish();}
    @Override protected void onPause(){if(volumeKeys!=null)volumeKeys.cancel();super.onPause();}
    @Override protected void onStop(){if(output!=null)output.close();super.onStop();if(!isFinishing())finish();}
    @Override protected void onDestroy(){ if(volumeKeys!=null)volumeKeys.cancel();
        if(output!=null)output.close();if(registered)unregisterReceiver(screenOff);super.onDestroy();
    }
    @Override public void onBackPressed(){finish();}
}
