package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import android.widget.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk=35,shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class SmartScheduleActivityTest {
    @Test public void directSpeechInsertsResultsAndReleasesMicrophoneSession() {
        Application app=RuntimeEnvironment.getApplication();
        AppSettings.setLanguage(app,"fa");
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.RECORD_AUDIO);
        android.content.pm.ResolveInfo engine=new android.content.pm.ResolveInfo();
        engine.serviceInfo=new android.content.pm.ServiceInfo();
        engine.serviceInfo.packageName="test.speech";engine.serviceInfo.name="test.speech.Engine";
        Shadows.shadowOf(app.getPackageManager()).addResolveInfoForIntent(
                new Intent("android.speech.RecognitionService"),engine);
        SmartAlarmActivity host=activity(false);
        host.findViewById(android.R.id.content).findViewWithTag("smart_voice_input").performClick();
        android.speech.SpeechRecognizer recognizer=org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer();
        assertNotNull(recognizer);
        org.robolectric.shadows.ShadowSpeechRecognizer shadow=Shadows.shadowOf(recognizer);
        assertEquals("fa-IR",shadow.getLastRecognizerIntent().getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE));
        android.os.Bundle results=new android.os.Bundle();
        results.putStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION,
                new ArrayList<>(Collections.singletonList("فردا ساعت 08:30 جلسه")));
        shadow.triggerOnResults(results);
        EditText field=ReflectionHelpers.getField(host,"input");
        assertTrue(field.getText().toString().contains("08:30"));
        assertTrue(shadow.isDestroyed());
        assertTrue(ReflectionHelpers.<Boolean>getField(host,"listening"));
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));
        android.speech.SpeechRecognizer next=org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer();
        assertNotSame(recognizer,next);
        Shadows.shadowOf(next).triggerOnError(android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT);
        assertTrue(ReflectionHelpers.<Boolean>getField(host,"listening"));
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(400));
        android.speech.SpeechRecognizer afterPause=org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer();
        assertNotSame(next,afterPause);
        Shadows.shadowOf(afterPause).triggerOnError(android.speech.SpeechRecognizer.ERROR_NETWORK);
        assertTrue(ReflectionHelpers.<Boolean>getField(host,"listening"));
        host.findViewById(android.R.id.content).findViewWithTag("smart_voice_input").performClick();
        assertFalse(ReflectionHelpers.<Boolean>getField(host,"listening"));
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(4));
        assertSame(afterPause,org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer());
        assertNull(Shadows.shadowOf(host).getNextStartedActivity());
        host.finish();
    }
    @Test public void speechUsesAppLanguageWithoutRequiringOfflineLanguagePack() {
        Application app=RuntimeEnvironment.getApplication();
        for(String[] pair:new String[][]{{"fa","fa-IR"},{"en","en-US"},{"ar","ar-SA"}}) {
            AppSettings.setLanguage(app,pair[0]);
            Intent intent=SmartAlarmActivity.speechIntent(app);
            assertEquals(pair[1],intent.getStringExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE));
            assertFalse(intent.getBooleanExtra(android.speech.RecognizerIntent.EXTRA_PREFER_OFFLINE,false));
        }
    }

    @Test public void bothModesHaveTwoExamplesAndSpeechBeforeTimeWithIcons() throws Exception {
        for(boolean note:new boolean[]{false,true}) {
            SmartAlarmActivity host=activity(note);
            EditText field=ReflectionHelpers.getField(host,"input");
            String hint=field.getHint().toString();
            assertTrue(hint.contains("09:30"));
            String json=hint.substring(hint.indexOf('{'));
            assertEquals("09:30",new org.json.JSONObject(json).getString("time"));
            android.view.View time=host.findViewById(android.R.id.content).findViewWithTag("smart_insert_time");
            android.view.View voice=host.findViewById(android.R.id.content).findViewWithTag("smart_voice_input");
            LinearLayout helpers=(LinearLayout)time.getParent();
            assertEquals(0,helpers.indexOfChild(voice)); assertEquals(1,helpers.indexOfChild(time));
            assertNotNull(((Button)voice).getCompoundDrawablesRelative()[0]);
            assertNotNull(((Button)time).getCompoundDrawablesRelative()[0]);
            host.finish();
        }
    }
    private SmartAlarmActivity activity(boolean note) {
        AppString.init(RuntimeEnvironment.getApplication());
        Shadows.shadowOf((AlarmManager)RuntimeEnvironment.getApplication().getSystemService("alarm")).setCanScheduleExactAlarms(true);
        return Robolectric.buildActivity(SmartAlarmActivity.class,new Intent().putExtra("smartNote",note)).setup().get();
    }
    private void input(SmartAlarmActivity host,String text) {
        EditText input=ReflectionHelpers.getField(host,"input");input.setText(text);
        ReflectionHelpers.callInstanceMethod(host,"analyzeInput");
    }
    @Test public void alarmSavePreservesIntervalRule() {
        SmartAlarmActivity host=activity(false);
        input(host,"هر ده روز ساعت 08:30 دارو");
        ReflectionHelpers.callInstanceMethod(host,"saveAllAlarms");
        List<AlarmItem> saved=new AlarmStore(host).all();assertEquals(1,saved.size());
        assertEquals(RecurrenceUtils.INTERVAL_DAYS,saved.get(0).recurrenceMode);assertEquals(10,saved.get(0).intervalDays);
        host.finish();
    }
    @Test public void smartNoteSavesNoteAndWeekdayScheduleNotAnAlarm() {
        SmartAlarmActivity host=activity(true);
        input(host,"روزهای شنبه و یکشنبه ساعت 08:30 خرید");
        ReflectionHelpers.callInstanceMethod(host,"saveAllAlarms");
        List<NoForgetItem> notes=new NoForgetStore(host).all();assertEquals(1,notes.size());
        assertTrue(notes.get(0).reminderEnabled);assertTrue(notes.get(0).hasDue);
        assertEquals(RecurrenceUtils.WEEKDAYS,notes.get(0).recurrenceMode);
        assertEquals(2,RecurrenceUtils.parseWeekdays(notes.get(0).customDatesJson).size());
        assertEquals(0,new AlarmStore(host).all().size());host.finish();
    }
    @Test public void timePickerInsertsPaddedTimeAndUpdatesPreviewWithoutSaving() {
        SmartAlarmActivity host=activity(true);
        EditText input=ReflectionHelpers.getField(host,"input");input.setText("فردا خرید ");input.setSelection(input.length());
        host.findViewById(android.R.id.content).findViewWithTag("smart_insert_time").performClick();
        TimePickerDialog dialog=(TimePickerDialog)org.robolectric.shadows.ShadowDialog.getLatestDialog();
        dialog.updateTime(7,5);dialog.getButton(Dialog.BUTTON_POSITIVE).performClick();
        assertTrue(input.getText().toString().contains("07:05"));
        ArrayList<SmartAlarmParser.Candidate> candidates=ReflectionHelpers.getField(host,"candidates");
        assertEquals(1,candidates.size());assertEquals(7,candidates.get(0).hour);
        assertTrue(new NoForgetStore(host).all().isEmpty());host.finish();
    }
    @Test public void dictationUsesSameParserAndDoesNotSaveAutomatically() {
        SmartAlarmActivity host=activity(false);
        Intent speech=new Intent().putStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS,
                new ArrayList<>(Collections.singletonList("فردا ساعت هشت و نیم صبح دارو")));
        host.onActivityResult(7101,Activity.RESULT_OK,speech);
        ArrayList<SmartAlarmParser.Candidate> candidates=ReflectionHelpers.getField(host,"candidates");
        assertEquals(1,candidates.size());assertEquals(8,candidates.get(0).hour);assertEquals(30,candidates.get(0).minute);
        assertTrue(new AlarmStore(host).all().isEmpty());host.finish();
    }
    @Test public void minuteRecurrenceSurvivesAlarmAndNoteSerialization() throws Exception {
        SmartAlarmActivity host=activity(false);
        input(host,"هر 10 دقیقه دارو");
        ReflectionHelpers.callInstanceMethod(host,"saveAllAlarms");
        AlarmItem item=new AlarmStore(host).all().get(0);
        assertEquals(RecurrenceUtils.INTERVAL_MINUTES,item.recurrenceMode);assertEquals(10,item.intervalDays);
        AlarmItem restored=AlarmItem.fromJson(item.toJson());assertEquals(RecurrenceUtils.INTERVAL_MINUTES,restored.recurrenceMode);
        NoForgetItem note=new NoForgetItem(1,"note","body","[]",2,true,item.triggerAtMillis,true,System.currentTimeMillis(),
                RecurrenceUtils.INTERVAL_MINUTES,10,"[]");
        assertEquals(RecurrenceUtils.INTERVAL_MINUTES,NoForgetItem.fromJson(note.toJson()).recurrenceMode);
        host.finish();
    }

}
