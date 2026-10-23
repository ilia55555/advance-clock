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
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Runnable dismiss;
    private final int color;
    private boolean holding,finished;
    private long startedAt;
    private float progress;
    private final Runnable complete=this::completeHold;
    private final Runnable frame=new Runnable(){
        @Override public void run(){
            if(!holding)return;
            progress=Math.min(1f,(SystemClock.uptimeMillis()-startedAt)/(float)HOLD_MILLIS);
            invalidate();postOnAnimation(this);
        }
    };
    HoldToDismissView(Context context,int color,Runnable dismiss){
        super(context);this.color=color;this.dismiss=dismiss;
        setTag("alarm_dismiss_hold");setClickable(true);setFocusable(true);setLongClickable(true);
        setContentDescription(AppString.get(R.string.alarm_dismiss_hold_hint));
        setOnLongClickListener(view->{begin();return true;});
    }
    private void completeHold(){
        if(!holding || finished)return;
        long remaining=HOLD_MILLIS-(SystemClock.uptimeMillis()-startedAt);
        if(remaining>0){handler.postDelayed(complete,remaining);return;}
        holding=false;finished=true;progress=1;removeCallbacks(frame);invalidate();
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
        dismiss.run();
    }
    private void begin(){
        if(holding||finished||!isEnabled())return;
        holding=true;startedAt=SystemClock.uptimeMillis();progress=0;
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
        handler.postDelayed(complete,HOLD_MILLIS);postOnAnimation(frame);invalidate();
    }
    private void cancel(){
        holding=false;handler.removeCallbacks(complete);removeCallbacks(frame);progress=0;invalidate();
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);
    }
    @Override public boolean onTouchEvent(MotionEvent event){
        if(!isEnabled())return false;
        if(finished)return true;
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:begin();return true;
            case MotionEvent.ACTION_MOVE:
                if(event.getX()<0||event.getY()<0||event.getX()>getWidth()||event.getY()>getHeight())cancel();return true;
            case MotionEvent.ACTION_UP:
                if(holding && SystemClock.uptimeMillis()-startedAt>=HOLD_MILLIS)complete.run();
                if(!finished)cancel();performClick();return true;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_DOWN:cancel();return true;
        }
        return true;
    }
    @Override public boolean performClick(){super.performClick();return true;}
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        float x=getWidth()/2f,y=getHeight()/2f,r=Math.max(0,Math.min(x,y)-dp(5));
        paint.setStyle(Paint.Style.FILL);paint.setColor((color&0xffffff)|0x16000000);canvas.drawCircle(x,y,r,paint);
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setColor((color&0xffffff)|0x50000000);canvas.drawCircle(x,y,r,paint);
        paint.setColor(color);canvas.drawArc(x-r,y-r,x+r,y+r,-90,progress*360,false,paint);
        paint.setStyle(Paint.Style.FILL);float half=dp(9);
        canvas.drawRoundRect(x-half,y-half,x+half,y+half,dp(3),dp(3),paint);
    }
    @Override protected void onDetachedFromWindow(){cancel();super.onDetachedFromWindow();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);if(visibility!=VISIBLE)cancel();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(!focused)cancel();}
    private float dp(float value){return value*getResources().getDisplayMetrics().density;}
}
