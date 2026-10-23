package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Interactive and silent: dismissing this sample cannot touch a real playback service. */
final class AlarmDismissPreview extends LinearLayout {
    private final FrameLayout scene;
    private int mode;
    AlarmDismissPreview(Context context){
        super(context);setOrientation(VERTICAL);setTag("alarm_dismiss_preview");
        TextView title=new TextView(context);title.setText(AppString.get(R.string.alarm_dismiss_preview_title));
        title.setTextSize(14);title.setTextColor(AppSettings.textSecondary(context));title.setPadding(0,dp(8),0,dp(8));
        addView(title,new LayoutParams(-1,-2));
        scene=new FrameLayout(context);scene.setClipToOutline(true);
        GradientDrawable background=new GradientDrawable();background.setColor(AppSettings.field(context));background.setCornerRadius(dp(18));
        scene.setBackground(background);addView(scene,new LayoutParams(-1,dp(230)));
        setMode(AppSettings.alarmDismissMode(context));
    }
    void setMode(int mode){this.mode=mode;render();}
    private void render(){
        scene.removeAllViews();Context context=getContext();
        LinearLayout sample=new LinearLayout(context);sample.setOrientation(VERTICAL);sample.setGravity(Gravity.CENTER_HORIZONTAL);
        sample.setPadding(dp(12),dp(16),dp(12),dp(12));
        TextView time=new TextView(context);time.setText(TimeFormat.clock(context,System.currentTimeMillis()));
        time.setTextSize(28);time.setMaxLines(1);time.setHorizontallyScrolling(false);time.setIncludeFontPadding(false);
        time.setAutoSizeTextTypeUniformWithConfiguration(8,28,1,android.util.TypedValue.COMPLEX_UNIT_SP);time.setTypeface(null,Typeface.BOLD);time.setTextColor(AppSettings.textPrimary(context));time.setGravity(Gravity.CENTER);
        time.setTextDirection(View.TEXT_DIRECTION_LTR);sample.addView(time,new LayoutParams(-1,dp(44)));
        TextView caption=new TextView(context);caption.setText(AppString.get(R.string.alarm_dismiss_preview_try));
        caption.setTextSize(12);caption.setTextColor(AppSettings.textSecondary(context));caption.setGravity(Gravity.CENTER);
        caption.setMaxLines(2);caption.setAutoSizeTextTypeUniformWithConfiguration(4,12,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        sample.addView(caption,new LayoutParams(-1,dp(36)));sample.addView(new Space(context),new LayoutParams(1,0,1));
        View control=AlarmDismissal.control(context,mode,AppSettings.primaryColor(context),AppSettings.textPrimary(context),this::completed);
        sample.addView(control,new LayoutParams(-1,-2));
        if(mode==AlarmDismissal.SWIPE){
            SwipeAlarmSurface swipe=new SwipeAlarmSurface(context,this::completed);
            swipe.captureParentGestures();
            swipe.addView(sample,new FrameLayout.LayoutParams(-1,-1));
            swipe.addView(new SwipeHintArrow(context,control,AppSettings.textSecondary(context)),new FrameLayout.LayoutParams(-1,-1));
            scene.addView(swipe,new FrameLayout.LayoutParams(-1,-1));
        }else scene.addView(sample,new FrameLayout.LayoutParams(-1,-1));
    }
    private void completed(){
        scene.removeAllViews();Context context=getContext();
        LinearLayout done=new LinearLayout(context);done.setGravity(Gravity.CENTER);done.setOrientation(VERTICAL);
        TextView message=new TextView(context);message.setTag("alarm_dismiss_preview_done");
        message.setText(AppString.get(R.string.alarm_dismiss_preview_done));message.setTextColor(AppSettings.textPrimary(context));message.setTextSize(16);done.addView(message);
        Button again=new Button(context);again.setText(AppString.get(R.string.alarm_dismiss_preview_again));again.setAllCaps(false);
        again.setMaxLines(1);again.setEllipsize(null);again.setHorizontallyScrolling(false);again.setIncludeFontPadding(false);
        again.setAutoSizeTextTypeUniformWithConfiguration(4,14,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        again.setTextColor(AppSettings.primaryColor(context));again.setStateListAnimator(null);again.setBackgroundResource(R.drawable.bg_soft_button);
        again.setOnClickListener(view->render());LayoutParams params=new LayoutParams(-2,dp(44));params.topMargin=dp(12);done.addView(again,params);
        scene.addView(done,new FrameLayout.LayoutParams(-1,-1));
    }
    private int dp(int value){return AlarmDismissal.dp(getContext(),value);}
}
