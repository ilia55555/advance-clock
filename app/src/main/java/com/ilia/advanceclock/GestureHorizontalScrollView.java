package com.ilia.advanceclock;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.HorizontalScrollView;

public final class GestureHorizontalScrollView extends HorizontalScrollView {
    private float downX;
    private float downY;
    private final int slop;

    public GestureHorizontalScrollView(Context context) {
        this(context, null);
    }

    public GestureHorizontalScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        slop = ViewConfiguration.get(context).getScaledTouchSlop();
        setHorizontalScrollBarEnabled(false);
    }

    @Override public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = ev.getX();
                downY = ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = Math.abs(ev.getX() - downX);
                float dy = Math.abs(ev.getY() - downY);
                if (dx > slop && dx > dy) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override public boolean onTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_MOVE) {
            float dx = Math.abs(ev.getX() - downX);
            float dy = Math.abs(ev.getY() - downY);
            if (dx > slop && dx > dy) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
        } else if (ev.getActionMasked() == MotionEvent.ACTION_UP
                || ev.getActionMasked() == MotionEvent.ACTION_CANCEL) {
            getParent().requestDisallowInterceptTouchEvent(false);
        }
        return super.onTouchEvent(ev);
    }
}
