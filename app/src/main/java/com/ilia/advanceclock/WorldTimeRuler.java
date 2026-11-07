package com.ilia.advanceclock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.DecelerateInterpolator;
import java.util.Calendar;
import java.util.Date;

/** Physical left is the past in every locale. Drag freely; settle near wall-clock anchors. */
public final class WorldTimeRuler extends View {
    static final long MINUTE = 60_000L, RANGE = 24 * 60 * MINUTE, VISIBLE = 12 * 60 * MINUTE;
    interface Listener { void selected(long millis, boolean now); }
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long origin = System.currentTimeMillis(), selected = origin;
    private boolean live = true, dragging;
    private Listener listener;
    private ValueAnimator settle;
    private long viewport;
    private float downX;
    private long downViewport;
    private boolean moved;

    public WorldTimeRuler(Context c, AttributeSet attrs) {
        super(c, attrs);
        setFocusable(true); setClickable(true);
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        viewport=origin;
        setHorizontalScrollBarEnabled(true);
        setScrollbarFadingEnabled(true);
    }
    void setListener(Listener value) { listener = value; }
    void display(long now, long shown, boolean isLive) {
        if (dragging || settle != null) return;
        origin = now; selected = shown; live = isLive;
        viewport=Math.max(origin-RANGE,Math.min(origin+RANGE,shown));
        describe(); invalidate();
    }
    static long anchor(long millis) {
        Calendar local = Calendar.getInstance(AppTime.zone());
        local.setTimeInMillis(millis);
        long within = (local.get(Calendar.MINUTE) % 30) * MINUTE
                + local.get(Calendar.SECOND) * 1000L + local.get(Calendar.MILLISECOND);
        long delta = within <= 15 * MINUTE ? -within : 30 * MINUTE - within;
        return Math.abs(delta) < 5 * MINUTE ? millis + delta : millis;
    }
    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private float left() { return dp(12); }
    private float span() { return Math.max(1, getWidth() - 2 * left()); }
    private float x(long millis) { return getWidth()/2f + (float)((millis-viewport)/(2.0*VISIBLE))*span(); }
    private long time(float x) {
        return clamp(viewport + Math.round((x-getWidth()/2f)/span()*2*VISIBLE));
    }
    private long clamp(long value){return Math.max(origin-RANGE,Math.min(origin+RANGE,value));}
    private void choose(long millis, boolean isNow) {
        selected = millis; viewport=clamp(millis);live = isNow; describe(); invalidate();
        if (listener != null) listener.selected(selected, live);
    }
    private void describe() {
        setContentDescription(AppString.get(R.string.world_time_ruler) + ": "
                + TimeFormat.formatter(getContext(), false, AppTime.zone()).format(new Date(selected)));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int primary = AppSettings.primaryColor(getContext()), muted = AppSettings.textSecondary(getContext());
        float y=dp(20), mid=getWidth()/2f;
        paint.setStrokeWidth(dp(1)); paint.setColor(muted); paint.setAlpha(65);
        canvas.drawLine(Math.max(left(),x(origin-RANGE)),y,Math.min(left()+span(),x(origin+RANGE)),y,paint); paint.setAlpha(255);
        // Tick positions and magnets share local wall-clock hours, including fractional zones.
        Calendar local=Calendar.getInstance(AppTime.zone());local.setTimeInMillis(origin);
        long floor=origin-(local.get(Calendar.MINUTE)%30)*MINUTE-local.get(Calendar.SECOND)*1000L-local.get(Calendar.MILLISECOND);
        for (int half=-48; half<=49; half++) {
            long tick=floor+half*30*MINUTE;if(tick<origin-RANGE || tick>origin+RANGE)continue;
            float px=x(tick);if(px<left() || px>left()+span())continue;local.setTimeInMillis(tick);
            paint.setColor(muted);paint.setAlpha((int)(150*edgeAlpha(px)));
            if(local.get(Calendar.MINUTE)==0)canvas.drawCircle(px,y,dp(1.35f),paint);
            else canvas.drawLine(px,y-dp(2.3f),px,y+dp(2.3f),paint);
        }
        paint.setAlpha(255);paint.setColor(primary);
        if(x(origin)>=left() && x(origin)<=left()+span())canvas.drawCircle(x(origin),y,dp(3.7f),paint);
        // A separate thumb leaves the current-time dot visible when exploring other times.
        if (selected>=origin-RANGE && selected<=origin+RANGE) {
            float px=x(selected);paint.setColor(primary);paint.setStrokeWidth(dp(2));
            canvas.drawLine(px,y-dp(11),px,y+dp(11),paint);
            canvas.drawCircle(px,y-dp(12),dp(3),paint);
        }
        paint.setTextSize(dp(11));
        for(int hour=-24;hour<=24;hour+=12){
            float px=x(origin+hour*60*MINUTE);if(px<left()-1 || px>left()+span()+1)continue;
            paint.setColor(hour==0?primary:muted);paint.setAlpha((int)(255*edgeAlpha(px)));
            paint.setTextAlign(hour==-24?Paint.Align.LEFT:hour==24?Paint.Align.RIGHT:Paint.Align.CENTER);
            // Keep endpoint labels inside the viewport without adding a separate header.
            String label=hour==0?AppString.get(R.string.world_time_now):(hour<0?"−":"+")+Math.abs(hour)+" h";
            float half=paint.measureText(label)/2;
            if(hour!=24 && hour!=-24)px=Math.max(left()+half,Math.min(left()+span()-half,px));
            canvas.drawText(label,px,dp(47),paint);
        }
        paint.setAlpha(255);
    }
    private float edgeAlpha(float px){return .3f+.7f*Math.min(1,Math.max(0,Math.min(px-left(),left()+span()-px))/dp(18));}
    @Override protected int computeHorizontalScrollRange(){return getWidth()*3;}
    @Override protected int computeHorizontalScrollExtent(){return getWidth();}
    @Override protected int computeHorizontalScrollOffset(){return Math.round((float)((viewport-origin+RANGE)/(2.0*RANGE))*getWidth()*2);}
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelSettle(); dragging=true;
                if (getParent()!=null) getParent().requestDisallowInterceptTouchEvent(true);
                // Freeze the origin for this gesture so the range cannot drift under the finger.
                origin=System.currentTimeMillis();downX=event.getX();downViewport=viewport;moved=false;return true;
            case MotionEvent.ACTION_MOVE:
                if(dragging){
                    if(Math.abs(event.getX()-downX)>android.view.ViewConfiguration.get(getContext()).getScaledTouchSlop())moved=true;
                    if(moved){choose(clamp(downViewport-Math.round((event.getX()-downX)/span()*2*VISIBLE)),false);awakenScrollBars();}
                }return true;
            case MotionEvent.ACTION_UP:
                if(dragging){if(moved)choose(clamp(downViewport-Math.round((event.getX()-downX)/span()*2*VISIBLE)),false);else choose(time(event.getX()),false);dragging=false;finishSelection();performClick();}
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);return true;
            case MotionEvent.ACTION_CANCEL:
                dragging=false;cancelSettle();
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);return true;
        }
        return true;
    }
    private void finishSelection() {
        long target = Math.abs(selected-origin)<30_000L ? origin : anchor(selected);
        target=Math.max(origin-RANGE,Math.min(origin+RANGE,target));
        final long end=target; final boolean isNow=end==origin;
        if(end==selected || !ValueAnimator.areAnimatorsEnabled()){choose(end,isNow);return;}
        final long start=selected;
        settle=ValueAnimator.ofFloat(0,1);settle.setDuration(220);
        settle.setInterpolator(new DecelerateInterpolator());
        settle.addUpdateListener(a->{float f=(float)a.getAnimatedValue();choose(start+Math.round((end-start)*(double)f),isNow && f==1f);});
        settle.addListener(new android.animation.AnimatorListenerAdapter(){
            @Override public void onAnimationEnd(android.animation.Animator a){settle=null;}
        });settle.start();
    }
    private void cancelSettle(){if(settle!=null){settle.cancel();settle=null;}}
    @Override public boolean performClick(){super.performClick();return true;}
    @Override public boolean onKeyDown(int code,KeyEvent event){
        int direction=code==KeyEvent.KEYCODE_DPAD_LEFT?-1:code==KeyEvent.KEYCODE_DPAD_RIGHT?1:0;
        if(direction==0)return super.onKeyDown(code,event);
        step(direction);return true;
    }
    private void step(int direction){cancelSettle();choose(Math.max(origin-RANGE,Math.min(origin+RANGE,selected+direction*60*MINUTE)),false);finishSelection();}
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info){
        super.onInitializeAccessibilityNodeInfo(info);info.setClassName("android.widget.SeekBar");
        info.setRangeInfo(AccessibilityNodeInfo.RangeInfo.obtain(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_FLOAT,-1440,1440,(float)Math.max(-1440,Math.min(1440,(selected-origin)/(double)MINUTE))));
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_FORWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_BACKWARD);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SET_PROGRESS);
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args){
        if(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD || action==AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD){step(action==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD?1:-1);return true;}
        if(action==android.R.id.accessibilityActionSetProgress && args!=null){
            cancelSettle();float minutes=args.getFloat(AccessibilityNodeInfo.ACTION_ARGUMENT_PROGRESS_VALUE);
            if(!Float.isFinite(minutes))return false;
            choose(origin+Math.round(Math.max(-1440,Math.min(1440,minutes))*MINUTE),false);finishSelection();return true;
        }
        return super.performAccessibilityAction(action,args);
    }
    @Override protected void onDetachedFromWindow(){cancelSettle();dragging=false;super.onDetachedFromWindow();}
}
