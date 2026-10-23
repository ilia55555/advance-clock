package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.net.Uri;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows={ReminderScreenFrameworkTest.PlainStore.class,AlertSnoozesTest.FixedClock.class})
public class AlertSnoozesTest {
    private Application app;
    @Before public void init(){app=RuntimeEnvironment.getApplication();AppString.init(app);FixedClock.now=System.currentTimeMillis();
        Shadows.shadowOf(app.getSystemService(AlarmManager.class)).setCanScheduleExactAlarms(true);}
    @Test public void fiveSelectedTimesStayDistinctAcrossRestoreAndOnlyDeliveredEntryIsRemoved() throws Exception {
        AlarmItem item=new AlarmItem(42,"Wake",System.currentTimeMillis()+50000,0,true);
        item.imageUri1="content://photo/original";String payload=item.toJson().toString();
        int[] delays={5,10,15,60,1440};long base=FixedClock.now;
        assertTrue(AlertSnoozes.schedule(app,"alarm",42,item.label,payload,delays));
        Map<String,?> before=new HashMap<>(prefs().getAll());assertEquals(5,before.size());
        List<Long> deadlines=new ArrayList<>();for(Object value:before.values())deadlines.add(new JSONObject((String)value).getLong("when"));
        Collections.sort(deadlines);for(int i=0;i<delays.length;i++)assertEquals(base+delays[i]*60000L,(long)deadlines.get(i));
        assertEquals(5,Shadows.shadowOf(app.getSystemService(AlarmManager.class)).getScheduledAlarms().size());
        AlertSnoozes.restore(app);assertEquals(before,prefs().getAll());
        assertEquals(5,Shadows.shadowOf(app.getSystemService(AlarmManager.class)).getScheduledAlarms().size());
        String key=keyAt(deadlines.get(0));Uri first=uri(key);
        AlertSnoozes.deliver(app,first);assertNull(Shadows.shadowOf(app).getNextStartedService());
        FixedClock.now+=5*60000L;
        AlertSnoozes.deliver(app,first);
        Intent service=Shadows.shadowOf(app).getNextStartedService();assertNotNull(service);
        AlarmItem snapshot=AlarmItem.fromJson(new JSONObject(service.getStringExtra("alarmPayload")));
        assertEquals(item.label,snapshot.label);assertEquals(item.imageUri1,snapshot.imageUri1);
        assertEquals(4,prefs().getAll().size());
        AlertSnoozes.deliver(app,first);assertNull(Shadows.shadowOf(app).getNextStartedService());
        for(int i=1;i<delays.length;i++) {
            String nextKey=keyAt(deadlines.get(i));
            FixedClock.now+=(delays[i]-delays[i-1])*60000L;
            AlertSnoozes.deliver(app,uri(nextKey));assertNotNull(Shadows.shadowOf(app).getNextStartedService());
        }
        assertTrue(prefs().getAll().isEmpty());
    }
    @Test public void eachSourceRingsWithoutReplacingItsNormalScheduleOrToolSnapshot() throws Exception {
        NoForgetItem note=new NoForgetItem(7,"Note","Body","[]",2,true,System.currentTimeMillis()+900000,true,System.currentTimeMillis());
        new NoForgetStore(app).save(note);long original=note.dueAtMillis;
        for(String kind:new String[]{"note","timer","stopwatch","adhan"}) {
            long id=kind.equals("note")?7:kind.equals("adhan")?AdhanScheduler.FAJR:ToolAlarmScheduler.alarmId(kind);
            String payload=new JSONObject().put("image1","content://old/image").put("attachments","[]").toString();
            assertTrue(AlertSnoozes.schedule(app,kind,id,"Original",payload,new int[]{5}));
            String key=prefs().getAll().keySet().iterator().next();
            FixedClock.now+=5*60000L;
            AlertSnoozes.deliver(app,uri(key));Intent service=Shadows.shadowOf(app).getNextStartedService();assertNotNull(service);
            if(kind.equals("note")) {assertEquals(NoForgetSoundService.class.getName(),service.getComponent().getClassName());assertEquals(original,new NoForgetStore(app).find(7).dueAtMillis);}
            else if(kind.equals("adhan")) assertEquals(AdhanSoundService.class.getName(),service.getComponent().getClassName());
            else {assertEquals(kind,service.getStringExtra("toolKind"));assertEquals(payload,service.getStringExtra("toolPayload"));}
            assertTrue(prefs().getAll().isEmpty());
        }
    }
    @Test public void invalidSelectionCannotPartiallyScheduleAndCancellationOnlyRemovesItsSource() {
        assertFalse(AlertSnoozes.schedule(app,"adhan",0,"","",new int[]{5,9}));assertTrue(prefs().getAll().isEmpty());
        assertTrue(AlertSnoozes.schedule(app,"adhan",0,"","",new int[]{5,10}));
        assertTrue(AlertSnoozes.schedule(app,"timer",ToolAlarmScheduler.alarmId("timer"),"","{}",new int[]{15}));
        AlertSnoozes.cancel(app,"adhan",0);assertEquals(1,prefs().getAll().size());
        AlertSnoozes.deliver(app,uri("unknown"));assertNull(Shadows.shadowOf(app).getNextStartedService());
    }
    @Test @Config(shadows={FailingAlarmManager.class})
    public void failureInMiddleRollsBackNewBatchAndPreservesEarlierReminder() {
        FailingAlarmManager.calls=0;FailingAlarmManager.failAt=0;
        assertTrue(AlertSnoozes.schedule(app,"adhan",1,"","",new int[]{60}));
        Map<String,?> original=new HashMap<>(prefs().getAll());
        FailingAlarmManager.failAt=FailingAlarmManager.calls+2;
        assertFalse(AlertSnoozes.schedule(app,"adhan",0,"","",new int[]{5,10,15}));
        assertEquals(original,prefs().getAll());
        assertEquals(1,Shadows.shadowOf(app.getSystemService(AlarmManager.class)).getScheduledAlarms().size());
    }
    @Test public void restoreKeepsFutureRemindersAndReplaysOnlyLatestOverdueOccurrence() throws Exception {
        assertTrue(AlertSnoozes.schedule(app,"adhan",0,"","",new int[]{5,10,15,60,1440}));
        FixedClock.now+=30*60000L;
        AlertSnoozes.restore(app);
        assertEquals(3,prefs().getAll().size());
        int overdue=0;
        for(Object value:prefs().getAll().values())if(new JSONObject((String)value).getLong("when")<FixedClock.now)overdue++;
        assertEquals(1,overdue);
    }
    @org.robolectric.annotation.Implements(AlarmManager.class)
    public static class FailingAlarmManager extends org.robolectric.shadows.ShadowAlarmManager {
        static int calls,failAt;
        @org.robolectric.annotation.Implementation protected void setAlarmClock(AlarmManager.AlarmClockInfo info,PendingIntent operation) {
            if(++calls==failAt)throw new SecurityException("permission revoked during confirmation");
            super.setAlarmClock(info,operation);
        }
    }
    @org.robolectric.annotation.Implements(value=AlertSnoozes.class,isInAndroidSdk=false)
    public static class FixedClock {
        static long now;
        @org.robolectric.annotation.Implementation protected static long nowMillis(){return now;}
    }
    private SharedPreferences prefs(){return app.getSharedPreferences(AlertSnoozes.PREFS,0);}
    private Uri uri(String key){return Uri.parse("advanceclock://alert-snooze/"+key);}
    private String keyAt(long deadline)throws Exception {for(Map.Entry<String,?> e:prefs().getAll().entrySet())if(new JSONObject((String)e.getValue()).getLong("when")==deadline)return e.getKey();throw new AssertionError("missing time");}
}
