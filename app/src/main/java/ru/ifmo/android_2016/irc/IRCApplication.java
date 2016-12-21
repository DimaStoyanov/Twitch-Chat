package ru.ifmo.android_2016.irc;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.drawee.backends.pipeline.Fresco;

import java.util.Calendar;

import ru.ifmo.android_2016.irc.api.bettertwitchtv.emotes.BttvEmotesLoader;
import ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadgesLoader;
import ru.ifmo.android_2016.irc.api.twitch.emotes.TwitchEmotesLoader;
import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.drawee.DraweeSpan;
import ru.ifmo.android_2016.irc.utils.FileUtils;

/**
 * Created by ghost on 11/14/2016.
 */

public class IRCApplication extends android.app.Application {
    private static Handler mainThread;
    private static LocalBroadcastManager localBroadcastManager;
    private static String filesDir;
    private static String cacheDir;
    private static boolean debug = false;

    private Thread.UncaughtExceptionHandler old;
    private SharedPreferences preferences;
    private SharedPreferences.OnSharedPreferenceChangeListener listener =
            this::onSharedPreferences;

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable) -> {
        ServerList.save(filesDir);

        String filename = cacheDir + "/log " + java.text.DateFormat.getDateTimeInstance()
                .format(Calendar.getInstance().getTime());

        FileUtils.writeObjectToFile(filename, throwable);

        old.uncaughtException(thread, throwable);
    };

    public static void runOnUiThread(Runnable runnable) {
        mainThread.post(runnable);
    }

    public static LocalBroadcastManager getBroadcastManager() {
        return localBroadcastManager;
    }

    public static String getFilesDirectory() {
        return filesDir;
    }

    public static boolean isDebug() {
        return debug;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Fresco.initialize(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        filesDir = getFilesDir().toString();
        cacheDir = getCacheDir().toString();

        DraweeSpan.dp = getResources().getDisplayMetrics().density / 2;

        old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

        mainThread = new Handler(Looper.getMainLooper());

        new TwitchBadgesLoader().execute();
        new TwitchEmotesLoader().execute(0);
        new BttvEmotesLoader().execute();

        preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        preferences.registerOnSharedPreferenceChangeListener(listener);
    }

    private void onSharedPreferences(SharedPreferences sharedPreferences, String key) {

    }
}
