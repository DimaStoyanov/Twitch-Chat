package ru.ifmo.android_2016.irc.utils;

/**
 * Created by ghost on 11/13/2016.
 */

public class FunctionUtils {
    public static Runnable catchExceptions(final RunnableWithException runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };
    }

    @FunctionalInterface
    public interface RunnableWithException {
        void run() throws Exception;
    }
}
