package com.ilia.advanceclock;

import android.animation.ValueAnimator;
import android.app.*;
import android.content.*;
import android.transition.*;
import android.view.*;

/** Local, bounded motion. Window dismissal remains synchronous and owned by Android. */
final class AppMotion {
    static boolean enabled(){return ValueAnimator.areAnimatorsEnabled();}
    static void window(Window window,int style){if(window!=null)window.setWindowAnimations(enabled()?style:0);}
    static void expand(ViewGroup parent){
        if(parent==null || !parent.isLaidOut() || !enabled())return;

        TransitionSet set=new TransitionSet().setOrdering(TransitionSet.ORDERING_TOGETHER);
        set.addTransition(new ChangeBounds());set.addTransition(new Fade());set.setDuration(360);
        set.setInterpolator(new android.view.animation.PathInterpolator(.25f,.1f,.25f,1));
        TransitionManager.beginDelayedTransition(parent,set);
    }
    static void launch(Activity host,Intent intent,View origin){
        if(enabled() && origin!=null && origin.isLaidOut())host.startActivity(intent,ActivityOptions.makeScaleUpAnimation(origin,0,0,origin.getWidth(),origin.getHeight()).toBundle());
        else {host.startActivity(intent);if(!enabled())host.overridePendingTransition(0,0);}
    }
    static boolean editor(Activity host){return host instanceof AlarmEditorActivity || host instanceof NoForgetEditorActivity || host instanceof AlarmAttachmentEditorActivity || host instanceof AlarmPhotoCropActivity;}
    static void activity(Activity host){
        if(host instanceof AlarmRingActivity || host instanceof NoteReminderActivity || host instanceof AdhanRingActivity)return;
        window(host.getWindow(),editor(host)?R.style.MotionPage:R.style.MotionNavigation);
    }
}
