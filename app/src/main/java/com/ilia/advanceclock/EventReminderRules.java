package com.ilia.advanceclock;

import org.json.*;
import java.util.*;

/** Calendar-relative rules. Legacy minute arrays retain their original scheduler semantics. */
final class EventReminderRules {
    static final int MAX_CUSTOM=10,MAX_RULES=160;
    static final String[] UNITS={"minute","hour","day","week","month"};
    static final class Rule {
        final boolean before,custom;
        final String unit;
        final int amount,calendarType;
        Rule(boolean before,String unit,int amount,boolean custom){this(before,unit,amount,custom,CalendarUtils.GREGORIAN);}
        Rule(boolean before,String unit,int amount,boolean custom,int calendarType){this.before=before;this.unit=unit;this.amount=amount;this.custom=custom;this.calendarType=calendarType;}
        String key(){return (before?"before":"after")+"_"+unit+"_"+amount+(unit.equals("month") && calendarType!=CalendarUtils.GREGORIAN?"_calendar_"+calendarType:"");}
        long at(long occurrence,TimeZone zone){
            int signed=before?-amount:amount;
            if(unit.equals("minute"))return occurrence+signed*60000L;
            if(unit.equals("hour"))return occurrence+signed*3600000L;
            if(unit.equals("month") && calendarType!=CalendarUtils.GREGORIAN){
                android.icu.util.Calendar date=CalendarUtils.create(calendarType);
                date.setTimeZone(android.icu.util.TimeZone.getTimeZone(zone.getID()));date.setTimeInMillis(occurrence);
                date.add(android.icu.util.Calendar.MONTH,signed);return date.getTimeInMillis();
            }
            Calendar date=Calendar.getInstance(zone);date.setTimeInMillis(occurrence);
            date.add(unit.equals("month")?Calendar.MONTH:unit.equals("week")?Calendar.WEEK_OF_YEAR:Calendar.DAY_OF_YEAR,signed);
            return date.getTimeInMillis();
        }
        long next(long anchor,int recurrence,int interval,String dates,TimeZone zone,long now){
            long span=(long)amount*(unit.equals("month")?31L*86400000:unit.equals("week")?7L*86400000:
                    unit.equals("day")?86400000:unit.equals("hour")?3600000:60000)+3L*86400000;
            long low=now-span,high=now+span;
            // Invert the calendar offset, including month-end clamping and DST.
            while(low<high){long middle=low+(high-low)/2;if(at(middle,zone)>now)high=middle;else low=middle+1;}
            long margin=(!unit.equals("minute") && !unit.equals("hour") && zone.useDaylightTime())?3*3600000L:0;
            long probe=low-margin-1,best=-1,bestTime=Long.MAX_VALUE;
            // Calendar-day/month offsets can fold around a DST gap. Check neighboring occurrences
            // so an arbitrary minute interval cannot miss the earliest upcoming reminder.
            for(int i=0;i<2048;i++) {
                long occurrence=RecurrenceUtils.next(anchor,recurrence,interval,dates,probe,zone);
                if(occurrence<=0)break;
                long when=at(occurrence,zone);
                if(when>now && when<bestTime){best=occurrence;bestTime=when;}
                if(occurrence>=low+margin)break;
                probe=occurrence;
            }
            return best;
        }
    }
    static boolean rich(String json){return json!=null && json.trim().startsWith("{");}
    static boolean valid(Rule rule){
        int max=rule.unit.equals("month")?120:rule.unit.equals("week")?520:rule.unit.equals("day")?3650:
                rule.unit.equals("hour")?87600:rule.unit.equals("minute")?5256000:0;
        return rule.amount>0 && rule.amount<=max && rule.calendarType>=0 && rule.calendarType<=2;
    }
    static List<Rule> parse(int mode,String json,boolean legacyBefore){
        ArrayList<Rule> result=new ArrayList<>();
        if(mode==AlarmReminderUtils.MODE_NONE)return result;
        if(!rich(json)){
            for(int minutes:AlarmReminderUtils.effective(mode,json)){
                String unit=minutes%1440==0?"day":minutes%60==0?"hour":"minute";
                result.add(new Rule(legacyBefore,unit,unit.equals("day")?minutes/1440:unit.equals("hour")?minutes/60:minutes,false));
            }
            return result;
        }
        try{
            JSONObject root=new JSONObject(json);if(root.optInt("v")!=1)return result;
            JSONArray array=root.getJSONArray("rules");Set<String> seen=new HashSet<>();int custom=0;
            for(int i=0;i<Math.min(array.length(),MAX_RULES);i++){
                JSONObject o=array.optJSONObject(i);if(o==null)continue;
                Rule rule=new Rule(o.optBoolean("before"),o.optString("unit"),o.optInt("amount"),o.optBoolean("custom"),o.optInt("calendar",CalendarUtils.GREGORIAN));
                if(valid(rule) && !seen.contains(rule.key()) && (!rule.custom || custom<MAX_CUSTOM)){
                    result.add(rule);seen.add(rule.key());if(rule.custom)custom++;
                }
            }
        }catch(Exception ignored){}
        return result;
    }
    static String encode(Collection<Rule> rules){
        JSONArray array=new JSONArray();Set<String> seen=new HashSet<>();int custom=0;
        for(Rule rule:rules)try{
            if(!valid(rule) || !seen.add(rule.key()) || (rule.custom && custom++>=MAX_CUSTOM))continue;
            array.put(new JSONObject().put("before",rule.before).put("unit",rule.unit).put("amount",rule.amount).put("custom",rule.custom).put("calendar",rule.calendarType));
            if(array.length()>=MAX_RULES)break;
        }catch(JSONException ignored){}
        try{return new JSONObject().put("v",1).put("rules",array).toString();}catch(JSONException impossible){return "{}";}
    }
    static String label(android.content.Context context,Rule rule){
        if(rule.amount==1)return context.getString(rule.unit.equals("month")?R.string.event_one_month:
                rule.unit.equals("week")?R.string.event_one_week:rule.unit.equals("day")?R.string.event_one_day:
                rule.unit.equals("hour")?R.string.event_one_hour:R.string.event_one_minute);
        int id=rule.unit.equals("month")?R.string.event_unit_month:rule.unit.equals("week")?R.string.event_unit_week:
                rule.unit.equals("day")?R.string.event_unit_day:rule.unit.equals("hour")?R.string.event_unit_hour:R.string.event_unit_minute;
        return context.getString(id,CalendarUtils.fa(rule.amount));
    }
}
