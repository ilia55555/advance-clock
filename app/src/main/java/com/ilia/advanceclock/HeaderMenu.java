package com.ilia.advanceclock;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.*;
import android.widget.*;

final class HeaderMenu {
    static PopupWindow show(Activity host, View anchor, Menu menu, PopupMenu.OnMenuItemClickListener action) {
        int primary=AppSettings.primaryColor(host);
        LinearLayout list=new LinearLayout(host);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setLayoutDirection(AppSettings.layoutDirection(host));
        list.setPadding(dp(host,8),dp(host,8),dp(host,8),dp(host,8));
        ScrollView scroll=new ScrollView(host);
        scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.addView(list);
        int width=Math.min(dp(host,280),host.getResources().getDisplayMetrics().widthPixels-dp(host,32));
        PopupWindow popup=new PopupWindow(scroll,width,-2,true);
        GradientDrawable panel=shape(host,AppSettings.surface(host),22);
        panel.setStroke(dp(host,1),(primary & 0x00FFFFFF)|0x26000000);
        popup.setBackgroundDrawable(panel);
        popup.setElevation(dp(host,12));popup.setOutsideTouchable(true);
        for(int i=0;i<menu.size();i++) {
            MenuItem item=menu.getItem(i);if(!item.isVisible())continue;
            LinearLayout row=new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
            row.setMinimumHeight(dp(host,56));row.setPadding(dp(host,10),dp(host,8),dp(host,10),dp(host,8));
            row.setFocusable(true);row.setEnabled(item.isEnabled());
            row.setBackground(new RippleDrawable(ColorStateList.valueOf((primary & 0x00FFFFFF)|0x22000000),
                    shape(host,Color.TRANSPARENT,14),shape(host,Color.WHITE,14)));
            ImageView icon=new ImageView(host);
            icon.setImageResource(icon(item.getItemId()));icon.setColorFilter(primary);
            icon.setPadding(dp(host,8),dp(host,8),dp(host,8),dp(host,8));
            icon.setBackground(shape(host,(primary & 0x00FFFFFF)|0x18000000,12));
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(dp(host,36),dp(host,36));
            ip.setMarginEnd(dp(host,12));row.addView(icon,ip);
            TextView title=new TextView(host);title.setText(item.getTitle());title.setTextSize(15);
            title.setTextColor(AppSettings.textPrimary(host));title.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            row.addView(title,new LinearLayout.LayoutParams(0,-2,1));
            row.setOnClickListener(v->{popup.dismiss();action.onMenuItemClick(item);});
            list.addView(row,new LinearLayout.LayoutParams(-1,-2));
        }
        popup.showAsDropDown(anchor,0,dp(host,6),Gravity.END);
        list.setAlpha(0);list.setTranslationY(-dp(host,8));
        list.animate().alpha(1).translationY(0).setDuration(160).start();
        return popup;
    }
    private static GradientDrawable shape(Activity host,int color,int radius) {
        GradientDrawable shape=new GradientDrawable();shape.setColor(color);shape.setCornerRadius(dp(host,radius));return shape;
    }
    private static int icon(int id) {
        switch(id) {
            case 1:return R.drawable.ic_md_settings;
            case 7:return R.drawable.ic_alarm;
            case 4:return android.R.drawable.ic_popup_reminder;
            case 5:return R.drawable.ic_settings_widgets;
            case 6:return R.drawable.ic_settings_tabs;
            case 8:return R.drawable.ic_settings_tools;
            default:return R.drawable.ic_settings_info;
        }
    }
    private static int dp(Activity host,int value) {return Math.round(value*host.getResources().getDisplayMetrics().density);}
}
