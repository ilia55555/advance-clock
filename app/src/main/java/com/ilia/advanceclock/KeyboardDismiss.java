package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

final class KeyboardDismiss {
    static void onTouch(Activity host, MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                && host.getWindow().getDecorView().findFocus() instanceof EditText
                && !hitsInput(host.getWindow().getDecorView(), (int)event.getRawX(), (int)event.getRawY())) {
            hide(host);
        }
    }
    static void hide(Activity host) {
        View focus=host.getWindow().getDecorView().findFocus();
        if (!(focus instanceof EditText)) return;
        InputMethodManager manager=host.getSystemService(InputMethodManager.class);
        if (manager!=null) manager.hideSoftInputFromWindow(focus.getWindowToken(),0);
        ViewGroup content=host.findViewById(android.R.id.content);
        if (content!=null) {
            int descendants=content.getDescendantFocusability();
            content.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            content.setFocusableInTouchMode(true);
            focus.clearFocus();
            content.requestFocus();
            content.setDescendantFocusability(descendants);
        } else focus.clearFocus();
    }
    private static boolean hitsInput(View view,int x,int y) {
        if (!view.isShown()) return false;
        if (view instanceof EditText) {
            int[] position=new int[2];
            view.getLocationOnScreen(position);
            Rect rect=new Rect(position[0],position[1],position[0]+view.getWidth(),position[1]+view.getHeight());
            return rect.contains(x,y);
        }
        if (view instanceof ViewGroup) {
            ViewGroup group=(ViewGroup)view;
            for(int i=0;i<group.getChildCount();i++) if(hitsInput(group.getChildAt(i),x,y)) return true;
        }
        return false;
    }
}
