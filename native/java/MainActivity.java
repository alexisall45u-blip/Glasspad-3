package com.glasspad.notes;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    private volatile String ins = "0,0,0,0";

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        try {
            final WebView wv = getBridge().getWebView();
            wv.addJavascriptInterface(new Object() {
                @JavascriptInterface
                public String get() { return ins; }

                @JavascriptInterface
                public void style(final boolean dark) {
                    runOnUiThread(() -> {
                        WindowInsetsControllerCompat c = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
                        c.setAppearanceLightStatusBars(!dark);
                        c.setAppearanceLightNavigationBars(!dark);
                    });
                }
            }, "GPInsets");
            final View content = findViewById(android.R.id.content);
            ViewCompat.setOnApplyWindowInsetsListener(content, (v, w) -> {
                Insets s = w.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
                boolean kb = w.isVisible(WindowInsetsCompat.Type.ime());
                int kbh = w.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                v.setPadding(0, 0, 0, (kb && Build.VERSION.SDK_INT >= 30) ? kbh : 0);
                float d = getResources().getDisplayMetrics().density;
                ins = (s.top / d) + "," + (kb ? 0f : s.bottom / d) + "," + (s.left / d) + "," + (s.right / d);
                final String js = "window.__gp&&window.__gp('" + ins + "')";
                wv.post(() -> wv.evaluateJavascript(js, null));
                return WindowInsetsCompat.CONSUMED;
            });
            WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
            if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
            ViewCompat.requestApplyInsets(content);
        } catch (Exception e) {}
    }

    @Override
    public void onPause() {
        super.onPause();
        try { NotesWidget.refresh(this); } catch (Exception e) {}
    }
}
