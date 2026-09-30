package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public final class SoundPickerActivity extends Activity {
    public static final String EXTRA_URI = "soundUri";
    public static final String EXTRA_NAME = "soundName";
    private static final int REQ_AUDIO = 810;
    private LinearLayout list;

    @Override protected void onCreate(Bundle state) {
        AppSettings.applyTheme(this); AppSettings.applyModalOverlay(this); super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setLayoutDirection(AppSettings.layoutDirection(this));
        root.setPadding(dp(18), dp(12), dp(18), dp(18)); root.setBackgroundColor(AppSettings.background(this));
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); top.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
        ImageButton close = new ImageButton(this); close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0); close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        TextView title = new TextView(this); title.setText(AppString.get(R.string.runtime_text_0150)); title.setTextSize(24);
        title.setTextColor(AppSettings.textPrimary(this)); title.setTypeface(null, 1);
        title.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL); title.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1)); root.addView(top);
        Button upload = button(AppString.get(R.string.runtime_text_0151)); upload.setOnClickListener(v -> upload());
        root.addView(upload, new LinearLayout.LayoutParams(-1, dp(54)));
        ScrollView scroll = new ScrollView(this); list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL); scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, 0, 1); sp.topMargin=dp(10); root.addView(scroll, sp);
        setContentView(root); AppSettings.applyFullscreenInsets(root); AppSettings.playFullscreenEnter(this); render();
    }

    private void render() {
        list.removeAllViews();
        for (SoundLibrary.Sound sound : SoundLibrary.all(this)) {
            Button row = button(sound.name); row.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
            row.setOnClickListener(v -> {
                setResult(RESULT_OK, new Intent().putExtra(EXTRA_URI, sound.uri).putExtra(EXTRA_NAME, sound.name));
                finish();
            });
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(58));p.bottomMargin=dp(7);list.addView(row,p);
        }
    }

    private void upload() {
        Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("audio/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent,REQ_AUDIO);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){super.onActivityResult(requestCode,resultCode,data);
        if(requestCode!=REQ_AUDIO||resultCode!=RESULT_OK||data==null||data.getData()==null)return;
        Uri uri=data.getData();try{getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}
        SoundLibrary.add(this,uri);render();}
    private Button button(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextColor(AppSettings.textPrimary(this));b.setPadding(dp(16),0,dp(16),0);b.setBackgroundResource(R.drawable.bg_card);return b;}
    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    @Override public void finish(){super.finish();AppSettings.playFullscreenExit(this);}
}
