package ru.ifmo.android_2016.irc.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

/**
 * Created by dtrunin on 05.04.2015.
 */

public final class SessionStore {

    // Синглетон: статическое поле instance инициализируется при первом "обращении" к классу
    //            SessionStore в коде приложения. Синхронизация выполняется виртуальной машиной
    //            при загрузке класса.
    private static SessionStore instance = new SessionStore();

    public static SessionStore getInstance() {
        return instance;
    }

    private Session session;
    private volatile SharedPreferences prefs;

    public synchronized Session getSession(Context context) {
        if (session == null) {
            session = readFromPrefs(context);
        }
        return session;
    }

    public synchronized void updateKeys(Context context,
                                        String accessToken, String sessionSecretKey) {
        if (session == null) {
            session = new Session();
        }
        session.setKeys(accessToken, sessionSecretKey);
        saveToPrefs(context, accessToken, sessionSecretKey);
    }

    private Session readFromPrefs(Context context) {
        final SharedPreferences prefs = getPrefs(context);
        final Session session = new Session();
        final String accessToken = prefs.getString(KEY_ACCESS_TOKEN, null);
        final String sessionSecretKey = prefs.getString(KEY_SECRET_KEY, null);
        if (!TextUtils.isEmpty(accessToken) && !TextUtils.isEmpty(sessionSecretKey)) {
            session.setKeys(accessToken, sessionSecretKey);
        }
        return session;
    }

    private void saveToPrefs(Context context, String accessToken, String sessionSecretKey) {
        final SharedPreferences.Editor editor = getPrefs(context).edit();
        if (accessToken == null) {
            editor.remove(KEY_ACCESS_TOKEN);
        } else {
            editor.putString(KEY_ACCESS_TOKEN, accessToken);
        }
        if (sessionSecretKey == null) {
            editor.remove(KEY_SECRET_KEY);
        } else {
            editor.putString(KEY_SECRET_KEY, sessionSecretKey);
        }
        editor.apply();
    }

    private SharedPreferences getPrefs(Context context) {
        if (prefs == null) {
            synchronized (SessionStore.class) {
                if (prefs == null) {
                    prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                }
            }
        }
        return prefs;
    }

    private SessionStore() {
    }

    private static final String PREFS_NAME = "session";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_SECRET_KEY = "secret_key";
}
