package ru.ifmo.android_2016.irc.utils;

/**
 * Created by ghost on 11/22/2016.
 */

@SuppressWarnings("unused")
public final class Log {
    private static final boolean DEBUG = false;

    private Log() {
    }

    public static void i(String tag, String smg) {
        if (DEBUG) android.util.Log.i(tag, smg);
    }

    public static void d(String tag, String msg) {
        if (DEBUG) android.util.Log.d(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (DEBUG) android.util.Log.w(tag, msg);
    }

    public static void wtf(String tag, String msg) {
        if (DEBUG) android.util.Log.wtf(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (DEBUG) android.util.Log.e(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (DEBUG) android.util.Log.v(tag, msg);
    }

    public static void i(String tag, String smg, Throwable tr) {
        if (DEBUG) android.util.Log.i(tag, smg, tr);
    }

    public static void d(String tag, String msg, Throwable tr) {
        if (DEBUG) android.util.Log.d(tag, msg, tr);
    }

    public static void w(String tag, String msg, Throwable tr) {
        if (DEBUG) android.util.Log.w(tag, msg, tr);
    }

    public static void wtf(String tag, String msg, Throwable tr) {
        if (DEBUG) android.util.Log.wtf(tag, msg, tr);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (DEBUG) android.util.Log.e(tag, msg, tr);
    }

    public static void v(String tag, String msg, Throwable tr) {
        if (DEBUG) android.util.Log.v(tag, msg, tr);
    }
}
