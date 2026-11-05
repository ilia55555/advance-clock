package com.ilia.advanceclock;

import android.app.Application;
import android.content.*;
import android.content.res.Resources;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class BundledAlarmSoundsTest {
    static final String[] NAMES={"a_quiet_thought","aegean_sea","ballerina","bells","cute_bells","for_love","i_dont_wanna_live_forever","metamorphosis","morning_flower","new_york","piano","nature"};
    Context context(Set<String> present){
        Context base=RuntimeEnvironment.getApplication();Resources original=base.getResources();
        Resources resources=new Resources(original.getAssets(),original.getDisplayMetrics(),original.getConfiguration()){
            @Override public int getIdentifier(String name,String type,String pkg){if(type.equals("raw") && present.contains(name))return R.raw.rawadhan_1;return super.getIdentifier(name,type,pkg);}
        };
        return new ContextWrapper(base){@Override public Resources getResources(){return resources;}};
    }
    @Test public void allTwelveUseTheirOwnNamesAndLocalResourceUrisInThreeLanguages(){
        Set<String> present=new HashSet<>();for(String name:NAMES)present.add("alarm_"+name);
        Context c=context(present);
        for(String[] language:new String[][]{{"en","A Quiet Thought"},{"fa","اندیشه‌ای آرام"},{"ar","خاطرة هادئة"}}){
            AppSettings.setLanguage(c,language[0]);AppString.init(c);List<SoundLibrary.Sound> sounds=SoundLibrary.all(c);
            assertEquals(12,sounds.stream().filter(s->s.uri.contains("/raw/alarm_") && !s.uri.matches(".*/alarm_[0-9]+$")).count());
            for(String name:NAMES){String uri="android.resource://"+c.getPackageName()+"/raw/alarm_"+name;assertTrue(uri,sounds.stream().anyMatch(s->uri.equals(s.uri)));}
            assertEquals(language[1],SoundLibrary.name(c,"android.resource://"+c.getPackageName()+"/raw/alarm_a_quiet_thought"));
            assertTrue(SoundLibrary.all(c,true).stream().noneMatch(s->s.uri.contains("/raw/alarm_")));
        }
    }
    @Test public void missingMp3sDoNotCreateBrokenChoicesAndExistingSoundsRemain(){
        Context c=context(Collections.singleton("alarm_piano"));AppString.init(c);
        List<SoundLibrary.Sound> sounds=SoundLibrary.all(c);assertTrue(sounds.stream().anyMatch(s->s.uri.endsWith("/alarm_piano")));
        assertFalse(sounds.stream().anyMatch(s->s.uri.endsWith("/alarm_nature")));assertEquals("",sounds.get(0).uri);
    }
}
