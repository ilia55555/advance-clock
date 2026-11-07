package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.EditText;

/** Reserve only the keyboard area that actually overlaps this window's content. */
final class KeyboardAvoidance {
    private final Window window;
    private boolean closed;
    private final ViewGroup content;
    private final int left, top, right, bottom;
    private final ViewTreeObserver.OnGlobalLayoutListener layout = this::update;
    private final ViewTreeObserver.OnGlobalFocusChangeListener focus = this::focusChanged;

    static void install(Activity host) {
        install(host.getWindow());
    }
    static void install(Window window) {
        if(window==null)return;
        ViewGroup content=window.findViewById(android.R.id.content);
        if(content==null || content.getTag(R.id.keyboard_avoidance)!=null)return;
        KeyboardAvoidance controller=new KeyboardAvoidance(window,content);
        content.setTag(R.id.keyboard_avoidance,controller);
    }
    static void forget(Activity host) {
        View content=host.findViewById(android.R.id.content);
        if(content==null)return;
        Object value=content.getTag(R.id.keyboard_avoidance);
        if(value instanceof KeyboardAvoidance){
            KeyboardAvoidance controller=(KeyboardAvoidance)value;
            controller.closed=true;
            if(content.getViewTreeObserver().isAlive()){
                content.getViewTreeObserver().removeOnGlobalLayoutListener(controller.layout);
                content.getViewTreeObserver().removeOnGlobalFocusChangeListener(controller.focus);
            }
            content.removeCallbacks(controller.reveal);
            content.setOnApplyWindowInsetsListener(null);
            content.setTag(R.id.keyboard_avoidance,null);
        }
    }
    private final Runnable reveal=this::revealFocus;
    private void focusChanged(View oldView,View newView){
        if(newView instanceof EditText){content.removeCallbacks(reveal);content.post(reveal);}
    }
    private KeyboardAvoidance(Window window,ViewGroup content) {
        this.window=window;this.content=content;
        left=content.getPaddingLeft();top=content.getPaddingTop();right=content.getPaddingRight();bottom=content.getPaddingBottom();
        int mode=window.getAttributes().softInputMode;
        window.setSoftInputMode((mode & ~WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST)
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        content.getViewTreeObserver().addOnGlobalLayoutListener(layout);
        content.getViewTreeObserver().addOnGlobalFocusChangeListener(focus);
        // Listen on the content container, leaving each screen's system-bar listener intact.
        content.setOnApplyWindowInsetsListener((view,insets)->{content.post(reveal);content.post(this::update);return insets;});
        content.requestApplyInsets();
    }
    private void update() {
        if(closed || content.getHeight()==0)return;
        WindowInsets insets=content.getRootWindowInsets();
        if(Build.VERSION.SDK_INT>=30 && insets!=null){
            int keyboardTop=window.getWindowManager().getCurrentWindowMetrics().getBounds().bottom
                    -insets.getInsets(WindowInsets.Type.ime()).bottom;
            apply(keyboardTop,insets.isVisible(WindowInsets.Type.ime()));
        }else{
            Rect visible=new Rect();content.getWindowVisibleDisplayFrame(visible);
            int[] position=new int[2];content.getLocationOnScreen(position);
            int overlap=position[1]+content.getHeight()-visible.bottom;
            boolean keyboard=visible.height()>0 && overlap>100*content.getResources().getDisplayMetrics().density;
            apply(visible.bottom,keyboard);
        }
    }
    void apply(int keyboardTop,boolean visible) {
        int[] position=new int[2];content.getLocationOnScreen(position);
        int overlap=visible?Math.max(0,position[1]+content.getHeight()-keyboardTop):0;
        int padding=bottom+overlap;
        if(content.getPaddingBottom()!=padding){
            content.setPadding(left,top,right,padding);
            content.removeCallbacks(reveal);content.post(reveal);
        }else if(visible)revealFocus();
    }
    private void revealFocus() {
        if(closed)return;
        View field=content.findFocus();
        if(!(field instanceof EditText) || !field.isShown())return;
        int gap=Math.round(12*field.getResources().getDisplayMetrics().density);
        Rect area=new Rect();field.getFocusedRect(area);
        area.top=Math.max(0,area.top-gap);area.bottom+=gap;
        // Scroll the surrounding form, without taking over focus or replacing field listeners.
        field.requestRectangleOnScreen(area,false);
    }
}
