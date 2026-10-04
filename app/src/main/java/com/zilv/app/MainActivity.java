package com.zilv.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(Locale.SIMPLIFIED_CHINESE);
                tts.setSpeechRate(0.95f);
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
            }
        });

        webView = new WebView(this);
        setContentView(webView);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl("file:///android_asset/index.html");
    }

    @Override public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack(); else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    public class Bridge {
        @JavascriptInterface public void copyText(String text) {
            ClipboardManager cm=(ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("自律-给AI的项目说明", text));
            runOnUiThread(() -> Toast.makeText(MainActivity.this,"已复制，可直接粘贴给ChatGPT",Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface public void speak(String text) {
            runOnUiThread(() -> {
                if (ttsReady && tts != null && text != null && !text.trim().isEmpty()) {
                    tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "zilv_prompt");
                }
            });
        }

        @JavascriptInterface public void stopSpeak() {
            runOnUiThread(() -> { if (tts != null) tts.stop(); });
        }

        @JavascriptInterface public void vibrate(int ms) {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(Math.max(30, ms), VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(Math.max(30, ms));
            }
        }

        @JavascriptInterface public void vibratePattern() {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            long[] pattern = new long[]{0,120,90,120};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            } else {
                v.vibrate(pattern, -1);
            }
        }
    }
}
