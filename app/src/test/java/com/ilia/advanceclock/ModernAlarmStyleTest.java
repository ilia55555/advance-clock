package com.ilia.advanceclock;
import android.app.*;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class ModernAlarmStyleTest {
 @Test public void newStylesRenderSelectedCalendarAndKeepExistingActions() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);
  for(int style:new int[]{3,4}) for(int calendar:new int[]{0,1,2}) {
   AppSettings.setAlarmScreenStyle(app,style);AppSettings.setDefaultCalendar(app,calendar);
   try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
    Activity host=controller.get();AppSettings.applyTheme(host);ReminderScreen.applyWindowTheme(host);
    ReminderScreen screen=new ReminderScreen(host,new ReminderScreen.Content(),()->{},()->{},m->{});
    host.setContentView(screen);
    screen.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));
    screen.layout(0,0,360,800);screen.getViewTreeObserver().dispatchOnGlobalLayout();
    TextView date=ReflectionHelpers.getField(screen,"dateLabel");
    TextView time=ReflectionHelpers.getField(screen,"timeLabel");
    assertEquals(CalendarUtils.formatDate(System.currentTimeMillis(),calendar),date.getText().toString());
    assertTrue(time.getText().length()>0);
    assertTrue(screen.getChildAt(0) instanceof SwipeAlarmSurface);
    if(style==3) {
        assertNotNull(ReflectionHelpers.getField(screen,"analogClock"));
        assertEquals(View.GONE,time.getVisibility());
        assertNull(time.getParent());
    }
   }
  }
 }
 @Test @Config(sdk=35) @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 public void digitalTimeActuallyPaintsVisibleDigits() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);AppSettings.setAlarmScreenStyle(app,4);
  try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
   Activity host=controller.get();AppSettings.applyTheme(host);
   ReminderScreen screen=new ReminderScreen(host,new ReminderScreen.Content(),()->{},null,m->{});
   host.setContentView(screen);
   for(int i=0;i<3;i++) {
    screen.measure(View.MeasureSpec.makeMeasureSpec(360,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));
    screen.layout(0,0,360,800);screen.getViewTreeObserver().dispatchOnGlobalLayout();
   }
   TextView time=ReflectionHelpers.getField(screen,"timeLabel");
   android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(time.getWidth(),time.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
   time.draw(new android.graphics.Canvas(bitmap));int painted=0;
   for(int y=0;y<bitmap.getHeight();y++)for(int x=0;x<bitmap.getWidth();x++)if(android.graphics.Color.alpha(bitmap.getPixel(x,y))>0)painted++;
   assertTrue("Digital clock rendered blank",painted>100);
   java.util.function.Consumer<Boolean> blur=ReflectionHelpers.getField(screen,"blurListener");
   blur.accept(true);
   assertTrue((host.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_BLUR_BEHIND)!=0);
   assertTrue(host.getWindow().getAttributes().getBlurBehindRadius()>0);
   blur.accept(false);
   assertEquals(0,host.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
  }
 }
 @Test public void glassEntryPointUsesSystemWallpaperWithoutRevealingPreviousApp() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);AppSettings.setAlarmScreenStyle(app,4);
  android.content.Intent intent=new android.content.Intent(app,AlarmRingActivity.class)
          .putExtra("toolKind",ToolAlarmScheduler.TIMER).putExtra("label","Alarm");
  try(var controller=Robolectric.buildActivity(AlarmRingActivity.class,intent).setup().visible()) {
   android.content.res.TypedArray attributes=controller.get().obtainStyledAttributes(new int[]{android.R.attr.windowIsTranslucent,android.R.attr.windowShowWallpaper});
   assertFalse(attributes.getBoolean(0,false));assertTrue(attributes.getBoolean(1,false));attributes.recycle();
   assertTrue((controller.get().getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)!=0);
  }
 }
 @Test @Config(sdk=35) @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
 public void swipeHintRemainsOneLineWithLargeFontsNearScreenBottom() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);AppSettings.setAlarmScreenStyle(app,4);
  float previous=RuntimeEnvironment.getFontScale();RuntimeEnvironment.setFontScale(2.5f);
  try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
   Activity host=controller.get();AppSettings.applyTheme(host);
   ReminderScreen screen=new ReminderScreen(host,new ReminderScreen.Content(),()->{},null,m->{});host.setContentView(screen);
   for(int i=0;i<3;i++) {
    screen.measure(View.MeasureSpec.makeMeasureSpec(320,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY));
    screen.layout(0,0,320,800);screen.getViewTreeObserver().dispatchOnGlobalLayout();
   }
   TextView hint=findText(screen,host.getString(R.string.alarm_swipe_stop));assertNotNull(hint);
   assertEquals(1,hint.getLineCount());assertEquals(0,hint.getLayout().getEllipsisCount(0));
   android.graphics.Rect rect=new android.graphics.Rect();hint.getDrawingRect(rect);screen.offsetDescendantRectToMyCoords(hint,rect);
   assertTrue("Swipe hint too far from bottom",screen.getHeight()-rect.bottom<=8);
  } finally {RuntimeEnvironment.setFontScale(previous);}
 }
 @Test public void timerShowsOnlyUserTitleOrSingleDefaultTitle() {
  Application app=RuntimeEnvironment.getApplication();AppString.init(app);
  for(String label:new String[]{"", "My timer"}) {
   android.content.Intent intent=new android.content.Intent(app,AlarmRingActivity.class).putExtra("toolKind",ToolAlarmScheduler.TIMER).putExtra("label",label);
   try(var controller=Robolectric.buildActivity(AlarmRingActivity.class,intent).setup().visible()) {
    android.view.ViewGroup root=controller.get().findViewById(android.R.id.content);
    assertNotNull(findText(root,label.isEmpty()?ToolAlarmScheduler.defaultLabel(ToolAlarmScheduler.TIMER):label));
    assertNull(findText(root,controller.get().getString(R.string.runtime_text_0584)));
   }
  }
 }
 private TextView findText(View view,String text) {
  if(view instanceof TextView && ((TextView)view).getText().toString().equals(text))return (TextView)view;
  if(view instanceof android.view.ViewGroup) {
   android.view.ViewGroup group=(android.view.ViewGroup)view;
   for(int i=0;i<group.getChildCount();i++) {TextView found=findText(group.getChildAt(i),text);if(found!=null)return found;}
  }
  return null;
 }
 @Test public void smallSwipeSpringsBackAndTwentyPercentSwipeStops() {
  try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
   int[] stopped={0};SwipeAlarmSurface surface=new SwipeAlarmSurface(controller.get(),()->stopped[0]++);
   controller.get().setContentView(surface);surface.layout(0,0,400,1000);
   gesture(surface,800,650); // 15% is intentionally insufficient.
   Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));
   assertEquals(0,stopped[0]);assertEquals(0,surface.getTranslationY(),.1f);
   gesture(surface,800,580);
   Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(1));
   assertEquals(1,stopped[0]);
  }
 }
 private void gesture(SwipeAlarmSurface surface,float from,float to) {
  int[] actions={MotionEvent.ACTION_DOWN,MotionEvent.ACTION_MOVE,MotionEvent.ACTION_UP};
  for(int action:actions) {
   MotionEvent event=MotionEvent.obtain(0,0,action,200,action==MotionEvent.ACTION_DOWN?from:to,0);
   surface.onTouchEvent(event);event.recycle();
  }
 }
}
