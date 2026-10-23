package org.example.deepseektts;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class SettingsActivity extends Activity {
    private EditText endpoint, voice;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("engine", MODE_PRIVATE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);

        TextView title = new TextView(this);
        title.setText("DeepSeek TTS (prototype)");
        title.setTextSize(22);
        root.addView(title, lp());

        TextView note = new TextView(this);
        note.setText("Experimental only. A compatible ds-tts service must be running separately; this app does not connect to DeepSeek directly.");
        note.setPadding(0, 12, 0, 16);
        root.addView(note, lp());

        endpoint = new EditText(this);
        endpoint.setSingleLine(true);
        endpoint.setHint("Service base URL");
        endpoint.setText(prefs.getString("endpoint", "http://127.0.0.1:3080"));
        root.addView(endpoint, lp());

        voice = new EditText(this);
        voice.setSingleLine(true);
        voice.setHint("mira / echo / stella / tide");
        voice.setText(prefs.getString("voice", "mira"));
        root.addView(voice, lp());

        Button save = new Button(this);
        save.setText("Save settings");
        save.setOnClickListener(v -> {
            String url = endpoint.getText().toString().trim().replaceAll("/+$", "");
            String selected = voice.getText().toString().trim();
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Toast.makeText(this, "Enter a valid HTTP(S) URL", Toast.LENGTH_LONG).show();
                return;
            }
            prefs.edit().putString("endpoint", url)
                .putString("voice", selected.isEmpty() ? "mira" : selected).apply();
            Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
        });
        root.addView(save, lp());

        Button ttsSettings = new Button(this);
        ttsSettings.setText("Open Android text-to-speech settings");
        ttsSettings.setOnClickListener(v -> {
            try { startActivity(new Intent("com.android.settings.TTS_SETTINGS")); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
        root.addView(ttsSettings, lp());
        setContentView(root);
    }

    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }
}
