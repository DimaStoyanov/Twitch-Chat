package ru.ifmo.android_2016.irc.utils;

import android.os.AsyncTask;

/**
 * Created by ghost on 11/25/2016.
 */

public class BackgroundTask extends AsyncTask<Void, Void, Void> {
    private final Runnable runnable;

    public BackgroundTask(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    protected Void doInBackground(Void... params) {
        runnable.run();
        return null;
    }
}
