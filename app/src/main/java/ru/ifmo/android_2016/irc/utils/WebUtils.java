package ru.ifmo.android_2016.irc.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

/**
 * Created by Dima Stoyanov on 31.10.2016.
 * Project Android-IRC
 * Start time : 17:00
 */

public final class WebUtils {

    private final static String TAG = WebUtils.class.getSimpleName();


    @SuppressWarnings("deprecation")
    public static void clearWebviewCookie(Context context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.d(TAG, "Using clearCookies code for API >=" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else {
            Log.d(TAG, "Using clearCookies code for API <" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieSyncManager cookieSyncMngr = CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }



    @Nullable
    public static String getAccessToken(@NonNull String url, @NonNull Context context) {
        final Uri uri = Uri.parse(url);
        final String fragment = uri.getFragment();

        if (fragment == null)
            return null;

        String error = null;
        String accessToken = null;
        String sessionSecretKey = null;

        int off = 0;
        int equalSignPosition;
        int length = fragment.length();

        while (off < length && (equalSignPosition = fragment.indexOf('=', off)) != -1) {
            final String key = fragment.substring(off, equalSignPosition);
            final int andSignPosition = fragment.indexOf('&', equalSignPosition + 1);
            final int valueEnd = andSignPosition > equalSignPosition ? andSignPosition : length;
            final String value = fragment.substring(equalSignPosition + 1, valueEnd);
            switch (key) {
                case "access_token":
                    accessToken = value;
                    break;
                case "session_secret_key":
                    sessionSecretKey = value;
                    break;
                case "error":
                    error = value;
                    break;
            }
            off = valueEnd + 1;
        }

        final Intent data = new Intent();
        if (!TextUtils.isEmpty(accessToken) /*&& !TextUtils.isEmpty(sessionSecretKey)*/) {
            SessionStore.getInstance().updateKeys(context, accessToken, sessionSecretKey);
        }
        if (!TextUtils.isEmpty(error)) {
            data.putExtra("error", error);
        }
        return accessToken;
    }
}
