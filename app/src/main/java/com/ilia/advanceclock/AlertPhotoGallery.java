package com.ilia.advanceclock;

import android.content.Context;
import android.view.MotionEvent;
import android.widget.ScrollView;

/** A gesture begun on photos belongs to the gallery until the finger is lifted. */
final class AlertPhotoGallery extends ScrollView {
    AlertPhotoGallery(Context context){super(context);}
    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        // A child photo can consume DOWN, so an OnTouchListener on ScrollView is too late.
        if(getParent()!=null)getParent().requestDisallowInterceptTouchEvent(true);
        try {return super.dispatchTouchEvent(event);}
        finally {
            int action=event.getActionMasked();
            if((action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL) && getParent()!=null)
                getParent().requestDisallowInterceptTouchEvent(false);
        }
    }
}
