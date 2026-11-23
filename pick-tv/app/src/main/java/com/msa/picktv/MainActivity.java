package com.msa.picktv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/** «اقترحلي» على التلفزيون: غلاف WebView لصفحة msa-pick، واليوتيوب بيفتح في SmartTube. */
public class MainActivity extends Activity {
    private static final String HOME = "https://msa-pick.drmsa30926-6ca.workers.dev/?tv=1";
    private WebView web;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0b1020"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setUserAgentString(s.getUserAgentString() + " AndroidTV MSAPickTV");
        web.setWebChromeClient(new WebChromeClient());
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                return route(r.getUrl());
            }
            @Override
            @SuppressWarnings("deprecation")
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                return route(Uri.parse(url));
            }
        });
        web.setFocusable(true);
        web.setFocusableInTouchMode(true);
        setContentView(web);
        web.requestFocus(View.FOCUS_DOWN);
        if (b != null) web.restoreState(b); else web.loadUrl(HOME);
    }

    /** أي لينك برّه msa-pick (يوتيوب وغيره) يتفتح في التطبيق المناسب على التلفزيون، زي SmartTube. */
    private boolean route(Uri u) {
        String host = u.getHost() == null ? "" : u.getHost();
        if (host.endsWith("workers.dev")) return false;
        // يوتيوب يتفتح في SmartTube (من غير إعلانات): بندوّر على أي تطبيق بيفتح لينكات يوتيوب غير يوتيوب الأصلي
        if (host.contains("youtube.com") || host.equals("youtu.be")) {
            if (openInSmartTube(u)) return true;
        }
        try {
            Intent i = "intent".equals(u.getScheme())
                    ? Intent.parseUri(u.toString(), Intent.URI_INTENT_SCHEME)
                    : new Intent(Intent.ACTION_VIEW, u);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private boolean openInSmartTube(Uri u) {
        android.content.pm.PackageManager pm = getPackageManager();
        // ١) أي تطبيق مسجّل إنه بيفتح لينكات يوتيوب، ما عدا يوتيوب الأصلي والمتصفحات
        try {
            Intent probe = new Intent(Intent.ACTION_VIEW, u);
            for (android.content.pm.ResolveInfo ri : pm.queryIntentActivities(probe, 0x00020000 /* MATCH_ALL */)) {
                String p = ri.activityInfo.packageName;
                if (p.startsWith("com.google.android.youtube") || p.equals(getPackageName()) || isBrowser(p)) continue;
                Intent i = new Intent(Intent.ACTION_VIEW, u);
                i.setClassName(p, ri.activityInfo.name);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            }
        } catch (Exception ignored) { }
        // ٢) أسماء SmartTube المعروفة، حتى لو مش مسجّل للينكات
        for (String pkg : SMARTTUBE) {
            try {
                pm.getPackageInfo(pkg, 0);
                Intent i = new Intent(Intent.ACTION_VIEW, u);
                i.setPackage(pkg);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            } catch (Exception ignored) { }
        }
        // ٣) أي تطبيق اسمه فيه smarttube
        try {
            for (android.content.pm.ApplicationInfo ai : pm.getInstalledApplications(0)) {
                String p = ai.packageName.toLowerCase();
                if (!p.contains("smarttube") && !p.contains("videomanager")) continue;
                Intent i = new Intent(Intent.ACTION_VIEW, u);
                i.setPackage(ai.packageName);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    private boolean isBrowser(String p) {
        return p.contains("browser") || p.contains("chrome") || p.contains("tvbro") || p.contains("firefox");
    }

    private static final String[] SMARTTUBE = {
            "com.teamsmart.videomanager.tv",      // SmartTube (F-Droid / stable)
            "com.liskovsoft.smarttubetv.beta",
            "com.liskovsoft.smarttubetv",
            "org.smarttube.stable",
            "org.smarttube.beta"
    };

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK) {
            web.evaluateJavascript("(window.__back&&window.__back())?1:0", res -> {
                if (!"1".equals(res)) finish();
            });
            return true;
        }
        return super.onKeyDown(code, e);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }
}
