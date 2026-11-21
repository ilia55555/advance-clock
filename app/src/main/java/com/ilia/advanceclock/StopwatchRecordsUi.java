package com.ilia.advanceclock;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** App-styled sheets for saving, viewing, filtering and managing stopwatch records. */
final class StopwatchRecordsUi {
    interface Saved { void onSaved(String category); }

    private StopwatchRecordsUi() {}

    static String format(long millis) {
        long cs = millis / 10;
        return CalendarUtils.fa(String.format(Locale.US, "%02d:%02d:%02d.%02d",
                cs / 360000, (cs / 6000) % 60, (cs / 100) % 60, cs % 100));
    }

    static String uncategorized() { return AppString.get(R.string.stopwatch_record_uncategorized); }

    /** Saves straight into a known category and reports whether the record was beaten. */
    static void saveDirect(Activity host, long millis, String category, boolean longerIfNew) {
        saveDirect(host, millis, category, longerIfNew, "");
    }

    static void saveDirect(Activity host, long millis, String category, boolean longerIfNew, String title) {
        StopwatchRecords store = new StopwatchRecords(host);
        StopwatchRecords.Record previous = store.add(category, longerIfNew, millis, title);
        StopwatchRecords.Activity saved = store.find(category);
        String message;
        if (previous == null) {
            message = AppString.get(R.string.stopwatch_record_first);
        } else if (saved != null && saved.beats(millis, previous.millis)) {
            message = AppString.get(R.string.stopwatch_record_new_best) + "  " + format(Math.abs(millis - previous.millis));
        } else {
            message = AppString.get(R.string.stopwatch_record_not_beaten) + "  " + format(previous.millis);
        }
        LogoToast.makeText(host, "«" + category + "»  " + message, Toast.LENGTH_LONG).show();
    }

