package com.ilia.advanceclock;

import android.content.*;
import android.widget.*;
import java.util.*;
import org.json.*;

/** Individual occurrence exclusions, persisted independently of edit form defaults. */
final class OccurrenceSkips {
    static String raw(Context c,String kind,long id){return c.getSharedPreferences("occurrence_skips",0).getString(kind+"/"+id,"[]");}
    static Set<Long> values(Context c,String kind,long id){Set<Long> result=new HashSet<>();try{JSONArray a=new JSONArray(raw(c,kind,id));for(int i=0;i<a.length();i++)result.add(a.getLong(i));}catch(JSONException ignored){}return result;}
    static boolean contains(Context c,String kind,long id,long at){return values(c,kind,id).contains(at);}
    static void clear(Context c,String kind,long id){c.getSharedPreferences("occurrence_skips",0).edit().remove(kind+"/"+id).apply();}
    static void remember(Context c,String kind,long id,long at){Set<Long> set=values(c,kind,id);set.add(at);ArrayList<Long> sorted=new ArrayList<>(set);Collections.sort(sorted);JSONArray a=new JSONArray();for(long value:sorted)a.put(value);c.getSharedPreferences("occurrence_skips",0).edit().putString(kind+"/"+id,a.toString()).commit();}
    static long next(Context c,String kind,long id,long anchor,int recurrence,int interval,String dates,long now,TimeZone zone){
        Set<Long> skipped=values(c,kind,id);long at=RecurrenceUtils.next(anchor,recurrence,interval,dates,now,zone);
        for(int i=0;at>0 && skipped.contains(at) && i<=skipped.size();i++)at=RecurrenceUtils.next(anchor,recurrence,interval,dates,at,zone);return at;
    }
    static long nextReminder(Context c,String kind,long id,EventReminderRules.Rule rule,long anchor,int recurrence,int interval,String dates,TimeZone zone,long now){
        Set<Long> skipped=values(c,kind,id);long at=rule.next(anchor,recurrence,interval,dates,zone,now);
        for(int i=0;at>0 && skipped.contains(at) && i<=skipped.size();i++)at=rule.next(anchor,recurrence,interval,dates,zone,rule.at(at,zone));return at;
    }
    static boolean today(long at,long now,TimeZone zone){Calendar a=Calendar.getInstance(zone),n=Calendar.getInstance(zone);a.setTimeInMillis(at);n.setTimeInMillis(now);return at>now && a.get(Calendar.ERA)==n.get(Calendar.ERA) && a.get(Calendar.YEAR)==n.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR)==n.get(Calendar.DAY_OF_YEAR);}
    static boolean skip(Context c,String kind,long id,long now){
        if(kind.equals("alarm"))synchronized(AlarmStore.transactionLock()){
            AlarmStore store=new AlarmStore(c);AlarmItem item=store.find(id);if(item==null || !item.enabled || item.recurrenceMode==RecurrenceUtils.NONE)return false;
            long at=next(c,kind,id,item.triggerAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now,item.timeZone());if(!today(at,now,item.timeZone()))return false;
            remember(c,kind,id,at);long next=next(c,kind,id,item.triggerAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,at,item.timeZone());
            if(next>0){item.triggerAtMillis=next;store.save(item);AlarmScheduler.schedule(c,item);}else{item.enabled=false;store.save(item);AlarmScheduler.cancelPrimary(c,id);EventReminderScheduler.sync(c,item);}
            ClockWidgetProvider.updateAll(c);return true;
        }
        synchronized(NoteTasks.class){
            NoForgetStore store=new NoForgetStore(c);NoForgetItem item=store.find(id);if(item==null || !item.reminderEnabled || !item.hasDue || item.recurrenceMode==RecurrenceUtils.NONE)return false;
            long at=next(c,kind,id,item.dueAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now,AppTime.zone());if(!today(at,now,AppTime.zone()))return false;
            remember(c,kind,id,at);long next=next(c,kind,id,item.dueAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,at,AppTime.zone());
            if(next>0){item.dueAtMillis=next;store.save(item);NoForgetScheduler.schedule(c,item);}else{item.reminderEnabled=false;store.save(item);NoForgetScheduler.cancelPrimaryOccurrence(c,id);EventReminderScheduler.sync(c,item);}
            NoForgetWidgetProvider.updateAll(c);return true;
        }
    }
    static void addBeside(android.widget.LinearLayout row,Button skip,Button active){
        Context c=row.getContext();active.setMinWidth(ReminderModalStyle.dp(c,72));active.setMinimumWidth(ReminderModalStyle.dp(c,72));
        row.addView(new android.view.View(c),new android.widget.LinearLayout.LayoutParams(0,1,1));
        android.widget.LinearLayout.LayoutParams lp=new android.widget.LinearLayout.LayoutParams(ReminderModalStyle.dp(c,64),ReminderModalStyle.dp(c,32));lp.setMarginEnd(ReminderModalStyle.dp(c,8));
        row.addView(skip,lp);row.addView(active);
    }
    static Button button(android.app.Activity host,String kind,long id,long at,int recurrence,boolean enabled,TimeZone zone,Runnable refresh){
        Button button=new Button(host);button.setTag("skip_today_"+kind+"_"+id);button.setText(R.string.skip_today_occurrence);
        button.setAllCaps(false);button.setTextSize(10);button.setMaxLines(2);button.setIncludeFontPadding(false);button.setEllipsize(android.text.TextUtils.TruncateAt.END);
        button.setMinWidth(0);button.setMinimumWidth(0);button.setMinHeight(0);button.setMinimumHeight(0);
        button.setPadding(ReminderModalStyle.dp(host,4),0,ReminderModalStyle.dp(host,4),0);button.setTextColor(AppSettings.textPrimary(host));
        button.setElevation(0);button.setStateListAnimator(null);button.setBackgroundTintList(null);
        button.setBackground(ReminderModalStyle.shape(host,android.graphics.Color.TRANSPARENT,AppSettings.primaryColor(host),8));
        button.setContentDescription(host.getString(R.string.skip_today_occurrence));button.setTooltipText(host.getString(R.string.skip_today_occurrence));
        button.setEnabled(enabled && recurrence!=RecurrenceUtils.NONE && today(at,System.currentTimeMillis(),zone));button.setAlpha(button.isEnabled()?1:.4f);
        button.setOnClickListener(v->{button.setEnabled(false);Context app=host.getApplicationContext();NoteTasks.WORK.execute(()->{
            try{skip(app,kind,id,System.currentTimeMillis());}catch(RuntimeException error){android.util.Log.e("OccurrenceSkips","Cannot skip occurrence",error);}
            host.runOnUiThread(()->{if(!host.isFinishing()&&!host.isDestroyed())refresh.run();});
        });});return button;
    }
}
