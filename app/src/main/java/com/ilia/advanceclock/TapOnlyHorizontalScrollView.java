package com.ilia.advanceclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;

/** A real tap opens settings; scrolling, cancelled touches and long presses never do. */
public final class TapOnlyHorizontalScrollView extends HorizontalScrollView {
    private Runnable tap;
    private float downX,downY;
    private long downTime;
    private boolean tracking,moved;
    public TapOnlyHorizontalScrollView(Context context){this(context,null);}
    public TapOnlyHorizontalScrollView(Context context,AttributeSet attrs){super(context,attrs);setClickable(true);setFocusable(true);}
    void setOnTapListener(Runnable action){tap=action;}
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        int action=event.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN) {
            tracking=true;moved=false;downX=event.getRawX();downY=event.getRawY();downTime=event.getEventTime();
        } else if(tracking) {
            int slop=ViewConfiguration.get(getContext()).getScaledTouchSlop();
            if(Math.hypot(event.getRawX()-downX,event.getRawY()-downY)>slop)moved=true;
        }
        if(action==MotionEvent.ACTION_POINTER_DOWN)moved=true;
        boolean clicked=tracking && action==MotionEvent.ACTION_UP && !moved
                && event.getEventTime()-downTime<ViewConfiguration.getLongPressTimeout()
                && event.getX()>=0 && event.getX()<getWidth() && event.getY()>=0 && event.getY()<getHeight();
        if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL)tracking=false;
        boolean handled=super.dispatchTouchEvent(event);
        if(clicked)performClick();
        return handled || clicked;
    }
    @Override public boolean performClick(){super.performClick();if(tap!=null)tap.run();return true;}
}
