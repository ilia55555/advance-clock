package com.ilia.advanceclock;

import android.animation.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.view.*;
import android.widget.TextView;

/** Short, bounded effects. No timers, bitmap allocations or work after detachment. */
final class TaskCelebration {
    static void strike(TextView text,boolean done,boolean animate){
        if(text.getTag(R.id.task_animation_tag) instanceof ValueAnimator)((ValueAnimator)text.getTag(R.id.task_animation_tag)).cancel();
        text.setPaintFlags(text.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
        if(!animate || !ValueAnimator.areAnimatorsEnabled()){
            text.setAlpha(done?.55f:1f);if(done)text.setPaintFlags(text.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG);return;
        }
        Drawable line=new Drawable(){
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override public void draw(Canvas canvas){
                Layout layout=text.getLayout();if(layout==null)return;
                float progress=(Integer)getLevel()/10000f;paint.setColor(text.getCurrentTextColor());paint.setStrokeWidth(text.getResources().getDisplayMetrics().density*1.5f);
                for(int i=0;i<layout.getLineCount();i++){
                    float left=layout.getLineLeft(i)+text.getTotalPaddingLeft(),right=layout.getLineRight(i)+text.getTotalPaddingLeft();
                    float y=layout.getLineBaseline(i)+text.getTotalPaddingTop()+text.getPaint().ascent()*.32f;
                    if(layout.getParagraphDirection(i)<0)canvas.drawLine(right,y,right-(right-left)*progress,y,paint);
                    else canvas.drawLine(left,y,left+(right-left)*progress,y,paint);
                }
            }
            @Override protected boolean onLevelChange(int level){invalidateSelf();return true;}
            public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        };
        line.setBounds(0,0,text.getWidth(),text.getHeight());text.getOverlay().add(line);
        ValueAnimator animator=ValueAnimator.ofFloat(done?0:1,done?1:0);animator.setDuration(220);
        animator.addUpdateListener(a->{float value=(float)a.getAnimatedValue();line.setLevel((int)(value*10000));text.setAlpha(1-.45f*value);});
        View.OnAttachStateChangeListener detach=new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){animator.cancel();}};
        text.addOnAttachStateChangeListener(detach);text.setTag(R.id.task_animation_tag,animator);
        animator.addListener(new AnimatorListenerAdapter(){@Override public void onAnimationEnd(Animator a){text.getOverlay().remove(line);text.removeOnAttachStateChangeListener(detach);text.setTag(R.id.task_animation_tag,null);text.setPaintFlags(done?text.getPaintFlags()|Paint.STRIKE_THRU_TEXT_FLAG:text.getPaintFlags()&~Paint.STRIKE_THRU_TEXT_FLAG);}});animator.start();
    }
    static final class Burst extends Drawable {
        static final int COUNT=64;
        final boolean fireworks;
        final float[] vx=new float[COUNT],vy=new float[COUNT],spin=new float[COUNT],offsetX=new float[COUNT],offsetY=new float[COUNT];
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        final float density,x,y;
        final int[] colors;
        Burst(float density,float x,float y,float width,boolean fireworks,java.util.Random random){
            this.density=density;this.x=x;this.y=y;this.fireworks=fireworks;
            colors=fireworks?new int[]{0xFFFFCB54,0xFFFF779F,0xFF75CFFF,0xFFA5EDAC,0xFFD7ACFF}
                    :new int[]{0xFF45B8A8,0xFFF7C65D,0xFFEF8FA1,0xFF6FAFE5,0xFFB19ADD};
            for(int i=0;i<COUNT;i++){
                double angle=random.nextDouble()*Math.PI*2;
                // Mix slower core particles with the outer burst: no fixed-radius empty oval.
                float reach=(i%8==0?random.nextFloat()*30:(float)Math.sqrt(random.nextFloat())*260)*density;
                vx[i]=(float)Math.cos(angle)*reach;
                vy[i]=(float)Math.sin(angle)*reach*(fireworks?1f:.75f)-(fireworks?0:90*density);
                float radius=(i%8==0?random.nextFloat()*.12f:(float)Math.sqrt(random.nextFloat()));
                offsetX[i]=fireworks?0:(float)Math.cos(angle)*Math.min(width*.15f,50*density)*radius;
                offsetY[i]=fireworks?0:(float)Math.sin(angle)*12*density*radius;
                spin[i]=(random.nextBoolean()?1:-1)*(180+random.nextFloat()*180);
            }
        }
        @Override public void draw(Canvas canvas){
            float t=getLevel()/10000f;
            float opacity=Math.min(1f,t/.05f)*Math.min(1f,(1-t)/.4f);
            float travel=fireworks?t*(2-t):t;
            for(int i=0;i<COUNT;i++){
                float px=x+offsetX[i]+vx[i]*travel;
                float py=y+offsetY[i]+vy[i]*travel+(fireworks?65:220)*density*t*t;
                float size=(fireworks?1.3f+i%3*.35f:2.5f+i%3*.7f)*density;
                paint.setColor(colors[i%colors.length]);
                if(fireworks){
                    // Crisp sparks and short glowing trails, without bitmap blur or shadows.
                    paint.setAlpha((int)(32*opacity));canvas.drawCircle(px,py,size*3,paint);
                    float previous=Math.max(0,travel-.09f);
                    paint.setAlpha((int)(190*opacity));paint.setStrokeWidth(size*.8f);
                    canvas.drawLine(x+vx[i]*previous,y+vy[i]*previous+65*density*t*t,px,py,paint);
                    paint.setAlpha((int)(250*opacity));canvas.drawCircle(px,py,size,paint);
                }else{
                    px+=(float)Math.sin(t*4+i)*5*density*t;
                    paint.setAlpha((int)(230*opacity));int saved=canvas.save();canvas.rotate(i*23+spin[i]*t,px,py);
                    if(i%4==0)canvas.drawCircle(px,py,size*.8f,paint);
                    else canvas.drawRoundRect(px-size,py-size*.55f,px+size,py+size*.55f,density,density,paint);
                    canvas.restoreToCount(saved);
                }
            }
        }
        @Override protected boolean onLevelChange(int n){invalidateSelf();return true;}
        public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    static void celebrate(View card){
        if(!ValueAnimator.areAnimatorsEnabled() || !(card.getRootView() instanceof ViewGroup))return;
        if(card.getTag(R.id.task_celebration_tag) instanceof ValueAnimator)((ValueAnimator)card.getTag(R.id.task_celebration_tag)).cancel();
        ViewGroup root=(ViewGroup)card.getRootView();int[] origin=new int[2],base=new int[2];card.getLocationInWindow(origin);root.getLocationInWindow(base);
        float x=origin[0]-base[0]+card.getWidth()/2f,y=origin[1]-base[1]+Math.min(card.getHeight()/2f,80*card.getResources().getDisplayMetrics().density);
        java.util.Random random=java.util.concurrent.ThreadLocalRandom.current();
        Drawable particles=new Burst(card.getResources().getDisplayMetrics().density,x,y,card.getWidth(),random.nextBoolean(),random);
        particles.setBounds(0,0,root.getWidth(),root.getHeight());root.getOverlay().add(particles);
        ValueAnimator animator=ValueAnimator.ofInt(0,10000);animator.setDuration(1200);animator.setInterpolator(new android.view.animation.LinearInterpolator());animator.addUpdateListener(a->particles.setLevel((int)a.getAnimatedValue()));
        View.OnAttachStateChangeListener detach=new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){animator.cancel();}};card.addOnAttachStateChangeListener(detach);
        animator.addListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator a){root.getOverlay().remove(particles);card.removeOnAttachStateChangeListener(detach);card.setTag(R.id.task_celebration_tag,null);}});card.setTag(R.id.task_celebration_tag,animator);animator.start();
    }
}
