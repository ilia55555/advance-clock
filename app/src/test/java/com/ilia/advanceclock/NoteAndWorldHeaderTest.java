package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import android.graphics.Rect;
import android.view.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35},shadows=ReminderScreenFrameworkTest.PlainStore.class)
public class NoteAndWorldHeaderTest {
    @Before public void init(){AppString.init(RuntimeEnvironment.getApplication());}
    private void checkHeader(View root,View header){
        WindowInsets insets=android.os.Build.VERSION.SDK_INT>=29?
                new WindowInsets.Builder().setSystemWindowInsets(android.graphics.Insets.of(0,30,0,20)).build():
                ReflectionHelpers.callConstructor(WindowInsets.class,ReflectionHelpers.ClassParameter.from(Rect.class,new Rect(0,30,0,20)));
        root.dispatchApplyWindowInsets(insets);
        root.measure(View.MeasureSpec.makeMeasureSpec(400,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY));
        root.layout(0,0,400,1000);
        assertEquals(0,root.getPaddingTop());assertEquals(0,root.getPaddingLeft());
        assertEquals(0,header.getTop());assertEquals(0,header.getLeft());assertEquals(root.getWidth(),header.getWidth());
        assertTrue(header.getPaddingTop()>=30);assertTrue(header.getHeight()>header.getPaddingTop()+header.getPaddingBottom());
    }
    @Test @org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
    public void themedHeaderPaintsSquareTopAndRoundedBottomCorners(){
        try(var controller=Robolectric.buildActivity(NoForgetEditorActivity.class).setup()){
            NoForgetEditorActivity host=controller.get();
            android.graphics.drawable.Drawable shape=host.findViewById(R.id.note_editor_header).getBackground();
            android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(200,100,android.graphics.Bitmap.Config.ARGB_8888);
            shape.setBounds(0,0,200,100);shape.draw(new android.graphics.Canvas(bitmap));
            int theme=AppSettings.primaryColor(host);
            assertEquals(theme,bitmap.getPixel(1,1));assertEquals(theme,bitmap.getPixel(198,1));
            assertEquals(0,android.graphics.Color.alpha(bitmap.getPixel(1,98)));
            assertEquals(0,android.graphics.Color.alpha(bitmap.getPixel(198,98)));
            assertEquals(theme,bitmap.getPixel(100,98));
        }
    }
    @Test public void noteHeaderStartsAtSheetEdgeAndKeepsTitleBelowSystemBar(){
        Application app=RuntimeEnvironment.getApplication();
        try(var controller=Robolectric.buildActivity(NoForgetEditorActivity.class,new Intent(app,NoForgetEditorActivity.class).putExtra("modalCreate",true)).setup().visible()){
            checkHeader(controller.get().findViewById(R.id.note_editor_root),controller.get().findViewById(R.id.note_editor_header));
        }
    }
    @Test public void worldAddDialogHasNoOuterGapAroundHeader(){
        try(var controller=Robolectric.buildActivity(MainActivity.class).setup().visible()){
            MainActivity host=controller.get();host.findViewById(R.id.tab_world).performClick();
            WorldClockPanelController world=ReflectionHelpers.getField(host,"worldController");
            ReflectionHelpers.setField(world,"indexReady",true);
            host.findViewById(R.id.world_fab).performClick();
            AlertDialog dialog=org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            View content=dialog.getWindow().getDecorView().findViewWithTag("world_add_content");
            assertNotNull(content);checkHeader(content,content.findViewWithTag("world_add_header"));
            assertEquals(android.R.id.content,((View)content.getParent()).getId());
            assertEquals(0,((View)content.getParent()).getPaddingTop());
            assertEquals(0,dialog.getWindow().getDecorView().getPaddingTop());dialog.dismiss();
        }
    }
}
