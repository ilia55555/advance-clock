package com.ilia.advanceclock;

import android.app.Activity;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

final class WorldClockPanelController {
    private final Activity host;
    private final LinearLayout list;
    private final Spinner spinner;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final String[] availableZones;
    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            render();
            long now = System.currentTimeMillis();
            handler.postDelayed(this, 60_000L - now % 60_000L + 50L);
        }
    };

    WorldClockPanelController(Activity host, View root) {
        this.host = host;
        list = root.findViewById(R.id.world_clock_list);
        spinner = root.findViewById(R.id.world_zone_spinner);
        availableZones = TimeZone.getAvailableIDs();
        Arrays.sort(availableZones);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(host,
                android.R.layout.simple_spinner_item, availableZones);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        root.findViewById(R.id.world_add).setOnClickListener(v -> addSelected());
        render();
    }

    private void addSelected() {
        String zone = (String) spinner.getSelectedItem();
        List<String> zones = WorldClockStore.zones(host);
        if (zones.contains(zone)) {
            Toast.makeText(host, "این منطقه زمانی قبلاً اضافه شده است", Toast.LENGTH_SHORT).show();
            return;
        }
        zones.add(zone);
        WorldClockStore.save(host, zones);
        render();
    }

    private void render() {
        list.removeAllViews();
        List<String> zones = WorldClockStore.zones(host);
        long now = System.currentTimeMillis();
        for (String zoneId : zones) {
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            row.setBackgroundResource(R.drawable.bg_card);

            TextView name = new TextView(host);
            name.setText(zoneId.replace('_', ' '));
            name.setTextColor(AppSettings.textPrimary(host));
            name.setTextSize(15);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            row.addView(name, new LinearLayout.LayoutParams(0, dp(54), 1f));

            SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.getDefault());
            format.setTimeZone(TimeZone.getTimeZone(zoneId));
            TextView time = new TextView(host);
            time.setText(format.format(new Date(now)));
            time.setTextColor(AppSettings.primaryColor(host));
            time.setTextSize(23);
            time.setGravity(Gravity.CENTER);
            row.addView(time, new LinearLayout.LayoutParams(dp(86), dp(54)));

            Button remove = new Button(host);
            remove.setText("حذف");
            remove.setAllCaps(false);
            remove.setTextColor(AppSettings.primaryColor(host));
            remove.setBackgroundResource(R.drawable.bg_soft_button);
            remove.setOnClickListener(v -> {
                List<String> updated = WorldClockStore.zones(host);
                if (updated.size() == 1) {
                    Toast.makeText(host, "حداقل یک ساعت جهانی باید باقی بماند", Toast.LENGTH_SHORT).show();
                    return;
                }
                updated.remove(zoneId);
                WorldClockStore.save(host, updated);
                render();
            });
            row.addView(remove, new LinearLayout.LayoutParams(dp(72), dp(44)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            list.addView(row, lp);
        }
    }

    void onResume() {
        handler.removeCallbacks(ticker);
        handler.post(ticker);
    }

    void onPause() {
        handler.removeCallbacks(ticker);
    }

    private int dp(int value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }
}