    /** Asks for the (optional) category, saves the time and returns the category used. */
    static void showSave(Activity host, long millis, String defaultName, Saved onSaved) {
        StopwatchRecords store = new StopwatchRecords(host);
        ArrayList<String> names = new ArrayList<>();
        for (StopwatchRecords.Activity a : store.all()) names.add(a.name);

        Sheet sheet = new Sheet(host, AppString.get(R.string.stopwatch_record_save));
        TextView time = text(host, format(millis), 30, AppSettings.primaryColor(host));
        time.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        time.setGravity(Gravity.CENTER);
        time.setTextDirection(View.TEXT_DIRECTION_LTR);
        sheet.content.addView(time, new LinearLayout.LayoutParams(-1, -2));

        EditText titleField = new EditText(host);
        titleField.setHint(AppString.get(R.string.stopwatch_record_title_hint));
        titleField.setSingleLine(true);
        titleField.setTextColor(AppSettings.textPrimary(host));
        titleField.setHintTextColor(AppSettings.textSecondary(host));
        titleField.setBackgroundResource(R.drawable.bg_field);
        titleField.setPadding(dp(host, 14), 0, dp(host, 14), 0);
        sheet.content.addView(titleField, params(host, 56, 12));

        AutoCompleteTextView name = new AutoCompleteTextView(host);
        name.setHint(AppString.get(R.string.stopwatch_record_activity_hint));
        name.setText(defaultName == null ? "" : defaultName.trim());
        name.setSingleLine(true);
        name.setThreshold(0);
        name.setTextColor(AppSettings.textPrimary(host));
        name.setHintTextColor(AppSettings.textSecondary(host));
        name.setBackgroundResource(R.drawable.bg_field);
        name.setPadding(dp(host, 14), 0, dp(host, 14), 0);
        name.setAdapter(new CategoryAdapter(host, names));
        // Show three rows at a time; the rest scroll.
        name.setDropDownHeight(dp(host, 48) * Math.min(3, Math.max(1, names.size())));
        name.setDropDownBackgroundDrawable(fieldShape(host));
        Runnable open = () -> { if (!names.isEmpty()) name.showDropDown(); };
        name.setOnClickListener(v -> open.run());
        name.setOnFocusChangeListener((v, focused) -> { if (focused) v.post(open); });
        sheet.content.addView(name, params(host, 56, 8));

        // "Shorter is better"  [switch]  "Longer is better", styled like the adhan rows.
        LinearLayout direction = new LinearLayout(host);
        direction.setOrientation(LinearLayout.HORIZONTAL);
        direction.setGravity(Gravity.CENTER_VERTICAL);
        direction.setLayoutDirection(AppSettings.layoutDirection(host));
        direction.setBackgroundResource(R.drawable.bg_field);
        direction.setPadding(dp(host, 12), dp(host, 4), dp(host, 12), dp(host, 4));
        TextView shorterLabel = text(host, AppString.get(R.string.stopwatch_record_shorter_better), 13, AppSettings.textPrimary(host));
        shorterLabel.setGravity(Gravity.CENTER);
        TextView longerLabel = text(host, AppString.get(R.string.stopwatch_record_longer_better), 13, AppSettings.textPrimary(host));
        longerLabel.setGravity(Gravity.CENTER);
        android.widget.Switch longer = new android.widget.Switch(host);
        direction.addView(shorterLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        direction.addView(longer, new LinearLayout.LayoutParams(dp(host, 60), dp(host, 52)));
        direction.addView(longerLabel, new LinearLayout.LayoutParams(0, -2, 1f));
        shorterLabel.setOnClickListener(v -> { if (longer.isEnabled()) longer.setChecked(false); });
        longerLabel.setOnClickListener(v -> { if (longer.isEnabled()) longer.setChecked(true); });
        Runnable paintDirection = () -> {
            boolean on = longer.isChecked();
            shorterLabel.setTextColor(on ? AppSettings.textSecondary(host) : AppSettings.primaryColor(host));
            longerLabel.setTextColor(on ? AppSettings.primaryColor(host) : AppSettings.textSecondary(host));
            shorterLabel.setTypeface(Typeface.DEFAULT, on ? Typeface.NORMAL : Typeface.BOLD);
            longerLabel.setTypeface(Typeface.DEFAULT, on ? Typeface.BOLD : Typeface.NORMAL);
            direction.setAlpha(longer.isEnabled() ? 1f : 0.6f);
        };
        longer.setOnCheckedChangeListener((b, c) -> paintDirection.run());
        sheet.content.addView(direction, params(host, -2, 8));
        Runnable sync = () -> {
            StopwatchRecords.Activity existing = store.find(category(name.getText().toString()));
            longer.setEnabled(existing == null);
            if (existing != null) longer.setChecked(existing.longerBetter);
            paintDirection.run();
        };
        name.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(android.text.Editable e) { sync.run(); }
        });
        sync.run();

