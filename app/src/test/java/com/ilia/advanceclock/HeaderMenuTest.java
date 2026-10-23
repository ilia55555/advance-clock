package com.ilia.advanceclock;
import android.app.*;
import android.view.*;
import android.widget.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(application=Application.class,sdk={28,35})
public class HeaderMenuTest {
 @Test public void selectedActionIsDeliveredAndMenuClosesInBothThemes() {
  AppString.init(RuntimeEnvironment.getApplication());
  for(int theme:new int[]{AppSettings.THEME_LIGHT,AppSettings.THEME_DARK}) {
   AppSettings.setThemeMode(RuntimeEnvironment.getApplication(),theme);
   try(var controller=Robolectric.buildActivity(Activity.class).setup().visible()) {
    Activity host=controller.get();AppSettings.applyTheme(host);
    Button anchor=new Button(host);host.setContentView(anchor);
    PopupMenu model=new PopupMenu(host,anchor);model.getMenu().add(0,1,0,"Settings");model.getMenu().add(0,9,1,"About");
    int[] clicked={0};
    PopupWindow menu=HeaderMenu.show(host,anchor,model.getMenu(),item->{clicked[0]=item.getItemId();return true;});
    assertTrue(menu.isShowing());assertTrue(menu.getElevation()>0);
    LinearLayout rows=(LinearLayout)((ScrollView)menu.getContentView()).getChildAt(0);
    assertEquals(2,rows.getChildCount());rows.getChildAt(1).performClick();
    assertEquals(9,clicked[0]);assertFalse(menu.isShowing());
   }
  }
 }
}
