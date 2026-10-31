package com.ilia.advanceclock;

import android.app.Application;
import android.net.Uri;
import java.io.*;
import java.net.*;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class ReadOnlyNetworkTest {
    @Test public void updateDestinationIsPinnedAndMetadataCannotSupplyAnotherLink()throws Exception {
        assertTrue(ReadOnlyNetwork.updateUrl(new URL(ReadOnlyNetwork.REPOSITORY+"/releases/latest/download/update.json")));
        for(String bad:new String[]{"http://github.com/infinitimeilia/infinitime/releases/latest/download/update.json",
                "https://github.com/evil/app/releases/latest/download/InfiniTime.apk",
                "https://github.com/infinitimeilia/infinitime/releases/latest/download/user-data.txt",
                "https://github.com@evil.test/infinitimeilia/infinitime/releases/latest/download/InfiniTime.apk",
                "https://github.com/infinitimeilia/infinitime/releases/latest/download/update.json?data=note",
                "https://release-assets.githubusercontent.com/other/path"})assertFalse(bad,ReadOnlyNetwork.updateUrl(new URL(bad)));
        AppUpdates.Release release=new AppUpdates.Release(new JSONObject().put("versionCode",9).put("versionName","1.0.0.9").put("tag","v1.0.0.9")
                .put("size",100).put("sha256","a".repeat(64)).put("url","https://evil.test/payload.apk").put("repository","https://github.com/evil/app").toString());
        assertEquals(ReadOnlyNetwork.REPOSITORY+"/releases/download/v1.0.0.9/InfiniTime.apk",release.url(ReadOnlyNetwork.REPOSITORY));
        assertThrows(IOException.class,()->release.url("https://github.com/evil/app"));
    }
    @Test public void locationAllowsOnlyRequiredQueriesAndRejectsExtraData()throws Exception {
        String city="https://nominatim.openstreetmap.org/search?q=Tehran&format=jsonv2&addressdetails=1&limit=20&accept-language=fa,en";
        String coordinate="https://timeapi.io/api/timezone/coordinate?latitude=35.7&longitude=51.4";
        ReadOnlyNetwork.locationUrl(new URL(city));ReadOnlyNetwork.locationUrl(new URL(coordinate));
        ReadOnlyNetwork.locationUrl(new URL("https://api.open-meteo.com/v1/forecast?latitude=35.7&longitude=51.4&current=temperature_2m&timezone=auto&forecast_days=1"));
        for(String bad:new String[]{city+"&notes=private",coordinate+"&file=photo",coordinate+"&latitude=36",
                "https://timeapi.io/api/timezone/coordinate?latitude=NaN&longitude=1",
                "https://timeapi.io/api/timezone/coordinate?latitude=91&longitude=1",
                "https://timeapi.io/other?latitude=1&longitude=1","https://evil.test/search?q=Tehran",
                "http://nominatim.openstreetmap.org/search?q=Tehran","https://user:password@timeapi.io/api/timezone/coordinate?latitude=1&longitude=1"})
            assertThrows(bad,IOException.class,()->ReadOnlyNetwork.locationUrl(new URL(bad)));
    }
    @Test public void bodyUploadMethodsPrivateHeadersAndAutomaticRedirectsCannotBeEnabled()throws Exception {
        HttpURLConnection delegate=new HttpURLConnection(new URL(ReadOnlyNetwork.REPOSITORY+"/releases/latest/download/update.json")){
            @Override public void connect(){}
            @Override public void disconnect(){}
            @Override public boolean usingProxy(){return false;}
            @Override public OutputStream getOutputStream(){throw new AssertionError("Must never reach transport output");}
        };
        ReadOnlyNetwork.DownloadConnection connection=new ReadOnlyNetwork.DownloadConnection(delegate);
        for(String method:new String[]{"POST","PUT","PATCH","DELETE","CONNECT"})assertThrows(ProtocolException.class,()->connection.setRequestMethod(method));
        assertThrows(IllegalStateException.class,()->connection.setDoOutput(true));assertThrows(ProtocolException.class,connection::getOutputStream);
        assertThrows(IllegalArgumentException.class,()->connection.setInstanceFollowRedirects(true));
        assertThrows(IllegalArgumentException.class,()->connection.setRequestProperty("Cookie","private"));
        assertThrows(IllegalArgumentException.class,()->connection.setRequestProperty("Authorization","secret"));
        assertThrows(IllegalArgumentException.class,()->connection.setRequestProperty("User-Agent","a private note"));
        connection.setRequestMethod("GET");connection.setRequestProperty("Accept","application/json");
        assertFalse(connection.getDoOutput());assertFalse(connection.getInstanceFollowRedirects());
    }
    @Test public void manualApkSaveCannotUploadThroughACloudDocumentProvider()throws Exception {
        UpdateFiles.requireLocalDestination(Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2Fapp.apk"));
        UpdateFiles.requireLocalDestination(Uri.parse("content://com.android.providers.downloads.documents/document/123"));
        assertThrows(IOException.class,()->UpdateFiles.requireLocalDestination(Uri.parse("content://com.google.android.apps.docs.storage/document/private")));
        assertThrows(IOException.class,()->UpdateFiles.requireLocalDestination(Uri.parse("https://evil.test/upload")));
    }
    @Test public void remoteMediaCannotBypassTheHttpBoundary() {
        for(String value:new String[]{"https://evil.test/audio","http://evil.test/video","rtsp://evil.test/live","ftp://evil.test/a"})
            assertThrows(IllegalArgumentException.class,()->LocalMedia.require(Uri.parse(value)));
        assertEquals("content",LocalMedia.require(Uri.parse("content://media/external/audio/1")).getScheme());
        assertEquals("android.resource",LocalMedia.require(Uri.parse("android.resource://com.ilia.advanceclock/raw/alarm1")).getScheme());
    }
}
