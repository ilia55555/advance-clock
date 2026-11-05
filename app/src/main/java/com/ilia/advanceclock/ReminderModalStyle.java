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
        if(!(content.getParent() instanceof ViewGroup))return;
        ViewGroup holder=(ViewGroup)content.getParent();
        if("reminder_dialog_body".equals(holder.getTag()))return;
        Button positive=dialog.getButton(AlertDialog.BUTTON_POSITIVE),negative=dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        LinearLayout footer=new LinearLayout(c);footer.setTag("reminder_dialog_actions");
        footer.setOrientation(LinearLayout.HORIZONTAL);footer.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        footer.setGravity(Gravity.CENTER_VERTICAL);footer.setBaselineAligned(false);
        footer.setPadding(dp(c,14),dp(c,10),dp(c,14),dp(c,14));
        for(Button button:new Button[]{negative,positive}){
            if(button==null)continue;boolean filled=button==positive;
            if(button.getParent() instanceof ViewGroup)((ViewGroup)button.getParent()).removeView(button);
            button.setAllCaps(false);button.setTextSize(14);button.setMinHeight(dp(c,48));button.setMinimumHeight(dp(c,48));
            button.setMinWidth(0);button.setMinimumWidth(0);button.setSingleLine(false);button.setMaxLines(2);
            button.setGravity(Gravity.CENTER);button.setLayoutDirection(AppSettings.layoutDirection(c));
            button.setPadding(dp(c,12),dp(c,8),dp(c,12),dp(c,8));button.setElevation(0);button.setStateListAnimator(null);
            button.setTextColor(filled?(Color.luminance(primary)>.179f?Color.BLACK:Color.WHITE):primary);
            button.setBackgroundTintList(null);button.setBackground(shape(c,filled?primary:Color.TRANSPARENT,primary,12));
            if(footer.getChildCount()>0)footer.addView(new Space(c),new LinearLayout.LayoutParams(dp(c,8),1));
            footer.addView(button,new LinearLayout.LayoutParams(0,ViewGroup.LayoutParams.WRAP_CONTENT,1));
        }
        // Use our own footer: OEM alert button bars may enforce a fixed inner height and clip
        // custom backgrounds even after the outer button panel is enlarged.
        View panel=dialog.findViewById(c.getResources().getIdentifier("buttonPanel","id","android"));
        if(panel!=null)panel.setVisibility(View.GONE);
        LinearLayout body=new LinearLayout(c);body.setTag("reminder_dialog_body");body.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams original=content.getLayoutParams();int index=holder.indexOfChild(content);holder.removeView(content);
        body.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        body.addView(footer,new LinearLayout.LayoutParams(-1,-2));
        holder.addView(body,index,original);
    }

    static GradientDrawable shape(Context c,int fill,int border,int radius){GradientDrawable s=new GradientDrawable();s.setColor(fill);s.setCornerRadius(dp(c,radius));s.setStroke(dp(c,1),border);return s;}
    static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
}
