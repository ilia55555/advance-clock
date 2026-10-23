package com.ilia.advanceclock;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.CopyOnWriteArraySet;

/** In-process updates keep visible controls in sync with hardware and playback changes. */
final class AlertOutputEvents {
    private static final CopyOnWriteArraySet<Runnable> listeners=new CopyOnWriteArraySet<>();
    static void add(Runnable listener){listeners.add(listener);}
    static void remove(Runnable listener){listeners.remove(listener);}
    static void changed(){
        if(Looper.myLooper()!=Looper.getMainLooper()){
            new Handler(Looper.getMainLooper()).post(AlertOutputEvents::changed);
            return;
        }
        for(Runnable listener:listeners)listener.run();
    }
}
