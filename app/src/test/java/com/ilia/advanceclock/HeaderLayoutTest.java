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
@Config(application=Application.class, sdk={28,35})
public class HeaderLayoutTest {
    @Test public void persianDateFitsAboveTabsEvenWithLargeSystemText() {
        Application app=RuntimeEnvironment.getApplication();
        AppString.init(app);
        for (float scale : new float[]{1f,1.5f,2f}) {
            Configuration config=new Configuration(app.getResources().getConfiguration());
            config.fontScale=scale;
            config.setLocale(new java.util.Locale("fa"));
            Context context=new ContextThemeWrapper(app.createConfigurationContext(config),
                    R.style.Theme_AdvanceClock_Base_Light);
            ViewGroup root=(ViewGroup)LayoutInflater.from(context).inflate(R.layout.activity_main,null);
            TextView date=root.findViewById(R.id.header_date);
            date.setText("۳۰ مهر ۱۴۰۵");
            TextView time=root.findViewById(R.id.header_time);
            time.setText("۱۱:۲۵");
            int width=Math.round(360*context.getResources().getDisplayMetrics().density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(2000,View.MeasureSpec.EXACTLY));
            root.layout(0,0,width,2000);
            View textBlock=(View)date.getParent();
            View clockRow=(View)textBlock.getParent();
            View tabs=root.findViewById(R.id.tab_bar);
            assertTrue("date clipped at font scale "+scale,
                    date.getBottom()<=textBlock.getHeight());
            assertTrue(textBlock.getBottom()<=clockRow.getHeight());
            assertTrue("date row overlaps tabs",clockRow.getBottom()<=tabs.getTop());
            assertTrue("clock overlaps date",time.getBottom()<=date.getTop());
        }
    }
}
