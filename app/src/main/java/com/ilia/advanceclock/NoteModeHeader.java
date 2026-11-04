package com.ilia.advanceclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;

/** Fixed labels: Tasks on the left, Note on the right in every locale. */
final class NoteModeHeader {
    static Switch add(Context c,LinearLayout parent,int index,boolean checked,boolean onHeader){
        LinearLayout row=new LinearLayout(c);row.setGravity(Gravity.CENTER);row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        row.setPadding(8,8,8,8);row.setTag("note_mode_header");
        TextView tasks=new TextView(c);tasks.setText(R.string.note_mode_tasks);
        Switch toggle=new Switch(c);toggle.setTag("note_mode_switch");toggle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        toggle.setContentDescription(c.getString(R.string.note_mode_selector));toggle.setChecked(checked);
        TextView note=new TextView(c);note.setText(R.string.note_mode_note);
        int color=onHeader?android.graphics.Color.WHITE:AppSettings.textPrimary(c);
        for(TextView label:new TextView[]{tasks,note}){label.setTextSize(18);label.setTextColor(color);label.setPadding(12,0,12,0);}
        toggle.setThumbTintList(ColorStateList.valueOf(onHeader?android.graphics.Color.WHITE:AppSettings.primaryColor(c)));
        row.addView(tasks);row.addView(toggle,new LinearLayout.LayoutParams(-2,ReminderModalStyle.dp(c,48)));row.addView(note);
        tasks.setOnClickListener(v->toggle.setChecked(true));note.setOnClickListener(v->toggle.setChecked(false));
        parent.addView(row,index,new LinearLayout.LayoutParams(-1,-2));return toggle;
    }
}
