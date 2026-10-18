package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class NoteReminderActivity extends Activity {
    private static java.lang.ref.WeakReference<NoteReminderActivity> showing =
            new java.lang.ref.WeakReference<>(null);
    private long noteId=-1L;

    static void finishIfShowing(long id) {
        NoteReminderActivity activity = showing.get();
        if (activity != null && activity.noteId == id) {
            activity.runOnUiThread(activity::finishAndRemoveTask);
        }
    }

    @Override protected void onDestroy() {
        if (showing.get() == this) showing.clear();
        super.onDestroy();
    }

    @Override protected void onCreate(Bundle state){
        AppSettings.applyTheme(this);
        super.onCreate(state);
        showing = new java.lang.ref.WeakReference<>(this);
        if(Build.VERSION.SDK_INT>=27){
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }else{
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            |WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        |WindowManager.LayoutParams.FLAG_FULLSCREEN);
        enterImmersiveMode();
        bind(getIntent());
    }

    @Override protected void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        setIntent(intent);
        bind(intent);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus)enterImmersiveMode();
    }

    private void enterImmersiveMode(){
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(android.view.WindowInsets.Type.systemBars());
            }
            return;
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        |View.SYSTEM_UI_FLAG_FULLSCREEN
                        |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        |View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        |View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    private void bind(Intent intent){
        noteId=intent==null?-1L:intent.getLongExtra("noteId",-1L);
        NoForgetItem item=new NoForgetStore(this).find(noteId);
        if(item==null){
            finish();
            return;
        }

        FrameLayout screen = new FrameLayout(this);
        screen.setBackgroundColor(AppSettings.background(this));
        screen.setLayoutDirection(AppSettings.layoutDirection(this));
        screen.setPadding(dp(20), 0, dp(20), 0);
        screen.setMinimumHeight(dp(480));

        LinearLayout headingContent = new LinearLayout(this);
        headingContent.setOrientation(LinearLayout.VERTICAL);
        headingContent.setPadding(0, dp(36), 0, dp(12));
        TextView heading = text(item.title.trim().isEmpty()
                ? AppString.get(R.string.runtime_text_0311) : item.title, 26);
        heading.setGravity(Gravity.CENTER);
        headingContent.addView(heading, matchWrap());
        if (!item.body.trim().isEmpty()) {
            TextView body = text(item.body, 17);
            body.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams params = matchWrap();
            params.topMargin = dp(14);
            headingContent.addView(body, params);
        }
        ScrollView header = new ScrollView(this);
        header.setFillViewport(false);
        header.addView(headingContent);
        screen.addView(header, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout footer = new LinearLayout(this);
        footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(0, dp(12), 0, dp(32));
        Button openNote = new Button(this);
        openNote.setText(AppString.get(R.string.runtime_text_0308));
        openNote.setAllCaps(false);
        openNote.setTextColor(AppSettings.primaryColor(this));
        openNote.setBackgroundResource(R.drawable.bg_soft_button);
        openNote.setOnClickListener(v -> {
            startActivity(new Intent(this, NoForgetEditorActivity.class).putExtra("noteId", noteId));
            stopAndClose();
        });
        footer.addView(openNote, new LinearLayout.LayoutParams(-1, dp(56)));
        Button dismiss = new Button(this);
        dismiss.setText(AppString.get(R.string.runtime_text_0309));
        dismiss.setAllCaps(false);
        dismiss.setTextColor(Color.WHITE);
        dismiss.setBackgroundResource(R.drawable.bg_teal_button);
        dismiss.setOnClickListener(v -> stopAndClose());
        LinearLayout.LayoutParams dismissParams = new LinearLayout.LayoutParams(-1, dp(56));
        dismissParams.topMargin = dp(12);
        footer.addView(dismiss, dismissParams);
        screen.addView(footer, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        List<NoteAttachment> attachments = NoteAttachment.parse(item.attachmentsJson);
        View shortcuts = attachmentStrip(attachments);
        screen.addView(shortcuts, new FrameLayout.LayoutParams(-1, dp(180), Gravity.CENTER));
        boolean hasImages = false;
        for (NoteAttachment attachment : attachments) if (attachment.isImage()) hasImages = true;
        final int preferredHeight = dp(hasImages ? 220 : 164);
        // Keep the shortcuts at the screen center whenever header/footer space permits it.
        // A long note scrolls independently instead of pushing its app shortcut downwards.
        screen.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            int height = screen.getHeight();
            headingContent.measure(View.MeasureSpec.makeMeasureSpec(
                    Math.max(1, screen.getWidth() - screen.getPaddingLeft() - screen.getPaddingRight()), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int headerHeight = Math.min(headingContent.getMeasuredHeight(), height / 3);
            FrameLayout.LayoutParams hp = (FrameLayout.LayoutParams) header.getLayoutParams();
            if (hp.height != headerHeight) { hp.height = headerHeight; header.setLayoutParams(hp); }
            int top = headerHeight + dp(8);
            int bottom = height - footer.getMeasuredHeight() - dp(8);
            int shortcutHeight = Math.max(dp(64), Math.min(preferredHeight, bottom - top));
            int shortcutTop = Math.max(top, Math.min((height - shortcutHeight) / 2, bottom - shortcutHeight));
            FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) shortcuts.getLayoutParams();
            if (cp.height != shortcutHeight || cp.topMargin != shortcutTop || cp.gravity != Gravity.TOP) {
                cp.height = shortcutHeight; cp.topMargin = shortcutTop; cp.gravity = Gravity.TOP;
                shortcuts.setLayoutParams(cp);
            }
        });
        screen.setOnApplyWindowInsetsListener((v, insets) -> {
            int safeTop, safeBottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsetsIgnoringVisibility(
                        android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout());
                safeTop = safe.top; safeBottom = safe.bottom;
            } else { safeTop = insets.getStableInsetTop(); safeBottom = insets.getStableInsetBottom(); }
            headingContent.setPadding(0, Math.max(dp(36), safeTop + dp(12)), 0, dp(12));
            footer.setPadding(0, dp(12), 0, safeBottom + dp(24));
            return insets;
        });
        ScrollView root = new ScrollView(this);
        root.setFillViewport(true);
        root.setBackgroundColor(AppSettings.background(this));
        root.addView(screen, new ScrollView.LayoutParams(-1, -1));
        setContentView(root);
        screen.requestApplyInsets();
    }

    private View attachmentStrip(List<NoteAttachment> values) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(AppSettings.layoutDirection(this));
        for (int i = 0; i < values.size(); i++) {
            NoteAttachment value = values.get(i);
            View card = attachmentView(value);
            card.setOnClickListener(v -> openAttachment(value));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    dp(NoteAttachment.KIND_APP.equals(value.kind) ? 100 : 200), -1);
            // Symmetric margins keep one app and groups centered without a trailing gap.
            params.setMarginStart(dp(4)); params.setMarginEnd(dp(4));
            row.addView(card, params);
        }
        scroll.addView(row, new HorizontalScrollView.LayoutParams(-2, -1));
        boolean appsOnly = !values.isEmpty();
        for (NoteAttachment value : values) if (!NoteAttachment.KIND_APP.equals(value.kind)) appsOnly = false;
        if (appsOnly) scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            int width = Math.max(dp(92), Math.min(dp(100),
                    (scroll.getWidth() - dp(8) * values.size()) / values.size()));
            for (int i = 0; i < row.getChildCount(); i++) {
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) row.getChildAt(i).getLayoutParams();
                if (params.width != width) { params.width = width; row.getChildAt(i).setLayoutParams(params); }
            }
        });
        return scroll;
    }

    private String appName(NoteAttachment value) {
        try {
            android.content.pm.ApplicationInfo info = getPackageManager().getApplicationInfo(value.value, 0);
            return getPackageManager().getApplicationLabel(info).toString();
        } catch (Exception ignored) {
            return value.name == null || value.name.isEmpty() || value.name.equals(value.value)
                    ? getString(R.string.media_app) : value.name;
        }
    }

    private void openAttachment(NoteAttachment value) {
        if (!NoteAttachment.KIND_APP.equals(value.kind)) { value.open(this); return; }
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(value.value);
            if (launch == null) {
                // Some launchers expose only the MAIN/LAUNCHER activity, not an INFO activity.
                Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(value.value);
                List<android.content.pm.ResolveInfo> activities = getPackageManager().queryIntentActivities(query, 0);
                if (!activities.isEmpty()) {
                    android.content.pm.ActivityInfo info = activities.get(0).activityInfo;
                    launch = query.setComponent(new android.content.ComponentName(info.packageName, info.name));
                }
            }
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(launch); return;
            }
        } catch (Exception ignored) {}
        Toast.makeText(this, R.string.note_app_unavailable, Toast.LENGTH_SHORT).show();
    }

    private View attachmentView(NoteAttachment value){
        LinearLayout card=new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(6),dp(6),dp(6),dp(6));
        card.setBackgroundColor(Color.TRANSPARENT);
        card.setClickable(true);
        card.setFocusable(true);
        String displayName = NoteAttachment.KIND_APP.equals(value.kind) ? appName(value) : value.name;
        card.setContentDescription(displayName);
        android.util.TypedValue ripple = new android.util.TypedValue();
        if (getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true))
            card.setBackgroundResource(ripple.resourceId);

        if(value.isImage()){
            ImageView image=new ImageView(this);
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            image.setBackgroundColor(Color.TRANSPARENT);
            try{
                image.setImageURI(Uri.parse(value.value));
            }catch(Exception ignored){}
            card.addView(
                    image,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(160)));
        }else if(NoteAttachment.KIND_APP.equals(value.kind)){
            ImageView icon=new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setImageDrawable(getPackageManager().getDefaultActivityIcon());
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            try{
                Drawable drawable=getPackageManager()
                        .getApplicationIcon(value.value);
                icon.setImageDrawable(drawable);
            }catch(Exception ignored){}
            card.addView(
                    icon,
                    new LinearLayout.LayoutParams(dp(80),dp(80)));
        }else{
            TextView symbol=text(
                    NoteAttachment.KIND_URL.equals(value.kind)?"🌐":"📎",
                    52);
            symbol.setGravity(Gravity.CENTER);
            card.addView(
                    symbol,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(132)));
        }

        TextView name=text(displayName,15);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        name.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams np=matchWrap();
        np.topMargin=dp(8);
        card.addView(name,np);
        return card;
    }

    private TextView text(String value,int sp){
        TextView view=new TextView(this);
        view.setText(CalendarUtils.fa(value));
        view.setTextSize(sp);
        view.setTextColor(AppSettings.textPrimary(this));
        return view;
    }

    private LinearLayout.LayoutParams matchWrap(){
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value){
        return Math.round(
                value*getResources().getDisplayMetrics().density);
    }

    private void stopAndClose(){
        stopService(NoForgetSoundService.stopIntent(this));
        finishAndRemoveTask();
    }

    @Override public void onBackPressed(){
        stopAndClose();
    }
}
