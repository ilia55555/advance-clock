package com.ilia.advanceclock;

import android.content.Context;
import android.view.*;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

final class SwipeAlarmSurface extends FrameLayout {
    private final Runnable stop;
    private float downX,downY;
    private boolean dragging,finished,completed;
    private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable finishFallback=this::completeDismiss;
    private final Runnable resetFallback=this::resetPosition;

    private boolean captureParentGestures;
    void captureParentGestures(){captureParentGestures=true;}
    private void releaseParent(){if(captureParentGestures && getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);}
    SwipeAlarmSurface(Context context,Runnable stop) { super(context);this.stop=stop;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); }
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        // Once dismissal is committed, a second finger/tap must never cancel its animation.
        if(finished)return true;
        return super.dispatchTouchEvent(event);
    }
    private void beginGesture(MotionEvent event) {
        main.removeCallbacks(resetFallback);
        animate().withEndAction(null).cancel();
        setTranslationY(0);setAlpha(1);
        downX=event.getRawX();downY=event.getRawY();dragging=false;
    }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if(finished)return true;
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN) {
            beginGesture(event);
            if(captureParentGestures && getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
        } else if(event.getActionMasked()==MotionEvent.ACTION_MOVE && !finished) {
            float dy=event.getRawY()-downY,dx=event.getRawX()-downX;
            if(dy < -ViewConfiguration.get(getContext()).getScaledTouchSlop() && -dy>Math.abs(dx)) {
                dragging=true;return true;
            }
        }
        if(event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL)releaseParent();
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(finished) return true;
        float dy=event.getRawY()-downY;
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: beginGesture(event);return true;
            case MotionEvent.ACTION_MOVE:
                if(dy<0) { dragging=true;setTranslationY(dy*.85f); }
                else setTranslationY(0);
                return true;
            case MotionEvent.ACTION_UP:
                releaseParent();
                if(dragging && -dy>=getHeight()*.20f && getHeight()>0) dismiss();
                else springBack();return true;
            case MotionEvent.ACTION_CANCEL: releaseParent();springBack();return true;
        }
        return true;
    }
    private void resetPosition() {
        if(finished)return;
        main.removeCallbacks(resetFallback);
        animate().withEndAction(null).cancel();
        dragging=false;setTranslationY(0);setAlpha(1);
    }
    private void springBack() {
        if(finished)return;
        dragging=false;
        animate().withEndAction(null).cancel();
        main.removeCallbacks(resetFallback);main.postDelayed(resetFallback,500);
        animate().translationY(0).alpha(1).setDuration(450)
                .setInterpolator(new OvershootInterpolator(1.4f)).withEndAction(this::resetPosition).start();
    }
    private void completeDismiss() {
        if(!finished || completed)return;
        completed=true;
        main.removeCallbacks(finishFallback);
        stop.run();
    }
    private void dismiss() {
        if(finished)return;
        finished=true;releaseParent();
        main.removeCallbacks(resetFallback);
        animate().withEndAction(null).cancel();
        // Completion cannot depend on OEM animation callbacks or animation settings.
        main.postDelayed(finishFallback,220);
        animate().translationY(-getHeight()).alpha(0).setDuration(180)
                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                .withEndAction(this::completeDismiss).start();
    }
    @Override protected void onDetachedFromWindow() {
        main.removeCallbacks(resetFallback);
        animate().withEndAction(null).cancel();
        if(finished)completeDismiss();
        else {setTranslationY(0);setAlpha(1);}
        releaseParent();super.onDetachedFromWindow();
    }
    @Override public void onInitializeAccessibilityNodeInfo(android.view.accessibility.AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(new android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction(
                android.view.accessibility.AccessibilityNodeInfo.ACTION_DISMISS,getContext().getString(R.string.ui_stop_alarm)));
    }
    @Override public boolean performAccessibilityAction(int action,android.os.Bundle args) {
        if(action==android.view.accessibility.AccessibilityNodeInfo.ACTION_DISMISS) { if(!finished)dismiss();return true; }
        return super.performAccessibilityAction(action,args);
    }
}
