package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public final class StopwatchActivity extends Activity {
    private StopwatchPanelController controller;
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        super.onCreate(state);
        setContentView(R.layout.activity_stopwatch);
        View root = findViewById(android.R.id.content);
        controller = new StopwatchPanelController(this, root);
        View back = findViewById(R.id.back_button);
        ((View) back.getParent()).setVisibility(View.VISIBLE);
        back.setOnClickListener(v -> finish());
        controller.setActive(true);
    }
    @Override protected void onResume() { super.onResume(); controller.onResume(); }
    @Override protected void onPause() { controller.onPause(); super.onPause(); }
}
