package com.makeblock.xystage;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * Created with IntelliJ IDEA.
 * User: Administrator
 * Date: 13-10-6
 * Time: 下午4:36
 * To change this template use File | Settings | File Templates.
 */
public class InfoActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info);
        WebView webView = (WebView)findViewById(R.id.webView);
        webView.loadUrl("http://forum.makeblock.cc/t/xy-plotter-application-for-andoid/228/");
        webView.getSettings().setUseWideViewPort(false);
        webView.getSettings().setLoadWithOverviewMode(false);


    }
}
