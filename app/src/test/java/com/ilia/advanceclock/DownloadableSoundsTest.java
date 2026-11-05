package com.ilia.advanceclock;

import android.app.*;
import android.content.*;
import android.net.Uri;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class DownloadableSoundsTest {
    Context c;
    @Before public void setup(){c=RuntimeEnvironment.getApplication();AppString.init(c);}
    String digest(byte[] data)throws Exception{StringBuilder value=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(data))value.append(String.format(Locale.ROOT,"%02x",b&255));return value.toString();}
    JSONObject entry(byte[] data,int version)throws Exception{return new JSONObject().put("id","quiet-thought").put("name","A Quiet Thought").put("file","A Quiet Thought.mp3").put("version",version).put("size",data.length).put("sha256",digest(data));}
    DownloadableSounds.Entry sound(byte[] data,int version)throws Exception{return new DownloadableSounds.Entry(entry(data,version));}
    static class Response extends HttpURLConnection {
        final byte[] bytes;final int status;boolean disconnected;
        Response(URL url,byte[] data,int status){super(url);bytes=data;this.status=status;}
        @Override public int getResponseCode(){return status;}
        @Override public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
        @Override public long getContentLengthLong(){return bytes.length;}
        @Override public void disconnect(){disconnected=true;}
        @Override public void connect(){}
        @Override public boolean usingProxy(){return false;}
    }
    @Test public void catalogRejectsArbitraryPathsDuplicateIdsAndInvalidHashes()throws Exception{
        byte[] data="ID3test".getBytes("UTF-8");JSONObject good=entry(data,1);
        assertEquals(1,DownloadableSounds.parse(new JSONObject().put("schemaVersion",1).put("sounds",new JSONArray().put(good)).toString()).size());
        for(String file:new String[]{"../secret.mp3","https://evil.test/a.mp3","nested/a.mp3","a.apk","a%2f.mp3","/a.mp3","a\\b.mp3"}){
            JSONObject invalid=entry(data,1).put("file",file);assertThrows(Exception.class,()->new DownloadableSounds.Entry(invalid));
        }
        assertThrows(Exception.class,()->new DownloadableSounds.Entry(entry(data,1).put("sha256","bad")));
        assertThrows(Exception.class,()->new DownloadableSounds.Entry(entry(data,1).put("size",DownloadableSounds.MAX_FILE+1)));
        assertThrows(Exception.class,()->DownloadableSounds.parse(new JSONObject().put("schemaVersion",1).put("sounds",new JSONArray().put(good).put(good)).toString()));
    }
    @Test public void networkingOnlyAllowsOfficialFolderWithoutQueryOrRedirectTargets()throws Exception{
        assertTrue(DownloadableSounds.allowed(new URL(DownloadableSounds.BASE+"alarms.json")));
        assertTrue(DownloadableSounds.allowed(new URL(DownloadableSounds.BASE+"I%20Don't%20Wanna%20Live%20Forever.mp3")));
        for(String bad:new String[]{"http://raw.githubusercontent.com/infinitimeilia/infinitime/main/alarm/a.mp3",DownloadableSounds.BASE+"../a.mp3",DownloadableSounds.BASE+"%2e%2e%2fa.mp3",DownloadableSounds.BASE+"a.mp3?notes=private",DownloadableSounds.BASE+"a.mp3#x","https://raw.githubusercontent.com/evil/repo/main/alarm/a.mp3","https://raw.githubusercontent.com@evil.test/a.mp3","https://evil.test/a.mp3"}){
            assertFalse(bad,DownloadableSounds.allowed(new URL(bad)));assertThrows(IOException.class,()->ReadOnlyNetwork.sound(new URL(bad)));
        }
    }
    @Test public void verifiedDownloadsPlayOfflineAndUpdatesKeepTheSameSelectedUri()throws Exception{
        byte[] first="ID3first sound".getBytes("UTF-8"),second="ID3updated sound".getBytes("UTF-8");DownloadableSounds.Entry e=sound(first,1);
        long[] progress={0};DownloadableSounds.download(c,e,new DownloadableSounds.Request(),(done,total)->progress[0]=done,u->new Response(u,first,200));
        assertEquals(first.length,progress[0]);String uri=DownloadableSounds.uri(c,e);assertTrue(DownloadableSounds.current(c,e));
        assertEquals(e.name,SoundLibrary.name(c,uri));LocalMedia.require(Uri.parse(uri));
        DownloadableSounds.Entry updated=sound(second,2);assertFalse(DownloadableSounds.current(c,updated));
        DownloadableSounds.download(c,updated,new DownloadableSounds.Request(),(d,t)->{},u->new Response(u,second,200));
        assertEquals(uri,DownloadableSounds.uri(c,updated));assertArrayEquals(second,Files.readAllBytes(new File(Uri.parse(uri).getPath()).toPath()));
        SoundLibrary.add(c,Uri.parse("content://media/external/audio/media/123"));
        assertEquals(1,SoundLibrary.all(c).stream().filter(s->uri.equals(s.uri)).count());
        assertThrows(SecurityException.class,()->DownloadableSounds.download(c,e,new DownloadableSounds.Request(),(d,t)->{},u->new Response(u,first,200)));
    }
    @Test public void corruptTruncatedCancelledAndRedirectedDownloadsPreserveInstalledSound()throws Exception{
        byte[] good="ID3original".getBytes("UTF-8"),next="ID3newversion".getBytes("UTF-8");DownloadableSounds.Entry old=sound(good,1),updated=sound(next,2);
        DownloadableSounds.download(c,old,new DownloadableSounds.Request(),(d,t)->{},u->new Response(u,good,200));File file=new File(Uri.parse(DownloadableSounds.uri(c,old)).getPath());
        byte[] corrupt=next.clone();corrupt[3]^=1;
        assertThrows(SecurityException.class,()->DownloadableSounds.download(c,updated,new DownloadableSounds.Request(),(d,t)->{},u->new Response(u,corrupt,200)));
        assertThrows(SecurityException.class,()->DownloadableSounds.download(c,updated,new DownloadableSounds.Request(),(d,t)->{},u->new Response(u,new byte[2],200)));
        assertThrows(IOException.class,()->DownloadableSounds.download(c,updated,new DownloadableSounds.Request(),(d,t)->{},u->new Response(u,next,302)));
        DownloadableSounds.Request cancelled=new DownloadableSounds.Request();cancelled.cancel();
        assertThrows(IOException.class,()->DownloadableSounds.download(c,updated,cancelled,(d,t)->{},u->{throw new AssertionError("Cancelled request made a connection");}));
        assertArrayEquals(good,Files.readAllBytes(file.toPath()));assertTrue(DownloadableSounds.current(c,old));assertEquals(0,Objects.requireNonNull(file.getParentFile().listFiles((dir,name)->name.endsWith(".part"))).length);
    }
    @Test public void pickerUsesCachedCatalogAndInstalledFilesWithoutStartingNetworkRequests()throws Exception{
        byte[] data="ID3local".getBytes("UTF-8");DownloadableSounds.Entry e=sound(data,1);
        c.getSharedPreferences("downloaded_sounds",0).edit().putString("catalog",DownloadableSounds.encode(Collections.singletonList(e))).commit();
        try(var host=Robolectric.buildActivity(SoundPickerActivity.class).setup().visible()){
            assertNotNull(host.get().findViewById(android.R.id.content).findViewWithTag("online_sounds_check"));
            assertNull(org.robolectric.util.ReflectionHelpers.getField(host.get(),"soundRequest"));
        }
    }
}
