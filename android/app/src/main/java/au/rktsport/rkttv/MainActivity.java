package au.rktsport.rkttv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

/**
 * RKT TV app: a full-screen browser locked to the RKT TV site.
 * Content still comes from the website, so updates to index.html on GitHub reach the TV automatically.
 * Remembers the last club opened and goes straight back to it next time.
 */
public class MainActivity extends Activity {

    static final String HOME = "https://rktsport.github.io/rkttv/";
    static final String PREFS = "rkttv";
    static final long RETRY_MS = 30000;

    private WebView web;
    private SharedPreferences prefs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private long lastBack = 0;
    private boolean offline = false;

    private final Runnable retry = new Runnable() {
        @Override public void run() { offline = false; web.loadUrl(startUrl()); }
    };

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);   // lets YouTube slides play, including with sound
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setUserAgentString(s.getUserAgentString() + " RKTTV-App/1.0");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return !(url.startsWith("http://") || url.startsWith("https://"));   // ignore mailto: etc. on a TV
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return shouldOverrideUrlLoading(view, request.getUrl().toString());
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (url != null && url.startsWith(HOME) && !offline) {
                    String keep = url.contains("#") ? url.substring(0, url.indexOf('#')) : url;
                    prefs.edit().putString("url", keep).apply();
                }
                web.requestFocus();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showOffline();
            }

            @SuppressWarnings("deprecation")
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                if (failingUrl != null && failingUrl.equals(view.getUrl())) showOffline();
            }
        });

        web.loadUrl(startUrl());
    }

    private String startUrl() {
        String u = prefs.getString("url", HOME);
        return (u != null && u.startsWith(HOME)) ? u : HOME;
    }

    private void showOffline() {
        offline = true;
        String html = "<html><body style='margin:0;background:#07080d;color:#fff;font-family:sans-serif;display:flex;"
            + "align-items:center;justify-content:center;height:100vh;text-align:center'><div>"
            + "<div style='font-size:48px;font-weight:800;letter-spacing:2px'>RKT TV</div>"
            + "<div style='font-size:24px;margin-top:16px;opacity:.75'>Waiting for an internet connection&hellip;</div>"
            + "<div style='font-size:18px;margin-top:10px;opacity:.5'>Trying again every 30 seconds</div></div></body></html>";
        web.loadDataWithBaseURL(null, html, "text/html", "utf-8", null);
        handler.removeCallbacks(retry);
        handler.postDelayed(retry, RETRY_MS);   // if it fails again, onReceivedError brings us back here
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {          // Menu button = back to the club list
            web.loadUrl(HOME);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onBackPressed() {
        if (web.canGoBack()) { web.goBack(); return; }
        long now = System.currentTimeMillis();
        if (now - lastBack < 2500) { super.onBackPressed(); return; }
        lastBack = now;
        Toast.makeText(this, "Press Back again to exit RKT TV", Toast.LENGTH_SHORT).show();
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        int flags = View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
        if (Build.VERSION.SDK_INT >= 19) flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override protected void onResume() { super.onResume(); web.onResume(); hideSystemUi(); }
    @Override protected void onPause() { web.onPause(); super.onPause(); }
    @Override protected void onDestroy() { handler.removeCallbacksAndMessages(null); web.destroy(); super.onDestroy(); }
}
