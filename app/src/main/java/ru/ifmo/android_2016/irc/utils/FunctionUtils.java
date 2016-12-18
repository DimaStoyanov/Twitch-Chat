package ru.ifmo.android_2016.irc.utils;

import com.annimon.stream.Optional;
import com.annimon.stream.function.Consumer;
import com.annimon.stream.function.FunctionalInterface;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;

/**
 * Created by ghost on 11/13/2016.
 */

public class FunctionUtils {
    public static Runnable catchExceptions(final RunnableWithException<? extends Exception> runnable,
                                           Consumer<Exception> exceptionHandler) {
        return () -> {
            try {
                runnable.run();
            } catch (Exception e) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(e);
                }
            }
        };
    }

    public static Runnable lolExceptions(final RunnableWithException<? extends Exception> runnable) {
        return catchExceptions(runnable, FunctionUtils::throwChecked);
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
    public interface ConsumerWithException<E extends Exception, T> {
        void accept(T param) throws E;
    }

    @SuppressWarnings("WeakerAccess")
    public static class TryWithUrlConnection<E extends Exception> {
        private final CallableWithException<E, HttpURLConnection> func;

        private TryWithUrlConnection(CallableWithException<E, HttpURLConnection> func) {
            this.func = func;
        }

        public WithException doOp(ConsumerWithException<E, HttpURLConnection> procedure) {
            return new WithException(() -> {
                HttpURLConnection connection = func.call();
                try {
                    procedure.accept(connection);
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

            public WithException catchWith(Class<E> exception, Consumer<Exception> catcher) {
                return new WithException(() -> {
                    try {
                        this.run();
                    } catch (Exception x) {
                        if (exception.isInstance(x)) {
                            catcher.accept(x);
                        } else {
                            throw x;
                        }
                    }
                });
            }

            public Runnable catchException(Consumer<Exception> handler) {
                return () -> {
                    try {
                        run();
                    } catch (Exception x) {
                        handler.accept(x);
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
                    throwChecked(e);
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

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void uncheckedThrow(Object what) throws T {
        throw (T) what;
    }

    @SuppressWarnings("RedundantTypeArguments")
    public static void throwChecked(Throwable throwable) {
        FunctionUtils.<RuntimeException>uncheckedThrow(throwable);
    }

    public static void lol(RunnableWithException<? extends Exception> r) {
        lolExceptions(r).run();
    }

    public static <T> T fuckCheckedExceptions(CallableWithException<Exception, T> runnable) {
        try {
            return runnable.call();
        } catch (Exception e) {
            throwChecked(e);
        }
        return null;
    }

    public static <T> void doIfNotNull(T object, Consumer<T> action) {
        if (object != null) {
            action.accept(object);
        }
    }

    public static Optional<InputStream> getInputStream(HttpURLConnection httpURLConnection) throws IOException {
        httpURLConnection.connect();

        if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return Optional.of(httpURLConnection.getInputStream());
        } else {
            return Optional.empty();
        }
    }
}