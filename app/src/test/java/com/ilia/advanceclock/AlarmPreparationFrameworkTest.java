package com.ilia.advanceclock;

import android.app.AlarmManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.annotation.RealObject;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application = Application.class, sdk = {28, 35},
        shadows = {AlarmPreparationFrameworkTest.FakePlayer.class, ReminderScreenFrameworkTest.PlainStore.class})
public final class AlarmPreparationFrameworkTest {
    @Implements(MediaPlayer.class)
    public static class FakePlayer {
        @RealObject private MediaPlayer real;
        static int created, started, released;
        static boolean dataSourceOnMain, deferPreparation;
        static MediaPlayer.OnPreparedListener lastListener;
        static MediaPlayer lastPlayer;
        private MediaPlayer.OnPreparedListener listener;
        @Implementation protected void __constructor__() { created++; }
        @Implementation protected void setAudioAttributes(AudioAttributes attributes) {}
        @Implementation protected void setLooping(boolean looping) {}
        @Implementation protected void setDataSource(Context c, Uri uri) {
            dataSourceOnMain |= android.os.Looper.myLooper() == android.os.Looper.getMainLooper();
        }
        @Implementation protected void setOnPreparedListener(MediaPlayer.OnPreparedListener value) {
            listener = value;
            if (value != null) { lastListener = value; lastPlayer = real; }
        }
        @Implementation protected void setOnErrorListener(MediaPlayer.OnErrorListener value) {}
        @Implementation protected void prepareAsync() { if (!deferPreparation && listener != null) listener.onPrepared(real); }
        @Implementation protected void setVolume(float left, float right) {}
        @Implementation protected void start() { started++; }
        @Implementation protected void release() { released++; }
    }

    @org.junit.Before public void resetAudioFixture() {
        AlarmAudioEngine.stop();
        drain();
        FakePlayer.created = FakePlayer.started = FakePlayer.released = 0;
        FakePlayer.dataSourceOnMain = FakePlayer.deferPreparation = false;
        FakePlayer.lastListener = null;
        FakePlayer.lastPlayer = null;
    }

    @org.junit.After public void releaseAudioFixture() {
        AlarmAudioEngine.stop();
        drain();
    }

    private Context init() {
        Context c = RuntimeEnvironment.getApplication();
        AppString.init(c);
        Shadows.shadowOf(c.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);
        return c;
    }
    private void drain() { Shadows.shadowOf(AlarmAudioEngine.handler().getLooper()).idle(); }

    @Test public void unusedWarmPlayerExpiresWithoutPlayback() {
        Context c = init();
        AlarmAudioEngine.prime(c, "clock:expire", "content://sounds/local", System.currentTimeMillis() + 5000);
        drain();
        assertEquals(1, FakePlayer.created);
        Shadows.shadowOf(AlarmAudioEngine.handler().getLooper()).idleFor(java.time.Duration.ofSeconds(11));
        assertEquals(1, FakePlayer.released);
        assertEquals(0, FakePlayer.started);
    }

    @Test public void stoppingWhilePreparingRejectsLatePreparedCallback() {
        Context c = init();
        FakePlayer.deferPreparation = true;
        AlarmAudioEngine.play(c, "clock:late", "content://sounds/local");
        drain();
        MediaPlayer.OnPreparedListener late = FakePlayer.lastListener;
        MediaPlayer player = FakePlayer.lastPlayer;
        assertNotNull(late);
        AlarmAudioEngine.stop(); drain();
        AlarmAudioEngine.handler().post(() -> late.onPrepared(player));
        drain();
        assertEquals(0, FakePlayer.started);
        assertEquals(1, FakePlayer.released);
    }

    @Test public void preparationIsSilentAndPreparedPlayerIsReusedOnlyWhenAlarmFires() {
        Context c = init();
        String uri = "android.resource://" + c.getPackageName() + "/" + R.raw.rawadhan_1;
        AlarmAudioEngine.prime(c, "clock:17", uri, System.currentTimeMillis() + 5000);
        drain();
        assertEquals(1, FakePlayer.created);
        assertEquals(0, FakePlayer.started);
        assertFalse(FakePlayer.dataSourceOnMain);
        AlarmAudioEngine.play(c, "clock:17", uri);
        drain();
        assertEquals(1, FakePlayer.created);
        assertEquals(1, FakePlayer.started);
        AlarmAudioEngine.stop(); drain();
        assertEquals(1, FakePlayer.released);
    }

