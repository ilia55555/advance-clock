package com.ilia.advanceclock;

import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;

/** Keep prayer controls outside visible navigation bars and display cutouts. */
final class AlertWindowInsets {
    static void apply(Window window, View root) {
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false);
        else window.getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        int left=root.getPaddingLeft(), top=root.getPaddingTop();
        int right=root.getPaddingRight(), bottom=root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            int l,t,r,b;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe=insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                l=safe.left; t=safe.top; r=safe.right; b=safe.bottom;
            } else {
                l=insets.getSystemWindowInsetLeft(); t=insets.getSystemWindowInsetTop();
                r=insets.getSystemWindowInsetRight(); b=insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left+l,top+t,right+r,bottom+b);
            // The root owns system insets; descendants must not add them a second time.
            return Build.VERSION.SDK_INT >= 30 ? WindowInsets.CONSUMED : insets.consumeSystemWindowInsets();
        });
        root.requestApplyInsets();
    }
}
