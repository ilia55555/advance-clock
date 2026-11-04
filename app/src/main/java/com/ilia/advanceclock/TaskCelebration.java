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
        ValueAnimator animator=ValueAnimator.ofFloat(done?0:1,done?1:0);animator.setDuration(260);
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
        final int[] colors={0xFF26A69A,0xFFFFCA28,0xFFEF5350,0xFF42A5F5,0xFFAB47BC};
        Drawable particles=new Drawable(){
            final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);final float density=card.getResources().getDisplayMetrics().density;
            @Override public void draw(Canvas canvas){float t=getLevel()/10000f;paint.setAlpha((int)(255*(1-t)));
                for(int i=0;i<28;i++){double angle=(i*2.399963);float speed=(50+i%7*13)*density;
                    float px=x+(float)Math.cos(angle)*speed*t,py=y+(float)Math.sin(angle)*speed*t+100*density*t*t;
                    paint.setColor(colors[i%colors.length]);paint.setAlpha((int)(255*(1-t)));canvas.drawCircle(px,py,(2+i%3)*density,paint);
                }
            }
            @Override protected boolean onLevelChange(int n){invalidateSelf();return true;}
            public void setAlpha(int a){} public void setColorFilter(ColorFilter f){} public int getOpacity(){return PixelFormat.TRANSLUCENT;}
        };
        particles.setBounds(0,0,root.getWidth(),root.getHeight());root.getOverlay().add(particles);
        ValueAnimator animator=ValueAnimator.ofInt(0,10000);animator.setDuration(850);animator.addUpdateListener(a->particles.setLevel((int)a.getAnimatedValue()));
        View.OnAttachStateChangeListener detach=new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){}public void onViewDetachedFromWindow(View v){animator.cancel();}};card.addOnAttachStateChangeListener(detach);
        animator.addListener(new AnimatorListenerAdapter(){public void onAnimationEnd(Animator a){root.getOverlay().remove(particles);card.removeOnAttachStateChangeListener(detach);card.setTag(R.id.task_celebration_tag,null);}});card.setTag(R.id.task_celebration_tag,animator);animator.start();
    }
}
