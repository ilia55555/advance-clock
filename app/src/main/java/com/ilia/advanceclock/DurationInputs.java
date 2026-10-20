package com.ilia.advanceclock;

import android.graphics.Rect;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

/** Places the first keyboard-opening tap at the end, without constraining later caret movement. */
final class DurationInputs {
    private final EditText hours, minutes, seconds;
    private boolean imeVisible, updating;

    DurationInputs(EditText hours, EditText minutes, EditText seconds) {
        this.hours = hours; this.minutes = minutes; this.seconds = seconds;
        for (EditText field : new EditText[]{hours, minutes, seconds}) {
            field.setFilters(new android.text.InputFilter[0]);
            field.setSingleLine(true);
            field.setTextDirection(View.TEXT_DIRECTION_LTR);
            field.setSelectAllOnFocus(false);
            field.setImeOptions(EditorInfo.IME_ACTION_DONE);
            field.setOnTouchListener(new View.OnTouchListener() {
                boolean opening;
                @Override public boolean onTouch(View v, MotionEvent event) {
                    if (event.getActionMasked() == MotionEvent.ACTION_DOWN) opening = !keyboardVisible();
                    if (event.getActionMasked() == MotionEvent.ACTION_UP && opening)
                        field.post(() -> { if (field.hasFocus()) field.setSelection(field.length()); });
                    return false;
                }
            });
            field.setOnFocusChangeListener((v, focused) -> {
                if (!focused) field.post(() -> {
                    if (!hours.hasFocus() && !minutes.hasFocus() && !seconds.hasFocus()) normalize();
                });
            });
            field.setOnEditorActionListener((v, action, event) -> {
                if (action != EditorInfo.IME_ACTION_DONE) return false;
                normalize();
                field.clearFocus();
                InputMethodManager keyboard = field.getContext().getSystemService(InputMethodManager.class);
                if (keyboard != null) keyboard.hideSoftInputFromWindow(field.getWindowToken(), 0);
                return true;
            });
        }
        View group = (View) hours.getParent();
        group.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            boolean visible = keyboardVisible();
            if (imeVisible && !visible) normalize();
            imeVisible = visible;
        });
    }

    private boolean keyboardVisible() {
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsets insets = hours.getRootWindowInsets();
            if (insets != null) return insets.isVisible(WindowInsets.Type.ime());
        }
        Rect frame = new Rect();
        View root = hours.getRootView();
        root.getWindowVisibleDisplayFrame(frame);
        return root.getHeight() > 0 && root.getHeight() - frame.bottom > root.getHeight() * .18f;
    }

    long readMillis() {
        DurationValue value = normalize();
        if (value == null) return 0L;
        try { return value.schedulableMillis(System.currentTimeMillis()); }
        catch (ArithmeticException error) {
            hours.setError(hours.getContext().getString(R.string.duration_too_large));
            return 0L;
        }
    }

    private DurationValue normalize() {
        if (updating) return null;
        try {
            DurationValue value = new DurationValue(hours.getText().toString(), minutes.getText().toString(), seconds.getText().toString());
            updating = true;
            update(hours, value.hours.toString()); update(minutes, Integer.toString(value.minutes));
            update(seconds, Integer.toString(value.seconds));
            return value;
        } catch (NumberFormatException invalid) { return null; }
        finally { updating = false; }
    }

    private void update(EditText field, String value) {
        if (field.getText().toString().equals(value)) return;
        int start = Math.max(0, field.getSelectionStart()), end = Math.max(0, field.getSelectionEnd());
        field.setText(value);
        if (field.hasFocus()) field.setSelection(Math.min(start, field.length()), Math.min(end, field.length()));
    }
}
