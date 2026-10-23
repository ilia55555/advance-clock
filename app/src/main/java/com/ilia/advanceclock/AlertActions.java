package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.function.Consumer;

/** Shared compact actions and multi-select snooze picker for every ringing surface. */
final class AlertActions extends LinearLayout {
    static final int[] MINUTES={5,10,15,30,60,120,180,360,720,1440};
    private static final int[] LABELS={R.string.runtime_text_0575,R.string.alert_snooze_10,
            R.string.runtime_text_0576,R.string.runtime_text_0577,R.string.runtime_text_0578,
            R.string.runtime_text_0579,R.string.runtime_text_0580,R.string.runtime_text_0581,
            R.string.runtime_text_0582,R.string.runtime_text_0583};
    private final Activity host;
    private final AlertOutput output;
    private final LinearLayout sound, vibration;
    private final int color;
    private AlertDialog dialog;
    private final Runnable refresh=this::refreshState;

    AlertActions(Activity host, AlertOutput output, String openLabel, Runnable open,
                 int selectedMinutes, int color, Consumer<int[]> snooze) {
        super(host);this.host=host;this.output=output;this.color=color;
        setOrientation(VERTICAL);setTag("alert_actions");
        LinearLayout row=new LinearLayout(host);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setGravity(Gravity.CENTER_HORIZONTAL|Gravity.TOP);
        sound=action("alert_sound",R.drawable.ic_alert_sound_off,AppString.get(R.string.alert_mute),()->{
            if(output!=null) output.mute(!output.muted());refreshState();
        });
        vibration=action("alert_vibration",R.drawable.ic_alert_vibration_off,AppString.get(R.string.alert_vibration_off),()->{
            if(output!=null) output.vibrate(!output.vibrating());refreshState();
        });
        LinearLayout destination=action("alert_open",R.drawable.ic_alert_open,openLabel,open);
        row.addView(sound,column());row.addView(vibration,column());row.addView(destination,column());
        if(open==null) destination.setVisibility(INVISIBLE);
        addView(row,new LayoutParams(-1,-2));
        Button later=new Button(host);later.setTag("alert_snooze");
        later.setText(AppString.get(R.string.ui_snooze));later.setAllCaps(false);
        later.setTextColor(color);later.setTextSize(13);later.setMinHeight(0);later.setMinimumHeight(0);
        later.setMinWidth(0);later.setMinimumWidth(0);later.setPadding(dp(16),0,dp(16),0);
        later.setIncludeFontPadding(false);later.setMaxLines(1);later.setEllipsize(null);
        later.setHorizontallyScrolling(false);
        later.setAutoSizeTextTypeUniformWithConfiguration(4,13,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        later.setStateListAnimator(null);later.setElevation(0);later.setBackground(border());
        later.setOnClickListener(v->showSnooze(selectedMinutes,snooze));
        LayoutParams params=new LayoutParams(Math.min(dp(180),getResources().getDisplayMetrics().widthPixels-dp(64)),dp(40));params.topMargin=dp(8);params.gravity=Gravity.CENTER_HORIZONTAL;
        addView(later,params);refreshState();
    }
    private LayoutParams column() {
        LayoutParams params=new LayoutParams(0,-2,1);params.setMarginStart(dp(4));params.setMarginEnd(dp(4));return params;
    }
    private LinearLayout action(String tag,int icon,String title,Runnable click) {
        LinearLayout column=new LinearLayout(host);column.setOrientation(VERTICAL);column.setGravity(Gravity.CENTER_HORIZONTAL);
        ImageButton button=new ImageButton(host);button.setTag(tag);button.setImageResource(icon);button.setColorFilter(color);
        button.setPadding(dp(12),dp(12),dp(12),dp(12));button.setBackground(border());button.setStateListAnimator(null);button.setElevation(0);
        button.setContentDescription(title);button.setOnClickListener(v->{if(click!=null)click.run();});
        column.addView(button,new LayoutParams(dp(48),dp(48)));
        TextView label=new TextView(host);label.setText(title);label.setTextSize(10);label.setTextColor(color);
        label.setGravity(Gravity.CENTER);label.setPadding(0,dp(4),0,0);label.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        label.setMaxLines(1);label.setEllipsize(null);label.setIncludeFontPadding(false);
        label.setHorizontallyScrolling(false);
        label.setAutoSizeTextTypeUniformWithConfiguration(2,10,1,android.util.TypedValue.COMPLEX_UNIT_SP);
        column.addView(label,new LayoutParams(-1,-2));return column;
    }
    private android.graphics.drawable.Drawable border() {
        GradientDrawable shape=new GradientDrawable();shape.setColor(Color.TRANSPARENT);shape.setCornerRadius(dp(12));shape.setStroke(dp(1),color);
        return new RippleDrawable(ColorStateList.valueOf((color&0xFFFFFF)|0x22000000),shape,null);
    }
    void refreshState() {
        if(output==null)return;
        update(sound,output.muted()?R.drawable.ic_alert_sound_on:R.drawable.ic_alert_sound_off,
                AppString.get(output.muted()?R.string.alert_unmute:R.string.alert_mute));
        update(vibration,output.vibrating()?R.drawable.ic_alert_vibration_off:R.drawable.ic_alert_vibration_on,
                AppString.get(output.vibrating()?R.string.alert_vibration_off:R.string.alert_vibration_on));
    }
    private void update(LinearLayout column,int icon,String label) {
        ImageButton button=(ImageButton)column.getChildAt(0);
        if(label.contentEquals(button.getContentDescription()))return;
        button.setImageResource(icon);button.setContentDescription(label);((TextView)column.getChildAt(1)).setText(label);
    }
    private void showSnooze(int initial,Consumer<int[]> accept) {
        boolean[] chosen=new boolean[MINUTES.length];String[] labels=new String[MINUTES.length];
        for(int i=0;i<MINUTES.length;i++){chosen[i]=MINUTES[i]==initial;labels[i]=AppString.get(LABELS[i]);}
        dialog=new AlertDialog.Builder(host).setTitle(AppString.get(R.string.ui_snooze))
                .setMultiChoiceItems(labels,chosen,(d,index,checked)->chosen[index]=checked)
                .setNegativeButton(AppString.get(R.string.runtime_text_0003),null)
                .setPositiveButton(AppString.get(R.string.alert_confirm),null).create();
        dialog.show();
        for(int i=0;i<chosen.length;i++)dialog.getListView().setItemChecked(i,chosen[i]);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button->{
            ArrayList<Integer> selected=new ArrayList<>();for(int i=0;i<chosen.length;i++)if(chosen[i])selected.add(MINUTES[i]);
            if(selected.isEmpty()){LogoToast.makeText(host,R.string.alert_snooze_choose,Toast.LENGTH_SHORT).show();return;}
            int[] result=new int[selected.size()];for(int i=0;i<result.length;i++)result[i]=selected.get(i);
            dialog.dismiss();accept.accept(result);
        });
    }
    @Override protected void onAttachedToWindow(){super.onAttachedToWindow();AlertOutputEvents.add(refresh);refreshState();}
    @Override protected void onDetachedFromWindow(){AlertOutputEvents.remove(refresh);if(dialog!=null)dialog.dismiss();super.onDetachedFromWindow();}
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
}
