package com.ilia.advanceclock;

import android.os.Build;
import android.view.*;

/** Draw the header to the sheet edges while keeping its text below system bars. */
final class EditorHeaderInsets {
    static void apply(Window window,View root,View header){
        int headerTop=header.getPaddingTop(),headerBottom=header.getPaddingBottom();
        int bottom=root.getPaddingBottom();
        if(Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false);
        else window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        window.setStatusBarColor(AppSettings.primaryColor(root.getContext()));
        root.setOnApplyWindowInsetsListener((view,insets)->{
            int topInset,bottomInset;
            if(Build.VERSION.SDK_INT>=30){
                android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());
                topInset=safe.top;bottomInset=safe.bottom;
            }else{topInset=insets.getSystemWindowInsetTop();bottomInset=insets.getSystemWindowInsetBottom();}
            header.setPadding(header.getPaddingLeft(),headerTop+topInset,header.getPaddingRight(),headerBottom);
            view.setPaddingRelative(0,0,0,bottom+bottomInset);
            return insets;
        });
        root.requestApplyInsets();
    }
}
