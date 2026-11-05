package com.ilia.advanceclock;

import android.app.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows={ReminderScreenFrameworkTest.PlainStore.class,SoundPreviewTest.Metadata.class})
public class SoundPickerListTest {
    Application c;byte[] bytes;
    @Before public void init()throws Exception{c=RuntimeEnvironment.getApplication();AppString.init(c);bytes="ID3sound".getBytes("UTF-8");}
    DownloadableSounds.Entry entry(String id,int version)throws Exception{
        StringBuilder hash=new StringBuilder();for(byte b:java.security.MessageDigest.getInstance("SHA-256").digest(bytes))hash.append(String.format(Locale.ROOT,"%02x",b&255));
        return new DownloadableSounds.Entry(new JSONObject().put("id",id).put("name",id).put("file",id+".mp3").put("version",version).put("size",bytes.length).put("sha256",hash.toString()));
    }
    void download(DownloadableSounds.Entry e)throws Exception{DownloadableSounds.download(c,e,new DownloadableSounds.Request(),(d,t)->{},url->new DownloadableSoundsTest.Response(url,bytes,200));}
    @Test public void systemDefaultStaysFirstAndDownloadsAreNewestFirstInTheSameList()throws Exception{
        DownloadableSounds.Entry first=entry("first",1),second=entry("second",1),notDownloaded=entry("available",1);download(first);download(second);
        c.getSharedPreferences("downloaded_sounds",0).edit().putString("catalog",DownloadableSounds.encode(Arrays.asList(first,second,notDownloaded))).commit();SoundLibrary.add(c,Uri.parse("content://media/personal.mp3"));
        try(var host=Robolectric.buildActivity(SoundPickerActivity.class).setup().visible()){
            LinearLayout list=ReflectionHelpers.getField(host.get(),"list");
            assertEquals("",((SoundPreviewRow)list.getChildAt(0)).uri);
            assertEquals(DownloadableSounds.uri(c,second),((SoundPreviewRow)list.getChildAt(1)).uri);
            assertEquals(DownloadableSounds.uri(c,first),((SoundPreviewRow)list.getChildAt(2)).uri);
            assertEquals(DownloadableSounds.uri(c,notDownloaded),((SoundPreviewRow)list.getChildAt(3)).uri);
            assertEquals("content://media/personal.mp3",((SoundPreviewRow)list.getChildAt(4)).uri);
            for(int i=0;i<list.getChildCount();i++)assertTrue(list.getChildAt(i) instanceof SoundPreviewRow);
            for(int i=5;i<list.getChildCount();i++)assertTrue(((SoundPreviewRow)list.getChildAt(i)).uri.startsWith("android.resource://"));
        }
        download(entry("first",2));
        try(var host=Robolectric.buildActivity(SoundPickerActivity.class).setup().visible()){
            LinearLayout list=ReflectionHelpers.getField(host.get(),"list");assertEquals("",((SoundPreviewRow)list.getChildAt(0)).uri);assertEquals(DownloadableSounds.uri(c,first),((SoundPreviewRow)list.getChildAt(1)).uri);
        }
    }
    @Test public void clickingCheckShowsInfinityImmediatelyAndLeavingCancelsRequestAndAnimation()throws Exception{
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
        try(var host=Robolectric.buildActivity(SoundPickerActivity.class).setup().visible()){
            ExecutorService worker=ReflectionHelpers.getField(host.get(),"soundWorker");worker.submit(()->{entered.countDown();try{release.await();}catch(InterruptedException ignored){Thread.currentThread().interrupt();}});
            assertTrue(entered.await(2,TimeUnit.SECONDS));
            View root=host.get().findViewById(android.R.id.content);Button check=root.findViewWithTag("online_sounds_check");View loading=root.findViewWithTag("online_sounds_loading");assertEquals(View.GONE,loading.getVisibility());
            check.performClick();assertEquals(View.VISIBLE,loading.getVisibility());assertFalse(check.isEnabled());assertEquals("",check.getText().toString());
            assertEquals(c.getString(R.string.online_sounds_checking),check.getContentDescription());
            DownloadableSounds.Request request=ReflectionHelpers.getField(host.get(),"soundRequest");assertNotNull(request);assertNull(request.connection);
            host.pause().stop().destroy();assertTrue(request.cancelled);assertNull(ReflectionHelpers.getField(loading,"animator"));
        }finally{release.countDown();}
    }
}
