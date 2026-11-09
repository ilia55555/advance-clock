package com.ilia.advanceclock;

import android.app.KeyguardManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;

public final class AdhanSoundService extends Service {
    public static final String AUDIO_RESOURCE_NAME = "adhan";
    public static final String AUDIO_FILE_NAME = "adhan.mp3";
    public static final String ACTION_START = "com.ilia.advanceclock.ADHAN_START";
    public static final String ACTION_MUNAJAT_BEFORE = "com.ilia.advanceclock.MUNAJAT_BEFORE_START";
    public static final String ACTION_STOP = "com.ilia.advanceclock.ADHAN_STOP";
    public static final String ACTION_MUTE = "com.ilia.advanceclock.ADHAN_MUTE";
    public static final String ACTION_UNMUTE = "com.ilia.advanceclock.ADHAN_UNMUTE";
    public static final String ACTION_LOWER = "com.ilia.advanceclock.ADHAN_LOWER";
    public static final String ACTION_RAISE = "com.ilia.advanceclock.ADHAN_RAISE";

    private static java.lang.ref.WeakReference<AdhanSoundService> running =
            new java.lang.ref.WeakReference<>(null);
    private boolean playing;
    private boolean foreground;
    private int phase; // 0 adhan, 1 before, 2 after
    private MunajatConfig munajat;
    private long munajatRampDuration;
    private long prayerAt;
    private final Runnable munajatBoundary=()->{if(phase==1){stopRing();stopSelf();}};
    private final Runnable munajatRamp=new Runnable(){public void run(){
        if(phase==0 || player==null || munajat==null)return;
        try{float position=munajatRampDuration>0?player.getCurrentPosition()/(float)munajatRampDuration:0;float gain=muted?0:munajat.gain(position);player.setVolume(gain,gain);if(munajat.gradual && !muted)stopHandler.postDelayed(this,200);}catch(RuntimeException ignored){}
    }};
    static void stopMunajatIfDisabled(Context c){AdhanSoundService service=running.get();if(service!=null && service.phase!=0 && !MunajatConfig.load(c,service.type,service.phase==1).playable()){service.stopRing();service.stopSelf();}}

    static void onRingVisibilityChanged() {
        AdhanSoundService service = running.get();
        if (service != null) service.updateNotificationVisibility();
    }

    private void updateNotificationVisibility() {
        if (!playing) return;
        if(phase!=0)return;
        if (AdhanRingActivity.isVisible(type)) {
            if (foreground) {
                stopForeground(STOP_FOREGROUND_REMOVE);
                foreground = false;
            }
        } else if (!foreground) {
            // Restore background playback protection without opening the screen again.
            startForeground(3_300_000 + type, notification(false));
            foreground = true;
        }
    }

    private MediaPlayer player;
    private Vibrator vibrator;
    private boolean vibrationEnabled;
    private float volume = 1f;
    private boolean muted;
    private int type;

