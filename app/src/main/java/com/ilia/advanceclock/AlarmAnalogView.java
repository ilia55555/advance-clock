package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.Calendar;
import java.util.TimeZone;

/** A theme-aware clock face; no launcher artwork or image assets. */
final class AlarmAnalogView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private long paletteMinute=Long.MIN_VALUE;
    private Palette palette;
    static final class Palette {
        final boolean night;
        final int face,ink,accent,ticks;
        Palette(boolean night,int accent,float warmth){
            this.night=night;this.accent=accent;
            face=night?Color.BLACK:blend(Color.WHITE,0xFFFFE4AD,warmth*.32f);
            ink=night?Color.WHITE:Color.BLACK;
            ticks=night?0xFF666666:0xFFDADDE1;
        }
    }
    static Palette paletteAt(Context context,long millis){
        boolean solar=AppSettings.prayerLocationSet(context);
        TimeZone zone=solar?AppSettings.prayerTimeZone(context):TimeZone.getDefault();
        Calendar date=Calendar.getInstance(zone);date.setTimeInMillis(millis);
        int minute=date.get(Calendar.HOUR_OF_DAY)*60+date.get(Calendar.MINUTE);
        int rise=360,set=1080;
        if(solar){
            PrayerTimeCalculator.Times times=PrayerTimeCalculator.calculate(millis,
                    AppSettings.prayerLatitude(context),AppSettings.prayerLongitude(context),zone);
            if(times.sunriseMinutes>=0 && times.sunsetMinutes>times.sunriseMinutes && times.sunsetMinutes<1440){
                rise=times.sunriseMinutes;set=times.sunsetMinutes;
            }
        }
        boolean night=minute<rise || minute>=set;
        float warmth=solar?Math.max(0f,1f-Math.min(Math.abs(minute-rise),Math.abs(minute-set))/60f):0f;
        int accent=solar?blend(0xFFFFB300,0xFFFF7A32,warmth):AppSettings.primaryColor(context);
        return new Palette(night,accent,warmth);
    }
    private static int blend(int from,int to,float amount){
        return Color.rgb(Math.round(Color.red(from)+(Color.red(to)-Color.red(from))*amount),
                Math.round(Color.green(from)+(Color.green(to)-Color.green(from))*amount),
                Math.round(Color.blue(from)+(Color.blue(to)-Color.blue(from))*amount));
    }
    AlarmAnalogView(Context context) { super(context); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long millis=System.currentTimeMillis();
        if(palette==null || paletteMinute!=millis/60000){palette=paletteAt(getContext(),millis);paletteMinute=millis/60000;}
        boolean dark=palette.night;
        int ink=palette.ink;
        float size=Math.min(getWidth(),getHeight()), radius=size*.46f;
        canvas.save();canvas.translate(getWidth()/2f,getHeight()/2f);
        paint.setColor(palette.face);paint.setStyle(Paint.Style.FILL);
        paint.setShadowLayer(size*.025f,0,size*.01f,dark?0x66000000:0x22000000);
        canvas.drawCircle(0,0,radius,paint);
        paint.clearShadowLayer();
        paint.setStrokeCap(Paint.Cap.ROUND);
        for(int i=0;i<60;i++) {
            canvas.save();canvas.rotate(i*6);
            paint.setColor(i%5==0?ink:palette.ticks);
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
        hand(canvas,seconds*6,radius*.87f,size*.007f,palette.accent);
        paint.setColor(palette.accent);canvas.drawCircle(0,0,size*.021f,paint);
        paint.setColor(palette.face);canvas.drawCircle(0,0,size*.009f,paint);
        canvas.restore();
    }
    private void hand(Canvas canvas,float angle,float length,float width,int color) {
        canvas.save();canvas.rotate(angle);paint.setColor(color);paint.setStrokeWidth(width);
        canvas.drawLine(0,length*.13f,0,-length,paint);canvas.restore();
    }
}
