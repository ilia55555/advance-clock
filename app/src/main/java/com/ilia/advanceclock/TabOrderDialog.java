package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public final class TabOrderDialog {
    private TabOrderDialog() {}

    public static void show(Context context, Runnable onSaved) {
        ArrayList<String> order = new ArrayList<>(Arrays.asList(AppSettings.tabOrder(context)));
        LinearLayout list = new LinearLayout(context);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(dp(context, 16), dp(context, 8), dp(context, 16), dp(context, 8));
        list.setLayoutDirection(android.view.View.LAYOUT_DIRECTION_RTL);
        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            list.removeAllViews();
            for (int i = 0; i < order.size(); i++) {
                int index = i;
                LinearLayout row = new LinearLayout(context);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setOrientation(LinearLayout.HORIZONTAL);
                TextView title = new TextView(context);
                title.setText((i + 1) + ". " + title(order.get(i)));
                title.setTextColor(AppSettings.textPrimary(context));
                title.setTextSize(16);
                title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                row.addView(title, new LinearLayout.LayoutParams(0, dp(context, 52), 1f));
                Button up = moveButton(context, "↑");
                up.setEnabled(i > 0);
                up.setOnClickListener(v -> {
                    Collections.swap(order, index, index - 1);
                    render[0].run();
                });
                row.addView(up, new LinearLayout.LayoutParams(dp(context, 48), dp(context, 44)));
                Button down = moveButton(context, "↓");
                down.setEnabled(i < order.size() - 1);
                down.setOnClickListener(v -> {
                    Collections.swap(order, index, index + 1);
                    render[0].run();
                });
                row.addView(down, new LinearLayout.LayoutParams(dp(context, 48), dp(context, 44)));
                list.addView(row);
            }
        };
        render[0].run();
        new AlertDialog.Builder(context)
                .setTitle("ترتیب تب‌ها")
                .setMessage("با دکمه‌های بالا و پایین، جای هر تب را تغییر دهید.")
                .setView(list)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره", (dialog, which) -> {
                    AppSettings.setTabOrder(context, order);
                    onSaved.run();
                })
                .show();
    }

    private static Button moveButton(Context context, String text) {
        Button button = new Button(context);
        button.setText(text);
        button.setTextSize(20);
        button.setTextColor(AppSettings.primaryColor(context));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        return button;
    }

    private static String title(String tab) {
        switch (tab) {
            case "noforget": return "یادداشت‌ها";
            case "stopwatch": return "کرنومتر";
            case "timer": return "تایمر";
            case "world": return "ساعت جهانی";
            default: return "ساعت";
        }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
