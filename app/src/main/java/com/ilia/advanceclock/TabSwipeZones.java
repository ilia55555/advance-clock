package com.ilia.advanceclock;

import android.graphics.Rect;
import android.view.View;

/** Decide ownership at DOWN; taps and vertical scrolling still go to the child. */
final class TabSwipeZones {
    private static boolean hit(View view,float x,float y,Rect rect){
        if(view==null || !view.isShown() || !view.getLocalVisibleRect(rect))return false;
        int[] location=new int[2];view.getLocationOnScreen(location);rect.offset(location[0],location[1]);
        return rect.contains((int)x,(int)y);
    }
    static boolean edge(View root,float x,float y){
        Rect rect=new Rect();if(!hit(root,x,y,rect))return false;
        float band=24*root.getResources().getDisplayMetrics().density;
        return x-rect.left<band || rect.right-x<band || y-rect.top<band || rect.bottom-y<band;
    }
    static boolean calendar(TripleCalendarView view,float x,float y){
        Rect rect=new Rect();if(!hit(view,x,y,rect))return false;
        int[] location=new int[2];view.getLocationOnScreen(location);
        return view.tabSwipeFooter(y-location[1]);
    }
    static boolean prayer(View view,float x,float y){
        Rect rect=new Rect();if(!hit(view,x,y,rect))return false;
        int[] location=new int[2];view.getLocationOnScreen(location);
        float band=12*view.getResources().getDisplayMetrics().density,localY=y-location[1];
        return localY<band || localY>=view.getHeight()-band;
    }
}