    private final Handler stopHandler = new Handler(Looper.getMainLooper());
    private final Runnable maximumDuration = this::stopSelf;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) silence();
        }
    };

    public static Intent startIntent(Context context, int type) {
        return command(context, ACTION_START).putExtra("adhanType", type);
    }

    public static Intent command(Context context, String action) {
        return new Intent(context, AdhanSoundService.class).setAction(action);
    }

    private static AdhanSoundService output(long id) {
        AdhanSoundService service=running.get();
        return service!=null && service.playing && service.type == id ? service : null;
    }
    static boolean outputMuted(long id) { AdhanSoundService service=output(id);return service!=null && service.muted; }
    static boolean outputVibrating(long id) { AdhanSoundService service=output(id);return service!=null && service.vibrationEnabled; }
    static void setOutputMuted(long id,boolean value) {
        AdhanSoundService service=output(id);if(service==null)return;
        service.setMuted(value);
    }
    static void setOutputVibrating(long id,boolean value) {
        AdhanSoundService service=output(id);if(service==null)return;
        service.vibrationEnabled=value;if(service.vibrator!=null)service.vibrator.cancel(); if(value)service.startVibration();
        AlertOutputEvents.changed();
    }

    private void stopRing(){playing=false;foreground=false;stopHandler.removeCallbacks(maximumDuration);stopPlayback();stopForeground(STOP_FOREGROUND_REMOVE);AlertOutputEvents.changed();}
    static void stopRing(int type){AdhanSoundService service=output(type);if(service!=null){service.stopRing();service.stopSelf();}}
    @Override public void onTaskRemoved(Intent rootIntent){stopRing();stopSelf();super.onTaskRemoved(rootIntent);}

    static void refreshVolume(Context context,int changedType){
        AdhanSoundService service=running.get();if(service==null || !service.playing || (changedType!=899 && changedType!=service.type))return;
        if(service.phase!=0)return;
        float previous=service.volume;
        int percent=AppSettings.adhanVolume(context,service.type);service.volume=percent/100f;
        AlarmVolume.apply(context,percent);
        if(percent==0)service.muted=true;else if(previous==0)service.muted=false;
        service.applyVolume();
    }

    @Override public void onCreate() {
        super.onCreate();
        running = new java.lang.ref.WeakReference<>(this);
        NotificationHelper.ensureChannels(this);
        IntentFilter screenFilter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, screenFilter, RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, screenFilter);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;
        String action = intent.getAction();
        if (ACTION_STOP.equals(action)) {
            stopRing();
            stopSelf();
            return START_NOT_STICKY;
        }
        if (ACTION_MUTE.equals(action)) silence();
        else if (ACTION_UNMUTE.equals(action)) setMuted(false);
        else if (ACTION_LOWER.equals(action)) lowerVolume();
        else if (ACTION_RAISE.equals(action)) raiseVolume();
        else if (ACTION_START.equals(action)) {
            phase=0;
            type = intent.getIntExtra("adhanType", AdhanScheduler.FAJR);
            boolean openWhileUnlocked = shouldOpenFullscreenWhileUnlocked();
            playing = true;
            startForeground(3_300_000 + type, notification(!AdhanRingActivity.isVisible(type)));
            foreground = true;
            if (openWhileUnlocked && !AdhanRingActivity.isVisible(type)) openRingActivity();
            startPlayback();
            updateNotificationVisibility();
        }else if(ACTION_MUNAJAT_BEFORE.equals(action)){
            if(playing){if(!foreground){startForeground(3_300_000+type,notification(false));foreground=true;}return START_NOT_STICKY;}
            type=intent.getIntExtra("adhanType",0);prayerAt=intent.getLongExtra("prayerAt",0);
            if(prayerAt<=System.currentTimeMillis()){stopSelf();return START_NOT_STICKY;}
            startMunajat(true,prayerAt);
        }
        return START_NOT_STICKY;
    }

    private Notification notification(boolean allowFullscreen) {
        if(phase!=0){
            PendingIntent open=PendingIntent.getActivity(this,3_600_000+type,new Intent(this,MunajatSettingsActivity.class).putExtra("adhanType",type),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            PendingIntent stop=PendingIntent.getService(this,3_400_000+type,command(this,ACTION_STOP),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification.Builder builder=new Notification.Builder(this,NotificationHelper.ADHAN_CHANNEL).setSmallIcon(R.drawable.ic_alarm)
                    .setContentTitle(getString(R.string.munajat_title)+" • "+AdhanScheduler.title(type)).setContentText(getString(phase==1?R.string.munajat_before:R.string.munajat_after))
                    .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true).addAction(0,getString(R.string.runtime_text_0307),stop);
            if(Build.VERSION.SDK_INT>=31)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);return builder.build();
        }
        Intent ring = new Intent(this, AdhanRingActivity.class)
                .putExtra("adhanType", type)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent open = PendingIntent.getActivity(
                this, 3_200_000 + type, ring,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(
                this, 3_400_000 + type, command(this, ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(
                this, NotificationHelper.ADHAN_CHANNEL)
                .setSmallIcon(R.drawable.ic_alarm)
                .setContentTitle(CalendarUtils.fa(AdhanScheduler.title(type)))
                .setContentText(AppSettings.adhanNotification(this, type)
                        ? AppSettings.prayerLocationLabel(this)
                        : AppString.get(R.string.runtime_text_0407))
                .setCategory(Notification.CATEGORY_ALARM)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open)
                .setStyle(new Notification.DecoratedCustomViewStyle())
                .setCustomContentView(notificationControls(R.layout.notification_adhan, open, stop))
                .setCustomBigContentView(notificationControls(R.layout.notification_adhan_expanded, open, stop));
        if (allowFullscreen && shouldOpenFullscreen()) builder.setFullScreenIntent(open, true);
        if(Build.VERSION.SDK_INT>=31)builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE);
        return builder.build();
    }

    private android.widget.RemoteViews notificationControls(int layout, PendingIntent open, PendingIntent stop) {
        android.widget.RemoteViews views = new android.widget.RemoteViews(getPackageName(), layout);
        views.setTextViewText(R.id.adhan_notification_title, CalendarUtils.fa(AdhanScheduler.title(type)));
        views.setTextViewText(R.id.adhan_notification_stop, AppString.get(R.string.runtime_text_0307));
        views.setOnClickPendingIntent(R.id.adhan_notification_title, open);
        views.setOnClickPendingIntent(R.id.adhan_notification_stop, stop);
        return views;
    }

    private boolean shouldOpenFullscreen() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        return locked ? AppSettings.adhanFullscreenLocked(this, type)
                : AppSettings.adhanFullscreenUnlocked(this, type);
    }

    private boolean shouldOpenFullscreenWhileUnlocked() {
        KeyguardManager keyguard = getSystemService(KeyguardManager.class);
        boolean locked = keyguard != null && keyguard.isKeyguardLocked();
        return !locked && AppSettings.adhanFullscreenUnlocked(this, type);
    }

    private void openRingActivity() {
        try {
            Intent ring = new Intent(this, AdhanRingActivity.class)
                    .putExtra("adhanType", type)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                            | Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(ring);
        } catch (Exception ignored) {
            // Android may still enforce its background-activity policy.
            // The high-priority full-screen notification remains the fallback.
        }
    }

    private void startPlayback() {
        stopPlayback();
        volume = AppSettings.adhanVolume(this, type) / 100f;
        muted = volume==0;
        if(AppSettings.adhanSound(this,type))AlarmVolume.apply(this,Math.round(volume*100));
        stopHandler.removeCallbacks(maximumDuration);
        if (!AppSettings.adhanSound(this, type)) {
            stopHandler.postDelayed(maximumDuration, 10 * 60_000L);
        }
        if (AppSettings.adhanSound(this, type)) {
            String selected = AppSettings.adhanSoundUri(this, type);
            Uri audioUri = selected == null || selected.isEmpty() ? null : Uri.parse(selected);
            if (audioUri == null) {
                int resource = getResources().getIdentifier(
                        AUDIO_RESOURCE_NAME, "raw", getPackageName());
                if (resource != 0) audioUri = Uri.parse(
                        "android.resource://" + getPackageName() + "/" + resource);
                else audioUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            }
            if (audioUri != null) {
                try {
                    player = new MediaPlayer();
                    player.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
                    player.setDataSource(this, LocalMedia.require(audioUri));
                    player.setLooping(false);
                    player.setOnPreparedListener(value->{
                        if(player!=value || !playing)return;
                        applyVolume();value.start();
                    });
                    player.setWakeMode(this, android.os.PowerManager.PARTIAL_WAKE_LOCK);
                    player.setOnCompletionListener(value -> {if(player!=value || phase!=0)return;if(MunajatConfig.load(this,type,false).playable())startMunajat(false,0);else stopSelf();});
                    player.setOnErrorListener((value, what, extra) -> {
                        stopSelf();
                        return true;
                    });
                    player.prepareAsync();
                } catch (Exception ignored) { stopSelf(); }
            }
        }
        vibrationEnabled=AppSettings.adhanVibrate(this,type);
        if(vibrationEnabled)startVibration();
        if(!AppSettings.adhanSound(this,type) && MunajatConfig.load(this,type,false).playable())startMunajat(false,0);
        AlertOutputEvents.changed();
    }

    private void startVibration() {
        try {
            vibrator=getSystemService(Vibrator.class);
            if(vibrator!=null)AlarmVibration.start(vibrator, VibrationEffect.createWaveform(new long[]{0,700,350,700},0));
        }catch(RuntimeException ignored){}
    }

    private void silence() {
        setMuted(true);
        vibrationEnabled=false;
        try { if(vibrator!=null)vibrator.cancel(); } catch(RuntimeException ignored){}
        AlertOutputEvents.changed();
    }

    private void setMuted(boolean value) {
        muted = value;
        applyVolume();
        AlertOutputEvents.changed();
    }

    private void lowerVolume() {
        muted = false;
        volume = Math.max(0f, volume - 0.10f);
        AlarmVolume.apply(this,Math.round(volume*100));muted=volume==0;
        applyVolume();
        AlertOutputEvents.changed();
    }

    private void raiseVolume() {
        muted = false;
        volume = Math.min(1f, volume + 0.10f);
        AlarmVolume.apply(this,Math.round(volume*100));muted=volume==0;
        applyVolume();
        AlertOutputEvents.changed();
    }

    private void applyVolume() {
        if(phase!=0 && player!=null){stopHandler.removeCallbacks(munajatRamp);munajatRamp.run();return;}
        if (player != null) try{player.setVolume(muted ? 0f : 1f, muted ? 0f : 1f);}catch(RuntimeException ignored){}
    }

    private void startMunajat(boolean before,long boundary){
        stopPlayback();stopHandler.removeCallbacks(maximumDuration);
        phase=before?1:2;munajat=MunajatConfig.load(this,type,before);
        if(!munajat.playable() || AppSettings.isAdhanSuppressedNow(this,type)){stopRing();stopSelf();return;}
        playing=true;foreground=true;muted=false;vibrationEnabled=false;
        startForeground(3_300_000+type,notification(false));
        AlarmVolume.apply(this,munajat.ceiling());
        if(before)stopHandler.postDelayed(munajatBoundary,Math.max(1,boundary-System.currentTimeMillis()));
        // Bound a missing/failed preparation; a prepared file gets its own full duration below.
        stopHandler.postDelayed(maximumDuration,30_000L);
        try{
            MediaPlayer value=new MediaPlayer();player=value;
            value.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            value.setDataSource(this,LocalMedia.require(Uri.parse(munajat.uri)));value.setLooping(false);value.setWakeMode(this,android.os.PowerManager.PARTIAL_WAKE_LOCK);
            value.setOnPreparedListener(ready->{if(player!=ready || phase==0)return;int duration=ready.getDuration();munajatRampDuration=duration;if(before)munajatRampDuration=Math.max(1,Math.min(duration>0?duration:Long.MAX_VALUE,boundary-System.currentTimeMillis()));if(duration>0){stopHandler.removeCallbacks(maximumDuration);stopHandler.postDelayed(maximumDuration,duration+10_000L);}float gain=muted?0:munajat.gain(0);ready.setVolume(gain,gain);ready.start();munajatRamp.run();});
            value.setOnCompletionListener(done->{if(player==done && phase!=0){stopRing();stopSelf();}});
            value.setOnErrorListener((failed,what,extra)->{if(player==failed){stopRing();stopSelf();}return true;});value.prepareAsync();
        }catch(Exception ignored){stopRing();stopSelf();}
        AlertOutputEvents.changed();
    }

    private void stopPlayback() {
        stopHandler.removeCallbacks(munajatRamp);stopHandler.removeCallbacks(munajatBoundary);
        try { if (player != null) player.stop(); } catch (Exception ignored) {}
        try { if (player != null) player.release(); } catch (Exception ignored) {}
        player = null;
        if (vibrator != null) vibrator.cancel();
        vibrator = null;
    }

    @Override public void onDestroy() {
        playing = false;
        foreground = false;
        AlertOutputEvents.changed();
        if (running.get() == this) running.clear();
        stopHandler.removeCallbacks(maximumDuration);
        stopPlayback();
        AdhanRingActivity.finishIfShowing(type);
        try { unregisterReceiver(screenReceiver); } catch (Exception ignored) {}
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
