package top.yemanqi.pokecatch;

import android.app.Activity;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoSessionSettings;
import org.mozilla.geckoview.GeckoView;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    // 直接连官方地址。Gecko 自带 DNS，能在 IPv6 不通时回落到 IPv4（fast-fallback-to-IPv4），
    // 而且 location.origin 与官方一致 —— Supabase 的登录回调白名单才认，登录/云存档才能用。
    private static final String START_URL = "https://pokecatch-online.netlify.app/";

    private GeckoRuntime runtime;
    private GeckoSession session;
    private GeckoView geckoView;

    private LinearLayout offlineView;
    private LinearLayout loadingView;
    private ProgressBar loadingBar;
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        geckoView = new GeckoView(this);
        geckoView.setBackgroundColor(Color.BLACK);
        root.addView(geckoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // 加载提示
        loadingView = new LinearLayout(this);
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

        startGecko();

        if (!isOnline()) showOffline();
    }

    private void startGecko() {
        // GeckoView 支持从一个 YAML 读 prefs。APK 是 debug 签名（debuggable），这条路可用。
        // 三条关键 pref：
        //   network.dns.disableIPv6        —— 完全不查 AAAA，避开运营商断掉的 IPv6 路径
        //   network.http.fast-fallback-to-IPv4 —— 万一同一条连接里 IPv6 已排上，5 秒内回退 IPv4
        //   network.dns.native             —— 用系统解析器（Android 的 getaddrinfo），保持与系统一致
        File cfg = new File(getFilesDir(), "geckoview-config.yaml");
        try (FileOutputStream out = new FileOutputStream(cfg)) {
            String yaml = "prefs:\n"
                    + "  network.dns.disableIPv6: true\n"
                    + "  network.http.fast-fallback-to-IPv4: true\n"
                    + "  network.dns.native: true\n";
            out.write(yaml.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            // 写不进去也不致命，只是少了强制 IPv4
        }

        GeckoRuntimeSettings settings = new GeckoRuntimeSettings.Builder()
                .configFilePath(cfg.getAbsolutePath())
                .remoteDebuggingEnabled(false)
                .build();
        runtime = GeckoRuntime.create(getApplicationContext(), settings);

        session = new GeckoSession();
        GeckoSessionSettings ss = session.getSettings();
        ss.setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
        ss.setViewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE);
        ss.setAllowJavascript(true);
        ss.setConsoleOutputEnabled(false);

        session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
            @Override
            public void onProgressChange(GeckoSession s, int progress) {
                if (progress >= 90) hideLoading();
            }

            @Override
            public void onPageStop(GeckoSession s, boolean success) {
                if (success) {
                    offlineView.setVisibility(View.GONE);
                    geckoView.setVisibility(View.VISIBLE);
                    geckoView.postDelayed(MainActivity.this::hideLoading, 3000);
                } else {
                    showOffline();
                }
            }
        });

        session.setContentDelegate(new GeckoSession.ContentDelegate() {
            @Override
            public GeckoSession onCreateWindow(GeckoSession opener,
                                               @org.mozilla.geckoview.GeckoSession.ContentDelegate.OpenWindowInfo info) {
                // 谷歌登录有时用 window.open 弹窗。开一个新 session 接住，不丢。
                GeckoSession popup = new GeckoSession();
                popup.getSettings().setUserAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE);
                popup.open(runtime);
                return popup;
            }
        });

        session.open(runtime);
        geckoView.setSession(session);
        session.setActive(true);
        session.loadUri(START_URL);
    }

    private void hideLoading() {
        loadingView.setVisibility(View.GONE);
        loadingBar.setVisibility(View.GONE);
        loadingText.setVisibility(View.GONE);
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
                geckoView.setVisibility(View.VISIBLE);
                if (session != null) session.loadUri(START_URL);
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
        geckoView.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && session != null
                && session.getHistoryDelegate() == null) {
            final boolean[] handled = {false};
            session.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }
}
