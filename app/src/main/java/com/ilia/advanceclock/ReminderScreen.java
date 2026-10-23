package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import java.util.*;
import java.util.function.IntConsumer;

/** The same alert surface and controls for alarms, notes, stopwatch and timer. */
final class ReminderScreen extends FrameLayout {
    static final int[] SNOOZE_MINUTES = {5,15,30,60,120,180,360,720,1440};
    private static final int[] SNOOZE_LABELS = {R.string.runtime_text_0575,R.string.runtime_text_0576,
            R.string.runtime_text_0577,R.string.runtime_text_0578,R.string.runtime_text_0579,
            R.string.runtime_text_0580,R.string.runtime_text_0581,R.string.runtime_text_0582,R.string.runtime_text_0583};
    static final class Content {
        String title = "", body = "", image1 = "", image2 = "", openLabel = "";
        boolean background;
        List<NoteAttachment> attachments = new ArrayList<>();
        int snoozeMinutes = 15;
    }
    private final Activity host;
    private final android.os.Handler clockHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private TextView timeLabel, dateLabel;
    private AlarmAnalogView analogClock;
    private java.util.function.Consumer<Boolean> blurListener;
    private final Runnable clockTick = new Runnable() {
        @Override public void run() { updateClock(); clockHandler.postDelayed(this, 1000 - System.currentTimeMillis()%1000); }
    };
    static void applyWindowTheme(Activity host) {
        if (AppSettings.alarmScreenStyle(host)==4)
            host.getTheme().applyStyle(R.style.OverlayAdvanceClockBlur,true);
    }

    private int textColor, mutedColor, primary, secondary, background;

