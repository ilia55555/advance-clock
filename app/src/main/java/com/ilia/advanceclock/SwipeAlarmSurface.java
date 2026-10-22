package com.ilia.advanceclock;

import android.content.Context;
import android.view.*;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

final class SwipeAlarmSurface extends FrameLayout {
    private final Runnable stop;
    private float downX,downY;
    private boolean dragging,finished;
    SwipeAlarmSurface(Context context,Runnable stop) { super(context);this.stop=stop;setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES); }
    @Override public boolean onInterceptTouchEvent(MotionEvent event) {
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN) {
            downX=event.getRawX();downY=event.getRawY();dragging=false;animate().cancel();
        } else if(event.getActionMasked()==MotionEvent.ACTION_MOVE && !finished) {
            float dy=event.getRawY()-downY,dx=event.getRawX()-downX;
            if(dy < -ViewConfiguration.get(getContext()).getScaledTouchSlop() && -dy>Math.abs(dx)) {
                dragging=true;return true;
            }
        }
        return false;
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(finished) return true;
        float dy=event.getRawY()-downY;
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: downX=event.getRawX();downY=event.getRawY();return true;
            case MotionEvent.ACTION_MOVE:
                if(dy<0) { dragging=true;setTranslationY(dy*.85f); } return true;
            case MotionEvent.ACTION_UP:
                if(dragging && -dy>=getHeight()*.20f && getHeight()>0) dismiss();
                else springBack();return true;
            case MotionEvent.ACTION_CANCEL: springBack();return true;
        }
        return true;
    }
    private void springBack() { dragging=false;animate().translationY(0).setDuration(450).setInterpolator(new OvershootInterpolator(1.4f)).start(); }
    private void dismiss() {
        finished=true;
        animate().translationY(-getHeight()).alpha(0).setDuration(180)
                .setInterpolator(new android.view.animation.AccelerateInterpolator()).withEndAction(stop).start();
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
