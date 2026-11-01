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
        LinearLayout card=new LinearLayout(host);card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(AppSettings.layoutDirection(host));
        int pad=dp(host,16);card.setPadding(pad,pad,pad,pad);
        GradientDrawable bg=new GradientDrawable();bg.setColor(AppSettings.themeMode(host)==AppSettings.THEME_DARK?0xFF4A1919:0xFFFFEBEE);
        bg.setCornerRadius(dp(host,18));bg.setStroke(dp(host,1),0xFFC62828);card.setBackground(bg);
        TextView message=new TextView(host);message.setText(R.string.permissions_warning);message.setTextSize(14);
        message.setTextColor(AppSettings.themeMode(host)==AppSettings.THEME_DARK?0xFFFFCDD2:0xFFB71C1C);
        card.addView(message,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout actions=new LinearLayout(host);actions.setGravity(Gravity.END);card.addView(actions);
        Button review=new Button(host);review.setAllCaps(false);review.setText(R.string.permissions_review);review.setTextColor(0xFFC62828);
        Button close=new Button(host);close.setAllCaps(false);close.setText(R.string.permissions_dismiss);close.setTextColor(0xFFC62828);
        actions.addView(review,new LinearLayout.LayoutParams(0,-2,1));actions.addView(close,new LinearLayout.LayoutParams(0,-2,1));
        int width=host.getResources().getDisplayMetrics().widthPixels-dp(host,24);
        PopupWindow popup=new PopupWindow(card,width,-2,false);popup.setBackgroundDrawable(bg);popup.setOutsideTouchable(false);
        close.setTag("permissions_warning_close");
        close.setOnClickListener(v->popup.dismiss());
        review.setOnClickListener(v->{popup.dismiss();host.startActivityForResult(new Intent(host,PermissionCenterActivity.class).putExtra("review",true),PermissionCenterActivity.REQUEST);});
        View decor=host.getWindow().getDecorView();
        int bottom=0;
        android.view.WindowInsets insets=decor.getRootWindowInsets();
        if(insets!=null) bottom=android.os.Build.VERSION.SDK_INT>=30
                ? insets.getInsets(android.view.WindowInsets.Type.systemBars()).bottom
                : insets.getSystemWindowInsetBottom();
        popup.showAtLocation(decor,Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL,0,dp(host,12)+bottom);
        return popup;
    }
    private static int dp(Activity host,int value){return Math.round(value*host.getResources().getDisplayMetrics().density);}
}
