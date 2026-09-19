package com.ilia.advanceclock;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public final class PenSizeAdapter extends BaseAdapter {
    private static final int[] ICONS = {
            R.drawable.ic_pen_size_1,
            R.drawable.ic_pen_size_2,
            R.drawable.ic_pen_size_3,
            R.drawable.ic_pen_size_4
    };

    private final Context context;

    public PenSizeAdapter(Context context) {
        this.context = context;
    }

    @Override public int getCount() {
        return ICONS.length;
    }

    @Override public Object getItem(int position) {
        return ICONS[Math.max(0, Math.min(ICONS.length - 1, position))];
    }

    @Override public long getItemId(int position) {
        return position;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        return createIcon(position, 44);
    }

    @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createIcon(position, 48);
    }

    private View createIcon(int position, int heightDp) {
        ImageView image = new ImageView(context);
        image.setImageResource(ICONS[Math.max(0, Math.min(ICONS.length - 1, position))]);
        image.setColorFilter(AppSettings.primaryColor(context), PorterDuff.Mode.SRC_IN);
        image.setScaleType(ImageView.ScaleType.CENTER);
        image.setPadding(dp(10), dp(8), dp(10), dp(8));
        image.setBackgroundResource(R.drawable.bg_field);
        image.setContentDescription("اندازه قلم " + (position + 1));

        android.widget.AbsListView.LayoutParams lp =
                new android.widget.AbsListView.LayoutParams(dp(48), dp(heightDp));
        image.setLayoutParams(lp);
        return image;
    }

    private int dp(int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
