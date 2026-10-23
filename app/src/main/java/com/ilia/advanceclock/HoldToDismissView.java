package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.*;

/** A real timed hold; animator duration settings cannot shorten the confirmation period. */
final class HoldToDismissView extends View {
    static final long HOLD_MILLIS=2500;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final android.graphics.drawable.Drawable dismissIcon;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Runnable dismiss;
    private final int color,style;
    private final long duration;
    private boolean holding,finished;
    private long startedAt;
    private float progress;
    private final Runnable complete=this::completeHold;
    private final Runnable frame=new Runnable(){
        @Override public void run(){
            if(!holding)return;
            progress=Math.min(1f,(SystemClock.uptimeMillis()-startedAt)/(float)duration);
            invalidate();postOnAnimation(this);
        }
    };
    HoldToDismissView(Context context,int color,Runnable dismiss){
        this(context,color,color,AlarmDismissal.HOLD,HOLD_MILLIS,dismiss);
    }
    HoldToDismissView(Context context,int color,int textColor,int style,long duration,Runnable dismiss){
        super(context);dismissIcon=context.getDrawable(R.drawable.ic_alarm_dismiss).mutate();this.color=color;this.style=style;this.duration=duration;this.dismiss=dismiss;
        setTag("alarm_dismiss_hold");setClickable(true);setFocusable(true);setLongClickable(duration>0);
        setContentDescription(duration==0?AppString.get(R.string.ui_stop_alarm):
                context.getString(R.string.alarm_dismiss_hold_seconds,CalendarUtils.fa((int)(duration/1000))));
        if(duration>0)setOnLongClickListener(view->{begin();return true;});
    }
    private void completeHold(){
        if(!holding || finished)return;
        long remaining=duration-(SystemClock.uptimeMillis()-startedAt);
        if(remaining>0){handler.postDelayed(complete,remaining);return;}
        holding=false;finished=true;progress=1;removeCallbacks(frame);invalidate();
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
        dismiss.run();
    }
    private void begin(){
        if(duration==0||holding||finished||!isEnabled())return;
        holding=true;startedAt=SystemClock.uptimeMillis();progress=0;
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
        handler.postDelayed(complete,duration);postOnAnimation(frame);invalidate();
    }
    private void cancel(){
        holding=false;handler.removeCallbacks(complete);removeCallbacks(frame);progress=0;invalidate();
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled())return false;
        if(finished)return true;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:if(duration>0)begin();else {holding=true;invalidate();}return true;
            case MotionEvent.ACTION_MOVE:
                if(event.getX()<0||event.getY()<0||event.getX()>getWidth()||event.getY()>getHeight())cancel();return true;
            case MotionEvent.ACTION_UP:
                if(duration==0){boolean click=holding;cancel();if(click)performClick();return true;}
                if(holding && SystemClock.uptimeMillis()-startedAt>=duration)complete.run();
                if(!finished)cancel();performClick();return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_DOWN:cancel();return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();if(duration==0 && !finished && isEnabled()){finished=true;dismiss.run();}return true;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float x=getWidth()/2f,y=getHeight()/2f;
        boolean circle=style==AlarmDismissal.HOLD,outline=style==AlarmDismissal.OUTLINE;
        float inset=dp(3),round=circle?y:style==AlarmDismissal.TAP?y:dp(16);
        RectF bounds=new RectF(inset,inset,getWidth()-inset,getHeight()-inset);
        paint.setStyle(Paint.Style.FILL);paint.setColor(outline?((color&0xffffff)|0x12000000):color);
        canvas.drawRoundRect(bounds,round,round,paint);
        if(holding){
            canvas.save();canvas.clipRect(inset,inset,inset+(getWidth()-2*inset)*progress,getHeight()-inset);
            paint.setColor(outline?((color&0xffffff)|0x33000000):0x22ffffff);
            canvas.drawRoundRect(bounds,round,round,paint);canvas.restore();
        }
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(2));paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor(outline?color:((color&0xffffff)|0x55000000));
        canvas.drawRoundRect(bounds,round,round,paint);
        if(duration>0 && progress>0){
            paint.setColor(outline?color:Color.WHITE);paint.setStrokeWidth(dp(3));
            if(circle)canvas.drawArc(bounds,-90,progress*360,false,paint);
            else {Path border=new Path();border.addRoundRect(bounds,round,round,Path.Direction.CW);
                PathMeasure measure=new PathMeasure(border,false);Path segment=new Path();
                measure.getSegment(0,measure.getLength()*progress,segment,true);canvas.drawPath(segment,paint);}
        }
        int ink=outline?color:Color.luminance(color)>.5?Color.BLACK:Color.WHITE;
        dismissIcon.setTint(ink);
        float iconX=circle?x:dp(30),half=dp(circle?17:14);
        dismissIcon.setBounds(Math.round(iconX-half),Math.round(y-half),Math.round(iconX+half),Math.round(y+half));
        dismissIcon.draw(canvas);
        if(!circle){paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
            String label=AppString.get(R.string.ui_stop_alarm);paint.setTextSize(dp(16));
            float available=getWidth()-dp(78);while(paint.measureText(label)>available && paint.getTextSize()>dp(8))paint.setTextSize(paint.getTextSize()-1);
            paint.setTextAlign(Paint.Align.CENTER);canvas.drawText(label,(getWidth()+dp(30))/2f,y-(paint.ascent()+paint.descent())/2,paint);}
    }
    @Override protected void onDetachedFromWindow(){cancel();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)cancel();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused)cancel();}
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
}
