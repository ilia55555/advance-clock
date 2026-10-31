package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import org.json.JSONObject;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class UpdateDownloadProgressTest {
    private Context app;
    @Before public void setup(){app=RuntimeEnvironment.getApplication();}
    private AppUpdates.Release release(byte[] payload,int size) throws Exception {
        return new AppUpdates.Release(new JSONObject().put("versionCode",7).put("versionName","1.0.0.7").put("tag","v1.0.0.7")
                .put("size",size).put("sha256",AppUpdates.hex(MessageDigest.getInstance("SHA-256").digest(payload))).toString());
    }
    private Set<String> staged(){String[] names=app.getCacheDir().list((d,n)->n.startsWith("update-")&&n.endsWith(".apk"));return new HashSet<>(Arrays.asList(names==null?new String[0]:names));}
    static class Connection extends HttpURLConnection {
        final byte[] bytes;boolean disconnected;
        Connection(byte[] bytes)throws Exception{super(new URL("https://github.com/release"));this.bytes=bytes;}
        @Override public int getResponseCode(){return 200;}
        @Override public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}
        @Override public void disconnect(){disconnected=true;}
        @Override public void connect(){}
        @Override public boolean usingProxy(){return false;}
    }
    @Test public void byteCountsAdvanceBeforeVerificationAndUntrustedApkIsDeleted() throws Exception {
        byte[] bytes=new byte[100000];Arrays.fill(bytes,(byte)42);Connection conn=new Connection(bytes);Set<String> before=staged();
        List<Long> counts=new ArrayList<>();boolean[] verifying={false};
        try{
            AppUpdates.download(app,release(bytes,bytes.length),(done,total,checking)->{
                assertEquals(bytes.length,total);assertTrue(done>=0 && done<=total);counts.add(done);verifying[0]|=checking;
            },new ForegroundUpdates.Request(null),(u,r)->conn);fail("Untrusted bytes must not become an APK");
        }catch(AppUpdates.UpdateFailure error){assertEquals(R.string.update_signature_error,error.message);}
        assertEquals(Long.valueOf(0),counts.get(0));assertEquals(Long.valueOf(bytes.length),counts.get(counts.size()-1));
        assertTrue(counts.size()>3);assertTrue(verifying[0]);assertTrue(conn.disconnected);assertEquals(before,staged());
    }
    @Test public void sizeMismatchNeverReachesCertificateCheckAndRemovesPrivateFile() throws Exception {
        byte[] bytes=new byte[100];Set<String> before=staged();boolean[] verifying={false};
        for(int size:new int[]{99,101}) {
            Connection conn=new Connection(bytes);
            try{AppUpdates.download(app,release(bytes,size),(d,t,v)->verifying[0]|=v,new ForegroundUpdates.Request(null),(u,r)->conn);fail();}
            catch(AppUpdates.UpdateFailure error){assertEquals(R.string.update_hash_error,error.message);}
            assertTrue(conn.disconnected);assertEquals(before,staged());assertFalse(verifying[0]);
        }
    }
    @Test public void cancelledExportDoesNotWriteAnythingAndNoInstallerIsLaunched() throws Exception {
        ForegroundUpdates.Request request=new ForegroundUpdates.Request(null);request.cancelled=true;
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        assertThrows(IOException.class,()->UpdateFiles.copy(new ByteArrayInputStream(new byte[100]),out,request));
        assertEquals(0,out.size());
    }
}
