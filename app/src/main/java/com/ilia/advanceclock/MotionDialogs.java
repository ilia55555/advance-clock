package com.ilia.advanceclock;
import android.app.*;
import android.content.Context;
final class MotionDialogs {
    static class Builder extends AlertDialog.Builder {
        Builder(Context c){super(c);}
        @Override public AlertDialog create(){AlertDialog dialog=super.create();KeyboardAvoidance.install(dialog.getWindow());AppMotion.window(dialog.getWindow(),R.style.MotionDialog);return dialog;}
    }
    static class TimePicker extends TimePickerDialog {
        TimePicker(Context c,OnTimeSetListener listener,int hour,int minute,boolean full){super(c,listener,hour,minute,full);AppMotion.window(getWindow(),R.style.MotionPicker);}
    }
}
