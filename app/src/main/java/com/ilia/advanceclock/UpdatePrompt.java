package com.ilia.advanceclock;

import android.app.*;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;
import java.util.Locale;

/** Optional bottom sheet; once per release automatically, always available from the menu. */
final class UpdatePrompt {
    private static WeakReference<Dialog> current = new WeakReference<>(null);
    private static WeakReference<Activity> owner = new WeakReference<>(null);
    static void dismiss(Activity host) {
        Dialog dialog = current.get();
        if (owner.get() == host && dialog != null) dialog.dismiss();
    }
    static void show(Activity host, AppUpdates.Release release, boolean manual) {
        if (release == null || host.isFinishing() || host.isDestroyed()) return;
        if (!manual && AppUpdates.prefs(host).getLong("shown_code", 0) == release.code) return;
        Dialog existing = current.get(); if (existing != null) existing.dismiss();
        Dialog dialog = new Dialog(host); dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        LinearLayout root = new LinearLayout(host); root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(AppSettings.layoutDirection(host)); root.setPadding(dp(host,20),dp(host,12),dp(host,20),dp(host,20));
        GradientDrawable background = new GradientDrawable(); background.setColor(AppSettings.surface(host));
        float r = dp(host,24); background.setCornerRadii(new float[]{r,r,r,r,0,0,0,0});
        background.setStroke(dp(host,1),AppSettings.primaryColor(host)); root.setBackground(background);
        LinearLayout header = new LinearLayout(host); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(host, host.getString(R.string.update_available), 20); title.setTypeface(null,Typeface.BOLD);
        title.setTag("update_prompt_title"); header.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        ImageButton close = new ImageButton(host); close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(host)); close.setBackgroundColor(Color.TRANSPARENT);
        close.setContentDescription(host.getString(R.string.runtime_text_0002)); close.setOnClickListener(v->dialog.dismiss());
        header.addView(close,new LinearLayout.LayoutParams(dp(host,44),dp(host,44))); root.addView(header);
        String language = Locale.forLanguageTag(AppSettings.language(host)).getLanguage();
        String message = release.text("title",language);
        if (!message.isEmpty()) root.addView(text(host,message,16));
        TextView version = text(host,release.version,14); root.addView(version);
        String notes = release.text("releaseNotes",language);
        if (!notes.isEmpty()) {
            root.addView(text(host,host.getString(R.string.update_whats_new),16));
            TextView body = text(host,notes,15); body.setTag("update_release_notes");
            ScrollView scroll = new ScrollView(host) {
                @Override protected void onMeasure(int width, int height) {
                    super.onMeasure(width, View.MeasureSpec.makeMeasureSpec(dp(host,220),View.MeasureSpec.AT_MOST));
                }
            };
            scroll.addView(body);
            root.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
        }
        LinearLayout actions = new LinearLayout(host);
        Button later = new Button(host); later.setText(R.string.update_later); later.setOnClickListener(v->dialog.dismiss());
        Button download = new Button(host); download.setText(R.string.update_download); download.setTag("update_download_button");
        download.setOnClickListener(v->{ dialog.dismiss(); host.startActivity(new Intent(host,UpdateDownloadActivity.class)); });
        actions.addView(later,new LinearLayout.LayoutParams(0,dp(host,50),1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,dp(host,50),1); params.setMarginStart(dp(host,8));
        actions.addView(download,params); root.addView(actions);
        dialog.setContentView(root); dialog.setCanceledOnTouchOutside(true); dialog.setCancelable(true);
        dialog.show(); current = new WeakReference<>(dialog); owner = new WeakReference<>(host);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(-1,-2); window.setGravity(Gravity.BOTTOM);
            root.setTranslationY(dp(host,80)); root.setAlpha(0);
            root.animate().translationY(0).alpha(1).setDuration(220).start();
        }
        AppUpdates.prefs(host).edit().putLong("shown_code",release.code).apply();
    }
    private static TextView text(Activity host,String value,int size) {
        TextView text = new TextView(host); text.setText(value); text.setTextSize(size);
        text.setTextColor(AppSettings.textPrimary(host)); text.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        text.setPadding(0,dp(host,5),0,dp(host,5)); return text;
    }
    private static int dp(Activity host,int value) { return Math.round(value*host.getResources().getDisplayMetrics().density); }
}
