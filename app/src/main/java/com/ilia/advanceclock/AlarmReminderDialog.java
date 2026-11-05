package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Typeface;

import android.view.*;
import android.widget.*;
import java.util.*;

public final class AlarmReminderDialog {
    public interface Callback {void onConfigured(int mode,String customMinutesJson);}
    public static void show(Context c,int mode,String json,Callback callback){show(c,mode,json,false,callback);}
    public static void show(Context c,int mode,String json,boolean legacyBefore,Callback callback){show(c,mode,json,legacyBefore,System.currentTimeMillis()+86400000L,AppTime.zone(),callback);}
    public static void show(Context c,int mode,String json,boolean legacyBefore,long eventMillis,TimeZone zone,Callback callback){if(eventMillis<=0){LogoToast.makeText(c,R.string.event_set_due,Toast.LENGTH_SHORT).show();return;}new Editor(c,mode,json,legacyBefore,eventMillis,zone,callback).show();}
    private static final class Editor {
        final long eventMillis;final TimeZone zone;
        final Context c;final Callback callback;
        final LinkedHashMap<String,EventReminderRules.Rule> selected=new LinkedHashMap<>();
        final List<SettingsSection> directions=new ArrayList<>();
        final List<EventReminderRules.Rule> manual=new ArrayList<>();
        final Map<String,List<CheckBox>> bindings=new HashMap<>();
        boolean updating;

