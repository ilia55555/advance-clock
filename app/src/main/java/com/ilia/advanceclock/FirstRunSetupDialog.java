package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

final class FirstRunSetupDialog {
    interface Callback { void onCompleted(boolean languageChanged); }

    private FirstRunSetupDialog() {}

    static void show(Activity activity, Callback completed) {
        int padding = dp(activity, 20);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(padding, dp(activity, 8), padding, 0);

        TextView message = label(activity, activity.getString(R.string.first_setup_message));
        content.addView(message);
        content.addView(label(activity, activity.getString(R.string.language_label)));
        Spinner language = spinner(activity, R.array.language_options);
        language.setSelection(AppSettings.languagePosition(activity));
        content.addView(language, new LinearLayout.LayoutParams(-1, dp(activity, 54)));

        content.addView(label(activity, activity.getString(R.string.calendar_label)));
        Spinner calendar = spinner(activity, R.array.calendar_options);
        calendar.setSelection(AppSettings.defaultCalendar(activity));
        content.addView(calendar, new LinearLayout.LayoutParams(-1, dp(activity, 54)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.first_setup_title)
                .setView(content)
                .setPositiveButton(R.string.continue_label, null)
                .create();
        dialog.setCancelable(false);
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String selectedLanguage = AppSettings.languageCodes()[language.getSelectedItemPosition()];
                    boolean languageChanged = !selectedLanguage.equals(AppSettings.language(activity));
                    AppSettings.setLanguage(activity, selectedLanguage);
                    AppSettings.setDefaultCalendar(activity, calendar.getSelectedItemPosition());
                    dialog.dismiss();
                    completed.onCompleted(languageChanged);
                }));
        dialog.show();
    }

    private static Spinner spinner(Activity activity, int array) {
        Spinner spinner = new Spinner(activity);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(activity, array,
                android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        return spinner;
    }

    private static TextView label(Activity activity, String text) {
        TextView label = new TextView(activity);
        label.setText(text);
        label.setTextColor(AppSettings.textPrimary(activity));
        label.setTextSize(14);
        label.setPadding(0, dp(activity, 10), 0, dp(activity, 4));
        return label;
    }

    private static int dp(Activity activity, int value) {
        return Math.round(value * activity.getResources().getDisplayMetrics().density);
    }
}
