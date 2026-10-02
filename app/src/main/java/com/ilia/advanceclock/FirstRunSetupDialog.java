package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import java.util.Locale;

final class FirstRunSetupDialog {
    interface Callback { void onCompleted(boolean languageChanged); }

    private FirstRunSetupDialog() {}

    static void show(Activity activity, Callback completed) {
        int padding = dp(activity, 22);
        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setLayoutDirection(AppSettings.layoutDirection(activity));
        content.setPadding(padding, dp(activity, 18), padding, dp(activity, 10));
        GradientDrawable card = new GradientDrawable();
        card.setColor(AppSettings.surface(activity));
        card.setCornerRadius(dp(activity, 22));
        card.setStroke(dp(activity, 1), AppSettings.primaryColor(activity));
        content.setBackground(card);

        TextView icon = new TextView(activity);
        icon.setText(R.string.ui_prayer_icon_midnight);
        icon.setGravity(Gravity.CENTER);
        icon.setTextSize(38);
        icon.setTextColor(AppSettings.primaryColor(activity));
        content.addView(icon, new LinearLayout.LayoutParams(-1, dp(activity, 52)));

        TextView title = new TextView(activity);
        title.setGravity(Gravity.CENTER);
        title.setTextSize(23);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(AppSettings.textPrimary(activity));
        content.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView message = new TextView(activity);
        message.setGravity(Gravity.CENTER);
        message.setTextSize(14);
        message.setTextColor(AppSettings.textSecondary(activity));
        message.setPadding(0, dp(activity, 8), 0, dp(activity, 10));
        content.addView(message, new LinearLayout.LayoutParams(-1, -2));

        TextView languageLabel = label(activity);
        content.addView(languageLabel);
        Spinner language = languageSpinner(
                activity,
                activity.getResources().getStringArray(R.array.language_options));
        language.setBackgroundResource(R.drawable.bg_field);
        language.setPadding(dp(activity, 12), 0, dp(activity, 12), 0);
        language.setSelection(
                AppSettings.selectableLanguagePosition(activity), false);
        content.addView(language, fieldParams(activity));

        TextView calendarLabel = label(activity);
        content.addView(calendarLabel);
        Spinner calendar = spinner(activity, activity.getResources().getStringArray(
                R.array.calendar_options));
        calendar.setBackgroundResource(R.drawable.bg_field);
        calendar.setPadding(dp(activity, 12), 0, dp(activity, 12), 0);
        calendar.setSelection(AppSettings.defaultCalendar(activity), false);
        content.addView(calendar, fieldParams(activity));

        Switch adhan = new Switch(activity);
        adhan.setText(AppString.get(R.string.runtime_text_0360));
        adhan.setTextColor(AppSettings.textPrimary(activity));
        adhan.setChecked(false);
        adhan.setPadding(0, dp(activity, 12), 0, 0);
        content.addView(adhan, new LinearLayout.LayoutParams(-1, dp(activity, 56)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .setPositiveButton(R.string.continue_label, null)
                .create();
        dialog.setCancelable(false);

        final boolean[] refreshing = {false};
        Runnable refreshLanguage = () -> {
            if (refreshing[0]) return;
            refreshing[0] = true;
            try {
                int position = language.getSelectedItemPosition();
                if (!AppSettings.isSelectableLanguagePosition(position)) {
                    position = AppSettings.selectableLanguagePosition(activity);
                }

                String code = AppSettings.languageCodes()[position];
                Context localized = localizedContext(activity, code);
                int direction = isRtl(code)
                        ? View.LAYOUT_DIRECTION_RTL
                        : View.LAYOUT_DIRECTION_LTR;

                title.setText(localized.getString(R.string.first_setup_title));
                message.setText(localized.getString(R.string.first_setup_message));
                languageLabel.setText(localized.getString(R.string.language_label));
                calendarLabel.setText(localized.getString(R.string.calendar_label));
                adhan.setText(localized.getString(R.string.runtime_text_0360));

                int calendarPosition = calendar.getSelectedItemPosition();

                language.setAdapter(languageAdapter(
                        activity,
                        localized.getResources().getStringArray(
                                R.array.language_options)));
                language.setSelection(position, false);

                calendar.setAdapter(adapter(
                        activity,
                        localized.getResources().getStringArray(
                                R.array.calendar_options)));
                calendar.setSelection(
                        Math.max(0, calendarPosition),
                        false);

                content.setLayoutDirection(direction);
                languageLabel.setLayoutDirection(direction);
                calendarLabel.setLayoutDirection(direction);
                language.setLayoutDirection(direction);
                calendar.setLayoutDirection(direction);
                adhan.setLayoutDirection(direction);
                adhan.setTextDirection(
                        isRtl(code)
                                ? View.TEXT_DIRECTION_RTL
                                : View.TEXT_DIRECTION_LTR);

                if (dialog.isShowing()) {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(
                            localized.getString(R.string.continue_label));
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                            .setLayoutDirection(direction);
                }
            } finally {
                refreshing[0] = false;
            }
        };

        language.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id) {
                refreshLanguage.run();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        dialog.setOnShowListener(ignored -> {
            refreshLanguage.run();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(0xFFFFFFFF);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setBackgroundResource(
                    R.drawable.bg_teal_button);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                int languagePosition = language.getSelectedItemPosition();
                if (!AppSettings.isSelectableLanguagePosition(languagePosition)) {
                    languagePosition = AppSettings.selectableLanguagePosition(activity);
                }
                String selectedLanguage = AppSettings.languageCodes()[languagePosition];
                boolean languageChanged = !selectedLanguage.equals(AppSettings.language(activity));
                AppSettings.setLanguage(activity, selectedLanguage);
                AppSettings.setDefaultCalendar(activity, calendar.getSelectedItemPosition());
                AppSettings.setAdhanEnabled(activity, adhan.isChecked());
                dialog.dismiss();
                completed.onCompleted(languageChanged);
            });
        });
        dialog.show();
    }

