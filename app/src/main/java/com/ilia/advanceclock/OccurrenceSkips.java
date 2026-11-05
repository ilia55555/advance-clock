package com.ilia.advanceclock;

import android.content.*;
import android.widget.*;
import java.util.*;
import org.json.*;

/** Individual exclusions and whole-day quiet windows, independent of edit form defaults. */
final class OccurrenceSkips {
    static String raw(Context c,String kind,long id){return c.getSharedPreferences("occurrence_skips",0).getString(kind+"/"+id,"[]");}
    static Set<Long> values(Context c,String kind,long id){Set<Long> result=new HashSet<>();try{JSONArray a=new JSONArray(raw(c,kind,id));for(int i=0;i<a.length();i++)result.add(a.getLong(i));}catch(JSONException ignored){}return result;}
    private static String dayRaw(Context c,String kind,long id){return c.getSharedPreferences("occurrence_skips",0).getString(kind+"/"+id+"/days","[]");}
    static String signature(Context c,String kind,long id){return raw(c,kind,id)+dayRaw(c,kind,id);}
    private static List<long[]> windows(Context c,String kind,long id){
        List<long[]> result=new ArrayList<>();try{JSONArray a=new JSONArray(dayRaw(c,kind,id));for(int i=0;i<a.length();i++){
            JSONArray w=a.getJSONArray(i);long start=w.getLong(0),end=w.getLong(1);if(end>start)result.add(new long[]{start,end});
        }}catch(JSONException ignored){}return result;
    }
    private static long blockedUntil(List<long[]> windows,long at){long end=0;for(long[] w:windows)if(at>=w[0] && at<w[1])end=Math.max(end,w[1]);return end;}
    static boolean dayContains(Context c,String kind,long id,long at){return blockedUntil(windows(c,kind,id),at)>0;}
    static boolean contains(Context c,String kind,long id,long at){return values(c,kind,id).contains(at) || dayContains(c,kind,id,at);}
    static boolean reminderBlocked(Context c,String kind,long id,long occurrence,long when){return contains(c,kind,id,occurrence) || dayContains(c,kind,id,when);}
    static long[] dayWindow(long now,TimeZone zone){Calendar day=Calendar.getInstance(zone);day.setTimeInMillis(now);day.set(Calendar.HOUR_OF_DAY,0);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);long start=day.getTimeInMillis();day.add(Calendar.DAY_OF_YEAR,1);return new long[]{start,day.getTimeInMillis()};}
    private static boolean rememberDay(Context c,String kind,long id,long now,TimeZone zone){
        long[] day=dayWindow(now,zone);List<long[]> windows=windows(c,kind,id);if(blockedUntil(windows,now)>0)return false;
        windows.add(day);JSONArray a=new JSONArray();for(long[] w:windows)a.put(new JSONArray().put(w[0]).put(w[1]));
        return c.getSharedPreferences("occurrence_skips",0).edit().putString(kind+"/"+id+"/days",a.toString()).commit();
    }
    static void clear(Context c,String kind,long id){c.getSharedPreferences("occurrence_skips",0).edit().remove(kind+"/"+id).remove(kind+"/"+id+"/days").apply();}
    static void remember(Context c,String kind,long id,long at){Set<Long> set=values(c,kind,id);set.add(at);ArrayList<Long> sorted=new ArrayList<>(set);Collections.sort(sorted);JSONArray a=new JSONArray();for(long value:sorted)a.put(value);c.getSharedPreferences("occurrence_skips",0).edit().putString(kind+"/"+id,a.toString()).commit();}
    static long next(Context c,String kind,long id,long anchor,int recurrence,int interval,String dates,long now,TimeZone zone){
        Set<Long> skipped=values(c,kind,id);List<long[]> windows=windows(c,kind,id);
        long at=RecurrenceUtils.next(anchor,recurrence,interval,dates,now,zone);
        while(at>0){long end=blockedUntil(windows,at);if(end==0 && !skipped.contains(at))return at;
            long next=RecurrenceUtils.next(anchor,recurrence,interval,dates,Math.max(at,end-1),zone);
            if(next<=at)return -1L;at=next;
        }return -1L;
    }
    static long nextReminder(Context c,String kind,long id,EventReminderRules.Rule rule,long anchor,int recurrence,int interval,String dates,TimeZone zone,long now){
        Set<Long> skipped=values(c,kind,id);List<long[]> windows=windows(c,kind,id);
        long probe=now,at=rule.next(anchor,recurrence,interval,dates,zone,probe);
        while(at>0){
            long when=rule.at(at,zone),eventEnd=blockedUntil(windows,at),reminderEnd=blockedUntil(windows,when);
            if(eventEnd==0 && reminderEnd==0 && !skipped.contains(at))return at;
            long after=when;
            if(eventEnd>0)after=Math.max(after,rule.at(eventEnd-1,zone));
            if(reminderEnd>0)after=Math.max(after,reminderEnd-1);
            if(after<=probe)return -1L;probe=after;
            at=rule.next(anchor,recurrence,interval,dates,zone,probe);
        }return -1L;
    }
    static boolean today(long at,long now,TimeZone zone){Calendar a=Calendar.getInstance(zone),n=Calendar.getInstance(zone);a.setTimeInMillis(at);n.setTimeInMillis(now);return at>now && a.get(Calendar.ERA)==n.get(Calendar.ERA) && a.get(Calendar.YEAR)==n.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR)==n.get(Calendar.DAY_OF_YEAR);}
    static boolean skip(Context c,String kind,long id,long now){
        if(kind.equals("alarm"))synchronized(AlarmStore.transactionLock()){
            AlarmStore store=new AlarmStore(c);AlarmItem item=store.find(id);if(item==null || !item.enabled || item.recurrenceMode==RecurrenceUtils.NONE)return false;
            if(!rememberDay(c,kind,id,now,item.timeZone()))return false;
            AlarmScheduler.cancelSkippedDayReminders(c,item);AlertSnoozes.cancelBlocked(c,kind,id);
            long next=next(c,kind,id,item.triggerAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now,item.timeZone());
            if(next>0){item.triggerAtMillis=next;store.save(item);AlarmScheduler.schedule(c,item);}else{item.enabled=false;store.save(item);AlarmScheduler.cancelPrimary(c,id);EventReminderScheduler.sync(c,item);}
            ClockWidgetProvider.updateAll(c);return true;
        }
        synchronized(NoteTasks.class){
            NoForgetStore store=new NoForgetStore(c);NoForgetItem item=store.find(id);if(item==null || !item.reminderEnabled || !item.hasDue || item.recurrenceMode==RecurrenceUtils.NONE)return false;
            if(!rememberDay(c,kind,id,now,AppTime.zone()))return false;
            NoForgetScheduler.cancelSkippedDaySnooze(c,id);AlertSnoozes.cancelBlocked(c,kind,id);
            long next=next(c,kind,id,item.dueAtMillis,item.recurrenceMode,item.intervalDays,item.customDatesJson,now,AppTime.zone());
            if(next>0){item.dueAtMillis=next;store.save(item);NoForgetScheduler.scheduleKeepingSnoozes(c,item);}else{item.reminderEnabled=false;store.save(item);NoForgetScheduler.cancelPrimaryOccurrence(c,id);EventReminderScheduler.sync(c,item);}
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
        button.setEnabled(enabled && recurrence!=RecurrenceUtils.NONE && !dayContains(host,kind,id,System.currentTimeMillis()));button.setAlpha(button.isEnabled()?1:.4f);
        button.setOnClickListener(v->{button.setEnabled(false);Context app=host.getApplicationContext();NoteTasks.WORK.execute(()->{
            try{skip(app,kind,id,System.currentTimeMillis());}catch(RuntimeException error){android.util.Log.e("OccurrenceSkips","Cannot skip occurrence",error);}
            host.runOnUiThread(()->{if(!host.isFinishing()&&!host.isDestroyed())refresh.run();});
        });});return button;
    }
}
