package com.ilia.advanceclock;

import android.graphics.Color;
import android.widget.TextView;

final class ButtonStyles {
    private ButtonStyles() {}
    static void destructive(TextView button) {
        button.setBackgroundTintList(null);
        button.setBackgroundResource(R.drawable.bg_delete_button);
        button.setTextColor(Color.WHITE);
    }
}
