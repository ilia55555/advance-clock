package com.ilia.advanceclock;
import android.app.*;
import android.content.Context;
final class MotionDialogs {
    static class Builder extends AlertDialog.Builder {
        Builder(Context c){super(c);}
        @Override public AlertDialog create(){AlertDialog dialog=super.create();KeyboardAvoidance.install(dialog.getWindow());AppMotion.window(dialog.getWindow(),R.style.MotionDialog);return dialog;}
    }
    /** Wheel time picker; tap a value to type it. */
    static class TimePicker extends TimeEntryDialog {
        TimePicker(Context c,TimePickerDialog.OnTimeSetListener listener,int hour,int minute,boolean full){super(c,listener,hour,minute,full);AppMotion.window(getWindow(),R.style.MotionPicker);}
    }
}
