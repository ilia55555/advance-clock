package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class AlarmReminderDialog {
    public interface Callback {void onConfigured(int mode,String customMinutesJson);}
    public static void show(Context c,int mode,String json,Callback callback){show(c,mode,json,false,callback);}
    public static void show(Context c,int mode,String json,boolean legacyBefore,Callback callback){new Editor(c,mode,json,legacyBefore,callback).show();}
    private static final class Editor {
        final Context c;final Callback callback;
        final LinkedHashMap<String,EventReminderRules.Rule> selected=new LinkedHashMap<>();
        final List<SettingsSection> directions=new ArrayList<>();
        final List<EventReminderRules.Rule> manual=new ArrayList<>();
        final Map<String,List<CheckBox>> bindings=new HashMap<>();
        boolean updating;

        final TextView count;
        final LinearLayout root;
        Editor(Context c,int mode,String json,boolean legacyBefore,Callback callback){
            this.c=c;this.callback=callback;
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
            AlertDialog dialog=new AlertDialog.Builder(c).setTitle(AppString.get(R.string.runtime_text_0021)).setView(scroll)
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
            for(int minutes:new int[]{5,10,15,30,45})option(hours.content,new EventReminderRules.Rule(before,"minute",minutes,false));
            for(int hour=1;hour<=24;hour++)option(hours.content,new EventReminderRules.Rule(before,"hour",hour,false));
            SettingsSection days=accordion(section.content,groups,key+"_days",R.string.event_days);
            for(int day:new int[]{1,2,3,5,7,10,14,21,30})option(days.content,new EventReminderRules.Rule(before,"day",day,false));
            SettingsSection longer=accordion(section.content,groups,key+"_longer",R.string.event_weeks_months);
            for(int week:new int[]{1,2,3,4})option(longer.content,new EventReminderRules.Rule(before,"week",week,false));
            for(int month:new int[]{1,2,3,6,12})option(longer.content,new EventReminderRules.Rule(before,"month",month,false));
            SettingsSection custom=accordion(section.content,groups,key+"_custom",R.string.event_custom);
            LinearLayout entry=new LinearLayout(c);entry.setGravity(Gravity.CENTER_VERTICAL);entry.setBaselineAligned(false);
            EditText amount=new EditText(c);amount.setHint(R.string.event_amount);amount.setSingleLine(true);
            amount.setInputType(InputType.TYPE_CLASS_NUMBER);amount.setTextSize(15);amount.setTextColor(AppSettings.textPrimary(c));
            amount.setTag("event_"+key+"_amount");entry.addView(amount,new LinearLayout.LayoutParams(0,dp(52),1));
            Spinner unit=new Spinner(c);String[] names={c.getString(R.string.event_unit_minutes_name),c.getString(R.string.event_unit_hours_name),c.getString(R.string.event_unit_days_name),c.getString(R.string.event_unit_weeks_name),c.getString(R.string.event_unit_months_name)};
            ArrayAdapter<String> adapter=new ArrayAdapter<>(c,android.R.layout.simple_spinner_item,names);adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);unit.setAdapter(adapter);
            unit.setTag("event_"+key+"_unit");entry.addView(unit,new LinearLayout.LayoutParams(0,dp(52),1));custom.content.addView(entry);
            Button add=new Button(c);add.setText(R.string.event_add);add.setAllCaps(false);add.setTextColor(AppSettings.primaryColor(c));add.setBackground(ReminderModalStyle.shape(c,AppSettings.field(c),AppSettings.primaryColor(c),12));
            add.setTag("event_"+key+"_add");custom.content.addView(add,new LinearLayout.LayoutParams(-1,dp(48)));
            LinearLayout entries=column();custom.content.addView(entries);
            for(EventReminderRules.Rule rule:new ArrayList<>(manual))if(rule.before==before)manualRow(entries,rule);
            add.setOnClickListener(v->{
                int n;try{n=Integer.parseInt(amount.getText().toString().trim());}catch(Exception e){n=0;}
                EventReminderRules.Rule rule=new EventReminderRules.Rule(before,EventReminderRules.UNITS[unit.getSelectedItemPosition()],n,true);
                if(!EventReminderRules.valid(rule)){amount.setError(c.getString(R.string.event_invalid_amount));return;}
                if(selected.containsKey(rule.key()))return;
                if(manual.size()>=EventReminderRules.MAX_CUSTOM){LogoToast.makeText(c,R.string.event_custom_limit,Toast.LENGTH_SHORT).show();return;}
                manual.add(rule);selected.put(rule.key(),rule);manualRow(entries,rule);syncChecks(rule.key(),true);amount.setText("");updateCount();
            });
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
