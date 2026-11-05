package com.ilia.advanceclock;

import android.content.Context;
import android.net.Uri;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

/** Explicit foreground downloads only. Catalog entries cannot supply network addresses or disk paths. */
final class DownloadableSounds {
    static final String BASE="https://raw.githubusercontent.com/infinitimeilia/infinitime/main/alarm/";
    static final int MAX_FILE=20*1024*1024,MAX_CATALOG=256*1024;
    static final class Entry {
        final String id,name,file,sha256;final int version;final long size;
        Entry(JSONObject o)throws Exception{
            id=o.getString("id");name=o.getString("name");file=o.getString("file");sha256=o.getString("sha256");version=o.getInt("version");size=o.getLong("size");
            if(!id.matches("[a-z0-9][a-z0-9_-]{0,63}") || name.trim().isEmpty() || name.length()>100 || name.matches("(?s).*[\\p{Cntrl}].*") || !validFile(file)
                    || !sha256.matches("[0-9a-f]{64}") || size<1 || size>MAX_FILE || version<1)throw new IOException("Invalid sound entry");
        }
    }
    static boolean validFile(String file){return file!=null && !file.contains("..") && file.matches("[A-Za-z0-9][A-Za-z0-9 _.'()!-]{0,100}\\.mp3");}
    static boolean allowed(URL url){
        if(url==null || !"https".equals(url.getProtocol()) || !"raw.githubusercontent.com".equals(url.getHost()) || url.getUserInfo()!=null || url.getQuery()!=null || url.getRef()!=null || (url.getPort()!=-1 && url.getPort()!=443))return false;
        String path=Uri.decode(url.getPath()),prefix="/infinitimeilia/infinitime/main/alarm/";
        if(!path.startsWith(prefix))return false;String file=path.substring(prefix.length());return "alarms.json".equals(file) || validFile(file);
    }
    static List<Entry> parse(String raw)throws Exception{
        if(raw.length()>MAX_CATALOG)throw new IOException("Catalog too large");
        JSONObject root=new JSONObject(raw);if(root.getInt("schemaVersion")!=1)throw new IOException("Unsupported catalog");
        JSONArray array=root.getJSONArray("sounds");if(array.length()>500)throw new IOException("Too many sounds");
        List<Entry> result=new ArrayList<>();Set<String> ids=new HashSet<>(),files=new HashSet<>();
        for(int i=0;i<array.length();i++){Entry entry=new Entry(array.getJSONObject(i));if(!ids.add(entry.id) || !files.add(entry.file.toLowerCase(Locale.ROOT)))throw new IOException("Duplicate sound");result.add(entry);}return result;
    }
    interface Progress {void changed(long done,long total);}
    interface ConnectionFactory {HttpURLConnection open(URL url)throws Exception;}
    static final class Request {
        volatile boolean cancelled;volatile HttpURLConnection connection;
        void check()throws IOException{if(cancelled || Thread.currentThread().isInterrupted())throw new IOException("Cancelled");}
        void cancel(){cancelled=true;HttpURLConnection c=connection;if(c!=null)new Thread(c::disconnect,"cancel-sound-download").start();}
    }
    private static File directory(Context c){return new File(c.getFilesDir(),"downloaded_sounds");}
    private static File target(Context c,Entry e){return new File(directory(c),e.id+".mp3");}
    static boolean available(Context c,Entry e){return target(c,e).isFile();}
    static String uri(Context c,Entry e){return Uri.fromFile(target(c,e)).toString();}
    static boolean owns(Context c,String uri){return uri!=null && uri.startsWith(Uri.fromFile(directory(c)).toString()+"/");}
    static List<Entry> installed(Context c){try{return parse(c.getSharedPreferences("downloaded_sounds",0).getString("installed","{\"schemaVersion\":1,\"sounds\":[]}"));}catch(Exception ignored){return Collections.emptyList();}}
    static Entry installed(Context c,Entry entry){for(Entry e:installed(c))if(e.id.equals(entry.id) && target(c,e).isFile())return e;return null;}
    static boolean current(Context c,Entry entry){return current(c,entry,installed(c,entry));}
    static boolean current(Context c,Entry entry,Entry old){return old!=null && old.sha256.equals(entry.sha256) && target(c,entry).length()==entry.size;}
    static List<Entry> cached(Context c){try{return parse(c.getSharedPreferences("downloaded_sounds",0).getString("catalog",""));}catch(Exception ignored){return Collections.emptyList();}}
    private static HttpURLConnection open(URL url)throws Exception{
        HttpURLConnection c=ReadOnlyNetwork.sound(url);c.setConnectTimeout(15000);c.setReadTimeout(20000);c.setRequestProperty("Accept-Encoding","identity");return c;
    }
    private static HttpURLConnection response(URL url,Request request,ConnectionFactory factory)throws Exception{
        request.check();if(!allowed(url))throw new IOException("Untrusted source");HttpURLConnection c=factory.open(url);request.connection=c;
        try{request.check();if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());return c;}catch(Exception e){c.disconnect();request.connection=null;throw e;}
    }
    static List<Entry> refresh(Context c,Request request)throws Exception{
        HttpURLConnection connection=response(new URL(BASE+"alarms.json"),request,DownloadableSounds::open);
        try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){request.check();if(out.size()+n>MAX_CATALOG)throw new IOException("Catalog too large");out.write(buffer,0,n);}
            String raw=out.toString("UTF-8");List<Entry> entries=parse(raw);request.check();c.getSharedPreferences("downloaded_sounds",0).edit().putString("catalog",raw).commit();return entries;
        }finally{connection.disconnect();request.connection=null;}
    }
    static synchronized void download(Context c,Entry entry,Request request,Progress progress)throws Exception{download(c,entry,request,progress,DownloadableSounds::open);}
    static synchronized void download(Context c,Entry entry,Request request,Progress progress,ConnectionFactory factory)throws Exception{
        Entry previous=installed(c,entry);if(previous!=null && previous.version>entry.version)throw new SecurityException("Sound downgrade blocked");
        if(!directory(c).isDirectory() && !directory(c).mkdirs())throw new IOException("Storage unavailable");
        File temp=File.createTempFile("sound-",".part",directory(c));HttpURLConnection connection=null;
        try{
            connection=response(new URL(BASE+Uri.encode(entry.file)),request,factory);long header=connection.getContentLengthLong();if(header>=0 && header!=entry.size)throw new SecurityException("Size mismatch");
            MessageDigest hash=MessageDigest.getInstance("SHA-256");long count=0;
            try(InputStream in=connection.getInputStream();FileOutputStream out=new FileOutputStream(temp)){
                byte[] buffer=new byte[16384];int n;while((n=in.read(buffer))!=-1){request.check();count+=n;if(count>entry.size)throw new SecurityException("Size mismatch");hash.update(buffer,0,n);out.write(buffer,0,n);progress.changed(count,entry.size);}out.getFD().sync();
            }
            StringBuilder digest=new StringBuilder();for(byte b:hash.digest())digest.append(String.format(Locale.ROOT,"%02x",b&255));
            if(count!=entry.size || !entry.sha256.equals(digest.toString()))throw new SecurityException("Integrity check failed");request.check();
            Files.move(temp.toPath(),target(c,entry).toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            List<Entry> entries=new ArrayList<>(installed(c));entries.removeIf(e->e.id.equals(entry.id));entries.add(entry);
            c.getSharedPreferences("downloaded_sounds",0).edit().putString("installed",encode(entries)).commit();
        }finally{if(connection!=null)connection.disconnect();request.connection=null;temp.delete();}
    }
    static String encode(List<Entry> entries)throws Exception{
        JSONArray array=new JSONArray();for(Entry e:entries)array.put(new JSONObject().put("id",e.id).put("name",e.name).put("file",e.file).put("version",e.version).put("size",e.size).put("sha256",e.sha256));
        return new JSONObject().put("schemaVersion",1).put("sounds",array).toString();
    }
}
