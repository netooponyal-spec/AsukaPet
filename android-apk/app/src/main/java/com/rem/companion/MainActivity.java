package com.rem.companion;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;
import android.graphics.Color;

public class MainActivity extends Activity {
    private WebView webView;
    private static final int OVERLAY_REQUEST = 41;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    private void showHome() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 36, 28, 24);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setBackgroundColor(Color.rgb(24, 24, 30));

        TextView title = new TextView(this);
        title.setText("Рэм — компаньон");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-1, 72));

        TextView info = new TextView(this);
        info.setText("Включи режим поверх других приложений, чтобы Рэм оставался на экране. Перетаскивай его пальцем. Нажатие на персонажа показывает быстрые действия.");
        info.setTextColor(Color.LTGRAY);
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(-1, -2);
        ip.setMargins(0, 12, 0, 28);
        root.addView(info, ip);

        Button start = new Button(this);
        start.setText("Показать Рэм поверх приложений");
        root.addView(start, new LinearLayout.LayoutParams(-1, -2));
        start.setOnClickListener(v -> enableOverlay());

        TextView diagnostics = new TextView(this);
        diagnostics.setText("ДИАГНОСТИКА 3D\\nПроверка перенесена сюда. Для проверки модели открой 3D-сцену в приложении. Если персонаж не виден, проверь интернет и пришли сюда текст ошибки.");
        diagnostics.setTextColor(Color.WHITE);
        diagnostics.setTextSize(14);
        diagnostics.setPadding(14, 14, 14, 14);
        diagnostics.setBackgroundColor(Color.rgb(42, 42, 52));
        LinearLayout.LayoutParams dp = new LinearLayout.LayoutParams(-1, -2);
        dp.topMargin = 18;
        root.addView(diagnostics, dp);

        Button diagnose = new Button(this);
        diagnose.setText("Проверить 3D в приложении");
        LinearLayout.LayoutParams diagp = new LinearLayout.LayoutParams(-1, -2);
        diagp.topMargin = 10;
        root.addView(diagnose, diagp);
        diagnose.setOnClickListener(v -> showWebsite());

        Button site = new Button(this);
        site.setText("Открыть веб-версию");
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.topMargin = 12;
        root.addView(site, sp);
        site.setOnClickListener(v -> showWebsite());

        setContentView(root);
    }

    private void enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_REQUEST);
            return;
        }
        Intent service = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(service);
        else startService(service);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_REQUEST) {
            if (Settings.canDrawOverlays(this)) enableOverlay();
        }
    }

    private void showWebsite() {
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        request.grant(request.getResources());
                    } else {
                        request.deny();
                        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 7);
                    }
                });
            }
        });
        setContentView(webView);
        webView.loadUrl("https://netooponyal-spec.github.io/AsukaPet/");
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
