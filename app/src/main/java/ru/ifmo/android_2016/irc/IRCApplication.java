package ru.ifmo.android_2016.irc;

import android.content.Intent;

/**
 * Created by ghost on 11/14/2016.
 */

public class IrcApplication extends android.app.Application {
    private Thread.UncaughtExceptionHandler old;

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable) -> {
        old.uncaughtException(thread, throwable);
    };


    @Override
    public void onCreate() {
        //TODO: Uncaught Exceptions on the screen
        old = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
        super.onCreate();
    }
}
