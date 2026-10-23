package com.ilia.advanceclock;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** The same dismissal control is used by real ringing screens and the silent settings preview. */
final class AlarmDismissal {
    static final int TAP=0,HOLD=1,SWIPE=2,OUTLINE=3;
    static View control(Context context,int mode,int primary,int textColor,Runnable dismiss){
        if(mode!=SWIPE){
            int seconds=AppSettings.alarmDismissSeconds(context,mode);
            LinearLayout container=new LinearLayout(context);container.setOrientation(LinearLayout.VERTICAL);container.setGravity(Gravity.CENTER_HORIZONTAL);
            HoldToDismissView button=new HoldToDismissView(context,primary,textColor,mode,seconds*1000L,dismiss);
            button.setTag(mode==TAP?"alarm_dismiss_tap":mode==HOLD?"alarm_dismiss_hold":"alarm_dismiss_outline");
            container.addView(button,new LinearLayout.LayoutParams(mode==HOLD?dp(context,88):dp(context,220),dp(context,mode==HOLD?88:58)));
            TextView hint=hint(context,seconds==0?AppString.get(R.string.alarm_dismiss_instant):
                    context.getString(R.string.alarm_dismiss_hold_seconds,CalendarUtils.fa(seconds)),textColor,13);
            hint.setPadding(0,dp(context,6),0,0);container.addView(hint,new LinearLayout.LayoutParams(-1,-2));return container;
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
