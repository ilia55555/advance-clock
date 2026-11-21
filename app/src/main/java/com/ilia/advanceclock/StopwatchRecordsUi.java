package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Dialogs for saving, viewing and managing personal stopwatch records. */
final class StopwatchRecordsUi {
    private StopwatchRecordsUi() {}

    static String format(long millis) {
        long cs = millis / 10;
        return CalendarUtils.fa(String.format(Locale.US, "%02d:%02d:%02d.%02d",
                cs / 360000, (cs / 6000) % 60, (cs / 100) % 60, cs % 100));
    }

    /** Asks which activity the elapsed time belongs to, saves it and reports the result. */
    static void showSave(Activity host, long millis, String defaultName, Runnable onSaved) {
        StopwatchRecords store = new StopwatchRecords(host);
        List<StopwatchRecords.Activity> activities = store.all();
        ArrayList<String> names = new ArrayList<>();
        for (StopwatchRecords.Activity a : activities) names.add(a.name);

        LinearLayout box = column(host);
        TextView time = text(host, format(millis), 28, AppSettings.primaryColor(host));
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        time.setGravity(Gravity.CENTER);
        time.setTextDirection(View.TEXT_DIRECTION_LTR);
        box.addView(time, new LinearLayout.LayoutParams(-1, -2));

        AutoCompleteTextView name = new AutoCompleteTextView(host);
        name.setHint(AppString.get(R.string.stopwatch_record_activity_hint));
        name.setText(defaultName == null ? "" : defaultName.trim());
        name.setSingleLine(true);
        name.setThreshold(0);
        name.setTextColor(AppSettings.textPrimary(host));
        name.setHintTextColor(AppSettings.textSecondary(host));
        name.setBackgroundResource(R.drawable.bg_field);
        name.setPadding(dp(host, 14), 0, dp(host, 14), 0);
        name.setAdapter(new ArrayAdapter<>(host, android.R.layout.simple_dropdown_item_1line, names));
        name.setOnClickListener(v -> name.showDropDown());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(-1, dp(host, 56));
        nameParams.topMargin = dp(host, 12);
        box.addView(name, nameParams);

        CheckBox longer = new CheckBox(host);
        longer.setText(AppString.get(R.string.stopwatch_record_longer_better));
        longer.setTextColor(AppSettings.textPrimary(host));
        LinearLayout.LayoutParams longerParams = new LinearLayout.LayoutParams(-1, -2);
        longerParams.topMargin = dp(host, 8);
        box.addView(longer, longerParams);
        Runnable syncDirection = () -> {
            StopwatchRecords.Activity existing = store.find(name.getText().toString());
            longer.setEnabled(existing == null);
            if (existing != null) longer.setChecked(existing.longerBetter);
        };
        name.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable e) { syncDirection.run(); }
        });
        syncDirection.run();

        AlertDialog dialog = new MotionDialogs.Builder(host)
                .setTitle(AppString.get(R.string.stopwatch_record_save))
                .setView(box)
                .setNegativeButton(AppString.get(R.string.runtime_text_0003), null)
                .setPositiveButton(AppString.get(R.string.runtime_text_0005), null)
                .create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String value = name.getText().toString().trim();
            if (value.isEmpty()) {
                name.setError(AppString.get(R.string.stopwatch_record_activity_hint));
                return;
            }
            StopwatchRecords.Record previous = store.add(value, longer.isChecked(), millis);
            StopwatchRecords.Activity saved = store.find(value);
            String message;
            if (previous == null) {
                message = AppString.get(R.string.stopwatch_record_first);
            } else if (saved != null && saved.beats(millis, previous.millis)) {
                message = AppString.get(R.string.stopwatch_record_new_best) + "  "
                        + format(Math.abs(millis - previous.millis));
            } else {
                message = AppString.get(R.string.stopwatch_record_not_beaten) + "  " + format(previous.millis);
            }
            LogoToast.makeText(host, message, Toast.LENGTH_LONG).show();
            dialog.dismiss();
            if (onSaved != null) onSaved.run();
        }));
        dialog.show();
    }

    /** Lists activities with their best time; tap one to manage its records. */
    static void showList(Activity host, Runnable onChanged) {
        StopwatchRecords store = new StopwatchRecords(host);
        List<StopwatchRecords.Activity> activities = store.all();
        if (activities.isEmpty()) {
            new MotionDialogs.Builder(host)
                    .setTitle(AppString.get(R.string.stopwatch_records))
                    .setMessage(AppString.get(R.string.stopwatch_records_empty))
                    .setPositiveButton(AppString.get(R.string.runtime_text_0004), null)
                    .show();
            return;
        }
        java.util.Set<Long> filter = filter(host);
        LinearLayout list = column(host);
        AlertDialog[] holder = new AlertDialog[1];
        TextView filterInfo = text(host, filter.isEmpty()
                ? AppString.get(R.string.stopwatch_records_filter_all)
                : AppString.get(R.string.stopwatch_records_filter_some), 13, AppSettings.textSecondary(host));
        list.addView(filterInfo);
        for (StopwatchRecords.Activity a : activities) {
            if (!filter.isEmpty() && !filter.contains(a.id)) continue;
            StopwatchRecords.Record best = a.best();
            View row = row(host, a.name,
                    AppString.get(R.string.stopwatch_record_best) + "  " + (best == null ? "—" : format(best.millis))
                            + "   •   " + CalendarUtils.fa(String.valueOf(a.records.size())) + " "
                            + AppString.get(R.string.stopwatch_record_count));
            row.setOnClickListener(v -> {
                holder[0].dismiss();
                showActivity(host, a.id, onChanged);
            });
            list.addView(row, rowParams(host));
        }
        holder[0] = new MotionDialogs.Builder(host)
                .setTitle(AppString.get(R.string.stopwatch_records))
                .setView(scroll(host, list))
                .setNeutralButton(AppString.get(R.string.stopwatch_records_filter), (d, w) ->
                        chooseFilter(host, activities, onChanged))
                .setPositiveButton(AppString.get(R.string.runtime_text_0004), null)
                .create();
        holder[0].show();
    }

    private static final String FILTER_PREFS = "stopwatch_records";
    private static final String FILTER_KEY = "filter_ids";

    private static java.util.Set<Long> filter(Activity host) {
        java.util.HashSet<Long> out = new java.util.HashSet<>();
        String raw = host.getSharedPreferences(FILTER_PREFS, Activity.MODE_PRIVATE).getString(FILTER_KEY, "");
        for (String part : raw.split(",")) {
            try { if (!part.isEmpty()) out.add(Long.parseLong(part)); } catch (NumberFormatException ignored) {}
        }
        // Forget ids of deleted activities.
        java.util.HashSet<Long> existing = new java.util.HashSet<>();
        for (StopwatchRecords.Activity a : new StopwatchRecords(host).all()) existing.add(a.id);
        out.retainAll(existing);
        return out;
    }

    /** Lets the user show only some activities (none selected = show all). */
    private static void chooseFilter(Activity host, List<StopwatchRecords.Activity> activities, Runnable onChanged) {
        java.util.Set<Long> current = filter(host);
        String[] names = new String[activities.size()];
        boolean[] checked = new boolean[activities.size()];
        for (int i = 0; i < names.length; i++) {
            names[i] = activities.get(i).name;
            checked[i] = current.contains(activities.get(i).id);
        }
        new MotionDialogs.Builder(host)
                .setTitle(AppString.get(R.string.stopwatch_records_filter))
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setNeutralButton(AppString.get(R.string.stopwatch_records_filter_all), (d, w) -> {
                    saveFilter(host, new java.util.HashSet<>());
                    showList(host, onChanged);
                })
                .setPositiveButton(AppString.get(R.string.runtime_text_0004), (d, w) -> {
                    java.util.HashSet<Long> picked = new java.util.HashSet<>();
                    for (int i = 0; i < checked.length; i++) if (checked[i]) picked.add(activities.get(i).id);
                    saveFilter(host, picked);
                    showList(host, onChanged);
                })
                .setOnCancelListener(d -> showList(host, onChanged))
                .show();
    }

    private static void saveFilter(Activity host, java.util.Set<Long> ids) {
        StringBuilder out = new StringBuilder();
        for (Long id : ids) { if (out.length() > 0) out.append(','); out.append(id); }
        host.getSharedPreferences(FILTER_PREFS, Activity.MODE_PRIVATE).edit().putString(FILTER_KEY, out.toString()).apply();
    }

    private static void showActivity(Activity host, long id, Runnable onChanged) {
        StopwatchRecords store = new StopwatchRecords(host);
        List<StopwatchRecords.Activity> all = store.all();
        StopwatchRecords.Activity activity = null;
        for (StopwatchRecords.Activity a : all) if (a.id == id) activity = a;
        if (activity == null) { showList(host, onChanged); return; }
        final StopwatchRecords.Activity current = activity;

        ArrayList<StopwatchRecords.Record> sorted = new ArrayList<>(current.records);
        sorted.sort((x, y) -> current.longerBetter ? Long.compare(y.millis, x.millis) : Long.compare(x.millis, y.millis));

        LinearLayout list = column(host);
        TextView direction = text(host, AppString.get(current.longerBetter
                ? R.string.stopwatch_record_longer_better : R.string.stopwatch_record_shorter_better),
                13, AppSettings.textSecondary(host));
        list.addView(direction);
        AlertDialog[] holder = new AlertDialog[1];
        int rank = 1;
        for (StopwatchRecords.Record r : sorted) {
            String when = CalendarUtils.formatDate(r.at, AppSettings.defaultCalendar(host))
                    + "  " + TimeFormat.clock(host, r.at);
            View row = row(host, (rank == 1 ? "🏆 " : CalendarUtils.fa(String.valueOf(rank)) + ".  ") + format(r.millis), when);
            row.setOnClickListener(v -> new MotionDialogs.Builder(host)
                    .setMessage(AppString.get(R.string.stopwatch_record_delete_confirm))
                    .setNegativeButton(AppString.get(R.string.runtime_text_0003), null)
                    .setPositiveButton(AppString.get(R.string.stopwatch_record_delete), (d, w) -> {
                        current.records.remove(r);
                        store.save(all);
                        holder[0].dismiss();
                        if (onChanged != null) onChanged.run();
                        showActivity(host, id, onChanged);
                    }).show());
            list.addView(row, rowParams(host));
            rank++;
        }

        holder[0] = new MotionDialogs.Builder(host)
                .setTitle(current.name)
                .setView(scroll(host, list))
                .setNeutralButton(AppString.get(R.string.stopwatch_record_manage), (d, w) ->
                        manage(host, current, all, store, onChanged))
                .setPositiveButton(AppString.get(R.string.stopwatch_records_back), (d, w) -> showList(host, onChanged))
                .create();
        holder[0].show();
    }

    private static void manage(Activity host, StopwatchRecords.Activity activity,
                               List<StopwatchRecords.Activity> all, StopwatchRecords store, Runnable onChanged) {
        String[] options = {
                AppString.get(R.string.stopwatch_record_rename),
                AppString.get(activity.longerBetter ? R.string.stopwatch_record_shorter_better
                        : R.string.stopwatch_record_longer_better),
                AppString.get(R.string.stopwatch_record_delete_activity)};
        new MotionDialogs.Builder(host).setTitle(activity.name).setItems(options, (d, which) -> {
            if (which == 0) {
                EditText input = new EditText(host);
                input.setText(activity.name);
                input.setSingleLine(true);
                input.setSelectAllOnFocus(true);
                new MotionDialogs.Builder(host).setTitle(options[0]).setView(input)
                        .setNegativeButton(AppString.get(R.string.runtime_text_0003), null)
                        .setPositiveButton(AppString.get(R.string.runtime_text_0005), (x, y) -> {
                            String value = input.getText().toString().trim();
                            if (!value.isEmpty()) { activity.name = value; store.save(all); }
                            if (onChanged != null) onChanged.run();
                            showActivity(host, activity.id, onChanged);
                        }).show();
            } else if (which == 1) {
                activity.longerBetter = !activity.longerBetter;
                store.save(all);
                if (onChanged != null) onChanged.run();
                showActivity(host, activity.id, onChanged);
            } else {
                AlertDialog confirm = new MotionDialogs.Builder(host)
                        .setMessage(AppString.get(R.string.stopwatch_record_delete_activity_confirm))
                        .setNegativeButton(AppString.get(R.string.runtime_text_0003), null)
                        .setPositiveButton(AppString.get(R.string.stopwatch_record_delete), (x, y) -> {
                            all.remove(activity);
                            store.save(all);
                            if (onChanged != null) onChanged.run();
                            showList(host, onChanged);
                        }).create();
                confirm.setOnShowListener(s -> ButtonStyles.destructive(confirm.getButton(AlertDialog.BUTTON_POSITIVE)));
                confirm.show();
            }
        }).show();
    }

    private static View row(Activity host, String title, String subtitle) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setBackgroundResource(R.drawable.bg_field);
        row.setPadding(dp(host, 14), dp(host, 10), dp(host, 14), dp(host, 10));
        row.setClickable(true);
        row.setFocusable(true);
        TextView t = text(host, title, 16, AppSettings.textPrimary(host));
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        row.addView(t);
        TextView s = text(host, subtitle, 12, AppSettings.textSecondary(host));
        s.setPadding(0, dp(host, 3), 0, 0);
        row.addView(s);
        return row;
    }

    private static LinearLayout.LayoutParams rowParams(Activity host) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(host, 8);
        return p;
    }

    private static LinearLayout column(Activity host) {
        LinearLayout box = new LinearLayout(host);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setLayoutDirection(AppSettings.layoutDirection(host));
        box.setPadding(dp(host, 20), dp(host, 8), dp(host, 20), dp(host, 4));
        return box;
    }

    private static ScrollView scroll(Activity host, View content) {
        ScrollView scroll = new ScrollView(host);
        scroll.addView(content);
        return scroll;
    }

    private static TextView text(Activity host, String value, int size, int color) {
        TextView view = new TextView(host);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        return view;
    }

    private static int dp(Activity host, int value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }
}
