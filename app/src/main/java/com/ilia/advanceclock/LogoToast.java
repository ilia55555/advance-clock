package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Transient feedback hosted only in the current app window, never a system toast. */
public final class LogoToast {
    private static LogoToast current;
    private final Context context;
    private final CharSequence message;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private View content;
    private Application application;
    private Application.ActivityLifecycleCallbacks lifecycle;
    private final Runnable dismiss = this::cancel;

    private LogoToast(Context context, CharSequence message) {
        this.context = context;
        this.message = message;
    }
    public static LogoToast makeText(Context context, CharSequence message, int ignoredDuration) {
        return new LogoToast(context, message);
    }
    public static LogoToast makeText(Context context, int message, int ignoredDuration) {
        return makeText(context, context.getText(message), ignoredDuration);
    }
    public void show() {
        if (Looper.myLooper() != Looper.getMainLooper()) { handler.post(this::show); return; }
        Activity host = findActivity(context);
        if (host == null || host.isFinishing() || host.isDestroyed()) return;
        if (host.getApplication() instanceof AdvanceClockApplication
                && AdvanceClockApplication.resumedActivity() != host) return;
        if (current != null) current.cancel();
        ViewGroup decor = (ViewGroup) host.getWindow().getDecorView();
        LinearLayout card = new LinearLayout(host);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setLayoutDirection(AppSettings.layoutDirection(host));
        card.setPadding(dp(16), dp(10), dp(16), dp(10));
        GradientDrawable background = new GradientDrawable();
        background.setColor(AppSettings.surface(host));
        background.setCornerRadius(dp(18));
        background.setStroke(dp(1), AppSettings.primaryColor(host));
        card.setBackground(background);
        card.setElevation(dp(6));
        ImageView logo = new ImageView(host);
        logo.setImageDrawable(host.getApplicationInfo().loadIcon(host.getPackageManager()));
        LinearLayout.LayoutParams icon = new LinearLayout.LayoutParams(dp(32), dp(32));
        icon.setMarginEnd(dp(10));
        card.addView(logo, icon);
        TextView text = new TextView(host);
        text.setText(message == null ? "" : message);
        text.setTextColor(AppSettings.textPrimary(host));
        text.setTextSize(14);
        card.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                Math.min(dp(360), host.getResources().getDisplayMetrics().widthPixels - dp(32)), -2,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        params.bottomMargin = dp(72);
        int slop = ViewConfiguration.get(host).getScaledTouchSlop();
        float[] down = new float[2];
        card.setOnTouchListener((view, event) -> {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                down[0] = event.getRawX(); down[1] = event.getRawY(); return true;
            }
            if (event.getActionMasked() == MotionEvent.ACTION_MOVE
                    && Math.hypot(event.getRawX()-down[0], event.getRawY()-down[1]) > slop) cancel();
            return true;
        });
        content = card;
        decor.addView(card, params); // Only the card receives touches; the rest of the window stays usable.
        current = this;
        application = host.getApplication();
        lifecycle = new Application.ActivityLifecycleCallbacks() {
            public void onActivityPaused(Activity activity) { if (activity == host) cancel(); }
            public void onActivityDestroyed(Activity activity) { if (activity == host) cancel(); }
            public void onActivityCreated(Activity a, Bundle b) {}
            public void onActivityStarted(Activity a) {}
            public void onActivityResumed(Activity a) {}
            public void onActivityStopped(Activity a) {}
            public void onActivitySaveInstanceState(Activity a, Bundle b) {}
        };
        application.registerActivityLifecycleCallbacks(lifecycle);
        card.announceForAccessibility(text.getText());
        handler.postDelayed(dismiss, 3000L);
    }
    public void cancel() {
        if (Looper.myLooper() != Looper.getMainLooper()) { handler.post(this::cancel); return; }
        handler.removeCallbacks(dismiss);
        if (content != null && content.getParent() instanceof ViewGroup)
            ((ViewGroup) content.getParent()).removeView(content);
        content = null;
        if (application != null && lifecycle != null) application.unregisterActivityLifecycleCallbacks(lifecycle);
        application = null; lifecycle = null;
        if (current == this) current = null;
    }
    private static Activity findActivity(Context context) {
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) return (Activity) context;
            Context base = ((ContextWrapper) context).getBaseContext();
            if (base == context) break;
            context = base;
        }
        return AdvanceClockApplication.resumedActivity();
    }
    private int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }
}
