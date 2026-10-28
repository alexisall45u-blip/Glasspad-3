package com.glasspad.notes;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

public class QuickNoteActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.quick_note);
        Window w = getWindow();
        w.setLayout((int) (getResources().getDisplayMetrics().widthPixels * 0.92f), ViewGroup.LayoutParams.WRAP_CONTENT);
        w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        final EditText t = findViewById(R.id.qt);
        final EditText bd = findViewById(R.id.qb);
        bd.requestFocus();
        findViewById(R.id.qc).setOnClickListener(v -> finish());
        findViewById(R.id.qs).setOnClickListener(v -> {
            String title = t.getText().toString().trim();
            String body = bd.getText().toString().trim();
            if (title.isEmpty() && body.isEmpty()) { finish(); return; }
            try {
                SharedPreferences p = getSharedPreferences("CapacitorStorage", MODE_PRIVATE);
                JSONArray a = new JSONArray(p.getString("pending", "[]"));
                JSONObject o = new JSONObject();
                o.put("t", title);
                o.put("b", body);
                o.put("ts", System.currentTimeMillis());
                a.put(o);
                p.edit().putString("pending", a.toString()).apply();
                NotesWidget.refresh(this);
            } catch (Exception e) {}
            Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
