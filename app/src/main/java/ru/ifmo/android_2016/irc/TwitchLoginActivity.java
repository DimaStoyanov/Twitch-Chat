package ru.ifmo.android_2016.irc;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.constant.TwitchApiConstant;

public class TwitchLoginActivity extends AppCompatActivity {
    private static final String TAG = TwitchLoginActivity.class.getSimpleName();
    private static final Pattern accessToken = Pattern.compile("access_token=([a-z0-9]*)");

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_twitch_login);
        final WebView webView = (WebView) findViewById(R.id.webView);
        final ProgressBar progressBar = (ProgressBar) findViewById(R.id.progressBar);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                super.onPageFinished(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith(TwitchApiConstant.REDIRECT_URL)) {
                    Matcher matcher;
                    if ((matcher = accessToken.matcher(Uri.parse(url).getFragment())).find()) {
                        setResult(200, new Intent()
                                .putExtra("ru.ifmo.android_2016.irc.Token", matcher.group(1)));
                    } else {
                        setResult(404, new Intent());
                    }
                    finish();
                    return true;
                }
                return false;
            }
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(TwitchApiConstant.OAUTH_URL);
    }
}
