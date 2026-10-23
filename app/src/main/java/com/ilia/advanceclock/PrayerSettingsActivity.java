package com.ilia.advanceclock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Keeps existing widget shortcuts pointing at the unified settings page. */
public final class PrayerSettingsActivity extends Activity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        startActivity(new Intent(this, SettingsActivity.class)
                .putExtra(SettingsActivity.EXTRA_SECTION, "prayer"));
        finish();
    }
}
