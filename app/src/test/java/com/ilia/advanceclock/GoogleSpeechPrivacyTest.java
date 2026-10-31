package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.speech.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class GoogleSpeechPrivacyTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    @Test public void voiceOpensOnlyGoogleUiAndNeverSendsExistingPrivateText()throws Exception {
        Application app=RuntimeEnvironment.getApplication();
        ResolveInfo google=new ResolveInfo();google.activityInfo=new ActivityInfo();
        google.activityInfo.packageName=SmartAlarmActivity.GOOGLE_SPEECH_PACKAGE;google.activityInfo.name="GoogleVoice";
        Shadows.shadowOf(app.getPackageManager()).addResolveInfoForIntent(SmartAlarmActivity.speechIntent(app),google);
        try(var controller=Robolectric.buildActivity(SmartAlarmActivity.class).setup().visible()) {
            SmartAlarmActivity host=controller.get();
            EditText input=org.robolectric.util.ReflectionHelpers.getField(host,"input");input.setText("private note that must stay local");
            host.findViewById(android.R.id.content).findViewWithTag("smart_voice_input").performClick();
            Intent speech=Shadows.shadowOf(host).getNextStartedActivity();
            assertNotNull(speech);assertEquals(SmartAlarmActivity.GOOGLE_SPEECH_PACKAGE,speech.getPackage());
            assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH,speech.getAction());
            assertFalse(speech.getBooleanExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,true));
            assertNull(speech.getData());
            for(String key:speech.getExtras().keySet())assertFalse(String.valueOf(speech.getExtras().get(key)).contains("private note"));
            assertNull(org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer());
        }
        String[] permissions=app.getPackageManager().getPackageInfo(app.getPackageName(),PackageManager.GET_PERMISSIONS).requestedPermissions;
        assertFalse(Arrays.asList(permissions).contains(android.Manifest.permission.RECORD_AUDIO));
    }
    @Test public void missingGoogleDoesNotFallBackToAnArbitraryRecognitionService() {
        try(var controller=Robolectric.buildActivity(SmartAlarmActivity.class).setup().visible()) {
            SmartAlarmActivity host=controller.get();host.findViewById(android.R.id.content).findViewWithTag("smart_voice_input").performClick();
            assertNull(Shadows.shadowOf(host).getNextStartedActivity());
            assertNull(org.robolectric.shadows.ShadowSpeechRecognizer.getLatestSpeechRecognizer());
        }
    }
}
