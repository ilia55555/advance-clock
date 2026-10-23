package com.ilia.advanceclock;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Languages advertised by the installed speech engine, independently of the app locale. */
final class SmartSpeechLanguages {
    private final Activity host;
    private final Spinner spinner;
    private final ArrayList<String> tags = new ArrayList<>();
    private final ArrayList<String> labels = new ArrayList<>();
    private final ArrayAdapter<String> adapter;
    private final Runnable changed;
    private String selected;

    SmartSpeechLanguages(Activity host, Runnable changed) {
        this.host=host; this.changed=changed;
        selected=host.getSharedPreferences("smart_speech",0).getString("language",
                SmartAlarmActivity.speechIntent(host).getStringExtra(RecognizerIntent.EXTRA_LANGUAGE));
        spinner=new Spinner(host);
        spinner.setTag("smart_speech_language");
        spinner.setContentDescription(host.getString(R.string.smart_speech_language));
        spinner.setBackgroundResource(R.drawable.bg_field);
        adapter=new ArrayAdapter<String>(host,android.R.layout.simple_spinner_item,labels) {
            @Override public View getView(int position, View convertView, ViewGroup parent) {
                TextView text=(TextView)super.getView(position,convertView,parent);
                text.setText(Locale.forLanguageTag(tags.get(position)).getDisplayLanguage(AppString.locale()));
                text.setTextSize(13); text.setSingleLine(true);
                text.setTextColor(AppSettings.textPrimary(host));
                text.setPadding(4,0,4,0);
                return text;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        // Provisional choices when the engine does not publish language details.
        setLanguages(Arrays.asList(selected,"fa-IR","en-US","ar-SA"),null);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(AdapterView<?> parent) {}
            public void onItemSelected(AdapterView<?> parent,View view,int position,long id) {
                if(position<0 || position>=tags.size()) return;
                String tag=tags.get(position);
                if(tag.equals(selected)) return;
                selected=tag;
                host.getSharedPreferences("smart_speech",0).edit().putString("language",tag).apply();
                changed.run();
            }
        });
    }

    Spinner view() { return spinner; }
    String language() { return selected; }

    void requestSupportedLanguages() {
        String service=android.provider.Settings.Secure.getString(host.getContentResolver(),"voice_recognition_service");
        ComponentName component=service==null ? null : ComponentName.unflattenFromString(service);
        Intent details=component!=null
                ? new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS).setPackage(component.getPackageName())
                : RecognizerIntent.getVoiceDetailsIntent(host);
        if(details==null) {
            details=new Intent(RecognizerIntent.ACTION_GET_LANGUAGE_DETAILS);
        }
        try {
            host.sendOrderedBroadcast(details,null,new BroadcastReceiver() {
                @Override public void onReceive(Context context,Intent intent) {
                    if(host.isFinishing() || host.isDestroyed()) return;
                    acceptDetails(getResultExtras(false));
                }
            },null,Activity.RESULT_OK,null,null);
        } catch (RuntimeException unavailable) {
            // Keep provisional choices when an OEM engine does not expose details.
        }
    }

    void acceptDetails(Bundle details) {
        if(details==null) return;
        ArrayList<String> supported=details.getStringArrayList(RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES);
        if(supported==null || supported.isEmpty()) return;
        setLanguages(supported,details.getString(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE));
    }

    private void setLanguages(List<String> supported,String preferred) {
        LinkedHashSet<String> clean=new LinkedHashSet<>();
        for(String value:supported) {
            if(value==null || value.trim().isEmpty()) continue;
            String tag=Locale.forLanguageTag(value.trim().replace('_','-')).toLanguageTag();
            if(!"und".equals(tag)) clean.add(tag);
        }
        if(clean.isEmpty()) return;
        String old=selected;
        tags.clear();tags.addAll(clean);labels.clear();
        for(String tag:tags) labels.add(Locale.forLanguageTag(tag).getDisplayName(AppString.locale()));
        if(!tags.contains(selected)) {
            selected=null;
            for(String tag:tags) if(Locale.forLanguageTag(tag).getLanguage()
                    .equals(Locale.forLanguageTag(old).getLanguage())) {selected=tag;break;}
            if(selected==null) selected=preferred!=null && tags.contains(preferred) ? preferred : tags.get(0);
        }
        adapter.notifyDataSetChanged();spinner.setSelection(tags.indexOf(selected));
        if(!selected.equals(old)) {
            host.getSharedPreferences("smart_speech",0).edit().putString("language",selected).apply();
            changed.run();
        }
    }
}