    static void prepareWindow(Activity host) {
        if (Build.VERSION.SDK_INT >= 33) host.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, () -> { /* Stop explicitly. */ });
        if (Build.VERSION.SDK_INT >= 27) { host.setShowWhenLocked(true); host.setTurnScreenOn(true); }
        else host.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        host.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(AppSettings.alarmScreenStyle(host)==4) {
            host.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
            host.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        }
        hideSystemBars(host);
    }
    static void hideSystemBars(Activity host) {
        // prepareWindow runs before setContentView: initialize DecorView before querying its controller.
        // PhoneWindow.getInsetsController dereferences mDecor directly on Android 11+.
        View decor = host.getWindow().getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            host.getWindow().setDecorFitsSystemWindows(false);
            android.view.WindowInsetsController controller = decor.getWindowInsetsController();
            if (controller != null) {
                controller.setSystemBarsBehavior(android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                controller.hide(android.view.WindowInsets.Type.systemBars());
            }
        } else decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    ReminderScreen(Activity host, Content data, Runnable stop, Runnable open, IntConsumer snooze) {
        super(host); this.host = host;
        int style = AppSettings.alarmScreenStyle(host);
        boolean modern = style >= 3;
        palette(style);
        setBackgroundColor(background);
        host.getWindow().setStatusBarColor(background); host.getWindow().setNavigationBarColor(background);
        List<String> photos = new ArrayList<>();
        if (data.image1 != null && !data.image1.isEmpty()) photos.add(data.image1);
        if (data.image2 != null && !data.image2.isEmpty()) photos.add(data.image2);
        List<NoteAttachment> values = new ArrayList<>(data.attachments);
        boolean photoBackground = style != 4 && data.background && !photos.isEmpty();
        if (photoBackground) {
            LinearLayout images = new LinearLayout(host); images.setOrientation(LinearLayout.VERTICAL);
            addView(images, new FrameLayout.LayoutParams(-1, -1));
            for (String uri : photos) {
                AlarmPhotoView image = new AlarmPhotoView(host, false); image.load(uri);
                images.addView(image, new LinearLayout.LayoutParams(-1, 0, 1));
            }
            values.removeIf(value -> value.isImage() && photos.contains(value.value));
            textColor = Color.WHITE; mutedColor = 0xFFEEEEEE;
        } else for (String uri : photos) {
            boolean present = false;
            for (NoteAttachment value : values) if (uri.equals(value.value)) present = true;
            if (!present) values.add(new NoteAttachment(NoteAttachment.KIND_FILE, uri, host.getString(R.string.runtime_text_0061), "image/jpeg"));
        }
        FrameLayout screen = new FrameLayout(host);
        screen.setBackgroundColor(photoBackground ? 0x66000000 : background);
        screen.setLayoutDirection(AppSettings.layoutDirection(host));
        screen.setPadding(dp(20), 0, dp(20), 0); screen.setMinimumHeight(dp(560));
        LinearLayout headingContent = new LinearLayout(host); headingContent.setOrientation(LinearLayout.VERTICAL);
        headingContent.setPadding(0, dp(30), 0, dp(12));
        if (style == 3) {
            analogClock = new AlarmAnalogView(host);
            int clockSize = Math.min(dp(270), Math.min(host.getResources().getDisplayMetrics().widthPixels-dp(64),
                    Math.round(host.getResources().getDisplayMetrics().heightPixels*.34f)));
            LinearLayout.LayoutParams clockParams = new LinearLayout.LayoutParams(clockSize,clockSize);
            clockParams.gravity=Gravity.CENTER_HORIZONTAL;
            headingContent.addView(analogClock,clockParams);
        }
        dateLabel = text("", modern ? 18 : 14);
        dateLabel.setTextColor(mutedColor); dateLabel.setGravity(Gravity.CENTER);
        timeLabel = text("", style == 4 ? 80 : 48);
        timeLabel.setTextColor(modern ? textColor : photoBackground ? Color.WHITE : primary);
        timeLabel.setGravity(Gravity.CENTER);timeLabel.setTextDirection(View.TEXT_DIRECTION_LTR);
        timeLabel.setTypeface(null, android.graphics.Typeface.BOLD);
        timeLabel.setMaxLines(1);
        timeLabel.setHorizontallyScrolling(false);
        timeLabel.setAutoSizeTextTypeUniformWithConfiguration(28,style == 4 ? 80 : 48,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        if (modern) { if(style!=3) headingContent.addView(timeLabel,matchWrap()); else timeLabel.setVisibility(View.GONE); headingContent.addView(dateLabel,matchWrap()); }
        else { headingContent.addView(dateLabel,matchWrap()); headingContent.addView(timeLabel,matchWrap()); }
        updateClock();
        TextView title = text(data.title, 18); title.setGravity(Gravity.CENTER); headingContent.addView(title, matchWrap());
        if (data.body != null && !data.body.isEmpty()) {
            TextView body = text(data.body, 16); body.setGravity(Gravity.CENTER); body.setTextColor(mutedColor);
            LinearLayout.LayoutParams bp = matchWrap(); bp.topMargin = dp(8); headingContent.addView(body, bp);
        }
        ScrollView header = new ScrollView(host); header.addView(headingContent);
        screen.addView(header, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));
        LinearLayout footer = new LinearLayout(host); footer.setOrientation(LinearLayout.VERTICAL);
        footer.setPadding(0, dp(8), 0, modern ? dp(2) : dp(32));
        if (open != null) {
            Button edit = button(data.openLabel.isEmpty() ? host.getString(R.string.media_open) : data.openLabel, false);
            edit.setOnClickListener(v -> open.run()); footer.addView(edit, new LinearLayout.LayoutParams(-1, dp(44)));
        }
        LinearLayout snoozeRow = new LinearLayout(host); snoozeRow.setGravity(Gravity.CENTER_VERTICAL);
        Spinner durations = new Spinner(host);
        String[] labels = new String[SNOOZE_LABELS.length];
        int selected = 1;
        for (int i = 0; i < labels.length; i++) { labels[i] = host.getString(SNOOZE_LABELS[i]); if (SNOOZE_MINUTES[i] == data.snoozeMinutes) selected = i; }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(host, android.R.layout.simple_spinner_item, labels) {
            @Override public View getView(int position, View reuse, android.view.ViewGroup parent) {
                TextView label = (TextView) super.getView(position, reuse, parent);
                label.setTextColor(textColor); label.setHorizontallyScrolling(false);
                label.setMaxLines(1); return label;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        durations.setAdapter(adapter); durations.setSelection(selected);
        durations.setContentDescription(host.getString(R.string.ui_snooze_after));
        snoozeRow.addView(durations, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button later = button(host.getString(R.string.ui_snooze), false);
        later.setOnClickListener(v -> snooze.accept(SNOOZE_MINUTES[durations.getSelectedItemPosition()]));
        snoozeRow.addView(later, new LinearLayout.LayoutParams(0, dp(48), 1));
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, dp(48)); rowParams.topMargin = dp(8);
        boolean centeredSnooze = style == 4 && values.isEmpty();
        if (centeredSnooze) {
            FrameLayout.LayoutParams centered = new FrameLayout.LayoutParams(-1,dp(56),Gravity.CENTER);
            centered.leftMargin=dp(24); centered.rightMargin=dp(24);
            screen.addView(snoozeRow,centered);
        } else footer.addView(snoozeRow,rowParams);
        Button dismiss = button(host.getString(R.string.ui_stop_alarm), true); dismiss.setOnClickListener(v -> stop.run());
        LinearLayout.LayoutParams dismissParams = new LinearLayout.LayoutParams(-1, dp(60)); dismissParams.topMargin = dp(10);
        TextView swipeHint = null;
        if (modern) {
            swipeHint = text(host.getString(R.string.alarm_swipe_stop),18);
            swipeHint.setGravity(Gravity.CENTER);
            swipeHint.setPadding(0,dp(12),0,0);
            swipeHint.setIncludeFontPadding(false);
            swipeHint.setMaxLines(1);
            swipeHint.setHorizontallyScrolling(false);
            swipeHint.setEllipsize(null);
            swipeHint.setAutoSizeTextTypeUniformWithConfiguration(6,18,1,android.util.TypedValue.COMPLEX_UNIT_SP);
            footer.addView(swipeHint, new LinearLayout.LayoutParams(-1,-2));
        } else footer.addView(dismiss, dismissParams);
        screen.addView(footer, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        View shortcuts = attachmentStrip(values);
        screen.addView(shortcuts, new FrameLayout.LayoutParams(-1, dp(164), Gravity.CENTER));
        if (values.isEmpty()) shortcuts.setVisibility(View.GONE);
        boolean hasImages = false; for (NoteAttachment value : values) if (value.isImage()) hasImages = true;
        final int preferredHeight = dp(hasImages ? 220 : 164);
        screen.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int height = screen.getHeight();
            headingContent.measure(View.MeasureSpec.makeMeasureSpec(Math.max(1, screen.getWidth() - screen.getPaddingLeft() - screen.getPaddingRight()), View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int headerHeight = Math.min(headingContent.getMeasuredHeight(), modern ? height * 3 / 5 : height / 3);
            FrameLayout.LayoutParams hp = (FrameLayout.LayoutParams) header.getLayoutParams();
            if (hp.height != headerHeight) { hp.height = headerHeight; header.setLayoutParams(hp); }
            int top = headerHeight + dp(8), bottom = height - footer.getMeasuredHeight() - dp(8);
            ReminderLayout.Placement placement = ReminderLayout.centered(height, top, bottom, preferredHeight);
            int shortcutHeight = placement.height, shortcutTop = placement.top;
            FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) shortcuts.getLayoutParams();
            if (cp.height != shortcutHeight || cp.topMargin != shortcutTop || cp.gravity != Gravity.TOP) {
                cp.height = shortcutHeight; cp.topMargin = shortcutTop; cp.gravity = Gravity.TOP; shortcuts.setLayoutParams(cp);
            }
        });
        screen.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom;
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets safe = insets.getInsetsIgnoringVisibility(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout()); top = safe.top; bottom = safe.bottom;
            } else { top = insets.getStableInsetTop(); bottom = insets.getStableInsetBottom(); }
            headingContent.setPadding(0, Math.max(dp(30), top + dp(8)), 0, dp(8));
            int visibleBottom=Build.VERSION.SDK_INT>=30 ? insets.getInsets(android.view.WindowInsets.Type.systemBars() | android.view.WindowInsets.Type.displayCutout()).bottom : insets.getSystemWindowInsetBottom();
            footer.setPadding(0, dp(8), 0, modern ? visibleBottom + dp(2) : bottom + dp(24)); return insets;
        });
        ScrollView root = new ScrollView(host); root.setFillViewport(true); root.addView(screen, new ScrollView.LayoutParams(-1, -1));
        if (modern) {
            SwipeAlarmSurface swipe = new SwipeAlarmSurface(host,stop);
            swipe.addView(root,new FrameLayout.LayoutParams(-1,-1));
            swipe.addView(new SwipeHintArrow(host,swipeHint,mutedColor),new FrameLayout.LayoutParams(-1,-1));
            addView(swipe,new FrameLayout.LayoutParams(-1,-1));
        } else addView(root,new FrameLayout.LayoutParams(-1,-1));
        if(style==4) { screen.setBackgroundColor(Color.TRANSPARENT);setBackgroundColor(0x66000000); }
        if (style == 4 && Build.VERSION.SDK_INT>=31) {
            blurListener = enabled -> {
                if (enabled) {
                    setBackgroundColor(0x55000000);
                    host.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                    WindowManager.LayoutParams attributes=host.getWindow().getAttributes();
                    attributes.setBlurBehindRadius(dp(75));
                    host.getWindow().setAttributes(attributes);
                    host.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                } else {
                    setBackgroundColor(0x99000000);
                    host.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                }
            };
        }
        screen.requestApplyInsets();
    }
    private void updateClock() {
        long now=System.currentTimeMillis();
        if(timeLabel!=null) timeLabel.setText(TimeFormat.clock(host,now));
        if(dateLabel!=null) dateLabel.setText(CalendarUtils.formatDate(now,AppSettings.defaultCalendar(host)));
        if(analogClock!=null) analogClock.invalidate();
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();clockHandler.post(clockTick);
        if(Build.VERSION.SDK_INT>=31 && blurListener!=null) {
            host.getWindowManager().addCrossWindowBlurEnabledListener(host.getMainExecutor(),blurListener);
            blurListener.accept(host.getWindowManager().isCrossWindowBlurEnabled());
        }
    }
    @Override protected void onDetachedFromWindow() {
        clockHandler.removeCallbacks(clockTick);
        if(Build.VERSION.SDK_INT>=31 && blurListener!=null)
            host.getWindowManager().removeCrossWindowBlurEnabledListener(blurListener);
        super.onDetachedFromWindow();
    }

    private void palette(int style) {
        if (style == 3) { background = AppSettings.background(host); primary = AppSettings.primaryColor(host); textColor = AppSettings.textPrimary(host); mutedColor = AppSettings.textSecondary(host); secondary = AppSettings.field(host); }
        else if (style == 4) { background = 0xFF17212B; primary = Color.WHITE; textColor = Color.WHITE; mutedColor = 0xFFE1E5EB; secondary = 0x554E5B69; }
        else if (style == 1) { background = 0xFF080B0D; primary = 0xFF53C7C0; textColor = 0xFFF7FAF9; mutedColor = 0xFF9BAAA6; secondary = 0xFF2A3533; }
        else if (style == 2) { background = 0xFFFFF5EA; primary = 0xFFB85C00; textColor = 0xFF4B2A12; mutedColor = 0xFF8B6A50; secondary = 0xFFFFDFC0; }
        else { background = 0xFFF5FAF9; primary = AppSettings.primaryColor(host); textColor = 0xFF173F3B; mutedColor = 0xFF758783; secondary = 0xFFE7F1EF; }
    }
    private Button button(String label, boolean main) {
        Button view = new Button(host); view.setText(label); view.setAllCaps(false);
        view.setTextColor(main ? Color.WHITE : primary);
        GradientDrawable shape = new GradientDrawable(); shape.setColor(main ? primary : secondary); shape.setCornerRadius(dp(18));
        view.setBackground(shape); return view;
    }
    private View attachmentStrip(List<NoteAttachment> values) {
        HorizontalScrollView scroll = new HorizontalScrollView(host);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.setLayoutDirection(AppSettings.layoutDirection(host));
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
            android.content.pm.ApplicationInfo info = host.getPackageManager().getApplicationInfo(value.value, 0);
            return host.getPackageManager().getApplicationLabel(info).toString();
        } catch (Exception ignored) {
            return value.name == null || value.name.isEmpty() || value.name.equals(value.value)
                    ? host.getString(R.string.media_app) : value.name;
        }
    }

    private void openAttachment(NoteAttachment value) {
        android.app.KeyguardManager keyguard = host.getSystemService(android.app.KeyguardManager.class);
        if (keyguard != null && keyguard.isKeyguardLocked()) {
            keyguard.requestDismissKeyguard(host, new android.app.KeyguardManager.KeyguardDismissCallback() {
                @Override public void onDismissSucceeded() {
                    if (!host.isFinishing() && !host.isDestroyed()) openUnlockedAttachment(value);
                }
            });
        } else openUnlockedAttachment(value);
    }

    private void openUnlockedAttachment(NoteAttachment value) {
        if (!NoteAttachment.KIND_APP.equals(value.kind)) { value.open(host); return; }
        try {
            Intent launch = host.getPackageManager().getLaunchIntentForPackage(value.value);
            if (launch == null) {
                // Some launchers expose only the MAIN/LAUNCHER activity, not an INFO activity.
                Intent query = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(value.value);
                List<android.content.pm.ResolveInfo> activities = host.getPackageManager().queryIntentActivities(query, 0);
                if (!activities.isEmpty()) {
                    android.content.pm.ActivityInfo info = activities.get(0).activityInfo;
                    launch = query.setComponent(new android.content.ComponentName(info.packageName, info.name));
                }
            }
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                host.startActivity(launch); return;
            }
        } catch (Exception ignored) {}
        LogoToast.makeText(host, R.string.note_app_unavailable, Toast.LENGTH_SHORT).show();
    }

    private View attachmentView(NoteAttachment value){
        LinearLayout card=new LinearLayout(host);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(6),dp(6),dp(6),dp(6));
        card.setBackgroundColor(Color.TRANSPARENT);
        card.setClickable(true);
        card.setFocusable(true);
        String displayName = NoteAttachment.KIND_APP.equals(value.kind) ? appName(value) : value.name;
        card.setContentDescription(displayName);
        android.util.TypedValue ripple = new android.util.TypedValue();
        if (host.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, ripple, true))
            card.setBackgroundResource(ripple.resourceId);

        if(value.isImage()){
            AlarmPhotoView image = new AlarmPhotoView(host, false);
            image.load(value.value);
            card.addView(
                    image,
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(160)));
        }else if(NoteAttachment.KIND_APP.equals(value.kind)){
            ImageView icon=new ImageView(host);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            icon.setImageDrawable(host.getPackageManager().getDefaultActivityIcon());
            icon.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            try{
                Drawable drawable=host.getPackageManager()
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
        TextView view=new TextView(host);
        view.setText(CalendarUtils.fa(value));
        view.setTextSize(sp);
        view.setTextColor(textColor);
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

}