    private static Context localizedContext(Context context, String languageCode) {
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(languageCode));
        configuration.setLayoutDirection(Locale.forLanguageTag(languageCode));
        return context.createConfigurationContext(configuration);
    }

    private static boolean isRtl(String languageCode) {
        String language = Locale.forLanguageTag(languageCode).getLanguage();
        return "fa".equals(language) || "ar".equals(language);
    }

    private static Spinner spinner(Activity activity, String[] values) {
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(adapter(activity, values));
        return spinner;
    }

    private static Spinner languageSpinner(
            Activity activity, String[] values) {
        Spinner spinner = new Spinner(activity);
        spinner.setAdapter(languageAdapter(activity, values));
        return spinner;
    }

    private static ArrayAdapter<String> languageAdapter(
            Activity activity,
            String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                activity,
                android.R.layout.simple_spinner_item,
                values) {
            @Override public boolean areAllItemsEnabled() {
                return false;
            }

            @Override public boolean isEnabled(int position) {
                return AppSettings.isSelectableLanguagePosition(position);
            }

            @Override public View getDropDownView(
                    int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                boolean enabled = isEnabled(position);
                view.setEnabled(enabled);
                view.setAlpha(enabled ? 1f : 0.38f);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(enabled
                            ? AppSettings.textPrimary(activity)
                            : AppSettings.textSecondary(activity));
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private static ArrayAdapter<String> adapter(Activity activity, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity,
                android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return adapter;
    }

    private static TextView label(Activity activity) {
        TextView label = new TextView(activity);
        label.setTextColor(AppSettings.textPrimary(activity));
        label.setTextSize(14);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setPadding(0, dp(activity, 13), 0, dp(activity, 5));
        return label;
    }

    private static LinearLayout.LayoutParams fieldParams(Activity activity) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(activity, 54));
        params.bottomMargin = dp(activity, 3);
        return params;
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
}
