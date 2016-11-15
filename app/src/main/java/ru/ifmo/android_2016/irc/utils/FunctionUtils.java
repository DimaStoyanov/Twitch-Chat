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

    public static class TryWithUrlConnection<E extends Exception> {
        private final CallableWithException<E, HttpURLConnection> func;

        public TryWithUrlConnection(CallableWithException<E, HttpURLConnection> func) {
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

            public WithException(RunnableWithException<E> runnable) {
                this.runnable = runnable;
            }
            
            public Runnable catchWithException(Procedure<Exception> catcher) {
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
        }
    }

    public static TryWithUrlConnection<IOException> tryWith(
            CallableWithException<IOException, HttpURLConnection> func) {
        return new TryWithUrlConnection<>(func);
    }
}
