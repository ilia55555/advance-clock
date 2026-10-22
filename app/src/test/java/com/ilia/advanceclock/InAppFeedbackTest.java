package com.ilia.advanceclock;
import android.app.*;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import java.time.Duration;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class InAppFeedbackTest {
 @Test public void lastsThreeSecondsAndDoesNotQueue() {
  try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
   Activity host=controller.get();
   LogoToast first=LogoToast.makeText(host,"first",0);first.show();
   View old=ReflectionHelpers.getField(first,"content");assertNotNull(old.getParent());
   LogoToast next=LogoToast.makeText(host,"next",1);next.show();assertNull(old.getParent());
   View card=ReflectionHelpers.getField(next,"content");
   Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(2999));
   assertNotNull(card.getParent());
   Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(Duration.ofMillis(1));
   assertNull(card.getParent());
  }
 }
 @Test public void touchesOutsideMessageStillReachTheScreen() {
  try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
   Activity host=controller.get();
   android.widget.Button button=new android.widget.Button(host);
   int[] clicks={0};button.setOnClickListener(v -> clicks[0]++);host.setContentView(button);
   LogoToast toast=LogoToast.makeText(host,"message",0);toast.show();
   View decor=host.getWindow().getDecorView();
   decor.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),
                 View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY));
   decor.layout(0,0,1080,1920);
   MotionEvent down=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,100,100,0);
   MotionEvent up=MotionEvent.obtain(0,10,MotionEvent.ACTION_UP,100,100,0);
   decor.dispatchTouchEvent(down);decor.dispatchTouchEvent(up);
   Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
   assertEquals(1,clicks[0]);down.recycle();up.recycle();toast.cancel();
  }
 }
 @Test public void dismissesInEveryDirectionAndWhenActivityPauses() {
  try(var controller=Robolectric.buildActivity(Activity.class).setup()) {
   for(int[] direction:new int[][]{{100,0},{-100,0},{0,100},{0,-100}}) {
    LogoToast toast=LogoToast.makeText(controller.get(),"message",0);toast.show();
    View card=ReflectionHelpers.getField(toast,"content");
    MotionEvent down=MotionEvent.obtain(0,0,MotionEvent.ACTION_DOWN,150,150,0);
    MotionEvent move=MotionEvent.obtain(0,10,MotionEvent.ACTION_MOVE,150+direction[0],150+direction[1],0);
    card.dispatchTouchEvent(down);card.dispatchTouchEvent(move);
    assertNull(card.getParent());down.recycle();move.recycle();
   }
   LogoToast toast=LogoToast.makeText(controller.get(),"message",0);toast.show();
   View card=ReflectionHelpers.getField(toast,"content");
   controller.pause();assertNull(card.getParent());
  }
 }
}
