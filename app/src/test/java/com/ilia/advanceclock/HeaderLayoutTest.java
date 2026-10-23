package com.ilia.advanceclock;

import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
@Config(application=Application.class, sdk={28,35})
public class HeaderLayoutTest {
    @Test public void persianDateFitsAboveTabsEvenWithLargeSystemText() {
        Application app=RuntimeEnvironment.getApplication();
        AppString.init(app);
        for (float scale : new float[]{.85f,1f,1.5f,2f}) {
            Configuration config=new Configuration(app.getResources().getConfiguration());
            config.fontScale=scale;
            config.setLocale(new java.util.Locale("fa"));
            Context context=new ContextThemeWrapper(AppSettings.localizedContext(app.createConfigurationContext(config)),
                    R.style.Theme_AdvanceClock_Base_Light);
            assertEquals(1f,context.getResources().getConfiguration().fontScale,0f);
            ViewGroup root=(ViewGroup)LayoutInflater.from(context).inflate(R.layout.activity_main,null);
            TextView date=root.findViewById(R.id.header_date);
            date.setText("۳۰ مهر ۱۴۰۵");
            TextView time=root.findViewById(R.id.header_time);
            time.setText("۱۱:۲۵");
            assertTrue(date.getTextSize()<time.getTextSize());
            assertTrue(date.getTextSize()>=time.getTextSize()*.6f);
            int width=Math.round(360*context.getResources().getDisplayMetrics().density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(2000,View.MeasureSpec.EXACTLY));
            root.layout(0,0,width,2000);
            for (int tabId : new int[]{R.id.tab_clock,R.id.tab_noforget,R.id.tab_stopwatch,R.id.tab_timer,R.id.tab_world}) {
                TextView tab=root.findViewById(tabId);
                assertEquals("tab wrapped at font scale "+scale,1,tab.getLineCount());
                assertEquals(16f*context.getResources().getDisplayMetrics().density,tab.getTextSize(),.1f);
            }
            View clockRow=(View)time.getParent();
            View tabs=root.findViewById(R.id.tab_bar);
            assertTrue("date clipped at font scale "+scale,
                    date.getHeight()>=date.getLayout().getHeight()+date.getCompoundPaddingTop()+date.getCompoundPaddingBottom());
            assertTrue("date overlaps tabs",date.getBottom()<=tabs.getTop());
            android.graphics.Rect clockInk=new android.graphics.Rect(),dateInk=new android.graphics.Rect();
            time.getPaint().getTextBounds(time.getText().toString(),0,time.length(),clockInk);
            date.getPaint().getTextBounds(date.getText().toString(),0,date.length(),dateInk);
            int clockBottom=clockRow.getTop()+time.getTop()+time.getBaseline()+clockInk.bottom;
            int dateTop=date.getTop()+date.getBaseline()+dateInk.top;
            assertTrue("clock and date glyphs overlap at scale "+scale,clockBottom<dateTop);
            View menu=root.findViewById(R.id.header_menu), theme=root.findViewById(R.id.theme_toggle);
            assertEquals(time.getTop()+time.getHeight()/2f,menu.getTop()+menu.getHeight()/2f,.5f);
            assertEquals(time.getTop()+time.getHeight()/2f,theme.getTop()+theme.getHeight()/2f,.5f);
        }
    }
}
