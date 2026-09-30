package com.ilia.advanceclock;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class NoteAttachment {
    static final String KIND_FILE = "file";
    static final String KIND_APP = "app";
    static final String KIND_URL = "url";

    final String kind;
    final String value;
    final String name;
    final String mime;

    NoteAttachment(String kind, String value, String name, String mime) {
        this.kind=kind; this.value=value; this.name=name; this.mime=mime;
    }

    static List<NoteAttachment> parse(String raw) {
        ArrayList<NoteAttachment> values=new ArrayList<>();
        try {
            JSONArray array=new JSONArray(raw==null?"[]":raw);
            for(int i=0;i<array.length();i++){
                JSONObject o=array.optJSONObject(i);
                if(o!=null) values.add(new NoteAttachment(
                        o.optString("kind",KIND_FILE),
                        o.optString("value",""),
                        o.optString("name",AppString.get(R.string.runtime_text_0059)),
                        o.optString("mime","*/*")));
            }
        } catch(Exception ignored){}
        return values;
    }

    static String encode(List<NoteAttachment> values) {
        JSONArray array=new JSONArray();
        for(NoteAttachment value:values){
            JSONObject o=new JSONObject();
            try{
                o.put("kind",value.kind);
                o.put("value",value.value);
                o.put("name",value.name);
                o.put("mime",value.mime);
                array.put(o);
            }catch(Exception ignored){}
        }
        return array.toString();
    }

    static NoteAttachment app(Context context,String packageName) {
        try {
            ApplicationInfo info=context.getPackageManager().getApplicationInfo(packageName,0);
            return new NoteAttachment(KIND_APP,packageName,
                    context.getPackageManager().getApplicationLabel(info).toString(),"");
        } catch(Exception ignored){
            return new NoteAttachment(KIND_APP,packageName,packageName,"");
        }
    }

    static NoteAttachment url(String raw) {
        String value=raw==null?"":raw.trim();
        if(!value.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*$")) value="https://"+value;
        Uri uri=Uri.parse(value);
        String host=uri.getHost();
        String label=(host==null||host.trim().isEmpty())?value:host;
        return new NoteAttachment(KIND_URL,value,label,"text/html");
    }

    boolean isImage() {
        return KIND_FILE.equals(kind) && mime != null && mime.startsWith("image/");
    }

    void open(Context context) {
        try {
            Intent intent;
            if(KIND_APP.equals(kind)) {
                intent=context.getPackageManager().getLaunchIntentForPackage(value);
            } else if(KIND_URL.equals(kind)) {
                intent=new Intent(Intent.ACTION_VIEW,Uri.parse(value));
            } else {
                intent=new Intent(Intent.ACTION_VIEW)
                        .setDataAndType(Uri.parse(value),mime==null||mime.isEmpty()?"*/*":mime)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            if(intent!=null){
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        } catch(Exception ignored){}
    }
}
