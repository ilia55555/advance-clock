package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;

public final class AlarmPhotoCropActivity extends Activity {
    private AlarmPhotoView photo;
    private int width, height;
    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); super.onCreate(state);
        Point size = new Point(); getWindowManager().getDefaultDisplay().getRealSize(size);
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.graphics.Rect bounds = getWindowManager().getCurrentWindowMetrics().getBounds();
            size.set(bounds.width(), bounds.height());
        }
        width = Math.max(1, size.x);
        height = AlarmPhotoLayout.height(size.y, getIntent().getIntExtra("count", 1));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(AppSettings.background(this)); root.setPadding(dp(20), dp(24), dp(20), dp(32));
        TextView help = new TextView(this); help.setText(R.string.media_crop_hint); help.setTextColor(AppSettings.textPrimary(this)); root.addView(help);
        android.widget.FrameLayout area = new android.widget.FrameLayout(this);
        root.addView(area, new LinearLayout.LayoutParams(-1, 0, 1));
        photo = new AlarmPhotoView(this, true); area.addView(photo);
        area.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            int w = Math.min(area.getWidth(), (int) ((long) area.getHeight() * width / height));
            android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(w, (int) ((long) w * height / width), android.view.Gravity.CENTER);
            photo.setLayoutParams(lp);
        });
        Button save = new Button(this); save.setText(R.string.tool_save); root.addView(save);
        Button cancel = new Button(this); cancel.setText(android.R.string.cancel); root.addView(cancel);
        cancel.setOnClickListener(v -> finish());
        save.setOnClickListener(v -> {
            Bitmap bitmap;
            try { bitmap = photo.export(width, height); } catch (OutOfMemoryError error) { bitmap = null; }
            if (bitmap == null) { Toast.makeText(this, R.string.media_image_error, Toast.LENGTH_SHORT).show(); return; }
            save.setEnabled(false);
            Bitmap result = bitmap;
            new Thread(() -> {
                Uri output = null;
                try {
                    output = AlarmMediaProvider.create(this);
                    try (java.io.OutputStream stream = getContentResolver().openOutputStream(output)) { result.compress(Bitmap.CompressFormat.JPEG, 95, stream); }
                } catch (Exception ignored) { output = null; }
                result.recycle(); Uri done = output;
                runOnUiThread(() -> {
                    if (done != null) { setResult(RESULT_OK, new Intent().setData(done)); finish(); }
                    else { save.setEnabled(true); Toast.makeText(this, R.string.media_image_error, Toast.LENGTH_SHORT).show(); }
                });
            }, "alarm-crop-save").start();
        });
        setContentView(root); AppSettings.applyFullscreenInsets(root);
        photo.load(getIntent().getStringExtra("uri"));
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
}
