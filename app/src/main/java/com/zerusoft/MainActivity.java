package com.zerusoft;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class MainActivity extends AppCompatActivity {

    // ============================================================
    //  KONFIGURASI — Edit sesuai kebutuhan
    // ============================================================
    private static final String APP_URL = "https://www.zerusoft.web.id";
    private static final String APP_DOMAIN = "zerusoft.web.id";
    // ============================================================

    private WebView webView;
    private ProgressBar progressBar;
    private RelativeLayout rootLayout;
    private static final int FILE_CHOOSER_REQUEST = 1;
    private ValueCallback<Uri[]> filePathCallback;

    // ---- State buat HTML5 Fullscreen API (video/iframe fullscreen) ----
    private View fullscreenCustomView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private int savedSystemUiVisibility;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(0xFF0A0A0F);
        window.setNavigationBarColor(0xFF0A0A0F);

        rootLayout = new RelativeLayout(this);
        rootLayout.setLayoutParams(new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        RelativeLayout.LayoutParams pbp = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT, 8);
        pbp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        progressBar.setLayoutParams(pbp);

        webView = new WebView(this);
        webView.setLayoutParams(new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT));

        rootLayout.addView(webView);
        rootLayout.addView(progressBar);
        setContentView(rootLayout);

        // Beri padding agar konten tidak tertimpa status bar & navbar
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setBuiltInZoomControls(false);
        s.setAllowFileAccess(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String url = r.getUrl().toString();
                if (!url.contains(APP_DOMAIN)) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    return true;
                }
                return false;
            }
            @Override
            public void onPageFinished(WebView v, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView v, int p) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(p);
                if (p == 100) progressBar.setVisibility(View.GONE);
            }
            @Override
            public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> cb,
                                             FileChooserParams p) {
                filePathCallback = cb;
                startActivityForResult(p.createIntent(), FILE_CHOOSER_REQUEST);
                return true;
            }

            // ---- Ini yang bikin fullscreen (dari video ATAU dari
            // requestFullscreen() manapun di halaman web) beneran jalan
            // di dalam WebView. Tanpa 2 method di bawah ini, permintaan
            // fullscreen dari JavaScript nggak direspons sama sekali. ----
            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenCustomView != null) {
                    // udah ada view fullscreen lain yang aktif, tolak yang baru
                    callback.onCustomViewHidden();
                    return;
                }
                fullscreenCustomView = view;
                fullscreenCallback = callback;

                webView.setVisibility(View.GONE);
                rootLayout.addView(fullscreenCustomView, new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT));

                savedSystemUiVisibility = getWindow().getDecorView().getSystemUiVisibility();
                setImmersiveFullscreen(true);
            }

            @Override
            public void onHideCustomView() {
                if (fullscreenCustomView == null) return;

                webView.setVisibility(View.VISIBLE);
                rootLayout.removeView(fullscreenCustomView);
                fullscreenCustomView = null;

                if (fullscreenCallback != null) {
                    fullscreenCallback.onCustomViewHidden();
                    fullscreenCallback = null;
                }

                setImmersiveFullscreen(false);
                getWindow().getDecorView().setSystemUiVisibility(savedSystemUiVisibility);
            }
        });

        webView.loadUrl(APP_URL);
    }

    /** Sembunyiin/tampilin status bar & navigation bar pas mode fullscreen. */
    private void setImmersiveFullscreen(boolean enable) {
        if (enable) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == FILE_CHOOSER_REQUEST && filePathCallback != null) {
            Uri[] r = (res == Activity.RESULT_OK && data != null) ?
                new Uri[]{data.getData()} : null;
            filePathCallback.onReceiveValue(r);
            filePathCallback = null;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // Kalau lagi fullscreen, tombol back keluar dari fullscreen
            // dulu (bukan langsung nutup app/balik halaman).
            if (fullscreenCustomView != null) {
                if (fullscreenCallback != null) {
                    fullscreenCallback.onCustomViewHidden();
                } else {
                    webView.getWebChromeClient().onHideCustomView();
                }
                return true;
            }
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override protected void onPause() { super.onPause(); webView.onPause(); }
    @Override protected void onResume() { super.onResume(); webView.onResume(); }
}