        final TextView count;
        final LinearLayout root;
        Editor(Context c,int mode,String json,boolean legacyBefore,long eventMillis,TimeZone zone,Callback callback){
            this.c=c;this.callback=callback;this.eventMillis=eventMillis/60000L*60000L;this.zone=zone;
            for(EventReminderRules.Rule rule:EventReminderRules.parse(mode,json,legacyBefore)){
                selected.put(rule.key(),rule);if(rule.custom)manual.add(rule);
            }
            root=column();root.setPadding(dp(14),dp(8),dp(14),dp(8));root.setLayoutDirection(AppSettings.layoutDirection(c));
            count=new TextView(c);count.setTextSize(15);count.setTextColor(AppSettings.primaryColor(c));count.setTypeface(null,Typeface.BOLD);
            count.setPadding(dp(8),dp(8),dp(8),dp(8));count.setTag("event_reminder_count");root.addView(count);updateCount();
            direction(true);direction(false);
        }
        void show(){
            ScrollView scroll=ReminderModalStyle.scroll(c,root);
            AlertDialog dialog=new MotionDialogs.Builder(c).setTitle(AppString.get(R.string.runtime_text_0021)).setView(scroll)
                    .setNegativeButton(AppString.get(R.string.runtime_text_0003),null)
                    .setPositiveButton(AppString.get(R.string.runtime_text_0004),(d,w)->callback.onConfigured(
                            selected.isEmpty()?AlarmReminderUtils.MODE_NONE:AlarmReminderUtils.MODE_CUSTOM,EventReminderRules.encode(selected.values())))
                    .create();dialog.show();ReminderModalStyle.apply(dialog,scroll);
        }
        void direction(boolean before){
            String key=before?"before":"after";
            SettingsSection section=accordion(root,directions,key,before?R.string.event_before:R.string.event_after);
            List<SettingsSection> groups=new ArrayList<>();
            SettingsSection hours=accordion(section.content,groups,key+"_hours",R.string.event_hours);
            for(int minutes:new int[]{5,10,15,20,30,40,45})option(hours.content,new EventReminderRules.Rule(before,"minute",minutes,false));
            for(int hour:new int[]{1,2,3,6,12,18,24})option(hours.content,new EventReminderRules.Rule(before,"hour",hour,false));
            SettingsSection days=accordion(section.content,groups,key+"_days",R.string.event_days);
            for(int day:new int[]{1,2,3,5,7,10,14,21,30})option(days.content,new EventReminderRules.Rule(before,"day",day,false));
            SettingsSection longer=accordion(section.content,groups,key+"_longer",R.string.event_weeks_months);
            for(int week:new int[]{1,2,3,4})option(longer.content,new EventReminderRules.Rule(before,"week",week,false));
            for(int month:new int[]{1,2,3,6,12})option(longer.content,new EventReminderRules.Rule(before,"month",month,false,AppSettings.defaultCalendar(c)));
            SettingsSection custom=accordion(section.content,groups,key+"_custom",R.string.event_custom);
            Calendar picked=Calendar.getInstance(zone);picked.setTimeInMillis(eventMillis+(before?-86400000L:86400000L));
            int[] calendarType={AppSettings.defaultCalendar(c)};
            LinearLayout entry=new LinearLayout(c);entry.setGravity(Gravity.CENTER_VERTICAL);entry.setBaselineAligned(false);
            Button date=fieldButton("event_"+key+"_date"),time=fieldButton("event_"+key+"_time");
            DateTimeFieldStyle.apply(c,date,time);
            Runnable render=()->{date.setText(CalendarUtils.formatDate(picked.getTimeInMillis(),calendarType[0],zone));time.setText(CalendarUtils.fa(TimeFormat.formatter(c,false,zone).format(picked.getTime())));};
            LinearLayout.LayoutParams dateLp=new LinearLayout.LayoutParams(0,dp(56),1);dateLp.setMarginEnd(dp(6));entry.addView(date,dateLp);
            entry.addView(time,new LinearLayout.LayoutParams(0,dp(56),1));custom.content.addView(entry);render.run();
            date.setOnClickListener(v->CalendarPickerDialog.showDateAny(c,WorldAlarmReference.pickerDate(picked.getTimeInMillis(),zone),calendarType[0],(millis,type)->{
                Calendar day=Calendar.getInstance(AppTime.zone());day.setTimeInMillis(millis);
                picked.set(day.get(Calendar.YEAR),day.get(Calendar.MONTH),day.get(Calendar.DAY_OF_MONTH));calendarType[0]=type;render.run();
            }));
            time.setOnClickListener(v->new MotionDialogs.TimePicker(c,(view,hour,minute)->{
                picked.set(Calendar.HOUR_OF_DAY,hour);picked.set(Calendar.MINUTE,minute);picked.set(Calendar.SECOND,0);picked.set(Calendar.MILLISECOND,0);render.run();
            },picked.get(Calendar.HOUR_OF_DAY),picked.get(Calendar.MINUTE),AppSettings.clock24Hour(c)).show());
            Button add=new Button(c);add.setText(R.string.event_add);add.setAllCaps(false);add.setTextColor(AppSettings.primaryColor(c));add.setBackground(ReminderModalStyle.shape(c,AppSettings.field(c),AppSettings.primaryColor(c),12));
            add.setTag("event_"+key+"_add");custom.content.addView(add,new LinearLayout.LayoutParams(-1,dp(48)));
            LinearLayout entries=column();custom.content.addView(entries);
            for(EventReminderRules.Rule rule:new ArrayList<>(manual))if(rule.before==before)manualRow(entries,rule);
            // Keep previously selected, retired presets visible and removable without changing them.
            for(EventReminderRules.Rule rule:new ArrayList<>(selected.values()))if(rule.before==before && !bindings.containsKey(rule.key()))manualRow(entries,rule);
            add.setOnClickListener(v->{
                long delta=(picked.getTimeInMillis()/60000L*60000L-eventMillis)/60000L;
                if((before && delta>=0) || (!before && delta<=0)){
                    LogoToast.makeText(c,before?R.string.event_pick_before:R.string.event_pick_after,Toast.LENGTH_SHORT).show();return;
                }
                if(picked.getTimeInMillis()<=System.currentTimeMillis()){LogoToast.makeText(c,R.string.event_pick_future,Toast.LENGTH_SHORT).show();return;}
                EventReminderRules.Rule rule=new EventReminderRules.Rule(before,"minute",(int)Math.abs(delta),true);
                if(!EventReminderRules.valid(rule)){LogoToast.makeText(c,R.string.event_invalid_amount,Toast.LENGTH_SHORT).show();return;}
                if(selected.containsKey(rule.key()))return;
                if(manual.size()>=EventReminderRules.MAX_CUSTOM){LogoToast.makeText(c,R.string.event_custom_limit,Toast.LENGTH_SHORT).show();return;}
                manual.add(rule);selected.put(rule.key(),rule);manualRow(entries,rule);syncChecks(rule.key(),true);updateCount();
            });
        }
        Button fieldButton(String tag){
            Button button=new Button(c);button.setTag(tag);button.setAllCaps(false);button.setTextSize(14);button.setTextColor(AppSettings.textPrimary(c));
            button.setBackground(ReminderModalStyle.shape(c,AppSettings.field(c),AppSettings.primaryColor(c),12));return button;
        }
        SettingsSection accordion(LinearLayout parent,List<SettingsSection> siblings,String key,int title){
            SettingsSection[] ref=new SettingsSection[1];
            SettingsSection section=new SettingsSection(c,key,title,R.drawable.ic_settings_calendar,()->{
                boolean open=!ref[0].isExpanded();for(SettingsSection s:siblings)s.setExpanded(s==ref[0] && open);
            });ref[0]=section;siblings.add(section);section.setExpanded(false);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(8);parent.addView(section,lp);return section;
        }
        void option(LinearLayout parent,EventReminderRules.Rule rule){
            CheckBox check=new CheckBox(c);check.setText(EventReminderRules.label(c,rule));check.setTextSize(14);check.setTextColor(AppSettings.textPrimary(c));
            check.setButtonTintList(android.content.res.ColorStateList.valueOf(AppSettings.primaryColor(c)));
            check.setMinHeight(dp(44));check.setTag("event_rule_"+rule.key());check.setChecked(selected.containsKey(rule.key()));
            bindings.computeIfAbsent(rule.key(),key->new ArrayList<>()).add(check);
            check.setOnCheckedChangeListener((button,on)->{
                if(updating)return;
                EventReminderRules.Rule owner=rule;
                for(EventReminderRules.Rule custom:manual)if(custom.key().equals(rule.key()))owner=custom;
                if(on)selected.put(rule.key(),owner);else selected.remove(rule.key());
                syncChecks(rule.key(),on);updateCount();
            });
            parent.addView(check,new LinearLayout.LayoutParams(-1,-2));
        }
        void manualRow(LinearLayout parent,EventReminderRules.Rule rule){
            LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER_VERTICAL);
            option(row,rule);
            ((CheckBox)row.getChildAt(0)).setText(CalendarUtils.formatDate(rule.at(eventMillis,zone),AppSettings.defaultCalendar(c),zone)+"  •  "+CalendarUtils.fa(TimeFormat.formatter(c,false,zone).format(new Date(rule.at(eventMillis,zone)))));
            row.getChildAt(0).setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            ImageButton remove=new ImageButton(c);remove.setImageResource(R.drawable.ic_md_delete);remove.setColorFilter(0xFFC62828);
            remove.setContentDescription(c.getString(R.string.tool_delete));remove.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            row.addView(remove,new LinearLayout.LayoutParams(dp(44),dp(44)));
            remove.setOnClickListener(v->{manual.remove(rule);selected.remove(rule.key());syncChecks(rule.key(),false);parent.removeView(row);updateCount();});parent.addView(row);
        }
        void syncChecks(String key,boolean checked){
            updating=true;for(CheckBox box:bindings.getOrDefault(key,Collections.emptyList()))box.setChecked(checked);updating=false;
        }
        void updateCount(){count.setText(c.getString(R.string.reminder_count_summary,CalendarUtils.fa(selected.size())));}
        LinearLayout column(){LinearLayout view=new LinearLayout(c);view.setOrientation(LinearLayout.VERTICAL);return view;}
        int dp(int n){return ReminderModalStyle.dp(c,n);}
    }
}
