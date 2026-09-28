package com.ilia.advanceclock;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public final class SmartAlarmActivity extends Activity {
    private final ArrayList<SmartAlarmParser.Candidate> candidates = new ArrayList<>();

    private EditText input;
    private TextView detection;
    private TextView warning;
    private TextView previewTitle;
    private LinearLayout previewList;
    private Button analyze;
    private Button saveAll;
    private Spinner bulkMode;
    private EditText bulkMinutes;

    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        AppSettings.applyModalOverlay(this);
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(AppSettings.background(this));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(18), dp(12), dp(18), dp(24));
        root.setBackgroundColor(AppSettings.background(this));
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        root.addView(buildTopBar());
        root.addView(buildIntro());
        root.addView(buildInputCard(), cardParams());
        root.addView(buildTimingCard(), cardParams());
        root.addView(buildPreviewCard(), cardParams());

        setContentView(scroll);
        AppSettings.applyFullscreenInsets(scroll);
        AppSettings.playFullscreenEnter(this);
        refreshPreview();
    }

    private View buildTopBar() {
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = text("هشدار هوشمند", 25, AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        top.addView(title, new LinearLayout.LayoutParams(0, dp(56), 1f));

        ImageButton close = new ImageButton(this);
        close.setImageResource(R.drawable.ic_md_close);
        close.setColorFilter(AppSettings.textPrimary(this), PorterDuff.Mode.SRC_IN);
        close.setBackgroundColor(0x00000000);
        close.setPadding(dp(12), dp(12), dp(12), dp(12));
        close.setContentDescription("بستن");
        close.setOnClickListener(v -> finish());
        top.addView(close, new LinearLayout.LayoutParams(dp(48), dp(48)));
        return top;
    }

    private View buildIntro() {
        TextView intro = text(
                "متن معمولی، برنامه کپی‌شده یا JSON را وارد کنید. "
                        + "تاریخ‌ها و ساعت‌ها خودکار استخراج می‌شوند و قبل از ذخیره "
                        + "می‌توانید هر هشدار را جداگانه ویرایش، حذف یا زمان زنگ آن را جابه‌جا کنید.",
                12,
                AppSettings.textSecondary(this));
        intro.setPadding(0, 0, 0, dp(10));
        return intro;
    }

    private View buildInputCard() {
        LinearLayout card = card();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView title = text("ورودی هوشمند", 18, AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, dp(44), 1f));

        Button paste = softButton("پیست");
        paste.setOnClickListener(v -> pasteClipboard());
        header.addView(paste, new LinearLayout.LayoutParams(dp(76), dp(40)));

        Button clear = softButton("پاک کردن");
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(dp(88), dp(40));
        clearParams.setMarginStart(dp(6));
        clear.setOnClickListener(v -> {
            input.setText("");
            candidates.clear();
            detection.setText("تشخیص خودکار");
            warning.setText("");
            refreshPreview();
        });
        header.addView(clear, clearParams);
        card.addView(header);

        detection = chip("تشخیص خودکار");
        LinearLayout.LayoutParams detectionParams = chipParams();
        detectionParams.topMargin = dp(4);
        card.addView(detection, detectionParams);

        input = new EditText(this);
        input.setMinHeight(dp(230));
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setBackgroundResource(R.drawable.bg_field);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setTextColor(AppSettings.textPrimary(this));
        input.setHintTextColor(AppSettings.textSecondary(this));
        input.setTextSize(14);
        input.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setHint(
                "هر متنی را اینجا پیست کنید…\n\n"
                        + "مثال:\n"
                        + "۱۴۰۵/۰۷/۰۴ ساعت ۱۳:۰۰ تا ۱۵:۰۰ خاموشی احتمالی\n"
                        + "۱۴۰۵/۰۷/۰۵ ساعت ۱۵:۰۰ جلسه\n\n"
                        + "یا JSON شامل date / time / start / title");
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(240));
        inputParams.topMargin = dp(8);
        card.addView(input, inputParams);

        Button simplify = softButton("ساده‌سازی: فقط تاریخ و ساعت");
        simplify.setOnClickListener(v -> simplifyInput());
        LinearLayout.LayoutParams simplifyParams = new LinearLayout.LayoutParams(-1, dp(46));
        simplifyParams.topMargin = dp(8);
        card.addView(simplify, simplifyParams);

        TextView simplifyHint = text(
                "هر خط یک تاریخ و نزدیک‌ترین ساعت‌های آن است؛ متن را انتخاب، جابه‌جا یا "
                        + "ویرایش کنید و سپس تحلیل را بزنید.",
                11,
                AppSettings.textSecondary(this));
        simplifyHint.setPadding(0, dp(4), 0, 0);
        card.addView(simplifyHint);

        analyze = primaryButton("تحلیل متن و ساخت پیش‌نمایش");
        analyze.setEnabled(false);
        analyze.setAlpha(0.55f);
        LinearLayout.LayoutParams analyzeParams = new LinearLayout.LayoutParams(-1, dp(54));
        analyzeParams.topMargin = dp(10);
        card.addView(analyze, analyzeParams);

        warning = text("", 11, 0xFFC05A3B);
        warning.setPadding(0, dp(7), 0, 0);
        card.addView(warning);

        input.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                boolean enabled = s != null && !s.toString().trim().isEmpty();
                analyze.setEnabled(enabled);
                analyze.setAlpha(enabled ? 1f : 0.55f);
            }

            @Override public void afterTextChanged(Editable s) {}
        });

        analyze.setOnClickListener(v -> analyzeInput());
        return card;
    }

    private void simplifyInput() {
        String simplified = SmartAlarmParser.simplify(input.getText().toString());
        if (simplified.isEmpty()) {
            LogoToast.makeText(
                    this,
                    "تاریخ و ساعت قابل ساده‌سازی پیدا نشد",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        input.setText(simplified);
        input.setSelection(input.length());
        candidates.clear();
        warning.setText("");
        detection.setText("متن ساده‌شده؛ برای ساخت هشدار تحلیل را بزنید");
        refreshPreview();
    }

    private View buildTimingCard() {
        LinearLayout card = card();

        TextView title = text(
                "زمان زنگ برای همه",
                17,
                AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(title);

        TextView hint = text(
                "این تنظیم را می‌توانید روی همه موارد اعمال کنید؛ "
                        + "بعداً هر هشدار را جداگانه هم می‌توان تغییر داد.",
                11,
                AppSettings.textSecondary(this));
        hint.setPadding(0, dp(3), 0, dp(8));
        card.addView(hint);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        bulkMode = spinner(new String[]{"سرِ وقت", "قبل از زمان", "بعد از زمان"});
        row.addView(bulkMode, new LinearLayout.LayoutParams(0, dp(52), 1f));

        bulkMinutes = new EditText(this);
        bulkMinutes.setText("5");
        bulkMinutes.setHint("دقیقه");
        bulkMinutes.setSelectAllOnFocus(true);
        bulkMinutes.setSingleLine(true);
        bulkMinutes.setGravity(Gravity.CENTER);
        bulkMinutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        bulkMinutes.setTextColor(AppSettings.textPrimary(this));
        bulkMinutes.setHintTextColor(AppSettings.textSecondary(this));
        bulkMinutes.setBackgroundResource(R.drawable.bg_field);
        LinearLayout.LayoutParams minParams = new LinearLayout.LayoutParams(dp(90), dp(52));
        minParams.setMarginStart(dp(8));
        row.addView(bulkMinutes, minParams);
        card.addView(row);

        LinearLayout presets = new LinearLayout(this);
        presets.setOrientation(LinearLayout.HORIZONTAL);
        presets.setGravity(Gravity.CENTER);
        presets.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        presets.setPadding(0, dp(8), 0, 0);
        for (int value : new int[]{5, 10, 15, 30}) {
            Button b = softButton(CalendarUtils.fa(value) + " دقیقه");
            b.setOnClickListener(v -> bulkMinutes.setText(String.valueOf(value)));
            presets.addView(b, weightedButtonParams());
        }
        card.addView(presets);

        Button apply = softButton("اعمال این زمان‌بندی روی همه هشدارهای شناسایی‌شده");
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(-1, dp(48));
        applyParams.topMargin = dp(8);
        apply.setOnClickListener(v -> applyBulkTiming());
        card.addView(apply, applyParams);
        return card;
    }

    private View buildPreviewCard() {
        LinearLayout card = card();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        previewTitle = text(
                "پیش‌نمایش هشدارها",
                17,
                AppSettings.textPrimary(this));
        previewTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(previewTitle, new LinearLayout.LayoutParams(0, dp(44), 1f));

        Button removeAll = softButton("حذف همه");
        removeAll.setOnClickListener(v -> {
            candidates.clear();
            refreshPreview();
        });
        header.addView(removeAll, new LinearLayout.LayoutParams(dp(92), dp(40)));
        card.addView(header);

        TextView hint = text(
                "زمان اصلی استخراج‌شده و زمان واقعی زنگ هر مورد را قبل از ذخیره بررسی کنید.",
                11,
                AppSettings.textSecondary(this));
        hint.setPadding(0, 0, 0, dp(8));
        card.addView(hint);

        previewList = new LinearLayout(this);
        previewList.setOrientation(LinearLayout.VERTICAL);
        previewList.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.addView(previewList, new LinearLayout.LayoutParams(-1, -2));

        saveAll = primaryButton("ذخیره همه هشدارها");
        saveAll.setEnabled(false);
        saveAll.setAlpha(0.55f);
        LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(-1, dp(56));
        saveParams.topMargin = dp(10);
        saveAll.setOnClickListener(v -> saveAllAlarms());
        card.addView(saveAll, saveParams);

        return card;
    }

    private void pasteClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            LogoToast.makeText(this, "متنی در کلیپ‌بورد نیست", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            LogoToast.makeText(this, "متنی در کلیپ‌بورد نیست", Toast.LENGTH_SHORT).show();
            return;
        }

        CharSequence value = clip.getItemAt(0).coerceToText(this);
        if (value == null || value.toString().trim().isEmpty()) {
            LogoToast.makeText(this, "متن قابل استفاده‌ای در کلیپ‌بورد نیست", Toast.LENGTH_SHORT).show();
            return;
        }

        input.setText(value);
        input.setSelection(input.length());
    }

    private void analyzeInput() {
        SmartAlarmParser.Result result =
                SmartAlarmParser.parse(this, input.getText().toString());

        candidates.clear();
        candidates.addAll(result.candidates);
        detection.setText(result.jsonDetected
                ? "تشخیص خودکار: JSON"
                : "تشخیص خودکار: متن عادی");

        if (result.warnings.isEmpty()) {
            warning.setText("");
        } else {
            StringBuilder message = new StringBuilder();
            for (String item : result.warnings) {
                if (message.length() > 0) message.append("\n");
                message.append("• ").append(item);
            }
            warning.setText(message.toString());
        }

        refreshPreview();
        if (!candidates.isEmpty()) {
            LogoToast.makeText(
                    this,
                    CalendarUtils.fa(candidates.size()) + " هشدار شناسایی شد",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void applyBulkTiming() {
        if (candidates.isEmpty()) {
            LogoToast.makeText(this, "ابتدا متن را تحلیل کنید", Toast.LENGTH_SHORT).show();
            return;
        }

        int mode = bulkMode.getSelectedItemPosition();
        int minutes = readPositiveInt(bulkMinutes, 5);
        if (mode == SmartAlarmParser.OFFSET_EXACT) minutes = 0;

        for (SmartAlarmParser.Candidate candidate : candidates) {
            candidate.offsetMode = mode;
            candidate.offsetMinutes = minutes;
        }
        refreshPreview();
    }

    private void refreshPreview() {
        if (previewList == null) return;
        previewList.removeAllViews();

        previewTitle.setText(
                "پیش‌نمایش هشدارها • "
                        + CalendarUtils.fa(candidates.size())
                        + " مورد");

        if (candidates.isEmpty()) {
            TextView empty = text(
                    "هنوز هشداری شناسایی نشده است.",
                    12,
                    AppSettings.textSecondary(this));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(16), 0, dp(16));
            previewList.addView(empty);
        } else {
            for (int i = 0; i < candidates.size(); i++) {
                previewList.addView(buildPreviewRow(i, candidates.get(i)));
            }
        }

        boolean canSave = !candidates.isEmpty();
        saveAll.setEnabled(canSave);
        saveAll.setAlpha(canSave ? 1f : 0.55f);
    }

    private View buildPreviewRow(int index, SmartAlarmParser.Candidate candidate) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        row.setPadding(dp(12), dp(11), dp(12), dp(11));
        row.setBackgroundResource(R.drawable.bg_field);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        titleRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView number = chip(CalendarUtils.fa(index + 1));
        titleRow.addView(number, new LinearLayout.LayoutParams(dp(40), dp(34)));

        TextView title = text(
                candidate.label,
                14,
                AppSettings.textPrimary(this));
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(dp(8), 0, 0, 0);
        titleRow.addView(title, new LinearLayout.LayoutParams(0, dp(40), 1f));
        row.addView(titleRow);

        boolean past = candidate.triggerMillis() <= System.currentTimeMillis();
        StringBuilder detail = new StringBuilder();
        detail.append(candidate.dateText());
        detail.append("  •  زمان متن: ").append(candidate.baseTimeText());
        if (!candidate.endTimeText().isEmpty()) {
            detail.append(" تا ").append(candidate.endTimeText());
        }
        detail.append("\nزنگ: ").append(candidate.triggerTimeText());
        detail.append("  •  ").append(candidate.offsetText());
        detail.append("  •  ").append(CalendarUtils.calendarName(candidate.calendarType));
        if (past) detail.append("\n⚠ زمان زنگ در گذشته است و ذخیره نمی‌شود.");

        TextView detailView = text(
                detail.toString(),
                12,
                past ? 0xFFC44C4C : AppSettings.textSecondary(this));
        detailView.setPadding(0, dp(5), 0, dp(8));
        row.addView(detailView);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        actions.setGravity(Gravity.CENTER);

        Button edit = softButton("ویرایش");
        edit.setOnClickListener(v -> editCandidate(index));
        actions.addView(edit, weightedButtonParams());

        Button duplicate = softButton("کپی");
        duplicate.setOnClickListener(v -> {
            SmartAlarmParser.Candidate copy = copyCandidate(candidate);
            candidates.add(index + 1, copy);
            refreshPreview();
        });
        actions.addView(duplicate, weightedButtonParams());

        Button delete = softButton("حذف");
        delete.setTextColor(0xFFC44C4C);
        delete.setOnClickListener(v -> {
            candidates.remove(index);
            refreshPreview();
        });
        actions.addView(delete, weightedButtonParams());

        row.addView(actions);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(8);
        row.setLayoutParams(params);
        return row;
    }

    private void editCandidate(int index) {
        if (index < 0 || index >= candidates.size()) return;
        SmartAlarmParser.Candidate candidate = candidates.get(index);
        SmartAlarmParser.Candidate draft = copyCandidate(candidate);
        CandidateUiState currentState = CandidateUiState.get(candidate);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(dp(8), dp(2), dp(8), 0);

        TextView labelTitle = dialogLabel("عنوان");
        root.addView(labelTitle);

        EditText label = dialogEdit(draft.label, false);
        root.addView(label, new LinearLayout.LayoutParams(-1, dp(52)));

        TextView dateTitle = dialogLabel("تاریخ و ساعت اصلی");
        root.addView(dateTitle);

        LinearLayout dateRow = new LinearLayout(this);
        dateRow.setOrientation(LinearLayout.HORIZONTAL);
        dateRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button date = fieldButton(draft.dateText());
        dateRow.addView(date, new LinearLayout.LayoutParams(0, dp(52), 1f));

        Button time = fieldButton(draft.baseTimeText());
        LinearLayout.LayoutParams timeParams = new LinearLayout.LayoutParams(dp(112), dp(52));
        timeParams.setMarginStart(dp(8));
        dateRow.addView(time, timeParams);
        root.addView(dateRow);

        TextView ringTitle = dialogLabel("زمان زنگ");
        root.addView(ringTitle);

        LinearLayout ringRow = new LinearLayout(this);
        ringRow.setOrientation(LinearLayout.HORIZONTAL);
        ringRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Spinner mode = spinner(new String[]{"سرِ وقت", "قبل از زمان", "بعد از زمان"});
        mode.setSelection(draft.offsetMode);
        ringRow.addView(mode, new LinearLayout.LayoutParams(0, dp(52), 1f));

        EditText minutes = dialogEdit(
                String.valueOf(Math.max(0, draft.offsetMinutes)),
                true);
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(dp(96), dp(52));
        mp.setMarginStart(dp(8));
        ringRow.addView(minutes, mp);
        root.addView(ringRow);

        TextView priorityTitle = dialogLabel("اولویت");
        root.addView(priorityTitle);
        Spinner priority = spinner(new String[]{
                "کم", "نسبتاً کم", "متوسط", "زیاد", "خیلی زیاد"
        });
        priority.setSelection(currentState.priority);
        root.addView(priority, new LinearLayout.LayoutParams(-1, dp(52)));

        Switch vibrate = new Switch(this);
        vibrate.setText("لرزش همراه هشدار");
        vibrate.setTextColor(AppSettings.textPrimary(this));
        vibrate.setChecked(currentState.vibrate);
        vibrate.setPadding(0, dp(6), 0, 0);
        root.addView(vibrate, new LinearLayout.LayoutParams(-1, dp(52)));

        date.setOnClickListener(v -> CalendarPickerDialog.showDate(
                this,
                draft.baseMillis(),
                draft.calendarType,
                (picked, type) -> {
                    android.icu.util.Calendar c =
                            CalendarUtils.fromMillis(type, picked);
                    draft.calendarType = type;
                    draft.year = c.get(android.icu.util.Calendar.YEAR);
                    draft.month = c.get(android.icu.util.Calendar.MONTH) + 1;
                    draft.day = c.get(android.icu.util.Calendar.DAY_OF_MONTH);
                    date.setText(draft.dateText());
                }));

        time.setOnClickListener(v -> new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    draft.hour = hour;
                    draft.minute = minute;
                    time.setText(draft.baseTimeText());
                },
                draft.hour,
                draft.minute,
                true).show());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("ویرایش هشدار")
                .setView(root)
                .setNegativeButton("انصراف", null)
                .setPositiveButton("ذخیره", null)
                .create();

        dialog.setOnShowListener(ignored ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String newLabel = label.getText().toString().trim();
                    if (newLabel.isEmpty()) {
                        label.setError("عنوان نمی‌تواند خالی باشد");
                        return;
                    }

                    draft.label = newLabel;
                    draft.offsetMode = mode.getSelectedItemPosition();
                    draft.offsetMinutes = draft.offsetMode == SmartAlarmParser.OFFSET_EXACT
                            ? 0 : readPositiveInt(minutes, 5);

                    copyCandidateInto(draft, candidate);
                    CandidateUiState.put(candidate, vibrate.isChecked(),
                            priority.getSelectedItemPosition());
                    dialog.dismiss();
                    refreshPreview();
                }));
        dialog.show();
    }

    private void saveAllAlarms() {
        if (candidates.isEmpty()) return;

        AlarmStore store = new AlarmStore(this);
        List<AlarmItem> existing = store.all();
        long now = System.currentTimeMillis();
        int saved = 0;
        int skippedPast = 0;
        int skippedDuplicate = 0;
        int scheduleFailed = 0;

        long idSeed = Math.max(now, 1_000_000L);
        for (int i = 0; i < candidates.size(); i++) {
            SmartAlarmParser.Candidate candidate = candidates.get(i);
            long trigger = candidate.triggerMillis();

            if (trigger <= now) {
                skippedPast++;
                continue;
            }

            if (isDuplicate(existing, candidate.label, trigger)) {
                skippedDuplicate++;
                continue;
            }

            CandidateUiState state = CandidateUiState.get(candidate);
            long id = uniqueId(store, idSeed + i);

            AlarmItem item = new AlarmItem(
                    id,
                    candidate.label,
                    trigger,
                    AlarmItem.REPEAT_NONE,
                    true,
                    state.vibrate,
                    state.priority,
                    RecurrenceUtils.NONE,
                    1,
                    "[]",
                    15,
                    AlarmReminderUtils.MODE_NONE,
                    "[]");

            try {
                store.save(item);
            } catch (RuntimeException error) {
                scheduleFailed++;
                continue;
            }
            if (!AlarmScheduler.schedule(this, item)) {
                store.delete(item.id);
                scheduleFailed++;
                continue;
            }
            existing.add(item);
            saved++;
        }

        ClockWidgetProvider.updateAll(this);

        StringBuilder message = new StringBuilder();
        message.append(CalendarUtils.fa(saved)).append(" هشدار ذخیره شد");
        if (skippedPast > 0) {
            message.append("\n")
                    .append(CalendarUtils.fa(skippedPast))
                    .append(" مورد گذشته رد شد");
        }
        if (skippedDuplicate > 0) {
            message.append("\n")
                    .append(CalendarUtils.fa(skippedDuplicate))
                    .append(" مورد تکراری ذخیره نشد");
        }
        if (scheduleFailed > 0) {
            message.append("\n")
                    .append(CalendarUtils.fa(scheduleFailed))
                    .append(" مورد زمان‌بندی نشد و ذخیره نشد");
        }

        if (scheduleFailed > 0
                && !PermissionHelper.exactAlarmsGranted(this)
                && Build.VERSION.SDK_INT >= 31) {
            message.append("\nبرای اجرای دقیق، دسترسی آلارم دقیق را فعال کنید.");
            LogoToast.makeText(this, message.toString(), Toast.LENGTH_LONG).show();
            try {
                startActivity(new Intent(
                        Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + getPackageName())));
            } catch (Exception ignored) {}
            return;
        }

        if (saved == 0) {
            LogoToast.makeText(this, message.toString(), Toast.LENGTH_LONG).show();
            return;
        }

        setResult(RESULT_OK);

        LogoToast.makeText(this, message.toString(), Toast.LENGTH_LONG).show();
        finish();
    }

    private boolean isDuplicate(
            List<AlarmItem> existing,
            String label,
            long trigger) {
        String normalizedLabel = label == null ? "" : label.trim();
        for (AlarmItem item : existing) {
            if (Math.abs(item.triggerAtMillis - trigger) < 60_000L
                    && item.label.trim().equalsIgnoreCase(normalizedLabel)) {
                return true;
            }
        }
        return false;
    }

    private long uniqueId(AlarmStore store, long seed) {
        long id = seed;
        while (store.find(id) != null) id++;
        return id;
    }

    private void copyCandidateInto(
            SmartAlarmParser.Candidate source,
            SmartAlarmParser.Candidate target) {
        target.label = source.label;
        target.calendarType = source.calendarType;
        target.year = source.year;
        target.month = source.month;
        target.day = source.day;
        target.hour = source.hour;
        target.minute = source.minute;
        target.endHour = source.endHour;
        target.endMinute = source.endMinute;
        target.offsetMode = source.offsetMode;
        target.offsetMinutes = source.offsetMinutes;
        target.source = source.source;
    }

    private SmartAlarmParser.Candidate copyCandidate(
            SmartAlarmParser.Candidate source) {
        SmartAlarmParser.Candidate c = new SmartAlarmParser.Candidate();
        c.label = source.label;
        c.calendarType = source.calendarType;
        c.year = source.year;
        c.month = source.month;
        c.day = source.day;
        c.hour = source.hour;
        c.minute = source.minute;
        c.endHour = source.endHour;
        c.endMinute = source.endMinute;
        c.offsetMode = source.offsetMode;
        c.offsetMinutes = source.offsetMinutes;
        c.source = source.source;
        CandidateUiState old = CandidateUiState.get(source);
        CandidateUiState.put(c, old.vibrate, old.priority);
        return c;
    }

    private int readPositiveInt(EditText field, int fallback) {
        try {
            String raw = field.getText().toString().trim()
                    .replace('۰','0').replace('۱','1').replace('۲','2')
                    .replace('۳','3').replace('۴','4').replace('۵','5')
                    .replace('۶','6').replace('۷','7').replace('۸','8')
                    .replace('۹','9');
            int value = Integer.parseInt(raw);
            return Math.max(0, Math.min(1440, value));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    @Override public void finish() {
        super.finish();
        AppSettings.playFullscreenExit(this);
    }

    private TextView dialogLabel(String value) {
        TextView v = text(value, 12, AppSettings.textSecondary(this));
        v.setPadding(0, dp(10), 0, dp(4));
        return v;
    }

    private EditText dialogEdit(String value, boolean numeric) {
        EditText edit = new EditText(this);
        edit.setText(value);
        edit.setSingleLine(true);
        edit.setSelectAllOnFocus(numeric);
        edit.setTextColor(AppSettings.textPrimary(this));
        edit.setHintTextColor(AppSettings.textSecondary(this));
        edit.setBackgroundResource(R.drawable.bg_field);
        edit.setPadding(dp(12), 0, dp(12), 0);
        edit.setInputType(numeric
                ? InputType.TYPE_CLASS_NUMBER
                : InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        return edit;
    }

    private Button fieldButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setBackgroundResource(R.drawable.bg_field);
        return button;
    }

    private Spinner spinner(String[] values) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setBackgroundResource(R.drawable.bg_field);
        return spinner;
    }

    private Button softButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextSize(11);
        button.setMinWidth(0);
        button.setPadding(dp(8), 0, dp(8), 0);
        button.setTextColor(AppSettings.primaryColor(this));
        button.setBackgroundResource(R.drawable.bg_soft_button);
        return button;
    }

    private LinearLayout.LayoutParams weightedButtonParams() {
        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(0, dp(42), 1f);
        params.setMarginStart(dp(3));
        params.setMarginEnd(dp(3));
        return params;
    }

    private TextView chip(String value) {
        TextView chip = text(value, 11, AppSettings.primaryColor(this));
        chip.setGravity(Gravity.CENTER);
        chip.setBackgroundResource(R.drawable.bg_soft_button);
        chip.setPadding(dp(10), 0, dp(10), 0);
        return chip;
    }

    private LinearLayout.LayoutParams chipParams() {
        return new LinearLayout.LayoutParams(-2, dp(34));
    }

    private Button primaryButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(0xFFFFFFFF);
        button.setTextSize(14);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackgroundResource(R.drawable.bg_orange_button);
        return button;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setBackgroundResource(R.drawable.bg_card);
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(12);
        return params;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setGravity(Gravity.START);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    /**
     * Keeps UI-only attributes per parsed candidate without changing the parser's
     * data contract or persisted AlarmItem schema.
     */
    private static final class CandidateUiState {
        private static final java.util.WeakHashMap<
                SmartAlarmParser.Candidate, CandidateUiState> STATES =
                new java.util.WeakHashMap<>();

        final boolean vibrate;
        final int priority;

        CandidateUiState(boolean vibrate, int priority) {
            this.vibrate = vibrate;
            this.priority = PriorityUtils.clamp(priority);
        }

        static CandidateUiState get(SmartAlarmParser.Candidate candidate) {
            CandidateUiState state = STATES.get(candidate);
            return state == null
                    ? new CandidateUiState(true, PriorityUtils.MEDIUM)
                    : state;
        }

        static void put(
                SmartAlarmParser.Candidate candidate,
                boolean vibrate,
                int priority) {
            STATES.put(candidate, new CandidateUiState(vibrate, priority));
        }
    }
}
