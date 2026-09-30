package com.ilia.advanceclock;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

public final class AlarmReminderDialog {
    public interface Callback {
        void onConfigured(int mode, String customMinutesJson);
    }

    private AlarmReminderDialog() {}

    public static void show(Context context, int currentMode, String customJson, Callback callback) {
        String[] labels = AlarmReminderUtils.optionLabels();
        boolean[] checked = new boolean[labels.length];

        if (currentMode == AlarmReminderUtils.MODE_NONE) {
            checked[0] = true;
        } else if (currentMode == AlarmReminderUtils.MODE_SMART) {
            checked[1] = true;
        } else {
            List<Integer> selected = AlarmReminderUtils.effective(
                    AlarmReminderUtils.MODE_CUSTOM, customJson);
            for (int i = 0; i < AlarmReminderUtils.VALUES.length; i++) {
                checked[i + 2] = selected.contains(AlarmReminderUtils.VALUES[i]);
            }
            if (selected.isEmpty()) checked[0] = true;
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("یادآوری‌ها")
                .setMultiChoiceItems(labels, checked, (d, which, isChecked) -> {
                    checked[which] = isChecked;
                    ListView list = ((AlertDialog) d).getListView();

                    if ((which == 0 || which == 1) && isChecked) {
                        for (int i = 0; i < checked.length; i++) {
                            boolean value = i == which;
                            checked[i] = value;
                            list.setItemChecked(i, value);
                        }
                        return;
                    }

                    if (which >= 2 && isChecked) {
                        checked[0] = false;
                        checked[1] = false;
                        list.setItemChecked(0, false);
                        list.setItemChecked(1, false);
                    }

                    boolean anyCustom = false;
                    for (int i = 2; i < checked.length; i++) {
                        anyCustom |= checked[i];
                    }

                    if (!checked[0] && !checked[1] && !anyCustom) {
                        checked[0] = true;
                        list.setItemChecked(0, true);
                    }
                })
                .setNegativeButton("انصراف", null)
                .setPositiveButton("تأیید", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {
                            if (checked[0]) {
                                callback.onConfigured(
                                        AlarmReminderUtils.MODE_NONE, "[]");
                            } else if (checked[1]) {
                                callback.onConfigured(
                                        AlarmReminderUtils.MODE_SMART, "[]");
                            } else {
                                ArrayList<Integer> minutes = new ArrayList<>();
                                for (int i = 0; i < AlarmReminderUtils.VALUES.length; i++) {
                                    if (checked[i + 2]) {
                                        minutes.add(AlarmReminderUtils.VALUES[i]);
                                    }
                                }

                                if (minutes.isEmpty()) {
                                    callback.onConfigured(
                                            AlarmReminderUtils.MODE_NONE, "[]");
                                } else {
                                    callback.onConfigured(
                                            AlarmReminderUtils.MODE_CUSTOM,
                                            AlarmReminderUtils.toJson(minutes));
                                }
                            }
                            dialog.dismiss();
                        }));

        dialog.show();
    }
}
