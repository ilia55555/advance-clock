package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Dismissible, non-modal warning. The menu remains available after dismissal. */
final class PermissionWarning {
    static PopupWindow show(Activity host) {
        return show(host,false,R.string.permissions_warning,()->host.startActivityForResult(
                new Intent(host,PermissionCenterActivity.class),PermissionCenterActivity.REQUEST));
    }
    static PopupWindow optional(Activity host,int message,Runnable grant) {
        return show(host,true,message,grant);
    }
    private static PopupWindow show(Activity host,boolean optional,int messageRes,Runnable grant) {
        LinearLayout card=new LinearLayout(host);card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(AppSettings.layoutDirection(host));
        int pad=dp(host,16);card.setPadding(pad,pad,pad,pad);
        GradientDrawable bg=new GradientDrawable();bg.setColor(optional ? 0xFFFFD600
                : (AppSettings.themeMode(host)==AppSettings.THEME_DARK?0xFF4A1919:0xFFFFEBEE));
        bg.setCornerRadius(dp(host,18));bg.setStroke(dp(host,1),optional?0xFFFFD600:0xFFC62828);card.setBackground(bg);
        TextView message=new TextView(host);message.setText(messageRes);message.setTextSize(14);
        message.setTextColor(optional ? 0xFF202020
                : (AppSettings.themeMode(host)==AppSettings.THEME_DARK?0xFFFFCDD2:0xFFB71C1C));
        card.addView(message,new LinearLayout.LayoutParams(-1,-2));
        boolean dark=AppSettings.themeMode(host)==AppSettings.THEME_DARK;
        int accent=optional ? 0xFF202020 : (dark?0xFFFF8A80:0xFFB71C1C);
        LinearLayout actions=new LinearLayout(host);actions.setGravity(Gravity.CENTER_VERTICAL);
        actions.setLayoutDirection(AppSettings.layoutDirection(host));
        LinearLayout.LayoutParams actionsParams=new LinearLayout.LayoutParams(-1,-2);
        actionsParams.topMargin=dp(host,14);card.addView(actions,actionsParams);
        Button review=action(host,optional?R.string.permissions_optional_allow:R.string.permissions_review,true,accent,dark);
        if(optional)review.setTextColor(0xFFFFD600);
        Button close=action(host,R.string.permissions_dismiss,false,accent,dark);
        LinearLayout.LayoutParams reviewParams=new LinearLayout.LayoutParams(0,dp(host,48),1);
        reviewParams.setMarginEnd(dp(host,10));
        actions.addView(review,reviewParams);
        actions.addView(close,new LinearLayout.LayoutParams(0,dp(host,48),1));
        int width=host.getResources().getDisplayMetrics().widthPixels-dp(host,24);
        PopupWindow popup=new PopupWindow(card,width,-2,false);popup.setBackgroundDrawable(bg);popup.setOutsideTouchable(false);
        close.setTag("permissions_warning_close");
        close.setOnClickListener(v->popup.dismiss());
        review.setTag("permissions_warning_grant");
        review.setOnClickListener(v->{popup.dismiss();grant.run();});
        View decor=host.getWindow().getDecorView();
        int bottom=0;
        android.view.WindowInsets insets=decor.getRootWindowInsets();
        if(insets!=null) bottom=android.os.Build.VERSION.SDK_INT>=30
                ? insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom
                : insets.getSystemWindowInsetBottom();
        popup.showAtLocation(decor,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,dp(host,12)+bottom);
        return popup;
    }
    private static Button action(Activity host,int text,boolean filled,int accent,boolean dark) {
        Button button=new Button(host);button.setText(text);button.setAllCaps(false);
        button.setTextSize(14);button.setSingleLine(true);button.setGravity(Gravity.CENTER);
        button.setAutoSizeTextTypeUniformWithConfiguration(11,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        button.setTextColor(filled ? (dark ? 0xFF351010 : android.graphics.Color.WHITE) : accent);
        button.setTypeface(button.getTypeface(),filled ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        button.setPadding(dp(host,12),0,dp(host,12),0);
        button.setMinWidth(0);button.setMinimumWidth(0);
        button.setElevation(0);button.setStateListAnimator(null);
        GradientDrawable shape=new GradientDrawable();shape.setCornerRadius(dp(host,14));
        shape.setColor(filled ? accent : android.graphics.Color.TRANSPARENT);
        shape.setStroke(dp(host,1),accent);
        GradientDrawable mask=new GradientDrawable();mask.setColor(android.graphics.Color.WHITE);mask.setCornerRadius(dp(host,14));
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(filled ? 0x33FFFFFF : ((accent & 0x00FFFFFF) | 0x22000000)),shape,mask));
        return button;
    }
    private static int dp(Activity host,int value){return Math.round(value*host.getResources().getDisplayMetrics().density);}
}
