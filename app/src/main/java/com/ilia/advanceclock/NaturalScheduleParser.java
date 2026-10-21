package com.ilia.advanceclock;

import android.content.Context;
import java.util.*;
import java.util.regex.*;

/** Offline, explicit scheduling grammar shared by smart alarms and smart notes. */
final class NaturalScheduleParser {
    private static final String DAYS = "یکشنبه|دوشنبه|سه‌شنبه|چهارشنبه|پنجشنبه|جمعه|شنبه";
    private static final Pattern WEEKDAY = Pattern.compile(DAYS);
    private static final Pattern INTERVAL = Pattern.compile("(?<![\\p{L}])هر\\s*(\\d+)?\\s*(روز|هفته|ماه|سال|دقیقه|ساعت)");
    private static final Pattern RELATIVE = Pattern.compile("(\\d+)\\s*(روز|هفته|ماه|سال)\\s*(بعد|دیگر|دیگه|آینده|قبل)");
    private static final Pattern DELAY = Pattern.compile("(\\d+)\\s*(دقیقه|ساعت)\\s*(بعد|دیگر|دیگه)");
    private static final Pattern TIME = Pattern.compile("(?<!\\d)(?:ساعت\\s*)?(\\d{1,2})[:٫.]([0-5]?\\d)(?!\\d)\\s*(صبح|بامداد|ظهر|عصر|بعدازظهر|بعد از ظهر|شب|am|pm)?|ساعت\\s*(\\d{1,2})(?![\\d:٫.])\\s*(صبح|بامداد|ظهر|عصر|بعدازظهر|بعد از ظهر|شب|am|pm)?", Pattern.CASE_INSENSITIVE);
    private static final Map<String,Integer> NUMBERS = new LinkedHashMap<>();
    static {
        String[] ones = {"صفر","یک","دو","سه","چهار","پنج","شش","هفت","هشت","نه","ده","یازده","دوازده","سیزده","چهارده","پانزده","شانزده","هفده","هجده","نوزده"};
        for (int i=0;i<ones.length;i++) NUMBERS.put(ones[i],i);
        String[] tens = {"بیست","سی","چهل","پنجاه","شصت","هفتاد","هشتاد","نود"};
        for(int i=0;i<tens.length;i++) NUMBERS.put(tens[i],20+i*10);
        String[] hundreds = {"صد","دویست","سیصد","چهارصد","پانصد","ششصد","هفتصد","هشتصد","نهصد"};
        for(int i=0;i<hundreds.length;i++) NUMBERS.put(hundreds[i],100+i*100);
    }
    static String normalizeSpeech(String text) {
        String s=text.replaceAll("یک[ ‌]+شنبه","یکشنبه").replaceAll("دو[ ‌]+شنبه","دوشنبه")
                .replaceAll("سه[ ‌]+شنبه","سه‌شنبه").replaceAll("چهار[ ‌]+شنبه","چهارشنبه")
                .replaceAll("پنج[ ‌]+شنبه","پنجشنبه").replace("پسفردا","پس فردا");
        ArrayList<String> words=new ArrayList<>(NUMBERS.keySet());
        words.sort((a,b)->Integer.compare(b.length(),a.length()));
        String atom=String.join("|",words);
        Matcher m=Pattern.compile("(?<![\\p{L}\\p{N}])((?:"+atom+")(?:\\s+و\\s+(?:"+atom+"))*)(?![\\p{L}\\p{N}])").matcher(s);
        StringBuffer out=new StringBuffer();
        while(m.find()) {
            int value=0;for(String word:m.group().split("\\s+و\\s+")) value+=NUMBERS.get(word);
            m.appendReplacement(out,Integer.toString(value));
        }
        m.appendTail(out);s=out.toString();
        s=s.replaceAll("ساعت\\s*(\\d{1,2})\\s*و\\s*نیم","ساعت $1:30")
                .replaceAll("ساعت\\s*(\\d{1,2})\\s*و\\s*ربع","ساعت $1:15")
                .replaceAll("ساعت\\s*(\\d{1,2})\\s*و\\s*(\\d{1,2})\\s*(?:دقیقه)?","ساعت $1:$2")
                .replaceAll("نیم\\s*ساعت","30 دقیقه");
        Matcher quarter=Pattern.compile("(?:1|یک)\\s*ربع\\s*به\\s*(\\d{1,2})").matcher(s);
        out=new StringBuffer();while(quarter.find()) {
            int hour=Integer.parseInt(quarter.group(1));
            quarter.appendReplacement(out,"ساعت "+((hour+23)%24)+":45");
        }quarter.appendTail(out);
        return out.toString().replaceAll("(?<![\\d:])(\\d{1,2})\\s*(صبح|بامداد|عصر|شب|ظهر)", "ساعت $1 $2")
                .replaceAll("ساعت\\s*ساعت", "ساعت").replaceAll("(?<=:)(\\d)(?!\\d)","0$1")
                .replaceAll("بعد\\s+از\\s+(\\d+)\\s*(روز|هفته|ماه|سال)","$1 $2 بعد");
    }
    private static int day(String name) {
        switch(name){case "یکشنبه":return Calendar.SUNDAY;case "دوشنبه":return Calendar.MONDAY;
            case "سه‌شنبه":return Calendar.TUESDAY;case "چهارشنبه":return Calendar.WEDNESDAY;
            case "پنجشنبه":return Calendar.THURSDAY;case "جمعه":return Calendar.FRIDAY;default:return Calendar.SATURDAY;}
    }
    private static List<Integer> days(String text) {
        List<Integer> days=new ArrayList<>();Matcher m=WEEKDAY.matcher(text);
        while(m.find()){int d=day(m.group());if(!days.contains(d))days.add(d);}
        Matcher range=Pattern.compile("("+DAYS+")\\s*(?:تا|الی)\\s*("+DAYS+")").matcher(text);
        while(range.find()) {
            int start=day(range.group(1)), end=day(range.group(2));
            for(int d=start, i=0;i<7;i++,d=d%7+1){if(!days.contains(d))days.add(d);if(d==end)break;}
        }
        return days;
    }
    static void recurrence(SmartAlarmParser.Candidate c,String text) {
        List<Integer> selected=days(text);
        Matcher interval=INTERVAL.matcher(text);
        boolean hasInterval=interval.find();
        int count=hasInterval&&interval.group(1)!=null?Integer.parseInt(interval.group(1)):1;
        if(count<1||count>36500) throw new IllegalArgumentException("Invalid repeat interval");
        boolean weekly=!selected.isEmpty()&&(selected.size()>1||Pattern.compile("(?<![\\p{L}])هر").matcher(text).find()||text.contains("روزهای")||text.contains("روز‌های")||Pattern.compile("(?:"+DAYS+")[ ‌-]*ها").matcher(text).find());
        if(weekly){
            c.recurrenceMode=RecurrenceUtils.WEEKDAYS;
            c.intervalDays=hasInterval&&interval.group(2).equals("هفته")?count:1;
            c.customDatesJson=RecurrenceUtils.weekdaysToJson(selected,c.intervalDays==1);
        } else if(hasInterval){
            String unit=interval.group(2);
            c.intervalDays=count;
            if(unit.equals("دقیقه")||unit.equals("ساعت")){c.recurrenceMode=RecurrenceUtils.INTERVAL_MINUTES;c.intervalDays=unit.equals("ساعت")?Math.multiplyExact(count,60):count;}
            else if(unit.equals("روز"))c.recurrenceMode=count==1?RecurrenceUtils.DAILY:RecurrenceUtils.INTERVAL_DAYS;
            else if(unit.equals("هفته")){c.recurrenceMode=RecurrenceUtils.INTERVAL_DAYS;c.intervalDays=Math.multiplyExact(count,7);}
            else {c.recurrenceMode=unit.equals("ماه")?RecurrenceUtils.MONTHLY:RecurrenceUtils.YEARLY;c.customDatesJson="{\"period\":"+count+"}";}
        } else if(text.matches("(?s).*روز[ ‌]*در[ ‌]*میان.*")){c.recurrenceMode=RecurrenceUtils.INTERVAL_DAYS;c.intervalDays=2;}
        else if(text.contains("روزانه")||text.contains("هرروز"))c.recurrenceMode=RecurrenceUtils.DAILY;
        else if(text.contains("هفتگی"))c.recurrenceMode=RecurrenceUtils.WEEKLY;
        else if(text.contains("ماهانه"))c.recurrenceMode=RecurrenceUtils.MONTHLY;
        else if(text.contains("سالانه"))c.recurrenceMode=RecurrenceUtils.YEARLY;
    }
    static boolean parse(Context context,String text,SmartAlarmParser.Result result) {
        // Explicit numeric/named calendar dates retain the existing validated calendar parser.
        if(text.matches("(?s).*\\d{2,4}\\s*[/.-]\\s*\\d{1,2}\\s*[/.-]\\s*\\d{1,2}.*")
                ||Pattern.compile("\\d+\\s*(?:فروردین|اردیبهشت|خرداد|تیر|مرداد|شهریور|مهر|آبان|آذر|دی|بهمن|اسفند)(?![\\p{L}])").matcher(text).find())return false;
        Matcher timeMatcher=TIME.matcher(text);List<int[]> times=new ArrayList<>();
        while(timeMatcher.find()) {
            int hour=Integer.parseInt(timeMatcher.group(1)!=null?timeMatcher.group(1):timeMatcher.group(4));
            int minute=timeMatcher.group(2)!=null?Integer.parseInt(timeMatcher.group(2)):0;
            String part=timeMatcher.group(1)!=null?timeMatcher.group(3):timeMatcher.group(5);
            if(part!=null){if((part.equals("عصر")||part.contains("بعد")||part.equals("شب")||part.equalsIgnoreCase("pm"))&&hour<12)hour+=12;
                if((part.equals("صبح")||part.equals("بامداد")||part.equalsIgnoreCase("am"))&&hour==12)hour=0;
                if(part.equals("ظهر")&&hour<12)hour+=12;
                if(part.equals("شب")&&hour==12)hour=0;}
            if(hour<0||hour>23){result.warnings.add(context.getString(R.string.smart_invalid_schedule));return true;}
            times.add(new int[]{hour,minute});
        }
        Matcher anchors=Pattern.compile("پس[ ‌]*فردا|فردا|امروز|امشب|\\d+\\s*(?:روز|هفته|ماه|سال)\\s*(?:بعد|دیگر|دیگه|آینده)").matcher(text);
        List<Integer> anchorStarts=new ArrayList<>();while(anchors.find())anchorStarts.add(anchors.start());
        if(anchorStarts.size()>1){
            StringBuilder sharedTimes=new StringBuilder();for(int[] t:times)sharedTimes.append(String.format(Locale.US," %02d:%02d",t[0],t[1]));
            for(int i=0;i<anchorStarts.size();i++){
                String part=text.substring(anchorStarts.get(i),i+1<anchorStarts.size()?anchorStarts.get(i+1):text.length());
                if(!TIME.matcher(part).find())part+=sharedTimes;
                parse(context,part,result);
            }
            return true;
        }
        Calendar base=Calendar.getInstance();long now=System.currentTimeMillis();
        boolean explicit=false, relativeClock=false;Matcher relative=RELATIVE.matcher(text);Matcher delay=DELAY.matcher(text);
        if(text.contains("پس فردا")||text.contains("پس‌فردا")){base.add(Calendar.DAY_OF_YEAR,2);explicit=true;}
        else if(text.contains("فردا")){base.add(Calendar.DAY_OF_YEAR,1);explicit=true;}
        else if(text.contains("امروز")||text.contains("امشب"))explicit=true;
        else if(relative.find()){
            int value;try{value=Integer.parseInt(relative.group(1));}catch(Exception e){value=-1;}
            if(value<0||value>36500){result.warnings.add(context.getString(R.string.smart_invalid_schedule));return true;}
            if(relative.group(3).equals("قبل"))value=-value;
            base.add(relative.group(2).equals("روز")?Calendar.DAY_OF_YEAR:relative.group(2).equals("هفته")?Calendar.WEEK_OF_YEAR:relative.group(2).equals("ماه")?Calendar.MONTH:Calendar.YEAR,value);explicit=true;
        } else if(delay.find()){
            int count;try{count=Integer.parseInt(delay.group(1));}catch(Exception e){count=-1;}
            if(count<=0||count>525600){result.warnings.add(context.getString(R.string.smart_invalid_schedule));return true;}
            base.add(delay.group(2).equals("ساعت")?Calendar.HOUR_OF_DAY:Calendar.MINUTE,count);
            if(times.isEmpty()){times.add(new int[]{base.get(Calendar.HOUR_OF_DAY),base.get(Calendar.MINUTE)});relativeClock=true;}explicit=true;
        }
        if(times.isEmpty()) {
            SmartAlarmParser.Candidate rule=new SmartAlarmParser.Candidate();
            try { recurrence(rule,text); } catch(RuntimeException invalid){result.warnings.add(context.getString(R.string.smart_invalid_schedule));return true;}
            if(rule.recurrenceMode==RecurrenceUtils.INTERVAL_MINUTES){
                base.add(Calendar.MINUTE,rule.intervalDays);
                times.add(new int[]{base.get(Calendar.HOUR_OF_DAY),base.get(Calendar.MINUTE)});
                explicit=true;relativeClock=true;
            } else { result.warnings.add(context.getString(R.string.smart_missing_time));return true; }
        }
        // Separate clauses such as "شنبه ساعت 8 و یکشنبه ساعت 9" keep their own times.
        List<Integer> selected=days(text);
        Matcher weekday=WEEKDAY.matcher(text);List<Integer> starts=new ArrayList<>();
        while(weekday.find())starts.add(weekday.start());
        if(starts.size()>1){boolean separate=true;
            for(int i=0;i<starts.size();i++)if(!TIME.matcher(text.substring(starts.get(i),i+1<starts.size()?starts.get(i+1):text.length())).find())separate=false;
            if(separate){for(int i=0;i<starts.size();i++)parse(context,"هر "+text.substring(starts.get(i),i+1<starts.size()?starts.get(i+1):text.length()),result);return true;}}
        for(int i=0;i<times.size();i++){
            int[] time=times.get(i);Calendar date=(Calendar)base.clone();
            date.set(Calendar.HOUR_OF_DAY,time[0]);date.set(Calendar.MINUTE,time[1]);date.set(Calendar.SECOND,0);date.set(Calendar.MILLISECOND,0);
            SmartAlarmParser.Candidate c=new SmartAlarmParser.Candidate();c.hour=time[0];c.minute=time[1];c.source=text;c.label=label(text);
            try{recurrence(c,text);}catch(RuntimeException invalid){result.warnings.add(context.getString(R.string.smart_invalid_schedule));return true;}
            if(!selected.isEmpty()){
                if(c.recurrenceMode==RecurrenceUtils.WEEKDAYS){long next=RecurrenceUtils.next(date.getTimeInMillis(),c.recurrenceMode,c.intervalDays,c.customDatesJson,now);if(next<0)continue;date.setTimeInMillis(next);}
                else {int offset=(selected.get(0)-date.get(Calendar.DAY_OF_WEEK)+7)%7;date.add(Calendar.DAY_OF_YEAR,offset);if(date.getTimeInMillis()<=now)date.add(Calendar.WEEK_OF_YEAR,1);}
            } else if(!explicit){
                if(c.recurrenceMode==RecurrenceUtils.INTERVAL_DAYS)date.add(Calendar.DAY_OF_YEAR,c.intervalDays);
                else if(c.recurrenceMode==RecurrenceUtils.INTERVAL_MINUTES)date.setTimeInMillis(RecurrenceUtils.next(date.getTimeInMillis(),c.recurrenceMode,c.intervalDays,c.customDatesJson,now));
                else if(date.getTimeInMillis()<=now)date.add(Calendar.DAY_OF_YEAR,1);
            }
            c.calendarType=AppSettings.defaultCalendar(context);
            android.icu.util.Calendar local=CalendarUtils.fromMillis(c.calendarType,date.getTimeInMillis());
            c.year=local.get(android.icu.util.Calendar.YEAR);c.month=local.get(android.icu.util.Calendar.MONTH)+1;c.day=local.get(android.icu.util.Calendar.DAY_OF_MONTH);
            if(!relativeClock) SmartAlarmParser.applyNaturalDetails(c,text);
            if(times.size()==2&&text.matches("(?s).*\\d.*(?:تا|الی|until|to).*\\d.*")) {c.endHour=times.get(1)[0];c.endMinute=times.get(1)[1];result.candidates.add(c);break;}
            result.candidates.add(c);
        }
        return true;
    }
    private static String label(String text) {
        String value=TIME.matcher(text).replaceAll(" ");value=INTERVAL.matcher(value).replaceAll(" ");value=RELATIVE.matcher(value).replaceAll(" ");value=DELAY.matcher(value).replaceAll(" ");value=WEEKDAY.matcher(value).replaceAll(" ");
        value=value.replaceAll("(?<![\\p{L}])(?:پس[ ‌]*فردا|فردا|امروز|امشب|روزانه|هفتگی|ماهانه|سالانه|روزهای|روز‌های|هر|یکبار|1\\s*بار|میخوام|می‌خوام|می خواهم|می‌خواهم|داشته باشم|زنگ بزنه|هشدار|یادآوری|یادداشت)(?![\\p{L}])", " ")
                .replaceAll("\\s+"," ").trim();
        String previous;
        do {
            previous=value;
            value=value.replaceAll("^(?:در|روز|و|برای|که)(?:\\s+|$)|(?:^|\\s+)(?:و|در|برای|که)$", "").trim();
        } while(!value.equals(previous));
        return value.isEmpty()?AppString.get(R.string.runtime_text_0263):value;
    }
}
