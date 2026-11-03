package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;

public final class AlarmPhotoCropActivity extends Activity {
    private AlarmPhotoView photo;
    private int width, height;
    private Button save, cancel;
    private SaveJob job;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); super.onCreate(state);
        if (android.os.Build.VERSION.SDK_INT >= 33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> { if (job == null) finish(); });
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
        FrameLayout area = new FrameLayout(this);
        root.addView(area, new LinearLayout.LayoutParams(-1, 0, 1));
        photo = new AlarmPhotoView(this, true); area.addView(photo);
        area.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            int w = Math.min(area.getWidth(), (int) ((long) area.getHeight() * width / height));
            photo.setLayoutParams(new FrameLayout.LayoutParams(w, (int) ((long) w * height / width), android.view.Gravity.CENTER));
        });
        LinearLayout actions=new LinearLayout(this);
        actions.setLayoutDirection(AppSettings.layoutDirection(this));
        actions.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(-1,dp(52));
        actionParams.topMargin=dp(16);root.addView(actions,actionParams);
        save=actionButton(R.string.tool_save,true);
        cancel=actionButton(android.R.string.cancel,false);
        LinearLayout.LayoutParams saveParams=new LinearLayout.LayoutParams(0,-1,1);
        saveParams.setMarginEnd(dp(10));actions.addView(save,saveParams);
        actions.addView(cancel,new LinearLayout.LayoutParams(0,-1,1));
        cancel.setOnClickListener(v -> finish());
        save.setOnClickListener(v -> savePhoto());
        setContentView(root); AppSettings.applyFullscreenInsets(root);
        photo.load(getIntent().getStringExtra("uri"));
        if (state != null) photo.restoreFraming(state.getFloatArray("frame"));
        job = (SaveJob) getLastNonConfigurationInstance();
        if (job != null) { job.owner = this; save.setEnabled(false); cancel.setEnabled(false); }
    }

    private Button actionButton(int title,boolean filled) {
        Button button=new Button(this);button.setText(title);button.setAllCaps(false);
        button.setTextSize(16);button.setSingleLine(true);button.setMinWidth(0);
        button.setPadding(dp(12),0,dp(12),0);button.setElevation(0);button.setStateListAnimator(null);
        int accent=AppSettings.primaryColor(this);
        button.setTextColor(filled ? (android.graphics.Color.luminance(accent)>.179f?android.graphics.Color.BLACK:android.graphics.Color.WHITE) : accent);
        android.graphics.drawable.GradientDrawable shape=new android.graphics.drawable.GradientDrawable();
        shape.setCornerRadius(dp(14));shape.setColor(filled?accent:android.graphics.Color.TRANSPARENT);
        shape.setStroke(dp(1),accent);
        android.graphics.drawable.GradientDrawable mask=new android.graphics.drawable.GradientDrawable();
        mask.setColor(android.graphics.Color.WHITE);mask.setCornerRadius(dp(14));
        button.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf((accent & 0x00FFFFFF)|0x22000000),shape,mask));
        return button;
    }

    private void savePhoto() {
        Bitmap bitmap;
        try { bitmap = photo.export(width, height); } catch (OutOfMemoryError error) { bitmap = null; }
        if (bitmap == null) { error(); return; }
        save.setEnabled(false); cancel.setEnabled(false);
        job = new SaveJob(); job.owner = this;
        job.start(getApplicationContext(), bitmap);
    }
    @Override protected void onResume() {
        super.onResume();
        if (job != null && job.finished) finishSave();
    }
    private void finishSave() {
        if (isFinishing() || isDestroyed() || isChangingConfigurations() || job == null || !job.finished) return;
        Uri output = job.output; job.owner = null; job = null;
        if (output != null) { setResult(RESULT_OK, new Intent().setData(output)); finish(); }
        else { save.setEnabled(true); cancel.setEnabled(true); error(); }
    }
    @Override public Object onRetainNonConfigurationInstance() { return job; }
    @Override protected void onSaveInstanceState(Bundle out) {
        out.putFloatArray("frame", photo.framing()); super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        if (job != null && job.owner == this) job.owner = null;
        super.onDestroy();
    }
    // Legacy devices use this path; Android 13+ uses the callback registered above.
    @android.annotation.SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() { if (job == null) super.onBackPressed(); }
    private void error() { LogoToast.makeText(this, R.string.media_image_error, Toast.LENGTH_SHORT).show(); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    /** Retains only the save operation across rotation; the worker uses application context. */
    private static final class SaveJob {
        AlarmPhotoCropActivity owner;
        boolean finished;
        Uri output;
        void start(Context context, Bitmap bitmap) {
            new Thread(() -> {
                Uri target = null;
                try {
                    target = AlarmMediaProvider.create(context);
                    try (java.io.OutputStream stream = context.getContentResolver().openOutputStream(target)) {
                        if (stream == null || !bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream))
                            throw new java.io.IOException("Cannot save photo");
                    }
                } catch (Exception | OutOfMemoryError ignored) {
                    if (target != null) AlarmMediaProvider.discard(context, target.toString());
                    target = null;
                } finally { bitmap.recycle(); }
                Uri result = target;
                new Handler(Looper.getMainLooper()).post(() -> {
                    output = result; finished = true;
                    if (owner != null) owner.finishSave();
                });
            }, "alarm-crop-save").start();
        }
    }
}
