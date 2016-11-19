package ru.ifmo.android_2016.irc.utils;

import com.annimon.stream.function.Function;
import com.annimon.stream.function.FunctionalInterface;

import java.io.IOException;
import java.net.HttpURLConnection;

/**
 * Created by ghost on 11/13/2016.
 */

public class FunctionUtils {
    public static Runnable catchExceptions(final RunnableWithException<Exception> runnable,
                                           Function<Exception, Void> exceptionHandler) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                exceptionHandler.apply(e);
            }
        };
    }

    public static Runnable catchExceptions(final RunnableWithException<Exception> runnable) {
        return catchExceptions(runnable, (e) -> {
            e.printStackTrace();
            return null;
        });
    }

    @FunctionalInterface
    public interface RunnableWithException<E extends Exception> {
        void run() throws E;
    }

    @FunctionalInterface
    public interface CallableWithException<E extends Exception, R> {
        R call() throws E;
    }

    @FunctionalInterface
    public interface Procedure<P> {
        void call(P param);
    }

    @FunctionalInterface
    public interface ProcedureWithException<E extends Exception, P> {
        void call(P param) throws E;
    }

    @SuppressWarnings("WeakerAccess")
    public static class TryWithUrlConnection<E extends Exception> {
        private final CallableWithException<E, HttpURLConnection> func;

        private TryWithUrlConnection(CallableWithException<E, HttpURLConnection> func) {
            this.func = func;
        }

        public WithException doOp(ProcedureWithException<E, HttpURLConnection> procedure) {
            return new WithException(() -> {
                HttpURLConnection connection = func.call();
                try {
                    procedure.call(connection);
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            });
        }

        public class WithException implements RunnableWithException<E> {
            private final RunnableWithException<E> runnable;

            private WithException(RunnableWithException<E> runnable) {
                this.runnable = runnable;
            }

            public WithException catchWith(Class<E> exception, Procedure<Exception> catcher) {
                return new WithException(() -> {
                    try {
                        this.run();
                    } catch (Exception x) {
                        if (exception.isInstance(x)) {
                            catcher.call(x);
                        } else {
                            throw x;
                        }
                    }
                });
            }

            public Runnable catchException(Procedure<Exception> handler) {
                return () -> {
                    try {
                        run();
                    } catch (Exception x) {
                        x.printStackTrace();
                    }
                };
            }

            @Override
            public void run() throws E {
                runnable.run();
            }

            public void runUnchecked() {
                try {
                    run();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static TryWithUrlConnection<IOException> tryWith(
            CallableWithException<IOException, HttpURLConnection> func) {
        return new TryWithUrlConnection<>(func);
    }

    public static class Reference<T> {
        public T ref;

        public Reference() {
            ref = null;
        }

        public Reference(T ref) {
            this.ref = ref;
        }
    }
}