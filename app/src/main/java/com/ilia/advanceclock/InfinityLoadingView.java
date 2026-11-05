package com.ilia.advanceclock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** The supplied infinity path, dashed from 685 to -685 over two seconds. */
final class InfinityLoadingView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private ValueAnimator animator;
    private final float[] dash={300,385};
    InfinityLoadingView(Context context){
        super(context);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);setClickable(false);
        paint.setColor(AppSettings.primaryColor(context));paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(18);paint.setStrokeCap(Paint.Cap.ROUND);
        path.moveTo(275,75);path.rCubicTo(0,31,-27,50,-50,50);path.rCubicTo(-58,0,-92,-100,-150,-100);
        path.rCubicTo(-28,0,-50,22,-50,50);path.rCubicTo(0,28,23,50,50,50);path.rCubicTo(58,0,92,-100,150,-100);path.rCubicTo(24,0,50,19,50,50);path.close();
        paint.setPathEffect(new DashPathEffect(dash,685));
    }
    private void refresh(){
        if(!isAttachedToWindow() || !isShown() || getWindowVisibility()!=VISIBLE){stop();return;}
        if(animator!=null || !ValueAnimator.areAnimatorsEnabled())return;
        animator=ValueAnimator.ofFloat(685,-685);animator.setDuration(2000);animator.setInterpolator(new LinearInterpolator());animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(value->{paint.setPathEffect(new DashPathEffect(dash,(float)value.getAnimatedValue()));invalidate();});animator.start();
    }
    void stop(){if(animator!=null){animator.cancel();animator=null;}}
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);float scale=Math.min(getWidth()/300f,getHeight()/150f);canvas.save();canvas.translate((getWidth()-300*scale)/2,(getHeight()-150*scale)/2);canvas.scale(scale,scale);canvas.drawPath(path,paint);canvas.restore();}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();refresh();}
    @Override protected void onDetachedFromWindow(){stop();super.onDetachedFromWindow();}
    @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);refresh();}
    @Override protected void onWindowVisibilityChanged(int visibility){super.onWindowVisibilityChanged(visibility);refresh();}
}
