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
        scroll.setBackgroundColor(Color.TRANSPARENT);scroll.setClipToPadding(true);scroll.addView(content);return scroll;
    }
    static void apply(AlertDialog dialog,ScrollView content){
        Context c=dialog.getContext();int primary=AppSettings.primaryColor(c);
        GradientDrawable background=shape(c,AppSettings.surface(c),primary,20);
        dialog.getWindow().setBackgroundDrawable(background);
        // Native alert panels can paint over the window border; keep their fills transparent.
        View parent=dialog.findViewById(c.getResources().getIdentifier("parentPanel","id","android"));
        if(parent instanceof ViewGroup){parent.setBackgroundColor(Color.TRANSPARENT);parent.setPadding(dp(c,2),dp(c,2),dp(c,2),dp(c,2));((ViewGroup)parent).setClipChildren(true);}
        content.setPadding(dp(c,2),0,dp(c,2),dp(c,4));
        dialog.getWindow().setLayout(Math.min(dp(c,560),c.getResources().getDisplayMetrics().widthPixels-dp(c,28)),-2);
        for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE}){
            Button b=dialog.getButton(which);if(b==null)continue;boolean filled=which==AlertDialog.BUTTON_POSITIVE;
            b.setAllCaps(false);b.setTextSize(14);b.setMinHeight(dp(c,48));b.setMinimumHeight(dp(c,48));b.setPadding(dp(c,18),dp(c,8),dp(c,18),dp(c,8));
            b.setElevation(0);b.setStateListAnimator(null);
            b.setTextColor(filled?(Color.luminance(primary)>.179f?Color.BLACK:Color.WHITE):primary);
            b.setBackground(shape(c,filled?primary:Color.TRANSPARENT,primary,12));
            if(b.getLayoutParams() instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)b.getLayoutParams();lp.height=dp(c,48);lp.setMargins(dp(c,6),dp(c,4),dp(c,6),dp(c,4));b.setLayoutParams(lp);}
        }
        // The framework button bar has a fixed theme height on some phones. Its height must
        // include the custom button backgrounds, margins and accessibility/font padding.
        View panel=dialog.findViewById(c.getResources().getIdentifier("buttonPanel","id","android"));
        if(panel instanceof ViewGroup){
            panel.setBackgroundColor(Color.TRANSPARENT);panel.setMinimumHeight(dp(c,76));panel.setPadding(dp(c,12),dp(c,10),dp(c,12),dp(c,10));
            ViewGroup.LayoutParams lp=panel.getLayoutParams();lp.height=ViewGroup.LayoutParams.WRAP_CONTENT;panel.setLayoutParams(lp);
        }
    }
    static GradientDrawable shape(Context c,int fill,int border,int radius){GradientDrawable s=new GradientDrawable();s.setColor(fill);s.setCornerRadius(dp(c,radius));s.setStroke(dp(c,1),border);return s;}
    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
}
