package com.ilia.advanceclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** The same dismissal control is used by real ringing screens and the silent settings preview. */
final class AlarmDismissal {
    static final int TAP=0,HOLD=1,SWIPE=2;
    static View control(Context context,int mode,int primary,int textColor,Runnable dismiss){
        if(mode==TAP){
            Button button=new Button(context);button.setTag("alarm_dismiss_tap");
            button.setText(AppString.get(R.string.ui_stop_alarm));button.setAllCaps(false);button.setTextSize(16);
            button.setTextColor(Color.luminance(primary)>.5?0xff17212b:Color.WHITE);
            button.setMinHeight(dp(context,60));button.setStateListAnimator(null);button.setElevation(0);
            GradientDrawable shape=new GradientDrawable();shape.setColor(primary);shape.setCornerRadius(dp(context,18));
            button.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000),shape,null));
            button.setOnClickListener(view->dismiss.run());return button;
        }
        if(mode==HOLD){
            LinearLayout container=new LinearLayout(context);container.setOrientation(LinearLayout.VERTICAL);container.setGravity(Gravity.CENTER_HORIZONTAL);
            container.addView(new HoldToDismissView(context,primary,dismiss),new LinearLayout.LayoutParams(dp(context,72),dp(context,72)));
            TextView hint=hint(context,AppString.get(R.string.alarm_dismiss_hold_hint),textColor,14);
            hint.setPadding(0,dp(context,4),0,0);container.addView(hint,new LinearLayout.LayoutParams(-1,-2));return container;
        }
        TextView hint=hint(context,AppString.get(R.string.alarm_swipe_stop),textColor,18);
        hint.setTag("alarm_dismiss_swipe_hint");hint.setPadding(0,dp(context,12),0,0);return hint;
    }
    private static TextView hint(Context context,String text,int color,int max){
        TextView hint=new TextView(context);hint.setText(text);hint.setTextSize(max);hint.setTextColor(color);
        hint.setGravity(Gravity.CENTER);hint.setMaxLines(1);hint.setEllipsize(null);hint.setIncludeFontPadding(false);
        hint.setHorizontallyScrolling(false);hint.setAutoSizeTextTypeUniformWithConfiguration(4,max,1,android.util.TypedValue.COMPLEX_UNIT_SP);return hint;
    }
    static int dp(Context context,int value){return Math.round(value*context.getResources().getDisplayMetrics().density);}
}
