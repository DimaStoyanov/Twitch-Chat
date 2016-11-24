package ru.ifmo.android_2016.irc;

import android.os.Handler;
import android.os.Looper;

import com.facebook.drawee.backends.pipeline.Fresco;

import ru.ifmo.android_2016.irc.drawee.DraweeSpan;

/**
 * Created by ghost on 11/14/2016.
 */

public class IRCApplication extends android.app.Application {
    private Thread.UncaughtExceptionHandler old;

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable) -> {
        old.uncaughtException(thread, throwable);
    };

    private static Handler mainThread;

    public static void runOnUiThread(Runnable runnable) {
        mainThread.post(runnable);
    }

    @Override
    public void onCreate() {
        Fresco.initialize(this);

        DraweeSpan.dp = getResources().getDisplayMetrics().density / 2;

        //TODO: Uncaught Exceptions on the screen
        Fresco.initialize(this);
        old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        super.onCreate();

        mainThread = new Handler(Looper.getMainLooper());
    }
}
