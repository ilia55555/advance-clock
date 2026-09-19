package com.ilia.advanceclock;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public final class NoForgetQuickAddActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        AppSettings.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noforget_quick_add);

        EditText text = findViewById(R.id.quick_note_text);
        Spinner priority = findViewById(R.id.quick_note_priority);
        ArrayAdapter<String> p = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                new String[]{"اهمیت کم","اهمیت عادی","اهمیت زیاد"});
        p.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        priority.setAdapter(p);
        priority.setSelection(NoForgetItem.PRIORITY_NORMAL);

        findViewById(R.id.quick_note_save).setOnClickListener(v -> {
            String value = text.getText().toString().trim();
            if (value.isEmpty()) {
                Toast.makeText(this, "یادداشت خالی است", Toast.LENGTH_SHORT).show();
                return;
            }
            long now = System.currentTimeMillis();
            NoForgetItem item = new NoForgetItem(
                    now, value, "", "[]",
                    priority.getSelectedItemPosition(),
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
