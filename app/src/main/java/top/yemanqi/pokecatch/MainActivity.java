package top.yemanqi.pokecatch;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String START_URL = "https://pokecatch-online.netlify.app/";

    private WebView webView;
    private LinearLayout offlineView;
    private ProgressBar loadingBar;
    private TextView loadingText;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        // 游戏用 https + 跨域 CDN，混合内容一律放行没有意义，保持严格
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(false);
        }
        // 站点依赖 Service Worker 缓存游戏资源；WebView 默认不启用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            android.webkit.ServiceWorkerController.getInstance()
                    .setServiceWorkerClient(new android.webkit.ServiceWorkerClient() {
                        @Override
                        public android.webkit.WebResourceResponse shouldInterceptRequest(
                                WebResourceRequest request) {
                            return null;
                        }
                    });
        }

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 90) hideLoading();
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("pokecatch-online")) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                } catch (Exception ignored) { }
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                offlineView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                // 网页壳加载完了，但游戏是异步起的，多等一会再收起进度条
                view.postDelayed(MainActivity.this::hideLoading, 3500);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showOffline();
            }
        });

        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 加载提示：一个居中的进度圈 + 一行小字，避免黑屏看不出状态
        LinearLayout loadingView = new LinearLayout(this);
        loadingView.setOrientation(LinearLayout.VERTICAL);
        loadingView.setGravity(Gravity.CENTER);
        loadingView.setBackgroundColor(Color.BLACK);

        loadingBar = new ProgressBar(this);
        loadingBar.setIndeterminate(true);

        loadingText = new TextView(this);
        loadingText.setText("正在加载…");
        loadingText.setTextColor(Color.parseColor("#8B93A7"));
        loadingText.setTextSize(12f);
        loadingText.setGravity(Gravity.CENTER);
        loadingText.setPadding(0, 24, 0, 0);

        loadingView.addView(loadingBar);
        loadingView.addView(loadingText);

        offlineView = buildOfflineView();
        offlineView.setVisibility(View.GONE);

        root.addView(loadingView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(offlineView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);

        if (isOnline()) {
            webView.loadUrl(START_URL);
        } else {
            showOffline();
        }
    }

    private void hideLoading() {
        loadingBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
        loadingBar.getRootView();
        if (loadingBar.getParent() instanceof View) {
            ((View) loadingBar.getParent()).setVisibility(View.GONE);
        }
    }

    private LinearLayout buildOfflineView() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.parseColor("#111318"));
        box.setPadding(64, 64, 64, 64);

        TextView title = new TextView(this);
        title.setText("连不上");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22f);
        title.setGravity(Gravity.CENTER);

        TextView desc = new TextView(this);
        desc.setText("检查一下网络，然后重试。");
        desc.setTextColor(Color.parseColor("#8B93A7"));
        desc.setTextSize(14f);
        desc.setGravity(Gravity.CENTER);
        desc.setPadding(0, 16, 0, 40);

        Button retry = new Button(this);
        retry.setText("重试");
        retry.setOnClickListener(v -> {
            if (isOnline()) {
                offlineView.setVisibility(View.GONE);
                webView.setVisibility(View.VISIBLE);
                webView.loadUrl(START_URL);
            }
        });

        box.addView(title);
        box.addView(desc);
        box.addView(retry);
        return box;
    }

    private boolean isOnline() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            NetworkInfo ni = cm.getActiveNetworkInfo();
            return ni != null && ni.isConnected();
        } catch (Exception e) {
            return true;
        }
    }

    private void showOffline() {
        hideLoading();
        offlineView.setVisibility(View.VISIBLE);
        webView.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        webView.restoreState(savedInstanceState);
    }
}
