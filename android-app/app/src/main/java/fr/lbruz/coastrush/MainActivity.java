package fr.lbruz.coastrush;

import android.app.Activity;
import android.os.Bundle;
import android.view.Choreographer;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.util.DisplayMetrics;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.webkit.WebViewAssetLoader;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private WebView webView;
    private WebViewAssetLoader assetLoader;
    private final NativeFrameStats frameStats = new NativeFrameStats();

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
        webView.addJavascriptInterface(frameStats, "CoastRushFrameStats");
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
                if (request.isForMainFrame()) showLoadError(error == null ? "Erreur WebView inconnue" : error.getDescription().toString());
            }
        });
        setContentView(webView);
        frameStats.start();
        webView.loadUrl("https://appassets.androidplatform.net/assets/index.html");
        webView.requestFocus();
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

    private void showLoadError(String detail) {
        String safe = detail == null ? "Erreur inconnue" : detail.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        webView.loadDataWithBaseURL("https://appassets.androidplatform.net/", "<meta name='viewport' content='width=device-width,initial-scale=1'><body style='background:#071326;color:white;font:18px sans-serif;padding:24px'><h2>Coast Rush</h2><p>Impossible de charger le jeu local.</p><p style='color:#9cecff'>" + safe + "</p><p>Vérifiez que index.html est présent dans les ressources de l’application.</p></body>", "text/html", "UTF-8", null);
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

    private boolean dispatchRemoteKey(KeyEvent event) {
        if (webView == null) return false;
        int code = event.getKeyCode();
        if (code == KeyEvent.KEYCODE_BACK) return false;
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (!down && event.getAction() != KeyEvent.ACTION_UP) return false;
        String key = null;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_UP: key = "ArrowUp"; break;
            case KeyEvent.KEYCODE_DPAD_DOWN: key = "ArrowDown"; break;
            case KeyEvent.KEYCODE_DPAD_LEFT: key = "ArrowLeft"; break;
            case KeyEvent.KEYCODE_DPAD_RIGHT: key = "ArrowRight"; break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_BUTTON_A: key = "Enter"; break;
            default: return false;
        }
        if (down && event.getRepeatCount() > 0) return true;
        if (down && (key.equals("ArrowUp") || key.equals("ArrowDown") || key.equals("ArrowLeft") || key.equals("ArrowRight"))) {
            final int delta = (key.equals("ArrowDown") || key.equals("ArrowRight")) ? 1 : -1;
            webView.evaluateJavascript("(function(){var m=document.getElementById('menu');if(!m||getComputedStyle(m).display==='none')return;var a=[].slice.call(m.querySelectorAll('button,input')).filter(function(x){return x.getClientRects().length&& !x.disabled;});if(!a.length)return;var i=a.indexOf(document.activeElement);i=(i+(" + delta + ")+a.length)%a.length;a[i].focus();a[i].scrollIntoView({block:'nearest'});})();", null);
            return true;
        }
        if (down && key.equals("Enter")) {
            webView.evaluateJavascript("(function(){var m=document.getElementById('menu');if(m&&getComputedStyle(m).display!=='none'){var a=document.activeElement;if(!a||!m.contains(a)||typeof a.click!=='function')a=document.getElementById('go');if(a)a.click();return;}var e=new KeyboardEvent('keydown',{key:'Enter',code:'Enter',bubbles:true,cancelable:true});window.dispatchEvent(e);})();", null);
            return true;
        }
        if (!down) {
            String jsKey = JSONObject.quote(key);
            webView.evaluateJavascript("(function(){var k=" + jsKey + ";var e=new KeyboardEvent('keyup',{key:k,code:k,bubbles:true,cancelable:true});window.dispatchEvent(e);})();", null);
        }
        return true;
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN) {
            if (webView != null) webView.evaluateJavascript("if(typeof state!=='undefined' && state!=='menu'){ if(typeof endDuel==='function' && duelW) endDuel(); else if(typeof toMenu==='function') toMenu(); } else if(window.CoastRushFrameStats&&CoastRushFrameStats.exitApp) CoastRushFrameStats.exitApp();", null);
            return true;
        }
        if (dispatchRemoteKey(event)) return true;
        return super.dispatchKeyEvent(event);
    }

    @Override protected void onPause() {
        frameStats.stop();
        if (webView != null) webView.evaluateJavascript("window.__coastRushNativePaused=true", null);
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
            webView.evaluateJavascript("window.__coastRushNativePaused=false", null);
            webView.post(this::publishDisplayInfo);
        }
        frameStats.start();
        hideSystemUi();
    }

    @Override protected void onDestroy() {
        frameStats.stop();
        if (webView != null) { webView.stopLoading(); webView.destroy(); webView = null; }
        super.onDestroy();
    }

    private final class NativeFrameStats implements Choreographer.FrameCallback {
        private boolean running = false;
        private long previousFrameNs = 0L;
        private long windowStartNs = 0L;
        private long intervalTotalNs = 0L;
        private long intervalMaxNs = 0L;
        private long callbacksTotal = 0L;
        private long samples = 0L;
        private long delayed = 0L;
        private long lastFrameNs = 0L;

        void start() {
            if (running) return;
            running = true;
            previousFrameNs = 0L;
            windowStartNs = System.nanoTime();
            Choreographer.getInstance().postFrameCallback(this);
        }

        void stop() {
            running = false;
            Choreographer.getInstance().removeFrameCallback(this);
        }

        @Override public void doFrame(long frameTimeNanos) {
            if (!running) return;
            synchronized (this) {
                callbacksTotal++;
                lastFrameNs = frameTimeNanos;
                if (previousFrameNs > 0L && frameTimeNanos > previousFrameNs) {
                    long gap = frameTimeNanos - previousFrameNs;
                    intervalTotalNs += gap;
                    intervalMaxNs = Math.max(intervalMaxNs, gap);
                    samples++;
                    if (gap > 33_400_000L) delayed++;
                }
                previousFrameNs = frameTimeNanos;
            }
            Choreographer.getInstance().postFrameCallback(this);
        }

        @JavascriptInterface public void exitApp() { runOnUiThread(() -> finish()); }

        @JavascriptInterface public synchronized String getStats() {
            long now = System.nanoTime();
            long elapsed = Math.max(1L, now - windowStartNs);
            double fps = samples * 1_000_000_000.0 / elapsed;
            double avgMs = samples == 0 ? 0.0 : intervalTotalNs / 1_000_000.0 / samples;
            double maxMs = intervalMaxNs / 1_000_000.0;
            long ageMs = lastFrameNs == 0L ? -1L : Math.max(0L, (now - lastFrameNs) / 1_000_000L);
            double targetHz = getWindowManager().getDefaultDisplay().getRefreshRate();
            JSONObject out = new JSONObject();
            try {
                out.put("bridge", "ok");
                out.put("nativeFps", Math.round(fps * 10.0) / 10.0);
                out.put("nativeAvgMs", Math.round(avgMs * 100.0) / 100.0);
                out.put("nativeMaxMs", Math.round(maxMs * 100.0) / 100.0);
                out.put("nativeDelayedCallbacks", delayed);
                out.put("nativeSamples", samples);
                out.put("nativeCallbacks", callbacksTotal);
                out.put("targetHz", Math.round(targetHz * 10.0) / 10.0);
                out.put("lastCallbackAgeMs", ageMs);
            } catch (Exception ignored) { }
            windowStartNs = now;
            intervalTotalNs = 0L;
            intervalMaxNs = 0L;
            samples = 0L;
            delayed = 0L;
            return out.toString();
        }
    }
}