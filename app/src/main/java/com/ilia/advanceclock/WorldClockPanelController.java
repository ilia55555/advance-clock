package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class WorldClockPanelController {
    private static final Locale PERSIAN = new Locale("fa");
    private final Activity host;
    private final LinearLayout list;
    private final TextView referenceSummary;
    private final Button nowButton;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ArrayList<ZoneOption> allZones = new ArrayList<>();
    private final ArrayList<ZoneOption> filteredZones = new ArrayList<>();
    private long referenceMillis;
    private boolean referenceMode;

    private final Runnable ticker = new Runnable() {
        @Override public void run() {
            renderClocks();
            long now = System.currentTimeMillis();
            handler.postDelayed(this, 60_000L - now % 60_000L + 50L);
        }
    };

    WorldClockPanelController(Activity host, View root) {
        this.host = host;
        list = root.findViewById(R.id.world_clock_list);
        referenceSummary = root.findViewById(R.id.world_reference_summary);
        nowButton = root.findViewById(R.id.world_reference_now);
        buildZoneIndex();

        root.findViewById(R.id.world_fab).setOnClickListener(v -> showAddDialog());
        root.findViewById(R.id.world_pick_reference).setOnClickListener(v -> pickReferenceDate());
        root.findViewById(R.id.world_alarm).setOnClickListener(v ->
                host.startActivity(new android.content.Intent(host, AlarmEditorActivity.class)
                        .putExtra("modalCreate", true)));
        nowButton.setOnClickListener(v -> {
            referenceMode = false;
            referenceMillis = 0L;
            renderClocks();
        });
        renderClocks();
    }

    private void buildZoneIndex() {
        Map<String, LinkedHashSet<String>> aliases = new HashMap<>();
        for (String id : TimeZone.getAvailableIDs()) {
            String canonical = android.icu.util.TimeZone.getCanonicalID(id);
            if (canonical == null || canonical.isEmpty()) canonical = id;
            aliases.computeIfAbsent(canonical, ignored -> new LinkedHashSet<>()).add(id);
        }
        for (Map.Entry<String, LinkedHashSet<String>> entry : aliases.entrySet()) {
            String id = entry.getKey();
            String city = cityName(id);
            String continent = continentName(id);
            String countryEnglish = "";
            String countryPersian = "";
            try {
                String region = android.icu.util.TimeZone.getRegion(id);
                if (region != null && !"001".equals(region)) {
                    Locale country = new Locale("", region);
                    countryEnglish = country.getDisplayCountry(Locale.ENGLISH);
                    countryPersian = country.getDisplayCountry(PERSIAN);
                }
            } catch (IllegalArgumentException ignored) {
            }
            String aliasText = android.text.TextUtils.join(" ", entry.getValue());
            String extra = explicitSearchAliases(id);
            String searchText = normalize(id + " " + city + " " + continent + " "
                    + countryEnglish + " " + countryPersian + " " + aliasText + " " + extra);
            String label = city;
            if (!countryPersian.isEmpty()) label += " • " + countryPersian;
            else if (!countryEnglish.isEmpty()) label += " • " + countryEnglish;
            label += " • " + continent;
            allZones.add(new ZoneOption(id, label, searchText));
        }
        Collections.sort(allZones, (a, b) -> a.label.compareToIgnoreCase(b.label));
    }

    private void showAddDialog() {
        LinearLayout content = new LinearLayout(host);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(10), dp(18), 0);

        TextView title = text("افزودن منطقه زمانی", 19, AppSettings.textPrimary(host));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView hint = text("شهر، استان، کشور یا قاره را جست‌وجو کنید", 12,
                AppSettings.textSecondary(host));
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(-1, -2);
        hintParams.topMargin = dp(3);
        content.addView(hint, hintParams);

        EditText dialogSearch = new EditText(host);
        dialogSearch.setSingleLine(true);
        dialogSearch.setHint("جست‌وجوی شهر، استان، کشور یا قاره");
        dialogSearch.setTextColor(AppSettings.textPrimary(host));
        dialogSearch.setHintTextColor(AppSettings.textSecondary(host));
        dialogSearch.setBackgroundResource(R.drawable.bg_field);
        dialogSearch.setPadding(dp(12), 0, dp(12), 0);
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(-1, dp(56));
        searchParams.topMargin = dp(12);
        content.addView(dialogSearch, searchParams);

        Spinner dialogSpinner = new Spinner(host);
        dialogSpinner.setBackgroundResource(R.drawable.bg_field);
        dialogSpinner.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(-1, dp(58));
        spinnerParams.topMargin = dp(8);
        content.addView(dialogSpinner, spinnerParams);

        Button addButton = new Button(host);
        addButton.setText("افزودن ساعت انتخاب‌شده");
        addButton.setTextColor(0xFFFFFFFF);
        addButton.setTextSize(15);
        addButton.setAllCaps(false);
        addButton.setBackgroundResource(R.drawable.bg_teal_button);
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(56));
        buttonParams.topMargin = dp(10);
        content.addView(addButton, buttonParams);

        AlertDialog dialog = new AlertDialog.Builder(host)
                .setView(content)
                .setNegativeButton("انصراف", null)
                .create();

        dialogSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterZones(dialogSpinner, s == null ? "" : s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        filterZones(dialogSpinner, "");
        addButton.setOnClickListener(v -> {
            if (addSelected(dialogSpinner)) dialog.dismiss();
        });
        dialog.show();
    }

    private void filterZones(Spinner spinner, String query) {
        String normalized = normalize(query);
        filteredZones.clear();
        for (ZoneOption option : allZones) {
            if (normalized.isEmpty() || option.searchText.contains(normalized)) {
                filteredZones.add(option);
            }
        }
        ArrayList<String> labels = new ArrayList<>();
        for (ZoneOption option : filteredZones) labels.add(option.label);
        if (labels.isEmpty()) labels.add("نتیجه‌ای پیدا نشد");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(host,
                android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private boolean addSelected(Spinner spinner) {
        int position = spinner.getSelectedItemPosition();
        if (position < 0 || position >= filteredZones.size()) {
            Toast.makeText(host, "ابتدا یک شهر، استان، کشور یا قاره را جست‌وجو کنید",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        String zone = filteredZones.get(position).id;
        List<String> zones = WorldClockStore.zones(host);
        if (zones.contains(zone)) {
            Toast.makeText(host, "این منطقه زمانی قبلاً اضافه شده است", Toast.LENGTH_SHORT).show();
            return false;
        }
        zones.add(zone);
        WorldClockStore.save(host, zones);
        renderClocks();
        return true;
    }

    private void pickReferenceDate() {
        long initial = referenceMode ? referenceMillis : System.currentTimeMillis();
        CalendarPickerDialog.showDateAny(host, initial, AppSettings.defaultCalendar(host),
                (picked, calendarType) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.setTimeInMillis(picked);
                    Calendar initialTime = Calendar.getInstance();
                    initialTime.setTimeInMillis(initial);
                    selected.set(Calendar.HOUR_OF_DAY, initialTime.get(Calendar.HOUR_OF_DAY));
                    selected.set(Calendar.MINUTE, initialTime.get(Calendar.MINUTE));
                    new TimePickerDialog(host, (view, hour, minute) -> {
                        selected.set(Calendar.HOUR_OF_DAY, hour);
                        selected.set(Calendar.MINUTE, minute);
                        selected.set(Calendar.SECOND, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        referenceMillis = selected.getTimeInMillis();
                        referenceMode = true;
                        renderClocks();
                    }, initialTime.get(Calendar.HOUR_OF_DAY),
                            initialTime.get(Calendar.MINUTE), true).show();
                });
    }

    private void renderClocks() {
        list.removeAllViews();
        List<String> zones = WorldClockStore.zones(host);
        long shownMillis = referenceMode ? referenceMillis : System.currentTimeMillis();
        referenceSummary.setText(referenceMode
                ? "تبدیل زمان محلی شما: " + localDateTime(shownMillis)
                : "زمان فعلی در مناطق انتخاب‌شده");
        nowButton.setVisibility(referenceMode ? View.VISIBLE : View.GONE);

        for (String zoneId : zones) {
            LinearLayout row = new LinearLayout(host);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(14), dp(10), dp(14), dp(10));
            row.setBackgroundResource(R.drawable.bg_card);

            LinearLayout details = new LinearLayout(host);
            details.setOrientation(LinearLayout.VERTICAL);
            TextView name = text(cityName(zoneId), 16, AppSettings.textPrimary(host));
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            details.addView(name);
            TextView geography = text(geography(zoneId), 11, AppSettings.textSecondary(host));
            details.addView(geography);
            row.addView(details, new LinearLayout.LayoutParams(0, dp(58), 1f));

            TimeZone zone = TimeZone.getTimeZone(zoneId);
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            timeFormat.setTimeZone(zone);
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE، d MMM yyyy", Locale.getDefault());
            dateFormat.setTimeZone(zone);
            LinearLayout converted = new LinearLayout(host);
            converted.setOrientation(LinearLayout.VERTICAL);
            converted.setGravity(Gravity.CENTER);
            TextView time = text(timeFormat.format(new Date(shownMillis)), 23,
                    AppSettings.primaryColor(host));
            time.setGravity(Gravity.CENTER);
            converted.addView(time);
            TextView date = text(dateFormat.format(new Date(shownMillis)), 11,
                    AppSettings.textSecondary(host));
            date.setGravity(Gravity.CENTER);
            converted.addView(date);
            row.addView(converted, new LinearLayout.LayoutParams(dp(122), dp(58)));

            ImageButton remove = new ImageButton(host);
            remove.setImageResource(R.drawable.ic_delete_red);
            remove.setBackgroundResource(R.drawable.bg_delete_outline);
            remove.setContentDescription("حذف " + cityName(zoneId));
            remove.setPadding(dp(5), dp(5), dp(5), dp(5));
            boolean[] deleteArmed = {false};
            remove.setOnClickListener(v -> {
                if (!deleteArmed[0]) {
                    deleteArmed[0] = true;
                    remove.setImageResource(R.drawable.ic_md_delete);
                    remove.setBackgroundResource(R.drawable.bg_delete_confirm);
                    remove.setContentDescription("تأیید حذف " + cityName(zoneId));
                    Toast.makeText(host, "برای تأیید حذف دوباره بزنید", Toast.LENGTH_SHORT).show();
                    return;
                }
                removeZone(zoneId);
            });
            row.addView(remove, new LinearLayout.LayoutParams(dp(20), dp(20)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = dp(8);
            list.addView(row, lp);
        }
    }

    private void removeZone(String zoneId) {
        List<String> updated = WorldClockStore.zones(host);
        if (updated.size() == 1) {
            Toast.makeText(host, "حداقل یک ساعت جهانی باید باقی بماند", Toast.LENGTH_SHORT).show();
            return;
        }
        updated.remove(zoneId);
        WorldClockStore.save(host, updated);
        renderClocks();
    }

    private String geography(String id) {
        String country = "";
        try {
            String region = android.icu.util.TimeZone.getRegion(id);
            if (region != null && !"001".equals(region)) {
                country = new Locale("", region).getDisplayCountry(PERSIAN);
            }
        } catch (IllegalArgumentException ignored) {
        }
        String continent = continentName(id);
        return country.isEmpty() ? continent : country + " • " + continent;
    }

    private String cityName(String id) {
        if ("America/Regina".equals(id) || "Canada/Saskatchewan".equals(id)) return "Regina";
        int slash = id.lastIndexOf('/');
        return (slash >= 0 ? id.substring(slash + 1) : id).replace('_', ' ');
    }

    private String continentName(String id) {
        String prefix = id.contains("/") ? id.substring(0, id.indexOf('/')) : id;
        switch (prefix) {
            case "Asia": return "آسیا";
            case "Europe": return "اروپا";
            case "Africa": return "آفریقا";
            case "America": return "آمریکا";
            case "Australia": return "استرالیا";
            case "Pacific": return "اقیانوسیه";
            case "Atlantic": return "اقیانوس اطلس";
            case "Indian": return "اقیانوس هند";
            case "Antarctica": return "جنوبگان";
            default: return prefix;
        }
    }

    private String explicitSearchAliases(String id) {
        if ("America/Regina".equals(id)) return "Regina رجاینا Saskatchewan ساسکاچوان";
        if ("Asia/Tehran".equals(id)) return "Tehran تهران Iran ایران";
        if ("Asia/Kuwait".equals(id)) return "Kuwait کویت";
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT)
                .replace('ي', 'ی').replace('ك', 'ک');
    }

    private String localDateTime(long millis) {
        return new SimpleDateFormat("yyyy/MM/dd  HH:mm", Locale.getDefault())
                .format(new Date(millis));
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(host);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
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

    private static final class ZoneOption {
        final String id;
        final String label;
        final String searchText;

        ZoneOption(String id, String label, String searchText) {
            this.id = id;
            this.label = label;
            this.searchText = searchText;
        }
    }
}
