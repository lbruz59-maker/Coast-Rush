package com.lbruz.coastrush;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.view.Choreographer;
import android.view.Display;
import android.util.DisplayMetrics;
import androidx.webkit.WebViewAssetLoader;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private final NativeBridge bridge = new NativeBridge();

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUi();
        assetLoader = new WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
            .build();
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF050B1B);
        webView.setFocusable(true);
        webView.setFocusableInTouchMode(true);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(true);
        s.setTextZoom(100);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        webView.addJavascriptInterface(bridge, "CoastRushFrameStats");
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                publishDisplayInfo();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) android.util.Log.e("CoastRush", "WebView load error: " + (error == null ? "unknown" : error.getDescription()));
            }
        });
        setContentView(webView);
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        webView.requestFocus();
        bridge.start();
    }

    private void publishDisplayInfo() {
        if (webView == null) return;
        DisplayMetrics real = new DisplayMetrics();
        Display display = getWindowManager().getDefaultDisplay();
        display.getRealMetrics(real);
        float hz = display.getRefreshRate();
        int width = Math.max(1, webView.getWidth());
        int height = Math.max(1, webView.getHeight());
        float density = getResources().getDisplayMetrics().density;
        String js = "window.__COAST_RUSH_NATIVE_DISPLAY__={screenPxWidth:" + real.widthPixels +
            ",screenPxHeight:" + real.heightPixels + ",webViewPxWidth:" + width +
            ",webViewPxHeight:" + height + ",density:" + density +
            ",refreshRateHz:" + hz + "};";
        webView.evaluateJavascript(js, null);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    private boolean sendRemoteKey(KeyEvent event) {
        if (webView == null) return false;
        String key;
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT: key = "ArrowLeft"; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: key = "ArrowRight"; break;
            case KeyEvent.KEYCODE_DPAD_UP: key = "ArrowUp"; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: key = "ArrowDown"; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER: key = "Enter"; break;
            case KeyEvent.KEYCODE_BUTTON_A: key = "Enter"; break;
            case KeyEvent.KEYCODE_BACK: return false;
            default: return false;
        }
        if (event.getAction() != KeyEvent.ACTION_DOWN && event.getAction() != KeyEvent.ACTION_UP) return false;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() > 0) return true;
        final String jsKey = JSONObject.quote(key);
        final String type = event.getAction() == KeyEvent.ACTION_DOWN ? "keydown" : "keyup";
        webView.evaluateJavascript("(function(){var k=" + jsKey + ";var e=new KeyboardEvent('" + type + "',{key:k,code:k,bubbles:true,cancelable:true});window.dispatchEvent(e);})();", null);
        return true;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (webView != null) webView.evaluateJavascript("if(typeof state!=='undefined' && state!=='menu'){if(typeof toMenu==='function')toMenu();}", null);
            return true;
        }
        if (sendRemoteKey(event)) return true;
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onPause() {
        bridge.stop();
        if (webView != null) {
            webView.evaluateJavascript("window.__coastRushLifecycle && window.__coastRushLifecycle('pause')", null);
            webView.onPause();
        }
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        hideSystemUi();
        if (webView != null) {
            webView.onResume();
            webView.evaluateJavascript("window.__coastRushLifecycle && window.__coastRushLifecycle('resume')", null);
            webView.post(this::publishDisplayInfo);
        }
        bridge.start();
    }

    @Override public void onBackPressed() {
        if (webView != null) webView.evaluateJavascript("if(typeof toMenu==='function')toMenu()", null);
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        bridge.stop();
        if (webView != null) { webView.stopLoading(); webView.destroy(); webView = null; }
        super.onDestroy();
    }

    private final class NativeBridge implements Choreographer.FrameCallback {
        private boolean running = false;
        private long callbacks = 0, samples = 0, totalNs = 0, maxNs = 0, previousNs = 0, startNs = 0, delayed = 0;

        void start() {
            if (running) return;
            running = true;
            previousNs = 0;
            startNs = System.nanoTime();
            Choreographer.getInstance().postFrameCallback(this);
        }
        void stop() {
            running = false;
            Choreographer.getInstance().removeFrameCallback(this);
        }
        @Override public void doFrame(long frameTimeNanos) {
            if (!running) return;
            synchronized (this) {
                callbacks++;
                if (previousNs > 0 && frameTimeNanos > previousNs) {
                    long gap = frameTimeNanos - previousNs;
                    totalNs += gap; maxNs = Math.max(maxNs, gap); samples++;
                    if (gap > 33400000L) delayed++;
                }
                previousNs = frameTimeNanos;
            }
            Choreographer.getInstance().postFrameCallback(this);
        }
        @JavascriptInterface public synchronized String getStats() {
            long now = System.nanoTime();
            double elapsed = Math.max(1L, now - startNs);
            double fps = samples * 1000000000.0 / elapsed;
            double avg = samples == 0 ? 0 : totalNs / 1000000.0 / samples;
            JSONObject out = new JSONObject();
            try {
                out.put("bridge", "ok");
                out.put("nativeFps", Math.round(fps * 10.0) / 10.0);
                out.put("nativeAvgMs", Math.round(avg * 100.0) / 100.0);
                out.put("nativeMaxMs", maxNs / 1000000.0);
                out.put("nativeDelayedCallbacks", delayed);
                out.put("nativeSamples", samples);
                out.put("nativeCallbacks", callbacks);
                out.put("targetHz", getWindowManager().getDefaultDisplay().getRefreshRate());
                out.put("lastCallbackAgeMs", previousNs == 0 ? -1 : Math.max(0, (now - previousNs) / 1000000L));
            } catch (Exception ignored) { }
            startNs = now; totalNs = 0; maxNs = 0; samples = 0; delayed = 0;
            return out.toString();
        }
        @JavascriptInterface public void exitApp() { runOnUiThread(() -> finish()); }
    }
}