    @Test public void changedSoundCannotClaimStalePreparationAndCancelReleasesIt() {
        Context c = init();
        AlarmAudioEngine.prime(c, "clock:18", "content://sounds/old", System.currentTimeMillis() + 5000);
        drain();
        AlarmAudioEngine.play(c, "clock:18", "content://sounds/new");
        drain();
        assertEquals(2, FakePlayer.created);
        assertEquals(1, FakePlayer.started);
        assertEquals(1, FakePlayer.released);
        AlarmAudioEngine.stop(); drain();
        AlarmAudioEngine.prime(c, "clock:19", "content://sounds/new", System.currentTimeMillis() + 5000);
        drain();
        AlarmAudioEngine.cancelPrepared("clock:19"); drain();
        assertEquals(3, FakePlayer.released);
    }

    @Test public void primaryDeadlineIsUnchangedAndPreparationIsCancelledWithAlarm() {
        Context c = init();
        long deadline = System.currentTimeMillis() + 60_000;
        AlarmItem item = new AlarmItem(20, "Test", deadline, 0, true);
        new AlarmStore(c).save(item);
        assertTrue(AlarmScheduler.schedule(c, item));
        AlarmManager manager = c.getSystemService(AlarmManager.class);
        assertEquals(deadline, manager.getNextAlarmClock().getTriggerTime());
        boolean foundWarm = false;
        for (org.robolectric.shadows.ShadowAlarmManager.ScheduledAlarm alarm : Shadows.shadowOf(manager).getScheduledAlarms()) {
            Intent intent = Shadows.shadowOf(alarm.operation).getSavedIntent();
            if (intent.getComponent().getClassName().equals(AlarmPreparationReceiver.class.getName())) {
                foundWarm = true;
                assertEquals(deadline - 5000, alarm.triggerAtTime);
            }
        }
        assertTrue(foundWarm);
        AlarmScheduler.cancel(c, item.id);
        assertTrue(Shadows.shadowOf(manager).getScheduledAlarms().isEmpty());
    }

    @Test public void shortTimersStillHaveTheirRealAlarmWithoutAnEarlyPreparation() {
        Context c = init();
        long deadline = System.currentTimeMillis() + 2000;
        assertTrue(ToolAlarmScheduler.schedule(c, ToolAlarmScheduler.TIMER, deadline, "Timer"));
        AlarmManager manager = c.getSystemService(AlarmManager.class);
        assertEquals(deadline, manager.getNextAlarmClock().getTriggerTime());
        assertEquals(1, Shadows.shadowOf(manager).getScheduledAlarms().size());
        ToolAlarmScheduler.cancel(c, ToolAlarmScheduler.TIMER);
        assertTrue(Shadows.shadowOf(manager).getScheduledAlarms().isEmpty());
    }

    @Test public void concurrentStoreInstancesCannotLoseAlarmUpdates() throws Exception {
        Context c = init();
        java.util.concurrent.ExecutorService threads = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Future<?> first = threads.submit(() -> {
                for (int i = 0; i < 20; i++)
                    new AlarmStore(c).save(new AlarmItem(100 + i, "First", System.currentTimeMillis() + 60000, 0, true));
            });
            java.util.concurrent.Future<?> second = threads.submit(() -> {
                for (int i = 0; i < 20; i++)
                    new AlarmStore(c).save(new AlarmItem(200 + i, "Second", System.currentTimeMillis() + 60000, 0, true));
            });
            first.get(15, java.util.concurrent.TimeUnit.SECONDS);
            second.get(15, java.util.concurrent.TimeUnit.SECONDS);
            assertEquals(40, new AlarmStore(c).all().size());
        } finally { threads.shutdownNow(); }
    }

    @Test public void obsoletePreparationDoesNotCreateAnyPlayer() {
        Context c = init();
        Intent stale = new Intent().putExtra("kind", AlarmPreparationReceiver.CLOCK)
                .putExtra("alarmId", 999L).putExtra("deadline", System.currentTimeMillis() + 5000);
        AlarmPreparationReceiver.prepare(c, stale);
        drain();
        assertEquals(0, FakePlayer.created);
        assertEquals(0, FakePlayer.started);
    }
}
