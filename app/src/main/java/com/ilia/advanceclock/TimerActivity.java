package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public final class TimerActivity extends Activity {
    private TimerPanelController controller;
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        super.onCreate(state);
        setContentView(R.layout.activity_timer);
        View root = findViewById(android.R.id.content);
        controller = new TimerPanelController(this, root);
        View back = findViewById(R.id.back_button);
        ((View) back.getParent()).setVisibility(View.VISIBLE);
        back.setOnClickListener(v -> finish());
        controller.setActive(true);
    }
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        KeyboardDismiss.onTouch(this, event);
        return super.dispatchTouchEvent(event);
    }
    @Override protected void onResume() { super.onResume(); controller.onResume(); }
    @Override protected void onPause() { controller.onPause(); super.onPause(); }
}
