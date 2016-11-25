package ru.ifmo.android_2016.irc;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.facebook.drawee.backends.pipeline.Fresco;

import java.util.Calendar;

import ru.ifmo.android_2016.irc.api.bettertwitchtv.BttvEmotesLoaderTask;
import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.drawee.DraweeSpan;
import ru.ifmo.android_2016.irc.utils.FileUtils;

/**
 * Created by ghost on 11/14/2016.
 */

public class IRCApplication extends android.app.Application {
    private Thread.UncaughtExceptionHandler old;

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable) -> {
        ServerList.save(filesDir);

        String filename = cacheDir + "/log " + java.text.DateFormat.getDateTimeInstance()
                .format(Calendar.getInstance().getTime());

        FileUtils.writeObjectToFile(filename, throwable);

        old.uncaughtException(thread, throwable);
    };

    private static Handler mainThread;
    private static LocalBroadcastManager localBroadcastManager;
    private static String filesDir;

    private static String cacheDir;

    public static void runOnUiThread(Runnable runnable) {
        mainThread.post(runnable);
    }

    public static LocalBroadcastManager getBroadcastManager() {
        return localBroadcastManager;
    }

    public static String getFilesDirectory() {
        return filesDir;
    }

    @Override
    public void onCreate() {
        Fresco.initialize(this);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        filesDir = getFilesDir().toString();
        cacheDir = getCacheDir().toString();
        new BttvEmotesLoaderTask().execute();

        DraweeSpan.dp = getResources().getDisplayMetrics().density / 2;

        //TODO: Uncaught Exceptions on the screen
        old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);

        mainThread = new Handler(Looper.getMainLooper());

        super.onCreate();
    }
}
