package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** Shared theme, borders and actions for recurrence and reminder editors. */
final class ReminderModalStyle {
    static ScrollView scroll(Context c,View content){
        ScrollView scroll=new ScrollView(c){
            @Override protected void onMeasure(int width,int height){
                int cap=(int)(c.getResources().getDisplayMetrics().heightPixels*.62f);
                int available=View.MeasureSpec.getMode(height)==View.MeasureSpec.UNSPECIFIED?cap:View.MeasureSpec.getSize(height);
                super.onMeasure(width,View.MeasureSpec.makeMeasureSpec(Math.min(cap,available),View.MeasureSpec.AT_MOST));
            }
        };scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
        scroll.setBackgroundColor(AppSettings.surface(c));scroll.addView(content);return scroll;
    }
    static void apply(AlertDialog dialog,ScrollView content){
        Context c=dialog.getContext();int primary=AppSettings.primaryColor(c);
        GradientDrawable background=shape(c,AppSettings.surface(c),primary,20);
        dialog.getWindow().setBackgroundDrawable(background);
        dialog.getWindow().setLayout(Math.min(dp(c,560),c.getResources().getDisplayMetrics().widthPixels-dp(c,28)),-2);
        for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE}){
            Button b=dialog.getButton(which);if(b==null)continue;boolean filled=which==AlertDialog.BUTTON_POSITIVE;
            b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(c,48));b.setPadding(dp(c,18),dp(c,8),dp(c,18),dp(c,8));
            b.setElevation(0);b.setStateListAnimator(null);
            b.setTextColor(filled?(Color.luminance(primary)>.179f?Color.BLACK:Color.WHITE):primary);
            b.setBackground(shape(c,filled?primary:Color.TRANSPARENT,primary,12));
            if(b.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)b.getLayoutParams();lp.setMargins(dp(c,6),dp(c,8),dp(c,6),dp(c,8));b.setLayoutParams(lp);}
        }
    }
    static GradientDrawable shape(Context c,int fill,int border,int radius){GradientDrawable s=new GradientDrawable();s.setColor(fill);s.setCornerRadius(dp(c,radius));s.setStroke(dp(c,1),border);return s;}
    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
}
