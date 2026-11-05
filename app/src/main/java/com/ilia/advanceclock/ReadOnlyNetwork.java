package com.ilia.advanceclock;

import android.net.Uri;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.HttpsURLConnection;

/** Single outbound HTTP boundary: HTTPS, approved destinations, GET/HEAD and no request body. */
final class ReadOnlyNetwork {
    static final String REPOSITORY="https://github.com/infinitimeilia/infinitime";
    private ReadOnlyNetwork(){}
    static boolean updateUrl(URL u){
        if(!secure(u))return false;
        if("github.com".equals(u.getHost()))
            return u.getQuery()==null && u.getPath().matches("/infinitimeilia/infinitime/releases/(?:latest/download|download/v[0-9]+(?:\\.[0-9]+){3})/(?:update\\.json|InfiniTime\\.apk)");
        return ("release-assets.githubusercontent.com".equals(u.getHost()) || "objects.githubusercontent.com".equals(u.getHost()))
                && u.getPath().startsWith("/github-production-release-asset/");
    }
    private static boolean secure(URL u){return u!=null && "https".equals(u.getProtocol()) && u.getUserInfo()==null
            && (u.getPort()==-1 || u.getPort()==443) && u.getRef()==null;}
    static void locationUrl(URL u)throws IOException{
        if(!secure(u))throw new IOException("Only HTTPS location requests are allowed");
        Uri uri=Uri.parse(u.toString());Set<String> keys=uri.getQueryParameterNames();Set<String> allowed;
        if("nominatim.openstreetmap.org".equals(u.getHost()) && "/search".equals(u.getPath())) {
            allowed=new HashSet<>(Arrays.asList("q","format","addressdetails","limit","accept-language"));
            String q=uri.getQueryParameter("q");if(q==null || q.trim().isEmpty() || q.length()>160 || q.indexOf('\n')>=0 || q.indexOf('\r')>=0)throw new IOException("Invalid city search");
            exact(uri,"format","jsonv2");exact(uri,"addressdetails","1");exact(uri,"limit","20");
            String language=uri.getQueryParameter("accept-language");
            if(language==null || !language.matches("[A-Za-z,-]{1,48}"))throw new IOException("Invalid language");
        } else if("nominatim.openstreetmap.org".equals(u.getHost()) && "/reverse".equals(u.getPath())) {
            allowed=new HashSet<>(Arrays.asList("lat","lon","format","addressdetails","zoom","accept-language"));
            coordinate(uri,"lat",90);coordinate(uri,"lon",180);
            exact(uri,"format","jsonv2");exact(uri,"addressdetails","1");exact(uri,"zoom","10");
            String language=uri.getQueryParameter("accept-language");
            if(language==null || !language.matches("[A-Za-z,-]{1,48}"))throw new IOException("Invalid language");
        } else if(("timeapi.io".equals(u.getHost()) && "/api/timezone/coordinate".equals(u.getPath()))
                || ("api.open-meteo.com".equals(u.getHost()) && "/v1/forecast".equals(u.getPath()))) {
            allowed=new HashSet<>(Arrays.asList("latitude","longitude"));
            coordinate(uri,"latitude",90);coordinate(uri,"longitude",180);
            if("api.open-meteo.com".equals(u.getHost())) {
                allowed.addAll(Arrays.asList("current","timezone","forecast_days"));
                exact(uri,"current","temperature_2m");exact(uri,"timezone","auto");exact(uri,"forecast_days","1");
            }
        }else throw new IOException("Unapproved location destination");
        if(!allowed.containsAll(keys))throw new IOException("Unexpected location data");
        for(String key:keys)if(uri.getQueryParameters(key).size()!=1)throw new IOException("Duplicate query parameter");
    }
    private static void exact(Uri u,String key,String value)throws IOException{if(!value.equals(u.getQueryParameter(key)))throw new IOException("Invalid location option");}
    private static void coordinate(Uri u,String key,double bound)throws IOException{
        try{double n=Double.parseDouble(u.getQueryParameter(key));if(!Double.isFinite(n) || Math.abs(n)>bound)throw new Exception();}
        catch(Exception e){throw new IOException("Invalid coordinates");}
    }
    static HttpURLConnection update(URL url)throws IOException{
        if(!updateUrl(url))throw new IOException("Unapproved update destination");return connect(url);
    }
    static HttpURLConnection sound(URL url)throws IOException{
        if(!DownloadableSounds.allowed(url))throw new IOException("Unapproved sound destination");return connect(url);
    }
    static HttpURLConnection location(String address)throws IOException{URL url=new URL(address);locationUrl(url);return connect(url);}
    private static HttpURLConnection connect(URL url)throws IOException{
        HttpsURLConnection delegate=(HttpsURLConnection)url.openConnection();
        delegate.setInstanceFollowRedirects(false);delegate.setDoOutput(false);delegate.setRequestMethod("GET");
        delegate.setUseCaches(false);
        return new DownloadConnection(delegate);
    }
    /** Prevents accidental future uploads even when a caller asks for an output stream. */
    static final class DownloadConnection extends HttpURLConnection {
        private final HttpURLConnection delegate;
        DownloadConnection(HttpURLConnection delegate){super(delegate.getURL());this.delegate=delegate;}
        @Override public void setDoOutput(boolean output){if(output)throw new IllegalStateException("Uploads disabled");delegate.setDoOutput(false);}
        @Override public boolean getDoOutput(){return false;}
        @Override public OutputStream getOutputStream()throws IOException{throw new ProtocolException("Uploads disabled");}
        @Override public void setRequestMethod(String method)throws ProtocolException{
            if(!"GET".equals(method) && !"HEAD".equals(method))throw new ProtocolException("Uploads disabled");delegate.setRequestMethod(method);
        }
        @Override public String getRequestMethod(){return delegate.getRequestMethod();}
        @Override public void setInstanceFollowRedirects(boolean follow){if(follow)throw new IllegalArgumentException("Unvalidated redirects disabled");delegate.setInstanceFollowRedirects(false);}
        @Override public boolean getInstanceFollowRedirects(){return false;}
        @Override public void setRequestProperty(String key,String value){
            boolean safe=("Accept".equalsIgnoreCase(key)&&"application/json".equals(value))
                    || ("Accept-Encoding".equalsIgnoreCase(key)&&"identity".equals(value))
                    || ("User-Agent".equalsIgnoreCase(key)&&("InfiniTime-Update".equals(value)||"AdvanceClock/1.0 (Android; com.ilia.advanceclock)".equals(value)));
            if(!safe)throw new IllegalArgumentException("Private headers disabled");
            delegate.setRequestProperty(key,value);
        }
        @Override public void addRequestProperty(String key,String value){setRequestProperty(key,value);}
        @Override public void setConnectTimeout(int timeout){delegate.setConnectTimeout(timeout);}
        @Override public int getConnectTimeout(){return delegate.getConnectTimeout();}
        @Override public void setReadTimeout(int timeout){delegate.setReadTimeout(timeout);}
        @Override public int getReadTimeout(){return delegate.getReadTimeout();}
        @Override public void setUseCaches(boolean use){delegate.setUseCaches(use);}
        @Override public void connect()throws IOException{delegate.connect();}
        @Override public void disconnect(){delegate.disconnect();}
        @Override public boolean usingProxy(){return delegate.usingProxy();}
        @Override public int getResponseCode()throws IOException{return delegate.getResponseCode();}
        @Override public InputStream getInputStream()throws IOException{return delegate.getInputStream();}
        @Override public InputStream getErrorStream(){return delegate.getErrorStream();}
        @Override public String getHeaderField(String name){return delegate.getHeaderField(name);}
        @Override public long getContentLengthLong(){return delegate.getContentLengthLong();}
        @Override public int getContentLength(){return delegate.getContentLength();}
    }
}
