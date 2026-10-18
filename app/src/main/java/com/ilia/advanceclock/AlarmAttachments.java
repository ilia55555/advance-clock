package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.Button;

final class AlarmAttachments {
    static SharedPreferences prefs(Context context) { return context.getSharedPreferences("time_tools", Context.MODE_PRIVATE); }
    static void open(Activity host, String key) {
        host.startActivity(new Intent(host, AlarmAttachmentEditorActivity.class).putExtra("key", key));
    }
    static Button button(Activity host, String key) {
        Button button = new Button(host); button.setText(R.string.media_attachments);
        button.setAllCaps(false); button.setTextColor(AppSettings.primaryColor(host));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        button.setOnClickListener(v -> open(host, key)); return button;
    }
    static void bindButton(Activity host, Button first, String key) {
        android.view.ViewGroup row = (android.view.ViewGroup) first.getParent();
        for (int i = 0; i < row.getChildCount(); i++) if (row.getChildAt(i) != first) row.getChildAt(i).setVisibility(View.GONE);
        first.setText(R.string.media_attachments);
        first.setOnClickListener(v -> open(host, key));
    }
    static void read(Context context, String key, AlarmItem item) {
        SharedPreferences p = prefs(context);
        item.imageUri1 = p.getString(key + "_image1", ""); item.imageUri2 = p.getString(key + "_image2", "");
        item.imageSource1 = p.getString(key + "_source1", item.imageUri1);
        item.imageSource2 = p.getString(key + "_source2", item.imageUri2);
        item.attachmentsJson = p.getString(key + "_attachments", "[]");
        item.imagesAsBackground = p.getBoolean(key + "_background", false);
    }
    static void write(Context context, String key, AlarmItem item) {
        prefs(context).edit().putString(key + "_image1", item.imageUri1).putString(key + "_image2", item.imageUri2)
                .putString(key + "_source1", item.imageSource1.isEmpty() ? item.imageUri1 : item.imageSource1)
                .putString(key + "_source2", item.imageSource2.isEmpty() ? item.imageUri2 : item.imageSource2)
                .putString(key + "_attachments", item.attachmentsJson)
                .putBoolean(key + "_background", item.imagesAsBackground).apply();
    }
}
