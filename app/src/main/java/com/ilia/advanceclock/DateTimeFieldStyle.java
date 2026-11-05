package com.ilia.advanceclock;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.TextView;

/** Larger, shared input typography; dates can wrap instead of losing their last digits. */
final class DateTimeFieldStyle {
    static void apply(Context context,TextView date,TextView time){
        field(context,date,R.dimen.editor_date_min_text_size,R.dimen.editor_date_text_size,2);
        field(context,time,R.dimen.editor_time_min_text_size,R.dimen.editor_time_text_size,1);
    }
    private static void field(Context c,TextView view,int min,int max,int lines){
        view.setGravity(Gravity.CENTER);view.setPadding(dp(c,4),dp(c,4),dp(c,4),dp(c,4));view.setMaxLines(lines);view.setIncludeFontPadding(false);view.setHorizontallyScrolling(false);
        view.setTextSize(TypedValue.COMPLEX_UNIT_PX,c.getResources().getDimension(max));
        view.setAutoSizeTextTypeUniformWithConfiguration(Math.max(1,Math.round(c.getResources().getDimension(min))),Math.max(1,Math.round(c.getResources().getDimension(max))),1,TypedValue.COMPLEX_UNIT_PX);
    }
    private static int dp(Context c,int value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
}
