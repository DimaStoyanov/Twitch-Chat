package ru.ifmo.android_2016.irc;

/**
 * Created by ghost on 11/14/2016.
 */

public class IRCApplication extends android.app.Application {

    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = (thread, throwable) -> {
        //Nothing
    };

    @Override
    public void onCreate() {
        super.onCreate();
        //TODO: Uncaught Exceptions on the screen
        //Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
    }
}
