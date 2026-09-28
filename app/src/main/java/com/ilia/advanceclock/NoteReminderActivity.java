package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class NoteReminderActivity extends Activity {
    private long noteId = -1L;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this);
        super.onCreate(state);
        if (Build.VERSION.SDK_INT >= 27) { setShowWhenLocked(true); setTurnScreenOn(true); }
        else getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        bind(getIntent());
    }

    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); bind(intent); }

    private void bind(Intent intent) {
        noteId = intent == null ? -1L : intent.getLongExtra("noteId", -1L);
        NoForgetItem item = new NoForgetStore(this).find(noteId);
        if (item == null) { finish(); return; }
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL); content.setPadding(dp(20), dp(42), dp(20), dp(24));
        TextView heading = text(item.title.trim().isEmpty() ? "یادآوری یادداشت" : item.title, 28);
        heading.setGravity(Gravity.CENTER); content.addView(heading, matchWrap());
        if (!item.body.trim().isEmpty()) { TextView body = text(item.body, 17); body.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams bp = matchWrap(); bp.topMargin = dp(14); content.addView(body, bp); }
        addAttachments(content, NoteAttachment.parse(item.attachmentsJson));
        Button openNote = new Button(this); openNote.setText("باز کردن یادداشت"); openNote.setOnClickListener(v -> {
            startActivity(new Intent(this, NoForgetEditorActivity.class).putExtra("noteId", noteId)); stopAndClose(); });
        LinearLayout.LayoutParams op = matchWrap(); op.topMargin = dp(18); content.addView(openNote, op);
        Button dismiss = new Button(this); dismiss.setText("قطع هشدار"); dismiss.setOnClickListener(v -> stopAndClose());
        content.addView(dismiss, matchWrap());
        ScrollView root = new ScrollView(this); root.addView(content); setContentView(root);
    }

    private void addAttachments(LinearLayout root, List<NoteAttachment> values) {
        if (values.isEmpty()) return;
        HorizontalScrollView scroll = new HorizontalScrollView(this); scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        for (NoteAttachment value : values) {
            View card = attachmentView(value); card.setOnClickListener(v -> value.open(this));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(220), dp(180)); cp.setMarginEnd(dp(10));
            row.addView(card, cp);
        }
        scroll.addView(row); LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(180));
        sp.topMargin = dp(22); root.addView(scroll, sp);
    }

    private View attachmentView(NoteAttachment value) {
        if (!"app".equals(value.kind) && value.mime != null && value.mime.startsWith("image/")) {
            ImageView image = new ImageView(this); image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            try { image.setImageURI(Uri.parse(value.value)); } catch (Exception ignored) {}
            image.setContentDescription(value.name); return image;
        }
        Button button = new Button(this); button.setText(value.name); button.setAllCaps(false);
        if ("app".equals(value.kind)) try {
            Drawable icon = getPackageManager().getApplicationIcon(value.value);
            icon.setBounds(0, 0, dp(64), dp(64)); button.setCompoundDrawables(null, icon, null, null);
        } catch (Exception ignored) {}
        return button;
    }

    private TextView text(String value, int sp) { TextView view = new TextView(this); view.setText(CalendarUtils.fa(value));
        view.setTextSize(sp); view.setTextColor(AppSettings.textPrimary(this)); return view; }
    private LinearLayout.LayoutParams matchWrap() { return new LinearLayout.LayoutParams(-1, -2); }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void stopAndClose() { stopService(NoForgetSoundService.stopIntent(this)); finishAndRemoveTask(); }
    @Override public void onBackPressed() { stopAndClose(); }
}
