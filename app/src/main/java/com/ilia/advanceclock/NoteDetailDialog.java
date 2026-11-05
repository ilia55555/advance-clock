package com.ilia.advanceclock;

import android.app.*;
import android.graphics.Rect;
import android.view.*;

/** Attached to the alert window: closing details never stops or snoozes the reminder. */
final class NoteDetailDialog {
    static Dialog show(Activity host,long noteId){
        Dialog dialog=new Dialog(host);dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        AppMotion.window(dialog.getWindow(),R.style.MotionDialog);
        NoteDetailView details=new NoteDetailView(host,noteId,dialog::dismiss);
        if(!details.render())return null;
        details.setBackground(ReminderModalStyle.shape(host,AppSettings.background(host),AppSettings.primaryColor(host),18));details.setClipToOutline(true);
        int inset=ReminderModalStyle.dp(host,1);details.setPadding(inset,inset,inset,inset);
        dialog.setContentView(details);dialog.setCanceledOnTouchOutside(false);
        dialog.setOnKeyListener((d,code,event)->{
            if(code!=KeyEvent.KEYCODE_VOLUME_DOWN && code!=KeyEvent.KEYCODE_VOLUME_UP)return false;
            return event.getAction()==KeyEvent.ACTION_DOWN?host.onKeyDown(code,event):host.onKeyUp(code,event);
        });
        dialog.show();
        Window window=dialog.getWindow();if(window!=null){
            window.setBackgroundDrawable(ReminderModalStyle.shape(host,AppSettings.surface(host),AppSettings.primaryColor(host),18));
            Rect visible=new Rect();host.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
            int height=visible.height()>0?visible.height():host.getResources().getDisplayMetrics().heightPixels;
            window.setLayout(Math.min(ReminderModalStyle.dp(host,560),host.getResources().getDisplayMetrics().widthPixels-ReminderModalStyle.dp(host,24)),Math.round(height*.88f));
        }
        return dialog;
    }
    static void refresh(Dialog dialog){
        if(dialog==null || !dialog.isShowing())return;
        android.view.View view=dialog.getWindow().getDecorView().findViewWithTag("note_detail_view");
        if(view instanceof NoteDetailView && !((NoteDetailView)view).render())dialog.dismiss();
    }
}