        sheet.action(AppString.get(R.string.runtime_text_0003), false, v -> sheet.dialog.dismiss());
        sheet.action(AppString.get(R.string.runtime_text_0005), true, v -> {
            String value = category(name.getText().toString());
            saveDirect(host, millis, value, longer.isChecked(), titleField.getText().toString().trim());
            sheet.dialog.dismiss();
            if (onSaved != null) onSaved.onSaved(value);
        });
        sheet.show();
    }

    private static String category(String raw) {
        String value = raw == null ? "" : raw.trim();
        return value.isEmpty() ? uncategorized() : value;
    }

    /** Lists activities with their best time; tap one to see and manage its records. */
    static void showList(Activity host, Runnable onChanged) {
        StopwatchRecords store = new StopwatchRecords(host);
        List<StopwatchRecords.Activity> activities = store.all();
        Sheet sheet = new Sheet(host, AppString.get(R.string.stopwatch_records));
        if (activities.isEmpty()) {
            sheet.content.addView(text(host, AppString.get(R.string.stopwatch_records_empty), 14,
                    AppSettings.textSecondary(host)));
            sheet.action(AppString.get(R.string.stopwatch_records_close), false, v -> sheet.dialog.dismiss());
            sheet.show();
            return;
        }
        java.util.Set<Long> filter = filter(host);
        sheet.content.addView(text(host, filter.isEmpty()
                ? AppString.get(R.string.stopwatch_records_filter_all)
                : AppString.get(R.string.stopwatch_records_filter_some), 13, AppSettings.textSecondary(host)));
        for (StopwatchRecords.Activity a : activities) {
            if (!filter.isEmpty() && !filter.contains(a.id)) continue;
            StopwatchRecords.Record best = a.best();
            View row = card(host, a.name,
                    AppString.get(R.string.stopwatch_record_best) + "  " + (best == null ? "—" : format(best.millis))
                            + "   •   " + CalendarUtils.fa(String.valueOf(a.records.size())) + " "
                            + AppString.get(R.string.stopwatch_record_count), true);
            row.setOnClickListener(v -> {
                sheet.dialog.dismiss();
                showActivity(host, a.id, onChanged);
            });
            sheet.content.addView(row, params(host, -2, 8));
        }
        sheet.action(AppString.get(R.string.stopwatch_records_close), false, v -> sheet.dialog.dismiss());
        sheet.action(AppString.get(R.string.stopwatch_records_filter), true, v -> {
            sheet.dialog.dismiss();
            chooseFilter(host, activities, onChanged);
        });
        sheet.show();
    }

    private static void showActivity(Activity host, long id, Runnable onChanged) {
        StopwatchRecords store = new StopwatchRecords(host);
        List<StopwatchRecords.Activity> all = store.all();
        StopwatchRecords.Activity found = null;
        for (StopwatchRecords.Activity a : all) if (a.id == id) found = a;
        if (found == null) { showList(host, onChanged); return; }
        final StopwatchRecords.Activity current = found;

        ArrayList<StopwatchRecords.Record> sorted = new ArrayList<>(current.records);
        sorted.sort((x, y) -> current.longerBetter ? Long.compare(y.millis, x.millis) : Long.compare(x.millis, y.millis));

        Sheet sheet = new Sheet(host, current.name);
        sheet.content.addView(text(host, AppString.get(current.longerBetter
                ? R.string.stopwatch_record_longer_better : R.string.stopwatch_record_shorter_better),
                13, AppSettings.textSecondary(host)));
        int rank = 1;
        for (StopwatchRecords.Record r : sorted) {
            String when = CalendarUtils.formatDate(r.at, AppSettings.defaultCalendar(host))
                    + "  " + TimeFormat.clock(host, r.at);
            String subtitle = r.title.isEmpty() ? when : r.title + "   •   " + when;
            View row = card(host, (rank == 1 ? "🏆 " : CalendarUtils.fa(String.valueOf(rank)) + ".  ") + format(r.millis), subtitle, false);
            row.setOnClickListener(v -> confirm(host, AppString.get(R.string.stopwatch_record_delete_confirm), () -> {
                current.records.remove(r);
                store.save(all);
                sheet.dialog.dismiss();
                if (onChanged != null) onChanged.run();
                showActivity(host, id, onChanged);
            }));
            sheet.content.addView(row, params(host, -2, 8));
            rank++;
        }
        sheet.action(AppString.get(R.string.stopwatch_records_back), false, v -> {
            sheet.dialog.dismiss();
            showList(host, onChanged);
        });
        sheet.action(AppString.get(R.string.stopwatch_record_manage), true, v -> {
            sheet.dialog.dismiss();
            manage(host, current, all, store, onChanged);
        });
        sheet.show();
    }

    private static void manage(Activity host, StopwatchRecords.Activity activity,
                               List<StopwatchRecords.Activity> all, StopwatchRecords store, Runnable onChanged) {
        Sheet sheet = new Sheet(host, activity.name);
        View rename = card(host, AppString.get(R.string.stopwatch_record_rename), "", true);
        View direction = card(host, AppString.get(activity.longerBetter ? R.string.stopwatch_record_shorter_better
                : R.string.stopwatch_record_longer_better), "", true);
        View delete = card(host, AppString.get(R.string.stopwatch_record_delete_activity), "", true);
        sheet.content.addView(rename, params(host, -2, 8));
        sheet.content.addView(direction, params(host, -2, 8));
        sheet.content.addView(delete, params(host, -2, 8));
        rename.setOnClickListener(v -> {
            sheet.dialog.dismiss();
            Sheet edit = new Sheet(host, AppString.get(R.string.stopwatch_record_rename));
            EditText input = new EditText(host);
            input.setText(activity.name);
            input.setSingleLine(true);
            input.setSelectAllOnFocus(true);
            input.setTextColor(AppSettings.textPrimary(host));
            input.setBackgroundResource(R.drawable.bg_field);
            input.setPadding(dp(host, 14), 0, dp(host, 14), 0);
            edit.content.addView(input, params(host, 56, 4));
            edit.action(AppString.get(R.string.runtime_text_0003), false, x -> {
                edit.dialog.dismiss();
                showActivity(host, activity.id, onChanged);
            });
            edit.action(AppString.get(R.string.runtime_text_0005), true, x -> {
                String value = input.getText().toString().trim();
                if (!value.isEmpty()) { activity.name = value; store.save(all); }
                edit.dialog.dismiss();
                if (onChanged != null) onChanged.run();
                showActivity(host, activity.id, onChanged);
            });
            edit.show();
        });
        direction.setOnClickListener(v -> {
            activity.longerBetter = !activity.longerBetter;
            store.save(all);
            sheet.dialog.dismiss();
            if (onChanged != null) onChanged.run();
            showActivity(host, activity.id, onChanged);
        });
        delete.setOnClickListener(v -> confirm(host, AppString.get(R.string.stopwatch_record_delete_activity_confirm), () -> {
            all.remove(activity);
            store.save(all);
            sheet.dialog.dismiss();
            if (onChanged != null) onChanged.run();
            showList(host, onChanged);
        }));
        sheet.action(AppString.get(R.string.stopwatch_records_back), false, v -> {
            sheet.dialog.dismiss();
            showActivity(host, activity.id, onChanged);
        });
        sheet.show();
    }

    private static void confirm(Activity host, String message, Runnable onYes) {
        Sheet sheet = new Sheet(host, message);
        sheet.action(AppString.get(R.string.runtime_text_0003), false, v -> sheet.dialog.dismiss());
        Button yes = sheet.action(AppString.get(R.string.stopwatch_record_delete), true, v -> {
            sheet.dialog.dismiss();
            onYes.run();
        });
        ButtonStyles.destructive(yes);
        sheet.show();
    }

    private static final String FILTER_PREFS = "stopwatch_records";
    private static final String FILTER_KEY = "filter_ids";

    private static java.util.Set<Long> filter(Activity host) {
        java.util.HashSet<Long> out = new java.util.HashSet<>();
        String raw = host.getSharedPreferences(FILTER_PREFS, Activity.MODE_PRIVATE).getString(FILTER_KEY, "");
        for (String part : raw.split(",")) {
            try { if (!part.isEmpty()) out.add(Long.parseLong(part)); } catch (NumberFormatException ignored) {}
        }
        java.util.HashSet<Long> existing = new java.util.HashSet<>();
        for (StopwatchRecords.Activity a : new StopwatchRecords(host).all()) existing.add(a.id);
        out.retainAll(existing);
        return out;
    }

    /** Show only some activities (none selected = show all). */
    private static void chooseFilter(Activity host, List<StopwatchRecords.Activity> activities, Runnable onChanged) {
        java.util.Set<Long> current = filter(host);
        Sheet sheet = new Sheet(host, AppString.get(R.string.stopwatch_records_filter));
        ArrayList<CheckBox> boxes = new ArrayList<>();
        for (StopwatchRecords.Activity a : activities) {
            CheckBox box = new CheckBox(host);
            box.setText(a.name);
            box.setTextColor(AppSettings.textPrimary(host));
            box.setChecked(current.contains(a.id));
            box.setBackgroundResource(R.drawable.bg_field);
            box.setPadding(dp(host, 8), 0, dp(host, 8), 0);
            boxes.add(box);
            sheet.content.addView(box, params(host, 52, 8));
        }
        sheet.action(AppString.get(R.string.stopwatch_records_filter_all), false, v -> {
            saveFilter(host, new java.util.HashSet<>());
            sheet.dialog.dismiss();
            showList(host, onChanged);
        });
        sheet.action(AppString.get(R.string.runtime_text_0004), true, v -> {
            java.util.HashSet<Long> picked = new java.util.HashSet<>();
            for (int i = 0; i < boxes.size(); i++) if (boxes.get(i).isChecked()) picked.add(activities.get(i).id);
            saveFilter(host, picked);
            sheet.dialog.dismiss();
            showList(host, onChanged);
        });
        sheet.show();
    }

    private static void saveFilter(Activity host, java.util.Set<Long> ids) {
        StringBuilder out = new StringBuilder();
        for (Long id : ids) { if (out.length() > 0) out.append(','); out.append(id); }
        host.getSharedPreferences(FILTER_PREFS, Activity.MODE_PRIVATE).edit().putString(FILTER_KEY, out.toString()).apply();
    }

    /** Centred modal in the app style: bold title, cards, actions. */
    private static final class Sheet {
        final Dialog dialog;
        final LinearLayout content;
        private final LinearLayout actions;
        private final Activity host;

        Sheet(Activity host, String title) {
            this.host = host;
            dialog = new Dialog(host);
            AppMotion.window(dialog.getWindow(), R.style.MotionDialog);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            LinearLayout root = new LinearLayout(host);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setLayoutDirection(AppSettings.layoutDirection(host));
            root.setPadding(dp(host, 18), dp(host, 14), dp(host, 18), dp(host, 18));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(AppSettings.surface(host));
            bg.setCornerRadius(dp(host, 24));
            root.setBackground(bg);

            // Title only, aligned to the reading start (right in Persian, left in English).
            TextView header = text(host, title, 20, AppSettings.textPrimary(host));
            header.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            header.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            header.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            header.setPadding(dp(host, 4), dp(host, 8), dp(host, 4), dp(host, 8));
            root.addView(header);

            content = new LinearLayout(host);
            content.setOrientation(LinearLayout.VERTICAL);
            ScrollView scroll = new ScrollView(host);
            scroll.addView(content);
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
            scrollParams.topMargin = dp(host, 4);
            root.addView(scroll, scrollParams);

            actions = new LinearLayout(host);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setVisibility(View.GONE);
            root.addView(actions, params(host, 52, 12));
            dialog.setContentView(root);
            dialog.setCanceledOnTouchOutside(true);
        }

        Button action(String label, boolean primary, View.OnClickListener listener) {
            Button button = new Button(host);
            button.setText(label);
            button.setAllCaps(false);
            button.setBackgroundResource(primary ? R.drawable.bg_teal_button : R.drawable.bg_field);
            button.setTextColor(primary ? Color.WHITE : AppSettings.primaryColor(host));
            button.setOnClickListener(listener);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1f);
            if (actions.getChildCount() > 0) p.setMarginStart(dp(host, 8));
            actions.addView(button, p);
            actions.setVisibility(View.VISIBLE);
            return button;
        }

        void show() {
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                WindowManager.LayoutParams p = new WindowManager.LayoutParams();
                p.copyFrom(window.getAttributes());
                int screen = host.getResources().getDisplayMetrics().widthPixels;
                p.width = Math.min(screen - dp(host, 32), dp(host, 480));
                p.height = WindowManager.LayoutParams.WRAP_CONTENT;
                p.gravity = Gravity.CENTER;
                window.setAttributes(p);
            }
        }
    }

    private static GradientDrawable fieldShape(Activity host) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppSettings.field(host));
        bg.setCornerRadius(dp(host, 12));
        bg.setStroke(dp(host, 1), AppSettings.themeMode(host) == AppSettings.THEME_DARK ? 0xFF343D3A : 0xFFD8E8E7);
        return bg;
    }

    /** Category suggestions that match anywhere in the name, like a search. */
    private static final class CategoryAdapter extends ArrayAdapter<String> {
        private final List<String> all;
        private final List<String> shown = new ArrayList<>();
        private final Activity host;

        CategoryAdapter(Activity host, List<String> names) {
            super(host, android.R.layout.simple_list_item_1);
            this.host = host;
            this.all = new ArrayList<>(names);
            shown.addAll(all);
        }

        @Override public int getCount() { return shown.size(); }
        @Override public String getItem(int position) { return shown.get(position); }

        @Override public View getView(int position, View convert, android.view.ViewGroup parent) {
            TextView view = convert instanceof TextView ? (TextView) convert : new TextView(host);
            view.setText(shown.get(position));
            view.setTextColor(AppSettings.textPrimary(host));
            view.setTextSize(15);
            view.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            view.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
            view.setPadding(dp(host, 14), 0, dp(host, 14), 0);
            view.setMinHeight(dp(host, 48));
            view.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, dp(host, 48)));
            return view;
        }

        @Override public android.widget.Filter getFilter() {
            return new android.widget.Filter() {
                @Override protected FilterResults performFiltering(CharSequence query) {
                    String q = query == null ? "" : query.toString().trim().toLowerCase(Locale.ROOT);
                    ArrayList<String> out = new ArrayList<>();
                    for (String name : all) if (q.isEmpty() || name.toLowerCase(Locale.ROOT).contains(q)) out.add(name);
                    FilterResults results = new FilterResults();
                    results.values = out;
                    results.count = out.size();
                    return results;
                }

                @SuppressWarnings("unchecked")
                @Override protected void publishResults(CharSequence query, FilterResults results) {
                    shown.clear();
                    if (results.values != null) shown.addAll((List<String>) results.values);
                    notifyDataSetChanged();
                }
            };
        }
    }

    /** Card in the style of the Tools sheet rows. */
    private static View card(Activity host, String title, String subtitle, boolean arrow) {
        LinearLayout row = new LinearLayout(host);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(AppSettings.layoutDirection(host));
        row.setPadding(dp(host, 14), dp(host, 12), dp(host, 14), dp(host, 12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(AppSettings.field(host));
        bg.setCornerRadius(dp(host, 16));
        bg.setStroke(dp(host, 1), AppSettings.themeMode(host) == AppSettings.THEME_DARK ? 0xFF343D3A : 0xFFD8E8E7);
        row.setBackground(bg);
        row.setClickable(true);
        row.setFocusable(true);
        LinearLayout column = new LinearLayout(host);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView t = text(host, title, 16, AppSettings.textPrimary(host));
        t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        column.addView(t);
        if (subtitle != null && !subtitle.isEmpty()) {
            TextView s = text(host, subtitle, 12, AppSettings.textSecondary(host));
            s.setPadding(0, dp(host, 3), 0, 0);
            column.addView(s);
        }
        row.addView(column, new LinearLayout.LayoutParams(0, -2, 1f));
        if (arrow) {
            TextView mark = text(host, AppSettings.isRtlLanguage(host) ? "‹" : "›", 24, AppSettings.primaryColor(host));
            mark.setGravity(Gravity.CENTER);
            row.addView(mark, new LinearLayout.LayoutParams(dp(host, 28), -2));
        }
        return row;
    }

    private static LinearLayout.LayoutParams params(Activity host, int heightDp, int topDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, heightDp < 0 ? -2 : dp(host, heightDp));
        p.topMargin = dp(host, topDp);
        return p;
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
