package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Calendar;

/** A theme-aware clock face; no launcher artwork or image assets. */
final class AlarmAnalogView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    AlarmAnalogView(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        boolean dark=AppSettings.themeMode(getContext())==AppSettings.THEME_DARK;
        int ink=dark?0xFFF1F4F7:0xFF17191C;
        float size=Math.min(getWidth(),getHeight()), radius=size*.46f;
        canvas.save();canvas.translate(getWidth()/2f,getHeight()/2f);
        paint.setColor(dark?0xFF252A31:Color.WHITE);paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(size*.025f,0,size*.01f,dark?0x66000000:0x22000000);
        canvas.drawCircle(0,0,radius,paint);
        paint.clearShadowLayer();
        paint.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<60;i++) {
            canvas.save();canvas.rotate(i*6);
            paint.setColor(i%5==0?ink:dark?0xFF555C65:0xFFDADDE1);
            paint.setStrokeWidth(size*(i%5==0?.008f:.005f));
            canvas.drawLine(0,-radius*.91f,0,-radius*(i%5==0?.86f:.88f),paint);
            canvas.restore();
        }
        paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextSize(size*.11f);
        paint.setTextAlign(Paint.Align.CENTER);paint.setColor(ink);
        for(int i=1;i<=12;i++) {
            double angle=Math.toRadians(i*30-90);
            float x=(float)Math.cos(angle)*radius*.70f,y=(float)Math.sin(angle)*radius*.70f;
            canvas.drawText(CalendarUtils.fa(i),x,y-(paint.ascent()+paint.descent())/2,paint);
        }
        Calendar now=Calendar.getInstance();
        float seconds=now.get(Calendar.SECOND), minutes=now.get(Calendar.MINUTE)+seconds/60f;
        hand(canvas,(now.get(Calendar.HOUR)+minutes/60f)*30,radius*.49f,size*.022f,ink);
        hand(canvas,minutes*6,radius*.77f,size*.016f,ink);
        hand(canvas,seconds*6,radius*.87f,size*.007f,AppSettings.primaryColor(getContext()));
        paint.setColor(AppSettings.primaryColor(getContext()));canvas.drawCircle(0,0,size*.021f,paint);
        paint.setColor(dark?0xFF252A31:Color.WHITE);canvas.drawCircle(0,0,size*.009f,paint);
        canvas.restore();
    }
    private void hand(Canvas canvas,float angle,float length,float width,int color) {
        canvas.save();canvas.rotate(angle);paint.setColor(color);paint.setStrokeWidth(width);
        canvas.drawLine(0,length*.13f,0,-length,paint);canvas.restore();
    }
}
