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
    static void celebrate(View card){
        if(!ValueAnimator.areAnimatorsEnabled() || !(card.getRootView() instanceof ViewGroup))return;
        if(card.getTag(R.id.task_celebration_tag) instanceof ValueAnimator)((ValueAnimator)card.getTag(R.id.task_celebration_tag)).cancel();
        ViewGroup root=(ViewGroup)card.getRootView();int[] origin=new int[2],base=new int[2];card.getLocationInWindow(origin);root.getLocationInWindow(base);
        float x=origin[0]-base[0]+card.getWidth()/2f,y=origin[1]-base[1]+Math.min(card.getHeight()/2f,80*card.getResources().getDisplayMetrics().density);
        final int[] colors={0xFF45B8A8,0xFFF7C65D,0xFFEF8FA1,0xFF6FAFE5,0xFFB19ADD};
        Drawable particles=new Drawable(){
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final float density=card.getResources().getDisplayMetrics().density;
            final float[] vx=new float[64],vy=new float[64],spin=new float[64],offsetX=new float[64],offsetY=new float[64];
            {
                for(int i=0;i<64;i++){
                    double angle=i*2.399963;
                    float reach=(125+i%8*18)*density;
                    vx[i]=(float)Math.cos(angle)*reach;
                    vy[i]=(float)Math.sin(angle)*reach*.75f-90*density;
                    offsetX[i]=(float)Math.cos(angle)*Math.min(card.getWidth()*.15f,50*density);
                    offsetY[i]=(float)Math.sin(angle)*12*density;
                    spin[i]=(i%2==0?1:-1)*(180+i%5*45);
                }
            }
            @Override public void draw(Canvas canvas){
                float t=getLevel()/10000f;
                float opacity=Math.min(1f,t/.05f)*Math.min(1f,(1-t)/.4f);
                for(int i=0;i<64;i++){
                    float px=x+offsetX[i]+vx[i]*t+(float)Math.sin(t*4+i)*5*density*t;
                    float py=y+offsetY[i]+vy[i]*t+220*density*t*t;
                    float size=(2.5f+i%3*.7f)*density;
                    paint.setColor(colors[i%colors.length]);paint.setAlpha((int)(230*opacity));
                    int saved=canvas.save();canvas.rotate(i*23+spin[i]*t,px,py);
                    if(i%4==0)canvas.drawCircle(px,py,size*.8f,paint);
                    else canvas.drawRoundRect(px-size,py-size*.55f,px+size,py+size*.55f,density,density,paint);
                    canvas.restoreToCount(saved);
                }
            }
            @Override protected boolean onLevelChange(int n){invalidateSelf();return true;}
            public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        };
        particles.setBounds(0,0,root.getWidth(),root.getHeight());root.getOverlay().add(particles);
        ValueAnimator animator=ValueAnimator.ofInt(0,10000);animator.setDuration(1200);animator.setInterpolator(new android.view.animation.LinearInterpolator());animator.addUpdateListener(a->particles.setLevel((int)a.getAnimatedValue()));
        View.OnAttachStateChangeListener detach=new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){animator.cancel();}};card.addOnAttachStateChangeListener(detach);
        animator.addListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator a){root.getOverlay().remove(particles);card.removeOnAttachStateChangeListener(detach);card.setTag(R.id.task_celebration_tag,null);}});card.setTag(R.id.task_celebration_tag,animator);animator.start();
    }
}
