package com.ilia.advanceclock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

/** The selection action is independent of preview playback and seeking. */
final class SoundPreviewRow extends LinearLayout {
    final String uri,name;final boolean available;
    private final SoundPreviewController controller;
    private final ImageButton play;
    private final SeekBar seek;
    private final TextView elapsed,duration;
    private boolean tracking;
    SoundPreviewRow(Context c,SoundPreviewController controller,String uri,String name,boolean available,View selection,View extra){
        super(c);this.controller=controller;this.uri=uri;this.name=name;this.available=available;
        setOrientation(VERTICAL);setTag("sound_preview:"+uri);setBackgroundResource(R.drawable.bg_card);
        LinearLayout top=new LinearLayout(c);top.setGravity(Gravity.CENTER_VERTICAL);top.setLayoutDirection(AppSettings.layoutDirection(c));
        top.addView(selection,new LayoutParams(0,dp(54),1));if(extra!=null)top.addView(extra,new LayoutParams(-2,dp(54)));
        play=new ImageButton(c);play.setTag("sound_preview_play");play.setPadding(dp(14),dp(14),dp(14),dp(14));play.setElevation(0);play.setStateListAnimator(null);play.setBackgroundTintList(null);play.setImageTintList(ColorStateList.valueOf(Color.WHITE));
        GradientDrawable circle=new GradientDrawable();circle.setShape(GradientDrawable.OVAL);circle.setColor(AppSettings.primaryColor(c));
        play.setBackground(new InsetDrawable(new RippleDrawable(ColorStateList.valueOf(0x44FFFFFF),circle,null),dp(8)));play.setEnabled(available);play.setAlpha(available?1:.35f);
        play.setOnClickListener(v->controller.toggle(this));top.addView(play,new LayoutParams(dp(48),dp(48)));addView(top,new LayoutParams(-1,-2));
        LinearLayout timeline=new LinearLayout(c);timeline.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);timeline.setGravity(Gravity.CENTER_VERTICAL);timeline.setPadding(dp(12),0,dp(12),dp(4));
        elapsed=label(c,"sound_preview_elapsed");duration=label(c,"sound_preview_duration");seek=new SeekBar(c);seek.setTag("sound_preview_seek");seek.setMax(1);seek.setEnabled(false);
        seek.setProgressTintList(ColorStateList.valueOf(AppSettings.primaryColor(c)));seek.setThumbTintList(ColorStateList.valueOf(AppSettings.primaryColor(c)));
        timeline.addView(elapsed,new LayoutParams(-2,dp(32)));timeline.addView(seek,new LayoutParams(0,dp(36),1));timeline.addView(duration,new LayoutParams(-2,dp(32)));addView(timeline,new LayoutParams(-1,-2));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onStartTrackingTouch(SeekBar bar){tracking=true;}
            public void onProgressChanged(SeekBar bar,int value,boolean user){if(user)elapsed.setText(time(value));}
            public void onStopTrackingTouch(SeekBar bar){tracking=false;controller.seek(SoundPreviewRow.this,bar.getProgress());}
        });
        controller.register(this);
    }
    private TextView label(Context c,String tag){TextView text=new TextView(c);text.setTag(tag);text.setTextSize(11);text.setGravity(Gravity.CENTER);text.setTextColor(AppSettings.textSecondary(c));text.setIncludeFontPadding(false);return text;}
    void update(int position,int length,boolean playing){
        play.setImageResource(playing?R.drawable.ic_sound_preview_pause:R.drawable.ic_sound_preview_play);
        play.setContentDescription(getContext().getString(playing?R.string.sound_preview_pause:R.string.sound_preview_play,name));
        seek.setEnabled(available && length>0);seek.setMax(Math.max(1,length));duration.setText(length>0?time(length):"--:--");
        if(!tracking){seek.setProgress(Math.max(0,Math.min(position,length)));elapsed.setText(time(position));}
    }
    static String time(int millis){int seconds=Math.max(0,millis)/1000;return seconds>=3600?String.format(java.util.Locale.getDefault(),"%d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60):String.format(java.util.Locale.getDefault(),"%d:%02d",seconds/60,seconds%60);}
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();controller.register(this);}
    @Override protected void onDetachedFromWindow(){controller.unregister(this);super.onDetachedFromWindow();}
    @Override public void onVisibilityAggregated(boolean visible){super.onVisibilityAggregated(visible);if(!visible)controller.hidden(this);}
    private int dp(int value){return ReminderModalStyle.dp(getContext(),value);}
}
