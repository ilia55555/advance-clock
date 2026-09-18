package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

public final class NoForgetQuickAddActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noforget_quick_add);

        EditText text = findViewById(R.id.quick_note_text);
        findViewById(R.id.quick_note_save).setOnClickListener(v -> {
            String value = text.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(this, "یادداشت خالی است", Toast.LENGTH_SHORT).show();
                return;
            }
            long now = System.currentTimeMillis();
            NoForgetItem item = new NoForgetItem(
                    now, value, "", "[]",
                    NoForgetItem.PRIORITY_NORMAL,
                    false, 0L, false, now
            );
            new NoForgetStore(this).save(item);
            NoForgetWidgetProvider.updateAll(this);
            finish();
        });
        findViewById(R.id.quick_note_more).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, NoForgetEditorActivity.class));
            finish();
        });
        findViewById(R.id.quick_note_cancel).setOnClickListener(v -> finish());
    }
}
