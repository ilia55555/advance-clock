package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.*;
import android.view.*;

/** An unmarked board between alert text and controls; paths live only for this alert. */
final class AlertDrawingView extends View {
    private final Paint ink=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path drawing=new Path();
    AlertDrawingView(Context context,boolean dark) {
        super(context);
        ink.setColor(dark?0xFFEDEDE7:0xFF222222);
        ink.setStyle(Paint.Style.STROKE);ink.setStrokeWidth(3*getResources().getDisplayMetrics().density);
        ink.setStrokeCap(Paint.Cap.ROUND);ink.setStrokeJoin(Paint.Join.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    @Override protected void onDraw(Canvas canvas){super.onDraw(canvas);canvas.drawPath(drawing,ink);}
    @Override public boolean onTouchEvent(MotionEvent event) {
        switch(event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
                drawing.moveTo(event.getX(),event.getY());drawing.lineTo(event.getX()+.1f,event.getY());break;
            case MotionEvent.ACTION_MOVE:
                for(int i=0;i<event.getHistorySize();i++)drawing.lineTo(event.getHistoricalX(i),event.getHistoricalY(i));
                drawing.lineTo(event.getX(),event.getY());break;
            case MotionEvent.ACTION_UP:case MotionEvent.ACTION_CANCEL:
                if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(false);break;
            default:return true;
        }
        invalidate();return true;
    }
}
